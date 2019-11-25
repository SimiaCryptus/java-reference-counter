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
  private int count = 1;

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
    testSet(new BasicType[] { datum1 });
    testList(datum1);
    testMap(datum1);
    testArray(values);
  }

  private void testArray(BasicType[] values) {
    this.arrayContainer = new ArrayContainer(values);
    System.out.println(String.format("Instantiated %s", arrayContainer));
    for (int i = 0; i < count; i++) {
      use(arrayContainer);
    }
    useClosures1(arrayContainer, testBasicType());
    useClosures2(arrayContainer, testBasicType());
    useClosures3(arrayContainer, testBasicType());
  }

  private void testMap(BasicType... values) {
    this.mapValuesContainer = new MapValuesContainer(values);
    System.out.println(String.format("Instantiated %s", mapValuesContainer));
    for (int i = 0; i < count; i++) {
      use(mapValuesContainer);
    }
  }

  private void testList(BasicType... values) {
    this.listContainer = new ListContainer(values);
    System.out.println(String.format("Instantiated %s", listContainer));
    for (int i = 0; i < count; i++) {
      use(listContainer);
    }
  }

  private void testSet(BasicType... values) {
    this.setContainer = new SetContainer(values);
    System.out.println(String.format("Instantiated %s", setContainer));
    for (int i = 0; i < count; i++) {
      use(setContainer);
    }
  }

  private void testSimpleContainer() {
    this.simpleContainer = new SimpleContainer();
    System.out.println(String.format("Instantiated %s", simpleContainer));
    for (int i = 0; i < count; i++) {
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
    for (int i = 0; i < count; i++) {
      use(datum1);
    }
    return datum1;
  }

  private void use(BasicType basicType) {
    System.out.println(String.format("Increment %s", basicType));
    basicType.value++;
  }

  private void use(SimpleContainer simpleContainer) {
    System.out.println(String.format("Increment %s", simpleContainer));
    simpleContainer.value.value++;
  }

  private void use(ArrayContainer arrayContainer) {
    System.out.println(String.format("Increment %s", arrayContainer));
    java.util.Arrays.stream(arrayContainer.values).forEach(x -> {
      x.value++;
    });
  }

  private void use(MapValuesContainer mapValuesContainer) {
    System.out.println(String.format("Increment %s", mapValuesContainer));
    for (int i = 0; i < count; i++) {
      mapValuesContainer.values.values().stream().forEach(x -> {
        x.value++;
      });
    }
  }

  private void use(ListContainer listContainer) {
    System.out.println(String.format("Increment %s", listContainer));
    if (listContainer.values.size() != new java.util.HashSet<>(listContainer.values).size())
      throw new RuntimeException();
    for (int i = 0; i < count; i++) {
      if (!listContainer.values.add(listContainer.values.iterator().next()))
        throw new RuntimeException();
      final java.util.List<BasicType> list = java.util.Arrays.asList(new BasicType());
      listContainer.values.addAll(list);
      if (!listContainer.values.containsAll(list))
        throw new RuntimeException();
      if (!listContainer.values.retainAll(list))
        throw new RuntimeException();
      if (!listContainer.values.addAll(list))
        throw new RuntimeException();
      listContainer.values.removeAll(list);
      if (false) {
        if (listContainer.values.size() != listContainer.values.toArray().length)
          throw new RuntimeException();
        if (listContainer.values.size() != listContainer.values.toArray(new BasicType[] {}).length)
          throw new RuntimeException();
      }
      listContainer.values.stream().forEach(x -> {
        x.value++;
      });
    }
  }

  private void use(SetContainer setContainer) {
    System.out.println(String.format("Increment %s", setContainer));
    if (setContainer.values.size() != new java.util.HashSet<>(setContainer.values).size())
      throw new RuntimeException();
    for (int i = 0; i < count; i++) {
      if (setContainer.values.add(setContainer.values.iterator().next()))
        throw new RuntimeException();
      final java.util.List<BasicType> list = java.util.Arrays.asList(new BasicType());
      setContainer.values.addAll(list);
      if (!setContainer.values.containsAll(list))
        throw new RuntimeException();
      if (!setContainer.values.retainAll(list))
        throw new RuntimeException();
      setContainer.values.removeAll(list);
      if (!setContainer.values.addAll(list))
        throw new RuntimeException();
      if (0 == setContainer.values.size())
        throw new RuntimeException();
      if (false) {
        if (setContainer.values.size() != setContainer.values.toArray().length)
          throw new RuntimeException();
        if (setContainer.values.size() != setContainer.values.toArray(new BasicType[] {}).length)
          throw new RuntimeException();
      }
      setContainer.values.stream().forEach(x -> {
        x.value++;
      });
    }
  }

  private void useClosures1(ArrayContainer left, BasicType right) {
    System.out.println(String.format("Increment %s", left));
    java.util.Arrays.stream(left.values).forEach(x -> {
      x.value += right.value;
    });
  }

  private void useClosures2(ArrayContainer left, BasicType right) {
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

  private void useClosures3(ArrayContainer left, BasicType right) {
    System.out.println(String.format("Increment %s", left));
    java.util.Arrays.stream(left.values).forEach(new RefAwareConsumer<BasicType>() {
      @Override
      public void accept(BasicType x) {
        x.value += right.value;
      }
    });
  }
}
