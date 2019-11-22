package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.ReferenceCounting;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

public class RefArrayList<T extends ReferenceCounting> extends RefCollection<T> implements List<T> {
  private final ArrayList<T> inner;

  public RefArrayList() {
    this(new ArrayList<>());
  }

  public RefArrayList(ArrayList<T> inner) {
    super(inner);
    this.inner = inner;
  }

  @Override
  public boolean addAll(int index, @NotNull Collection<? extends T> c) {
    return false;
  }

  @Override
  public T get(int index) {
    return inner.get(index);
  }

  @Override
  public T set(int index, T element) {
    return inner.set(index, element);
  }

  @Override
  public void add(int index, T element) {
    inner.add(index, element);
  }

  @Override
  public T remove(int index) {
    return inner.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return inner.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return inner.lastIndexOf(o);
  }

  @NotNull
  @Override
  public ListIterator<T> listIterator() {
    return inner.listIterator();
  }

  @NotNull
  @Override
  public ListIterator<T> listIterator(int index) {
    return inner.listIterator(index);
  }

  @NotNull
  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    return inner.subList(fromIndex, toIndex);
  }

}
