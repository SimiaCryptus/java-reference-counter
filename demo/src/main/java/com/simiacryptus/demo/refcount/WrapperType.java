package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

public class WrapperType<T extends ReferenceCounting> extends ReferenceCountingBase {
  private T inner;

  public WrapperType(T inner) {
    this.setInner(inner);
  }

  public void _free() {
  }

  public T getInner() {
    return inner;
  }

  public WrapperType<T> setInner(T inner) {
    this.inner = inner;
    return this;
  }
}
