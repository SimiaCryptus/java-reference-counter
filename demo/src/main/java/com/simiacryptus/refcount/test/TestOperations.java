package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;

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
    self.freeRef();
  }

  private void run() {
    BasicType basicType = testBasicType();
    testSimpleContainer();
    test(basicType.addRef());
    basicType.freeRef();
  }

  private void test(BasicType datum1) {
    final BasicType[] values = { datum1.addRef() };
    testSet(new BasicType[] { datum1.addRef() });
    testList(datum1.addRef());
    testMap(datum1.addRef());
    datum1.freeRef();
    testArray(com.simiacryptus.refcount.test.BasicType.addRefs(values));
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(values);
  }

  private void testArray(BasicType[] values) {
    {
      ArrayContainer temp3707 = new ArrayContainer(com.simiacryptus.refcount.test.BasicType.addRefs(values));
      if (null != this.arrayContainer)
        this.arrayContainer.freeRef();
      this.arrayContainer = temp3707.addRef();
      temp3707.freeRef();
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

  private void testMap(BasicType... values) {
    {
      MapValuesContainer temp5068 = new MapValuesContainer(com.simiacryptus.refcount.test.BasicType.addRefs(values));
      if (null != this.mapValuesContainer)
        this.mapValuesContainer.freeRef();
      this.mapValuesContainer = temp5068.addRef();
      temp5068.freeRef();
    }
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(values);
    System.out.println(String.format("Instantiated %s", mapValuesContainer));
    for (int i = 0; i < count; i++) {
      mapValuesContainer.use();
    }
  }

  private void testList(BasicType... values) {
    {
      ListContainer temp3964 = new ListContainer(com.simiacryptus.refcount.test.BasicType.addRefs(values));
      if (null != this.listContainer)
        this.listContainer.freeRef();
      this.listContainer = temp3964.addRef();
      temp3964.freeRef();
    }
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(values);
    System.out.println(String.format("Instantiated %s", listContainer));
    for (int i = 0; i < count; i++) {
      listContainer.use();
    }
  }

  private void testSet(BasicType... values) {
    {
      SetContainer temp8146 = new SetContainer(com.simiacryptus.refcount.test.BasicType.addRefs(values));
      if (null != this.setContainer)
        this.setContainer.freeRef();
      this.setContainer = temp8146.addRef();
      temp8146.freeRef();
    }
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(values);
    System.out.println(String.format("Instantiated %s", setContainer));
    for (int i = 0; i < count; i++) {
      setContainer.test();
    }
  }

  private void testSimpleContainer() {
    {
      SimpleContainer temp7817 = new SimpleContainer();
      if (null != this.simpleContainer)
        this.simpleContainer.freeRef();
      this.simpleContainer = temp7817.addRef();
      temp7817.freeRef();
    }
    System.out.println(String.format("Instantiated %s", simpleContainer));
    for (int i = 0; i < count; i++) {
      simpleContainer.use();
    }
  }

  @NotNull
  private BasicType testBasicType() {
    BasicType datum1 = new BasicType();
    BasicType temp1767 = testBasicType(datum1.addRef());
    datum1.freeRef();
    return temp1767;
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
