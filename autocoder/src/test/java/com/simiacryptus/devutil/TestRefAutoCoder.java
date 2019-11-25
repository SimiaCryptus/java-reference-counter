package com.simiacryptus.devutil;

import com.simiacryptus.devutil.ref.RefAutoCoder;
import org.junit.Test;

public class TestRefAutoCoder {
  @Test
  public void add() {
    new RefAutoCoder("../demo").setAddRefcounting(true).rewrite();
  }
  @Test
  public void remove() {
    new RefAutoCoder("../demo").setAddRefcounting(false).rewrite();
  }
}
