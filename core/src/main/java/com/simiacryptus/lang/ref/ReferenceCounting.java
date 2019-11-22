/*
 * Copyright (c) 2019 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.lang.ref;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

public interface ReferenceCounting {

  int currentRefCount();

  void freeRefAsync();

  ReferenceCounting addRef();

  boolean tryAddRef();

  boolean isFinalized();

  boolean assertAlive();

  int freeRef();

  UUID getObjectId();

  ReferenceCounting detach();

  public static <T> T wrapInterface(T obj, ReferenceCounting... refs) {
    final Class<?> objClass = obj.getClass();
    final ReferenceCountingBase refcounter = new ReferenceCountingBase() {
      @Override
      protected void _free() {
        Arrays.stream(refs).forEach(ReferenceCounting::freeRef);
        super._free();
      }
    };
    return (T) Proxy.newProxyInstance(objClass.getClassLoader(), Stream.concat(
        Arrays.stream(objClass.getInterfaces()),
        Stream.of(ReferenceCounting.class)
    ).toArray(i -> new Class[i]), new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if(method.getDeclaringClass().equals(ReferenceCounting.class)) {
          return method.invoke(refcounter, args);
        } else {
          return method.invoke(obj, args);
        }
      }
    });
  };

}
