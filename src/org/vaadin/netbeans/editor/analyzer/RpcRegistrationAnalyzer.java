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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.editor.VaadinTaskFactory;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;
import org.vaadin.netbeans.utils.JavaUtils;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;

/**
 * @author denis
 */
public class RpcRegistrationAnalyzer implements TypeAnalyzer {

    private static final String CLIENT_CONNECTOR =
            "com.vaadin.server.AbstractClientConnector"; // NOI18N

    private static final String CONNECTOR =
            "com.vaadin.client.ui.AbstractConnector"; // NOI18N 

    static final String REGISTER_RPC = "registerRpc"; // NOI18N 

    private static final String SERVER_RPC =
            "com.vaadin.shared.communication.ServerRpc"; // NOI18N 

    private static final String CLIENT_RPC =
            "com.vaadin.shared.communication.ClientRpc"; // NOI18N 

    @Override
    public void analyze( TypeElement type, CompilationInfo info,
            Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {
        Project project = FileOwnerQuery.getOwner(info.getFileObject());
        if (project == null) {
            return;
        }
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        if (support == null || !support.isEnabled()) {
            return;
        }
        final boolean[] hasGwtXml = new boolean[1];
        try {
            support.runModelOperation(new ModelOperation() {

                @Override
                public void run( VaadinModel model ) {
                    hasGwtXml[0] = model.getGwtXml() != null;
                }
            });
        }
        catch (IOException ignore) {
        }
        if (!hasGwtXml[0]) {
            return;
        }

        Collection<? extends TypeMirror> supertypes =
                JavaUtils.getSupertypes(type.asType(), info);
        boolean serverComponent = false;
        boolean clientComponent = false;
        for (TypeMirror superType : supertypes) {
            Element superElement = info.getTypes().asElement(superType);
            if (superElement instanceof TypeElement) {
                String fqn =
                        ((TypeElement) superElement).getQualifiedName()
                                .toString();
                if (fqn.equals(CLIENT_CONNECTOR)) {
                    serverComponent = true;
                    break;
                }
                else if (fqn.equals(CONNECTOR)) {
                    clientComponent = true;
                    break;
                }
            }
        }
        if (cancel.get()) {
            return;
        }
        if (serverComponent) {
            findServerRpcUsage(type, info, descriptions, factory, cancel);
        }
        if (cancel.get()) {
            return;
        }
        if (clientComponent) {
            findClientRpcUsage(type, info, descriptions, factory, cancel);
        }
    }

    @NbBundle.Messages({ "noClientRpc=Client RPC interface is not used",
            "registerClientRpc=Client RPC interface is not registered" })
    private void findClientRpcUsage( TypeElement type, CompilationInfo info,
            Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {
        RegisterRpcScanner scanner = new RegisterRpcScanner(type, true);
        scanner.scan(info.getTrees().getPath(type), info);
        if (!scanner.isFound()) {
            List<Integer> positions =
                    AbstractJavaFix.getElementPosition(info, type);
            Fix fix =
                    new CreateClientRpcFix(info.getFileObject(),
                            ElementHandle.create(type),
                            scanner.getRpcVariable(),
                            scanner.getRpcVariableType());
            String msg =
                    scanner.getRpcVariable() == null ? Bundle.noClientRpc()
                            : Bundle.registerClientRpc();
            ErrorDescription description =
                    ErrorDescriptionFactory.createErrorDescription(
                            Severity.HINT, msg, Collections.singletonList(fix),
                            info.getFileObject(), positions.get(0),
                            positions.get(1));
            descriptions.add(description);
        }
    }

    @NbBundle.Messages({ "noServerRpc=Server RPC interface is not used",
            "registerServerRpc=Server RPC interface is not registered" })
    private void findServerRpcUsage( TypeElement type, CompilationInfo info,
            Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {
        RegisterRpcScanner scanner = new RegisterRpcScanner(type, false);
        scanner.scan(info.getTrees().getPath(type), info);
        if (!scanner.isFound()) {
            List<Integer> positions =
                    AbstractJavaFix.getElementPosition(info, type);
            Fix fix =
                    new CreateServerRpcFix(info.getFileObject(),
                            ElementHandle.create(type),
                            scanner.getRpcVariable(),
                            scanner.getRpcVariableType());
            String msg =
                    scanner.getRpcVariable() == null ? Bundle.noServerRpc()
                            : Bundle.registerServerRpc();
            ErrorDescription description =
                    ErrorDescriptionFactory.createErrorDescription(
                            Severity.HINT, msg, Collections.singletonList(fix),
                            info.getFileObject(), positions.get(0),
                            positions.get(1));
            descriptions.add(description);
        }
    }

    private class RegisterRpcScanner extends
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
                                            CLIENT_RPC);
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
                                            SERVER_RPC);
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
            if (!REGISTER_RPC.contentEquals(method.getSimpleName())) {
                return false;
            }
            Element enclosingElement = method.getEnclosingElement();
            if (enclosingElement instanceof TypeElement) {
                TypeElement type = (TypeElement) enclosingElement;
                if (isClient
                        && type.getQualifiedName().contentEquals(CONNECTOR))
                {
                    return true;
                }
                else if (!isClient
                        && type.getQualifiedName().contentEquals(
                                CLIENT_CONNECTOR))
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

}
