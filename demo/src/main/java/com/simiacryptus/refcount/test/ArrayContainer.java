package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import java.util.function.Consumer;

public class ArrayContainer extends ReferenceCountingBase {
  public BasicType[] values;

  public ArrayContainer(BasicType... values) {
    if (null != this.values)
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(this.values);
    this.values = com.simiacryptus.refcount.test.BasicType.addRefs(values);
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(values);
  }

  public void useClosures3(BasicType right) {
    System.out.println(String.format("Increment %s", this));
    com.simiacryptus.lang.ref.wrappers.RefArrays.stream(com.simiacryptus.refcount.test.BasicType.addRefs(this.values))
        .forEach(new RefAwareConsumer<BasicType>() {
          @Override
          public void accept(BasicType x) {
            x.value += right.value;
            x.freeRef();
          }

          public @Override void _free() {
            right.freeRef();
            super._free();
          }

          {
            right.addRef();
          }
        });
    right.freeRef();
  }

  public void useClosures2(BasicType right) {
    System.out.println(String.format("Increment %s", this));
    com.simiacryptus.lang.ref.wrappers.RefArrays.stream(com.simiacryptus.refcount.test.BasicType.addRefs(this.values))
        .forEach(com.simiacryptus.lang.ref.RefUtil.wrapInterface(new Consumer<BasicType>() {
          @Override
          public void accept(BasicType x) {
            x.value += right.value;
            x.freeRef();
          }
        }, right.addRef()));
    right.freeRef();
  }

  public void useClosures1(BasicType right) {
    System.out.println(String.format("Increment %s", this));
    com.simiacryptus.lang.ref.wrappers.RefArrays.stream(com.simiacryptus.refcount.test.BasicType.addRefs(this.values))
        .forEach(com.simiacryptus.lang.ref.RefUtil.wrapInterface(x -> {
          x.value += right.value;
          x.freeRef();
        }, right.addRef()));
    right.freeRef();
  }

  public void use() {
    System.out.println(String.format("Increment %s", this));
    com.simiacryptus.lang.ref.wrappers.RefArrays.stream(com.simiacryptus.refcount.test.BasicType.addRefs(this.values))
        .forEach(x -> {
          x.value++;
          x.freeRef();
        });
  }

  @Override
  public String toString() {
    return "ArrayContainer{" + "values=" + com.simiacryptus.lang.ref.wrappers.RefArrays
        .toString(com.simiacryptus.refcount.test.BasicType.addRefs(values)) + '}';
  }

  public @Override void _free() {
    if (null != values)
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(values);
    super._free();
  }

  public @Override ArrayContainer addRef() {
    return (ArrayContainer) super.addRef();
  }

  public static ArrayContainer[] addRefs(ArrayContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ArrayContainer::addRef)
        .toArray((x) -> new ArrayContainer[x]);
  }
}
