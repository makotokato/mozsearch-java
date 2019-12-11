package org.mozilla.mozsearch;

import java.nio.file.Paths;

public class JavaCodeAnalyzer {
  public static void main(String[] args) {
    if (args.length != 2) {
      System.err.println("Usage: JavaCodeAnalyzer <source path> <destination path>");
      System.exit(-1);
    }

    System.out.println("Generating references ...");
    JavaIndexer indexer = new JavaIndexer(Paths.get(args[0]), Paths.get(args[1]));
    final JavaRootPaths paths = new JavaRootPaths(Paths.get(args[0]));
    indexer.makeWithoutAllPackageRoot(paths.getPackageRoots());
  }
}
