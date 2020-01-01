package com.simiacryptus.lang;

import java.io.Serializable;

public class Tuple2<A, B> implements Serializable {
  public final A _1;
  public final B _2;

  public Tuple2() {
    this(null, null);
  }

  public Tuple2(final A a, final B b) {
    _1 = a;
    _2 = b;
  }

  public A getFirst() {
    return _1;
  }

  public B getSecond() {
    return _2;
  }
}
