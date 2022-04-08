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

package com.simiacryptus.lang;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.simiacryptus.ref.lang.RefIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

/**
 * The Settings interface provides access to the application's logger.
 *
 * @docgenVersion 9
 */
@RefIgnore
public interface Settings {
  Logger logger = LoggerFactory.getLogger(Settings.class);

  /**
   * Returns a new ObjectMapper.
   *
   * @return a new ObjectMapper
   * @docgenVersion 9
   */
  static ObjectMapper getMapper() {
    return new ObjectMapper()
        //.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)
        .enable(SerializationFeature.INDENT_OUTPUT);
  }

  /**
   * Get a boolean value from the system properties.
   *
   * @param key          the key to look up
   * @param defaultValue the default value to use if the key is not found
   * @return the boolean value
   * @docgenVersion 9
   */
  static boolean get(@Nonnull final String key, final boolean defaultValue) {
    boolean value = Boolean.parseBoolean(System.getProperty(key, Boolean.toString(defaultValue)));
    logger.info(String.format("%s = %s", key, value));
    return value;
  }

  /**
   * Gets the value of the given key, or the default value if the key is not found.
   *
   * @param key          the key to look up
   * @param defaultValue the default value to use if the key is not found
   * @return the value of the key, or the default value if the key is not found
   * @docgenVersion 9
   */
  @Nonnull
  static <T extends Enum<T>> T get(@Nonnull final String key,
                                   @Nonnull final T defaultValue) {
    T value = Enum.valueOf(defaultValue.getDeclaringClass(),
        System.getProperty(key, defaultValue.toString()));
    logger.info(String.format("%s = %s", key, value));
    return value;
  }

  /**
   * Returns the value for the given key, or the default value if the key is not found.
   *
   * @param key          the key to look up
   * @param defaultValue the value to return if the key is not found
   * @return the value for the given key, or the default value if the key is not found
   * @docgenVersion 9
   */
  static URI get(@Nonnull final String key,
                 final URI defaultValue) {
    String value = System.getProperty(key);
    if (null == value) {
      logger.info(String.format("%s = %s", key, defaultValue));
      return defaultValue;
    } else {
      logger.info(String.format("%s = %s", key, value));
      try {
        return new URI(value);
      } catch (URISyntaxException e) {
        logger.warn(String.format("Error parsing %s = %s", key, value), e);
        return defaultValue;
      }
    }
  }

  /**
   * Get a system property, returning a default value if not set.
   *
   * @param key          the system property key
   * @param defaultValue the default value to return if the property is not set
   * @return the system property value, or the default value if not set
   * @docgenVersion 9
   */
  static String get(@Nonnull final String key,
                    final String defaultValue) {
    String value = System.getProperty(key, defaultValue);
    logger.info(String.format("%s = %s", key, value));
    return value;
  }

  /**
   * Returns the value mapped by the specified key, or the default value if the map contains no mapping for the key.
   *
   * @param key          the key whose value is to be returned
   * @param defaultValue the default value to be returned if the map contains no mapping for the specified key
   * @return the value mapped by the specified key, or the default value if the map contains no mapping for the key
   * @docgenVersion 9
   */
  static int get(@Nonnull final String key, final int defaultValue) {
    String property = System.getProperty(key, Integer.toString(defaultValue));
    try {
      logger.info(String.format("%s = %s", key, property));
      return Integer.parseInt(property);
    } catch (Exception e) {
      logger.warn(String.format("Error parsing %s = %s", key, property), e);
      return defaultValue;
    }
  }

  /**
   * Returns the value associated with the given key, or the default value if the key is not found.
   *
   * @param key          the key to look up
   * @param defaultValue the value to return if the key is not found
   * @return the value associated with the given key, or the default value if the key is not found
   * @docgenVersion 9
   */
  static double get(@Nonnull final String key, final double defaultValue) {
    String property = System.getProperty(key, Double.toString(defaultValue));
    try {
      double value = Double.parseDouble(property);
      logger.info(String.format("%s = %s", key, property));
      return value;
    } catch (Exception e) {
      logger.warn(String.format("Error parsing %s = %s", key, property), e);
      return defaultValue;
    }
  }

  /**
   * Returns the value of the given key, or the default value if the key is not found.
   *
   * @param key          the key to look up
   * @param defaultValue the value to return if the key is not found
   * @return the value of the given key, or the default value if the key is not found
   * @docgenVersion 9
   */
  static long get(@Nonnull final String key, final long defaultValue) {
    String property = System.getProperty(key, Long.toString(defaultValue));
    try {
      long value = Long.parseLong(property);
      logger.info(String.format("%s = %s", key, value));
      return value;
    } catch (Exception e) {
      logger.warn(String.format("Error parsing %s = %s", key, property), e);
      return defaultValue;
    }
  }

  /**
   * Converts the given object to a JSON string using the given ObjectMapper.
   *
   * @param obj    the object to convert to a JSON string
   * @param mapper the ObjectMapper to use
   * @return the JSON string representation of the given object
   * @docgenVersion 9
   */
  @Nonnull
  static CharSequence toJson(final Object obj) {
    return toJson(obj, getMapper());
  }

  /**
   * Converts the given object to a JSON string using the given object mapper.
   *
   * @param obj          the object to convert
   * @param objectMapper the object mapper to use
   * @return the JSON string
   * @throws NullPointerException if obj or objectMapper is null
   * @docgenVersion 9
   */
  @Nonnull
  static CharSequence toJson(final Object obj,
                             @Nonnull final ObjectMapper objectMapper) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      objectMapper.writeValue(outputStream, obj);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new String(outputStream.toByteArray(), Charset.forName("UTF-8"));
  }
}
