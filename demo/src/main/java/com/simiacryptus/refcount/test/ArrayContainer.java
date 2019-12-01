package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import java.util.function.Consumer;

public class ArrayContainer extends ReferenceCountingBase {
  public BasicType[] values;

  public ArrayContainer(BasicType... values) {
    this.values = values;
  }

  public @Override void _free() {
    super._free();
  }

  @Override
  public String toString() {
    return "ArrayContainer{" + "values=" + java.util.Arrays.toString(values) + '}';
  }

  public void use() {
    System.out.println(String.format("Increment %s", this));
    java.util.Arrays.stream(this.values).forEach(x -> {
      x.value++;
    });
  }

  public void useClosures1(BasicType right) {
    System.out.println(String.format("Increment %s", this));
    java.util.Arrays.stream(this.values).forEach(x -> {
      x.value += right.value;
    });
  }

  public void useClosures2(BasicType right) {
    System.out.println(String.format("Increment %s", this));
    java.util.Arrays.stream(this.values).forEach(new Consumer<BasicType>() {
      @Override
      public void accept(BasicType x) {
        x.value += right.value;
      }
    });
  }

  public void useClosures3(BasicType right) {
    System.out.println(String.format("Increment %s", this));
    java.util.Arrays.stream(this.values).forEach(new RefAwareConsumer<BasicType>() {
      @Override
      public void accept(BasicType x) {
        x.value += right.value;
      }
    });
  }
}
