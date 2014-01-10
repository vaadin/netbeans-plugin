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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
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
import org.netbeans.spi.java.hints.HintContext;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.editor.hints.Analyzer;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;
import org.vaadin.netbeans.utils.JavaUtils;

/**
 * @author denis
 */
public class RpcRegistrationAnalyzer extends Analyzer {

    static final String CLIENT_CONNECTOR =
            "com.vaadin.server.AbstractClientConnector"; // NOI18N

    static final String CONNECTOR = "com.vaadin.client.ui.AbstractConnector"; // NOI18N 

    static final String REGISTER_RPC = "registerRpc"; // NOI18N 

    static final String SERVER_RPC =
            "com.vaadin.shared.communication.ServerRpc"; // NOI18N 

    static final String CLIENT_RPC =
            "com.vaadin.shared.communication.ClientRpc"; // NOI18N

    public RpcRegistrationAnalyzer( HintContext context ) {
        super(context);
    }

    @Override
    public void analyze() {
        Project project = FileOwnerQuery.getOwner(getInfo().getFileObject());
        if (project == null) {
            return;
        }
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        if (support == null || !support.isEnabled() || !support.isReady()) {
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
                JavaUtils.getSupertypes(getType().asType(), ElementKind.CLASS,
                        getInfo());
        boolean serverComponent = false;
        boolean clientComponent = false;
        for (TypeMirror superType : supertypes) {
            Element superElement = getInfo().getTypes().asElement(superType);
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
        if (isCanceled()) {
            return;
        }
        if (serverComponent) {
            findServerRpcUsage();
            findClientProxyRpcUsage();
        }
        if (isCanceled()) {
            return;
        }
        if (clientComponent) {
            findClientRpcUsage();
            findServerProxyRpcUsage();
        }
    }

    public ErrorDescription getNoServerRpc() {
        return myNoServerRpc;
    }

    public ErrorDescription getNoClientRpcProxy() {
        return myNoClientRpcProxy;
    }

    public ErrorDescription getNoClientRpc() {
        return myNoClientRpc;
    }

    public ErrorDescription getNoServerRpcProxy() {
        return myNoServerRpcProxy;
    }

    @NbBundle.Messages({ "noClientRpc=Client RPC interface is not used",
            "registerClientRpc=Client RPC interface is not registered" })
    private void findClientRpcUsage() {
        CompilationInfo info = getInfo();
        TypeElement type = getType();
        RegisterRpcScanner scanner = new RegisterRpcScanner(type, true);
        scanner.scan(info.getTrees().getPath(type), info);
        if (!scanner.isFound()) {
            List<Fix> fixes = new LinkedList<>();
            fixes.add(new CreateClientRpcFix(info.getFileObject(),
                    ElementHandle.create(type), scanner.getRpcVariable(),
                    scanner.getRpcVariableType()));
            TypeElement clientRpc =
                    info.getElements().getTypeElement(CLIENT_RPC);
            if (clientRpc != null) {
                try {
                    Set<TypeElement> subInterfaces =
                            JavaUtils.getSubinterfaces(clientRpc, info);
                    for (TypeElement iface : subInterfaces) {
                        if (IsInSourceQuery.isInSource(iface, info)) {
                            fixes.add(new CreateClientRpcFix(info
                                    .getFileObject(), ElementHandle
                                    .create(type), iface.getQualifiedName()
                                    .toString()));
                        }
                    }
                }
                catch (InterruptedException ignore) {
                }
            }
            String msg =
                    scanner.getRpcVariable() == null ? Bundle.noClientRpc()
                            : Bundle.registerClientRpc();
            myNoClientRpc = createHint(msg, fixes, type, info);
            getDescriptions().add(myNoClientRpc);
        }
    }

    @NbBundle.Messages({ "noServerRpc=Server RPC interface is not used",
            "registerServerRpc=Server RPC interface is not registered" })
    private void findServerRpcUsage() {
        CompilationInfo info = getInfo();
        TypeElement type = getType();
        RegisterRpcScanner scanner = new RegisterRpcScanner(type, false);
        scanner.scan(info.getTrees().getPath(type), info);
        if (!scanner.isFound()) {
            List<Fix> fixes = new LinkedList<>();
            fixes.add(new CreateServerRpcFix(info.getFileObject(),
                    ElementHandle.create(type), scanner.getRpcVariable(),
                    scanner.getRpcVariableType()));
            TypeElement serverRpc =
                    info.getElements().getTypeElement(SERVER_RPC);
            if (serverRpc != null) {
                try {
                    Set<TypeElement> subInterfaces =
                            JavaUtils.getSubinterfaces(serverRpc, info);
                    for (TypeElement iface : subInterfaces) {
                        if (IsInSourceQuery.isInSource(iface, info)) {
                            fixes.add(new CreateServerRpcFix(info
                                    .getFileObject(), ElementHandle
                                    .create(type), iface.getQualifiedName()
                                    .toString()));
                        }
                    }
                }
                catch (InterruptedException ignore) {
                }
            }
            String msg =
                    scanner.getRpcVariable() == null ? Bundle.noServerRpc()
                            : Bundle.registerServerRpc();
            myNoServerRpc = createHint(msg, fixes, type, info);
            getDescriptions().add(myNoServerRpc);
        }
    }

    @NbBundle.Messages("noClientRpcProxy=Client RPC interface proxy is not used")
    private void findClientProxyRpcUsage() {
        if (isCanceled()) {
            return;
        }
        CompilationInfo info = getInfo();
        TypeElement type = getType();
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
                        if (IsInSourceQuery.isInSource(iface, info)) {
                            fixes.add(new ClientRpcProxyFix(info
                                    .getFileObject(), iface.getQualifiedName()
                                    .toString(), ElementHandle.create(type)));
                        }
                    }
                }
                catch (InterruptedException ignore) {
                }
            }
            myNoClientRpcProxy =
                    createHint(Bundle.noClientRpcProxy(), fixes, type, info);
            getDescriptions().add(myNoClientRpcProxy);
        }
    }

    @NbBundle.Messages("noServerRpcProxy=Server RPC interface proxy is not used")
    private void findServerProxyRpcUsage() {
        if (isCanceled()) {
            return;
        }
        CompilationInfo info = getInfo();
        TypeElement type = getType();
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
                        if (IsInSourceQuery.isInSource(iface, info)) {
                            fixes.add(new ServerRpcProxyFix(info
                                    .getFileObject(), iface.getQualifiedName()
                                    .toString(), ElementHandle.create(type)));
                        }
                    }
                }
                catch (InterruptedException ignore) {
                }
            }
            myNoServerRpcProxy =
                    createHint(Bundle.noServerRpcProxy(), fixes, type, info);
            getDescriptions().add(myNoServerRpcProxy);

        }
    }

    private ErrorDescription createHint( String msg, List<Fix> fixes,
            TypeElement type, CompilationInfo info )
    {
        List<Integer> positions =
                AbstractJavaFix.getElementPosition(info, type);
        return ErrorDescriptionFactory.createErrorDescription(
                getSeverity(Severity.HINT), msg, fixes, info.getFileObject(),
                positions.get(0), positions.get(1));
    }

    private ErrorDescription myNoServerRpc;

    private ErrorDescription myNoClientRpcProxy;

    private ErrorDescription myNoClientRpc;

    private ErrorDescription myNoServerRpcProxy;
}
