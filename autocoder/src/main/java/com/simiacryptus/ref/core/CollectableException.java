/*
 * Copyright (c) 2020 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.ref.core;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

public class CollectableException extends RuntimeException {
  @Nonnull
  private final String[] messages;

  public CollectableException(@Nonnull String... str) {
    super(Arrays.stream(str).reduce((a, b) -> a + "\n" + b).get());
    this.messages = str;
  }

  @Nonnull
  public String[] getMessages() {
    return messages;
  }

  @Nonnull
  public static CollectableException combine(@Nonnull Collection<CollectableException> others) {
    return new CollectableException(others.stream()
        .flatMap(x -> Arrays.stream(x.messages))
        .toArray(i -> new String[i]));
  }

  @Nonnull
  public CollectableException combine(CollectableException... others) {
    return new CollectableException(Stream.concat(
        Stream.of(this),
        Stream.of(others)
    ).flatMap(x -> Arrays.stream(x.messages))
        .toArray(i -> new String[i]));
  }
}
