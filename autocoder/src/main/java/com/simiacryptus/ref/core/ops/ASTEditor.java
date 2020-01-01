package com.simiacryptus.ref.core.ops;

import com.simiacryptus.ref.core.ASTUtil;
import com.simiacryptus.ref.core.AutoCoder;
import com.simiacryptus.ref.core.ProjectInfo;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class ASTEditor extends LoggingASTVisitor {
  protected final ProjectInfo projectInfo;
  protected final String initialContent;
  private ASTMapping reparsed = null;

  public ASTEditor(CompilationUnit compilationUnit, ProjectInfo projectInfo, @Nonnull File file) {
    super(compilationUnit, file);
    this.projectInfo = projectInfo;
    this.initialContent = AutoCoder.read(this.file);
  }

  public ASTEditor(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file, boolean record) {
    this(compilationUnit, projectInfo, file);
    if (record) compilationUnit.recordModifications();
  }

  public ASTMapping getReparsed() {
    return reparsed;
  }

  protected void setReparsed(ASTMapping reparsed) {
    this.reparsed = reparsed;
  }

  public boolean write(boolean format) {
    final String finalSrc = updateContent();
    if (initialContent.equals(finalSrc)) return false;
    write(format ? AutoCoder.format(finalSrc) : finalSrc);
    return true;
  }

  public boolean revert() {
    final String currentContent = AutoCoder.read(this.file);
    if (currentContent.equals(initialContent)) return false;
    write(initialContent);
    return true;
  }

  public boolean writeFinal(boolean format) {
    update(true, format);
    return !AutoCoder.read(this.file).equals(initialContent);
  }

  @NotNull
  protected <T extends ASTNode> T copyIfAttached(@NotNull T node) {
    if (node.getParent() == null) {
      return node;
    } else {
      info(1, node, "Copy node %s", node);
      return (T) ASTNode.copySubtree(ast, node);
    }
  }

  protected final void replace(ASTNode child, ASTNode newChild) {
    final ASTNode parent = child.getParent();
    if (parent instanceof QualifiedName) {
      final QualifiedName qualifiedName = (QualifiedName) parent;
      if (qualifiedName.getQualifier().equals(child)) {
        if (!(newChild instanceof Name) && (newChild instanceof Expression)) {
          final FieldAccess fieldAccess = ast.newFieldAccess();
          fieldAccess.setExpression(copyIfAttached((Expression) newChild));
          fieldAccess.setName(copyIfAttached(qualifiedName.getName()));
          info(child, "Replacing %s with %s", child, newChild);
          replace(parent, fieldAccess);
          return;
        }
      }
    }
    StructuralPropertyDescriptor location = child.getLocationInParent();
    if (location != null) {
      info(1, child, "Replace %s with %s within %s", child, newChild, location);
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

  protected <T extends ASTNode> ASTMapping update(boolean write, boolean format) {
    if (write) {
      logger.info(String.format("Writing intermediate changes to %s", file));
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

  protected void write(String data) {
    try {
      FileUtils.write(file, data, "UTF-8");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  protected ASTEditor.Span getSpan(ASTNode node) {
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

  private ASTMapping repairAndUpdate(boolean format, CompilationUnit compilationUnit0, ASTMapping align0) {
    return repairAndUpdate(format, compilationUnit0, align0, 3);
  }

  private ASTMapping repairAndUpdate(boolean format, CompilationUnit compilationUnit0, ASTMapping align0, int retries) {
    compilationUnit0.recordModifications();
    align0.mismatches.forEach((from, to) -> {
      replace(to, ASTNode.copySubtree(to.getAST(), from));
    });
    final String content0 = AutoCoder.read(this.file);
    final String content1 = ASTUtil.updateContent(content0, compilationUnit0);
    if (content0.equals(content1)) {
      throw new RuntimeException("ASTNode fixups did not change document");
    }
    write(format ? AutoCoder.format(content1) : content1);
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

  public static class ASTMapping {
    public final HashMap<ASTNode, ASTNode> matches = new HashMap<>();
    public final HashMap<ASTNode, ASTNode> mismatches = new HashMap<>();
    public final List<String> errors = new ArrayList<>();

    public ASTMapping putAll(ASTMapping other) {
      matches.putAll(other.matches);
      mismatches.putAll(other.mismatches);
      errors.addAll(other.errors);
      return this;
    }
  }

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

    @Override
    public String toString() {
      return String.format("%s:{%d:%d-%d:%d}", file.getName(), lineStart, colStart, lineEnd, colEnd);
    }
  }
}
