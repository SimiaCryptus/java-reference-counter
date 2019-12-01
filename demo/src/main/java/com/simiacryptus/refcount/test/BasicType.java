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
    return this.label.compareTo(o.label);
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
    return label == basicType.label;
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

  public @Override void _free() {
    super._free();
  }
}
