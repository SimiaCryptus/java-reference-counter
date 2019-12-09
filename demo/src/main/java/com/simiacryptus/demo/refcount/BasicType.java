package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;

public @com.simiacryptus.ref.lang.RefAware class BasicType extends ReferenceCountingBase
    implements Comparable<BasicType> {
  private final String label;
  private final double doubleLabel;
  public int value;

  public BasicType() {
    this(Double.toString(TestOperations.random.nextDouble()));
  }

  public BasicType(String label) {
    this.value = 1;
    this.doubleLabel = TestOperations.random.nextDouble();
    this.label = label;
  }

  public @Override void _free() {
    super._free();
  }

  @Override
  public int compareTo(@NotNull BasicType o) {
    int temp4616 = this.label.compareTo(o.label);
    o.freeRef();
    return temp4616;
  }

  @RefIgnore
  @Override
  public boolean equals(Object o) {
    if (o == null)
      return false;
    if (!(o instanceof BasicType))
      return false;
    BasicType basicType = (BasicType) o;
    if (this == basicType) {
      return true;
    }
    return label == basicType.label;
  }

  @RefIgnore
  @Override
  public int hashCode() {
    return label.hashCode();
  }

  @Override
  public String toString() {
    return "BasicType{" + "values=" + value + '}';
  }

  public void use() {
    this.value++;
  }

  public @Override BasicType addRef() {
    return (BasicType) super.addRef();
  }

  public static BasicType[] addRefs(BasicType[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(BasicType::addRef)
        .toArray((x) -> new BasicType[x]);
  }

  public static BasicType[][] addRefs(BasicType[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(BasicType::addRefs)
        .toArray((x) -> new BasicType[x][]);
  }
}
