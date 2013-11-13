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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.editor.VaadinTaskFactory;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;
import org.vaadin.netbeans.utils.JavaUtils;

/**
 * @author denis
 */
public class RpcRegistrationAnalyzer implements TypeAnalyzer {

    static final String CLIENT_CONNECTOR =
            "com.vaadin.server.AbstractClientConnector"; // NOI18N

    static final String CONNECTOR = "com.vaadin.client.ui.AbstractConnector"; // NOI18N 

    static final String REGISTER_RPC = "registerRpc"; // NOI18N 

    static final String SERVER_RPC =
            "com.vaadin.shared.communication.ServerRpc"; // NOI18N 

    static final String CLIENT_RPC =
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
            findClientProxyRpcUsage(type, info, descriptions, factory, cancel);
        }
        if (cancel.get()) {
            return;
        }
        if (clientComponent) {
            findClientRpcUsage(type, info, descriptions, factory, cancel);
            findServerProxyRpcUsage(type, info, descriptions, factory, cancel);
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
            String msg =
                    scanner.getRpcVariable() == null ? Bundle.noClientRpc()
                            : Bundle.registerClientRpc();
            descriptions.add(createHint(
                    msg,
                    new CreateClientRpcFix(info.getFileObject(), ElementHandle
                            .create(type), scanner.getRpcVariable(), scanner
                            .getRpcVariableType()), type, info));
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
            String msg =
                    scanner.getRpcVariable() == null ? Bundle.noServerRpc()
                            : Bundle.registerServerRpc();
            descriptions.add(createHint(
                    msg,
                    new CreateServerRpcFix(info.getFileObject(), ElementHandle
                            .create(type), scanner.getRpcVariable(), scanner
                            .getRpcVariableType()), type, info));
        }
    }

    @NbBundle.Messages("noClientRpcProxy=Client RPC interface proxy is not used")
    private void findClientProxyRpcUsage( TypeElement type,
            CompilationInfo info, Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {
        if (cancel.get()) {
            return;
        }
        ClientRpcProxyScanner scanner = new ClientRpcProxyScanner();
        scanner.scan(info.getTrees().getPath(type), info);
        if (!scanner.hasRpcProxyAccess()) {
            List<Fix> fixes = new LinkedList<>();
            fixes.add(new ClientRpcProxyFix(info.getFileObject(), ElementHandle
                    .create(type)));
            TypeElement clientRpc =
                    info.getElements().getTypeElement(CLIENT_RPC);
            if (clientRpc != null) {
                try {
                    Set<TypeElement> subInterfaces =
                            JavaUtils.getSubinterfaces(clientRpc, info);
                    for (TypeElement iface : subInterfaces) {
                        if (isInSource(iface, info)) {
                            fixes.add(new ClientRpcProxyFix(info
                                    .getFileObject(), iface.getQualifiedName()
                                    .toString(), ElementHandle.create(type)));
                        }
                    }
                }
                catch (InterruptedException ignore) {
                }
            }
            descriptions.add(createHint(Bundle.noClientRpcProxy(), fixes, type,
                    info));
        }
    }

    @NbBundle.Messages("noServerRpcProxy=Server RPC interface proxy is not used")
    private void findServerProxyRpcUsage( TypeElement type,
            CompilationInfo info, Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {
        if (cancel.get()) {
            return;
        }
        ServerRpcProxyScanner scanner = new ServerRpcProxyScanner();
        scanner.scan(info.getTrees().getPath(type), info);
        if (!scanner.hasRpcProxyAccess()) {
            List<Fix> fixes = new LinkedList<>();
            fixes.add(new ServerRpcProxyFix(info.getFileObject(), ElementHandle
                    .create(type)));
            TypeElement serverRpc =
                    info.getElements().getTypeElement(SERVER_RPC);
            if (serverRpc != null) {
                try {
                    Set<TypeElement> subInterfaces =
                            JavaUtils.getSubinterfaces(serverRpc, info);
                    for (TypeElement iface : subInterfaces) {
                        if (isInSource(iface, info)) {
                            fixes.add(new ServerRpcProxyFix(info
                                    .getFileObject(), iface.getQualifiedName()
                                    .toString(), ElementHandle.create(type)));
                        }
                    }
                }
                catch (InterruptedException ignore) {
                }
            }
            descriptions.add(createHint(Bundle.noServerRpcProxy(), fixes, type,
                    info));

        }
    }

    private ErrorDescription createHint( String msg, Fix fix, TypeElement type,
            CompilationInfo info )
    {
        return createHint(msg, Collections.singletonList(fix), type, info);
    }

    private ErrorDescription createHint( String msg, List<Fix> fixes,
            TypeElement type, CompilationInfo info )
    {
        List<Integer> positions =
                AbstractJavaFix.getElementPosition(info, type);
        return ErrorDescriptionFactory.createErrorDescription(Severity.HINT,
                msg, fixes, info.getFileObject(), positions.get(0),
                positions.get(1));
    }

    private boolean isInSource( TypeElement type, CompilationInfo info ) {
        FileObject fileObject =
                SourceUtils.getFile(ElementHandle.create(type),
                        info.getClasspathInfo());
        if (fileObject == null) {
            return false;
        }
        Project project = FileOwnerQuery.getOwner(info.getFileObject());
        SourceGroup[] groups = JavaUtils.getJavaSourceGroups(project);
        for (SourceGroup sourceGroup : groups) {
            FileObject rootFolder = sourceGroup.getRootFolder();
            if (FileUtil.isParentOf(rootFolder, fileObject)) {
                return true;
            }
        }
        return false;
    }
}
