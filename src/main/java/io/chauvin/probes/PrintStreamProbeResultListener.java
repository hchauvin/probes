// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.chauvin.probes;

import java.io.PrintStream;

public class PrintStreamProbeResultListener extends CountingProbeResultListener {
  private PrintStream ps;

  public PrintStreamProbeResultListener(PrintStream ps) {
    this.ps = ps;
  }

  @Override
  public synchronized void onProbeResult(ProbeResult result) {
    super.onProbeResult(result);
    ps.println(
        result.getName()
            + " => "
            + result.getStatus()
            + (result.getRetries() > 0 ? "(" + result.getRetries() + ")" : ""));
    if (result.getMessage() != null && !result.getMessage().isEmpty()) {
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
}
