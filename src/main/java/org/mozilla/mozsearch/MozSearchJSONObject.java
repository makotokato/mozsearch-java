package org.mozilla.mozsearch;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.json.JSONObject;

public class MozSearchJSONObject extends JSONObject {
  public MozSearchJSONObject() {
    super();
  }

  public MozSearchJSONObject addSourceLine(final SimpleName name) {
    put(
            "loc",
            name.getBegin().get().line
                + ":"
                + (name.getBegin().get().column - 1)
                + "-"
                + (name.getBegin().get().column - 1 + name.getIdentifier().length()))
        .put("source", 1);
    return this;
  }

  public MozSearchJSONObject addTargetLine(final SimpleName name) {
    put("loc", name.getBegin().get().line + ":" + (name.getBegin().get().column - 1))
        .put("target", 1);
    return this;
  }

  public JSONObject addSource(
      final ClassOrInterfaceDeclaration n, final SimpleName name, final String scope) {
    if (((ClassOrInterfaceDeclaration) n).isInterface()) {
      return put("syntax", "def,type").put("pretty", "interface " + scope + name.getIdentifier());
    }
    return put("syntax", "def,type").put("pretty", "class " + scope + name.getIdentifier());
  }

  public JSONObject addSource(
      final ClassOrInterfaceType n, final SimpleName name, final String scope) {
    return put("syntax", "type,use").put("pretty", "class " + scope + name.getIdentifier());
  }

  public JSONObject addSource(
      final ConstructorDeclaration n, final SimpleName name, final String scope) {
    return put("syntax", "def,function")
        .put("pretty", "constructor " + scope + name.getIdentifier());
  }

  public JSONObject addSource(
      final MethodDeclaration n, final SimpleName name, final String scope) {
    return put("syntax", "def,function").put("pretty", "method " + scope + name.getIdentifier());
  }

  public JSONObject addSource(
      final VariableDeclarator n, final SimpleName name, final String scope) {
    return put("syntax", "def").put("pretty", "field " + scope + name.getIdentifier());
  }

  public JSONObject addSource(final MethodCallExpr n, final SimpleName name, final String scope) {
    return put("syntax", "use,function").put("pretty", "method " + scope + name.getIdentifier());
  }

  public JSONObject addSource(final FieldAccessExpr n, final SimpleName name, final String scope) {
    return put("syntax", "use,variable").put("pretty", "field " + scope + name.getIdentifier());
  }

  public JSONObject addTarget(
      final ClassOrInterfaceDeclaration n,
      final SimpleName name,
      final String scope,
      final String context) {
    return put("kind", "def").put("pretty", scope + name.getIdentifier());
  }

  public JSONObject addTarget(
      final ClassOrInterfaceType n,
      final SimpleName name,
      final String scope,
      final String context) {
    return put("kind", "use").put("pretty", scope + name.getIdentifier()).put("context", context);
  }

  public JSONObject addTarget(
      final ConstructorDeclaration n,
      final SimpleName name,
      final String scope,
      final String context) {
    return put("kind", "def").put("pretty", scope + name.getIdentifier()).put("context", context);
  }

  public JSONObject addTarget(
      final MethodDeclaration n, final SimpleName name, final String scope, final String context) {
    return put("kind", "def").put("pretty", scope + name.getIdentifier()).put("context", context);
  }

  public JSONObject addTarget(
      final VariableDeclarator n, final SimpleName name, final String scope, final String context) {
    return put("kind", "def").put("pretty", scope + name.getIdentifier()).put("context", context);
  }

  public JSONObject addTarget(
      final MethodCallExpr n, final SimpleName name, final String scope, final String context) {
    return put("kind", "use").put("pretty", scope + name.getIdentifier()).put("context", context);
  }

  public JSONObject addTarget(
      final FieldAccessExpr n, final SimpleName name, final String scope, final String context) {
    return put("kind", "use").put("pretty", scope + name.getIdentifier()).put("context", context);
  }
}
