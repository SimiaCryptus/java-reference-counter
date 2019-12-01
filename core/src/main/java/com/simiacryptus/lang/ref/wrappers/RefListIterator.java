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

package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.RefAware;
import com.simiacryptus.lang.ref.RefUtil;

import java.util.ListIterator;

@RefAware
class RefListIterator<T> extends RefIterator<T> implements ListIterator<T> {

  public RefListIterator(ListIterator<T> inner) {
    super(inner);
  }

  @Override
  public void add(T t) {
    getInner().add(t);
  }

  public ListIterator<T> getInner() {
    return (ListIterator<T>) inner;
  }

  @Override
  public boolean hasPrevious() {
    return getInner().hasPrevious();
  }

  @Override
  public int nextIndex() {
    return getInner().nextIndex();
  }

  @Override
  public T previous() {
    current = getInner().previous();
    return RefUtil.addRef(current);
  }

  @Override
  public int previousIndex() {
    return getInner().previousIndex();
  }

  @Override
  public void set(T t) {
    getInner().set(t);
    RefUtil.freeRef(current);
    current = null;
  }
}
