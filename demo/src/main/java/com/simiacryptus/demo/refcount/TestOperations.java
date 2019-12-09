package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;
import java.util.Random;

public @com.simiacryptus.ref.lang.RefAware class TestOperations extends ReferenceCountingBase {
  public static final int count = 1;
  public static final Random random = new Random(143892);
  private SimpleContainer simpleContainer;
  private ArrayContainer arrayContainer;
  private ArrayListContainer arrayListContainer;
  private LinkedListContainer linkedListContainer;
  private DeququeContainer deququeContainer;
  private HashMapValuesContainer hashMapValuesContainer;
  private LinkedHashMapValuesContainer linkedHashMapValuesContainer;
  private ConcurrentHashMapValuesContainer concurrentHashMapValuesContainer;
  private HashSetContainer hashSetContainer;
  private TreeSetContainer treeSetContainer;

  public static void main(String... args) {
    final TestOperations testOperations2424 = new TestOperations();
    testOperations2424.run();
    testOperations2424.freeRef();
  }

  public @Override void _free() {
    if (null != treeSetContainer)
      treeSetContainer.freeRef();
    if (null != hashSetContainer)
      hashSetContainer.freeRef();
    if (null != concurrentHashMapValuesContainer)
      concurrentHashMapValuesContainer.freeRef();
    if (null != linkedHashMapValuesContainer)
      linkedHashMapValuesContainer.freeRef();
    if (null != hashMapValuesContainer)
      hashMapValuesContainer.freeRef();
    if (null != deququeContainer)
      deququeContainer.freeRef();
    if (null != linkedListContainer)
      linkedListContainer.freeRef();
    if (null != arrayListContainer)
      arrayListContainer.freeRef();
    if (null != arrayContainer)
      arrayContainer.freeRef();
    if (null != simpleContainer)
      simpleContainer.freeRef();
    super._free();
  }

  private void run() {
    BasicType basicType = testBasicType();
    testSimpleContainer();
    test(basicType);
  }

  private void test(BasicType datum1) {
    testList();
    testArray(new BasicType[] { datum1 });
    testSet();
    testDeque();
    testMap();
  }

  private void testArray(BasicType[] values) {
    {
      {
        com.simiacryptus.demo.refcount.ArrayContainer temp8505 = new ArrayContainer(
            com.simiacryptus.demo.refcount.BasicType.addRefs(values));
        if (null != this.arrayContainer)
          this.arrayContainer.freeRef();
        this.arrayContainer = temp8505;
      }
      for (int i = 0; i < count; i++) {
        arrayContainer.use();
      }
      arrayContainer.useClosures1(testBasicType());
      arrayContainer.useClosures2(testBasicType());
      arrayContainer.useClosures3(testBasicType());
    }
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(values);
  }

  @NotNull
  private BasicType testBasicType() {
    BasicType datum1 = new BasicType();
    com.simiacryptus.ref.lang.RefUtil.freeRef(testBasicType(datum1.addRef()));
    return datum1;
  }

  private BasicType testBasicType(BasicType datum1) {
    for (int i = 0; i < count; i++) {
      datum1.use();
    }
    return datum1;
  }

  private void testDeque() {
    {
      {
        com.simiacryptus.demo.refcount.DeququeContainer temp3681 = new DeququeContainer();
        if (null != this.deququeContainer)
          this.deququeContainer.freeRef();
        this.deququeContainer = temp3681;
      }
      for (int i = 0; i < count; i++) {
        deququeContainer.test();
      }
    }
  }

  private void testList() {
    {
      {
        com.simiacryptus.demo.refcount.ArrayListContainer temp8826 = new ArrayListContainer();
        if (null != this.arrayListContainer)
          this.arrayListContainer.freeRef();
        this.arrayListContainer = temp8826;
      }
      for (int i = 0; i < count; i++) {
        arrayListContainer.test();
      }
    }
    {
      {
        com.simiacryptus.demo.refcount.LinkedListContainer temp6810 = new LinkedListContainer();
        if (null != this.linkedListContainer)
          this.linkedListContainer.freeRef();
        this.linkedListContainer = temp6810;
      }
      for (int i = 0; i < count; i++) {
        linkedListContainer.test();
      }
    }
  }

  private void testMap() {
    {
      {
        com.simiacryptus.demo.refcount.HashMapValuesContainer temp5182 = new HashMapValuesContainer();
        if (null != this.hashMapValuesContainer)
          this.hashMapValuesContainer.freeRef();
        this.hashMapValuesContainer = temp5182;
      }
      for (int i = 0; i < count; i++) {
        hashMapValuesContainer.use();
      }
    }
    {
      {
        com.simiacryptus.demo.refcount.LinkedHashMapValuesContainer temp6407 = new LinkedHashMapValuesContainer();
        if (null != this.linkedHashMapValuesContainer)
          this.linkedHashMapValuesContainer.freeRef();
        this.linkedHashMapValuesContainer = temp6407;
      }
      for (int i = 0; i < count; i++) {
        linkedHashMapValuesContainer.use();
      }
    }
    {
      {
        com.simiacryptus.demo.refcount.ConcurrentHashMapValuesContainer temp1795 = new ConcurrentHashMapValuesContainer();
        if (null != this.concurrentHashMapValuesContainer)
          this.concurrentHashMapValuesContainer.freeRef();
        this.concurrentHashMapValuesContainer = temp1795;
      }
      for (int i = 0; i < count; i++) {
        concurrentHashMapValuesContainer.use();
      }
    }
  }

  private void testSet() {
    {
      {
        com.simiacryptus.demo.refcount.HashSetContainer temp2525 = new HashSetContainer();
        if (null != this.hashSetContainer)
          this.hashSetContainer.freeRef();
        this.hashSetContainer = temp2525;
      }
      for (int i = 0; i < count; i++) {
        hashSetContainer.test();
      }
    }
    {
      {
        com.simiacryptus.demo.refcount.TreeSetContainer temp3009 = new TreeSetContainer();
        if (null != this.treeSetContainer)
          this.treeSetContainer.freeRef();
        this.treeSetContainer = temp3009;
      }
      for (int i = 0; i < count; i++) {
        treeSetContainer.test();
      }
    }
  }

  private void testSimpleContainer() {
    {
      com.simiacryptus.demo.refcount.SimpleContainer temp1234 = new SimpleContainer();
      if (null != this.simpleContainer)
        this.simpleContainer.freeRef();
      this.simpleContainer = temp1234;
    }
    for (int i = 0; i < count; i++) {
      simpleContainer.use();
    }
  }

  public @Override TestOperations addRef() {
    return (TestOperations) super.addRef();
  }

  public static TestOperations[] addRefs(TestOperations[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TestOperations::addRef)
        .toArray((x) -> new TestOperations[x]);
  }

  public static TestOperations[][] addRefs(TestOperations[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TestOperations::addRefs)
        .toArray((x) -> new TestOperations[x][]);
  }
}
