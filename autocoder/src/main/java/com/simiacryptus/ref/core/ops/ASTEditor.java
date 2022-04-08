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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The ASTEditor class is used to manage the projectInfo and initialContent fields.
 * The class also provides a method to check if the ASTMapping has been reparsed.
 *
 * @docgenVersion 9
 */
public abstract class ASTEditor extends LoggingASTVisitor {
  protected final ProjectInfo projectInfo;
  protected final String initialContent;
  @Nullable
  private ASTMapping reparsed = null;

  public ASTEditor(@Nonnull CompilationUnit compilationUnit, ProjectInfo projectInfo, @Nonnull File file) {
    super(compilationUnit, file);
    this.projectInfo = projectInfo;
    this.initialContent = AutoCoder.read(this.file);
  }

  public ASTEditor(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file, boolean record) {
    this(compilationUnit, projectInfo, file);
    if (record) compilationUnit.recordModifications();
  }

  /**
   * @return the reparsed ASTMapping, or null if there is none
   * @docgenVersion 9
   */
  @Nullable
  public ASTMapping getReparsed() {
    return reparsed;
  }

  /**
   * Sets the reparsed ASTMapping.
   *
   * @param reparsed the reparsed ASTMapping
   * @docgenVersion 9
   */
  protected void setReparsed(ASTMapping reparsed) {
    this.reparsed = reparsed;
  }

  /**
   * Updates the content and writes it to the file.
   *
   * @param format true if the content should be formatted, false otherwise
   * @return true if the content was updated, false otherwise
   * @docgenVersion 9
   */
  public boolean write(boolean format) {
    final String finalSrc = updateContent();
    if (initialContent.equals(finalSrc)) return false;
    write(format ? ASTUtil.format(finalSrc) : finalSrc);
    return true;
  }

  /**
   * Reverts the file to its initial content.
   *
   * @return true if the file was reverted, false if the file was already in its initial state
   * @docgenVersion 9
   */
  public boolean revert() {
    final String currentContent = AutoCoder.read(this.file);
    if (currentContent.equals(initialContent)) return false;
    write(initialContent);
    return true;
  }

  /**
   * @param format
   * @return
   * @docgenVersion 9
   */
  public boolean writeFinal(boolean format) {
    update(true, format);
    return !AutoCoder.read(this.file).equals(initialContent);
  }

