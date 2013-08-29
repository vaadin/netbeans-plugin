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
import org.openide.util.NbBundle;
import org.vaadin.netbeans.editor.VaadinTaskFactory;

/**
 * @author denis
 */
@NbBundle.Messages({ "noAccessors=JavaBeans specification is violated. Couldn''t find accessor methods" })
public class SharedStateAnalyzer extends ClientClassAnalyzer {

    private static final String GET = "get"; // NOI18N

    private static final String SET = "set"; // NOI18N

    private static final String IS = "is"; // NOI18N

    private static final String SHARED_STATE = "com.vaadin.shared.communication.SharedState"; // NOI18N

    private static final AccessorNameExtractor BOOLEAN_EXTRACTOR = new BooleanAccessorExtractor();

    private static final AccessorNameExtractor REGULAR_EXTRACTOR = new RegularAccessorExtractor();

    @Override
    protected boolean isClientClass( TypeElement type, CompilationInfo info ) {
        TypeElement sharedState = info.getElements().getTypeElement(
                SHARED_STATE);
        if (sharedState == null) {
            return false;
        }
        return info.getTypes().isSubtype(type.asType(), sharedState.asType());
    }

    @Override
    protected void checkClientClass( TypeElement type, CompilationInfo info,
            Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {
        List<VariableElement> fields = ElementFilter.fieldsIn(type
                .getEnclosedElements());
        List<VariableElement> localFields = new ArrayList<>(fields.size());
        for (VariableElement field : fields) {
            Set<Modifier> modifiers = field.getModifiers();
            if (cancel.get()) {
                return;
            }
            if (modifiers.contains(Modifier.PUBLIC)) {
                checkPublicField(field, field.asType(), info, descriptions);
            }
            else {
                localFields.add(field);
            }
        }
        if (cancel.get()) {
            return;
        }
        checkBeanStructure(localFields, info, descriptions, cancel);
    }

    private void checkBeanStructure( List<VariableElement> fields,
            CompilationInfo info, Collection<ErrorDescription> descriptions,
            AtomicBoolean cancel )
    {
        if (fields.isEmpty()) {
            return;
        }
        TypeElement clazzType = (TypeElement) fields.get(0)
                .getEnclosingElement();
        Set<ExecutableElement> allMethods = getPossibleAccessorMethods(clazzType);
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
            TypeMirror type = field.asType();
            boolean isBoolean = type.getKind() == TypeKind.BOOLEAN
                    || (booleanType != null && info.getTypes().isSubtype(type,
                            booleanType));
            boolean hasGetter;
            if (isBoolean) {
                hasGetter = checkBooleanGetter(field, allMethods, info, name);
            }
            else {
                hasGetter = checkRegularGetter(field, allMethods, info, name);
            }
            boolean hasSetter = checkSetter(field, allMethods, info, name);
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
                addNonMatchingAccessorsWarning(field, hasGetter, info,
                        descriptions);
            }
        }
        if (cancel.get()) {
            return;
        }
        checkBooleanFields(booleanFields, allMethods, info, descriptions);
        if (cancel.get()) {
            return;
        }

