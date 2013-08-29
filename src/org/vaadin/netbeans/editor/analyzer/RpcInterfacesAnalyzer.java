package org.vaadin.netbeans.editor.analyzer;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.vaadin.netbeans.editor.VaadinTaskFactory;

/**
 * @author denis
 */
public class RpcInterfacesAnalyzer extends ClientClassAnalyzer {

    private static final String CLIENT_RPC = "com.vaadin.shared.communication.ClientRpc"; // NOI18N

    private static final String SERVER_RPC = "com.vaadin.shared.communication.ServerRpc"; // NOI18N

    @Override
    protected boolean isClientClass( TypeElement type, CompilationInfo info ) {
        TypeElement clientRpc = info.getElements().getTypeElement(CLIENT_RPC);
        if (clientRpc != null) {
            if (info.getTypes().isSubtype(type.asType(), clientRpc.asType())) {
                return true;
            }
        }
        TypeElement serverRpc = info.getElements().getTypeElement(SERVER_RPC);
        if (serverRpc == null) {
            return false;
        }
        return info.getTypes().isSubtype(type.asType(), serverRpc.asType());
    }

    @Override
    protected void checkClientClass( TypeElement type, CompilationInfo info,
            Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {

        // TODO : check methods signature (no return value, no overloaded methods)  
    }
}
