/*
 * Copyright (c) 2019 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.ref.core.ops;

import com.simiacryptus.ref.lang.RefCoderIgnore;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;

import javax.annotation.Nonnull;

@RefCoderIgnore
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
