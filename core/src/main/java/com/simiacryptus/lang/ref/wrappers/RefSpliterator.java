package com.simiacryptus.lang.ref.wrappers;

import java.util.Spliterator;
import java.util.function.Consumer;

import static com.simiacryptus.lang.ref.RefUtil.addRef;
import static com.simiacryptus.lang.ref.RefUtil.freeRef;

class RefSpliterator<T> implements Spliterator<T> {
  private final Spliterator<T> inner;

  public RefSpliterator(Spliterator<T> inner) {
    this.inner = inner;
  }

  @Override
  public boolean tryAdvance(Consumer<? super T> action) {
    final boolean tryAdvance = inner.tryAdvance(t -> action.accept(addRef(t)));
    freeRef(tryAdvance);
    return tryAdvance;
  }

  @Override
  public Spliterator<T> trySplit() {
    final Spliterator<T> trySplit = this.inner.trySplit();
    return null == trySplit ? null : new RefSpliterator<>(trySplit);
  }

  @Override
  public long estimateSize() {
    return this.inner.estimateSize();
  }

  @Override
  public int characteristics() {
    return this.inner.characteristics();
  }
}
