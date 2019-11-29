package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.ReferenceCounting;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface RefList<T> extends ReferenceCounting, List<T> {

  public static <T> RefArrayList<T>[] addRefs(@NotNull RefArrayList<T>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefArrayList::addRef)
        .toArray((x) -> new RefArrayList[x]);
  }

  RefList<T> addRef();
}
