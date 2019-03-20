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
import java.util.Optional;

public class JavaAnalyzer {
  public static void main(String[] args) {
    lookingAllChildren(new File(args[0]), args[1]);
  }

  private static void lookingAllChildren(final File directory, String outputDirectory) {
    for (File file : directory.listFiles()) {
      if (file.isDirectory()) {
        // System.err.println(outputDirectory);
        lookingAllChildren(file, outputDirectory + "/" + file.getName());
      } else if (file.isFile() && file.getName().endsWith(".java")) {
        try {
          makeIndexing(file, outputDirectory + "/" + file.getName());
        } catch (IOException exception) {
          System.err.println(exception);
        }
      }
    }
  }

  private static void makeIndexing(final File file, final String outputDirectory)
      throws IOException {
    CombinedTypeSolver solver = new CombinedTypeSolver();
    solver.add(new ReflectionTypeSolver());
    solver.add(new JavaParserTypeSolver(file.getParent()));

    CompilationUnit unit = StaticJavaParser.parse(file.toPath());

    String packagename = "";
    Optional<PackageDeclaration> p = unit.getPackageDeclaration();
    if (p.isPresent()) {
      packagename = p.get().getName().toString();
    }

    // Find package root
    if (packagename.length() > 0) {
      String t = packagename;
      File directory = file;
      int pos = 0;
      while (pos >= 0) {
        directory = directory.getParentFile();
        pos = t.indexOf('.');
        t = t.substring(pos + 1);
      }
      System.out.println("Processing " + file.toString() + " ...");
      solver.add(new JavaParserTypeSolver(directory));
    }
    MozSearchVisitor visitor = new MozSearchVisitor(solver, outputDirectory);
    unit.accept(visitor, packagename + ".");
  }
}
