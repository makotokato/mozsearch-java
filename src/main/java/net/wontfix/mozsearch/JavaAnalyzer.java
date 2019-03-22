package net.wontfix.mozsearch;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class JavaAnalyzer {
  public static void main(String[] args) {
    System.out.println("Traversing java source to look for root...");
    JavaRootPaths paths = new JavaRootPaths(new File(args[0]));

    System.out.println("Generating references ...");
    lookingAllChildren(new File(args[0]), args[1], paths.getPackageRoots());
  }

  private static void lookingAllChildren(
      final File directory, final String outputDirectory, final List<String> roots) {
    for (File file : directory.listFiles()) {
      if (file.isDirectory()) {
        lookingAllChildren(file, outputDirectory + "/" + file.getName(), roots);
      } else if (file.isFile() && file.getName().endsWith(".java")) {
        try {
          makeIndex(file, outputDirectory + "/" + file.getName(), roots);
        } catch (IOException exception) {
          System.err.println(exception);
        }
      }
    }
  }

  private static void makeIndex(
      final File file, final String outputDirectory, final List<String> roots) throws IOException {
    CombinedTypeSolver solver = new CombinedTypeSolver();
    solver.add(new ReflectionTypeSolver());
    solver.add(new JavaParserTypeSolver(file.getParent()));
    // Set Android SDK's JAR
    // solver.add(new JarTypeSolver(""));

    CompilationUnit unit = StaticJavaParser.parse(file.toPath());

    String packagename = "";
    Optional<PackageDeclaration> p = unit.getPackageDeclaration();
    if (p.isPresent()) {
      packagename = p.get().getName().toString();
    }

    // Find package root
    for (String path : roots) {
      solver.add(new JavaParserTypeSolver(path));
    }
    System.out.println("Processing " + file.toPath().toString());
    MozSearchVisitor visitor = new MozSearchVisitor(solver, outputDirectory);
    unit.accept(visitor, packagename + ".");
  }
}
