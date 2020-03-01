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

package com.simiacryptus.ref.lang;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.simiacryptus.ref.RefSettings;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.concurrent.Executors.newFixedThreadPool;

@RefIgnore
@SuppressWarnings("unused")
public abstract class ReferenceCountingBase implements ReferenceCounting {

  private static final Logger logger = LoggerFactory.getLogger(ReferenceCountingBase.class);
  private static final long LOAD_TIME = System.nanoTime();
  private static final UUID jvmId = UUID.randomUUID();
  private static final ExecutorService gcPool = newFixedThreadPool(1,
      new ThreadFactoryBuilder().setDaemon(true).build());
  private static final ThreadLocal<Boolean> inFinalizer = new ThreadLocal<Boolean>() {
    @Nonnull
    @Override
    protected Boolean initialValue() {
      return false;
    }
  };
  public static boolean supressLog = false;

  static {
    if (RefSettings.INSTANCE() == null)
      throw new RuntimeException();
  }

  private transient final AtomicInteger references = new AtomicInteger(1);
  private transient final AtomicBoolean isFreed = new AtomicBoolean(false);
  @Nullable
  private transient final StackTraceElement[] refCreatedBy;
  private transient LinkedList<StackTraceElement[]> addRefs = null;
  private transient LinkedList<StackTraceElement[]> freeRefs = null;
  private transient volatile boolean isFinalized = false;
  private transient boolean detached = false;

  protected ReferenceCountingBase() {
    if (RefSettings.INSTANCE().isLifecycleDebug(getClass())) {
      refCreatedBy = getStackTrace();
      watch();
    } else if (RefSettings.INSTANCE().watchCreation) {
      refCreatedBy = getStackTrace();
    } else {
      refCreatedBy = null;
    }
  }

  public synchronized void watch() {
    if (addRefs == null) addRefs = new LinkedList<>();
    if (freeRefs == null) freeRefs = new LinkedList<>();
  }

  public boolean isDetached() {
    return detached;
  }

  public final boolean isFreed() {
    return isFreed.get();
  }

  public static CharSequence referenceReport(@Nonnull ReferenceCountingBase obj, boolean includeCaller) {
    return obj.referenceReport(includeCaller, obj.isFreed(), true);
  }

  @Nonnull
  public static StackTraceElement[] removeSuffix(
      @Nonnull final @RefAware StackTraceElement[] stack,
      @Nonnull final @RefAware Collection<StackTraceElement> prefix) {
    return Arrays.stream(stack).limit(stack.length - prefix.size()).toArray(i -> new StackTraceElement[i]);
  }

  @Nullable
  public static List<StackTraceElement> findCommonPrefix(
      @Nonnull final @RefAware List<List<StackTraceElement>> reversedStacks) {
    if (0 == reversedStacks.size())
      return null;
    List<StackTraceElement> protoprefix = reversedStacks.get(0);
    for (int i = 0; i < protoprefix.size(); i++) {
      final int finalI = i;
      if (!reversedStacks.stream().allMatch(x -> x.size() > finalI && x.get(finalI).equals(protoprefix.get(finalI)))) {
        return protoprefix.subList(0, i);
      }
    }
    return protoprefix;
  }

  public static <T> List<T> reverseCopy(
      @Nullable final @RefAware List<T> x) {
    if (null == x)
      return Arrays.asList();
    return IntStream.range(0, x.size()).map(i -> x.size() - 1 - i).mapToObj(i -> x.get(i))
        .collect(Collectors.toList());
  }

  public static <T> List<T> reverseCopy(@Nonnull final @RefAware T[] x) {
    return IntStream.range(0, x.length).map(i -> x.length - 1 - i).mapToObj(i -> x[i]).collect(Collectors.toList());
  }

  @Nonnull
  private static String getString(@Nullable @RefAware StackTraceElement[] trace) {
    return null == trace ? ""
        : Arrays.stream(trace).parallel().map(x -> "at " + x).reduce((a, b) -> a + "\n" + b).orElse("");
  }

  @Override
  public ReferenceCounting addRef() {
    if (references.updateAndGet(i -> i > 0 ? i + 1 : 0) == 0)
      throw new IllegalStateException(referenceReport(true, isFreed(), true));
    if (null != addRefs && addRefs.size() < RefSettings.maxTracesPerObject) {
      StackTraceElement[] stackTrace = getStackTrace();
      if (null != stackTrace) {
        synchronized (addRefs) {
          addRefs.add(stackTrace);
        }
      }
    }
    return this;
  }

