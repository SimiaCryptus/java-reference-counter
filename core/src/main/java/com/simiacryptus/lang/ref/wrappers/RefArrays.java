package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.ReferenceCounting;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class RefArrays {
  public static <T extends ReferenceCounting> RefStream<T> stream(T[] array) {
    return new RefStream<>(Arrays.stream(array));
  }

  public static <T extends ReferenceCounting> String toString(T[] values) {
    final String result = Arrays.toString(values);
    Arrays.stream(values).forEach(ReferenceCounting::freeRef);
    return result;
  }
}
