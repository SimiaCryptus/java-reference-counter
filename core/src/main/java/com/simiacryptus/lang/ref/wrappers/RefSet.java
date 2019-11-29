package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.ReferenceCounting;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface RefSet<T> extends ReferenceCounting, Set<T> {

  public static <T> RefSet<T>[] addRefs(@NotNull RefSet<T>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefSet::addRef)
        .toArray((x) -> new RefSet[x]);
  }

  RefSet<T> addRef();
}
