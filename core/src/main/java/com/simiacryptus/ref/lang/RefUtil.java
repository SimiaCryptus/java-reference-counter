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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * This class provides utility functions for working with references.
 *
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public class RefUtil {

  /**
   * Frees the given value.
   *
   * @param value the value to free, or {@code null}
   * @docgenVersion 9
   */
  public static <T> void freeRef(@Nullable @RefAware T value) {
    if (null != value) {
      Class<?> valueClass = value.getClass();
      if (!RefUtil.isFreeRefAware(valueClass)) {
        return;
      }
      if (value instanceof ReferenceCounting) {
        ((ReferenceCounting) value).freeRef();
      } else if (valueClass.isArray()) {
        synchronized (value) {
          int length = Array.getLength(value);
          for (int i = 0; i < length; i++) {
            Object arrayElement = Array.get(value, i);
            freeRef(arrayElement);
          }
        }
      } else if (value instanceof Map.Entry) {
        freeRef(((Map.Entry) value).getKey());
        freeRef(((Map.Entry) value).getValue());
      } else if (value instanceof Optional) {
        final Optional optional = (Optional) value;
        if (optional.isPresent())
          freeRef(get(optional));
      }
    }
  }

  /**
   * @param value the value to add a reference to
   * @return the value with a reference added
   * @docgenVersion 9
   */
  @Nullable
  @RefAware
  public static <T> T addRef(@Nullable @RefIgnore T value) {
    if (null != value) {
      Class<?> valueClass = value.getClass();
      if (!RefUtil.isRefAware(valueClass)) {
        return value;
      }
      if (value instanceof ReferenceCounting) ((ReferenceCounting) value).addRef();
      else if (valueClass.isArray()) {
        synchronized (value) {
          int length = Array.getLength(value);
          for (int i = 0; i < length; i++) {
            addRef(Array.get(value, i));
          }
        }
      }
    }
    return value;
  }

  /**
   * Adds references to the given objects.
   *
   * @param array the objects to add references to
   * @docgenVersion 9
   */
  public static void addRefs(@RefIgnore Object... array) {
    for (int i = 0; i < array.length; i++) {
      RefUtil.addRef(array[i]);
    }
  }

  /**
   * Frees the given references.
   *
   * @param array the references to free
   * @docgenVersion 9
   */
  public static void freeRefs(@RefIgnore Object... array) {
    for (int i = 0; i < array.length; i++) {
      RefUtil.freeRef(array[i]);
    }
  }

  /**
   * @param obj  the object to wrap
   * @param refs the references to use
   * @return the wrapped object
   * @docgenVersion 9
   */
  @Nonnull
  public static @RefAware
  <T> T wrapInterface(@Nonnull @RefAware T obj,
                      @Nonnull @RefAware Object... refs) {
    return (T) Proxy.newProxyInstance(
        ReferenceCounting.class.getClassLoader(),
        Stream.concat(
            Arrays.stream(obj.getClass().getInterfaces()),
            Stream.of(ReferenceCounting.class)
        ).distinct().toArray(i -> new Class[i]),
        new RefWrapperHandler(obj, new RefProxy(obj, refs)));
  }

  /**
   * @param optional The optional object to get the value from
   * @param <T>      The type of the optional object
   * @return The value from the optional object
   * @docgenVersion 9
   */
  @Nonnull
  @RefIgnore
  public static <T> T get(@Nonnull @RefAware Optional<T> optional) {
    return optional.get();
  }

  /**
   * Returns the value if present, otherwise returns {@code orElse}.
   *
   * @param <T>      the type of the value
   * @param optional the {@link Optional} to check
   * @param orElse   the value to return if absent
   * @return the value, if present, otherwise {@code orElse}
   * @docgenVersion 9
   */
  @Nonnull
  @RefIgnore
  public static <T> T orElseGet(@Nonnull @RefAware Optional<T> optional, @RefAware Supplier<T> orElse) {
    try {
      if (optional.isPresent()) {
        return optional.get();
      } else {
        return orElse.get();
      }
    } finally {
      RefUtil.freeRef(orElse);
    }
  }

  /**
   * Returns the value if present, otherwise returns {@code orElse}.
   *
   * @param <T>      the type of the value
   * @param optional the {@link Optional} to check for a value
   * @param orElse   the value to return if there is no value present
   * @return the value, if present, otherwise {@code orElse}
   * @throws NullPointerException if {@code optional} is null
   * @docgenVersion 9
   */
  @Nonnull
  @RefIgnore
  public static <T> T orElse(@Nonnull @RefAware Optional<T> optional, @RefAware T orElse) {
    if (optional.isPresent()) {
      return optional.get();
    } else {
      return orElse;
    }
  }

  /**
   * @param <T>
   * @param <R>
   * @param optional
   * @param fn
   * @return {@link Optional}
   * @docgenVersion 9
   */
  @RefIgnore
  public static <T, R> Optional<R> map(@RefAware Optional<T> optional, @RefAware Function<T, R> fn) {
    try {
      return optional.map(fn);
    } finally {
      RefUtil.freeRef(fn);
      RefUtil.freeRef(optional);
    }
  }

  /**
   * @RefIgnore public static <T> void set(@RefIgnore T[] array, int index, T value) {
   * T prev;
   * synchronized (array) {
   * prev = array[index];
   * array[index] = value;
   * }
   * RefUtil.freeRef(prev);
   * }
   * @docgenVersion 9
   */
  @RefIgnore
  public static <T> void set(@RefIgnore T[] array, int index, T value) {
    T prev;
    synchronized (array) {
      prev = array[index];
      array[index] = value;
    }
    RefUtil.freeRef(prev);
  }

  /**
   * @param obj the object to check
   * @return true if the object is not null
   * @docgenVersion 9
   */
  public static boolean assertAlive(@RefAware @RefIgnore @Nonnull Object obj) {
    if (obj instanceof ReferenceCounting) ((ReferenceCounting) obj).assertAlive();
    else if (obj.getClass().isArray()) {
      synchronized (obj) {
        int length = Array.getLength(obj);
        for (int i = 0; i < length; i++) {
          assertAlive(Array.get(obj, i));
        }
      }
    }
    return true;
  }

  /**
   * Watches the given object.
   *
   * @param obj the object to watch
   * @return true if the object was successfully watched; false otherwise
   * @throws NullPointerException if obj is null
   * @docgenVersion 9
   */
  public static boolean watch(@RefAware @RefIgnore @Nonnull Object obj) {
    if (obj instanceof ReferenceCounting) ((ReferenceCountingBase) obj).watch();
    else if (obj.getClass().isArray()) {
      synchronized (obj) {
        int length = Array.getLength(obj);
        for (int i = 0; i < length; i++) {
          watch(Array.get(obj, i));
        }
      }
    }
    return true;
  }

  /**
   * Returns true if the given class is reference-aware. A reference-aware class is one that can be used
   * with the Java Reflection API.
   *
   * @param c the class to check
   * @return true if the class is reference-aware, false otherwise
   * @docgenVersion 9
   */
  public static boolean isRefAware(Class<?> c) {
    if (ReferenceCounting.class.isAssignableFrom(c)) {
      return true;
    } else if (c.isArray()) {
      Class<?> componentType = c.getComponentType();
      if (componentType.isPrimitive()) return false;
      return true;
    } else {
      return false;
    }
  }

  /**
   * Returns true if the given class is free reference aware.
   *
   * @param c the class to check
   * @return true if the class is free reference aware
   * @docgenVersion 9
   */
  public static boolean isFreeRefAware(Class<?> c) {
    if (ReferenceCounting.class.isAssignableFrom(c)) {
      return true;
    } else if (Map.Entry.class.isAssignableFrom(c)) {
      return true;
    } else if (Optional.class.isAssignableFrom(c)) {
      return true;
    } else if (c.isArray()) {
      Class<?> componentType = c.getComponentType();
      if (componentType.isPrimitive()) return false;
      return true;
    } else {
      return false;
    }
  }

  /**
   * Determines if an object is not null.
   *
   * @param x the object to check
   * @return true if the object is not null, false otherwise
   * @docgenVersion 9
   */
  public static boolean isNotNull(@RefAware Object x) {
    boolean notNull = null != x;
    RefUtil.freeRef(x);
    return notNull;
  }

  /**
   * A handler for a reference-counting object.
   *
   * @param <T> the type of the object being reference-counted
   * @docgenVersion 9
   */
  private static class RefWrapperHandler<T> implements InvocationHandler {
    private final ReferenceCountingBase refcounter;
    private final T obj;

    public RefWrapperHandler(T obj, ReferenceCountingBase refcounter) {
      this.refcounter = refcounter;
      this.obj = obj;
    }

    /**
     * @Override public Object invoke(@RefAware Object proxy, @Nonnull @RefAware Method method, @RefAware Object[] args) throws Throwable {
     * if (method.getDeclaringClass().equals(ReferenceCounting.class)) {
     * return method.invoke(refcounter, args);
     * } else {
     * return method.invoke(obj, args);
     * }
     * }
     * @docgenVersion 9
     */
    @Override
    public Object invoke(@RefAware Object proxy,
                         @Nonnull @RefAware Method method,
                         @RefAware Object[] args) throws Throwable {
      if (method.getDeclaringClass().equals(ReferenceCounting.class)) {
        return method.invoke(refcounter, args);
      } else {
        return method.invoke(obj, args);
      }
    }
  }

  /**
   * This class is a proxy for an object that contains references to other objects.
   *
   * @param <T> The type of the object being proxied.
   * @docgenVersion 9
   */
  private static class RefProxy<T> extends ReferenceCountingBase {
    private final T obj;
    private final Object[] refs;

    public RefProxy(T obj, Object... refs) {
      this.obj = obj;
      this.refs = refs;
      //this.detach();
    }

    /**
     * Frees resources associated with this object.
     *
     * @docgenVersion 9
     * @see RefCounting#_free()
     */
    @Override
    protected void _free() {
      RefUtil.freeRef(refs);
      if (obj instanceof ReferenceCounting)
        ((ReferenceCounting) obj).freeRef();
      super._free();
    }
  }
}
