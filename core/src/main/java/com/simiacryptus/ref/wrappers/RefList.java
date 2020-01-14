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

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;

@RefIgnore
@SuppressWarnings("unused")
public interface RefList<T> extends ReferenceCounting, List<T>, RefCollection<T> {

  @Nonnull
  List<T> getInner();

  @Nonnull
  public static <T> RefList<T>[] addRefs(@Nonnull RefList<T>[] array) {
    return Arrays.stream(array).filter((x) -> x != null).map(RefList::addRef).toArray((x) -> new RefList[x]);
  }

  @Nonnull
  RefList<T> addRef();

  @Nonnull
  @Override
  RefListIterator<T> listIterator();

  @Nonnull
  @Override
  RefListIterator<T> listIterator(int index);

  @Override
  RefSpliterator<T> spliterator();

  @Override
  RefStream<T> stream();

  @Nonnull
  @Override
  RefList<T> subList(int fromIndex, int toIndex);

  @Override
  boolean contains(@RefAware Object o);

  @Nonnull
  @Override
  <T1> T1[] toArray(@Nonnull @RefAware T1[] a);

  @Override
  boolean add(@RefAware T t);

  @Override
  boolean remove(@RefAware Object o);

  @Override
  boolean containsAll(@Nonnull @RefAware Collection<?> c);

  @Override
  boolean addAll(@Nonnull @RefAware Collection<? extends T> c);

  @Override
  boolean addAll(int index, @Nonnull @RefAware Collection<? extends T> c);

  @Override
  boolean removeAll(@Nonnull @RefAware Collection<?> c);

  @Override
  boolean retainAll(@Nonnull @RefAware Collection<?> c);

  @Override
  default void replaceAll(@RefAware UnaryOperator<T> operator) {

  }

  @Override
  default void sort(@RefAware Comparator<? super T> c) {

  }

  @Override
  T set(int index, @RefAware T element);

  @Override
  void add(int index, @RefAware T element);

  @Override
  int indexOf(@RefAware Object o);

  @Override
  int lastIndexOf(@RefAware Object o);
}
