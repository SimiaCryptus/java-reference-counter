package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.RefUtil;
import com.simiacryptus.lang.ref.ReferenceCounting;
import com.simiacryptus.lang.ref.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class RefHashSet<T> extends ReferenceCountingBase implements RefSet<T> {

  private final HashMap<T, T> inner;

  public RefHashSet() {
    this(new HashMap<>());
  }

  RefHashSet(@Nonnull HashMap<T, T> inner) {
    if (inner instanceof ReferenceCounting) throw new IllegalArgumentException("inner class cannot be ref-aware");
    this.inner = inner;
    this.inner.keySet().forEach(RefUtil::addRef);
  }

  public RefHashSet(@NotNull Collection<T> values) {
    this();
    addAll(values);
  }

  public static <T> RefHashSet<T>[] addRefs(@NotNull RefHashSet<T>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefHashSet::addRef)
        .toArray((x) -> new RefHashSet[x]);
  }

  @Override
  protected void _free() {
    clear();
    super._free();
  }

  @Override
  public final boolean add(T o) {
    final T replaced = inner.put(o, o);
    if (null != replaced) {
      RefUtil.freeRef(replaced);
      return false;
    } else {
      return true;
    }
  }

  @Override
  public final boolean addAll(@NotNull Collection<? extends T> c) {
    if (c instanceof ReferenceCounting) {
      final boolean returnValue = c.stream().map(o -> add(o)).reduce((a, b) -> a || b).orElse(false);
      ((ReferenceCounting) c).freeRef();
      return returnValue;
    } else {
      return c.stream().map(o -> add(RefUtil.addRef(o))).reduce((a, b) -> a || b).orElse(false);
    }

  }

  @NotNull
  public @Override
  RefHashSet<T> addRef() {
    return (RefHashSet<T>) super.addRef();
  }

  @Override
  public synchronized final void clear() {
    inner.keySet().forEach(RefUtil::freeRef);
    inner.clear();
  }

  @Override
  public final boolean contains(Object o) {
    final boolean returnValue = inner.containsKey(o);
    RefUtil.freeRef(o);
    return returnValue;
  }

  @Override
  public final boolean containsAll(@NotNull Collection<?> c) {
    if (c instanceof ReferenceCounting) {
      final boolean returnValue = c.stream().filter(o -> {
        final boolean b = !inner.containsKey(o);
        RefUtil.freeRef(o);
        return b;
      }).count() == 0;
      ((ReferenceCounting) c).freeRef();
      return returnValue;
    } else {
      return c.stream().filter(o -> !inner.containsKey(o)).count() == 0;
    }
  }

  @Override
  public final boolean isEmpty() {
    return inner.isEmpty();
  }

  @NotNull
  @Override
  public final Iterator<T> iterator() {
    return new RefIterator<>(inner.keySet().iterator());
  }

  @Override
  public final boolean remove(Object o) {
    final T removed = inner.remove(o);
    RefUtil.freeRef(o);
    if (null != removed) {
      RefUtil.freeRef(removed);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public synchronized final boolean removeAll(@NotNull Collection<?> c) {
    if (c instanceof ReferenceCounting) {
      final boolean returnValue = ((RefStream<?>) c.stream()).map(o -> {
        final T remove = inner.remove(o);
        RefUtil.freeRef(o);
        final boolean b = remove != null;
        if (b) RefUtil.freeRef(o);
        return b;
      }).reduce((a, b) -> a || b).orElse(false);
      ((ReferenceCounting) c).freeRef();
      return returnValue;
    } else {
      return c.stream().map(o -> {
        final T remove = inner.remove(o);
        final boolean found = remove != null;
        if (found) RefUtil.freeRef(o);
        return found;
      }).reduce((a, b) -> a || b).orElse(false);
    }
  }

  @Override
  public synchronized final boolean retainAll(@NotNull Collection<?> c) {
    final Object[] toRemove;
    if (c instanceof ReferenceCounting) {
      toRemove = inner.keySet().stream().filter(o -> !c.contains(RefUtil.addRef(o))).toArray();
      ((ReferenceCounting) c).freeRef();
    } else {
      toRemove = inner.keySet().stream().filter(o -> !c.contains(o)).map(RefUtil::addRef).toArray();
    }
    for (Object o : toRemove) {
      inner.remove(o);
      RefUtil.freeRef(o);
    }
    return 0 < toRemove.length;
  }

  @Override
  public final int size() {
    return inner.size();
  }

  @Override
  public final RefStream<T> stream() {
    return new RefStream<T>(inner.keySet().stream());
  }

  @NotNull
  @Override
  public final Object[] toArray() {
    final @NotNull Object[] returnValue = inner.keySet().toArray();
    for (Object x : returnValue) {
      RefUtil.addRef(x);
    }
    return returnValue;
  }

  @NotNull
  @Override
  public final <T1> T1[] toArray(@NotNull T1[] a) {
    final @NotNull T1[] returnValue = inner.keySet().toArray(a);
    for (T1 x : returnValue) {
      RefUtil.addRef(x);
    }
    return returnValue;
  }
}
