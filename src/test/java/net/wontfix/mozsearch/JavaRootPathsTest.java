package net.wontfix.mozsearch;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
    List<String> paths = rootPaths.getPackageRoots();
    assertEquals(Paths.get(paths.get(0)), Paths.get("./src/main/java"));
  }
}
