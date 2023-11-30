package com.epf.systemf.jumbotrace;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import java.util.LinkedList;
import java.util.List;

import static com.github.javaparser.ast.type.PrimitiveType.*;

public final class Transformer extends ModifierVisitor<Void> {

    private static final String TARGET_ANNOTATION_NAME = "Specialize";
    private static final String MODIFIED_METH_ANNOTATION_NAME = "Specialized";
    private static final String MODIFIED_METH_ANNOT_FLD_KEY = "typeName";

    private static final List<Type> TOPMOST_TYPES = List.of(
            intType(), shortType(), longType(), floatType(), doubleType(), booleanType(), charType(), byteType(),
            StaticJavaParser.parseType("java.lang.Object")
    );

    private static final String RAW_PACKAGE_NAME = "raw";
    private static final String PROCESSED_PACKAGE_NAME = "processed";

    @Override
    public Visitable visit(CompilationUnit n, Void arg) {
        super.visit(n, arg);
        n.getPackageDeclaration().ifPresent(packageDeclaration -> {
            var name = packageDeclaration.getName();
            if (!name.getIdentifier().equals(RAW_PACKAGE_NAME)){
                throw new AssertionError();
            }
            name.setIdentifier(PROCESSED_PACKAGE_NAME);
        });
        return n;
    }

    @Override
    public Visitable visit(ClassOrInterfaceDeclaration classOrInterfaceDecl, Void arg) {
        super.visit(classOrInterfaceDecl, arg);
        var newMembersList = new NodeList<BodyDeclaration<?>>();
        for (var member: classOrInterfaceDecl.getMembers()){
            if (member instanceof MethodDeclaration methodDeclaration && checkAndDeleteTargetAnnotation(methodDeclaration.getAnnotations())) {
                newMembersList.addAll(replicate(methodDeclaration));
            } else {
                newMembersList.add(member);
            }
        }
        classOrInterfaceDecl.setMembers(newMembersList);
        return classOrInterfaceDecl;
    }

    private List<MethodDeclaration> replicate(MethodDeclaration methodDeclaration) {
        var specializedMethods = new LinkedList<MethodDeclaration>();
        for (var type: TOPMOST_TYPES){
            specializedMethods.add(copyWithType(methodDeclaration, type));
        }
        return specializedMethods;
    }

    private MethodDeclaration copyWithType(MethodDeclaration methodDeclaration, Type type){
        var copy = methodDeclaration.clone();
        copy.setType(type);
        copy.getParameters().forEach(parameter -> {
            var mustReplicate = checkAndDeleteTargetAnnotation(parameter.getAnnotations());
            if (mustReplicate){
                parameter.setType(type);
            }
        });
        copy.getAnnotations().add(new NormalAnnotationExpr(
                new Name(MODIFIED_METH_ANNOTATION_NAME),
                new NodeList<>(new MemberValuePair(MODIFIED_METH_ANNOT_FLD_KEY, new StringLiteralExpr(type.toString())))
        ));
        return copy;
    }

    private boolean checkAndDeleteTargetAnnotation(NodeList<AnnotationExpr> annotations){
        AnnotationExpr found = null;
        for (var annot : annotations) {
            if (annot.getName().getIdentifier().equals(Transformer.TARGET_ANNOTATION_NAME)) {
                found = annot;
                break;
            }
        }
        if (found == null){
            return false;
        } else {
            annotations.remove(found);
            return true;
        }
    }

}
