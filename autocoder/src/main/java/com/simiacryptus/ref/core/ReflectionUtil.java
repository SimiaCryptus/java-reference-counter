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

package com.simiacryptus.ref.core;

import org.apache.commons.lang3.ClassUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * This is the ReflectionUtil class.
 *
 * @docgenVersion 9
 */
public class ReflectionUtil {

  /**
   * Returns the value of the field with the given name from the given object.
   *
   * @param obj  the object to get the field value from
   * @param name the name of the field to get the value of
   * @return the value of the field, or null if the field does not exist
   * @docgenVersion 9
   */
  @Nullable
  public static <T> T getField(@Nonnull Object obj, String name) {
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

  /**
   * @Nonnull public static <T> T invokeMethod(@Nonnull Object obj, String name, @Nonnull Object... args);
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> T invokeMethod(@Nonnull Object obj, String name, @Nonnull Object... args) {
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

  /**
   * Returns the field with the given name in the given node class.
   *
   * @param nodeClass the node class to search in
   * @param name      the name of the field to search for
   * @return the field with the given name in the given node class
   * @throws NullPointerException if nodeClass or name is null
   * @docgenVersion 9
   */
  @Nonnull
  public static Field getField(@Nonnull Class<?> nodeClass, String name) {
    final Field[] fields = nodeClass.getDeclaredFields();
    final Optional<Field> parent = Arrays.stream(fields).filter(x -> x.getName().equals(name)).findFirst();
    if (!parent.isPresent()) {
      final Class<?> superclass = nodeClass.getSuperclass();
      if (superclass != null) {
        return getField(superclass, name);
      } else {
        throw new AssertionError(String.format("Cannot find field %s", name));
      }
    }
    final Field field = parent.get();
    field.setAccessible(true);
    return field;
  }

  /**
   * Sets the value of the field with the given name in the given AST node to the given value.
   *
   * @param astNode the AST node whose field is to be set
   * @param name    the name of the field to be set
   * @param value   the value to set the field to
   * @return the AST node with the given field set to the given value
   * @throws RuntimeException if the field cannot be set
   * @docgenVersion 9
   */
  @SuppressWarnings("unused")
  @Nonnull
  public static <T> T setField(@Nonnull T astNode, String name, Object value) {
    try {
      getField(astNode.getClass(), name).set(astNode, value);
      return astNode;
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
