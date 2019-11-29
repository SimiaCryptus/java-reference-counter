package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.RefUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

class RefIterator<T> implements Iterator<T> {

  private final Iterator<T> inner;
  private Object current;

  public RefIterator(Iterator<T> inner) {
    this.inner = inner;
  }

  @Override
  public boolean hasNext() {
    return inner.hasNext();
  }

  @NotNull
  @Override
  public T next() {
    current = inner.next();
    return RefUtil.addRef((T) current);
  }

  @Override
  public void remove() {
    inner.remove();
    RefUtil.freeRef(current);
    current = null;
  }
}
