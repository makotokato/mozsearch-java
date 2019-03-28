package net.wontfix.mozsearch;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

public class JavaAnalyzer {
  public static void main(String[] args) {
    System.out.println("Searching java sources to look for root path...");
    final File sourceDir = new File(args[0]);
    final File outputDir = new File(args[1]);
    final JavaRootPaths paths = new JavaRootPaths(Paths.get(args[0]));

    System.out.println("Generating references ...");
    final List<String> rootPaths = paths.getPackageRoots();
    final String[] rootPathString = rootPaths.toArray(new String[rootPaths.size()]);

    JavaIndexer indexer = new JavaIndexer(sourceDir, outputDir);
    indexer.make(rootPathString);
  }
}
