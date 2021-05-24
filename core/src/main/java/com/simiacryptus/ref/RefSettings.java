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

package com.simiacryptus.ref;

import com.simiacryptus.lang.Settings;
import com.simiacryptus.ref.lang.PersistanceMode;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.ReferenceCounting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.simiacryptus.lang.Settings.get;

@RefIgnore
public class RefSettings implements Settings {

  public static final int maxTracesPerObject = 100;
  private static final Logger logger = LoggerFactory.getLogger(RefSettings.class);
  public static int maxStackSize = 50;
  public static String stackPrefixFilter = "com.simiacryptus";
  @Nullable
  private static transient RefSettings INSTANCE = null;
  public final boolean watchEnable = get("WATCH_ENABLE", true);
  public final boolean watchCreation = get("WATCH_CREATE", true);
  public final boolean lifecycleDebug = get("DEBUG_LIFECYCLE", false);
  @Nonnull
  public final PersistanceMode doubleCacheMode = get("DOUBLE_CACHE_MODE", PersistanceMode.WEAK);
  private final Set<String> watchedClasses = Stream.<String>of(
//          "com.simiacryptus.mindseye.lang.ConstantResult"
//        "com.simiacryptus.mindseye.network.PipelineNetwork"
  ).filter(x -> x != null).collect(Collectors.toSet());
  private final Set<String> ignoredClasses = Stream.<String>of(
//      "com.simiacryptus.mindseye.lang.Delta",
//      "com.simiacryptus.mindseye.lang.State",
//      "com.simiacryptus.mindseye.network.InnerNode"
  ).filter(x -> x != null).collect(Collectors.toSet());

  private RefSettings() {
    System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
        Integer.toString(get("THREADS", 64)));
  }

  @Nullable
  public static RefSettings INSTANCE() {
    if (null == INSTANCE) {
      synchronized (RefSettings.class) {
        if (null == INSTANCE) {
          INSTANCE = new RefSettings();
          logger.info(String.format("Initialized %s = %s", INSTANCE.getClass().getSimpleName(), Settings.toJson(INSTANCE)));
        }
      }
    }
    return INSTANCE;
  }

  public static boolean filter(StackTraceElement stackTraceElement) {
    return stackTraceElement.getClassName().startsWith(stackPrefixFilter);
  }

  public boolean isLifecycleDebug(Class<? extends ReferenceCounting> objClass) {
    String key = objClass.getCanonicalName();
    return watchedClasses.contains(key) || (lifecycleDebug && !ignoredClasses.contains(key));
  }

}
