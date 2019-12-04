package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;

import java.util.function.Consumer;

public @com.simiacryptus.ref.lang.RefAware
class ArrayContainer extends ReferenceCountingBase {
  public BasicType[] values;

  public ArrayContainer(BasicType... values) {
    this.values = com.simiacryptus.demo.refcount.BasicType.addRefs(values);
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(values);
  }

  public static ArrayContainer[] addRefs(ArrayContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ArrayContainer::addRef)
        .toArray((x) -> new ArrayContainer[x]);
  }

  public static ArrayContainer[][] addRefs(ArrayContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ArrayContainer::addRefs)
        .toArray((x) -> new ArrayContainer[x][]);
  }

  public @Override
  void _free() {
    if (null != values)
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(values);
    super._free();
  }

  public @Override
  ArrayContainer addRef() {
    return (ArrayContainer) super.addRef();
  }

  @Override
  public String toString() {
    return "ArrayContainer{" + "values="
        + com.simiacryptus.ref.wrappers.RefArrays.toString(com.simiacryptus.demo.refcount.BasicType.addRefs(values))
        + '}';
  }

  public void use() {
    System.out.println(String.format("Increment %s", this));
    com.simiacryptus.ref.wrappers.RefArrays.stream(com.simiacryptus.demo.refcount.BasicType.addRefs(this.values))
        .forEach(x -> {
          x.value++;
          x.freeRef();
        });
  }

  public void useClosures1(BasicType right) {
    System.out.println(String.format("Increment %s", this));
    com.simiacryptus.ref.wrappers.RefArrays.stream(com.simiacryptus.demo.refcount.BasicType.addRefs(this.values))
        .forEach(com.simiacryptus.ref.lang.RefUtil
            .wrapInterface((java.util.function.Consumer<? super com.simiacryptus.demo.refcount.BasicType>) x -> {
              x.value += right.value;
              x.freeRef();
            }, right));
  }

  public void useClosures2(BasicType right) {
    System.out.println(String.format("Increment %s", this));
    com.simiacryptus.ref.wrappers.RefArrays.stream(com.simiacryptus.demo.refcount.BasicType.addRefs(this.values))
        .forEach(com.simiacryptus.ref.lang.RefUtil.wrapInterface(new Consumer<BasicType>() {
          @Override
          public void accept(BasicType x) {
            x.value += right.value;
            x.freeRef();
          }
        }, right));
  }

  public void useClosures3(BasicType right) {
    System.out.println(String.format("Increment %s", this));
    com.simiacryptus.ref.wrappers.RefArrays.stream(com.simiacryptus.demo.refcount.BasicType.addRefs(this.values))
        .forEach(new RefAwareConsumer<BasicType>() {
          {
            right.addRef();
          }

          public @Override
          void _free() {
            right.freeRef();
            super._free();
          }

          @Override
          public void accept(BasicType x) {
            x.value += right.value;
            x.freeRef();
          }
        });
    right.freeRef();
  }
}
