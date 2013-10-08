package org.vaadin.netbeans.editor.analyzer;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;

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
import org.vaadin.netbeans.code.generator.JavaUtils;
import org.vaadin.netbeans.editor.VaadinTaskFactory;

/**
 * @author denis
 */
public class RpcInterfacesAnalyzer extends AbstractJavaBeanAnalyzer {

    static final String CLIENT_RPC = "com.vaadin.shared.communication.ClientRpc"; // NOI18N

    static final String SERVER_RPC = "com.vaadin.shared.communication.ServerRpc"; // NOI18N

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
            checkMethodSingnature(method, info, descriptions, factory, cancel);
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

    @NbBundle.Messages({
            "# {0} - classFqn",
            "# {1} - containingClass",
            "# {2} - fieldName",
            "notSerializableField=Parameter type refers to class {1} with field ''{2}'' declared by non-serializable class {0}" })
    @Override
    protected String getNotSerializableFieldMessage(
            VariableElement checkTarget, VariableElement field, String fqn )
    {
        Element enclosing = field.getEnclosingElement();
        String clazz = "";
        if (enclosing instanceof TypeElement) {
            clazz = ((TypeElement) enclosing).getQualifiedName().toString();
        }
        return Bundle.notSerializableField(fqn, clazz, field.getSimpleName()
                .toString());
    }

    @NbBundle.Messages({
            "# {0} - containingClass",
            "# {1} - fieldName",
            "fieldHasNoGetter=Parameter type refers to class {0} with field ''{1}'' which hasn''t getter" })
    @Override
    protected String getNoGetterMessage( VariableElement checkTarget,
            VariableElement field )
    {
        Element enclosing = field.getEnclosingElement();
        String clazz = "";
        if (enclosing instanceof TypeElement) {
            clazz = ((TypeElement) enclosing).getQualifiedName().toString();
        }
        return Bundle.fieldHasNoGetter(clazz, field.getSimpleName().toString());
    }

    @NbBundle.Messages({
            "# {0} - containingClass",
            "# {1} - fieldName",
            "fieldHasNoSetter=Parameter type refers to class {0} with field ''{1}'' which hasn''t setter" })
    @Override
    protected String getNoSetterMessage( VariableElement checkTarget,
            VariableElement field )
    {
        Element enclosing = field.getEnclosingElement();
        String clazz = "";
        if (enclosing instanceof TypeElement) {
            clazz = ((TypeElement) enclosing).getQualifiedName().toString();
        }
        return Bundle.fieldHasNoSetter(clazz, field.getSimpleName().toString());
    }

    @NbBundle.Messages({
            "# {0} - containingClass",
            "# {1} - fieldName",
            "fieldHasNoAccessors=Parameter type refers to class {0} with field ''{1}'' which hasn''t accessors" })
    @Override
    protected String getNoAccessorsMessage( VariableElement target,
            VariableElement field )
    {
        Element enclosing = field.getEnclosingElement();
        String clazz = "";
        if (enclosing instanceof TypeElement) {
            clazz = ((TypeElement) enclosing).getQualifiedName().toString();
        }
        return Bundle.fieldHasNoAccessors(clazz, field.getSimpleName()
                .toString());
    }

    @Override
    protected void checkPublicField( VariableElement checkTarget,
            VariableElement field, TypeMirror type, CompilationInfo info,
            Collection<ErrorDescription> descriptions )
    {
        if (type instanceof TypeVariable) {
            return;
        }
        super.checkPublicField(checkTarget, field, type, info, descriptions);
    }

