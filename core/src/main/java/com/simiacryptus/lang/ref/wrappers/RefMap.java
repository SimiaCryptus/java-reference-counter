package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.ReferenceCounting;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface RefMap<K, V> extends ReferenceCounting, Map<K, V> {

  public static <K, V> RefMap<K, V>[] addRefs(@NotNull RefMap<K, V>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefMap::addRef)
        .toArray((x) -> new RefMap[x]);
  }

  RefMap<K, V> addRef();
}
