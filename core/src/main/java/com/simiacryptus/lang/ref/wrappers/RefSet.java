package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.ReferenceCounting;

import java.util.HashSet;
import java.util.Set;

public class RefSet<T extends ReferenceCounting> extends RefCollection<T> implements Set<T> {
  private final Set<? extends T> inner;

  public RefSet() {
    this(new HashSet<>());
  }

  public RefSet(Set<? extends T> inner) {
    super(inner);
    this.inner = inner;
  }

}
