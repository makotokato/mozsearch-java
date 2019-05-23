package org.mozilla.mozsearch;

import java.nio.file.Paths;

public class JavaAnalyzer {
  public static void main(String[] args) {
    if (args.length != 2) {
      System.err.println("Usage: JavaAnalyzer <source path> <destination path>");
      System.exit(-1);
    }
    System.out.println("Searching java sources to look for root path...");
    final JavaRootPaths paths = new JavaRootPaths(Paths.get(args[0]));

    System.out.println("Generating references ...");

    JavaIndexer indexer = new JavaIndexer(Paths.get(args[0]), Paths.get(args[1]));
    indexer.make(paths.getPackageRoots());
  }
}
