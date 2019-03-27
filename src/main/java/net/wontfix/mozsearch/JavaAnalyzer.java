package net.wontfix.mozsearch;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JavaAnalyzer {
  public static void main(String[] args) {
    System.out.println("Searching java sources to look for root path...");
    final File sourceDir = new File(args[0]);
    final File outputDir = new File(args[1]);
    final JavaRootPaths paths = new JavaRootPaths(sourceDir);

    System.out.println("Generating references ...");
    final List<String> rootPaths = paths.getPackageRoots();
    final String[] rootPathString = rootPaths.toArray(new String[rootPaths.size()]);
    for (String path : rootPathString) {
      lookingAllChildren(new File(path), sourceDir, outputDir, rootPaths);
    }
  }

  private static void lookingAllChildren(
      final File currentDir, final File srcDir, final File outputDir, final List<String> roots) {
    ArrayList<File> javaFiles = new ArrayList<File>();
    for (File file : currentDir.listFiles()) {
      if (file.isDirectory()) {
        lookingAllChildren(file, srcDir, outputDir, roots);
      } else if (file.isFile() && file.getName().endsWith(".java")) {
        javaFiles.add(file);
      }
    }
    makeIndexes(javaFiles, srcDir, outputDir, roots);
  }

  private static void makeIndexes(final List<File> files, final File srcDir, final File outputDir, final List<String> roots) {
    if (files.isEmpty()) {
      return;
    }

    final CombinedTypeSolver solver = new CombinedTypeSolver();
    solver.add(new ReflectionTypeSolver());
    solver.add(new JavaParserTypeSolver(files.get(0).getParent()));
    // Set Android SDK's JAR using ANDROID_SDK_ROOT
    String sdkroot = System.getenv("ANDROID_SDK_ROOT");
    if (sdkroot != null && sdkroot.length() > 0) {
      try {
        solver.add(new JarTypeSolver(sdkroot + "/platforms/android-28/android.jar"));
      } catch (IOException exception) {
      }
    }
    for (String path : roots) {
      solver.add(new JavaParserTypeSolver(path));
    }

    final JavaSymbolSolver symbolSolver = new JavaSymbolSolver(solver);
    StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);

    for (File file : files) {
      try {
        makeIndex(file, outputDir.toString() + "/./" + file.toString().substring(srcDir.toString().length() + 1));
      } catch (Exception exception) {
        System.err.println(exception);
      }
    }

    System.gc();
  }


  private static void makeIndex(
      final File file, final String outputDirectory) throws IOException {
    if (!file.isFile() || !file.getName().endsWith(".java")) {
      return;
    }
    final CompilationUnit unit = StaticJavaParser.parse(file.toPath());

    String packagename = "";
    Optional<PackageDeclaration> p = unit.getPackageDeclaration();
    if (p.isPresent()) {
      packagename = p.get().getName().toString() + ".";
    }

    System.out.print("Processing " + file.toPath().toString() + " ");
    MozSearchVisitor visitor = new MozSearchVisitor(outputDirectory);
    unit.accept(visitor, packagename);
    System.out.println("Done");
  }
}
