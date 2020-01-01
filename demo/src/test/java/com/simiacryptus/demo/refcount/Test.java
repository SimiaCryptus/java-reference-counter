/*
 * Copyright (c) 2020 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
