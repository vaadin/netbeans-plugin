package org.vaadin.netbeans.editor.analyzer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.NbBundle;
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
        List<ExecutableElement> methods = ElementFilter.methodsIn(type
                .getEnclosedElements());
        Map<String, ExecutableElement> methodNames = new HashMap<>();
        Set<String> duplicateNames = new HashSet<>();
        for (ExecutableElement method : methods) {
            if (cancel.get()) {
                return;
            }
            if (!method.getModifiers().contains(Modifier.PUBLIC)) {
                continue;
            }
            TypeMirror returnType = method.getReturnType();
            if (!returnType.getKind().equals(TypeKind.VOID)) {
                addBadReturnTypeWarning(method, info, descriptions);
            }
            String name = method.getSimpleName().toString();
            if (duplicateNames.contains(name)) {
                addDuplicateMethodName(method, info, descriptions);
            }
            else {
                ExecutableElement existingMethod = methodNames.get(name);
                if (existingMethod != null) {
                    addDuplicateMethodName(existingMethod, info, descriptions);
                    addDuplicateMethodName(method, info, descriptions);
                    methodNames.remove(name);
                    duplicateNames.add(name);
                }
                else {
                    methodNames.put(name, method);
                }
            }
        }
    }

    @NbBundle.Messages({ "# {0} - methodName",
            "duplicateRpcMethodName=Several RPC method with the same name ''{0}''" })
    private void addDuplicateMethodName( ExecutableElement method,
            CompilationInfo info, Collection<ErrorDescription> descriptions )
    {
        List<Integer> positions = AbstractJavaFix.getElementPosition(info,
                method);
        ErrorDescription description = ErrorDescriptionFactory
                .createErrorDescription(Severity.WARNING, Bundle
                        .duplicateRpcMethodName(method.getSimpleName()
                                .toString()), Collections.<Fix> emptyList(),
                        info.getFileObject(), positions.get(0), positions
                                .get(1));
        descriptions.add(description);
    }

    @NbBundle.Messages({ "methodHasReturnType=RPC method shouldn't declare return type" })
    private void addBadReturnTypeWarning( ExecutableElement method,
            CompilationInfo info, Collection<ErrorDescription> descriptions )
    {
        List<Integer> positions = AbstractJavaFix.getElementPosition(info,
                method);
        ErrorDescription description = ErrorDescriptionFactory
                .createErrorDescription(Severity.WARNING,
                        Bundle.methodHasReturnType(),
                        Collections.<Fix> emptyList(), info.getFileObject(),
                        positions.get(0), positions.get(1));
        descriptions.add(description);
    }
}