    @NbBundle.Messages({
            "#{0} - type",
            "nonSerializableDeclarationType=Parameter''s type refers to non-serializable class {0}" })
    @Override
    protected void checkJavaBean( VariableElement checkTarget,
            DeclaredType type, CompilationInfo info,
            Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {
        TypeElement serializable = info.getElements().getTypeElement(
                Serializable.class.getName());
        if (serializable != null
                && !info.getTypes().isSubtype(type, serializable.asType()))
        {
            List<Integer> positions = AbstractJavaFix.getElementPosition(info,
                    checkTarget);
            ErrorDescription description = ErrorDescriptionFactory
                    .createErrorDescription(Severity.WARNING,
                            Bundle.nonSerializableDeclarationType(type),
                            Collections.<Fix> emptyList(),
                            info.getFileObject(), positions.get(0),
                            positions.get(1));
            descriptions.add(description);
        }
        if (cancel.get()) {
            return;
        }
        FileObject fileObject = SourceUtils
                .getFile(ElementHandle.create(type.asElement()),
                        info.getClasspathInfo());
        if (fileObject == null) {
            return;
        }
        Project project = FileOwnerQuery.getOwner(info.getFileObject());
        SourceGroup[] groups = JavaUtils.getJavaSourceGroups(project);
        boolean isInSource = false;
        for (SourceGroup sourceGroup : groups) {
            FileObject rootFolder = sourceGroup.getRootFolder();
            if (FileUtil.isParentOf(rootFolder, fileObject)) {
                isInSource = true;
                break;
            }
        }
        if (!isInSource || cancel.get()) {
            return;
        }
        // TODO : check parameter's class location (call refactored checkClientPackage() methods) 
        super.checkJavaBean(checkTarget, type, info, descriptions, factory,
                cancel);
    }

    private void checkMethodSingnature( ExecutableElement method,
            CompilationInfo info, Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {
        List<? extends VariableElement> parameters = method.getParameters();
        ExecutableType methodType = (ExecutableType) method.asType();
        List<? extends TypeMirror> parameterTypes = methodType
                .getParameterTypes();

        int i = 0;
        for (VariableElement param : parameters) {
            checkType(param, parameterTypes.get(i), info, descriptions,
                    factory, cancel);
            i++;
        }

    }

    @NbBundle.Messages({
            "typeVarParameterDeclaration=Parameter is declared using type variable",
            "wildcardParameterDeclaration=Parameter is declared using wildcard" })
    private void checkType( VariableElement checkTarget, TypeMirror type,
            CompilationInfo info, Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {
        if (type instanceof DeclaredType) {
            DeclaredType declaredType = (DeclaredType) type;
            checkJavaBean(checkTarget, declaredType, info, descriptions,
                    factory, cancel);
            List<? extends TypeMirror> typeArguments = declaredType
                    .getTypeArguments();
            for (TypeMirror typeArg : typeArguments) {
                checkType(checkTarget, typeArg, info, descriptions, factory,
                        cancel);
            }
        }
        else if (type.getKind().equals(TypeKind.TYPEVAR)) {
            List<Integer> positions = AbstractJavaFix.getElementPosition(info,
                    checkTarget);
            ErrorDescription description = ErrorDescriptionFactory
                    .createErrorDescription(Severity.ERROR,
                            Bundle.typeVarParameterDeclaration(),
                            Collections.<Fix> emptyList(),
                            info.getFileObject(), positions.get(0),
                            positions.get(1));
            descriptions.add(description);
        }
        else if (type.getKind().equals(TypeKind.ARRAY)) {
            ArrayType arrayType = (ArrayType) type;
            checkType(checkTarget, arrayType.getComponentType(), info,
                    descriptions, factory, cancel);
        }
        else if (type.getKind().equals(TypeKind.WILDCARD)) {
            List<Integer> positions = AbstractJavaFix.getElementPosition(info,
                    checkTarget);
            ErrorDescription description = ErrorDescriptionFactory
                    .createErrorDescription(Severity.ERROR,
                            Bundle.wildcardParameterDeclaration(),
                            Collections.<Fix> emptyList(),
                            info.getFileObject(), positions.get(0),
                            positions.get(1));
            descriptions.add(description);
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
                .createErrorDescription(Severity.ERROR, Bundle
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
                .createErrorDescription(Severity.WARNING, Bundle
                        .methodHasReturnType(),
                        Collections.<Fix> singletonList(new SetVoidMethodFix(
                                info.getFileObject(), ElementHandle
                                        .create(method))),
                        info.getFileObject(), positions.get(0), positions
                                .get(1));
        descriptions.add(description);
    }

}
