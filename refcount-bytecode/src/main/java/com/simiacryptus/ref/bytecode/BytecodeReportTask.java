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
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@RefIgnore
public abstract class BytecodeReportTask extends AbstractBytecodeTask {

  @OutputFile
  public abstract RegularFileProperty getTextReport();

  @OutputFile
  public abstract RegularFileProperty getJsonReport();

  public BytecodeReportTask() {
    getTextReport().convention(getProject().getLayout().getBuildDirectory().file("reports/refcount-bytecode/report.txt"));
    getJsonReport().convention(getProject().getLayout().getBuildDirectory().file("reports/refcount-bytecode/report.json"));
    getFailOnError().convention(false);
  }

  @Override
  protected boolean checkOnlyMode() {
    return false;
  }

  @Override
  protected void onResult(@Nonnull VerificationResult result) throws IOException {
    File txtFile = getTextReport().get().getAsFile();
    if (txtFile.getParentFile() != null) {
      Files.createDirectories(txtFile.getParentFile().toPath());
    }
    Files.writeString(txtFile.toPath(), result.formatReport(), StandardCharsets.UTF_8);

    File jsonFile = getJsonReport().get().getAsFile();
    if (jsonFile.getParentFile() != null) {
      Files.createDirectories(jsonFile.getParentFile().toPath());
    }
    Files.writeString(jsonFile.toPath(), result.toJson(), StandardCharsets.UTF_8);
  }
}
