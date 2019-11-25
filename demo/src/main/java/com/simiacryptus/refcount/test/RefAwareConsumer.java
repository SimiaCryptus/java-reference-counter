package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import java.util.function.Consumer;

public abstract class RefAwareConsumer<T> extends ReferenceCountingBase implements Consumer<T> {
  public @Override void _free() {
    super._free();
  }

  public @Override RefAwareConsumer addRef() {
    return (RefAwareConsumer) super.addRef();
  }

  public static RefAwareConsumer[] addRefs(RefAwareConsumer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefAwareConsumer::addRef)
        .toArray((x) -> new RefAwareConsumer[x]);
  }
}
