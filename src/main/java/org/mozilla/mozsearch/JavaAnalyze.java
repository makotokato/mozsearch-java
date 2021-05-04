package org.mozilla.mozsearch;

import java.nio.file.Paths;

public class JavaAnalyze {
  public static void main(String[] args) {
    int n = 0;
    int timeout = -1;

    try {
      if (args[n].startsWith("--timeout")) {
        timeout = Integer.parseInt(args[n + 1]) * 1000;
        n += 2;
      }
    } catch (Exception e) {
      System.err.println("Usage: JavaAnalyze <source path> <destination path>");
      System.exit(-1);
    }

    if (args.length < n + 2) {
      System.err.println("Usage: JavaAnalyze <source path> <destination path>");
      System.exit(-1);
    }

    System.out.println("Generating references ...");
    JavaIndexer indexer = new JavaIndexer(Paths.get(args[n]), Paths.get(args[n + 1]));
    indexer.outputIndexes();
  }
}
