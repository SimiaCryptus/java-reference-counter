package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import java.util.function.Consumer;

public abstract class RefAwareConsumer<T> extends ReferenceCountingBase implements Consumer<T> {
}
