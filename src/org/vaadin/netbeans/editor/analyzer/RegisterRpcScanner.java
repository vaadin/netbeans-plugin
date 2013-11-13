/*
 * Copyright 2000-2013 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.vaadin.netbeans.editor.analyzer;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.CompilationInfo;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;

class RegisterRpcScanner extends
        TreePathScanner<Tree, CompilationInfo>
{

    RegisterRpcScanner( TypeElement type, boolean client ) {
        isClient = client;
        myType = type;
    }

    @Override
    public Tree visitMethodInvocation( MethodInvocationTree tree,
            CompilationInfo info )
    {
        TreePath path =
                info.getTrees().getPath(info.getCompilationUnit(), tree);
        if (path != null) {
            Element element = info.getTrees().getElement(path);
            if (element instanceof ExecutableElement) {
                ExecutableElement method = (ExecutableElement) element;
                if (isRegisterRpc(method, info)) {
                    isFound = true;
                }
            }
        }
        return super.visitMethodInvocation(tree, info);
    }

    @Override
    public Tree visitVariable( VariableTree tree, CompilationInfo info ) {
        TreePath variablePath =
                info.getTrees().getPath(info.getCompilationUnit(), tree);
        if (variablePath == null) {
            return super.visitVariable(tree, info);
        }
        Element varElement = info.getTrees().getElement(variablePath);
        if (varElement == null
                || !myType.equals(varElement.getEnclosingElement()))
        {
            return super.visitVariable(tree, info);
        }

        ExpressionTree initializer = tree.getInitializer();
        boolean isRpc = false;
        if (initializer instanceof NewClassTree) {
            NewClassTree newTree = (NewClassTree) initializer;
            TreePath path =
                    info.getTrees().getPath(info.getCompilationUnit(),
                            newTree);
            if (path != null) {
                Element element = info.getTrees().getElement(path);
                if (element instanceof ExecutableElement) {
                    TypeElement clazz =
                            info.getElementUtilities()
                                    .enclosingTypeElement(element);
                    if (isClient) {
                        Element clientRpc =
                                info.getElements().getTypeElement(
                                        RpcRegistrationAnalyzer.CLIENT_RPC);
                        if (clientRpc != null
                                && info.getTypes().isSubtype(
                                        clazz.asType(), clientRpc.asType()))
                        {
                            isRpc = true;
                        }
                    }
                    else {
                        Element serverRpc =
                                info.getElements().getTypeElement(
                                        RpcRegistrationAnalyzer.SERVER_RPC);
                        if (serverRpc != null
                                && info.getTypes().isSubtype(
                                        clazz.asType(), serverRpc.asType()))
                        {
                            isRpc = true;
                        }
                    }
                }
            }
        }
        if (isRpc) {
            myRpcVar = tree.getName().toString();
            myRpcVarType =
                    ((TypeElement) info.getTypes().asElement(
                            varElement.asType())).getQualifiedName()
                            .toString();
        }
        return super.visitVariable(tree, info);
    }

    protected boolean isRegisterRpc( ExecutableElement method,
            CompilationInfo info )
    {
        if (!RpcRegistrationAnalyzer.REGISTER_RPC.contentEquals(method.getSimpleName())) {
            return false;
        }
        Element enclosingElement = method.getEnclosingElement();
        if (enclosingElement instanceof TypeElement) {
            TypeElement type = (TypeElement) enclosingElement;
            if (isClient
                    && type.getQualifiedName().contentEquals(RpcRegistrationAnalyzer.CONNECTOR))
            {
                return true;
            }
            else if (!isClient
                    && type.getQualifiedName().contentEquals(
                            RpcRegistrationAnalyzer.CLIENT_CONNECTOR))
            {
                return true;
            }
        }
        return false;
    }

    boolean isFound() {
        return isFound;
    }

    String getRpcVariable() {
        return myRpcVar;
    }

    String getRpcVariableType() {
        return myRpcVarType;
    }

    private boolean isFound;

    private boolean isClient;

    private String myRpcVar;

    private String myRpcVarType;

    private TypeElement myType;
}