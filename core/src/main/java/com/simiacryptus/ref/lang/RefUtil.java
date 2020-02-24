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

@RefIgnore
@SuppressWarnings("unused")
public class RefUtil {

  public static <T> void freeRef(@Nullable @RefAware T value) {
    if (null != value) {
      if (value instanceof ReferenceCounting) {
        ((ReferenceCounting) value).freeRef();
      } else if (value.getClass().isArray()) {
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

  @Nullable
  @RefAware
  public static <T> T addRef(@Nullable @RefIgnore T value) {
    if (null != value) {
      if (value instanceof ReferenceCounting) ((ReferenceCounting) value).addRef();
      else if (value.getClass().isArray()) {
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

  public static <T> T[] addRefs(@RefIgnore T... array) {
    synchronized (array) {
      for (int i = 0; i < array.length; i++) {
        RefUtil.addRef(array[i]);
      }
      return array;
    }
  }

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

  @Nonnull
  @RefIgnore
  public static <T> T get(@Nonnull @RefAware Optional<T> optional) {
    return optional.get();
  }

  @Nonnull
  @RefIgnore
  public static <T> T orElseGet(@Nonnull @RefAware Optional<T> optional, @RefAware Supplier<T> orElse) {
    if (optional.isPresent()) {
      return optional.get();
    } else {
      return orElse.get();
    }
  }

  @Nonnull
  @RefIgnore
  public static <T> T orElse(@Nonnull @RefAware Optional<T> optional, @RefAware T orElse) {
    if (optional.isPresent()) {
      return optional.get();
    } else {
      return orElse;
    }
  }

  @RefIgnore
  public static <T, R> Optional<R> map(@RefAware Optional<T> optional, @RefAware Function<T, R> fn) {
    try {
      return optional.map(fn);
    } finally {
      RefUtil.freeRef(fn);
      RefUtil.freeRef(optional);
    }
  }

  @RefIgnore
  public static <T> void set(@RefIgnore T[] array, int index, T value) {
    T prev;
    synchronized (array) {
      prev = array[index];
      array[index] = value;
    }
    RefUtil.freeRef(prev);
  }

  public static boolean assertAlive(@RefAware @RefIgnore @Nonnull Object obj) {
    if (obj instanceof ReferenceCounting) ((ReferenceCounting) obj).assertAlive();
    else if(obj.getClass().isArray()) {
      synchronized (obj) {
        int length = Array.getLength(obj);
        for (int i = 0; i < length; i++) {
          assertAlive(Array.get(obj, i));
        }
      }
    }
    return true;
  }

  private static class RefWrapperHandler<T> implements InvocationHandler {
    private final ReferenceCountingBase refcounter;
    private final T obj;

    public RefWrapperHandler(T obj, ReferenceCountingBase refcounter) {
      this.refcounter = refcounter;
      this.obj = obj;
    }

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

  private static class RefProxy<T> extends ReferenceCountingBase {
    private final T obj;
    private final Object[] refs;

    public RefProxy(T obj, Object... refs) {
      this.obj = obj;
      this.refs = refs;
    }

    @Override
    protected void _free() {
      RefUtil.freeRef(refs);
      if (obj instanceof ReferenceCounting)
        ((ReferenceCounting) obj).freeRef();
      super._free();
    }
  }
}
