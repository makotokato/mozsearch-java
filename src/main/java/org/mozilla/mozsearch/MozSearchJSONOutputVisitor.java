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
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
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
  private long mTimeout = 1000 * 60; // 1 min

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
    return (System.currentTimeMillis() - mStart) > mTimeout;
  }

  public void setTimeoutForResolution(long timeout) {
    mTimeout = timeout;
  }

  private static String getScope(final String fullName, final SimpleName name) {
    return fullName.substring(0, fullName.length() - name.toString().length());
  }

  private String getScopeForParameterType(final Parameter parameter) {
    if (isLongTask()) {
      return "";
    }
    final Type type = parameter.getType();
    if (!type.isClassOrInterfaceType() && !type.isArrayType()) {
      return "";
    }
    try {
      final ResolvedParameterDeclaration decl = parameter.resolve();
      if (decl.getType().isReferenceType()) {
        return getScope(
            decl.getType().asReferenceType().getQualifiedName(),
            type.asClassOrInterfaceType().getName());
      } else if (decl.getType().isArray()) {
        // TODO: support n-array.
        final ResolvedType typeInArray = decl.getType().asArrayType().getComponentType();
        if (typeInArray.isReferenceType()) {
          return getScope(
              typeInArray.asReferenceType().getQualifiedName(),
              type.asArrayType().getComponentType().asClassOrInterfaceType().getName());
        }
      }
    } catch (Exception e) {
    }
    return "";
  }

  private String getScopeOfType(final Type type, final ResolvedReferenceType resolvedType) {
    if (resolvedType == null) {
      return "";
    }
    if (!type.isClassOrInterfaceType()) {
      return "";
    }

    return getScope(resolvedType.getQualifiedName(), type.asClassOrInterfaceType().getName());
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

  private void outputSource(final Type n, final String scope) {
    if (n.isClassOrInterfaceType()) {
      final ClassOrInterfaceType classType = n.asClassOrInterfaceType();
      outputSource(classType, scope);
    } else if (n.isArrayType()) {
      final Type typeInArray = n.asArrayType().getComponentType();
      outputSource(typeInArray, scope);
    }
  }

  private void outputSource(final ClassOrInterfaceType n, final String scope) {
    final SimpleName name = n.getName();
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

  private void outputSource(final MethodDeclaration n, final String scope) {
    final SimpleName name = n.getName();
    final String fullName = scope + name.getIdentifier();

    MozSearchJSONObject obj = new MozSearchJSONObject();
    obj.addSourceLine(name).addSource(n, name, scope);
    obj.addSymbol(fullName);

    outputJSON(obj);
  }

  private void outputSource(
      final VariableDeclarator n, final SimpleName name, final String scope, boolean isVariable) {
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

  private void outputSource(final Parameter n, final String scope) {
    final SimpleName name = n.getName();
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

  private void outputTarget(final Type n, final String scope, final String context) {
    if (n.isClassOrInterfaceType()) {
      final ClassOrInterfaceType type = n.asClassOrInterfaceType();
      outputTarget(type, scope, context);
    } else if (n.isArrayType()) {
      final Type typeInArray = n.asArrayType().getComponentType();
      outputTarget(typeInArray, scope, context);
    }
  }

  private void outputTarget(
      final ClassOrInterfaceType n, final String scope, final String context) {
    final SimpleName name = n.getName();
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

  private void outputTarget(final MethodDeclaration n, final String scope, final String context) {
    final SimpleName name = n.getName();
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

  private void outputTarget(final Parameter n, final String scope, final String context) {
    final SimpleName name = n.getName();
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
      // TODO: must resolve
      outputSource(classType, "");
      outputTarget(classType, "", "");
    }
    for (ClassOrInterfaceType classType : n.getImplementedTypes()) {
      // TODO: must resolve
      outputSource(classType, "");
      outputTarget(classType, "", "");
    }
    super.visit(n, a);
  }

  @Override
  public void visit(VariableDeclarator n, String a) {
    String scope = "";
    String context = "";
    boolean isVariable = false;
    ResolvedReferenceType resolvedType = null;

    if (!isLongTask()) {
      try {
        final ResolvedValueDeclaration decl = n.resolve();
        isVariable = decl.isVariable();
        if (decl.isField()) {
          final ResolvedTypeDeclaration typeDecl = decl.asField().declaringType();
          scope = typeDecl.getQualifiedName() + ".";
          context = typeDecl.getQualifiedName();
        }
        if (decl.getType().isReferenceType()) {
          resolvedType = decl.getType().asReferenceType();
        }
      } catch (Exception e) {
        // not resolved
      }
    }

    outputSource(n, n.getName(), scope, isVariable);
    outputTarget(n, n.getName(), scope, context);

    final Type type = n.getType();
    final String typeScope = getScopeOfType(type, resolvedType);
    outputSource(type, typeScope);
    outputTarget(type, typeScope, context);

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

    for (Parameter parameter : n.getParameters()) {
      final Type type = parameter.getType();
      final String typeScope = getScopeForParameterType(parameter);
      outputSource(type, typeScope);
      outputTarget(type, typeScope, context);

      outputSource(parameter, "");
      outputTarget(parameter, "", context);
    }

    super.visit(n, a);
  }

  @Override
  public void visit(MethodDeclaration n, String a) {
    String scope = "";
    String context = "";
    ResolvedReferenceType resolvedType = null;

    // Even if this analyze is too long, we resolve this.
    try {
      final ResolvedMethodDeclaration decl = n.resolve();
      scope = getScope(decl.getQualifiedName(), n.getName());
      if (scope.length() > 0) {
        context = scope.substring(0, scope.length() - 1);
      }
      if (decl.getReturnType().isReferenceType()) {
        resolvedType = decl.getReturnType().asReferenceType();
      }
    } catch (Exception e) {
      // not resolved
    }

    outputSource(n, scope);
    outputTarget(n, scope, context);
    // output method name only too
    if (scope.length() > 0) {
      outputSource(n, "");
      outputTarget(n, "", context);
    }

    // Output parameters
    for (Parameter parameter : n.getParameters()) {
      final Type type = parameter.getType();
      final String typeScope = getScopeForParameterType(parameter);
      outputSource(type, typeScope);
      outputTarget(type, typeScope, context);

      outputSource(parameter, "");
      outputTarget(parameter, "", context);
    }

    // Output return type
    final Type type = n.getType();
    final String typeScope = getScopeOfType(type, resolvedType);
    outputSource(type, typeScope);
    outputTarget(type, typeScope, context);

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
