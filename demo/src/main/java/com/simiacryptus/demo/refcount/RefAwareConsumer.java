package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import java.util.function.Consumer;

public abstract class RefAwareConsumer<T> extends ReferenceCountingBase implements Consumer<T> {
  public @Override void _free() {
    super._free();
  }
}
