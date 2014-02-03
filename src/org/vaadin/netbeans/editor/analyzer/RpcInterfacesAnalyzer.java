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
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
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
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.netbeans.spi.java.hints.HintContext;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.IsInSourceQuery;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;
import org.vaadin.netbeans.utils.XmlUtils;

/**
 * @author denis
 */
public class RpcInterfacesAnalyzer extends AbstractJavaBeanAnalyzer {

    static final String CLIENT_RPC =
            "com.vaadin.shared.communication.ClientRpc"; // NOI18N

    static final String SERVER_RPC =
            "com.vaadin.shared.communication.ServerRpc"; // NOI18N

    public RpcInterfacesAnalyzer( HintContext context, Mode mode ) {
        super(context, mode);
        assert !Mode.PACKAGE.equals(mode) : "Analyzer works for subclasses of "
                + "RPC interfaces that could be extended and implemented on server side"; //NOI18N
        myTypeVarParameterDeclarations = new LinkedList<>();
        myWildcardParameterDeclarations = new LinkedList<>();
        myDuplicateRpcMethods = new LinkedList<>();
        myNonVoidMethods = new LinkedList<>();
    }

    public RpcInterfacesAnalyzer( HintContext context ) {
        this(context, null);
    }

    @Override
    public void analyze() {
        TypeElement type = getType();
        if (type == null) {
            return;
        }
        CompilationInfo info = getInfo();

        if (!isRpcClass(type, info)) {
            return;
        }

        if (isPackageCheckMode()) {
            checkClientPackage();
        }
        else if (type.getKind().equals(ElementKind.INTERFACE)
                && isInClientPackage())
        {
            checkMethods(type);
        }
    }

    private boolean isInClientPackage() {
        VaadinSupport support = getSupport();
        if (support == null || !support.isEnabled() || !support.isReady()) {
            return false;
        }
        final boolean[] isInClient = new boolean[1];
        try {
            support.runModelOperation(new ModelOperation() {

                @Override
                public void run( VaadinModel model ) {
                    FileObject gwtXml = model.getGwtXml();
                    if (gwtXml == null) {
                        return;
                    }
                    try {
                        for (String path : model.getSourcePaths()) {
                            FileObject clientPkg =
                                    XmlUtils.getClientWidgetPackage(gwtXml,
                                            path, false);
                            if (clientPkg != null
                                    && FileUtil.isParentOf(clientPkg, getInfo()
                                            .getFileObject()))
                            {
                                isInClient[0] = true;
                            }
                        }
                    }
                    catch (IOException ignore) {
                    }
                }
            });
        }
        catch (IOException e) {
            Logger.getLogger(RpcInterfacesAnalyzer.class.getName()).log(
                    Level.INFO, null, e);
        }
        return isInClient[0];
    }

    public List<ErrorDescription> getTypeVarParameterDeclarations() {
        return myTypeVarParameterDeclarations;
    }

    public List<ErrorDescription> getWildcardParameterDeclarations() {
        return myWildcardParameterDeclarations;
    }

    public List<ErrorDescription> getDuplicateRpcMethods() {
        return myDuplicateRpcMethods;
    }

    public List<ErrorDescription> getNonVoidMethods() {
        return myNonVoidMethods;
    }

    private boolean isRpcClass( TypeElement type, CompilationInfo info ) {
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
            VariableElement field, TypeMirror type )
    {
        if (type instanceof TypeVariable) {
            return;
        }
        super.checkPublicField(checkTarget, field, type);
    }

    @NbBundle.Messages({
            "#{0} - type",
            "nonSerializableDeclarationType=Parameter''s type refers to non-serializable class {0}" })
    @Override
    protected void checkJavaBean( VariableElement checkTarget, DeclaredType type )
    {
        CompilationInfo info = getInfo();
        checkSerializable(checkTarget, type, info);
        if (isCanceled()) {
            return;
        }
        boolean isInSource =
                IsInSourceQuery.isInSource(type.asElement(), getInfo());
        if (!isInSource || isCanceled()) {
            return;
        }
        checkDefaultCtor(checkTarget, type, info);
        super.checkJavaBean(checkTarget, type);
    }

    @NbBundle.Messages({
            "#{0} - type",
            "nonDefaultCtor=Parameter''s type refers to class {0} which doesn''t have public no-arg contructor" })
    private void checkDefaultCtor( VariableElement checkTarget,
            DeclaredType type, CompilationInfo info )
    {
        Element element = type.asElement();
        if (element instanceof TypeElement) {
            if (!hasNoArgCtor((TypeElement) element)) {
                List<Integer> positions =
                        AbstractJavaFix.getElementPosition(info, checkTarget);
                ErrorDescription description =
                        ErrorDescriptionFactory.createErrorDescription(
                                getSeverity(Severity.WARNING),
                                Bundle.nonDefaultCtor(type),
                                Collections.<Fix> emptyList(),
                                info.getFileObject(), positions.get(0),
                                positions.get(1));
                getNonSerializables().add(description);
                getDescriptions().add(description);
            }
        }
    }

    private void checkSerializable( VariableElement checkTarget,
            DeclaredType type, CompilationInfo info )
    {
        TypeElement serializable =
                info.getElements().getTypeElement(Serializable.class.getName());
        if (serializable != null
                && !info.getTypes().isSubtype(type, serializable.asType()))
        {
            List<Integer> positions =
                    AbstractJavaFix.getElementPosition(info, checkTarget);
            ErrorDescription description =
                    ErrorDescriptionFactory.createErrorDescription(
                            getSeverity(Severity.WARNING),
                            Bundle.nonSerializableDeclarationType(type),
                            Collections.<Fix> emptyList(),
                            info.getFileObject(), positions.get(0),
                            positions.get(1));
            getNonSerializables().add(description);
            getDescriptions().add(description);
        }
    }

