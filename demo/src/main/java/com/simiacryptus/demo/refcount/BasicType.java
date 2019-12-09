package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;

public class BasicType extends ReferenceCountingBase implements Comparable<BasicType> {
  private final String label;
  private int value;

  public BasicType() {
    this(Long.toHexString(TestOperations.random.nextLong()));
  }

  public BasicType(String label) {
    this.setValue(1);
    this.label = label;
  }

  public @Override void _free() {
    super._free();
  }

  @Override
  public int compareTo(@NotNull BasicType o) {
    return this.label.compareTo(o.label);
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

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

  @RefIgnore
  @Override
  public int hashCode() {
    return label.hashCode();
  }

  @Override
  public String toString() {
    return "BasicType{" + "values=" + getValue() + '}';
  }

  public void use() {
    this.setValue(this.getValue() + 1);
  }
}
