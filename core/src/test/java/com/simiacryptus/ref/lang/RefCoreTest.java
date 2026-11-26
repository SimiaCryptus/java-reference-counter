package com.simiacryptus.ref.lang;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class RefCoreTest {

  private static class TestRef extends ReferenceCountingBase {
    final AtomicInteger freeCount = new AtomicInteger(0);

    @Override
    protected void _free() {
      freeCount.incrementAndGet();
      super._free();
    }
  }

  @Test
  public void testReferenceCountingLifecycle() {
    TestRef obj = new TestRef();
    obj.watch();
    assertEquals(1, obj.currentRefCount());
    assertFalse(obj.isFreed());
    assertFalse(obj.isDetached());
    assertTrue(obj.assertAlive());
    assertThrows(LifecycleException.class, obj::assertFreed);

    assertSame(obj, obj.addRef());
    assertEquals(2, obj.currentRefCount());
    assertTrue(obj.tryAddRef());
    assertEquals(3, obj.currentRefCount());

    String header = obj.referenceHeader();
    assertTrue(header.contains("TestRef"));
    CharSequence report = ReferenceCountingBase.referenceReport(obj, true);
    assertTrue(report.toString().contains("TestRef"));

    assertEquals(2, obj.freeRef());
    assertEquals(1, obj.freeRef());
    assertEquals(0, obj.freeRef());
    assertTrue(obj.isFreed());
    assertEquals(1, obj.freeCount.get());
    assertTrue(obj.assertFreed());

    // After free:
    assertEquals(0, obj.freeRef()); // idempotent once freed
    assertFalse(obj.tryAddRef());
    assertThrows(IllegalStateException.class, obj::addRef);
    assertThrows(LifecycleException.class, obj::assertAlive);
    assertTrue(obj.referenceReport(true, true, true).contains("freed by"));
  }

  @Test
  public void testReferenceCountingDetachedAndStaticHelpers() {
    TestRef obj = new TestRef();
    obj.detach();
    assertTrue(obj.isDetached());
    assertEquals(0, obj.freeRef());
    assertFalse(obj.isFreed());
    assertEquals(0, obj.freeCount.get());

    StackTraceElement e1 = new StackTraceElement("C1", "m1", "C1.java", 1);
    StackTraceElement e2 = new StackTraceElement("C2", "m2", "C2.java", 2);
    StackTraceElement e3 = new StackTraceElement("C3", "m3", "C3.java", 3);

    List<StackTraceElement> rev1 = ReferenceCountingBase.reverseCopy(new StackTraceElement[]{e1, e2, e3});
    assertEquals(Arrays.asList(e3, e2, e1), rev1);
    assertEquals(Arrays.asList(e1, e2, e3), ReferenceCountingBase.reverseCopy(rev1));
    assertTrue(ReferenceCountingBase.reverseCopy((List<StackTraceElement>) null).isEmpty());

    List<StackTraceElement> rev2 = Arrays.asList(e3, e2);
    List<StackTraceElement> prefix = ReferenceCountingBase.findCommonPrefix(Arrays.asList(rev1, rev2));
    assertEquals(Arrays.asList(e3, e2), prefix);
    assertNull(ReferenceCountingBase.findCommonPrefix(Collections.emptyList()));

    StackTraceElement[] trimmed = ReferenceCountingBase.removeSuffix(new StackTraceElement[]{e1, e2, e3}, Collections.singletonList(e3));
    assertEquals(2, trimmed.length);
  }

  @Test
  public void testRefLazyVal() {
    TestRef inner = new TestRef();
    AtomicInteger calls = new AtomicInteger(0);
    RefLazyVal<TestRef> lazy = RefLazyVal.wrap(() -> {
      calls.incrementAndGet();
      return inner;
    });

    TestRef first = lazy.get();
    TestRef second = lazy.get();
    assertSame(inner, first);
    assertSame(inner, second);
    assertEquals(1, calls.get());
    assertEquals(3, inner.currentRefCount()); // 1 held by lazy + 2 returned

    first.freeRef();
    second.freeRef();
    assertFalse(inner.isFreed());

    lazy.freeRef();
    assertTrue(inner.isFreed());
  }

  @Test
  public void testReferenceWrapper() {
    TestRef obj = new TestRef();
    ReferenceWrapper<TestRef> wrapper = new ReferenceWrapper<>(obj, ReferenceCounting::freeRef);
    assertSame(obj, wrapper.peek());
    assertFalse(obj.isFreed());

    TestRef unwrapped = wrapper.unwrap();
    assertSame(obj, unwrapped);
    assertThrows(IllegalStateException.class, wrapper::unwrap);
    unwrapped.freeRef();
    assertTrue(obj.isFreed());

    TestRef obj2 = new TestRef();
    ReferenceWrapper<TestRef> wrapper2 = new ReferenceWrapper<>(obj2, ReferenceCounting::freeRef);
    wrapper2.destroy();
    assertTrue(obj2.isFreed());
    wrapper2.destroy(); // no-op when already destroyed
  }

  @Test
  public void testRecycleBinAndPersistanceModes() {
    for (PersistanceMode mode : PersistanceMode.values()) {
      assertNotNull(mode.wrap("value"));
    }

    AtomicInteger frees = new AtomicInteger(0);
    RecycleBin<double[]> bin = new RecycleBin<double[]>() {
      @Override
      public double[] create(long length) {
        return new double[(int) length];
      }

      @Override
      public void reset(double[] data, long size) {
        Arrays.fill(data, 0.0);
      }

      @Override
      protected void free(double[] obj) {
        frees.incrementAndGet();
      }
    };
    bin.setPersistanceMode(PersistanceMode.STRONG)
        .setMinLengthPerBuffer(4)
        .setMaxLengthPerBuffer(1024)
        .setMaxItemsPerBuffer(2)
        .setPurgeFreq(60)
        .setProfiling(0);

    assertEquals(PersistanceMode.STRONG, bin.getPersistanceMode());
    assertEquals(4, bin.getMinLengthPerBuffer());
    assertEquals(1024.0, bin.getMaxLengthPerBuffer());
    assertEquals(2, bin.getMaxItemsPerBuffer());
    assertEquals(60, bin.getPurgeFreq());
    assertTrue(bin.isProfiling(8));
    assertFalse(bin.want(2));
    assertFalse(bin.want(2048));
    assertTrue(bin.want(8));

    double[] buf = bin.obtain(8);
    assertEquals(8, buf.length);
    buf[0] = 42.0;

    double[] copy = bin.copyOf(buf, 8);
    assertNotNull(copy);
    assertEquals(42.0, copy[0]);
    assertNull(bin.copyOf(null, 8));

    bin.recycle(buf, 8);
    assertEquals(8, bin.getSize());
    assertThrows(IllegalStateException.class, () -> bin.recycle(buf, 8));

    double[] reused = bin.obtain(8);
    assertSame(buf, reused);
    assertEquals(0.0, reused[0]); // reset was called

    bin.recycle(reused, 8);
    bin.recycle(copy, 8);
    // Exceeds maxItemsPerBuffer (2) -> frees directly
    bin.recycle(new double[8], 8);
    assertEquals(1, frees.get());

    // Below minLength -> frees directly
    bin.recycle(new double[2], 2);
    assertEquals(2, frees.get());

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    bin.printAllProfiling(new PrintStream(baos));
    assertTrue(baos.toString().contains("Memory Allocation Profiling"));

    long cleared = bin.clear();
    assertEquals(16, cleared);
    assertEquals(0, bin.getSize());

    // Static bins DOUBLES and FLOATS
    double[] d = RecycleBin.DOUBLES.obtain(16);
    assertEquals(16, d.length);
    RecycleBin.DOUBLES.recycle(d, 16);
    RecycleBin.DOUBLES.clear();

    float[] f = RecycleBin.FLOATS.obtain(16);
    assertEquals(16, f.length);
    RecycleBin.FLOATS.recycle(f, 16);
    RecycleBin.FLOATS.clear();

    assertTrue(RecycleBin.equals(null, null));
    assertFalse(RecycleBin.equals("a", null));
    assertFalse(RecycleBin.equals(null, "a"));
    assertTrue(RecycleBin.equals("a", "a"));
  }

  @Test
  public void testRefUtil() {
    assertTrue(RefUtil.isRefAware(TestRef.class));
    assertTrue(RefUtil.isRefAware(TestRef[].class));
    assertFalse(RefUtil.isRefAware(int[].class));
    assertFalse(RefUtil.isRefAware(String.class));

    assertTrue(RefUtil.isFreeRefAware(TestRef.class));
    assertTrue(RefUtil.isFreeRefAware(Map.Entry.class));
    assertTrue(RefUtil.isFreeRefAware(Optional.class));
    assertTrue(RefUtil.isFreeRefAware(Object[].class));
    assertFalse(RefUtil.isFreeRefAware(double[].class));
    assertFalse(RefUtil.isFreeRefAware(String.class));

    TestRef a = new TestRef();
    TestRef b = new TestRef();
    TestRef[] arr = new TestRef[]{a, b};
    RefUtil.addRef(arr);
    assertEquals(2, a.currentRefCount());
    assertEquals(2, b.currentRefCount());
    assertTrue(RefUtil.assertAlive(arr));
    assertTrue(RefUtil.watch(arr));

    TestRef c = new TestRef();
    RefUtil.set(arr, 0, c); // frees one ref of a
    assertEquals(1, a.currentRefCount());
    assertSame(c, arr[0]);

    RefUtil.addRefs(a, "plain");
    assertEquals(2, a.currentRefCount());
    RefUtil.freeRefs(a, a, "plain");
    assertTrue(a.isFreed());

    RefUtil.freeRef(arr); // frees c and one ref of b
    assertTrue(c.isFreed());
    assertEquals(1, b.currentRefCount());

    // Map.Entry and Optional freeRef
    TestRef k = new TestRef();
    TestRef v = new TestRef();
    RefUtil.freeRef(new AbstractMap.SimpleEntry<>(k, v));
    assertTrue(k.isFreed());
    assertTrue(v.isFreed());

    TestRef optVal = new TestRef();
    assertSame(optVal, RefUtil.orElse(Optional.of(optVal), null));
    assertEquals("fallback", RefUtil.orElse(Optional.empty(), "fallback"));
    assertEquals("supplied", RefUtil.orElseGet(Optional.empty(), () -> "supplied"));
    assertSame(optVal, RefUtil.orElseGet(Optional.of(optVal), () -> null));
    Optional<Integer> mapped = RefUtil.map(Optional.of(optVal), x -> 7);
    assertEquals(Optional.of(7), mapped);
    assertTrue(optVal.isFreed());

    assertTrue(RefUtil.isNotNull(b));
    assertTrue(b.isFreed());
    assertFalse(RefUtil.isNotNull(null));

    // wrapInterface
    TestRef captured = new TestRef();
    AtomicBoolean ran = new AtomicBoolean(false);
    Runnable wrapped = RefUtil.wrapInterface((Runnable) () -> ran.set(true), captured);
    wrapped.run();
    assertTrue(ran.get());
    assertFalse(captured.isFreed());
    ((ReferenceCounting) wrapped).freeRef();
    assertTrue(captured.isFreed());
  }
}
