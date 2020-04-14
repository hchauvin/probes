// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.chauvin.probes;

import javax.annotation.Nullable;

public class ProbeResult {
  private String name;
  private @Nullable String message;
  private ProbeStatus status;
  private int retries;

  public ProbeResult(String name, @Nullable String message, ProbeStatus status, int retries) {
    this.name = name;
    this.message = message;
    this.status = status;
    this.retries = retries;
  }

  public String getName() {
    return name;
  }

  public @Nullable String getMessage() {
    return message;
  }

  public ProbeStatus getStatus() {
    return status;
  }

  public int getRetries() {
    return retries;
  }

  public enum ProbeStatus {
    STARTED,
    OK,
    RETRY,
    ERROR,
    FATAL
  }
}
