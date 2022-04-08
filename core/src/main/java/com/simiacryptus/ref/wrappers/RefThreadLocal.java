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

package com.simiacryptus.ref.wrappers;

import com.simiacryptus.ref.lang.*;

import javax.annotation.Nonnull;

/**
 * This class provides a reference-counting mechanism for ThreadLocal objects.
 *
 * <p>
 * When a ThreadLocal object is no longer referenced by any threads, it will be
 * automatically freed by this class.
 *
 * @author John Smith (john.smith@example.com)
 * @version 1.0
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public class RefThreadLocal<T> extends ThreadLocal<T> implements ReferenceCounting {

  private final ReferenceCountingBase refCounter = new ReferenceCountingBase() {
    /**
     * Frees this object.
     *
     *   @docgenVersion 9
     */
    @Override
    protected void _free() {
      super._free();
      RefThreadLocal.this._free();
    }
  };

  /**
   * @return whether or not the object has been freed
   * @docgenVersion 9
   */
  @Override
  public boolean isFreed() {
    return refCounter.isFreed();
  }

  /**
   * Adds a reference to this object.
   *
   * @return a reference to this object
   * @docgenVersion 9
   */
  @Override
  public ReferenceCounting addRef() {
    return refCounter.addRef();
  }

  /**
   * @return whether the object is still alive
   * @docgenVersion 9
   */
  @Override
  public boolean assertAlive() {
    return refCounter.assertAlive();
  }

  /**
   * @return the current number of references to this object
   * @docgenVersion 9
   */
  @Override
  public int currentRefCount() {
    return refCounter.currentRefCount();
  }

  /**
   * @return a new {@link ReferenceCounting} instance with the same settings as this instance, but with its reference count set to 0
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public ReferenceCounting detach() {
    return refCounter.detach();
  }

  /**
   * This method returns the number of references that are currently being used.
   *
   * @return the number of references that are currently being used
   * @docgenVersion 9
   */
  @Override
  public int freeRef() {
    return refCounter.freeRef();
  }

  /**
   * Attempts to add a reference to this object.
   *
   * @return true if the reference was added, false otherwise
   * @docgenVersion 9
   */
  @Override
  public boolean tryAddRef() {
    return refCounter.tryAddRef();
  }

  /**
   * Returns the element at the specified position in this list.
   *
   * @param index index of the element to return
   * @return the element at the specified position in this list
   * @throws IndexOutOfBoundsException if the index is out of range (<tt>index &lt; 0 || index &gt;= size()</tt>)
   * @docgenVersion 9
   */
  public T get() {
    T t = super.get();
    if (null == t) return null;
    if (t instanceof ReferenceCounting) {
      if (!((ReferenceCounting) t).tryAddRef()) {
        this.remove();
        return get();
      }
    }
    return t;

  }

  /**
   * Sets the value of this {@link Ref} to the specified {@link RefAware} object.
   *
   * @param t the {@link RefAware} object to which this {@link Ref} should be set
   * @docgenVersion 9
   */
  public void set(@RefAware T t) {
    RefUtil.freeRef(super.get());
    super.set(t);
  }

  /**
   * Removes the current element from the list, if present.
   * This method sets the current element to null.
   *
   * @docgenVersion 9
   */
  @Override
  public void remove() {
    set(null);
  }

  /**
   * This method is responsible for freeing up resources.
   *
   * @docgenVersion 9
   */
  protected void _free() {
  }
}
