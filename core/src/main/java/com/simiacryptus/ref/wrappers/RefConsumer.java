package com.simiacryptus.ref.wrappers;

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.ReferenceCounting;

import java.util.function.Consumer;

@RefAware
public interface RefConsumer<T> extends Consumer<T>, ReferenceCounting {
  @Override
  void accept(T t);
}
