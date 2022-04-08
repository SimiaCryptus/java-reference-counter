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

package com.simiacryptus.ref.wrappers;

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

/**
 * A class that represents a linked list.
 *
 * @param <T> the type of element in the list
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public class RefLinkedList<T> extends RefAbstractList<T> {
  @Nonnull
  private final LinkedList<T> inner;

  public RefLinkedList() {
    this.inner = new LinkedList<>();
  }

  public RefLinkedList(@Nonnull @RefAware List<T> list) {
    this();
    this.addAll(list);
  }

  /**
   * @return the inner linked list
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public LinkedList<T> getInner() {
    return inner;
  }

  /**
   * Adds a reference to this object.
   *
   * @return a reference to this object
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  RefLinkedList<T> addRef() {
    return (RefLinkedList<T>) super.addRef();
  }

}