  /**
   * Returns the span of the given node.
   *
   * @param node the node to get the span of
   * @return the span of the given node
   * @docgenVersion 9
   */
  public ASTEditor.Span getSpan(@Nonnull ASTNode node) {
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

  /**
   * {@inheritDoc}
   *
   * @docgenVersion 9
   */
  @Override
  public void preVisit(ASTNode node) {
    super.preVisit(node);
  }

  /**
   * @Override public boolean preVisit2(ASTNode node) {
   * return super.preVisit2(node);
   * }
   * @docgenVersion 9
   */
  @Override
  public boolean preVisit2(ASTNode node) {
    return super.preVisit2(node);
  }

  /**
   * {@inheritDoc}
   *
   * @docgenVersion 9
   */
  @Override
  public void postVisit(ASTNode node) {
    super.postVisit(node);
  }

  /**
   * @param node the node to copy
   * @return the node, or a copy of the node if it is attached to a parent
   * @docgenVersion 9
   */
  @Nonnull
  protected <T extends ASTNode> T copyIfAttached(@Nonnull T node) {
    if (node.getParent() == null) {
      return node;
    } else {
      debug(1, node, "Copy node %s", node);
      return (T) ASTNode.copySubtree(ast, node);
    }
  }

  /**
   * Replaces the given child node of this node with the given replacement node.
   *
   * @param child    the child node to be replaced
   * @param newChild the replacement node
   * @throws NullPointerException if the given child or replacement node is null
   * @docgenVersion 9
   */
  protected final void replace(@Nonnull ASTNode child, ASTNode newChild) {
    final ASTNode parent = child.getParent();
    if (parent instanceof QualifiedName) {
      final QualifiedName qualifiedName = (QualifiedName) parent;
      if (qualifiedName.getQualifier().equals(child)) {
        if (!(newChild instanceof Name) && newChild instanceof Expression) {
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
   * @Nonnull protected <T extends ASTNode> ASTMapping update(boolean write, boolean format);
   * @docgenVersion 9
   */
  @Nonnull
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
   * Writes the given data to the file.
   *
   * @param data the data to write
   * @docgenVersion 9
   */
  protected void write(String data) {
    throwQueuedExceptions();
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
   * Repairs and updates the given compilation unit and AST mapping.
   *
   * @param format           whether or not to format the compilation unit
   * @param compilationUnit0 the compilation unit to repair and update
   * @param align0           the AST mapping to repair and update
   * @param maxIterations    the maximum number of iterations to perform
   * @return the repaired and updated AST mapping
   * @docgenVersion 9
   */
  @Nonnull
  private ASTMapping repairAndUpdate(boolean format, @Nonnull CompilationUnit compilationUnit0, @Nonnull ASTMapping align0) {
    return repairAndUpdate(format, compilationUnit0, align0, 3);
  }

  /**
   * Repairs and updates the given compilation unit and AST mapping.
   *
   * @param format           whether to format the compilation unit
   * @param compilationUnit0 the compilation unit to repair and update
   * @param align0           the AST mapping to repair and update
   * @param retries          the number of retries
   * @return the repaired and updated AST mapping
   * @throws IllegalArgumentException if the compilation unit or AST mapping is null
   * @docgenVersion 9
   */
  @Nonnull
  private ASTMapping repairAndUpdate(boolean format, @Nonnull CompilationUnit compilationUnit0, @Nonnull ASTMapping align0, int retries) {
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

  /**
   * Reads a compilation unit from a file.
   *
   * @return the compilation unit
   * @docgenVersion 9
   */
  private CompilationUnit read() {
    return projectInfo.read(file).values().iterator().next();
  }

  /**
   * Updates the content of the compilation unit to the given string.
   *
   * @param initialContent  the new content of the compilation unit
   * @param compilationUnit the compilation unit to update
   * @return the updated content of the compilation unit
   * @docgenVersion 9
   */
  private String updateContent() {
    return ASTUtil.updateContent(initialContent, compilationUnit);
  }

  /**
   * This class represents a mapping between two Abstract Syntax Trees (ASTs).
   *
   * @author Your Name
   * @docgenVersion 9
   */
  public static class ASTMapping {
    public final HashMap<ASTNode, ASTNode> matches = new HashMap<>();
    public final HashMap<ASTNode, ASTNode> mismatches = new HashMap<>();
    public final List<String> errors = new ArrayList<>();

    /**
     * @param other The other ASTMapping to get the matches, mismatches, and errors from.
     * @return This ASTMapping with the new matches, mismatches, and errors.
     * @Nonnull public ASTMapping putAll(@Nonnull ASTMapping other)
     * <p>
     * This method puts all of the other ASTMapping's matches, mismatches, and errors into this ASTMapping.
     * @docgenVersion 9
     */
    @Nonnull
    public ASTMapping putAll(@Nonnull ASTMapping other) {
      matches.putAll(other.matches);
      mismatches.putAll(other.mismatches);
      errors.addAll(other.errors);
      return this;
    }
  }

  /**
   * This class represents a span of text in a file.
   *
   * @param lineStart the line number where the span starts
   * @param colStart  the column number where the span starts
   * @param lineEnd   the line number where the span ends
   * @param colEnd    the column number where the span ends
   * @param file      the file containing the span
   * @docgenVersion 9
   */
  public static class Span {
    public final int lineStart;
    public final int colStart;
    public final int lineEnd;
    private final int colEnd;
    private final File file;

    public Span(File file, int lineStart, int colStart, int lineEnd, int colEnd) {
      this.file = file;
      this.lineStart = lineStart;
      this.colStart = colStart;
      this.lineEnd = lineEnd;
      this.colEnd = colEnd;
    }

    /**
     * @return a String representation of this object in the format
     * {@code <file name>:{<line start>:<column start>-<line end>:<column end>}}
     * @docgenVersion 9
     */
    @Override
    public String toString() {
      return String.format("%s:{%d:%d-%d:%d}", file.getName(), lineStart, colStart, lineEnd, colEnd);
    }
  }
}
