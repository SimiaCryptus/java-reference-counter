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
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.function.Supplier;

@RefIgnore
@SuppressWarnings("unused")
public enum PersistanceMode {
  SOFT {
    @Override
    public <T> Supplier<T> wrap(@RefAware T obj) {
      SoftReference<T> softReference = new SoftReference<>(obj);
      return () -> softReference.get();
    }
  },
  WEAK {
    @Override
    public <T> Supplier<T> wrap(@RefAware T obj) {
      WeakReference<T> weakReference = new WeakReference<>(obj);
      return () -> weakReference.get();
    }
  },
  STRONG {
    @Nonnull
    @Override
    public <T> Supplier<T> wrap(@Nonnull @RefAware T obj) {
      return () -> obj;
    }
  },
  NULL {
    @Nonnull
    @Override
    public <T> Supplier<T> wrap(@RefAware T obj) {
      return () -> null;
    }
  };

  @Nullable
  public abstract <T> Supplier<T> wrap(@RefAware T obj);
}
