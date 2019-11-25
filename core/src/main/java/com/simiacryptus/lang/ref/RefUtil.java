package com.simiacryptus.lang.ref;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.stream.Stream;

public class RefUtil {

  public static <T> void freeRef(T value) {
    if (null != value && value instanceof ReferenceCounting) ((ReferenceCounting) value).freeRef();
  }

  public static <T> T addRef(T value) {
    if (null != value && value instanceof ReferenceCounting) ((ReferenceCounting) value).addRef();
    return value;
  }

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
  }
}
