// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.chauvin.probes;

public interface ProbeResultListener {
  void onProbeResult(ProbeResult result);

  boolean success();

  int successCount();

  int probeCount();
}
