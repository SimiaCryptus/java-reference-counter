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

package com.simiacryptus.ref.bytecode;

import com.simiacryptus.ref.lang.RefIgnore;

import javax.annotation.Nonnull;

@RefIgnore
public enum RuleId {
  NON_STATIC_INNER_REFCOUNTED("Non-static inner class implements ReferenceCounting"),
  UNACCOUNTABLE_REF_FIELD("Non-reference-counted class declares reference-counted instance field"),
  MISSING_FREE_METHOD("Concrete ReferenceCountingBase subclass with reference-counted fields omits _free()"),
  MISSING_MUST_CALL_SUPER("Override of @MustCall method does not invoke super implementation"),
  MISSING_REF_AWARE_OVERRIDE("Method override omits @RefAware annotation declared on super parameter"),
  FIELD_NOT_FREED_IN_FREE("Reference-counted instance field is not freed in _free()"),
  FIELD_FREED_OUTSIDE_FREE("Reference-counted instance field is freed outside _free() without reassignment"),
  FIELD_OVERWRITTEN_WITHOUT_FREE("Reference-counted instance field is overwritten without freeing previous reference"),
  UNFREED_REFERENCE("Owned reference is not freed or transferred before method exit"),
  DISCARDED_REFERENCE("Owned reference result is discarded without calling freeRef()"),
  DOUBLE_FREE("Reference is freed or consumed multiple times"),
  USE_AFTER_FREE("Reference is accessed after being freed or consumed"),
  UNOWNED_REFERENCE_CONSUMED("Borrowed/unowned reference is freed, stored, or transferred without addRef()"),
  BRANCH_CONSUMPTION_MISMATCH("Reference consumption differs across control-flow branches"),
  BLIND_PARAMETER_PASS("Reference-counted value passed to non-@RefAware and non-ReferenceCounting parameter"),
  RETURN_LOSES_REF_AWARENESS("Owned reference returned as non-ReferenceCounting type without @RefAware"),
  UNSAFE_CLOSURE_CAPTURE("Reference-counted value captured by non-reference-counted closure leaks or mismanages ownership");

  @Nonnull
  private final String description;

  RuleId(@Nonnull String description) {
    this.description = description;
  }

  @Nonnull
  public String getDescription() {
    return description;
  }
}
