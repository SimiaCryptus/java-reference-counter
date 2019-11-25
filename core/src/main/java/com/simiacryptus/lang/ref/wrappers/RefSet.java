package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.RefUtil;
import com.simiacryptus.lang.ref.ReferenceCounting;
import com.simiacryptus.lang.ref.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RefSet<T> extends ReferenceCountingBase implements Set<T> {

  private final HashMap<T, T> inner;

  public RefSet() {
    this(new HashMap<>());
  }

  RefSet(HashMap<T, T> inner) {
    if (inner instanceof ReferenceCounting) throw new IllegalArgumentException("inner class cannot be ref-aware");
    this.inner = inner;
    this.inner.keySet().forEach(RefUtil::addRef);
  }

  @Override
  protected void _free() {
    clear();
    super._free();
  }

  public RefSet(Collection<T> values) {
    this();
    for (T value : values) {
      add(value);
    }
    RefUtil.freeRef(values);
  }

  @Override
  public final boolean add(T o) {
    final T replaced = inner.put(o, o);
    if (null != replaced) {
      RefUtil.freeRef(replaced);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public final boolean addAll(@NotNull Collection<? extends T> c) {
    if (c instanceof ReferenceCounting) {
      final boolean returnValue = c.stream().map(this::add).reduce((a, b) -> a || b).orElse(false);
      ((ReferenceCounting) c).freeRef();
      return returnValue;
    } else {
      return c.stream().map(this::add).reduce((a, b) -> a || b).orElse(false);
    }

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
      return c.stream().filter(o -> {
        final boolean b = !inner.containsKey(o);
        RefUtil.freeRef(o);
        return b;
      }).count() == 0;
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
      final boolean returnValue = c.stream().map(this::remove).reduce((a, b) -> a || b).orElse(false);
      ((ReferenceCounting) c).freeRef();
      return returnValue;
    } else {
      return c.stream().map(this::remove).reduce((a, b) -> a || b).orElse(false);
    }
  }

  @Override
  public synchronized final boolean retainAll(@NotNull Collection<?> c) {
    final Object[] toRemove;
    if (c instanceof ReferenceCounting) {
      toRemove = inner.keySet().stream().filter(o -> !c.contains(RefUtil.addRef(o))).toArray();
      ((ReferenceCounting) c).freeRef();
    } else {
      toRemove = inner.keySet().stream().filter(o -> !c.contains(o)).toArray();
    }
    for (Object o : toRemove) {
      inner.remove(o);
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

  public @Override RefSet<T> addRef() {
    return (RefSet<T>) super.addRef();
  }

  public static <T> RefSet<T>[] addRefs(RefSet<T>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefSet::addRef)
        .toArray((x) -> new RefSet[x]);
  }
}
