package org.mozilla.mozsearch;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class JavaIndexer {
  private Path mSourceDir;
  private Path mOutputDir;
  private ExecutorService mPool;
  private int mTimeout = -1;
  private int mThreadPoolCount = 4;

  public JavaIndexer(final Path sourceDir, final Path outputDir) {
    mSourceDir = sourceDir;
    mOutputDir = outputDir;
    mPool = null;
  }

  public void setTimeout(int timeout) {
    mTimeout = timeout;
  }

  public void useThreadPool(boolean enabled) {
    if (enabled) {
      mPool = Executors.newFixedThreadPool(mThreadPoolCount);

      if (Runtime.getRuntime().availableProcessors() < 4) {
        mThreadPoolCount = 2;
      } else {
        mThreadPoolCount = 4;
      }
    } else {
      mPool = null;
    }
  }

  public void outputIndexes() {
    try {
      indexAllChildren(mSourceDir, mSourceDir, mOutputDir);
    } catch (IOException exception) {
      System.err.println(exception);
    }

    if (mPool == null) {
      return;
    }

    mPool.shutdown();

    try {
      mPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (InterruptedException exception) {
    }
  }

  private void indexAllChildren(final Path currentDir, final Path srcDir, final Path outputDir)
      throws IOException {
    if (currentDir.toFile().getName().equals(".git")
        || currentDir.toFile().getName().equals(".hg")) {
      return;
    }
    ArrayList<Path> javaFiles = new ArrayList<Path>();
    List<Path> files = Files.list(currentDir).collect(Collectors.toList());
    for (Path file : files) {
      if (Files.isDirectory(file) && !Files.isSymbolicLink(file)) {
        indexAllChildren(file, srcDir, outputDir);
      } else if (file.toString().endsWith(".java")) {
        javaFiles.add(file);
      }
    }
    makeIndexes(javaFiles, srcDir, outputDir);
  }

  private static Path getImportJavaFile(
      final Path file, final String importName, final String packageName) {
    if (importName.startsWith("java.")) {
      return null;
    }

    String path = packageName;
    Path root = file.getParent().getParent();
    while (path.contains(".")) {
      root = root.getParent();
      path = path.substring(0, path.lastIndexOf(".") - 1);
    }
    final Path importFile = Paths.get(root.toString(), importName.replace(".", "/") + ".java");
    if (Files.exists(importFile)) {
      return importFile;
    }
    return null;
  }

  private void makeIndexes(final List<Path> files, final Path srcDir, final Path outputDir) {
    if (files.isEmpty()) {
      return;
    }

    final CombinedTypeSolver solver = new CombinedTypeSolver();
    solver.add(new ReflectionTypeSolver());

    // This is cached dir list not to add duplicated entry
    final ArrayList<Path> dirs = new ArrayList<Path>();

    // Add dir from import syntax.
    StaticJavaParser.getConfiguration().setSymbolResolver(null);
    for (Path file : files) {
      try {
        final CompilationUnit unit = StaticJavaParser.parse(file);
        if (unit.getPackageDeclaration().isPresent()) {
          final String packageName = unit.getPackageDeclaration().get().getName().toString();
          for (ImportDeclaration item : unit.getImports()) {
            final Path importFile = getImportJavaFile(file, item.getName().toString(), packageName);
            if (importFile != null) {
              try {
                if (!dirs.contains(importFile.getParent())) {
                  solver.add(new JavaParserTypeSolver(importFile.getParent()));
                  dirs.add(importFile.getParent());
                }
              } catch (Exception exception) {
              }
            }
          }
        }
      } catch (Exception exception) {
      }

      if (!dirs.contains(file.getParent())) {
        solver.add(new JavaParserTypeSolver(file.getParent()));
        dirs.add(file.getParent());
      }
    }

    // Set Android SDK's JAR using ANDROID_SDK_ROOT
    final String sdkroot = System.getenv("ANDROID_SDK_ROOT");
    if (sdkroot != null && sdkroot.length() > 0) {
      try {
        final String[] apis = new String[] {"android-30", "android-29", "android-28"};
        for (String api : apis) {
          final Path sdkrootPath = Paths.get(sdkroot, "platforms", api, "android.jar");
          if (Files.exists(sdkrootPath)) {
            solver.add(new JarTypeSolver(sdkrootPath));
            break;
          }
        }
      } catch (IOException exception) {
      }
    }

    StaticJavaParser.getConfiguration().setSymbolResolver(new JavaSymbolSolver(solver));

    for (Path file : files) {
      Path output =
          Paths.get(
              outputDir.toString(), file.toString().substring(srcDir.toString().length() + 1));
      try {
        makeIndex(file, output);
      } catch (Exception exception) {
        System.err.println(exception);
        try {
          Files.delete(output);
        } catch (IOException ioexception) {
        }
      }
    }
  }

  private void makeIndex(final Path file, final Path outputPath)
      throws IOException, ParseProblemException {
    if (!file.toString().endsWith(".java")) {
      return;
    }

    if (mPool != null) {
      final CompilationUnit unit = StaticJavaParser.parse(file);
      mPool.submit(new JavaIndexerTask(unit, file, outputPath, mTimeout));
      return;
    }

    System.out.println("Processing " + file.toString() + " ");

    final CompilationUnit unit = StaticJavaParser.parse(file);

    MozSearchJSONOutputVisitor visitor = new MozSearchJSONOutputVisitor(outputPath);
    if (mTimeout > 0) {
      visitor.setTimeout(mTimeout);
    }
    unit.accept(visitor, null);
  }
}
