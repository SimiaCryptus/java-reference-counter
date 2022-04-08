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

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class contains utility methods for working with collections.
 *
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public class RefCollections {

  /**
   * Returns a stream of the inner collection.
   *
   * @param c the collection
   * @return a stream of the inner collection
   * @docgenVersion 9
   */
  public static <T> Stream<T> getInnerStream(@Nonnull @RefAware Collection<T> c) {
    final Stream<T> stream = getInnerCollection(c).stream();
    assert !(stream instanceof RefStream);
    return stream;
  }

  /**
   * Returns the inner collection of the given collection. If the collection is not a RefCollection,
   * the collection itself is returned.
   *
   * @param c the collection to get the inner collection from
   * @return the inner collection of the given collection
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> Collection<T> getInnerCollection(@Nonnull @RefAware Collection<T> c) {
    if (c instanceof RefCollection) {
      return ((RefCollection<T>) c).getInner();
    } else {
      return c;
    }
  }

  /**
   * Returns an unmodifiable view of the specified RefMap. This method allows modules to provide users with "read-only" access to internal RefMaps. Query operations on the returned RefMap "read through" to the specified RefMap, and attempts to modify the returned RefMap, whether direct or via its collection views, result in an UnsupportedOperationException.
   *
   * @param map the RefMap for which an unmodifiable view is to be returned.
   * @return an unmodifiable view of the specified RefMap.
   * @docgenVersion 9
   */
  @Nonnull
  public static <K, V> RefMap<K, V> unmodifiableMap(@Nonnull RefMap<K, V> map) {
    return map; // TODO: Implement Me
  }

  /**
   * Shuffles the given list.
   *
   * @param list the list to shuffle
   * @param <T>  the type of the list elements
   * @docgenVersion 9
   */
  public static <T> void shuffle(@Nonnull @RefAware List<T> list) {
    Collections.shuffle(list);
    RefUtil.freeRef(list);
  }

  /**
   * Returns an unmodifiable view of the specified list. This method allows
   * modules to provide users with "read-only" access to internal lists. Query
   * operations on the returned list "read through" to the specified list, and
   * attempts to modify the returned list, whether direct or via its iterator,
   * result in an {@code UnsupportedOperationException}.
   *
   * <p>The returned list is backed by the specified list, so changes to the
   * specified list are reflected in the returned list, and vice-versa. If the
   * specified list is modified while an iteration over the returned list is in
   * progress (except through the iterator's own {@code remove} operation), the
   * results of the iteration are undefined.
   *
   * <p>The returned list supports all of the optional list operations.
   *
   * @param <T>  the type of elements in the list
   * @param list the list for which an unmodifiable view is to be returned
   * @return an unmodifiable view of the specified list
   * @docgenVersion 9
   */
  public static <T> RefList<T> unmodifiableList(RefList<T> list) {
    return list; // TODO: Implement me
  }

  /**
   * Returns a synchronized (thread-safe) list backed by the specified
   * list. In order to guarantee serial access, it is critical that
   * all access to the backing list is accomplished through the returned
   * list.
   * <p>
   * It is imperative that the user manually synchronize on the returned
   * list when iterating over it:
   * <p>
   * RefList<String> list = RefCollections.synchronizedList(new RefArrayList<String>());
   * ...
   * synchronized (list) {
   * Iterator<String> i = list.iterator(); // Must be in synchronized block
   * while (i.hasNext())
   * foo(i.next());
   * }
   * <p>
   * Failure to follow this advice may result in non-deterministic behavior.
   *
   * @param <T>  the class of the objects in the list
   * @param list the list to be "wrapped" in a synchronized list.
   * @return a synchronized view of the specified list.
   * @docgenVersion 9
   */
  public static <T> RefList<T> synchronizedList(RefList<T> list) {
    return list; // TODO: Implement me
  }
}
