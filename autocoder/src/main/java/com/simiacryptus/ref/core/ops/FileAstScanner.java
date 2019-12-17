package com.simiacryptus.ref.core.ops;

import com.simiacryptus.ref.core.ProjectInfo;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;

public class FileAstScanner extends FileAstVisitor {
  public FileAstScanner(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file, boolean record) {
    super(projectInfo, compilationUnit, file, record);
  }

  protected ASTMapping reparseAndAlign() {
    return null;
  }

  @Override
  public boolean write(boolean format) {
    return false;
  }
}
