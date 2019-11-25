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
  public static final int count = 1;

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
      arrayContainer.use();
    }
    arrayContainer.useClosures1(testBasicType());
    arrayContainer.useClosures2(testBasicType());
    arrayContainer.useClosures3(testBasicType());
  }

  private void testMap(BasicType... values) {
    this.mapValuesContainer = new MapValuesContainer(values);
    System.out.println(String.format("Instantiated %s", mapValuesContainer));
    for (int i = 0; i < count; i++) {
      mapValuesContainer.use();
    }
  }

  private void testList(BasicType... values) {
    this.listContainer = new ListContainer(values);
    System.out.println(String.format("Instantiated %s", listContainer));
    for (int i = 0; i < count; i++) {
      listContainer.use();
    }
  }

  private void testSet(BasicType... values) {
    this.setContainer = new SetContainer(values);
    System.out.println(String.format("Instantiated %s", setContainer));
    for (int i = 0; i < count; i++) {
      setContainer.use();
    }
  }

  private void testSimpleContainer() {
    this.simpleContainer = new SimpleContainer();
    System.out.println(String.format("Instantiated %s", simpleContainer));
    for (int i = 0; i < count; i++) {
      simpleContainer.use();
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
      datum1.use();
    }
    return datum1;
  }
}