        checkFields(fields, allMethods, info, descriptions);
    }

    @NbBundle.Messages({
            "noSetter=JavaBeans specification is violated. Couldn''t find setter method",
            "noGetter=JavaBeans specification is violated. Couldn''t find getter method" })
    private void addNonMatchingAccessorsWarning( VariableElement field,
            boolean hasGetter, CompilationInfo info,
            Collection<ErrorDescription> descriptions )
    {
        List<Integer> positions = AbstractJavaFix.getElementPosition(info,
                field);
        String msg;
        if (hasGetter) {
            msg = Bundle.noSetter();
        }
        else {
            msg = Bundle.noGetter();
        }
        ErrorDescription description = ErrorDescriptionFactory
                .createErrorDescription(Severity.WARNING, msg,
                        Collections.<Fix> emptyList(), info.getFileObject(),
                        positions.get(0), positions.get(1));
        descriptions.add(description);
    }

    private void checkBooleanFields( List<VariableElement> booleanFields,
            Set<ExecutableElement> methods, CompilationInfo info,
            Collection<ErrorDescription> descriptions )
    {
        // TODO : improve check for accessors via getter source tree analysis
        if (booleanFields.size() == 1) {
            VariableElement field = booleanFields.get(0);
            if (!hasAccessors(field.asType(), BOOLEAN_EXTRACTOR, methods, info))
            {
                addNoAccessorsWarning(field, info, descriptions);
            }
        }
        else if (booleanFields.size() > 1) {
            for (VariableElement field : booleanFields) {
                addNoAccessorsWarning(field, info, descriptions);
            }
        }
    }

    private void checkFields( List<VariableElement> fields,
            Set<ExecutableElement> methods, CompilationInfo info,
            Collection<ErrorDescription> descriptions )
    {
        // TODO : improve check for accessors via getter source tree analysis
        refineFieldsDeclarations(fields, info, descriptions);
        for (VariableElement field : fields) {
            if (!hasAccessors(field.asType(), REGULAR_EXTRACTOR, methods, info))
            {
                addNoAccessorsWarning(field, info, descriptions);
            }
        }
    }

    /*
     * Walk through all fields and find those with same type. Add editor warning
     * for such fields and remove them from <code>fields</code>. Remaining
     * fields are declared using unique type.
     */
    private void refineFieldsDeclarations( List<VariableElement> fields,
            CompilationInfo info, Collection<ErrorDescription> descriptions )
    {
        Set<TypeMirrorWrapper> multipleTypeDeclarations = new HashSet<>();
        Map<TypeMirrorWrapper, VariableElement> singleTypeDeclarations = new HashMap<>();

        for (Iterator<VariableElement> iterator = fields.iterator(); iterator
                .hasNext();)
        {
            VariableElement field = iterator.next();
            TypeMirror type = field.asType();
            TypeMirrorWrapper wrapper = new TypeMirrorWrapper(type, info);
            if (multipleTypeDeclarations.contains(wrapper)) {
                addNoAccessorsWarning(field, info, descriptions);
                iterator.remove();
            }
            else {
                VariableElement existingField = singleTypeDeclarations
                        .get(wrapper);
                if (existingField != null) {
                    multipleTypeDeclarations.add(wrapper);
                    addNoAccessorsWarning(existingField, info, descriptions);
                    addNoAccessorsWarning(field, info, descriptions);
                    iterator.remove();
                    singleTypeDeclarations.remove(wrapper);
                }
                else {
                    singleTypeDeclarations.put(wrapper, field);
                }
            }
        }
    }

    private void addNoAccessorsWarning( VariableElement field,
            CompilationInfo info, Collection<ErrorDescription> descriptions )
    {
        List<Integer> positions = AbstractJavaFix.getElementPosition(info,
                field);
        ErrorDescription description = ErrorDescriptionFactory
                .createErrorDescription(Severity.WARNING, Bundle.noAccessors(),
                        Collections.<Fix> emptyList(), info.getFileObject(),
                        positions.get(0), positions.get(1));
        descriptions.add(description);
    }

    private Set<ExecutableElement> getPossibleAccessorMethods(
            TypeElement clazzType )
    {
        List<ExecutableElement> methods = ElementFilter.methodsIn(clazzType
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

    private boolean hasAccessors( TypeMirror type,
            AccessorNameExtractor extractor, Set<ExecutableElement> methods,
            CompilationInfo info )
    {
        Set<String> getters = new HashSet<>();
        Set<String> setters = new HashSet<>();
        for (Iterator<ExecutableElement> iterator = methods.iterator(); iterator
                .hasNext();)
        {
            ExecutableElement method = iterator.next();
            ExecutableType methodType = (ExecutableType) method.asType();
            if (method.getParameters().isEmpty()) {
                String name = extractor.extractGetter(method.getSimpleName()
                        .toString());
                if (name != null
                        && isSameType(methodType.getReturnType(), type, info))
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
                                type, info))
                {
                    setters.add(name);
                    iterator.remove();
                }
            }
        }
        getters.retainAll(setters);
        return !getters.isEmpty();
    }

    private boolean checkBooleanGetter( VariableElement field,
            Set<ExecutableElement> methods, CompilationInfo info, String name )
    {
        return checkGetter(field, methods, info, name, getBooleanGetter(name));
    }

    private boolean checkRegularGetter( VariableElement field,
            Set<ExecutableElement> methods, CompilationInfo info, String name )
    {
        return checkGetter(field, methods, info, name, getRegularGetter(name));
    }

    private boolean checkSetter( VariableElement field,
            Set<ExecutableElement> methods, CompilationInfo info, String name )
    {
        ExecutableElement foundSetter = null;
        String setter = getSetter(name);
        for (ExecutableElement method : methods) {
            if (setter.contentEquals(method.getSimpleName())
                    && method.getParameters().size() == 1)
            {
                ExecutableType methodType = (ExecutableType) method.asType();
                TypeMirror returnType = methodType.getReturnType();
                if (!returnType.getKind().equals(TypeKind.VOID)) {
                    continue;
                }
                TypeMirror paramType = methodType.getParameterTypes().get(0);
                if (isSameType(paramType, field.asType(), info)) {
                    foundSetter = method;
                    break;
                }
            }
        }
        if (foundSetter != null) {
            methods.remove(foundSetter);
            return true;
        }
        else {
            return false;
        }
    }

    private boolean checkGetter( VariableElement field,
            Set<ExecutableElement> methods, CompilationInfo info, String name,
            String getter )
    {
        ExecutableElement foundGetter = null;
        for (ExecutableElement method : methods) {
            if (getter.contentEquals(method.getSimpleName())
                    && method.getParameters().isEmpty())
            {
                ExecutableType methodType = (ExecutableType) method.asType();
                TypeMirror returnType = methodType.getReturnType();
                if (isSameType(returnType, field.asType(), info)) {
                    foundGetter = method;
                    break;
                }
            }
        }
        if (foundGetter != null) {
            methods.remove(foundGetter);
            return true;
        }
        else {
            return false;
        }
    }

    @NbBundle.Messages({ "# {0} - classFqn",
            "notSerializable=Not serializable class {0} is used in field declaration" })
    private void checkPublicField( VariableElement field, TypeMirror type,
            CompilationInfo info, Collection<ErrorDescription> descriptions )
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
                    checkPublicField(field, args.get(0), info, descriptions);
                }
            }
            else if (mapType != null
                    && info.getTypes().isSubtype(info.getTypes().erasure(type),
                            info.getTypes().erasure(mapType.asType())))
            {
                List<? extends TypeMirror> args = declaredType
                        .getTypeArguments();
                if (args.size() == 2) {
                    checkPublicField(field, args.get(0), info, descriptions);
                    checkPublicField(field, args.get(1), info, descriptions);
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
                        info, field);
                ErrorDescription description = ErrorDescriptionFactory
                        .createErrorDescription(Severity.ERROR,
                                Bundle.notSerializable(fqn),
                                Collections.<Fix> emptyList(),
                                info.getFileObject(), positions.get(0),
                                positions.get(1));
                descriptions.add(description);
            }
        }
        else if (type instanceof ArrayType) {
            checkPublicField(field, ((ArrayType) type).getComponentType(),
                    info, descriptions);
        }
        else if (type instanceof WildcardType) {
            TypeMirror extendsBound = ((WildcardType) type).getExtendsBound();
            checkPublicField(field, extendsBound, info, descriptions);
        }
        else if (type instanceof TypeVariable) {
            TypeMirror upperBound = ((TypeVariable) type).getUpperBound();
            checkPublicField(field, upperBound, info, descriptions);
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
