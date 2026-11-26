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
import javax.annotation.Nullable;
import java.util.Objects;

@RefIgnore
public final class Diagnostic {
  @Nonnull
  private final RuleId ruleId;
  @Nonnull
  private final Severity severity;
  @Nonnull
  private final String className;
  @Nullable
  private final String methodName;
  @Nullable
  private final String methodDescriptor;
  @Nullable
  private final String sourceFile;
  private final int lineNumber;
  @Nonnull
  private final SourceLanguage language;
  @Nullable
  private final String originPath;
  @Nonnull
  private final String message;

  public Diagnostic(
      @Nonnull RuleId ruleId,
      @Nonnull Severity severity,
      @Nonnull String className,
      @Nullable String methodName,
      @Nullable String methodDescriptor,
      @Nullable String sourceFile,
      int lineNumber,
      @Nonnull SourceLanguage language,
      @Nullable String originPath,
      @Nonnull String message
  ) {
    this.ruleId = Objects.requireNonNull(ruleId, "ruleId");
    this.severity = Objects.requireNonNull(severity, "severity");
    this.className = Objects.requireNonNull(className, "className");
    this.methodName = methodName;
    this.methodDescriptor = methodDescriptor;
    this.sourceFile = sourceFile;
    this.lineNumber = lineNumber;
    this.language = Objects.requireNonNull(language, "language");
    this.originPath = originPath;
    this.message = Objects.requireNonNull(message, "message");
  }

  @Nonnull
  public RuleId getRuleId() {
    return ruleId;
  }

  @Nonnull
  public Severity getSeverity() {
    return severity;
  }

  @Nonnull
  public String getClassName() {
    return className;
  }

  @Nullable
  public String getMethodName() {
    return methodName;
  }

  @Nullable
  public String getMethodDescriptor() {
    return methodDescriptor;
  }

  @Nullable
  public String getSourceFile() {
    return sourceFile;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  @Nonnull
  public SourceLanguage getLanguage() {
    return language;
  }

  @Nullable
  public String getOriginPath() {
    return originPath;
  }

  @Nonnull
  public String getMessage() {
    return message;
  }

  @Nonnull
  public String formatLocation() {
    String filePart = sourceFile != null && !sourceFile.isEmpty()
        ? sourceFile
        : className.substring(className.lastIndexOf('.') + 1) + ".class";
    if (lineNumber > 0) {
      return filePart + ":" + lineNumber;
    }
    return filePart;
  }

  @Nonnull
  public String format() {
    StringBuilder sb = new StringBuilder();
    sb.append('[').append(severity).append("] ");
    sb.append('[').append(ruleId).append("] ");
    sb.append('(').append(language).append(") ");
    sb.append(formatLocation()).append(" - ");
    sb.append(className);
    if (methodName != null && !methodName.isEmpty()) {
      sb.append('#').append(methodName);
    }
    sb.append(": ").append(message);
    return sb.toString();
  }

  @Nonnull
  @Override
  public String toString() {
    return format();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Diagnostic)) return false;
    Diagnostic that = (Diagnostic) o;
    return lineNumber == that.lineNumber
        && ruleId == that.ruleId
        && severity == that.severity
        && className.equals(that.className)
        && Objects.equals(methodName, that.methodName)
        && Objects.equals(methodDescriptor, that.methodDescriptor)
        && Objects.equals(sourceFile, that.sourceFile)
        && language == that.language
        && message.equals(that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ruleId, severity, className, methodName, methodDescriptor, sourceFile, lineNumber, language, message);
  }
}
