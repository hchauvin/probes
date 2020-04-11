// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.chauvin.probes;

import javax.annotation.Nullable;

@FunctionalInterface
public interface Retriable {
  @Nullable
  String run();
}
