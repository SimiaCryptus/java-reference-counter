package com.simiacryptus.lang.ref.wrappers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Spliterator;
import java.util.function.Consumer;

import static com.simiacryptus.lang.ref.RefUtil.addRef;

class RefSpliterator<T> implements Spliterator<T> {
  private final Spliterator<T> inner;

  public RefSpliterator(Spliterator<T> inner) {
    this.inner = inner;
  }

  @Override
  public int characteristics() {
    return this.inner.characteristics();
  }

  @Override
  public long estimateSize() {
    return this.inner.estimateSize();
  }

  @Override
  public boolean tryAdvance(@NotNull Consumer<? super T> action) {
    return inner.tryAdvance(t -> action.accept(addRef(t)));
  }

  @Nullable
  @Override
  public Spliterator<T> trySplit() {
    final Spliterator<T> trySplit = this.inner.trySplit();
    return null == trySplit ? null : new RefSpliterator<>(trySplit);
  }
}
