package com.simiacryptus.ref.core.ops;

import com.simiacryptus.ref.core.ProjectInfo;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;

public class ASTScanner extends ASTEditor {
  public ASTScanner(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file, boolean record) {
    super(projectInfo, compilationUnit, file, record);
  }

  @Override
  public boolean write(boolean format) {
    return false;
  }

}
