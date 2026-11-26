package com.simiacryptus.ref.wrappers;

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class RefWrappersTest {

  private static class DummyRef extends ReferenceCountingBase {
  }

  @Test
  public void testRefAssert() {
    RefAssert.assertEquals("abc", "abc");
    RefAssert.assertEquals("msg", "abc", "abc");
    RefAssert.assertEquals((Object) null, (Object) null);
    RefAssert.assertEquals("long msg", 42L, 42L);
    RefAssert.assertEquals("int msg", 7, 7);
    RefAssert.assertEquals(true, true);

    DummyRef ref1 = new DummyRef();
    DummyRef ref2 = (DummyRef) ref1.addRef();
    RefAssert.assertEquals(ref1, ref2);
    ref1.freeRef();
    ref2.freeRef();
    assertTrue(ref1.isFreed());

    assertThrows(AssertionError.class, () -> RefAssert.assertEquals("a", "b"));
    assertThrows(AssertionError.class, () -> RefAssert.assertEquals("msg", "a", "b"));
    assertThrows(AssertionError.class, () -> RefAssert.assertEquals("msg", 1L, 2L));
    assertThrows(AssertionError.class, () -> RefAssert.assertEquals("msg", 1, 2));
    assertThrows(AssertionError.class, () -> RefAssert.assertEquals((Object) null, "b"));
    assertThrows(AssertionError.class, () -> RefAssert.fail("boom"));
  }

  @Test
  public void testRefString() {
    RefString s1 = new RefString("hello");
    RefString s2 = new RefString("hello".toCharArray());
    RefString s3 = new RefString("xxhelloyy".toCharArray(), 2, 5);
    RefString s4 = new RefString(new int[]{'h', 'e', 'l', 'l', 'o'}, 0, 5);
    RefString empty = new RefString();

    assertEquals(0, empty.length());
    assertEquals(5, s1.length());
    assertEquals('e', s1.charAt(1));
    assertEquals("ell", s1.subSequence(1, 4).toString());
    assertEquals(0, s1.compareTo("hello"));
    assertEquals(5, s2.length());
    assertEquals(0, s3.compareTo("hello"));
    assertEquals(0, s4.compareTo("hello"));

    DummyRef ref = new DummyRef();
    String formatted = RefString.format("val=%s", ref);
    assertTrue(formatted.startsWith("val="));
    assertTrue(ref.isFreed());
  }

  @Test
  public void testRefStringBuilder() {
    RefStringBuilder sb = new RefStringBuilder();
    sb.append("ab").append("cdef", 1, 3).append(new char[]{'x', 'y', 'z'}, 0, 2).append('!');
    assertEquals("abdexy!", sb.toString());
    assertEquals(7, sb.length());
    assertEquals('b', sb.charAt(1));
    assertEquals("bd", sb.subSequence(1, 3).toString());
    assertEquals("bd", sb.substring(1, 3));
    sb.delete(0, 2);
    assertEquals("dexy!", sb.toString());
    sb.reverse();
    assertEquals("!yxed", sb.toString());

    DummyRef ref = new DummyRef();
    RefStringBuilder sb2 = new RefStringBuilder("start:");
    sb2.append(ref);
    assertTrue(ref.isFreed());
    assertTrue(sb2.toString().startsWith("start:"));
  }

  @Test
  public void testRefArrays() {
    assertArrayEquals(new int[]{1, 2}, RefArrays.copyOf(new int[]{1, 2}, 2));
    assertArrayEquals(new byte[]{1, 2}, RefArrays.copyOf(new byte[]{1, 2}, 2));
    assertArrayEquals(new char[]{'a', 'b'}, RefArrays.copyOf(new char[]{'a', 'b'}, 2));
    assertArrayEquals(new double[]{1.0, 2.0}, RefArrays.copyOf(new double[]{1.0, 2.0}, 2));
    assertArrayEquals(new float[]{1f, 2f}, RefArrays.copyOf(new float[]{1f, 2f}, 2));
    assertArrayEquals(new String[]{"a", "b"}, RefArrays.copyOf(new String[]{"a", "b"}, 2));

    assertArrayEquals(new String[]{"b"}, RefArrays.copyOfRange(new String[]{"a", "b", "c"}, 1, 2));
    assertArrayEquals(new float[]{2f}, RefArrays.copyOfRange(new float[]{1f, 2f, 3f}, 1, 2));
    assertArrayEquals(new int[]{2}, RefArrays.copyOfRange(new int[]{1, 2, 3}, 1, 2));
    assertArrayEquals(new char[]{'b'}, RefArrays.copyOfRange(new char[]{'a', 'b', 'c'}, 1, 2));
    assertArrayEquals(new byte[]{2}, RefArrays.copyOfRange(new byte[]{1, 2, 3}, 1, 2));

    assertTrue(RefArrays.equals(new int[]{1}, new int[]{1}));
    assertTrue(RefArrays.equals(new long[]{1L}, new long[]{1L}));
    assertTrue(RefArrays.equals(new float[]{1f}, new float[]{1f}));
    assertTrue(RefArrays.equals(new byte[]{1}, new byte[]{1}));
    assertTrue(RefArrays.equals(new char[]{'a'}, new char[]{'a'}));
    assertTrue(RefArrays.equals(new double[]{1.0}, new double[]{1.0}));

    assertEquals("[1, 2]", RefArrays.toString(new int[]{1, 2}));
    assertEquals("[1, 2]", RefArrays.toString(new byte[]{1, 2}));
    assertEquals("[1.0, 2.0]", RefArrays.toString(new double[]{1.0, 2.0}));
    assertEquals("[1.0, 2.0]", RefArrays.toString(new float[]{1f, 2f}));
    assertEquals("[1, 2]", RefArrays.toString(new long[]{1L, 2L}));

    DummyRef r = new DummyRef();
    assertTrue(RefArrays.toString(new DummyRef[]{r}).startsWith("["));
    assertTrue(r.isFreed());

    double[] da = new double[3];
    RefArrays.parallelSetAll(da, i -> i * 2.0);
    assertArrayEquals(new double[]{0.0, 2.0, 4.0}, da);
    RefArrays.setAll(da, i -> i * 3.0);
    assertArrayEquals(new double[]{0.0, 3.0, 6.0}, da);

    int[] ia = new int[3];
    RefArrays.parallelSetAll(ia, i -> i + 1);
    assertArrayEquals(new int[]{1, 2, 3}, ia);
    RefArrays.setAll(ia, i -> i + 10);
    assertArrayEquals(new int[]{10, 11, 12}, ia);

    String[] sa = new String[2];
    RefArrays.setAll(sa, i -> "s" + i);
    assertArrayEquals(new String[]{"s0", "s1"}, sa);
    RefArrays.parallelSetAll(sa, i -> "p" + i);
    assertArrayEquals(new String[]{"p0", "p1"}, sa);

    assertEquals(1, RefArrays.binarySearch(new double[]{1.0, 2.0, 3.0}, 2.0));
    assertEquals(1, RefArrays.binarySearch(new int[]{1, 2, 3}, 2));

    assertEquals(Arrays.hashCode(da), RefArrays.hashCode(da));
    assertEquals(Arrays.hashCode(ia), RefArrays.hashCode(ia));
    assertEquals(Arrays.hashCode(new float[]{1f}), RefArrays.hashCode(new float[]{1f}));
    assertEquals(Arrays.hashCode(new long[]{1L}), RefArrays.hashCode(new long[]{1L}));
    assertEquals(Arrays.hashCode(new byte[]{1}), RefArrays.hashCode(new byte[]{1}));
    assertEquals(Arrays.hashCode(new char[]{'a'}), RefArrays.hashCode(new char[]{'a'}));

    Object[] nested = new Object[]{new Object[]{"a"}};
    assertEquals("[[a]]", RefArrays.deepToString(nested).toString());
    assertTrue(RefArrays.deepEquals(nested, new Object[]{new Object[]{"a"}}));
    assertEquals(Arrays.deepHashCode(nested), RefArrays.deepHashCode(nested));

    RefArrays.fill(sa, "z");
    assertArrayEquals(new String[]{"z", "z"}, sa);
    RefArrays.fill(da, 5.0);
    RefArrays.fill(da, 0, 1, 9.0);
    assertEquals(9.0, da[0]);
    RefArrays.fill(ia, 5);
    RefArrays.fill(ia, 0, 1, 9);
    assertEquals(9, ia[0]);
    float[] fa = new float[2];
    RefArrays.fill(fa, 1f);
    RefArrays.fill(fa, 0, 1, 2f);
    assertEquals(2f, fa[0]);
    long[] la = new long[2];
    RefArrays.fill(la, 1L);
    RefArrays.fill(la, 0, 1, 2L);
    assertEquals(2L, la[0]);
    char[] ca = new char[2];
    RefArrays.fill(ca, 'a');
    RefArrays.fill(ca, 0, 1, 'b');
    assertEquals('b', ca[0]);
    byte[] ba = new byte[2];
    RefArrays.fill(ba, 0, 1, (byte) 7);
    assertEquals((byte) 7, ba[0]);

    try (RefDoubleStream ds = RefArrays.stream(new double[]{1.0, 2.0})) {
      assertEquals(3.0, ds.sum(), 1e-9);
    }
    try (RefIntStream is = RefArrays.stream(new int[]{1, 2, 3})) {
      assertEquals(6, is.sum());
    }
    try (RefLongStream ls = RefArrays.stream(new long[]{1L, 2L, 3L})) {
      assertEquals(6L, ls.sum());
    }
    try (RefStream<String> emptyStream = RefArrays.stream((String[]) null)) {
      assertEquals(0, emptyStream.count());
    }

    RefList<String> list = RefArrays.asList("a", "b");
    assertEquals(2, list.size());
    list.freeRef();
  }

  @Test
  public void testRefCollections() {
    RefArrayList<String> list = new RefArrayList<>();
    list.add("a");
    list.add("b");
    assertEquals(2, RefCollections.getInnerCollection(list).size());
    assertEquals(2, RefCollections.getInnerStream(list).count());
    assertSame(list, RefCollections.unmodifiableList(list));
    assertSame(list, RefCollections.synchronizedList(list));

    List<String> plain = new ArrayList<>(Arrays.asList("x", "y"));
    assertSame(plain, RefCollections.getInnerCollection(plain));

    RefCollections.shuffle(list); // frees list
    assertTrue(list.isFreed());

    RefHashMap<String, String> map = new RefHashMap<>();
    assertSame(map, RefCollections.unmodifiableMap(map));
    map.freeRef();
  }
}
