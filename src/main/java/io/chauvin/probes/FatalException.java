// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.chauvin.probes;

/**
 * An exception thrown by {@link ProbeHelpers} when a probe results
 * in a fatal error.
 *
 * When a fatal error is encountered, all the in-flight probes are cancelled
 * and no new probe is scheduled.
 *
 * Fatal errors are useful if the error is so severe that it would make
 * no sense to execute other probes down the line.
 */
public class FatalException extends RuntimeException {}
