package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import java.util.ArrayList;

public class ListContainer extends ReferenceCountingBase {
  public java.util.ArrayList<BasicType> values = new java.util.ArrayList<>();

  public ListContainer(BasicType... values) {
    for (BasicType value : values) {
      this.values.add(value);
    }
  }

  public void use() {
    System.out.println(String.format("Increment %s", this));
    if (this.values.size() != new java.util.HashSet<>(this.values).size())
      throw new RuntimeException();
    for (int i = 0; i < TestOperations.count; i++) {
      if (!this.values.add(this.values.iterator().next()))
        throw new RuntimeException();
      final java.util.List<BasicType> list = java.util.Arrays.asList(new BasicType());
      this.values.addAll(list);
      if (!this.values.containsAll(list))
        throw new RuntimeException();
      if (!this.values.retainAll(list))
        throw new RuntimeException();
      if (!this.values.addAll(list))
        throw new RuntimeException();
      this.values.removeAll(list);
      if (false) {
        if (this.values.size() != this.values.toArray().length)
          throw new RuntimeException();
      }
      if (this.values.size() != this.values.toArray(new BasicType[] {}).length)
        throw new RuntimeException();
      this.values.stream().forEach(x -> {
        x.value++;
      });
    }
  }

  @Override
  public String toString() {
    return "ListContainer{" + "values=" + values + '}';
  }
}