    private void checkMethodSingnature( ExecutableElement method ) {
        List<? extends VariableElement> parameters = method.getParameters();
        ExecutableType methodType = (ExecutableType) method.asType();
        List<? extends TypeMirror> parameterTypes =
                methodType.getParameterTypes();

        int i = 0;
        for (VariableElement param : parameters) {
            checkType(param, parameterTypes.get(i));
            i++;
        }

    }

    private void checkMethods( TypeElement type ) {
        List<ExecutableElement> methods =
                ElementFilter.methodsIn(type.getEnclosedElements());
        Map<String, ExecutableElement> methodNames = new HashMap<>();
        Set<String> duplicateNames = new HashSet<>();
        for (ExecutableElement method : methods) {
            if (isCanceled()) {
                return;
            }
            if (!method.getModifiers().contains(Modifier.PUBLIC)) {
                continue;
            }
            checkMethodSingnature(method);
            TypeMirror returnType = method.getReturnType();
            if (!returnType.getKind().equals(TypeKind.VOID)) {
                addBadReturnTypeWarning(method);
            }
            String name = method.getSimpleName().toString();
            if (duplicateNames.contains(name)) {
                addDuplicateMethodName(method);
            }
            else {
                ExecutableElement existingMethod = methodNames.get(name);
                if (existingMethod != null) {
                    addDuplicateMethodName(existingMethod);
                    addDuplicateMethodName(method);
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
            "typeVarParameterDeclaration=Parameter is declared using type variable",
            "wildcardParameterDeclaration=Parameter is declared using wildcard" })
    private void checkType( VariableElement checkTarget, TypeMirror type ) {
        CompilationInfo info = getInfo();
        if (type instanceof DeclaredType) {
            DeclaredType declaredType = (DeclaredType) type;
            checkJavaBean(checkTarget, declaredType);
            List<? extends TypeMirror> typeArguments =
                    declaredType.getTypeArguments();
            for (TypeMirror typeArg : typeArguments) {
                checkType(checkTarget, typeArg);
            }
        }
        else if (type.getKind().equals(TypeKind.TYPEVAR)) {
            List<Integer> positions =
                    AbstractJavaFix.getElementPosition(info, checkTarget);
            ErrorDescription description =
                    ErrorDescriptionFactory.createErrorDescription(
                            getSeverity(Severity.ERROR),
                            Bundle.typeVarParameterDeclaration(),
                            Collections.<Fix> emptyList(),
                            info.getFileObject(), positions.get(0),
                            positions.get(1));
            getTypeVarParameterDeclarations().add(description);
            getDescriptions().add(description);
        }
        else if (type.getKind().equals(TypeKind.ARRAY)) {
            ArrayType arrayType = (ArrayType) type;
            checkType(checkTarget, arrayType.getComponentType());
        }
        else if (type.getKind().equals(TypeKind.WILDCARD)) {
            List<Integer> positions =
                    AbstractJavaFix.getElementPosition(info, checkTarget);
            ErrorDescription description =
                    ErrorDescriptionFactory.createErrorDescription(
                            getSeverity(Severity.ERROR),
                            Bundle.wildcardParameterDeclaration(),
                            Collections.<Fix> emptyList(),
                            info.getFileObject(), positions.get(0),
                            positions.get(1));
            getDescriptions().add(description);
            getWildcardParameterDeclarations().add(description);
        }
    }

    @NbBundle.Messages({ "# {0} - methodName",
            "duplicateRpcMethodName=Several RPC method with the same name ''{0}''" })
    private void addDuplicateMethodName( ExecutableElement method ) {
        List<Integer> positions =
                AbstractJavaFix.getElementPosition(getInfo(), method);
        ErrorDescription description =
                ErrorDescriptionFactory.createErrorDescription(
                        getSeverity(Severity.ERROR), Bundle
                                .duplicateRpcMethodName(method.getSimpleName()
                                        .toString()), Collections
                                .<Fix> emptyList(), getInfo().getFileObject(),
                        positions.get(0), positions.get(1));
        getDuplicateRpcMethods().add(description);
        getDescriptions().add(description);
    }

    @NbBundle.Messages({ "methodHasReturnType=RPC method shouldn't declare return type" })
    private void addBadReturnTypeWarning( ExecutableElement method ) {
        CompilationInfo info = getInfo();
        List<Integer> positions =
                AbstractJavaFix.getElementPosition(info, method);
        ErrorDescription description =
                ErrorDescriptionFactory.createErrorDescription(
                        getSeverity(Severity.WARNING), Bundle
                                .methodHasReturnType(), Collections
                                .<Fix> singletonList(new SetVoidMethodFix(info
                                        .getFileObject(), ElementHandle
                                        .create(method))),
                        info.getFileObject(), positions.get(0), positions
                                .get(1));
        getNonVoidMethods().add(description);
        getDescriptions().add(description);
    }

    private List<ErrorDescription> myTypeVarParameterDeclarations;

    private List<ErrorDescription> myWildcardParameterDeclarations;

    private List<ErrorDescription> myDuplicateRpcMethods;

    private List<ErrorDescription> myNonVoidMethods;
}
