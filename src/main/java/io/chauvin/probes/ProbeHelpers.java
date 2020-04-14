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
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Various helpers for reporting probe results.
 *
 * <p>In the {@code ProbeHelpers} methods, the probe names are derived from a thread-local context.
 * Probe "sections" can be nested and this gives a hierarchy from which a full probe name is
 * derived.
 */
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

  /**
   * Creates a new instance.
   *
   * @param listener Listener to report probe results to.
   * @param parallel Whether to execute probes in parallel or serially.
   */
  public ProbeHelpers(ProbeResultListener listener, boolean parallel) {
    this.listener = listener;
    this.sectionHierarchy.set(new Stack<>());
    this.parallel = parallel;
  }

  /**
   * Reports a probe retry for the probe given by the thread-local context.
   *
   * @param message Error message.
   * @param retries Number of retries so far.
   */
  public void onRetry(String message, int retries) {
    onProbeResult(new ProbeResult(name(), message, ProbeResult.Status.RETRY, retries));
  }

  /**
   * Reports a probe error for the probe given by the thread-local context.
   *
   * @param message Error message.
   */
  public void onError(String message) {
    onProbeResult(new ProbeResult(name(), message, ProbeResult.Status.ERROR, 0));
  }

  /**
   * Reports a probe fatal error for the probe given by th thread-local context.
   *
   * @param message Error message.
   * @param retries Number of retries.
   * @return Exception that should be thrown.
   */
  public RuntimeException onFatal(String message, int retries) {
    onProbeResult(new ProbeResult(name(), message, ProbeResult.Status.FATAL, retries));
    return new FatalException();
  }

  /**
   * Reports a probe success.
   *
   * @param retries The number of retries that was necessary for the probe to succeed.
   */
  public void onSuccess(int retries) {
    onProbeResult(new ProbeResult(name(), null, ProbeResult.Status.OK, retries));
  }

  private synchronized void onProbeResult(ProbeResult result) {
    if (completedSections.contains(result.getName())) {
      throw new IllegalStateException(
          String.format("<section '%s' is already completed>", result.getName()));
    }
    if (result.getStatus() != ProbeResult.Status.RETRY) {
      completedSections.add(result.getName());
    }
    listener.onProbeResult(result);
  }

  /** Reports a probe that succeeded with no retry. */
  public void onSuccess() {
    onSuccess(0);
  }

  /**
   * Adds a level to the thread-local probe section hierarchy and executes a runnable at this new
   * level.
   *
   * <p>All the probe results reported in {@code runnable} will be reported in a (nested) section
   * named {@code name}.
   *
   * @param name The name of the (nested) report section.
   * @param runnable The runnable to run within this report section.
   */
  public void section(String name, Runnable runnable) {
    sectionHierarchy.get().push(name);
    runnable.run();
    sectionHierarchy.get().pop();
  }

  /**
   * Reports a starting probe.
   *
   * @param message The message to accompany the report.
   */
  public void start(String message) {
    onProbeResult(new ProbeResult(name(), message, ProbeResult.Status.STARTED, 0));
  }

  /** Reports a starting probe, without any accompanying message. */
  public void start() {
    start(null);
  }

  /**
   * Retries a runnable with a simple backoff.
   *
   * @param name The name of the probe.
   * @param maxRetries The maximum number of retries after which a fatal error will be reported (and
   *     a {@link FatalException} exception thrown).
   * @param backoff The simple backoff.
   * @param runnable The runnable to try.
   */
  public void retry(
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

  /**
   * Ensures that the runnable does not throw, otherwise immediately reports a fatal error without
   * attempting to retry the runnable.
   *
   * @param name The name of the corresponding probe.
   * @param runnable The runnable to try.
   */
  public void must(String name, RunnableWithExceptions runnable) {
    retry(name, 0, Duration.ZERO, runnable);
  }

  /**
   * Wraps a future that is used to execute probes.
   *
   * <p>Allows the prober unwrapping of {@link FatalException} exceptions.
   */
  public static class ProbeFuture {
    private CompletableFuture<Void> future;

    /** Wraps a completable future. */
    public ProbeFuture(CompletableFuture<Void> future) {
      this.future = future;
    }

    /**
     * Waits if necessary for this future to complete, and then returns its result.
     *
     * <p>{@link ExecutionException} exceptions are unwrapped for the {@link FatalException}
     * exception.
     */
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

  /** Runs in parallel runnables that execute probes. */
  public ProbeFuture parallel(Runnable... runnables) {
    CompletableFuture<Void> future =
        CompletableFuture.allOf(
            Stream.of(runnables)
                .map(runnable -> CompletableFuture.runAsync(runnable, getExecutor()))
                .toArray(CompletableFuture[]::new));
    return new ProbeFuture(future);
  }

  /**
   * Shuts down immediately, cancelling all the pending tasks, and ensuring that no other parallel
   * task is scheduled.
   */
  public void shutdownNow() {
    List<Runnable> pendingTasks = executor.shutdownNow();
    if (!pendingTasks.isEmpty()) {
      throw new RuntimeException(pendingTasks.size() + " tasks are pending");
    }
  }

  /** Lazily creates an executor for parallel execution. */
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

  /** Assembles a probe name from a section hierarchy. */
  private String name() {
    return String.join(" :: ", sectionHierarchy.get());
  }
}
