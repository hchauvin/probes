// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.chauvin.probes;

import org.fusesource.jansi.Ansi;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class PrintStreamProbeResultListener extends CountingProbeResultListener {
  private final PrintStream ps;
  private final boolean showMessageOnRetry;
  private final boolean withUTF8Symbols;
  private final boolean withColors;

  public static PrintStream getUTF8Stdout() {
    try {
      return new PrintStream(System.out, true, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  public PrintStreamProbeResultListener(PrintStream ps) {
    this(ps, false, true, true);
  }

  public PrintStreamProbeResultListener(PrintStream ps,
                                        boolean showMessageOnRetry,
                                        boolean withUTF8Symbols,
                                        boolean withColors) {
    this.ps = ps;
    this.showMessageOnRetry = showMessageOnRetry;
    this.withUTF8Symbols = withUTF8Symbols;
    this.withColors = withColors;
  }

  @Override
  public synchronized void onProbeResult(ProbeResult result) {
    super.onProbeResult(result);
    ps.println(
        result.getName()
            + formatConnector("     → ", "     => ")
            + formatStatus(result.getStatus())
            + (result.getRetries() > 0 ? " (" + result.getRetries() + ")" : ""));
    if (result.getMessage() != null && !result.getMessage().isEmpty() &&
        (result.getStatus() != ProbeResult.ProbeStatus.RETRY || showMessageOnRetry)) {
      ps.println("    " + result.getMessage());
    }
  }

  public void printSummary() {
    if (success()) {
      ps.println(probeCount() + " probes all succeeded");
    } else {
      int failureCount = probeCount() - successCount();
      ps.println(failureCount + "/" + probeCount() + " probes FAILED");
    }
  }

  private String formatStatus(ProbeResult.ProbeStatus status) {
    switch (status) {
      case STARTED: return format("\uD83C\uDFC1", "STARTED", Ansi.Color.MAGENTA, Ansi.Color.DEFAULT);
      case OK: return format("✓", "OK", Ansi.Color.GREEN, Ansi.Color.DEFAULT);
      case RETRY: return format("⌛", "STILL WAITING...", Ansi.Color.BLUE, Ansi.Color.WHITE);
      case ERROR: return format("\uD83D\uDED1", "ERROR", Ansi.Color.RED, Ansi.Color.WHITE);
      case FATAL: return format("\uD83D\uDED1", "FATAL ERROR", Ansi.Color.RED, Ansi.Color.WHITE);
      default: throw new AssertionError();
    }
  }

  private String format(String symbol, String text, Ansi.Color bgColor, Ansi.Color fgColor) {
    String a = withUTF8Symbols ? symbol + " " + text : text;
    if (withColors) {
      return Ansi.ansi().bg(bgColor).fg(fgColor).a(a).reset().toString();
    } else {
      return text;
    }
  }

  private String formatConnector(String symbol, String fallbackText) {
    return withUTF8Symbols ? symbol : fallbackText;
  }
}
