package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;
import java.util.Random;

public class TestOperations extends ReferenceCountingBase {
  public static final int count = 5;
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
  }

  public @Override void _free() {
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
        this.arrayContainer = new ArrayContainer(values);
      }
      for (int i = 0; i < count; i++) {
        arrayContainer.test();
      }
      arrayContainer.useClosures1(testBasicType());
      arrayContainer.useClosures2(testBasicType());
      arrayContainer.useClosures3(testBasicType());
    }
  }

  @NotNull
  private BasicType testBasicType() {
    BasicType datum1 = new BasicType();
    testBasicType(datum1);
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
        this.deququeContainer = new DeququeContainer();
      }
      for (int i = 0; i < count; i++) {
        deququeContainer.test();
      }
    }
  }

  private void testList() {
    {
      {
        this.arrayListContainer = new ArrayListContainer();
      }
      for (int i = 0; i < count; i++) {
        arrayListContainer.test();
      }
    }
    {
      {
        this.linkedListContainer = new LinkedListContainer();
      }
      for (int i = 0; i < count; i++) {
        linkedListContainer.test();
      }
    }
  }

  private void testMap() {
    {
      {
        this.hashMapValuesContainer = new HashMapValuesContainer();
      }
      for (int i = 0; i < count; i++) {
        hashMapValuesContainer.test();
      }
    }
    {
      {
        this.linkedHashMapValuesContainer = new LinkedHashMapValuesContainer();
      }
      for (int i = 0; i < count; i++) {
        linkedHashMapValuesContainer.test();
      }
    }
    {
      {
        this.concurrentHashMapValuesContainer = new ConcurrentHashMapValuesContainer();
      }
      for (int i = 0; i < count; i++) {
        concurrentHashMapValuesContainer.test();
      }
    }
  }

  private void testSet() {
    {
      {
        this.hashSetContainer = new HashSetContainer();
      }
      for (int i = 0; i < count; i++) {
        hashSetContainer.test();
      }
    }
    {
      {
        this.treeSetContainer = new TreeSetContainer();
      }
      for (int i = 0; i < count; i++) {
        treeSetContainer.test();
      }
    }
  }

  private void testSimpleContainer() {
    {
      this.simpleContainer = new SimpleContainer();
    }
    for (int i = 0; i < count; i++) {
      simpleContainer.test();
    }
  }
}
