package net.wontfix.mozsearch;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import org.json.simple.JSONObject;

public class MozSearchVisitor extends VoidVisitorAdapter<String> {
  private CombinedTypeSolver mSolver;
  private String mOutputPath;

  public MozSearchVisitor(CombinedTypeSolver aSolver, final String output) {
    mSolver = aSolver;
    mOutputPath = output;
  }

  private void outputJSON(JSONObject obj) {
    try {
      File file = new File(mOutputPath);
      if (!file.getParentFile().exists()) {
        file.getParentFile().mkdirs();
      }
      PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
      printWriter.println(obj);
      printWriter.close();
    } catch (IOException exception) {
      System.err.println(exception);
    }
  }

  private void outputSource(final Node n, final SimpleName name) {
    outputSource(n, name, "");
  }

  private void outputSource(final Node n, final SimpleName name, final String scope) {
    JSONObject obj = new JSONObject();
    obj.put(
        "loc",
        name.getBegin().get().line
            + ":"
            + name.getBegin().get().column
            + "-"
            + (name.getBegin().get().column + name.getIdentifier().length()));
    obj.put("source", 1);
    if (n instanceof ClassOrInterfaceDeclaration) {
      obj.put("syntax", "def");
      if (((ClassOrInterfaceDeclaration) n).isInterface()) {
        obj.put("pretty", "interface " + scope + name.getIdentifier());
      } else {
        obj.put("pretty", "class " + scope + name.getIdentifier());
      }
    } else if (n instanceof ClassOrInterfaceType) {
      obj.put("syntax", "use");
      obj.put("pretty", "class " + scope + name.getIdentifier());
    } else if (n instanceof VariableDeclarator) {
      obj.put("syntax", "def");
      obj.put("pretty", "field " + scope + name.getIdentifier());
    } else if (n instanceof MethodDeclaration) {
      obj.put("syntax", "decl");
      obj.put("pretty", "method " + scope + name.getIdentifier());
    } else if (n instanceof MethodCallExpr) {
      obj.put("syntax", "use");
      obj.put("pretty", "method " + scope + name.getIdentifier());
    } else {
      obj.put("syntax", "use");
      obj.put("pretty", "property " + name.getIdentifier());
    }
    obj.put("sym", scope + name.getIdentifier());

    outputJSON(obj);
  }

  private void outputTarget(final Node n, final SimpleName name) {
    outputTarget(n, name, "");
  }

  private void outputTarget(final Node n, final SimpleName name, final String scope) {
    JSONObject obj = new JSONObject();
    obj.put("loc", name.getBegin().get().line + ":" + name.getBegin().get().column);
    obj.put("target", 1);

    if (n instanceof ClassOrInterfaceDeclaration) {
      obj.put("kind", "decl");
      obj.put("pretty", "class " + scope + name.getIdentifier());
    } else if (n instanceof ClassOrInterfaceType) {
      obj.put("kind", "use");
      obj.put("pretty", "class " + scope + name.getIdentifier());
    } else if (n instanceof VariableDeclarator) {
      obj.put("kind", "def");
      obj.put("pretty", "field " + scope + name.getIdentifier());
    } else if (n instanceof MethodDeclaration) {
      obj.put("kind", "decl");
      obj.put("pretty", "method " + scope + name.getIdentifier());
    } else if (n instanceof MethodCallExpr) {
      obj.put("kind", "use");
      obj.put("pretty", "method " + scope + name.getIdentifier());
    } else {
      obj.put("kind", "use");
      obj.put("pretty", "property " + name.getIdentifier());
    }

    obj.put("sym", scope + name.getIdentifier());

    outputJSON(obj);
  }

  @Override
  public void visit(ClassOrInterfaceDeclaration n, String a) {
    outputSource(n, n.getName(), a);
    outputTarget(n, n.getName(), a);

    for (ClassOrInterfaceType classType : n.getExtendedTypes()) {
      outputSource(classType, classType.getName());
      outputTarget(classType, classType.getName());
    }
    for (ClassOrInterfaceType classType : n.getImplementedTypes()) {
      outputSource(classType, classType.getName());
      outputTarget(classType, classType.getName());
    }
    super.visit(n, a);
  }

  @Override
  public void visit(VariableDeclarator n, String a) {
    String scope = "";
    Optional<Node> parent = n.getParentNode();
    try {
      parent = parent.get().getParentNode();
      if (parent.get() instanceof ClassOrInterfaceDeclaration) {
        scope = a + ((ClassOrInterfaceDeclaration) parent.get()).getName().getIdentifier() + ".";
      }
    } catch (Exception e) {
    }
    outputSource(n, n.getName(), scope);
    outputTarget(n, n.getName(), scope);
    super.visit(n, a);
  }

  @Override
  public void visit(MethodDeclaration n, String a) {
    String scope = "";
    try {
      Optional<Node> parent = n.getParentNode();
      if (parent.get() instanceof ClassOrInterfaceDeclaration) {
        scope = a + ((ClassOrInterfaceDeclaration) parent.get()).getName().getIdentifier() + ".";
      }
    } catch (Exception e) {
    }
    outputSource(n, n.getName(), scope);
    outputTarget(n, n.getName(), scope);
    // XXX Use JavaParserFacade.get(mSolver).getType(param) to get type
    super.visit(n, a);
  }

  @Override
  public void visit(MethodCallExpr n, String a) {
    String scope = "";
    try {
      ResolvedMethodDeclaration decl =
          JavaParserFacade.get(mSolver).solve(n).getCorrespondingDeclaration();
      scope = decl.getPackageName() + "." + decl.getClassName() + ".";
    } catch (Exception e) {
    }
    outputSource(n, n.getName(), scope);
    outputTarget(n, n.getName(), scope);

    super.visit(n, a);
  }
}
