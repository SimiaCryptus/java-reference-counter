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

@RefIgnore
public class RefSettings implements Settings {

  public static final int maxTracesPerObject = 100;
  private static final Logger logger = LoggerFactory.getLogger(RefSettings.class);
  public static int maxStackSize = 50;
  @Nullable
  private static transient RefSettings INSTANCE = null;
  private static String stackPrefixFilter = "com.simiacryptus";
  public final boolean watchEnable;
  public final boolean watchCreation;
  private final boolean lifecycleDebug;
  @Nonnull
  private final PersistanceMode doubleCacheMode;
  private final Set<String> watchedClasses;
  private final Set<String> ignoredClasses;

  protected RefSettings() {
    System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", Integer.toString(Settings.get("THREADS", 64)));
    this.doubleCacheMode = Settings.get("DOUBLE_CACHE_MODE", PersistanceMode.WEAK);
    this.lifecycleDebug = Settings.get("DEBUG_LIFECYCLE", false);
    this.watchCreation = Settings.get("WATCH_CREATE", false);
    this.watchEnable = Settings.get("WATCH_ENABLE", true);
    this.ignoredClasses = Stream.<String>of(
        "com.simiacryptus.mindseye.lang.Delta",
        "com.simiacryptus.mindseye.lang.State",
        "com.simiacryptus.mindseye.network.InnerNode"
//        "com.simiacryptus.mindseye.lang.Tensor"
//    ).map(name -> {
//      try {
//        return Class.forName(name);
//      } catch (ClassNotFoundException e) {
//        logger.warn("No Class Found: " + name);
//        return null;
//      }
//    }
    ).filter(x -> x != null).collect(Collectors.toSet());
    this.watchedClasses = Stream.<String>of(
//        "com.simiacryptus.mindseye.network.PipelineNetwork"
//        "com.simiacryptus.mindseye.lang.cudnn.CudnnHandle"
//        "com.simiacryptus.mindseye.art.util.VisualStyleContentNetwork.TileTrainer",
//        "com.simiacryptus.mindseye.lang.Tensor"
//        "com.simiacryptus.mindseye.network.PipelineNetwork",
//        "com.simiacryptus.mindseye.network.CountingResult",
//        "com.simiacryptus.mindseye.lang.PointSample",
//        "com.simiacryptus.mindseye.lang.cudnn.CudaTensorList",
//        "com.simiacryptus.mindseye.network.InnerNode",
//        "com.simiacryptus.mindseye.layers.cudnn.GramianLayer",
//        "com.simiacryptus.mindseye.layers.java.ImgTileSelectLayer",
//        "com.simiacryptus.mindseye.layers.ValueLayer",
//        "com.simiacryptus.mindseye.layers.java.ImgPixelGateLayer",
//        "com.simiacryptus.mindseye.network.PipelineNetwork",
//        "com.simiacryptus.mindseye.lang.StateSet",
//        "com.simiacryptus.mindseye.lang.DeltaSet",
//        "com.simiacryptus.mindseye.lang.State",
//        "com.simiacryptus.mindseye.lang.Delta",
//        "com.simiacryptus.mindseye.lang.Tensor",
//        "com.simiacryptus.mindseye.network.GraphEvaluationContext",
//        "com.simiacryptus.mindseye.layers.cudnn.PoolingLayer",
//        "com.simiacryptus.mindseye.lang.TensorArray",
//        "com.simiacryptus.mindseye.lang.cudnn.CudaTensor"
//    ).map(name -> {
//      try {
//        return Class.forName(name);
//      } catch (ClassNotFoundException e) {
//        logger.warn("No Class Found: " + name);
//        return null;
//      }
//    }
    ).filter(x -> x != null).collect(Collectors.toSet());
  }

  @Nonnull
  public PersistanceMode getDoubleCacheMode() {
    return doubleCacheMode;
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
