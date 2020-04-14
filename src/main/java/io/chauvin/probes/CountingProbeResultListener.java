// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.chauvin.probes;

import java.util.HashSet;
import java.util.Set;

/**
 * A probe result listener that counts the probes.
 */
public class CountingProbeResultListener implements ProbeResultListener {
  private Set<String> names = new HashSet<>();
  private int successCount = 0;
  private int probeCount = 0;

  @Override
  public synchronized void onProbeResult(ProbeResult result) {
    if (!names.contains(result.getName())) {
      ++probeCount;
      names.add(result.getName());
    }
    if (result.getStatus() == ProbeResult.Status.OK) {
      ++successCount;
    }
  }

  @Override
  public boolean success() {
    return successCount() == probeCount();
  }

  @Override
  public int successCount() {
    return successCount;
  }

  @Override
  public int probeCount() {
    return probeCount;
  }
}
