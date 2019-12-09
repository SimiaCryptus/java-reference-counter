package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import java.util.function.Consumer;

public abstract @com.simiacryptus.ref.lang.RefAware class RefAwareConsumer<T> extends ReferenceCountingBase
    implements Consumer<T> {
  public @Override void _free() {
    super._free();
  }

  public @Override RefAwareConsumer<T> addRef() {
    return (RefAwareConsumer<T>) super.addRef();
  }

  public static RefAwareConsumer[] addRefs(RefAwareConsumer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefAwareConsumer::addRef)
        .toArray((x) -> new RefAwareConsumer[x]);
  }

  public static RefAwareConsumer[][] addRefs(RefAwareConsumer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefAwareConsumer::addRefs)
        .toArray((x) -> new RefAwareConsumer[x][]);
  }
}
