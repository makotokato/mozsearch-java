package net.wontfix.mozsearch;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.util.Optional;

public class MozSearchVisitor extends VoidVisitorAdapter<String> {
    private CombinedTypeSolver mSolver;

    public MozSearchVisitor(CombinedTypeSolver aSolver) {
        mSolver = aSolver;
    }

    private static void outputSource(final Node n, final SimpleName name) {
        outputSource(n, name, "");
    }
    private static void outputSource(final Node n, final SimpleName name, final String scope) {
        System.out.print("{\"loc\":\"");
        System.out.print(name.getBegin().get().line + ":" + name.getBegin().get().column + "-" + (name.getBegin().get().column + name.getIdentifier().length()));
        System.out.print("\",");
        System.out.print("\"source\":1,");
        if (n instanceof ClassOrInterfaceDeclaration) {
            System.out.print("\"syntax\":\"def,prop\",");
            System.out.print("\"pretty\":\"type " + scope + name.getIdentifier() + "\",");
        } else if (n instanceof ClassOrInterfaceType) {
            System.out.print("\"syntax\":\"use,prop\",");
            System.out.print("\"pretty\":\"type " + name.getIdentifier() + "\",");
        } else if (n instanceof VariableDeclarator) {
            System.out.print("\"syntax\":\"def,prop\",");
            System.out.print("\"pretty\":\"local " + name.getIdentifier() + "\",");
        } else if (n instanceof Parameter) {
            System.out.print("\"syntax\":\"def,prop\",");
            System.out.print("\"pretty\":\"local " + name.getIdentifier() + "\",");
        } else if (n instanceof MethodDeclaration) {
            System.out.print("\"syntax\":\"decl\",");
            System.out.print("\"pretty\":\"function " + name.getIdentifier() + "\",");
        } else if (n instanceof MethodCallExpr) {
            System.out.print("\"syntax\":\"use,prop\",");
            System.out.print("\"pretty\":\"function " + name.getIdentifier() + "\",");
        } else if (n instanceof NameExpr) {
            System.out.print("\"syntax\":\"use,prop\",");
            System.out.print("\"pretty\":\"property " + name.getIdentifier() + "\",");
        }
        System.out.print("\"sym\":\"" + scope + name.getIdentifier() + "\"");
        System.out.println("},");
    }

    private void outputTarget(final Node n, final SimpleName name) {
        outputTarget(n, name, "");
    }
    private void outputTarget(final Node n, final SimpleName name, final String scope) {
        System.out.print("{\"loc\":\"");
        System.out.print(name.getBegin().get().line + ":" + name.getBegin().get().column);
        System.out.print("\",");
        System.out.print("\"target\":1,");

        if (n instanceof ClassOrInterfaceDeclaration) {
            System.out.print("\"kind\":\"decl\",");
            System.out.print("\"pretty\":\"type " + scope + name.getIdentifier() + "\",");
        } else if (n instanceof ClassOrInterfaceType) {
            System.out.print("\"kind\":\"use\",");
            System.out.print("\"pretty\":\"type " + name.getIdentifier() + "\",");
        } else if (n instanceof Parameter) {
            System.out.print("\"kind\":\"def\",");
            System.out.print("\"pretty\":\"local " + name.getIdentifier() + "\",");
        } else if (n instanceof VariableDeclarator) {
            System.out.print("\"kind\":\"def\",");
            System.out.print("\"pretty\":\"local " + name.getIdentifier() + "\",");
        } else if (n instanceof MethodDeclaration) {
            System.out.print("\"kind\":\"decl\",");
            System.out.print("\"pretty\":\"function " + name.getIdentifier() + "\",");
        } else if (n instanceof MethodCallExpr) {
            System.out.print("\"kind\":\"use\",");
            System.out.print("\"pretty\":\"function " + name.getIdentifier() + "\",");
        } else if (n instanceof NameExpr) {
            System.out.print("\"kind\":\"use\",");
            System.out.print("\"pretty\":\"property " + name.getIdentifier() + "\",");
        }

        System.out.print("\"sym\":\"" + scope + name.getIdentifier() + "\"");
        System.out.println("},");
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
                scope = a + ((ClassOrInterfaceDeclaration)parent.get()).getName().getIdentifier() + ".";
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
               scope = a + ((ClassOrInterfaceDeclaration)parent.get()).getName().getIdentifier() + ".";
           }
       } catch (Exception e) {
       }
       outputSource(n, n.getName(), scope);
       outputTarget(n, n.getName(), scope);
       for (Parameter param : n.getParameters()) {
          //outputSource(param, param.getName());
          //outputTarget(param, param.getName());
       }
       super.visit(n, a);
    }

    @Override
    public void visit(MethodCallExpr n, String a) {
       String scope = "";
       try {
           ResolvedMethodDeclaration decl = JavaParserFacade.get(mSolver).solve(n).getCorrespondingDeclaration();
           scope = decl.getPackageName() + "." + decl.getClassName() + ".";
       } catch (Exception e) {
       }
       outputSource(n, n.getName(), scope);
       outputTarget(n, n.getName(), scope);

       super.visit(n, a);
    }

    @Override
    public void visit(NameExpr n, String a) {
       //outputSource(n, n.getName());
       //outputTarget(n, n.getName());
       super.visit(n, a);
    }
}
