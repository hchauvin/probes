// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.chauvin.probes;

@FunctionalInterface
public interface RunnableWithExceptions {
  void run() throws Exception;
}
