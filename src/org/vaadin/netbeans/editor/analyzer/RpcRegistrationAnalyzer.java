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
import org.vaadin.netbeans.code.generator.JavaUtils;
import org.vaadin.netbeans.editor.VaadinTaskFactory;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;

/**
 * @author denis
 */
public class RpcRegistrationAnalyzer implements TypeAnalyzer {

    private static final String CLIENT_CONNECTOR = "com.vaadin.server.AbstractClientConnector"; // NOI18N

    private static final String CONNECTOR = "com.vaadin.client.ui.AbstractConnector"; // NOI18N 

    static final String REGISTER_RPC = "registerRpc"; // NOI18N 

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

        Collection<? extends TypeMirror> supertypes = JavaUtils.getSupertypes(
                type.asType(), info);
        boolean serverComponent = false;
        boolean clientComponent = false;
        for (TypeMirror superType : supertypes) {
            Element superElement = info.getTypes().asElement(superType);
            if (superElement instanceof TypeElement) {
                String fqn = ((TypeElement) superElement).getQualifiedName()
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

    @NbBundle.Messages("noClientRpc=Use Client RPC interface")
    private void findClientRpcUsage( TypeElement type, CompilationInfo info,
            Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {
        RegisterRpcScanner scanner = new RegisterRpcScanner(true);
        scanner.scan(info.getCompilationUnit(), info);
        if (!scanner.isFound()) {
            // TODO : add hint to <code>type</code> to create ClientRpc custom interface and use it in the code 
        }
    }

    @NbBundle.Messages("noServerRpc=No Server RPC interface")
    private void findServerRpcUsage( TypeElement type, CompilationInfo info,
            Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {
        RegisterRpcScanner scanner = new RegisterRpcScanner(false);
        scanner.scan(info.getCompilationUnit(), info);
        if (!scanner.isFound()) {
            List<Integer> positions = AbstractJavaFix.getElementPosition(info,
                    type);
            Fix fix = new CreateServerRpcFix(info.getFileObject(),
                    ElementHandle.create(type));
            ErrorDescription description = ErrorDescriptionFactory
                    .createErrorDescription(Severity.HINT,
                            Bundle.noServerRpc(),
                            Collections.singletonList(fix),
                            info.getFileObject(), positions.get(0),
                            positions.get(1));
            descriptions.add(description);
        }
    }

    private class RegisterRpcScanner extends
            TreePathScanner<Tree, CompilationInfo>
    {

        RegisterRpcScanner( boolean client ) {
            isClient = client;
        }

        @Override
        public Tree visitMethodInvocation( MethodInvocationTree tree,
                CompilationInfo info )
        {
            TreePath path = info.getTrees().getPath(info.getCompilationUnit(),
                    tree);
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
        public Tree visitVariable( VariableTree node, CompilationInfo p ) {
            // TODO : check RPC intafaces usage
            return super.visitVariable(node, p);
        }

        boolean isFound() {
            return isFound;
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

        private boolean isFound;

        private boolean isClient;
    }

}
