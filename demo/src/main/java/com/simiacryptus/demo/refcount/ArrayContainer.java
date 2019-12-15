package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;

public class ArrayContainer extends ReferenceCountingBase {
  public BasicType[] values;

  public ArrayContainer(BasicType... values) {
    this.values = values;
  }

  public @Override
  void _free() {
    super._free();
  }

  public void test() {
    java.util.Arrays.stream(this.values).forEach(x -> {
      x.setValue(x.getValue() + 1);
    });
  }

  @Override
  public String toString() {
    return "ArrayContainer{" + "values=" + java.util.Arrays.toString(values) + '}';
  }

  public void useClosures1(BasicType right) {
    java.util.Arrays.stream(this.values)
        .forEach((java.util.function.Consumer<? super com.simiacryptus.demo.refcount.BasicType>) x -> {
          x.setValue(x.getValue() + right.getValue());
        });
  }

  public void useClosures2(BasicType right) {
    java.util.Arrays.stream(this.values).forEach(new java.util.function.Consumer<BasicType>() {
      @Override
      public void accept(BasicType x) {
        x.setValue(x.getValue() + right.getValue());
      }
    });
  }

  public void useClosures3(BasicType right) {
    java.util.Arrays.stream(this.values).forEach(new RefAwareConsumer<BasicType>() {
      @Override
      public void accept(BasicType x) {
        x.setValue(x.getValue() + right.getValue());
      }
    });
  }
}
