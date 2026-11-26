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

package com.simiacryptus.ref.bytecode.model;

import com.simiacryptus.ref.lang.RefIgnore;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RefIgnore
public final class SignatureUtil {
  private SignatureUtil() {
  }

  /**
   * Parses top-level generic type arguments from a field or variable generic signature.
   * For example, {@code Ljava/util/Optional<Lcom/example/MyRef;>;} yields {@code [Lcom/example/MyRef;]}.
   */
  @Nonnull
  public static List<Type> extractTopLevelTypeArguments(@Nullable String typeSignature) {
    if (typeSignature == null || typeSignature.isEmpty() || typeSignature.indexOf('<') < 0) {
      return Collections.emptyList();
    }
    List<Type> args = new ArrayList<>();
    try {
      new SignatureReader(typeSignature).acceptType(new SignatureVisitor(Opcodes.ASM9) {
        @Override
        public SignatureVisitor visitTypeArgument(char wildcard) {
          return new TopLevelTypeCollector(args);
        }
      });
    } catch (RuntimeException ignored) {
      return Collections.emptyList();
    }
    return args;
  }

  /**
   * Extracts the return type generic signature from a method generic signature.
   */
  @Nullable
  public static String extractMethodReturnSignature(@Nullable String methodSignature) {
    if (methodSignature == null || methodSignature.isEmpty()) {
      return null;
    }
    int closeParen = methodSignature.lastIndexOf(')');
    if (closeParen < 0 || closeParen + 1 >= methodSignature.length()) {
      return null;
    }
    String tail = methodSignature.substring(closeParen + 1);
    int throwsIdx = tail.indexOf('^');
    return throwsIdx >= 0 ? tail.substring(0, throwsIdx) : tail;
  }

  /**
   * Extracts individual parameter generic signatures from a method generic signature.
   */
  @Nonnull
  public static List<String> extractMethodParameterSignatures(@Nullable String methodSignature) {
    if (methodSignature == null || methodSignature.isEmpty()) {
      return Collections.emptyList();
    }
    int openParen = methodSignature.indexOf('(');
    int closeParen = methodSignature.lastIndexOf(')');
    if (openParen < 0 || closeParen <= openParen) {
      return Collections.emptyList();
    }
    List<String> params = new ArrayList<>();
    String inner = methodSignature.substring(openParen + 1, closeParen);
    int i = 0;
    while (i < inner.length()) {
      int start = i;
      while (i < inner.length() && inner.charAt(i) == '[') {
        i++;
      }
      if (i >= inner.length()) break;
      char c = inner.charAt(i);
      if (c == 'L' || c == 'T') {
        int depth = 0;
        while (i < inner.length()) {
          char ch = inner.charAt(i++);
          if (ch == '<') {
            depth++;
          } else if (ch == '>') {
            depth--;
          } else if (ch == ';' && depth == 0) {
            break;
          }
        }
      } else {
        i++;
      }
      params.add(inner.substring(start, i));
    }
    return params;
  }

  private static final class TopLevelTypeCollector extends SignatureVisitor {
    private final List<Type> target;
    private int arrayDimensions = 0;
    private boolean captured = false;

    TopLevelTypeCollector(List<Type> target) {
      super(Opcodes.ASM9);
      this.target = target;
    }

    @Override
    public SignatureVisitor visitArrayType() {
      if (!captured) {
        arrayDimensions++;
      }
      return this;
    }

    @Override
    public void visitClassType(String name) {
      if (!captured) {
        captured = true;
        StringBuilder desc = new StringBuilder();
        for (int i = 0; i < arrayDimensions; i++) {
          desc.append('[');
        }
        desc.append('L').append(name).append(';');
        target.add(Type.getType(desc.toString()));
      }
    }

    @Override
    public SignatureVisitor visitTypeArgument(char wildcard) {
      return new SignatureVisitor(Opcodes.ASM9) {
      };
    }
  }
}
