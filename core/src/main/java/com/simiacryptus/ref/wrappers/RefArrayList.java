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
import java.util.ArrayList;
import java.util.Collection;

/**
 * A class that represents an ArrayList of references.
 *
 * @param <T> the type of reference contained in this list
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public class RefArrayList<T> extends RefAbstractList<T> {
  @Nonnull
  private final ArrayList<T> inner;

  public RefArrayList() {
    this.inner = new ArrayList<>();
  }

  public RefArrayList(int length) {
    this.inner = new ArrayList<>(length);
  }

  public RefArrayList(@Nonnull @RefAware Collection<T> list) {
    this(list.size());
    addAll(list);
  }

  /**
   * @return the inner ArrayList
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public ArrayList<T> getInner() {
    return inner;
  }

  /**
   * @return a new RefArrayList with a reference to this object
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  RefArrayList<T> addRef() {
    return (RefArrayList<T>) super.addRef();
  }

}
