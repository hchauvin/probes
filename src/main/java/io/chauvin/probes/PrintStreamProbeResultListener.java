// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.chauvin.probes;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.fusesource.jansi.Ansi;

/**
 * A probe result listener that pretty-print the probe results as they arrive, in a human-readable
 * manner.
 */
public class PrintStreamProbeResultListener extends CountingProbeResultListener {
  private final PrintStream ps;
  private final boolean showMessageOnRetry;
  private final boolean withUTF8Symbols;
  private final boolean withColors;

  /** @return A {@link PrintStream} for stdout with an enforced UTF-8 encoding. */
  public static PrintStream getUTF8Stdout() {
    try {
      return new PrintStream(System.out, true, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  /**
   * Creates a listener that will pretty-print probe results as they arrive to a print stream.
   *
   * @param ps The print stream to use for reports. The encoding is assumed to be UTF-8.
   * @see #getUTF8Stdout()
   */
  public PrintStreamProbeResultListener(PrintStream ps) {
    this(ps, false, true, true);
  }

  /**
   * Creates a listener that will pretty-print probe results as they arrive to a print stream, with
   * additional options.
   *
   * @param ps The print stream to use for reports. The encoding is assumed to be UTF-8.
   * @param showMessageOnRetry Shows messages when a probe errored and will be retried. Disabling
   *     showing these messages make sense if they would confuse the user.
   * @param withUTF8Symbols Puts UTF-8 symbols to the print stream. Disable this if this would give
   *     a garbled output, for instance in a terminal that does not support such symbols.
   * @param withColors Use ANSI colors in the output. Disable this if this would give a garbled
   *     output, for instance in a terminal or session that does not support colors.
   */
  public PrintStreamProbeResultListener(
      PrintStream ps, boolean showMessageOnRetry, boolean withUTF8Symbols, boolean withColors) {
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
    if (result.getMessage() != null
        && !result.getMessage().isEmpty()
        && (result.getStatus() != ProbeResult.Status.RETRY || showMessageOnRetry)) {
      ps.println("    " + result.getMessage());
    }
  }

  /** Prints a summary of all the probes that ran so far. */
  public void printSummary() {
    if (success()) {
      ps.println(probeCount() + " probes all succeeded");
    } else {
      int failureCount = probeCount() - successCount();
      ps.println(failureCount + "/" + probeCount() + " probes FAILED");
    }
  }

  private String formatStatus(ProbeResult.Status status) {
    switch (status) {
      case STARTED:
        return format("\uD83C\uDFC1", "STARTED", Ansi.Color.MAGENTA, Ansi.Color.WHITE);
      case OK:
        return format("✓", "OK", Ansi.Color.GREEN, Ansi.Color.DEFAULT);
      case RETRY:
        return format("⌛", "STILL WAITING...", Ansi.Color.BLUE, Ansi.Color.WHITE);
      case ERROR:
        return format("\uD83D\uDED1", "ERROR", Ansi.Color.RED, Ansi.Color.WHITE);
      case FATAL:
        return format("\uD83D\uDED1", "FATAL ERROR", Ansi.Color.RED, Ansi.Color.WHITE);
      default:
        throw new AssertionError();
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
