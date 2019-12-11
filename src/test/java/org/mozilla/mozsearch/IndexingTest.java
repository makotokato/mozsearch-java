package org.mozilla.mozsearch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class IndexingTest extends TestCase {
  /**
   * Create the test case
   *
   * @param testName name of the test case
   */
  public IndexingTest(String testName) {
    super(testName);
  }

  /** @return the suite of tests being tested */
  public static Test suite() {
    return new TestSuite(IndexingTest.class);
  }

  public void testIndexing() throws IOException {
    JavaIndexer indexer = new JavaIndexer(Paths.get("./src/test/resources/data"), Paths.get("/tmp"));
    indexer.outputIndexes();
    byte[] f1 = Files.readAllBytes(Paths.get("/tmp/hello.java"));
    byte[] f2 = Files.readAllBytes(Paths.get("./src/test/resources/result/hello.java.out"));
    assertTrue(f1.length == f2.length);
    Files.delete(Paths.get("/tmp/hello.java"));
  }
}
