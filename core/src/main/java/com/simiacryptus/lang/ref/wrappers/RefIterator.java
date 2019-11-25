package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.RefUtil;

import java.util.Iterator;

class RefIterator<T> implements Iterator<T> {

  private final Iterator<T> keyIterator;
  private Object current;

  public RefIterator(Iterator<T> keyIterator) {
    this.keyIterator = keyIterator;
  }

  @Override
  public boolean hasNext() {
    return keyIterator.hasNext();
  }

  @Override
  public T next() {
    current = keyIterator.next();
    RefUtil.addRef(current);
    return (T) current;
  }

  @Override
  public void remove() {
    keyIterator.remove();
    RefUtil.freeRef(current);
  }
}
