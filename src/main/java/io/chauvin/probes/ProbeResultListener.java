// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.chauvin.probes;

/**
 * Listen to probe results.
 *
 * @see {@link CountingProbeResultListener}
 */
public interface ProbeResultListener {
  /** Callback for a new probe result. */
  void onProbeResult(ProbeResult result);

  /** Returns {@code true} if all the probes have succeeded so far, false otherwise. */
  boolean success();

  /** Returns the number of successful probes so far. */
  int successCount();

  /** Returns the total number of probes that have been executed so far, excluding retries. */
  int probeCount();
}
