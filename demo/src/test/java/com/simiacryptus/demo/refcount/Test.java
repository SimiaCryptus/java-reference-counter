package com.simiacryptus.demo.refcount;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class Test {
  static {
    System.setProperty("DEBUG_LIFECYCLE", "true");
  }

  @org.junit.Test
  public void test() throws InterruptedException {
    ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory
        .getLogger(ReferenceCountingBase.class.getCanonicalName());
    logger.setLevel(Level.ALL);
    logger.setAdditive(false);
    final PrintStream out = System.out;
    final AtomicInteger bytes = new AtomicInteger(0);
    AppenderBase<ILoggingEvent> appender = new AppenderBase<ILoggingEvent>() {
      @Override
      protected synchronized void append(ILoggingEvent iLoggingEvent) {
        String formattedMessage = iLoggingEvent.getFormattedMessage();
        out.println(formattedMessage);
        out.flush();
        bytes.addAndGet(formattedMessage.length());
      }
    };
    appender.setName(UUID.randomUUID().toString());
    appender.start();
    logger.addAppender(appender);
    TestOperations.main();
    System.gc();
    Thread.sleep(1000);
    if (0 != bytes.get())
      throw new AssertionError();
  }
}
