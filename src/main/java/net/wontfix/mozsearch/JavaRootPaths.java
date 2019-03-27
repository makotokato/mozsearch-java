package net.wontfix.mozsearch;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JavaRootPaths {
  private ArrayList<String> mRoots;

  public JavaRootPaths(final Path aRoot) {
    mRoots = new ArrayList<String>();
    generateSourceRoot(aRoot);
  }

  public List<String> getPackageRoots() {
    return mRoots;
  }

  private void generateSourceRoot(final Path root) {
    try {
      for (Path path : Files.newDirectoryStream(root)) {
        if (Files.isDirectory(path)) {
          generateSourceRoot(path);
        } else if (path.toString().endsWith(".java")) {
          final String pathStr = getJavaSourceRoot(path);
          if (pathStr != null && !mRoots.contains(pathStr)) {
            mRoots.add(pathStr);
          }
        }
      }
    } catch (IOException e) {
    }
  }

  private static String getJavaSourceRoot(final Path javaPath) {
    CombinedTypeSolver solver = new CombinedTypeSolver();
    solver.add(new ReflectionTypeSolver());

    String packagename = "";

    try {
      CompilationUnit unit = StaticJavaParser.parse(javaPath);
      Optional<PackageDeclaration> p = unit.getPackageDeclaration();
      if (p.isPresent()) {
        packagename = p.get().getName().toString();
      }
    } catch (IOException exception) {
    }

    if (packagename.length() == 0) {
      return null;
    }

    // Find package root
    String t = packagename;
    File directory = javaPath.toFile().getParentFile();
    int pos = 0;
    while (true) {
      pos = t.lastIndexOf('.');
      if (pos < 0) {
        break;
      }
      if (!t.substring(t.lastIndexOf('.') + 1).equals(directory.getName())) {
        return null;
      }
      t = t.substring(0, pos);

      directory = directory.getParentFile();
    }
    return directory.getParentFile().toString();
  }
}
