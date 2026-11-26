package com.simiacryptus.lang;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class LangUtilitiesTest {

  @Test
  public void testDoubleStatisticsBasicAndCombine() {
    DoubleStatistics stats = new DoubleStatistics();
    assertEquals(0, stats.getCount());
    assertEquals(0.0, stats.getSumOfSquare());

    stats.accept(10.0);
    stats.accept(new double[]{20.0, 30.0});
    assertEquals(3, stats.getCount());
    assertEquals(60.0, stats.getSum(), 1e-9);
    assertEquals(20.0, stats.getAverage(), 1e-9);
    assertEquals(1400.0, stats.getSumOfSquare(), 1e-9);
    assertTrue(stats.getStandardDeviation() > 0);
    assertFalse(stats.toString().isEmpty());

    DoubleStatistics other = new DoubleStatistics();
    other.accept(40.0);
    stats.combine(other);
    assertEquals(4, stats.getCount());
    assertEquals(100.0, stats.getSum(), 1e-9);
    assertEquals(3000.0, stats.getSumOfSquare(), 1e-9);
  }

  @Test
  public void testDoubleStatisticsCollectors() {
    List<Double> values = Arrays.asList(1.0, 2.0, 3.0, 4.0);
    DoubleStatistics fromDoubles = values.stream().collect(DoubleStatistics.COLLECTOR);
    assertEquals(4, fromDoubles.getCount());
    assertEquals(10.0, fromDoubles.getSum(), 1e-9);
    assertEquals(30.0, fromDoubles.getSumOfSquare(), 1e-9);

    List<Integer> ints = Arrays.asList(2, 4, 6);
    DoubleStatistics fromNumbers = ints.stream().collect(DoubleStatistics.NUMBERS);
    assertEquals(3, fromNumbers.getCount());
    assertEquals(12.0, fromNumbers.getSum(), 1e-9);
    assertEquals(56.0, fromNumbers.getSumOfSquare(), 1e-9);
  }

  @Test
  public void testLazyVal() {
    AtomicInteger calls = new AtomicInteger(0);
    LazyVal<String> lazy = LazyVal.wrap(() -> {
      calls.incrementAndGet();
      return "computed";
    });
    assertEquals(0, calls.get());
    assertEquals("computed", lazy.get());
    assertEquals("computed", lazy.get());
    assertEquals(1, calls.get());
  }

  @Test
  public void testStackCounter() {
    StackCounter c1 = new StackCounter();
    StackCounter c2 = new StackCounter();
    c1.increment(10);
    c1.increment(20);
    c2.increment(5);

    String s1 = c1.toString();
    assertNotNull(s1);
    assertFalse(s1.isEmpty());

    String diff = StackCounter.toString(c1, c2, (a, b) -> a.getSum() - b.getSum());
    assertNotNull(diff);

    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    StackCounter.StackFrame f1 = new StackCounter.StackFrame(stack[0]);
    StackCounter.StackFrame f2 = new StackCounter.StackFrame(stack[0]);
    assertEquals(f1, f2);
    assertEquals(f1.hashCode(), f2.hashCode());
    assertNotEquals(f1, "not-a-frame");
    assertNotNull(f1.toString());
  }

  @Test
  public void testTuple2() {
    Tuple2<String, Integer> empty = new Tuple2<>();
    assertNull(empty.getFirst());
    assertNull(empty.getSecond());

    Tuple2<String, Integer> pair = new Tuple2<>("a", 42);
    assertEquals("a", pair.getFirst());
    assertEquals(42, pair.getSecond());
    assertEquals("a", pair._1);
    assertEquals(42, pair._2);
  }
}
