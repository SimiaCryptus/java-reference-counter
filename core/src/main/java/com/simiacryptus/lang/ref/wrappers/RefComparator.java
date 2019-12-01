package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.RefUtil;

import java.util.Comparator;
import java.util.function.Function;

public class RefComparator {

  public static <T extends Comparable<T>> Comparator<? super T> naturalOrder() {
    return (a, b) -> {
      final int result = a.compareTo(RefUtil.addRef(b));
      RefUtil.freeRef(a);
      RefUtil.freeRef(b);
      return result;
    };
  }

  public static <T, U extends Comparable<U>> Comparator<? super T> comparing(Function<T, U> fn) {
    return (a, b) -> {
      return fn.apply(a).compareTo(RefUtil.addRef(fn.apply(b)));
    };
  }
}
