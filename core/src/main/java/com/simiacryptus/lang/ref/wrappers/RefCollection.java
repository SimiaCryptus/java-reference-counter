package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.ReferenceCounting;
import com.simiacryptus.lang.ref.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

public class RefCollection<T extends ReferenceCounting> extends ReferenceCountingBase implements Collection<T>, Cloneable, Serializable {
  private final Collection<? extends T> inner;

  public RefCollection(Collection<? extends T> inner) {
    this.inner = inner;
  }

  @Override
  public final int size() {
    return inner.size();
  }

  @Override
  public final boolean isEmpty() {
    return inner.isEmpty();
  }

  @Override
  public final boolean contains(Object o) {
    return inner.contains(o);
  }

  @NotNull
  @Override
  public final Iterator<T> iterator() {
    return (Iterator<T>) inner.iterator();
  }

  @NotNull
  @Override
  public final Object[] toArray() {
    return inner.toArray();
  }

  @NotNull
  @Override
  public final <T1> T1[] toArray(@NotNull T1[] a) {
    return inner.toArray(a);
  }

  @Override
  public final boolean add(T t) {
    return ((Collection<T>)inner).add(t);
  }

  @Override
  public final boolean remove(Object o) {
    return inner.remove(o);
  }

  @Override
  public final boolean containsAll(@NotNull Collection<?> c) {
    return inner.containsAll(c);
  }

  @Override
  public final boolean addAll(@NotNull Collection<? extends T> c) {
    return ((Set<T>)inner).addAll(c);
  }

  @Override
  public final boolean retainAll(@NotNull Collection<?> c) {
    return inner.retainAll(c);
  }

  @Override
  public final boolean removeAll(@NotNull Collection<?> c) {
    return inner.removeAll(c);
  }

  @Override
  public final void clear() {
    inner.clear();
  }

  @Override
  public final Stream<T> stream() {
    return inner.stream().map(x->(T)x.addRef());
  }
}
