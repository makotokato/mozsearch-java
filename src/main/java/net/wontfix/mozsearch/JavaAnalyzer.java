package net.wontfix.mozsearch;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

public class JavaAnalyzer {
    public static void main(String[] args) {
        Path path = Paths.get(args[0]);
        try {
            CompilationUnit unit = StaticJavaParser.parse(path);
            String packagename = "";
            Optional<PackageDeclaration> p =  unit.getPackageDeclaration();
            if (p.isPresent()) {
                packagename = p.get().getName().toString();
            }

            CombinedTypeSolver solver = new CombinedTypeSolver();
            solver.add(new ReflectionTypeSolver());
            solver.add(new JavaParserTypeSolver(new File(path.getParent().toString())));
            if (packagename.length() > 0) {
                String t = packagename;
                Path directory = path.getParent();
                int pos = 0;
                while (pos >= 0) {
                    directory = directory.getParent();
                    solver.add(new JavaParserTypeSolver(new File(directory.toString())));
                    pos = t.indexOf('.');
                    t = t.substring(pos + 1);
                }
            }
    
            unit.accept(new MozSearchVisitor(solver), packagename + ".");
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
