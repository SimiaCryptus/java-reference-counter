/*
 * Copyright (c) 2019 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.ref.wrappers;

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * The type Ref linked list.
 *
 * @param <T> the type parameter
 */
@RefAware
@RefIgnore
public class RefLinkedList<T> extends RefAbstractList<T> {
  @NotNull
  private final List<T> inner;

  /**
   * Instantiates a new Ref linked list.
   */
  public RefLinkedList() {
    this.inner = new LinkedList<>();
  }

  /**
   * Instantiates a new Ref linked list.
   *
   * @param list the list
   */
  public RefLinkedList(@NotNull List<T> list) {
    this();
    this.addAll(list);
  }

  @Override
  public List<T> getInner() {
    return inner;
  }

  /**
   * Add refs ref linked list [ ].
   *
   * @param <T>   the type parameter
   * @param array the array
   * @return the ref linked list [ ]
   */
  public static <T> RefLinkedList<T>[] addRefs(@NotNull RefLinkedList<T>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefLinkedList::addRef)
        .toArray((x) -> new RefLinkedList[x]);
  }

  @NotNull
  public @Override
  RefLinkedList<T> addRef() {
    return (RefLinkedList<T>) super.addRef();
  }

}
