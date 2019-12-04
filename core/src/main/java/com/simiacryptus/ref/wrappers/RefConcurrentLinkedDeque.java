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
import com.simiacryptus.ref.lang.RefCoderIgnore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;

@RefAware
@RefCoderIgnore
public class RefConcurrentLinkedDeque<T> extends RefAbstractCollection<T> implements RefDeque<T> {

  private final ConcurrentLinkedDeque<T> inner;

  public RefConcurrentLinkedDeque() {
    inner = new ConcurrentLinkedDeque<>();
  }

  public static <T> RefConcurrentLinkedDeque<T>[] addRefs(@NotNull RefConcurrentLinkedDeque<T>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefConcurrentLinkedDeque::addRef)
        .toArray((x) -> new RefConcurrentLinkedDeque[x]);
  }

  @Override
  public boolean add(T t) {
    return getInner().add(t);
  }

  @Override
  public boolean addAll(@NotNull Collection<? extends T> c) {
    return getInner().addAll(c);
  }

  @Override
  public void addFirst(T t) {
    getInner().addFirst(t);
  }

  @Override
  public void addLast(T t) {
    getInner().addLast(t);
  }

  @NotNull
  public @Override
  RefConcurrentLinkedDeque<T> addRef() {
    return (RefConcurrentLinkedDeque<T>) super.addRef();
  }

  @NotNull
  @Override
  public RefIterator<T> descendingIterator() {
    return new RefIterator(getInner().descendingIterator());
  }

  @Override
  public T element() {
    return getInner().element();
  }

  @Override
  public T getFirst() {
    return getInner().getFirst();
  }

  @Override
  protected ConcurrentLinkedDeque<T> getInner() {
    return inner;
  }

  @Override
  public T getLast() {
    return getInner().getLast();
  }

  @Override
  public boolean offer(T t) {
    return getInner().offer(t);
  }

  @Override
  public boolean offerFirst(T t) {
    return getInner().offerFirst(t);
  }

  @Override
  public boolean offerLast(T t) {
    return getInner().offerLast(t);
  }

  @Nullable
  @Override
  public T peek() {
    return getInner().peek();
  }

  @Override
  public T peekFirst() {
    return getInner().peekFirst();
  }

  @Override
  public T peekLast() {
    return getInner().peekLast();
  }

  @Nullable
  @Override
  public T poll() {
    return getInner().poll();
  }

  @Nullable
  @Override
  public T pollFirst() {
    return getInner().peekFirst();
  }

  @Nullable
  @Override
  public T pollLast() {
    return getInner().pollLast();
  }

  @Override
  public T pop() {
    return getInner().pop();
  }

  @Override
  public void push(T t) {
    getInner().push(t);
  }

  @Override
  public boolean remove(Object o) {
    return getInner().remove(o);
  }

  @Override
  public T remove() {
    return getInner().remove();
  }

  @Override
  public T removeFirst() {
    return getInner().removeFirst();
  }

  @Override
  public boolean removeFirstOccurrence(Object o) {
    return getInner().removeFirstOccurrence(o);
  }

  @Override
  public T removeLast() {
    return getInner().removeLast();
  }

  @Override
  public boolean removeLastOccurrence(Object o) {
    return getInner().removeLastOccurrence(o);
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> c) {
    return getInner().retainAll(c);
  }

}
