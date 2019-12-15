package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;

public abstract class RefAwareConsumer<T> extends ReferenceCountingBase implements java.util.function.Consumer<T> {
  public @Override
  void _free() {
    super._free();
  }
}
