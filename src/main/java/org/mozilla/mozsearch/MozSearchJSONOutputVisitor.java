package org.mozilla.mozsearch;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.json.JSONObject;

public class MozSearchJSONOutputVisitor extends VoidVisitorAdapter<String> {
  private Path mOutputPath;

  public MozSearchJSONOutputVisitor(final Path output) {
    mOutputPath = output;
    if (Files.exists(output)) {
      try {
        Files.delete(output);
      } catch (IOException exception) {
        System.err.println(exception);
      }
    }
  }

  private void outputJSON(final JSONObject obj) {
    try {
      if (!Files.exists(mOutputPath.getParent())) {
        Files.createDirectories(mOutputPath.getParent());
      }
      PrintWriter printWriter =
          new PrintWriter(new BufferedWriter(new FileWriter(mOutputPath.toFile(), true)));
      printWriter.println(obj);
      printWriter.close();
    } catch (IOException exception) {
      System.err.println(exception);
    }
  }

  private void outputSource(final Node n, final SimpleName name) {
    outputSource(n, name, "");
  }

  private JSONObject outputSourceLine(final SimpleName name) {
    JSONObject obj = new JSONObject();
    return obj.put(
            "loc",
            name.getBegin().get().line
                + ":"
                + (name.getBegin().get().column - 1)
                + "-"
                + (name.getBegin().get().column - 1 + name.getIdentifier().length()))
        .put("source", 1);
  }

  private void outputSource(final Node n, final SimpleName name, final String scope) {
    JSONObject obj = outputSourceLine(name);
    if (n instanceof ClassOrInterfaceDeclaration) {
      if (((ClassOrInterfaceDeclaration) n).isInterface()) {
        obj.put("syntax", "def,type").put("pretty", "interface " + scope + name.getIdentifier());
      } else {
        obj.put("syntax", "def,type").put("pretty", "class " + scope + name.getIdentifier());
      }
    } else if (n instanceof ClassOrInterfaceType) {
      obj.put("syntax", "type,use").put("pretty", "class " + scope + name.getIdentifier());
    } else if (n instanceof VariableDeclarator) {
      obj.put("syntax", "def").put("pretty", "field " + scope + name.getIdentifier());
    } else if (n instanceof ConstructorDeclaration) {
      obj.put("syntax", "def,function")
          .put("pretty", "constructor " + scope + name.getIdentifier());
    } else if (n instanceof MethodDeclaration) {
      obj.put("syntax", "def,function").put("pretty", "method " + scope + name.getIdentifier());
    } else if (n instanceof MethodCallExpr) {
      obj.put("syntax", "use,function").put("pretty", "method " + scope + name.getIdentifier());
    } else if (n instanceof FieldAccessExpr) {
      obj.put("syntax", "use,variable").put("pretty", "field " + scope + name.getIdentifier());
    } else {
      obj.put("syntax", "use").put("pretty", scope + name.getIdentifier());
    }
    String fullName = scope + name.getIdentifier();
    obj.put("sym", fullName.replace('.', '#'));

    outputJSON(obj);
  }

  private void outputTarget(final Node n, final SimpleName name) {
    outputTarget(n, name, "");
  }

  private void outputTarget(final Node n, final SimpleName name, final String scope) {
    outputTarget(n, name, scope, "");
  }

  private JSONObject outputTargetLine(final SimpleName name) {
    JSONObject obj = new JSONObject();
    return obj.put("loc", name.getBegin().get().line + ":" + (name.getBegin().get().column - 1))
        .put("target", 1);
  }

  private void outputTarget(
      final Node n, final SimpleName name, final String scope, final String context) {
    JSONObject obj = outputTargetLine(name);

    if (n instanceof ClassOrInterfaceDeclaration) {
      obj.put("kind", "def").put("pretty", scope + name.getIdentifier());
    } else if (n instanceof ClassOrInterfaceType) {
      obj.put("kind", "use").put("pretty", scope + name.getIdentifier()).put("context", context);
    } else if (n instanceof VariableDeclarator) {
      obj.put("kind", "def").put("pretty", scope + name.getIdentifier()).put("context", context);
    } else if (n instanceof ConstructorDeclaration) {
      obj.put("kind", "def").put("pretty", scope + name.getIdentifier()).put("context", context);
    } else if (n instanceof MethodDeclaration) {
      obj.put("kind", "def").put("pretty", scope + name.getIdentifier()).put("context", context);
    } else if (n instanceof MethodCallExpr) {
      obj.put("kind", "use").put("pretty", scope + name.getIdentifier()).put("context", context);
    } else if (n instanceof FieldAccessExpr) {
      obj.put("kind", "use").put("pretty", scope + name.getIdentifier()).put("context", context);
    } else {
      obj.put("kind", "use").put("pretty", name.getIdentifier()).put("context", context);
    }
    String fullName = scope + name.getIdentifier();
    fullName = fullName.replace('.', '#');
    obj.put("sym", fullName);

    outputJSON(obj);
  }

