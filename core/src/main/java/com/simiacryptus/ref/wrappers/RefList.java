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
import com.simiacryptus.ref.lang.ReferenceCounting;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;

@RefAware
@RefIgnore
@SuppressWarnings("unused")
public interface RefList<T> extends ReferenceCounting, List<T>, RefCollection<T> {

  List<T> getInner();

  @NotNull
  public static <T> RefList<T>[] addRefs(@NotNull RefList<T>[] array) {
    return Arrays.stream(array).filter((x) -> x != null).map(RefList::addRef).toArray((x) -> new RefList[x]);
  }

  @NotNull
  RefList<T> addRef();

  @NotNull
  @Override
  RefListIterator<T> listIterator();

  @NotNull
  @Override
  RefListIterator<T> listIterator(int index);

  @Override
  RefSpliterator<T> spliterator();

  @Override
  RefStream<T> stream();

  @NotNull
  @Override
  RefList<T> subList(int fromIndex, int toIndex);

  @Override
  boolean contains(@com.simiacryptus.ref.lang.RefAware Object o);

  @NotNull
  @Override
  <T1> T1[] toArray(@NotNull @com.simiacryptus.ref.lang.RefAware T1[] a);

  @Override
  boolean add(@com.simiacryptus.ref.lang.RefAware T t);

  @Override
  boolean remove(@com.simiacryptus.ref.lang.RefAware Object o);

  @Override
  boolean containsAll(@NotNull @com.simiacryptus.ref.lang.RefAware Collection<?> c);

  @Override
  boolean addAll(@NotNull @com.simiacryptus.ref.lang.RefAware Collection<? extends T> c);

  @Override
  boolean addAll(int index, @NotNull @com.simiacryptus.ref.lang.RefAware Collection<? extends T> c);

  @Override
  boolean removeAll(@NotNull @com.simiacryptus.ref.lang.RefAware Collection<?> c);

  @Override
  boolean retainAll(@NotNull @com.simiacryptus.ref.lang.RefAware Collection<?> c);

  @Override
  default void replaceAll(@com.simiacryptus.ref.lang.RefAware UnaryOperator<T> operator) {

  }

  @Override
  default void sort(@com.simiacryptus.ref.lang.RefAware Comparator<? super T> c) {

  }

  @Override
  T set(int index, @com.simiacryptus.ref.lang.RefAware T element);

  @Override
  void add(int index, @com.simiacryptus.ref.lang.RefAware T element);

  @Override
  int indexOf(@com.simiacryptus.ref.lang.RefAware Object o);

  @Override
  int lastIndexOf(@com.simiacryptus.ref.lang.RefAware Object o);
}
