package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.RefUtil;
import com.simiacryptus.lang.ref.ReferenceCounting;

import java.util.Arrays;
import java.util.stream.Stream;

public class RefArrays {
  public static <T> RefStream<T> stream(T[] array) {
    return new RefStream<>(Arrays.stream(array).onClose(()->{
      Arrays.stream(array).forEach(RefUtil::freeRef);
    }));
  }

  public static <T> String toString(T[] values) {
    final String result = Arrays.toString(values);
    Arrays.stream(values).forEach(RefUtil::freeRef);
    return result;
  }
}
