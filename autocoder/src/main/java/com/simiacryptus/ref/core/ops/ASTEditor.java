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

package com.simiacryptus.ref.core.ops;

import com.simiacryptus.ref.core.ASTUtil;
import com.simiacryptus.ref.core.AutoCoder;
import com.simiacryptus.ref.core.ProjectInfo;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The type Ast editor.
 */
public abstract class ASTEditor extends LoggingASTVisitor {
  /**
   * The Project info.
   */
  protected final ProjectInfo projectInfo;
  /**
   * The Initial content.
   */
  protected final String initialContent;
  @Nullable
  private ASTMapping reparsed = null;

  /**
   * Instantiates a new Ast editor.
   *
   * @param compilationUnit the compilation unit
   * @param projectInfo     the project info
   * @param file            the file
   */
  public ASTEditor(@NotNull CompilationUnit compilationUnit, ProjectInfo projectInfo, @Nonnull File file) {
    super(compilationUnit, file);
    this.projectInfo = projectInfo;
    this.initialContent = AutoCoder.read(this.file);
  }

  /**
   * Instantiates a new Ast editor.
   *
   * @param projectInfo     the project info
   * @param compilationUnit the compilation unit
   * @param file            the file
   * @param record          the record
   */
  public ASTEditor(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file, boolean record) {
    this(compilationUnit, projectInfo, file);
    if (record) compilationUnit.recordModifications();
  }

  /**
   * Gets reparsed.
   *
   * @return the reparsed
   */
  @Nullable
  public ASTMapping getReparsed() {
    return reparsed;
  }

  /**
   * Sets reparsed.
   *
   * @param reparsed the reparsed
   */
  protected void setReparsed(ASTMapping reparsed) {
    this.reparsed = reparsed;
  }

  /**
   * Write boolean.
   *
   * @param format the format
   * @return the boolean
   */
  public boolean write(boolean format) {
    final String finalSrc = updateContent();
    if (initialContent.equals(finalSrc)) return false;
    write(format ? ASTUtil.format(finalSrc) : finalSrc);
    return true;
  }

  /**
   * Revert boolean.
   *
   * @return the boolean
   */
  public boolean revert() {
    final String currentContent = AutoCoder.read(this.file);
    if (currentContent.equals(initialContent)) return false;
    write(initialContent);
    return true;
  }

  /**
   * Write final boolean.
   *
   * @param format the format
   * @return the boolean
   */
  public boolean writeFinal(boolean format) {
    update(true, format);
    return !AutoCoder.read(this.file).equals(initialContent);
  }

  /**
   * Copy if attached t.
   *
   * @param <T>  the type parameter
   * @param node the node
   * @return the t
   */
  @NotNull
  protected <T extends ASTNode> T copyIfAttached(@NotNull T node) {
    if (node.getParent() == null) {
      return node;
    } else {
      debug(1, node, "Copy node %s", node);
      return (T) ASTNode.copySubtree(ast, node);
    }
  }

  /**
   * Replace.
   *
   * @param child    the child
   * @param newChild the new child
   */
  protected final void replace(@NotNull ASTNode child, ASTNode newChild) {
    final ASTNode parent = child.getParent();
    if (parent instanceof QualifiedName) {
      final QualifiedName qualifiedName = (QualifiedName) parent;
      if (qualifiedName.getQualifier().equals(child)) {
        if (!(newChild instanceof Name) && (newChild instanceof Expression)) {
          final FieldAccess fieldAccess = ast.newFieldAccess();
          fieldAccess.setExpression(copyIfAttached((Expression) newChild));
          fieldAccess.setName(copyIfAttached(qualifiedName.getName()));
          debug(child, "Replacing %s with %s", child, newChild);
          replace(parent, fieldAccess);
          return;
        }
      }
    }
    StructuralPropertyDescriptor location = child.getLocationInParent();
    if (location != null) {
      debug(1, child, "Replace %s with %s within %s", child, newChild, location);
      if (location.isChildListProperty()) {
        List list = (List) parent.getStructuralProperty(location);
        list.set(list.indexOf(child), newChild);
      } else if (location.isChildProperty()) {
        parent.setStructuralProperty(location, newChild);
      } else {
        warn(child, "Failed to replace %s with %s", child, newChild);
      }
    } else {
      warn(child, "Failed to replace %s with %s", child, newChild);
    }
  }

