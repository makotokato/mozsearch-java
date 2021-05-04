package org.mozilla.mozsearch;

import com.github.javaparser.ast.CompilationUnit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JavaIndexerTask implements Runnable {
  private CompilationUnit mUnit;
  private Path mInput;
  private Path mOutput;
  private int mTimeout;

  public JavaIndexerTask(
      final CompilationUnit unit, final Path file, final Path output, int timeout) {
    mUnit = unit;
    mInput = file;
    mOutput = output;
    mTimeout = timeout;
  }

  public void run() {
    System.out.println("Processing " + mInput.toString());

    try {
      MozSearchJSONOutputVisitor visitor = new MozSearchJSONOutputVisitor(mOutput);
      if (mTimeout > 0) {
        visitor.setTimeout(mTimeout);
      }
      mUnit.accept(visitor, null);
    } catch (Exception exception) {
      System.err.println(exception);
      try {
        Files.delete(mOutput);
      } catch (IOException ioexception) {
      }
    }
  }
}
