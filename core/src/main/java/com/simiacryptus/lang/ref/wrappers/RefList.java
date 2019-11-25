package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.RefUtil;
import com.simiacryptus.lang.ref.ReferenceCounting;
import com.simiacryptus.lang.ref.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class RefList<T> extends ReferenceCountingBase implements List<T> {
  private final List<T> inner;

  public RefList() {
    this.inner = new ArrayList<>();
    this.inner.forEach(RefUtil::addRef);
  }

  public RefList(List<T> list) {
    this();
    list.forEach(o -> add(RefUtil.addRef(o)));
    RefUtil.freeRef(list);
  }

  @Override
  protected void _free() {
    clear();
    super._free();
  }

  @Override
  public void add(int index, T element) {
    if (index < size()) RefUtil.freeRef(inner.get(index));
    inner.add(index, element);
  }

  @Override
  public final boolean add(T o) {
    return inner.add(o);
  }

  @Override
  public boolean addAll(int index, @NotNull Collection<? extends T> c) {
    for (int i = index; i < inner.size(); i++) {
      RefUtil.freeRef(inner.get(i));
    }
    return inner.addAll(index, c);
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
    inner.forEach(RefUtil::freeRef);
    inner.clear();
  }

  @Override
  public final boolean contains(Object o) {
    final boolean returnValue = inner.contains(o);
    RefUtil.freeRef(o);
    return returnValue;
  }

  @Override
  public final boolean containsAll(@NotNull Collection<?> c) {
    if (c instanceof ReferenceCounting) {
      final boolean returnValue = c.stream().filter(o -> {
        final boolean b = !inner.contains(o);
        RefUtil.freeRef(o);
        return b;
      }).count() == 0;
      ((ReferenceCounting) c).freeRef();
      return returnValue;
    } else {
      return c.stream().filter(o -> {
        final boolean b = !inner.contains(o);
        RefUtil.freeRef(o);
        return b;
      }).count() == 0;
    }
  }

  @Override
  public T get(int index) {
    return RefUtil.addRef(inner.get(index));
  }

  @Override
  public int indexOf(Object o) {
    final int index = inner.indexOf(o);
    RefUtil.freeRef(o);
    return index;
  }

  @Override
  public final boolean isEmpty() {
    return inner.isEmpty();
  }

  @NotNull
  @Override
  public final Iterator<T> iterator() {
    return new RefIterator<>(inner.iterator());
  }

  @Override
  public int lastIndexOf(Object o) {
    final int index = inner.lastIndexOf(o);
    RefUtil.freeRef(o);
    return index;
  }

  @NotNull
  @Override
  public RefListIterator<T> listIterator() {
    return new RefListIterator<>(inner.listIterator());
  }

  @NotNull
  @Override
  public RefListIterator<T> listIterator(int index) {
    return new RefListIterator<>(inner.listIterator(index));
  }

  @Override
  public T remove(int index) {
    return inner.remove(index);
  }

  @Override
  public final boolean remove(Object o) {
    final int index = inner.indexOf(o);
    if (index > 0) {
      RefUtil.freeRef(inner.remove(index));
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
      toRemove = inner.stream().filter(o -> !c.contains(RefUtil.addRef(o))).toArray();
      ((ReferenceCounting) c).freeRef();
    } else {
      toRemove = inner.stream().filter(o -> !c.contains(o)).toArray();
    }
    for (Object o : toRemove) {
      inner.remove(o);
    }
    return 0 < toRemove.length;
  }

  @Override
  public T set(int index, T element) {
    return inner.set(index, element);
  }

  @Override
  public final int size() {
    return inner.size();
  }

  @Override
  public final RefStream<T> stream() {
    return new RefStream<T>(inner.stream());
  }

  @NotNull
  @Override
  public RefList<T> subList(int fromIndex, int toIndex) {
    return new RefList<T>(inner.subList(fromIndex, toIndex));
  }

  @NotNull
  @Override
  public final Object[] toArray() {
    final @NotNull Object[] returnValue = inner.toArray();
    for (Object x : returnValue) {
      RefUtil.addRef(x);
    }
    return returnValue;
  }

  @NotNull
  @Override
  public final <T1> T1[] toArray(@NotNull T1[] a) {
    final @NotNull T1[] returnValue = inner.toArray(a);
    for (T1 x : returnValue) {
      RefUtil.addRef(x);
    }
    return returnValue;
  }

  public @Override RefList<T> addRef() {
    return (RefList<T>) super.addRef();
  }

  public static <T> RefList<T>[] addRefs(RefList<T>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefList::addRef)
        .toArray((x) -> new RefList[x]);
  }
}
