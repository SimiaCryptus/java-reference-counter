package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;

public class BasicType extends ReferenceCountingBase implements Comparable<BasicType> {
  public static final boolean BUG_WORKAROUND = true;
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

  @Override
  public int compareTo(@NotNull BasicType o) {
    int temp1475 = this.label.compareTo(o.label);
    o.freeRef();
    return temp1475;
  }

  public void use() {
    System.out.println(String.format("Increment %s", this));
    this.value++;
  }

  @Override
  public String toString() {
    return "BasicType{" + "values=" + value + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (BUG_WORKAROUND)
      return super.equals(o);
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    BasicType basicType = (BasicType) o;
    boolean temp3533 = label == basicType.label;
    basicType.freeRef();
    return temp3533;
  }

  @Override
  public int hashCode() {
    if (BUG_WORKAROUND)
      return super.hashCode();
    return label.hashCode();
  }

  public @Override void _free() {
    super._free();
  }

  public @Override BasicType addRef() {
    return (BasicType) super.addRef();
  }

  public static BasicType[] addRefs(BasicType[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(BasicType::addRef)
        .toArray((x) -> new BasicType[x]);
  }
}
