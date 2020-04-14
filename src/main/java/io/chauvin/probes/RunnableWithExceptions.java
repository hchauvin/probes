// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.chauvin.probes;

/** A runnable that can throw checked exceptions. */
@FunctionalInterface
public interface RunnableWithExceptions {
  void run() throws Exception;
}
