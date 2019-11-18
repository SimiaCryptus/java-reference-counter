package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCounting;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

public class ConsumerClass1 {
  private DataType2 datum2;
  private DataType3 datum3;

  public static void main(String... args) {
    new ConsumerClass1().run();
  }

  private void run() {
    DataType1 datum1 = test1();
    test2();
    test3(datum1.addRef());
    datum1.freeRef();
  }

  private void test3(DataType1 datum1) {
    final DataType1[] values = { datum1.addRef() };
    test3(com.simiacryptus.refcount.test.DataType1.addRefs(values));
    com.simiacryptus.refcount.test.DataType1.freeRefs(values);
    test3(new DataType1[] { datum1.addRef() });
    datum1.freeRef();
  }

  private void test3(DataType1[] values) {
    {
      DataType3 temp1574 = new DataType3(com.simiacryptus.refcount.test.DataType1.addRefs(values));
      if (null != this.datum3)
        this.datum3.freeRef();
      this.datum3 = temp1574.addRef();
      temp1574.freeRef();
    }
    com.simiacryptus.refcount.test.DataType1.freeRefs(values);
    System.out.println(String.format("Instantiated %s", datum3));
    for (int i = 0; i < 10; i++) {
      doSomething(datum3.addRef());
    }
  }

  private void test2() {
    {
      DataType2 temp4883 = new DataType2();
      if (null != this.datum2)
        this.datum2.freeRef();
      this.datum2 = temp4883.addRef();
      temp4883.freeRef();
    }
    System.out.println(String.format("Instantiated %s", datum2));
    for (int i = 0; i < 10; i++) {
      doSomething(datum2.addRef());
    }
  }

  @NotNull
  private DataType1 test1() {
    DataType1 datum1 = new DataType1();
    DataType1 temp4304 = test1a(datum1.addRef());
    datum1.freeRef();
    return temp4304;
  }

  private DataType1 test1a(DataType1 datum1) {
    System.out.println(String.format("Instantiated %s", datum1));
    for (int i = 0; i < 10; i++) {
      doSomething(datum1.addRef());
    }
    return datum1;
  }

  private static void doSomething(DataType1 obj) {
    System.out.println(String.format("Increment %s", obj));
    obj.value++;
    obj.freeRef();
  }

  private static void doSomething(DataType2 obj) {
    System.out.println(String.format("Increment %s", obj));
    obj.value.value++;
    obj.freeRef();
  }

  private static void doSomething(DataType3 obj) {
    System.out.println(String.format("Increment %s", obj));
    Arrays.stream(obj.values).forEach(x -> {
      x.value++;
      x.freeRef();
    });
    obj.freeRef();
  }

  private static void useClosures(DataType3 left, DataType1 right) {
    System.out.println(String.format("Increment %s", left));
    Arrays.stream(left.values).forEach(ReferenceCounting.wrapInterface(x -> {
      x.value += right.value;
      x.freeRef();
    }, right.addRef()));
    right.freeRef();
    left.freeRef();
  }

  private static void useClosures2(DataType3 left, DataType1 right) {
    System.out.println(String.format("Increment %s", left));
    Arrays.stream(left.values).forEach(ReferenceCounting.wrapInterface(x -> {
      x.value += right.value;
      x.freeRef();
    }, right.addRef()));
    right.freeRef();
    left.freeRef();
  }
}
