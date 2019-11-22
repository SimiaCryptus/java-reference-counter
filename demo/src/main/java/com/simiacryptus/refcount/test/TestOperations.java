package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

public class TestOperations extends ReferenceCountingBase {
  private SimpleContainer simpleContainer;
  private ArrayContainer arrayContainer;
  private ListContainer listContainer;
  private MapValuesContainer mapValuesContainer;
  private SetContainer setContainer;

  public static void main(String... args) {
    final TestOperations self = new TestOperations();
    self.run();
  }

  private void run() {
    BasicType basicType = testBasicType();
    testSimpleContainer();
    test(basicType);
  }

  private void test(BasicType datum1) {
    final BasicType[] values = { datum1 };
    testArray(values);
    testSet(new BasicType[] { datum1 });
    testList(datum1);
    testMap(datum1);
  }

  private void testArray(BasicType[] values) {
    this.arrayContainer = new ArrayContainer(values);
    System.out.println(String.format("Instantiated %s", arrayContainer));
    for (int i = 0; i < 10; i++) {
      use(arrayContainer);
    }
    useClosures1(arrayContainer, testBasicType());
    useClosures2(arrayContainer, testBasicType());
    useClosures3(arrayContainer, testBasicType());
  }

  private void testMap(BasicType... values) {
    this.mapValuesContainer = new MapValuesContainer(values);
    System.out.println(String.format("Instantiated %s", mapValuesContainer));
    for (int i = 0; i < 10; i++) {
      use(mapValuesContainer);
    }
  }

  private void testList(BasicType... values) {
    this.listContainer = new ListContainer(values);
    System.out.println(String.format("Instantiated %s", listContainer));
    for (int i = 0; i < 10; i++) {
      use(listContainer);
    }
  }

  private void testSet(BasicType... values) {
    this.setContainer = new SetContainer(values);
    System.out.println(String.format("Instantiated %s", setContainer));
    for (int i = 0; i < 10; i++) {
      use(setContainer);
    }
  }

  private void testSimpleContainer() {
    this.simpleContainer = new SimpleContainer();
    System.out.println(String.format("Instantiated %s", simpleContainer));
    for (int i = 0; i < 10; i++) {
      use(simpleContainer);
    }
  }

  @NotNull
  private BasicType testBasicType() {
    BasicType datum1 = new BasicType();
    return testBasicType(datum1);
  }

  private BasicType testBasicType(BasicType datum1) {
    System.out.println(String.format("Instantiated %s", datum1));
    for (int i = 0; i < 10; i++) {
      use(datum1);
    }
    return datum1;
  }

  private static void use(BasicType obj) {
    System.out.println(String.format("Increment %s", obj));
    obj.value++;
  }

  private static void use(SimpleContainer obj) {
    System.out.println(String.format("Increment %s", obj));
    obj.value.value++;
  }

  private static void use(ArrayContainer obj) {
    System.out.println(String.format("Increment %s", obj));
    java.util.Arrays.stream(obj.values).forEach(x -> {
      x.value++;
    });
  }

  private static void use(MapValuesContainer obj) {
    System.out.println(String.format("Increment %s", obj));
    for (int i = 0; i < 10; i++)
      obj.values.values().stream().forEach(x -> {
        x.value++;
      });
  }

  private static void use(ListContainer obj) {
    System.out.println(String.format("Increment %s", obj));
    for (int i = 0; i < 10; i++)
      obj.values.stream().forEach(x -> {
        x.value++;
      });
  }

  private static void use(SetContainer obj) {
    System.out.println(String.format("Increment %s", obj));
    for (int i = 0; i < 10; i++)
      obj.values.stream().forEach(x -> {
        x.value++;
      });
  }

  private static void useClosures1(ArrayContainer left, BasicType right) {
    System.out.println(String.format("Increment %s", left));
    java.util.Arrays.stream(left.values).forEach(x -> {
      x.value += right.value;
    });
  }

  private static void useClosures2(ArrayContainer left, BasicType right) {
    System.out.println(String.format("Increment %s", left));
    java.util.Arrays.stream(left.values).forEach(new Consumer<BasicType>() {
      @Override
      public void accept(BasicType x) {
        x.value += right.value;
      }
    });
  }

  public static abstract class RefAwareConsumer<T> extends ReferenceCountingBase implements Consumer<T> {
  }

  private static void useClosures3(ArrayContainer left, BasicType right) {
    System.out.println(String.format("Increment %s", left));
    java.util.Arrays.stream(left.values).forEach(new RefAwareConsumer<BasicType>() {
      @Override
      public void accept(BasicType x) {
        x.value += right.value;
      }
    });
  }
}
