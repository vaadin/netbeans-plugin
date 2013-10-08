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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.vaadin.netbeans.editor.VaadinTaskFactory;

/**
 * @author denis
 */
abstract class AbstractJavaBeanAnalyzer extends ClientClassAnalyzer {

    private static final String GET = "get"; // NOI18N

    private static final String SET = "set"; // NOI18N

    private static final String IS = "is"; // NOI18N

    private static final AccessorNameExtractor BOOLEAN_EXTRACTOR = new BooleanAccessorExtractor();

    private static final AccessorNameExtractor REGULAR_EXTRACTOR = new RegularAccessorExtractor();

    protected void checkJavaBean( VariableElement checkTarget,
            DeclaredType type, CompilationInfo info,
            Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {
        List<VariableElement> fields = ElementFilter.fieldsIn(type.asElement()
                .getEnclosedElements());
        List<VariableElement> localFields = new ArrayList<>(fields.size());
        for (VariableElement field : fields) {
            Set<Modifier> modifiers = field.getModifiers();
            if (cancel.get()) {
                return;
            }
            if (modifiers.contains(Modifier.STATIC)) {
                continue;
            }
            if (modifiers.contains(Modifier.PUBLIC)) {
                checkPublicField(checkTarget == null ? field : checkTarget,
                        field, info.getTypes().asMemberOf(type, field), info,
                        descriptions);
            }
            else {
                localFields.add(field);
            }
        }
        if (cancel.get()) {
            return;
        }
        checkBeanStructure(checkTarget, type, localFields, info, descriptions,
                cancel);
    }

    protected abstract String getNotSerializableFieldMessage(
            VariableElement checkTarget, VariableElement field, String fqn );

    protected abstract String getNoGetterMessage( VariableElement checkTarget,
            VariableElement field );

    protected abstract String getNoSetterMessage( VariableElement checkTarget,
            VariableElement field );

    protected abstract String getNoAccessorsMessage( VariableElement target,
            VariableElement field );

    protected void checkBeanStructure( VariableElement checkTarget,
            DeclaredType type, List<VariableElement> fields,
            CompilationInfo info, Collection<ErrorDescription> descriptions,
            AtomicBoolean cancel )
    {
        if (fields.isEmpty()) {
            return;
        }
        Set<ExecutableElement> allMethods = getPossibleAccessorMethods(type
                .asElement());
        TypeElement booleanElement = info.getElements().getTypeElement(
                Boolean.class.getName());
        TypeMirror booleanType = null;
        if (booleanElement != null) {
            booleanType = booleanElement.asType();
        }
        List<VariableElement> booleanFields = new ArrayList<>(fields.size());
        for (Iterator<VariableElement> iterator = fields.iterator(); iterator
                .hasNext();)
        {
            if (cancel.get()) {
                return;
            }

            VariableElement field = iterator.next();
            String name = field.getSimpleName().toString();
            TypeMirror fieldType = info.getTypes().asMemberOf(type, field);
            boolean isBoolean = fieldType.getKind() == TypeKind.BOOLEAN
                    || (booleanType != null && info.getTypes().isSubtype(
                            fieldType, booleanType));
            boolean hasGetter;
            if (isBoolean) {
                hasGetter = checkBooleanGetter(fieldType, type, allMethods,
                        info, name);
            }
            else {
                hasGetter = checkRegularGetter(fieldType, type, allMethods,
                        info, name);
            }
            boolean hasSetter = checkSetter(fieldType, type, allMethods, info,
                    name);
            if (hasGetter && hasSetter) {
                iterator.remove();
            }
            else if (!hasGetter && !hasSetter) {
                if (isBoolean) {
                    booleanFields.add(field);
                    iterator.remove();
                }
            }
            else {
                iterator.remove();
                addNonMatchingAccessorsWarning(checkTarget == null ? field
                        : checkTarget, field, hasGetter, info, descriptions);
            }
        }
        if (cancel.get()) {
            return;
        }
        checkBooleanFields(checkTarget, type, booleanFields, allMethods, info,
                descriptions);
        if (cancel.get()) {
            return;
        }

        checkFields(checkTarget, type, fields, allMethods, info, descriptions);
    }

    private void addNonMatchingAccessorsWarning( VariableElement target,
            VariableElement field, boolean hasGetter, CompilationInfo info,
            Collection<ErrorDescription> descriptions )
    {
        List<Integer> positions = AbstractJavaFix.getElementPosition(info,
                target);
        String msg;
        if (hasGetter) {
            msg = getNoSetterMessage(target, field);
        }
        else {
            msg = getNoGetterMessage(target, field);
        }
        ErrorDescription description = ErrorDescriptionFactory
                .createErrorDescription(Severity.WARNING, msg,
                        Collections.<Fix> emptyList(), info.getFileObject(),
                        positions.get(0), positions.get(1));
        descriptions.add(description);
    }

    private void checkBooleanFields( VariableElement checkTarget,
            DeclaredType type, List<VariableElement> booleanFields,
            Set<ExecutableElement> methods, CompilationInfo info,
            Collection<ErrorDescription> descriptions )
    {
        // TODO : improve check for accessors via getter source tree analysis
        if (booleanFields.size() == 1) {
            VariableElement field = booleanFields.get(0);
            if (!hasAccessors(info.getTypes().asMemberOf(type, field), type,
                    BOOLEAN_EXTRACTOR, methods, info))
            {
                addNoAccessorsWarning(
                        checkTarget == null ? field : checkTarget, field, info,
                        descriptions);
            }
        }
        else if (booleanFields.size() > 1) {
            for (VariableElement field : booleanFields) {
                addNoAccessorsWarning(
                        checkTarget == null ? field : checkTarget, field, info,
                        descriptions);
            }
        }
    }

    private void checkFields( VariableElement checkTarget, DeclaredType type,
            List<VariableElement> fields, Set<ExecutableElement> methods,
            CompilationInfo info, Collection<ErrorDescription> descriptions )
    {
        // TODO : improve check for accessors via getter source tree analysis
        refineFieldsDeclarations(checkTarget, type, fields, info, descriptions);
        for (VariableElement field : fields) {
            if (!hasAccessors(info.getTypes().asMemberOf(type, field), type,
                    REGULAR_EXTRACTOR, methods, info))
            {
                addNoAccessorsWarning(
                        checkTarget == null ? field : checkTarget, field, info,
                        descriptions);
            }
        }
    }

    /*
     * Walk through all fields and find those with same type. Add editor warning
     * for such fields and remove them from <code>fields</code>. Remaining
     * fields are declared using unique type.
     */
    private void refineFieldsDeclarations( VariableElement checkTarget,
            DeclaredType clazz, List<VariableElement> fields,
            CompilationInfo info, Collection<ErrorDescription> descriptions )
    {
        Set<TypeMirrorWrapper> multipleTypeDeclarations = new HashSet<>();
        Map<TypeMirrorWrapper, VariableElement> singleTypeDeclarations = new HashMap<>();

        for (Iterator<VariableElement> iterator = fields.iterator(); iterator
                .hasNext();)
        {
            VariableElement field = iterator.next();
            TypeMirror type = info.getTypes().asMemberOf(clazz, field);
            TypeMirrorWrapper wrapper = new TypeMirrorWrapper(type, info);
            if (multipleTypeDeclarations.contains(wrapper)) {
                addNoAccessorsWarning(
                        checkTarget == null ? field : checkTarget, field, info,
                        descriptions);
                iterator.remove();
            }
            else {
                VariableElement existingField = singleTypeDeclarations
                        .get(wrapper);
                if (existingField != null) {
                    multipleTypeDeclarations.add(wrapper);
                    addNoAccessorsWarning(checkTarget == null ? existingField
                            : checkTarget, existingField, info, descriptions);
                    addNoAccessorsWarning(checkTarget == null ? field
                            : checkTarget, field, info, descriptions);
                    iterator.remove();
                    singleTypeDeclarations.remove(wrapper);
                }
                else {
                    singleTypeDeclarations.put(wrapper, field);
                }
            }
        }
    }

    private void addNoAccessorsWarning( VariableElement target,
            VariableElement field, CompilationInfo info,
            Collection<ErrorDescription> descriptions )
    {
        List<Integer> positions = AbstractJavaFix.getElementPosition(info,
                target);
        ErrorDescription description = ErrorDescriptionFactory
                .createErrorDescription(Severity.WARNING,
                        getNoAccessorsMessage(target, field),
                        Collections.<Fix> emptyList(), info.getFileObject(),
                        positions.get(0), positions.get(1));
        descriptions.add(description);
    }

    private Set<ExecutableElement> getPossibleAccessorMethods( Element clazz ) {
        List<ExecutableElement> methods = ElementFilter.methodsIn(clazz
                .getEnclosedElements());
        Set<ExecutableElement> allMethods = new HashSet<>();
        for (ExecutableElement method : methods) {
            if (method.getParameters().size() > 1
                    || !method.getModifiers().contains(Modifier.PUBLIC))
            {
                continue;
            }
            allMethods.add(method);
        }
        return allMethods;
    }

    protected void checkPublicField( VariableElement checkTarget,
            VariableElement field, TypeMirror type, CompilationInfo info,
            Collection<ErrorDescription> descriptions )
    {
        TypeElement collectionType = info.getElements().getTypeElement(
                Collection.class.getName());
        TypeElement mapType = info.getElements().getTypeElement(
                Map.class.getName());
        TypeElement serializable = info.getElements().getTypeElement(
                Serializable.class.getName());
        if (serializable == null) {
            return;
        }
        if (type instanceof DeclaredType) {
            DeclaredType declaredType = (DeclaredType) type;
            if (collectionType != null
                    && info.getTypes().isSubtype(info.getTypes().erasure(type),
                            info.getTypes().erasure(collectionType.asType())))
            {
                List<? extends TypeMirror> args = declaredType
                        .getTypeArguments();
                if (args.size() == 1) {
                    checkPublicField(checkTarget, field, args.get(0), info,
                            descriptions);
                }
            }
            else if (mapType != null
                    && info.getTypes().isSubtype(info.getTypes().erasure(type),
                            info.getTypes().erasure(mapType.asType())))
            {
                List<? extends TypeMirror> args = declaredType
                        .getTypeArguments();
                if (args.size() == 2) {
                    checkPublicField(checkTarget, field, args.get(0), info,
                            descriptions);
                    checkPublicField(checkTarget, field, args.get(1), info,
                            descriptions);
                }
            }
            else if (!info.getTypes().isSubtype(type, serializable.asType())) {
                Element typeElement = info.getTypes().asElement(type);
                String fqn;
                if (typeElement instanceof TypeElement) {
                    fqn = ((TypeElement) typeElement).getQualifiedName()
                            .toString();
                }
                else {
                    fqn = type.toString();
                }
                List<Integer> positions = AbstractJavaFix.getElementPosition(
                        info, checkTarget);
                ErrorDescription description = ErrorDescriptionFactory
                        .createErrorDescription(
                                Severity.ERROR,
                                getNotSerializableFieldMessage(checkTarget,
                                        field, fqn), Collections
                                        .<Fix> emptyList(), info
                                        .getFileObject(), positions.get(0),
                                positions.get(1));
                descriptions.add(description);
            }
        }
        else if (type instanceof ArrayType) {
            checkPublicField(checkTarget, field,
                    ((ArrayType) type).getComponentType(), info, descriptions);
        }
        else if (type instanceof WildcardType) {
            TypeMirror extendsBound = ((WildcardType) type).getExtendsBound();
            checkPublicField(checkTarget, field, extendsBound, info,
                    descriptions);
        }
        else if (type instanceof TypeVariable) {
            TypeMirror upperBound = ((TypeVariable) type).getUpperBound();
            checkPublicField(checkTarget, field, upperBound, info, descriptions);
        }
    }

    private String getBooleanGetter( String name ) {
        return getAccessorName(name, IS);
    }

    private String getAccessorName( String name, String prefix ) {
        if (name.length() == 0) {
            return null;
        }
        else if (name.length() == 1) {
            return prefix + Character.toUpperCase(name.charAt(0));
        }
        else {
            StringBuilder builder = new StringBuilder(prefix);
            builder.append(Character.toUpperCase(name.charAt(0)));
            builder.append(name.substring(1, name.length()));
            return builder.toString();
        }
    }

    private String getSetter( String name ) {
        return getAccessorName(name, SET);
    }

    private String getRegularGetter( String name ) {
        return getAccessorName(name, GET);
    }

    private static boolean isSameType( TypeMirror type1, TypeMirror type2,
            CompilationInfo info )
    {
        return info.getTypes().isSameType(type1, type2)
                || info.getTypes().isSameType(boxedPrimitive(type1, info),
                        boxedPrimitive(type2, info));
    }

    private boolean hasAccessors( TypeMirror requiredType, DeclaredType clazz,
            AccessorNameExtractor extractor, Set<ExecutableElement> methods,
            CompilationInfo info )
    {
        Set<String> getters = new HashSet<>();
        Set<String> setters = new HashSet<>();
        for (Iterator<ExecutableElement> iterator = methods.iterator(); iterator
                .hasNext();)
        {
            ExecutableElement method = iterator.next();
            ExecutableType methodType = (ExecutableType) info.getTypes()
                    .asMemberOf(clazz, method);
            if (method.getParameters().isEmpty()) {
                String name = extractor.extractGetter(method.getSimpleName()
                        .toString());
                if (name != null
                        && isSameType(methodType.getReturnType(), requiredType,
                                info))
                {
                    getters.add(name);
                    iterator.remove();
                }
            }
            else if (method.getParameters().size() == 1
                    && methodType.getReturnType().getKind()
                            .equals(TypeKind.VOID))
            {
                String name = extractor.extractSetter(method.getSimpleName()
                        .toString());
                if (name != null
                        && isSameType(methodType.getParameterTypes().get(0),
                                requiredType, info))
                {
                    setters.add(name);
                    iterator.remove();
                }
            }
        }
        getters.retainAll(setters);
        return !getters.isEmpty();
    }

    private boolean checkBooleanGetter( TypeMirror fieldType,
            DeclaredType clazz, Set<ExecutableElement> methods,
            CompilationInfo info, String name )
    {
        return checkGetter(fieldType, clazz, methods, info, name,
                getBooleanGetter(name));
    }

    private boolean checkRegularGetter( TypeMirror fieldType,
            DeclaredType clazz, Set<ExecutableElement> methods,
            CompilationInfo info, String name )
    {
        return checkGetter(fieldType, clazz, methods, info, name,
                getRegularGetter(name));
    }

    private boolean checkSetter( TypeMirror fieldType, DeclaredType clazz,
            Set<ExecutableElement> methods, CompilationInfo info, String name )
    {
        String setter = getSetter(name);
        for (Iterator<ExecutableElement> iterator = methods.iterator(); iterator
                .hasNext();)
        {
            ExecutableElement method = iterator.next();
            if (setter.contentEquals(method.getSimpleName())
                    && method.getParameters().size() == 1)
            {
                ExecutableType methodType = (ExecutableType) method.asType();
                TypeMirror returnType = methodType.getReturnType();
                if (!returnType.getKind().equals(TypeKind.VOID)) {
                    continue;
                }
                TypeMirror paramType = methodType.getParameterTypes().get(0);
                if (isSameType(paramType, fieldType, info)) {
                    iterator.remove();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkGetter( TypeMirror fieldType, DeclaredType clazz,
            Set<ExecutableElement> methods, CompilationInfo info, String name,
            String getter )
    {
        for (Iterator<ExecutableElement> iterator = methods.iterator(); iterator
                .hasNext();)
        {
            ExecutableElement method = iterator.next();
            if (getter.contentEquals(method.getSimpleName())
                    && method.getParameters().isEmpty())
            {
                ExecutableType methodType = (ExecutableType) info.getTypes()
                        .asMemberOf(clazz, method);
                TypeMirror returnType = methodType.getReturnType();
                if (isSameType(returnType, fieldType, info)) {
                    iterator.remove();
                    return true;
                }
            }
        }
        return false;
    }

    private static TypeMirror boxedPrimitive( TypeMirror type,
            CompilationInfo info )
    {
        if (type.getKind().isPrimitive()) {
            return info
                    .getTypes()
                    .boxedClass(
                            info.getTypes().getPrimitiveType(type.getKind()))
                    .asType();
        }
        else {
            return type;
        }
    }

    static abstract class AccessorNameExtractor {

        abstract String extractGetter( String getter );

        String extractSetter( String setter ) {
            if (setter.startsWith(SET) && setter.length() > SET.length()) {
                String extracted = setter.substring(SET.length());
                return Character.toLowerCase(extracted.charAt(0))
                        + extracted.substring(1);
            }
            return null;
        }

        protected String extractGetter( String getter, String prefix ) {
            if (getter.startsWith(prefix) && getter.length() > prefix.length())
            {
                String extracted = getter.substring(prefix.length());
                return Character.toLowerCase(extracted.charAt(0))
                        + extracted.substring(1);
            }
            return null;
        }
    }

    static class BooleanAccessorExtractor extends AccessorNameExtractor {

        @Override
        String extractGetter( String getter ) {
            return extractGetter(getter, IS);
        }

    }

    static class RegularAccessorExtractor extends AccessorNameExtractor {

        @Override
        String extractGetter( String getter ) {
            return extractGetter(getter, GET);
        }
    }

    static class TypeMirrorWrapper {

        TypeMirrorWrapper( TypeMirror type, CompilationInfo info ) {
            myType = type;
            myInfo = info;
        }

        TypeMirror getType() {
            return myType;
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj instanceof TypeMirrorWrapper) {
                TypeMirror type = ((TypeMirrorWrapper) obj).getType();
                return isSameType(myType, type, myInfo);
            }
            return false;
        }

        @Override
        public int hashCode() {
            Element element = myInfo.getTypes().asElement(myType);
            if (element != null) {
                return element.hashCode();
            }
            else {
                return myType.toString().hashCode();
            }
        }

        private final TypeMirror myType;

        private final CompilationInfo myInfo;
    }
}