  @NotNull
  private StackTraceElement[] getStackTrace() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    stackTrace = Arrays.stream(stackTrace).filter(RefSettings::filter).toArray(StackTraceElement[]::new);
    return Arrays.copyOfRange(stackTrace, 3, Math.min(stackTrace.length, 3 + RefSettings.maxStackSize));
  }

  public boolean assertAlive() {
    boolean finalized = isFreed();
    if (finalized) {
      if (!inFinalizer.get()) {
        logger.warn(String.format("Using freed reference for %s", referenceHeader()));
        logger.warn(referenceReport(true, finalized, false));
        throw new LifecycleException(this);
      } else {
        throw new LifecycleException(this);
      }
    }
    return true;
  }

  public boolean assertFreed() {
    boolean finalized = isFreed();
    if (!finalized) {
      if (!inFinalizer.get()) {
        System.gc();
        logger.warn(String.format("Object not freed: %s", getClass().getSimpleName()));
        logger.warn(referenceReport(true, finalized, true));
        throw new LifecycleException(this);
      } else {
        throw new LifecycleException(this);
      }
    }
    return true;
  }

  @Override
  public int currentRefCount() {
    return references.get();
  }

  @Nonnull
  public ReferenceCountingBase detach() {
    this.detached = true;
    return this;
  }

  @Override
  public int freeRef() {
    if (isFreed()) {
      //logger.debug("Object has been finalized");
      return 0;
    }
    int refs = references.decrementAndGet();
    StackTraceElement[] stackTrace = getStackTrace();
    if (refs < 0 && !detached) {
      boolean isInFinalizer = Arrays.stream(stackTrace)
          .filter(x -> x.getClassName().equals("java.lang.ref.Finalizer")).findAny().isPresent();
      if (!isInFinalizer) {
        logger.warn(String.format("Error freeing reference for %s", getClass().getSimpleName()));
        logger.warn(referenceReport(true, isFreed(), true));
        throw new LifecycleException(this);
      } else {
        return refs;
      }
    }

    if (null != freeRefs && freeRefs.size() < RefSettings.maxTracesPerObject) {
      if (null != stackTrace) {
        synchronized (freeRefs) {
          freeRefs.add(stackTrace);
        }
      }
    }
    if (refs == 0 && !detached) {
      if (!isFreed.getAndSet(true)) {
        try {
          _free();
        } catch (LifecycleException e) {
          if (!inFinalizer.get())
            logger.info("Error freeing resources: " + referenceReport(true, isFreed(), true));
          throw e;
        }
      }
    }
    return refs;
  }

  public String referenceHeader() {
    LinkedList<StackTraceElement[]> addRefs = this.addRefs == null ? new LinkedList<>() : this.addRefs;
    LinkedList<StackTraceElement[]> freeRefs = this.freeRefs == null ? new LinkedList<>() : this.freeRefs;
    return String.format("Object %s (%d refs; %d adds, %d frees) ", getClass().getName(), references.get(), 1 + addRefs.size(), freeRefs.size());
  }

  public String referenceReport(boolean includeCaller, boolean isFinalized, boolean includeHeader) {
    @Nonnull
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    @Nonnull
    PrintStream out = new PrintStream(buffer);
    LinkedList<StackTraceElement[]> addRefs = this.addRefs == null ? new LinkedList<>() : this.addRefs;
    LinkedList<StackTraceElement[]> freeRefs = this.freeRefs == null ? new LinkedList<>() : this.freeRefs;
    if(includeHeader) out.print(
        String.format("Object %s (%d refs; %d adds, %d frees) ", getClass().getName(), references.get(), 1 + addRefs.size(), freeRefs.size()));
//    List<StackTraceElement> prefix = reverseCopy(findCommonPrefix(
//        Stream.concat(Stream.<StackTraceElement[]>of(refCreatedBy), Stream.concat(addRefs.stream(), freeRefs.stream()))
//            .filter(x -> x != null).map(x -> reverseCopy(x)).collect(Collectors.toList())));

    if (null != refCreatedBy) {
      //trace = removeSuffix(trace, prefix);
      out.println(String.format("created by \n\t%s", getString(this.refCreatedBy).replaceAll("\n", "\n\t")));
    }
    for (int i = 0; i < addRefs.size(); i++) {
      try {
        StackTraceElement[] stack = addRefs.get(i);
        if (null == stack) stack = new StackTraceElement[]{};
        //stack = removeSuffix(stack, prefix);
        final String string = getString(stack);
        if (!string.trim().isEmpty())
          out.println(String.format("reference added by %s\n\t%s", "", string.replaceAll("\n", "\n\t")));
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
    for (int i = 0; i < freeRefs.size() - (isFinalized ? 1 : 0); i++) {
      try {
        StackTraceElement[] stack = freeRefs.get(i);
        if (null == stack) stack = new StackTraceElement[]{};
//          stack = removeSuffix(stack, prefix);
        final String string = getString(stack);
        if (!string.trim().isEmpty())
          out.println(String.format("reference removed by %s\n\t%s", "", string.replaceAll("\n", "\n\t")));
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
    if (isFinalized && 0 < freeRefs.size()) {
      try {
        StackTraceElement[] stack = freeRefs.get(freeRefs.size() - 1);
//          stack = removeSuffix(stack, prefix);
        final String string = 0 == freeRefs.size() ? "" : getString(stack);
        if (!string.trim().isEmpty())
          out.println(String.format("freed by %s\n\t%s", "", string.replaceAll("\n", "\n\t")));
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
    if (includeCaller)
      out.println(String.format("apply current stack \n\t%s",
          getString(getStackTrace()).replaceAll("\n", "\n\t")));
    out.close();
    return buffer.toString();
  }

  @Override
  public boolean tryAddRef() {
    if (references.updateAndGet(i -> i > 0 ? i + 1 : 0) == 0) {
      return false;
    }
    if (null != addRefs) {
      StackTraceElement[] stackTrace = getStackTrace();
      if (null != stackTrace) {
        synchronized (addRefs) {
          addRefs.add(stackTrace);
        }
      }
    }
    return true;
  }

  @MustCall
  protected void _free() {
  }

  @Override
  protected final void finalize() {
    isFinalized = true;
    if (!isFreed.getAndSet(true)) {
      if (!isDetached() && !supressLog) {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Instance Reclaimed by GC at %.9f: %s", (System.nanoTime() - LOAD_TIME) / 1e9,
              referenceReport(false, false, true)));
        }
      }
      if (null != freeRefs) {
        StackTraceElement[] stackTrace = getStackTrace();
        if (null != stackTrace) {
          synchronized (freeRefs) {
            freeRefs.add(stackTrace);
          }
        }
      }
      inFinalizer.set(true);
      try {
        _free();
      } finally {
        inFinalizer.set(false);
      }
    }
  }

}
