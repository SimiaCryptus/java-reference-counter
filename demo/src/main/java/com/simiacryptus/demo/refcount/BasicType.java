package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;

public @com.simiacryptus.ref.lang.RefAware
class BasicType extends ReferenceCountingBase
    implements Comparable<BasicType> {
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

  public static BasicType[] addRefs(BasicType[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(BasicType::addRef)
        .toArray((x) -> new BasicType[x]);
  }

  public static BasicType[][] addRefs(BasicType[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(BasicType::addRefs)
        .toArray((x) -> new BasicType[x][]);
  }

  public @Override
  void _free() {
    super._free();
  }

  public @Override
  BasicType addRef() {
    return (BasicType) super.addRef();
  }

  @Override
  public int compareTo(@NotNull BasicType o) {
    int temp2684 = this.label.compareTo(o.label);
    o.freeRef();
    return temp2684;
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
    boolean temp6851 = label == basicType.label;
    basicType.freeRef();
    return temp6851;
  }

  @Override
  public int hashCode() {
    if (BUG_WORKAROUND)
      return super.hashCode();
    return label.hashCode();
  }

  @Override
  public String toString() {
    return "BasicType{" + "values=" + value + '}';
  }

  public void use() {
    System.out.println(String.format("Increment %s", this));
    this.value++;
  }
}
