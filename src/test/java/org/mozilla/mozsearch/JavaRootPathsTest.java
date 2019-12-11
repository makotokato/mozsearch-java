package org.mozilla.mozsearch;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/** Unit test for simple App. */
public class JavaRootPathsTest extends TestCase {
  /**
   * Create the test case
   *
   * @param testName name of the test case
   */
  public JavaRootPathsTest(String testName) {
    super(testName);
  }

  /** @return the suite of tests being tested */
  public static Test suite() {
    return new TestSuite(JavaRootPathsTest.class);
  }

  public void testRootPaths() {
    JavaRootPaths rootPaths = new JavaRootPaths(Paths.get("."));
    Path[] paths = rootPaths.getPackageRoots();
    assertTrue(Arrays.asList(paths).contains(Paths.get("./src/main/java")));
    assertTrue(Arrays.asList(paths).contains(Paths.get("./src/test/java")));
  }
}
