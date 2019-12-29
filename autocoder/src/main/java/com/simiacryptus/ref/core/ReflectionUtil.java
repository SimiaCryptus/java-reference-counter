package com.simiacryptus.ref.core;

import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ReflectionUtil {

  public static <T> T getField(Object obj, String name) {
    final Field value = Arrays.stream(obj.getClass().getDeclaredFields()).filter(x -> x.getName().equals(name)).findFirst().orElse(null);
    if (value != null) {
      value.setAccessible(true);
      try {
        return (T) value.get(obj);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  public static <T> T invokeMethod(Object obj, String name, Object... args) {
    final Method value = Arrays.stream(obj.getClass().getDeclaredMethods())
        .filter(x -> x.getName().equals(name))
        .filter(x -> {
          final Class<?>[] parameterTypes = x.getParameterTypes();
          if (parameterTypes.length != args.length) return false;
          for (int i = 0; i < parameterTypes.length; i++) {
            if (!ClassUtils.isAssignable(parameterTypes[i], args[i].getClass())) return false;
          }
          return true;
        })
        .findFirst()
        .orElse(null);
    if (value != null) {
      value.setAccessible(true);
      try {
        return (T) value.invoke(obj, args);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }
    throw new RuntimeException(String.format("Method %s.%s(%s) not found", obj.getClass(), name, Arrays.stream(args).map(x -> x.getClass().getSimpleName()).reduce((a, b) -> a + ", " + b).get()));
  }
}
