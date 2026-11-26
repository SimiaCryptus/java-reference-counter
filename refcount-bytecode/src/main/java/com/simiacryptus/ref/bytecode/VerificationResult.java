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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.simiacryptus.ref.lang.RefIgnore;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RefIgnore
public final class VerificationResult {
  @Nonnull
  private final List<Diagnostic> diagnostics;
  private final int analyzedClassCount;
  private final int analyzedMethodCount;
  private final int refCountedClassCount;
  @Nonnull
  private final Map<SourceLanguage, Integer> languageBreakdown;

  public VerificationResult(
      @Nonnull List<Diagnostic> diagnostics,
      int analyzedClassCount,
      int analyzedMethodCount,
      int refCountedClassCount,
      @Nonnull Map<SourceLanguage, Integer> languageBreakdown
  ) {
    this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
    this.analyzedClassCount = analyzedClassCount;
    this.analyzedMethodCount = analyzedMethodCount;
    this.refCountedClassCount = refCountedClassCount;
    Map<SourceLanguage, Integer> copy = new EnumMap<>(SourceLanguage.class);
    copy.putAll(languageBreakdown);
    this.languageBreakdown = Collections.unmodifiableMap(copy);
  }

  @Nonnull
  public List<Diagnostic> getDiagnostics() {
    return diagnostics;
  }

  @Nonnull
  public List<Diagnostic> getErrors() {
    return diagnostics.stream()
        .filter(d -> d.getSeverity() == Severity.ERROR)
        .collect(Collectors.toList());
  }

  @Nonnull
  public List<Diagnostic> getWarnings() {
    return diagnostics.stream()
        .filter(d -> d.getSeverity() == Severity.WARNING)
        .collect(Collectors.toList());
  }

  @Nonnull
  public List<Diagnostic> getByRule(@Nonnull RuleId ruleId) {
    return diagnostics.stream()
        .filter(d -> d.getRuleId() == ruleId)
        .collect(Collectors.toList());
  }

  public int getAnalyzedClassCount() {
    return analyzedClassCount;
  }

  public int getAnalyzedMethodCount() {
    return analyzedMethodCount;
  }

  public int getRefCountedClassCount() {
    return refCountedClassCount;
  }

  @Nonnull
  public Map<SourceLanguage, Integer> getLanguageBreakdown() {
    return languageBreakdown;
  }

  public boolean hasErrors() {
    return diagnostics.stream().anyMatch(d -> d.getSeverity() == Severity.ERROR);
  }

  public void throwIfErrors() {
    if (hasErrors()) {
      throw new BytecodeVerificationException(this);
    }
  }

  @Nonnull
  public String formatSummary() {
    String langs = languageBreakdown.entrySet().stream()
        .filter(e -> e.getValue() > 0)
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining(", "));
    return String.format(
        "Analyzed %d class(es) (%d ref-counted, %d method(s)%s): %d error(s), %d warning(s)",
        analyzedClassCount,
        refCountedClassCount,
        analyzedMethodCount,
        langs.isEmpty() ? "" : "; languages: " + langs,
        getErrors().size(),
        getWarnings().size()
    );
  }

  @Nonnull
  public String formatReport() {
    StringBuilder sb = new StringBuilder();
    sb.append("=== Reference-Counting Bytecode Verification Report ===\n");
    sb.append(formatSummary()).append('\n');
    if (!diagnostics.isEmpty()) {
      sb.append("\nDiagnostics:\n");
      for (Diagnostic diagnostic : diagnostics) {
        sb.append("  ").append(diagnostic.format()).append('\n');
      }
    }
    return sb.toString();
  }

  @Nonnull
  public String toJson() {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("analyzedClassCount", analyzedClassCount);
    payload.put("analyzedMethodCount", analyzedMethodCount);
    payload.put("refCountedClassCount", refCountedClassCount);
    payload.put("errorCount", getErrors().size());
    payload.put("warningCount", getWarnings().size());
    payload.put("languageBreakdown", languageBreakdown);
    payload.put("diagnostics", diagnostics);
    try {
      ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
      return mapper.writeValueAsString(payload);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to serialize VerificationResult to JSON", e);
    }
  }
}
