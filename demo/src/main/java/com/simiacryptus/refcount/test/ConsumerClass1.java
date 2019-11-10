package com.simiacryptus.refcount.test;

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
    test3(datum1);
  }

  private void test3(DataType1 datum1) {
    final DataType1[] values = { datum1 };
    test3(values);
    test3(new DataType1[] { datum1 });
  }

  private void test3(DataType1[] values) {
    this.datum3 = new DataType3(values);
    System.out.println(String.format("Instantiated %s", datum3));
    for (int i = 0; i < 10; i++) {
      doSomething(datum3);
    }
  }

  private void test2() {
    this.datum2 = new DataType2();
    System.out.println(String.format("Instantiated %s", datum2));
    for (int i = 0; i < 10; i++) {
      doSomething(datum2);
    }
  }

  @NotNull
  private DataType1 test1() {
    DataType1 datum1 = new DataType1();
    return test1a(datum1);
  }

  private DataType1 test1a(DataType1 datum1) {
    System.out.println(String.format("Instantiated %s", datum1));
    for (int i = 0; i < 10; i++) {
      doSomething(datum1);
    }
    return datum1;
  }

  private static void doSomething(DataType1 obj) {
    System.out.println(String.format("Increment %s", obj));
    obj.value++;
  }

  private static void doSomething(DataType2 obj) {
    System.out.println(String.format("Increment %s", obj));
    obj.value.value++;
  }

  private static void doSomething(DataType3 obj) {
    System.out.println(String.format("Increment %s", obj));
    Arrays.stream(obj.values).forEach(x -> x.value++);
  }
}
