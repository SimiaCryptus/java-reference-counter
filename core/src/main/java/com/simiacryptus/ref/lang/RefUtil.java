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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@RefAware
@RefIgnore
@SuppressWarnings("unused")
public class RefUtil {

  public static <T> void freeRef(@Nullable @RefAware T value) {
    if (null != value) {
      if (value instanceof ReferenceCounting) {
        ((ReferenceCounting) value).freeRef();
      } else if (value instanceof Map.Entry) {
        freeRef(((Map.Entry) value).getKey());
        freeRef(((Map.Entry) value).getValue());
      } else if (value instanceof Optional) {
        final Optional optional = (Optional) value;
        if (optional.isPresent())
          freeRef(optional.get());
      }
    }
  }

  @Nullable
  public static <T> T addRef(@Nullable @RefAware T value) {
    if (null != value && value instanceof ReferenceCounting)
      ((ReferenceCounting) value).addRef();
    return value;
  }

  @NotNull
  public static <T> T wrapInterface(@NotNull @RefAware T obj,
      @NotNull @RefAware Object... refs) {
    final Class<?> objClass = obj.getClass();
    final ReferenceCountingBase refcounter = new ReferenceCountingBase() {
      @Override
      protected void _free() {
        Arrays.stream(refs).forEach(RefUtil::freeRef);
        if (obj instanceof ReferenceCounting)
          ((ReferenceCounting) obj).freeRef();
        super._free();
      }
    };
    return (T) Proxy.newProxyInstance(ReferenceCounting.class.getClassLoader(),
        Stream.concat(Arrays.stream(objClass.getInterfaces()), Stream.of(ReferenceCounting.class)).distinct()
            .toArray(i -> new Class[i]),
        new InvocationHandler() {
          @Override
          public Object invoke(@RefAware Object proxy,
              @NotNull @RefAware Method method,
              @RefAware Object[] args) throws Throwable {
            if (method.getDeclaringClass().equals(ReferenceCounting.class)) {
              return method.invoke(refcounter, args);
            } else {
              return method.invoke(obj, args);
            }
          }
        });
  }

  public static <T> void freeRefs(@NotNull @RefAware T[] array) {
    Arrays.stream(array).filter((x) -> x != null).forEach(RefUtil::freeRef);
  }

}
