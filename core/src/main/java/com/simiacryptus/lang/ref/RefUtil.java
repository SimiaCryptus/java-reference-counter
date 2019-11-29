package com.simiacryptus.lang.ref;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class RefUtil {

  public static <T> void freeRef(@Nullable T value) {
    if (null != value) {
      if (value instanceof ReferenceCounting) {
        ((ReferenceCounting) value).freeRef();
      } else if (value instanceof Optional) {
        final Optional optional = (Optional) value;
        if(optional.isPresent()) freeRef(optional.get());
      }
    }
  }

  @Nullable
  public static <T> T addRef(@Nullable T value) {
    if (null != value && value instanceof ReferenceCounting) ((ReferenceCounting) value).addRef();
    return value;
  }

  public static <T> T wrapInterface(T obj, @NotNull ReferenceCounting... refs) {
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
      public Object invoke(Object proxy, @NotNull Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass().equals(ReferenceCounting.class)) {
          return method.invoke(refcounter, args);
        } else {
          return method.invoke(obj, args);
        }
      }
    });
  }
}
