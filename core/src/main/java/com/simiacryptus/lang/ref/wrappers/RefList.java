package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.ReferenceCounting;

import java.util.List;

public interface RefList<T> extends ReferenceCounting, List<T> {

  RefList<T> addRef();

  public static <T> RefArrayList<T>[] addRefs(RefArrayList<T>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefArrayList::addRef)
        .toArray((x) -> new RefArrayList[x]);
  }
}
