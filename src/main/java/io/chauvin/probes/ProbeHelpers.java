// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.chauvin.probes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class ProbeHelpers {
  private ProbeResultListener listener;
  private boolean parallel;
  private @Nullable ExecutorService executor;
  private Set<String> completedSections = new HashSet<>();

  private InheritableThreadLocal<Stack<String>> sectionHierarchy =
      new InheritableThreadLocal<Stack<String>>() {
        @Override
        public Stack<String> childValue(Stack<String> parentValue) {
          Stack<String> childValue = new Stack<>();
          childValue.addAll(parentValue);
          return childValue;
        }
      };

  public ProbeHelpers(ProbeResultListener listener, boolean parallel) {
    this.listener = listener;
    this.sectionHierarchy.set(new Stack<>());
    this.parallel = parallel;
  }

  public void onRetry(String message, int retries) {
    onProbeResult(new ProbeResult(name(), message, ProbeResult.ProbeStatus.RETRY, retries));
  }

  public void onError(String message) {
    onProbeResult(new ProbeResult(name(), message, ProbeResult.ProbeStatus.ERROR, 0));
  }

  public RuntimeException onFatal(String message, int retries) {
    onProbeResult(new ProbeResult(name(), message, ProbeResult.ProbeStatus.FATAL, retries));
    throw new FatalException();
  }

  public void onSuccess(int retries) {
    onProbeResult(new ProbeResult(name(), null, ProbeResult.ProbeStatus.OK, retries));
  }

  private synchronized void onProbeResult(ProbeResult result) {
    if (completedSections.contains(result.getName())) {
      throw onFatal(String.format("<section '%s' is already completed>", result.getName()), 0);
    }
    if (result.getStatus() != ProbeResult.ProbeStatus.RETRY) {
      completedSections.add(result.getName());
    }
    listener.onProbeResult(result);
  }

  public void onSuccess() {
    onSuccess(0);
  }

  public void section(String name, Runnable runnable) {
    sectionHierarchy.get().push(name);
    runnable.run();
    sectionHierarchy.get().pop();
  }

  public void retry(String name, int maxRetries, Duration backoff, Retriable retriable) {
    section(
        name,
        () -> {
          int retries = 0;
          while (true) {
            String errorMessage;
            try {
              errorMessage = retriable.run();
            } catch (Exception e) {
              throw onFatal(
                  "exception: "
                      + Stream.of(e.getStackTrace())
                          .map(StackTraceElement::toString)
                          .collect(Collectors.joining("\n")),
                  retries);
            }
            if (errorMessage == null) {
              onSuccess(retries);
              return;
            }
            if (retries >= maxRetries) {
              throw onFatal(errorMessage, retries);
            }
            onRetry(errorMessage, retries);
            try {
              Thread.sleep(backoff.toMillis());
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
            ++retries;
          }
        });
  }

  public void retryExceptions(
      String name, int maxRetries, Duration backoff, RunnableWithExceptions runnable) {
    section(
        name,
        () -> {
          int retries = 0;
          while (true) {
            boolean success = false;
            try {
              runnable.run();
              success = true;
            } catch (Exception e) {
              if (retries >= maxRetries) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                throw onFatal("exception: " + sw, retries);
              }
              String message = e.getMessage();
              onRetry(message, retries);
            }
            if (success) {
              onSuccess(retries);
              break;
            }
            try {
              Thread.sleep(backoff.toMillis());
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
            ++retries;
          }
        });
  }

  public void must(String name, RunnableWithExceptions runnable) {
    retryExceptions(name, 0, Duration.ZERO, runnable);
  }

  public static class ProbeFuture {
    private CompletableFuture<Void> future;

    public ProbeFuture(CompletableFuture<Void> future) {
      this.future = future;
    }

    public void get() {
      try {
        future.get();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } catch (ExecutionException e) {
        if (e.getCause() != null && e.getCause() instanceof FatalException) {
          throw (FatalException) e.getCause();
        }
        throw new RuntimeException(e);
      }
    }
  }

  public ProbeFuture parallel(Runnable... runnables) {
    CompletableFuture<Void> future =
        CompletableFuture.allOf(
            Stream.of(runnables)
                .map(runnable -> CompletableFuture.runAsync(runnable, getExecutor()))
                .toArray(CompletableFuture[]::new));
    return new ProbeFuture(future);
  }

  public void shutdownNow() {
    List<Runnable> pendingTasks = executor.shutdownNow();
    if (!pendingTasks.isEmpty()) {
      throw new RuntimeException(pendingTasks.size() + " tasks are pending");
    }
  }

  private synchronized ExecutorService getExecutor() {
    if (executor == null) {
      if (parallel) {
        executor = Executors.newCachedThreadPool();
      } else {
        executor = Executors.newSingleThreadExecutor();
      }
    }
    return executor;
  }

  private String name() {
    return String.join(" :: ", sectionHierarchy.get());
  }
}
