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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RefIgnore
public class BytecodeVerificationException extends RuntimeException {
  @Nonnull
  private final VerificationResult result;

  public BytecodeVerificationException(@Nonnull VerificationResult result) {
    super(buildMessage(Objects.requireNonNull(result, "result")));
    this.result = result;
  }

  @Nonnull
  private static String buildMessage(@Nonnull VerificationResult result) {
    List<Diagnostic> errors = result.getErrors();
    String header = String.format(
        "Bytecode reference-counting verification failed with %d error(s) (%d warning(s)) across %d class(es):",
        errors.size(),
        result.getWarnings().size(),
        result.getAnalyzedClassCount()
    );
    String details = errors.stream()
        .map(d -> "  " + d.format())
        .collect(Collectors.joining("\n"));
    return details.isEmpty() ? header : header + "\n" + details;
  }

  @Nonnull
  public VerificationResult getResult() {
    return result;
  }

  @Nonnull
  public List<Diagnostic> getDiagnostics() {
    return Collections.unmodifiableList(result.getDiagnostics());
  }

  @Nonnull
  public List<Diagnostic> getErrors() {
    return result.getErrors();
  }
}