  /**
   * Update ast mapping.
   *
   * @param <T>    the type parameter
   * @param write  the write
   * @param format the format
   * @return the ast mapping
   */
  @NotNull
  protected <T extends ASTNode> ASTMapping update(boolean write, boolean format) {
    if (write) {
      try {
        write(format);
      } catch (Throwable e) {
        logger.warn("Error writing source", e);
      }
    }
    final CompilationUnit reparse = read();
    final ASTMapping align = ASTUtil.align(compilationUnit, reparse);
    setReparsed(align);
    align.errors.stream().forEach(x -> warnRaw(0, compilationUnit, x));
    if (!align.mismatches.isEmpty()) {
      return repairAndUpdate(format, reparse, align);
    }
    return align;
  }

  /**
   * Write.
   *
   * @param data the data
   */
  protected void write(String data) {
    try {
      synchronized (ProjectInfo.class) {
        logger.info(String.format("Writing %s", file));
        FileUtils.write(file, data, "UTF-8");
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets span.
   *
   * @param node the node
   * @return the span
   */
  @NotNull
  protected ASTEditor.Span getSpan(@NotNull ASTNode node) {
    final int startPosition = node.getStartPosition();
    final int length = node.getLength();

    return new Span(
        file,
        compilationUnit.getLineNumber(startPosition),
        compilationUnit.getColumnNumber(startPosition),
        compilationUnit.getLineNumber(startPosition + length),
        compilationUnit.getColumnNumber(startPosition + length)
    );
  }

  @NotNull
  private ASTMapping repairAndUpdate(boolean format, @NotNull CompilationUnit compilationUnit0, @NotNull ASTMapping align0) {
    return repairAndUpdate(format, compilationUnit0, align0, 3);
  }

  @NotNull
  private ASTMapping repairAndUpdate(boolean format, @NotNull CompilationUnit compilationUnit0, @NotNull ASTMapping align0, int retries) {
    compilationUnit0.recordModifications();
    align0.mismatches.forEach((from, to) -> {
      replace(to, ASTNode.copySubtree(to.getAST(), from));
    });
    final String content0 = AutoCoder.read(this.file);
    final String content1 = ASTUtil.updateContent(content0, compilationUnit0);
    if (content0.equals(content1)) {
      throw new RuntimeException("ASTNode fixups did not change document");
    }
    write(format ? ASTUtil.format(content1) : content1);
    final CompilationUnit compilationUnit1 = read();
    final ASTMapping align1 = ASTUtil.align(this.compilationUnit, compilationUnit1);
    align1.errors.stream().forEach(x -> warnRaw(0, this.compilationUnit, x));
    if (!align1.mismatches.isEmpty()) {
      if (retries <= 0) {
        throw new RuntimeException("Could not repair");
      }
      return repairAndUpdate(format, compilationUnit1, align1, retries - 1);
    } else {
      setReparsed(align1);
      return align1;
    }
  }

  private CompilationUnit read() {
    return projectInfo.read(file).values().iterator().next();
  }

  private String updateContent() {
    return ASTUtil.updateContent(initialContent, compilationUnit);
  }

  /**
   * The type Ast mapping.
   */
  public static class ASTMapping {
    /**
     * The Matches.
     */
    public final HashMap<ASTNode, ASTNode> matches = new HashMap<>();
    /**
     * The Mismatches.
     */
    public final HashMap<ASTNode, ASTNode> mismatches = new HashMap<>();
    /**
     * The Errors.
     */
    public final List<String> errors = new ArrayList<>();

    /**
     * Put all ast mapping.
     *
     * @param other the other
     * @return the ast mapping
     */
    @NotNull
    public ASTMapping putAll(@NotNull ASTMapping other) {
      matches.putAll(other.matches);
      mismatches.putAll(other.mismatches);
      errors.addAll(other.errors);
      return this;
    }
  }

  /**
   * The type Span.
   */
  public static class Span {
    /**
     * The Line start.
     */
    public final int lineStart;
    /**
     * The Col start.
     */
    public final int colStart;
    /**
     * The Line end.
     */
    public final int lineEnd;
    private final int colEnd;
    private final File file;

    /**
     * Instantiates a new Span.
     *
     * @param file      the file
     * @param lineStart the line start
     * @param colStart  the col start
     * @param lineEnd   the line end
     * @param colEnd    the col end
     */
    public Span(File file, int lineStart, int colStart, int lineEnd, int colEnd) {
      this.file = file;
      this.lineStart = lineStart;
      this.colStart = colStart;
      this.lineEnd = lineEnd;
      this.colEnd = colEnd;
    }

    @Override
    public String toString() {
      return String.format("%s:{%d:%d-%d:%d}", file.getName(), lineStart, colStart, lineEnd, colEnd);
    }
  }
}