  private void outputSourceAndTarget(
      final Node n, final SimpleName name, final String scope, final String context) {
    outputSource(n, name, scope);
    outputTarget(n, name, scope, context);
  }

  private void outputSourceAndTarget(final Node n, final SimpleName name, final String scope) {
    outputSource(n, name, scope);
    outputTarget(n, name, scope);
  }

  private void outputSourceAndTarget(final Node n, final SimpleName name) {
    outputSource(n, name);
    outputTarget(n, name);
  }

  @Override
  public void visit(ClassOrInterfaceDeclaration n, String a) {
    if (n.isNestedType()) {
      outputSourceAndTarget(n, n.getName());
    } else {
      outputSourceAndTarget(n, n.getName(), a);
    }

    for (ClassOrInterfaceType classType : n.getExtendedTypes()) {
      outputSourceAndTarget(classType, classType.getName());
    }
    for (ClassOrInterfaceType classType : n.getImplementedTypes()) {
      outputSourceAndTarget(classType, classType.getName());
    }
    super.visit(n, a);
  }

  @Override
  public void visit(VariableDeclarator n, String a) {
    String scope = "";
    String context = "";

    Optional<Node> parent = n.getParentNode();
    try {
      parent = parent.get().getParentNode();
      if (parent.get() instanceof ClassOrInterfaceDeclaration) {
        scope = a + ((ClassOrInterfaceDeclaration) parent.get()).getName().getIdentifier() + ".";
        context = a + ((ClassOrInterfaceDeclaration) parent.get()).getName().getIdentifier();
      }
    } catch (Exception e) {
    }

    if (scope.length() > 0) {
      outputSourceAndTarget(n, n.getName(), scope, context);
    }

    super.visit(n, a);
  }

  @Override
  public void visit(ConstructorDeclaration n, String a) {
    String scope = "";
    String context = "";

    try {
      Optional<Node> parent = n.getParentNode();
      if (parent.get() instanceof ClassOrInterfaceDeclaration) {
        context = a + ((ClassOrInterfaceDeclaration) parent.get()).getName().getIdentifier();
        scope = context + ".";
      }
    } catch (Exception e) {
    }

    outputSourceAndTarget(n, n.getName(), scope, context);

    super.visit(n, a);
  }

  @Override
  public void visit(MethodDeclaration n, String a) {
    String scope = "";
    String context = "";

    try {
      Optional<Node> parent = n.getParentNode();
      if (parent.get() instanceof ClassOrInterfaceDeclaration) {
        context = a + ((ClassOrInterfaceDeclaration) parent.get()).getName().getIdentifier();
        scope = context + ".";
      }
    } catch (Exception e) {
    }

    outputSourceAndTarget(n, n.getName(), scope, context);
    if (scope.length() > 0) {
      outputSourceAndTarget(n, n.getName(), "", context);
    }

    super.visit(n, a);
  }

  @Override
  public void visit(MethodCallExpr n, String a) {
    String scope = "";
    String context = "";

    try {
      ResolvedMethodDeclaration decl = n.resolve();
      scope = decl.getPackageName() + "." + decl.getClassName() + ".";
    } catch (Exception e) {
    }

    try {
      Optional<Node> parent = n.getParentNode();
      if (parent.get() instanceof MethodDeclaration) {
        context = a + ((MethodDeclaration) parent.get()).getName().getIdentifier();
      }
    } catch (Exception e) {
    }

    outputSourceAndTarget(n, n.getName(), scope, context);

    super.visit(n, a);
  }

  @Override
  public void visit(FieldAccessExpr n, String a) {
    // XXX must implement
    super.visit(n, a);
  }
}
