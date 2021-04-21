package org.mozilla.mozsearch;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
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
  private long mStart;

  public MozSearchJSONOutputVisitor(final Path output) {
    mOutputPath = output;
    if (Files.exists(output)) {
      try {
        Files.delete(output);
      } catch (IOException exception) {
        System.err.println(exception);
      }
    }
    mStart = System.currentTimeMillis();
  }

  // Resolving type spends more time, so when execute time is too long,
  // we don't resolve type for fields. But declare will be resolved if possible.
  private boolean isLongTask() {
    return (System.currentTimeMillis() - mStart) > 1000 * 60; // 1min
  }

  private static String getScope(final String fullName, final SimpleName name) {
    return fullName.substring(0, fullName.length() - name.toString().length());
  }

  private static String getContext(final Node n) {
    try {
      Optional<Node> parent = n.getParentNode();
      while (parent.isPresent()) {
        if (parent.get() instanceof MethodDeclaration) {
          MethodDeclaration d = (MethodDeclaration) parent.get();
          final ResolvedMethodDeclaration decl = d.resolve();
          return decl.getQualifiedName();
        } else if (parent.get() instanceof ConstructorDeclaration) {
          final ConstructorDeclaration d = (ConstructorDeclaration) parent.get();
          final ResolvedReferenceTypeDeclaration decl = d.resolve().declaringType();
          return decl.getQualifiedName() + "." + d.getName();
        }
        parent = parent.get().getParentNode();
      }
    } catch (Exception e) {
      // not resolved
    }
    return "";
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

  private void outputSource(
      final ClassOrInterfaceDeclaration n, final SimpleName name, final String scope) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addSourceLine(name).addSource(n, name, scope);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputSource(
      final ClassOrInterfaceType n, final SimpleName name, final String scope) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addSourceLine(name).addSource(n, name, scope);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputSource(
      final ConstructorDeclaration n, final SimpleName name, final String scope) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addSourceLine(name).addSource(n, name, scope);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputSource(final MethodDeclaration n, final SimpleName name, final String scope) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addSourceLine(name).addSource(n, name, scope);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputSource(final VariableDeclarator n, final SimpleName name, final String scope, boolean isVariable) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addSourceLine(name).addSource(n, name, scope);
    if (isVariable) {
      obj.put("no_crossref", 1);
    }
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputSource(final ObjectCreationExpr n, final SimpleName name, final String scope) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addSourceLine(name).addSource(n, name, scope);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputSource(final MethodCallExpr n, final SimpleName name, final String scope) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addSourceLine(name).addSource(n, name, scope);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputSource(final FieldAccessExpr n, final SimpleName name, final String scope) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addSourceLine(name).addSource(n, name, scope);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputSource(final Parameter n, final SimpleName name, final String scope) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addSourceLine(name).addSource(n, name, scope);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputSource(final ReferenceType n, final SimpleName name, final String scope) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addSourceLine(name).addSource(n, name, scope);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputSource(final NameExpr n, final SimpleName name, final String scope) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addSourceLine(name).addSource(n, name, scope);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputTarget(
      final ClassOrInterfaceDeclaration n,
      final SimpleName name,
      final String scope,
      final String context) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addTargetLine(name).addTarget(n, name, scope, context);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputTarget(
      final ClassOrInterfaceType n,
      final SimpleName name,
      final String scope,
      final String context) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addTargetLine(name).addTarget(n, name, scope, context);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputTarget(
      final ConstructorDeclaration n,
      final SimpleName name,
      final String scope,
      final String context) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addTargetLine(name).addTarget(n, name, scope, context);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputTarget(
      final VariableDeclarator n, final SimpleName name, final String scope, final String context) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addTargetLine(name).addTarget(n, name, scope, context);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputTarget(
      final MethodDeclaration n, final SimpleName name, final String scope, final String context) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addTargetLine(name).addTarget(n, name, scope, context);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputTarget(
      final ObjectCreationExpr n, final SimpleName name, final String scope, final String context) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addTargetLine(name).addTarget(n, name, scope, context);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputTarget(
      final MethodCallExpr n, final SimpleName name, final String scope, final String context) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addTargetLine(name).addTarget(n, name, scope, context);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputTarget(
      final FieldAccessExpr n, final SimpleName name, final String scope, final String context) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addTargetLine(name).addTarget(n, name, scope, context);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputTarget(
      final NameExpr n, final SimpleName name, final String scope, final String context) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addTargetLine(name).addTarget(n, name, scope, context);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputTarget(
      final Parameter n, final SimpleName name, final String scope, final String context) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addTargetLine(name).addTarget(n, name, scope, context);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputTarget(
      final ReferenceType n, final SimpleName name, final String scope, final String context) {
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addTargetLine(name).addTarget(n, name, scope, context);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  // Declarations

  @Override
  public void visit(ClassOrInterfaceDeclaration n, String a) {
    String scope = "";

    try {
      ResolvedReferenceTypeDeclaration decl = n.resolve();
      scope = getScope(decl.getQualifiedName(), n.getName());
    } catch (Exception e) {
    }

    outputSource(n, n.getName(), scope);
    outputTarget(n, n.getName(), scope, "");

    for (ClassOrInterfaceType classType : n.getExtendedTypes()) {
      // TODO: must be resolve
      outputSource(classType, classType.getName(), "");
      outputTarget(classType, classType.getName(), "", "");
    }
    for (ClassOrInterfaceType classType : n.getImplementedTypes()) {
      // TODO: must be resolve
      outputSource(classType, classType.getName(), "");
      outputTarget(classType, classType.getName(), "", "");
    }
    super.visit(n, a);
  }

  @Override
  public void visit(VariableDeclarator n, String a) {
    String scope = "";
    String context = "";
    boolean isVariable = false;

    if (!isLongTask()) {
    try {
      final ResolvedValueDeclaration decl = n.resolve();
      isVariable = decl.isVariable();
      if (decl.isField()) {
        final ResolvedTypeDeclaration typeDecl = decl.asField().declaringType();
        scope = typeDecl.getQualifiedName() + ".";
        context = typeDecl.getQualifiedName();
      }
    } catch (Exception e) {
      // not resolved
    }
    }

    outputSource(n, n.getName(), scope, isVariable);
    outputTarget(n, n.getName(), scope, context);

    // TODO: output type
    final Type type = n.getType();
    if (type.isClassOrInterfaceType()) {
      final ClassOrInterfaceType classType = type.asClassOrInterfaceType();
      outputSource(classType, classType.getName(), "");
      outputTarget(classType, classType.getName(), "", "");
    }

    super.visit(n, a);
  }

  @Override
  public void visit(ConstructorDeclaration n, String a) {
    String scope = "";
    String context = "";

    // Even if this analyze is too long, we resolve this.
    try {
      ResolvedReferenceTypeDeclaration decl = n.resolve().declaringType();
      scope = decl.getQualifiedName() + ".";
      context = decl.getQualifiedName();
    } catch (Exception e) {
      // not resolved
    }

    outputSource(n, n.getName(), scope);
    outputTarget(n, n.getName(), scope, context);

    // TODO: output type of parameters
    for (Parameter parameter : n.getParameters()) {
      outputSource(parameter, parameter.getName(), "");
      outputTarget(parameter, parameter.getName(), "", context);
    }

    super.visit(n, a);
  }

  @Override
  public void visit(MethodDeclaration n, String a) {
    String scope = "";
    String context = "";

    // Even if this analyze is too long, we resolve this.
    try {
      final ResolvedMethodDeclaration decl = n.resolve();
      scope = getScope(decl.getQualifiedName(), n.getName());
      if (scope.length() > 0) {
        context = scope.substring(0, scope.length() - 1);
      }
    } catch (Exception e) {
      // not resolved
    }

    outputSource(n, n.getName(), scope);
    outputTarget(n, n.getName(), scope, context);
    // output method name only too
    if (scope.length() > 0) {
      outputSource(n, n.getName(), "");
      outputTarget(n, n.getName(), "", context);
    }

    // TODO: output type of parameters
    for (Parameter parameter : n.getParameters()) {
      outputSource(parameter, parameter.getName(), "");
      outputTarget(parameter, parameter.getName(), "", context);
    }

    super.visit(n, a);
  }

  @Override
  public void visit(MethodCallExpr n, String a) {
    String scope = "";

    if (!isLongTask()) {
      try {
        final ResolvedMethodDeclaration decl = n.resolve();
        scope = getScope(decl.getQualifiedName(), n.getName());
      } catch (Exception e) {
        // not resolved.
      }
    }

    final String context = getContext(n);

    outputSource(n, n.getName(), scope);
    outputTarget(n, n.getName(), scope, context);

    super.visit(n, a);
  }

  @Override
  public void visit(NameExpr n, String a) {
    String scope = "";

    if (!isLongTask()) {
      try {
        final ResolvedValueDeclaration decl = n.resolve();
        if (decl.isField()) {
          final ResolvedTypeDeclaration typeDecl = decl.asField().declaringType();
          scope = typeDecl.getQualifiedName() + ".";
        }
      } catch (Exception e) {
        // not resolved
      }
    }

    final String context = getContext(n);

    outputSource(n, n.getName(), scope);
    outputTarget(n, n.getName(), scope, context);

    super.visit(n, a);
  }

  @Override
  public void visit(ObjectCreationExpr n, String a) {
    String scope = "";

    if (!isLongTask()) {
      try {
        final ResolvedConstructorDeclaration decl = n.resolve();
        scope = getScope(decl.getQualifiedName(), n.getType().getName());
      } catch (Exception e) {
        // not resolved
      }
    }

    final String context = getContext(n);

    outputSource(n, n.getType().getName(), scope);
    outputTarget(n, n.getType().getName(), scope, context);

    super.visit(n, a);
  }

  @Override
  public void visit(FieldAccessExpr n, String a) {
    String scope = "";

    if (!isLongTask()) {
      try {
        final ResolvedFieldDeclaration decl = n.resolve().asField();
        final ResolvedTypeDeclaration typeDecl = decl.declaringType();
        scope = typeDecl.getQualifiedName() + ".";
      } catch (Exception e) {
        // not resolved
      }
    }

    final String context = getContext(n);

    outputSource(n, n.getName(), scope);
    outputTarget(n, n.getName(), scope, context);

    super.visit(n, a);
  }
}
