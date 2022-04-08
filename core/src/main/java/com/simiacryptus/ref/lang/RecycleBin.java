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
import com.simiacryptus.lang.StackCounter;
import com.simiacryptus.ref.RefSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * This class is a RecycleBin for doubles and floats.
 *
 * @author Java
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public abstract class RecycleBin<T> {

  public static final RecycleBin<double[]> DOUBLES = new RecycleBin<double[]>() {
    /**
     * Creates an array of doubles with the given length.
     *
     * @param length the length of the array
     * @return the array of doubles
     *
     *   @docgenVersion 9
     */
    @Nonnull
    @Override
    public double[] create(final long length) {
      return new double[(int) length];
    }

    /**
     * Resets the data array to all zeros.
     *
     * @param data the data array to reset
     * @param size the size of the data array
     *
     *   @docgenVersion 9
     */
    @Override
    public void reset(@Nonnull final double[] data, long size) {
      assert data.length == size;
      Arrays.fill(data, 0);
    }

    /**
     * This method frees the given object.
     *
     * @param obj The object to free.
     *
     *   @docgenVersion 9
     */
    @Override
    protected void free(double[] obj) {
    }
  }.setPersistanceMode(RefSettings.INSTANCE().doubleCacheMode);
  public static final RecycleBin<float[]> FLOATS = new RecycleBin<float[]>() {
    /**
     * Creates a new float array of the specified length.
     *
     * @param length the length of the array
     * @return the new array
     * @throws NullPointerException if the specified array is null
     *
     *   @docgenVersion 9
     */
    @Nonnull
    @Override
    public float[] create(final long length) {
      return new float[(int) length];
    }

    /**
     * Resets the data array to all zeros.
     *
     * @param data the data array to reset
     * @param size the size of the data array
     *
     *   @docgenVersion 9
     */
    @Override
    public void reset(@Nonnull final float[] data, long size) {
      assert data.length == size;
      Arrays.fill(data, 0);
    }

    /**
     * This method frees the given object.
     *
     * @param obj The object to be freed.
     *
     *   @docgenVersion 9
     */
    @Override
    protected void free(float[] obj) {
    }
  }.setPersistanceMode(RefSettings.INSTANCE().doubleCacheMode);
  protected static final Logger logger = LoggerFactory.getLogger(RecycleBin.class);
  private static volatile ScheduledExecutorService garbageTruck;
  private final Map<Long, ConcurrentLinkedDeque<ObjectWrapper>> buckets = new ConcurrentHashMap<>();
  private final StackCounter allocations = new StackCounter();
  private final StackCounter frees = new StackCounter();
  private final StackCounter recycle_put = new StackCounter();
  private final StackCounter recycle_get = new StackCounter();
  private int purgeFreq;
  private int profilingThreshold = Integer.MAX_VALUE;
  private PersistanceMode persistanceMode = PersistanceMode.WEAK;
  private int minLengthPerBuffer = 16;
  private double maxLengthPerBuffer = 1e9;
  private int maxItemsPerBuffer = 100;

  protected RecycleBin() {
    super();
    purgeFreq = 10;
    RecycleBin.getGarbageTruck().scheduleAtFixedRate(() -> {
      buckets.forEach((k, v) -> {
        ObjectWrapper poll;
        ArrayList<ObjectWrapper> young = new ArrayList<>();
        while (null != (poll = v.poll())) {
          if (poll.age() > purgeFreq) {
            T obj = poll.obj.get();
            if (obj != null) {
              freeItem(obj, k);
            }
          } else {
            young.add(poll);
          }
        }
        v.addAll(young);
      });
    }, purgeFreq, purgeFreq, TimeUnit.SECONDS);
  }

  /**
   * Returns a ScheduledExecutorService that can be used to schedule garbage truck pickups.
   *
   * @return a ScheduledExecutorService that can be used to schedule garbage truck pickups
   * @docgenVersion 9
   */
  public static ScheduledExecutorService getGarbageTruck() {
    if (null == RecycleBin.garbageTruck) {
      synchronized (RecycleBin.class) {
        if (null == RecycleBin.garbageTruck) {
          RecycleBin.garbageTruck = Executors.newScheduledThreadPool(1,
              new ThreadFactoryBuilder().setDaemon(true).build());
        }
      }
    }
    return RecycleBin.garbageTruck;
  }

  /**
   * Returns the maximum number of items that can be stored in a buffer.
   *
   * @return the maximum number of items that can be stored in a buffer
   * @docgenVersion 9
   */
  public int getMaxItemsPerBuffer() {
    return maxItemsPerBuffer;
  }

  /**
   * Sets the maximum number of items per buffer.
   *
   * @param maxItemsPerBuffer the maximum number of items per buffer
   * @return the recycle bin
   * @docgenVersion 9
   */
  @Nonnull
  public RecycleBin<T> setMaxItemsPerBuffer(int maxItemsPerBuffer) {
    this.maxItemsPerBuffer = maxItemsPerBuffer;
    return this;
  }

  /**
   * Returns the maximum length per buffer.
   *
   * @return the maximum length per buffer
   * @docgenVersion 9
   */
  public double getMaxLengthPerBuffer() {
    return maxLengthPerBuffer;
  }

  /**
   * Sets the maximum length per buffer.
   *
   * @param maxLengthPerBuffer the maximum length per buffer
   * @return the recycle bin
   * @docgenVersion 9
   */
  @Nonnull
  public RecycleBin<T> setMaxLengthPerBuffer(double maxLengthPerBuffer) {
    this.maxLengthPerBuffer = maxLengthPerBuffer;
    return this;
  }

  /**
   * Returns the minimum length per buffer.
   *
   * @return the minimum length per buffer
   * @docgenVersion 9
   */
  public int getMinLengthPerBuffer() {
    return minLengthPerBuffer;
  }

  /**
   * Sets the minimum length per buffer.
   *
   * @param minLengthPerBuffer the minimum length per buffer
   * @return the recycle bin
   * @docgenVersion 9
   */
  @Nonnull
  public RecycleBin<T> setMinLengthPerBuffer(int minLengthPerBuffer) {
    this.minLengthPerBuffer = minLengthPerBuffer;
    return this;
  }

  /**
   * Returns the current persistance mode.
   *
   * @return the current persistance mode
   * @docgenVersion 9
   */
  public PersistanceMode getPersistanceMode() {
    return persistanceMode;
  }

  /**
   * Sets the persistance mode.
   *
   * @param persistanceMode the new persistance mode
   * @return the recycle bin
   * @docgenVersion 9
   */
  @Nonnull
  public RecycleBin<T> setPersistanceMode(@RefAware PersistanceMode persistanceMode) {
    this.persistanceMode = persistanceMode;
    return this;
  }

  /**
   * Returns the purge frequency.
   *
   * @return the purge frequency
   * @docgenVersion 9
   */
  public int getPurgeFreq() {
    return purgeFreq;
  }

  /**
   * Sets the purge frequency.
   *
   * @param purgeFreq the purge frequency
   * @return the recycle bin
   * @docgenVersion 9
   */
  @Nonnull
  public RecycleBin<T> setPurgeFreq(int purgeFreq) {
    this.purgeFreq = purgeFreq;
    return this;
  }

  /**
   * Returns the size of the cache.
   *
   * @return the size of the cache
   * @docgenVersion 9
   */
  public long getSize() {
    return this.buckets.entrySet().stream().mapToLong(e -> e.getKey() * e.getValue().size()).sum();
  }

  /**
   * Sets the profiling threshold.
   *
   * @param threshold the profiling threshold
   * @return the recycle bin
   * @docgenVersion 9
   */
  @Nonnull
  public RecycleBin<T> setProfiling(final int threshold) {
    this.profilingThreshold = threshold;
    return this;
  }

  /**
   * Returns true if the arguments are equal to each other
   * and false otherwise.
   *
   * @param a an Object
   * @param b an Object
   * @return true if the arguments are equal to each other and false otherwise.
   * @docgenVersion 9
   */
  public static boolean equals(@Nullable @RefAware Object a, @Nullable @RefAware Object b) {
    if (a == b)
      return true;
    if (a == null || b == null)
      return false;
    return a.equals(b);
  }

  /**
   * Clears the bits in this {@code BitSet} whose corresponding
   * {@code boolean} value is {@code true}.
   *
   * @return the number of bits set to {@code false} in this {@code BitSet}
   * as a {@code long}
   * @docgenVersion 9
   * @since 1.7
   */
  public long clear() {
    Map<Long, ConcurrentLinkedDeque<ObjectWrapper>> buckets = this.buckets;
    return buckets.keySet().stream().mapToLong(length -> {
      ConcurrentLinkedDeque<ObjectWrapper> remove = buckets.remove(length);
      if (null == remove || remove.isEmpty())
        return 0;
      return remove.stream().mapToLong(ref -> {
        return freeItem(ref.obj.get(), length);
      }).sum();
    }).sum();
  }

  /**
   * Returns a copy of the original object, or null if the original is null.
   *
   * @param original the object to copy
   * @param size     the size of the object to copy
   * @return a copy of the original object, or null if the original is null
   * @docgenVersion 9
   */
  @Nullable
  public T copyOf(@Nullable final @RefAware T original, long size) {
    if (null == original)
      return null;
    final T copy = obtain(size);
    System.arraycopy(original, 0, copy, 0, (int) size);
    return copy;
  }

  /**
   * Creates an object of type T with the specified length.
   *
   * @param length the length of the object to be created
   * @return the newly created object
   * @throws NullPointerException if length is null
   * @docgenVersion 9
   */
  @Nonnull
  public abstract T create(long length);

  /**
   * Creates a new object with the specified length.
   *
   * @param length  the length of the new object
   * @param retries the number of retries
   * @return the new object
   * @throws IllegalArgumentException if the length is negative
   * @throws RetryException           if the number of retries is negative
   * @docgenVersion 9
   */
  @Nonnull
  public T create(long length, int retries) {
    try {
      @Nonnull
      T result = create(length);
      @Nullable
      StackCounter stackCounter = getAllocations(length);
      if (null != stackCounter) {
        stackCounter.increment(length);
      }
      return result;
    } catch (@Nonnull final Throwable e) {
      if (retries <= 0)
        throw new RuntimeException(String.format("Could not allocate %d bytes", length), e);
    }
    clearMemory(length);
    return create(length, retries - 1);
  }

  /**
   * Returns the number of allocations if the length is being profiled, or null if not.
   *
   * @param length the number to check
   * @return the number of allocations or null
   * @docgenVersion 9
   */
  @Nullable
  public StackCounter getAllocations(final long length) {
    if (!isProfiling(length))
      return null;
    return allocations;
  }

  /**
   * Returns the number of free stack frames, or null if profiling is disabled.
   *
   * @param length the number of stack frames to check
   * @return the number of free stack frames, or null
   * @docgenVersion 9
   */
  @Nullable
  public StackCounter getFrees(final long length) {
    if (!isProfiling(length))
      return null;
    return frees;
  }

  /**
   * @return the recycle_get stack counter if the specified length is being profiled, null otherwise
   * @docgenVersion 9
   */
  @Nullable
  public StackCounter getRecycle_get(final long length) {
    if (!isProfiling(length))
      return null;
    return recycle_get;
  }

  /**
   * @return the recycle_put stack counter if the specified length is being profiled, null otherwise
   * @docgenVersion 9
   */
  @Nullable
  public StackCounter getRecycle_put(final long length) {
    if (!isProfiling(length))
      return null;
    return recycle_put;
  }

  /**
   * Returns true if the specified length is greater than the profiling threshold.
   *
   * @param length the length to check
   * @return true if the specified length is greater than the profiling threshold
   * @docgenVersion 9
   */
  public boolean isProfiling(final long length) {
    return length > profilingThreshold;
  }

  /**
   * Obtains an object of type T with a length of long.
   *
   * @param length the length of the object to be obtained
   * @return an object of type T with the specified length
   * @docgenVersion 9
   */
  public T obtain(final long length) {
    final ConcurrentLinkedDeque<ObjectWrapper> bin = buckets.get(length);
    @Nullable
    StackCounter stackCounter = getRecycle_get(length);
    if (null != stackCounter) {
      stackCounter.increment(length);
    }
    if (null != bin) {
      final ObjectWrapper ref = bin.poll();
      if (null != ref) {
        final T data = ref.obj.get();
        if (null != data) {
          reset(data, length);
          return data;
        }
      }
    }
    return create(length, 1);
  }

  /**
   * Prints all profiling information to the given PrintStream.
   *
   * @param out the PrintStream to print to; must not be null
   * @docgenVersion 9
   */
  public void printAllProfiling(@Nonnull final @RefAware PrintStream out) {
    printDetailedProfiling(out);
    printNetProfiling(out);
  }

  /**
   * Prints a detailed report of the profiling information to the given {@link PrintStream}.
   *
   * @param out the {@link PrintStream} to print the report to
   * @docgenVersion 9
   */
  public void printDetailedProfiling(@Nonnull final @RefAware PrintStream out) {
    out.println("Memory Allocation Profiling:\n\t" + allocations.toString().replaceAll("\n", "\n\t"));
    out.println("Freed Memory Profiling:\n\t" + frees.toString().replaceAll("\n", "\n\t"));
    out.println("Recycle Bin (Put) Profiling:\n\t" + recycle_put.toString().replaceAll("\n", "\n\t"));
    out.println("Recycle Bin (Get) Profiling:\n\t" + recycle_get.toString().replaceAll("\n", "\n\t"));
  }

  /**
   * Prints the net profiling for the recycle bin.
   *
   * @param out the print stream to use, or null to not print anything
   * @docgenVersion 9
   */
  public void printNetProfiling(@Nullable final @RefAware PrintStream out) {
    if (null != out) {
      out.println("Recycle Bin (Net) Profiling:\n\t" + StackCounter
          .toString(recycle_put, recycle_get, (a, b) -> a.getSum() - b.getSum()).replaceAll("\n", "\n\t"));
    }
  }

  /**
   * Recycles the given data if it is not null and its size is greater than 0.
   *
   * @param data the data to recycle
   * @param size the size of the data
   * @docgenVersion 9
   */
  public void recycle(@Nullable final @RefAware T data, long size) {
    if (null != data && size >= getMinLengthPerBuffer() && size <= getMaxLengthPerBuffer()) {
      @Nullable
      StackCounter stackCounter = getRecycle_put(size);
      if (null != stackCounter) {
        stackCounter.increment(size);
      }
      ConcurrentLinkedDeque<ObjectWrapper> bin = getBin(size);
      if (bin.size() < Math.min(Math.max(1, (int) (getMaxLengthPerBuffer() / size)), getMaxItemsPerBuffer())) {
        //        synchronized (bin) {
        //        }
        boolean present = bin.stream().filter(x -> equals(x.obj.get(), data)).findAny().isPresent();
        if (present)
          throw new IllegalStateException();
        else {
          bin.add(new ObjectWrapper(wrap(data)));
          return;
        }
      }
    }
    freeItem(data, size);
  }

  /**
   * Resets the data and size.
   *
   * @param data the data to be reset
   * @param size the size to be reset
   * @docgenVersion 9
   */
  public abstract void reset(@RefAware T data, long size);

  /**
   * @param size
   * @return
   * @docgenVersion 9
   */
  public boolean want(long size) {
    if (size < getMinLengthPerBuffer())
      return false;
    if (size > getMaxLengthPerBuffer())
      return false;
    @Nullable
    StackCounter stackCounter = getRecycle_put(size);
    if (null != stackCounter) {
      stackCounter.increment(size);
    }
    ConcurrentLinkedDeque<ObjectWrapper> bin = getBin(size);
    return bin.size() < Math.min(Math.max(1, (int) (getMaxLengthPerBuffer() / size)), getMaxItemsPerBuffer());
  }

  /**
   * This method is used to free an object.
   *
   * @param obj The object to be freed.
   * @docgenVersion 9
   */
  protected abstract void free(@RefAware T obj);

  /**
   * This method returns the size of the freed item.
   *
   * @param obj  The object to be freed.
   * @param size The size of the object to be freed.
   * @return The size of the freed object.
   * @docgenVersion 9
   */
  protected long freeItem(@Nullable @RefAware T obj, long size) {
    @Nullable
    StackCounter stackCounter = getFrees(size);
    if (null != stackCounter) {
      stackCounter.increment(size);
    }
    if (null != obj)
      free(obj);
    return size;
  }

  /**
   * Returns the bin for the given size. If no bin exists for the given size, a new bin is created.
   *
   * @param size the size to get the bin for
   * @return the bin for the given size
   * @docgenVersion 9
   */
  protected ConcurrentLinkedDeque<ObjectWrapper> getBin(long size) {
    return buckets.computeIfAbsent(size, x -> new ConcurrentLinkedDeque<>());
  }

  /**
   * @param data the data to wrap
   * @return a supplier of the wrapped data
   * @docgenVersion 9
   */
  @Nullable
  protected Supplier<T> wrap(@RefAware T data) {
    return persistanceMode.wrap(data);
  }

  /**
   * Clears the memory.
   *
   * @param length the length of the memory to be cleared
   * @docgenVersion 9
   */
  private void clearMemory(long length) {
    long max = Runtime.getRuntime().maxMemory();
    long previous = Runtime.getRuntime().freeMemory();
    long size = getSize();
    logger.warn(
        String.format("Allocation of length %d failed; %s/%s used memory; %s items in recycle buffer; Clearing memory",
            length, previous, max, size));
    clear();
    System.gc();
    long after = Runtime.getRuntime().freeMemory();
    logger.warn(String.format("Clearing memory freed %s/%s bytes", previous - after, max));
  }

  /**
   * This class wraps an object in a supplier with a timestamp.
   *
   * @param <T> The type of the object to be wrapped.
   * @docgenVersion 9
   */
  @RefIgnore
  private class ObjectWrapper {
    public final Supplier<T> obj;
    public final long createdAt = System.nanoTime();

    private ObjectWrapper(final @RefAware Supplier<T> obj) {
      this.obj = obj;
    }

    /**
     * Returns the age of the object in seconds.
     *
     * @return the age of the object in seconds
     * @docgenVersion 9
     */
    public final double age() {
      return (System.nanoTime() - createdAt) / 1e9;
    }
  }
}
