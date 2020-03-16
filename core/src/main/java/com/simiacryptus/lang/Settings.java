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

@RefIgnore
public interface Settings {
  Logger logger = LoggerFactory.getLogger(Settings.class);

  static ObjectMapper getMapper() {
    return new ObjectMapper()
        //.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)
        .enable(SerializationFeature.INDENT_OUTPUT);
  }

  static boolean get(@Nonnull final String key, final boolean defaultValue) {
    boolean value = Boolean.parseBoolean(System.getProperty(key, Boolean.toString(defaultValue)));
    logger.info(String.format("%s = %s", key, value));
    return value;
  }

  @Nonnull
  static <T extends Enum<T>> T get(@Nonnull final String key,
                                   @Nonnull final T defaultValue) {
    T value = Enum.valueOf(defaultValue.getDeclaringClass(),
        System.getProperty(key, defaultValue.toString()));
    logger.info(String.format("%s = %s", key, value));
    return value;
  }

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
        throw new RuntimeException(e);
      }
    }
  }

  static String get(@Nonnull final String key,
                    final String defaultValue) {
    String value = System.getProperty(key, defaultValue);
    logger.info(String.format("%s = %s", key, value));
    return value;
  }

  static int get(@Nonnull final String key, final int defaultValue) {
    int value = Integer.parseInt(System.getProperty(key, Integer.toString(defaultValue)));
    logger.info(String.format("%s = %s", key, value));
    return value;
  }

  static double get(@Nonnull final String key, final double defaultValue) {
    double value = Double.parseDouble(System.getProperty(key, Double.toString(defaultValue)));
    logger.info(String.format("%s = %s", key, value));
    return value;
  }

  static long get(@Nonnull final String key, final long defaultValue) {
    long value = Long.parseLong(System.getProperty(key, Long.toString(defaultValue)));
    logger.info(String.format("%s = %s", key, value));
    return value;
  }

  @Nonnull
  static CharSequence toJson(final Object obj) {
    return toJson(obj, getMapper());
  }

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
