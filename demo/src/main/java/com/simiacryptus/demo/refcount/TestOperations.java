package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public @com.simiacryptus.ref.lang.RefAware
class TestOperations extends ReferenceCountingBase {
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
    self.freeRef();
  }

  public static TestOperations[] addRefs(TestOperations[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TestOperations::addRef)
        .toArray((x) -> new TestOperations[x]);
  }

  public static TestOperations[][] addRefs(TestOperations[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TestOperations::addRefs)
        .toArray((x) -> new TestOperations[x][]);
  }

  public @Override
  void _free() {
    if (null != setContainer)
      setContainer.freeRef();
    if (null != mapValuesContainer)
      mapValuesContainer.freeRef();
    if (null != listContainer)
      listContainer.freeRef();
    if (null != arrayContainer)
      arrayContainer.freeRef();
    if (null != simpleContainer)
      simpleContainer.freeRef();
    super._free();
  }

  public @Override
  TestOperations addRef() {
    return (TestOperations) super.addRef();
  }

  private void run() {
    BasicType basicType = testBasicType();
    testSimpleContainer();
    test(basicType);
  }

  private void test(BasicType datum1) {
    testArray(new BasicType[]{datum1});
    testSet();
    testList();
    testMap();
  }

  private void testArray(BasicType[] values) {
    {
      com.simiacryptus.demo.refcount.ArrayContainer temp4869 = new ArrayContainer(
          com.simiacryptus.demo.refcount.BasicType.addRefs(values));
      if (null != this.arrayContainer)
        this.arrayContainer.freeRef();
      this.arrayContainer = temp4869;
    }
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(values);
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
    com.simiacryptus.ref.lang.RefUtil.freeRef(testBasicType(datum1.addRef()));
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
    {
      com.simiacryptus.demo.refcount.ListContainer temp7744 = new ListContainer();
      if (null != this.listContainer)
        this.listContainer.freeRef();
      this.listContainer = temp7744;
    }
    System.out.println(String.format("Instantiated %s", listContainer));
    for (int i = 0; i < count; i++) {
      listContainer.test();
    }
  }

  private void testMap() {
    {
      com.simiacryptus.demo.refcount.MapValuesContainer temp3562 = new MapValuesContainer();
      if (null != this.mapValuesContainer)
        this.mapValuesContainer.freeRef();
      this.mapValuesContainer = temp3562;
    }
    System.out.println(String.format("Instantiated %s", mapValuesContainer));
    for (int i = 0; i < count; i++) {
      mapValuesContainer.use();
    }
  }

  private void testSet() {
    {
      com.simiacryptus.demo.refcount.SetContainer temp2638 = new SetContainer();
      if (null != this.setContainer)
        this.setContainer.freeRef();
      this.setContainer = temp2638;
    }
    System.out.println(String.format("Instantiated %s", setContainer));
    for (int i = 0; i < count; i++) {
      setContainer.test();
    }
  }

  private void testSimpleContainer() {
    {
      com.simiacryptus.demo.refcount.SimpleContainer temp4212 = new SimpleContainer();
      if (null != this.simpleContainer)
        this.simpleContainer.freeRef();
      this.simpleContainer = temp4212;
    }
    System.out.println(String.format("Instantiated %s", simpleContainer));
    for (int i = 0; i < count; i++) {
      simpleContainer.use();
    }
  }
}
