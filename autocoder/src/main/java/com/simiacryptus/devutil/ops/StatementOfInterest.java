package com.simiacryptus.devutil.ops;

import org.eclipse.jdt.core.dom.*;

import javax.annotation.Nonnull;

public class StatementOfInterest {
  public final int line;
  public final Statement statement;
  public final Block block;

  public StatementOfInterest(@Nonnull Statement statement, int line) {
    this.statement = statement;
    this.block = (Block) this.statement.getParent();
    this.line = line;
  }

  public boolean isComplexReturn() {
    if (!isReturn()) return false;
    return !(((ReturnStatement) statement).getExpression() instanceof SimpleName);
  }

  public boolean isReturn() {
    return statement instanceof ReturnStatement;
  }

}
