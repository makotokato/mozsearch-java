package net.wontfix.mozsearch;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JavaRootPaths {
  private ArrayList<String> mRoots;

  public JavaRootPaths(final File aRoot) {
    mRoots = new ArrayList<String>();
    generateSourceRoot(aRoot);
  }

  public List<String> getPackageRoots() {
    return mRoots;
  }

  private void generateSourceRoot(final File root) {
    for (File file : root.listFiles()) {
      if (file.isDirectory()) {
        generateSourceRoot(file);
      } else if (file.isFile() && file.getName().endsWith(".java")) {
        String path = getJavaSourceRoot(file);
        if (path.length() > 0 && !mRoots.contains(path)) {
          mRoots.add(path);
        }
      }
    }
  }

  private static String getJavaSourceRoot(final File file) {
    CombinedTypeSolver solver = new CombinedTypeSolver();
    solver.add(new ReflectionTypeSolver());

    String packagename = "";

    try {
      CompilationUnit unit = StaticJavaParser.parse(file.toPath());
      Optional<PackageDeclaration> p = unit.getPackageDeclaration();
      if (p.isPresent()) {
        packagename = p.get().getName().toString();
      }
    } catch (IOException exception) {
    }

    if (packagename.length() == 0) {
      return "";
    }

    // Find package root
    String t = packagename;
    File directory = file.getParentFile();
    int pos = 0;
    while (true) {
      pos = t.lastIndexOf('.');
      if (pos < 0) {
        break;
      }
      if (!t.substring(t.lastIndexOf('.') + 1).equals(directory.getName())) {
        return "";
      }
      t = t.substring(0, pos);

      directory = directory.getParentFile();
    }
    return directory.getParentFile().toString();
  }
}
