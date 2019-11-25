package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.RefUtil;

import java.util.ListIterator;

class RefListIterator<T> implements ListIterator<T> {

  private final ListIterator<T> listIterator;
  private T current;

  public RefListIterator(ListIterator<T> listIterator) {
    this.listIterator = listIterator;
  }

  @Override
  public void add(T t) {
    listIterator.add(RefUtil.addRef(t));
    RefUtil.freeRef(current);
    current = t;
  }

  @Override
  public boolean hasNext() {
    return listIterator.hasNext();
  }

  @Override
  public boolean hasPrevious() {
    return listIterator.hasPrevious();
  }

  @Override
  public T next() {
    current = listIterator.next();
    return RefUtil.addRef(current);
  }

  @Override
  public int nextIndex() {
    return listIterator.nextIndex();
  }

  @Override
  public T previous() {
    current = listIterator.previous();
    return RefUtil.addRef(current);
  }

  @Override
  public int previousIndex() {
    return listIterator.previousIndex();
  }

  @Override
  public void remove() {
    listIterator.remove();
    RefUtil.freeRef(current);
    current = null;
  }

  @Override
  public void set(T t) {
    listIterator.set(RefUtil.addRef(t));
    RefUtil.freeRef(current);
    current = t;
  }
}
