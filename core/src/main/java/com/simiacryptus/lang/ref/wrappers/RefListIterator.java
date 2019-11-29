package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.RefUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ListIterator;

class RefListIterator<T> extends RefIterator<T> implements ListIterator<T> {

  private final ListIterator<T> inner;
  @Nullable
  private T current;

  public RefListIterator(ListIterator<T> inner) {
    super(inner);
    this.inner = inner;
  }

  @Override
  public void add(T t) {
    current = null;
    inner.add(t);
  }

  @Override
  public boolean hasPrevious() {
    return inner.hasPrevious();
  }

  @Override
  public int nextIndex() {
    return inner.nextIndex();
  }

  @Override
  public T previous() {
    current = (inner.previous());
    return RefUtil.addRef(current);
  }

  @Override
  public int previousIndex() {
    return inner.previousIndex();
  }

  @Override
  public void set(T t) {
    inner.set(t);
    RefUtil.freeRef(current);
    current = null;
  }
}
