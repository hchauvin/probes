// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.chauvin.probes;

import javax.annotation.Nullable;

/** The result of a probe. */
public class ProbeResult {
  private String name;
  private @Nullable String message;
  private Status status;
  private int retries;

  public ProbeResult(String name, @Nullable String message, Status status, int retries) {
    this.name = name;
    this.message = message;
    this.status = status;
    this.retries = retries;
  }

  /** The name of the probe. */
  public String getName() {
    return name;
  }

  /**
   * An optional message associated with the probing, for instance, the description
   * of an error.
   */
  public @Nullable String getMessage() {
    return message;
  }

  /** The status of the probing. */
  public Status getStatus() {
    return status;
  }

  /** The number of times this probe was retried. */
  public int getRetries() {
    return retries;
  }

  /** The status of the probing. */
  public enum Status {
    /**
     * The probe just started.
     *
     * It is not required to report that a probe is starting.  Reporting this
     * might however be useful if the probe takes some time to execute.  This
     * gives an opportunity to the {@link ProbeResultListener} to give feedback
     * to the user.
     */
    STARTED,

    /** The probe succeeeded. */
    OK,

    /**
     * The probe failed, but the error is assumed to be transient, and the
     * probe will be retried.
     */
    RETRY,

    /**
     * The probe errored.  The error is non-fatal, the other probes will
     * continue executing, and new probes can be scheduled.
     */
    ERROR,

    /**
     * The probe errored, and the error is fatal.  The other probes will
     * be cancelled, and no new probe will be scheduled.
     */
    FATAL
  }
}
