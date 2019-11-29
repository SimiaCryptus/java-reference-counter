package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.RefUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class RefArrays {
  public static <T> RefStream<T> stream(@NotNull T[] array) {
    return new RefStream<>(Arrays.stream(array).onClose(() -> {
      Arrays.stream(array).forEach(RefUtil::freeRef);
    }));
  }

  @NotNull
  public static <T> String toString(@NotNull T[] values) {
    final String result = Arrays.toString(values);
    Arrays.stream(values).forEach(RefUtil::freeRef);
    return result;
  }

  @NotNull
  public static <T> RefList<T> asList(@NotNull T... items) {
    final RefArrayList<T> ts = new RefArrayList<>(Arrays.asList(items));
    for (T item : items) {
      RefUtil.freeRef(item);
    }
    return ts;
  }
}
