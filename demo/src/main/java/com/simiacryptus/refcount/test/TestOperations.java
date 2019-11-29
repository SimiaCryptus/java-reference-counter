package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;
import java.util.Random;

public class TestOperations extends ReferenceCountingBase {
  private SimpleContainer simpleContainer;
  private ArrayContainer arrayContainer;
  private ListContainer listContainer;
  private MapValuesContainer mapValuesContainer;
  private SetContainer setContainer;
  public static final int count = 1;
  public static final Random random = new Random(143892);

  public static void main(String... args) {
    final TestOperations self = new TestOperations();
    self.run();
    self.freeRef();
  }

  private void run() {
    BasicType basicType = testBasicType();
    testSimpleContainer();
    test(basicType.addRef());
    basicType.freeRef();
  }

  private void test(BasicType datum1) {
    testArray(new BasicType[] { datum1.addRef() });
    datum1.freeRef();
    testSet();
    testList();
    testMap();
  }

  private void testArray(BasicType[] values) {
    {
      ArrayContainer temp3170 = new ArrayContainer(com.simiacryptus.refcount.test.BasicType.addRefs(values));
      if (null != this.arrayContainer)
        this.arrayContainer.freeRef();
      this.arrayContainer = temp3170.addRef();
      temp3170.freeRef();
    }
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(values);
    System.out.println(String.format("Instantiated %s", arrayContainer));
    for (int i = 0; i < count; i++) {
      arrayContainer.use();
    }
    arrayContainer.useClosures1(testBasicType());
    arrayContainer.useClosures2(testBasicType());
    arrayContainer.useClosures3(testBasicType());
  }

  private void testMap() {
    {
      MapValuesContainer temp6383 = new MapValuesContainer();
      if (null != this.mapValuesContainer)
        this.mapValuesContainer.freeRef();
      this.mapValuesContainer = temp6383.addRef();
      temp6383.freeRef();
    }
    System.out.println(String.format("Instantiated %s", mapValuesContainer));
    for (int i = 0; i < count; i++) {
      mapValuesContainer.use();
    }
  }

  private void testList() {
    {
      ListContainer temp5556 = new ListContainer();
      if (null != this.listContainer)
        this.listContainer.freeRef();
      this.listContainer = temp5556.addRef();
      temp5556.freeRef();
    }
    System.out.println(String.format("Instantiated %s", listContainer));
    for (int i = 0; i < count; i++) {
      listContainer.test();
    }
  }

  private void testSet() {
    {
      SetContainer temp6039 = new SetContainer();
      if (null != this.setContainer)
        this.setContainer.freeRef();
      this.setContainer = temp6039.addRef();
      temp6039.freeRef();
    }
    System.out.println(String.format("Instantiated %s", setContainer));
    for (int i = 0; i < count; i++) {
      setContainer.test();
    }
  }

  private void testSimpleContainer() {
    {
      SimpleContainer temp4239 = new SimpleContainer();
      if (null != this.simpleContainer)
        this.simpleContainer.freeRef();
      this.simpleContainer = temp4239.addRef();
      temp4239.freeRef();
    }
    System.out.println(String.format("Instantiated %s", simpleContainer));
    for (int i = 0; i < count; i++) {
      simpleContainer.use();
    }
  }

  @NotNull
  private BasicType testBasicType() {
    BasicType datum1 = new BasicType();
    com.simiacryptus.lang.ref.RefUtil.freeRef(testBasicType(datum1.addRef()));
    return datum1;
  }

  private BasicType testBasicType(BasicType datum1) {
    System.out.println(String.format("Instantiated %s", datum1));
    for (int i = 0; i < count; i++) {
      datum1.use();
    }
    return datum1;
  }

  public @Override void _free() {
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

  public @Override TestOperations addRef() {
    return (TestOperations) super.addRef();
  }

  public static TestOperations[] addRefs(TestOperations[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TestOperations::addRef)
        .toArray((x) -> new TestOperations[x]);
  }
}
