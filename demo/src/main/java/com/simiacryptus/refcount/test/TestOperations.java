package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;
import java.util.Random;

public class TestOperations extends ReferenceCountingBase {
  public static final int count = 1;
  public static final Random random = new Random(143892);
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
    testArray(new BasicType[] { datum1 });
    testSet();
    testList();
    testMap();
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

  @NotNull
  private BasicType testBasicType() {
    BasicType datum1 = new BasicType();
    testBasicType(datum1);
    return datum1;
  }

  private BasicType testBasicType(BasicType datum1) {
    System.out.println(String.format("Instantiated %s", datum1));
    for (int i = 0; i < count; i++) {
      datum1.use();
    }
    return datum1;
  }

  private void testList() {
    this.listContainer = new ListContainer();
    System.out.println(String.format("Instantiated %s", listContainer));
    for (int i = 0; i < count; i++) {
      listContainer.test();
    }
  }

  private void testMap() {
    this.mapValuesContainer = new MapValuesContainer();
    System.out.println(String.format("Instantiated %s", mapValuesContainer));
    for (int i = 0; i < count; i++) {
      mapValuesContainer.use();
    }
  }

  private void testSet() {
    this.setContainer = new SetContainer();
    System.out.println(String.format("Instantiated %s", setContainer));
    for (int i = 0; i < count; i++) {
      setContainer.test();
    }
  }

  private void testSimpleContainer() {
    this.simpleContainer = new SimpleContainer();
    System.out.println(String.format("Instantiated %s", simpleContainer));
    for (int i = 0; i < count; i++) {
      simpleContainer.use();
    }
  }

  public @Override void _free() {
    super._free();
  }
}
