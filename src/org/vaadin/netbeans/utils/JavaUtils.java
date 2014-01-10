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
package org.vaadin.netbeans.utils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.java.queries.UnitTestForSourceQuery;
import org.netbeans.api.java.source.ClassIndex.SearchKind;
import org.netbeans.api.java.source.ClassIndex.SearchScope;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.editor.analyzer.SetServletWidgetsetFix;
import org.vaadin.netbeans.editor.analyzer.SetWidgetsetFix;
import org.vaadin.netbeans.ui.wizard.NewWidgetWizardIterator;

/**
 * @author denis
 */
public final class JavaUtils {

    public static final String CLASS_SUFFIX = ".class"; // NOI18N

    public static final String JAVA = "java"; // NOI18N

    public static final String JAVA_SUFFIX = "." + JAVA; // NOI18N

    public static final String VAADIN_SERVLET_CONFIGURATION =
            "com.vaadin.annotations.VaadinServletConfiguration";//NOI18N

    public static final String SERVLET_ANNOTATION =
            "javax.servlet.annotation.WebServlet"; // NOI18N

    public static final String WIDGETSET = "widgetset"; // NOI18N

    public static final String INIT_PARAMS = "initParams"; // NOI18N

    public static final String VALUE = "value"; // NOI18N

    public static final String NAME = "name"; // NOI18N

    public static final String UI = "ui"; // NOI18N

    public static final String WEB_INIT_PARAM =
            "javax.servlet.annotation.WebInitParam"; // NOI18N

    public static final String VAADIN_UI_FQN = "com.vaadin.ui.UI"; // NOI18N

    public static final String VAADIN_SERVLET =
            "com.vaadin.server.VaadinServlet"; // NOI18N

    public static final Set<ElementKind> TYPE_KINDS = EnumSet.of(
            ElementKind.CLASS, ElementKind.INTERFACE, ElementKind.ENUM,
            ElementKind.ANNOTATION_TYPE);

    private JavaUtils() {
    }

    public static boolean checkServletConfiguration( final Project project,
            String widgetset ) throws IOException
    {
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        if (support == null) {
            return false;
        }
        ClasspathInfo info = support.getClassPathInfo();
        JavaSource javaSource = JavaSource.create(info);

        final boolean[] annotationBased = new boolean[1];
        final Map<Object, FileObject> files = new HashMap<>();
        final Map<Object, ElementHandle<TypeElement>> handles = new HashMap<>();
        final Set<Object> vaadinAnnotations = new HashSet<>();

        javaSource.runUserActionTask(new Task<CompilationController>() {

            @Override
            public void run( CompilationController controller )
                    throws Exception
            {
                controller.toPhase(Phase.ELEMENTS_RESOLVED);

                List<TypeElement> vaadinServletConfigs =
                        findAnnotatedElements(VAADIN_SERVLET_CONFIGURATION,
                                controller);
                annotationBased[0] = !vaadinServletConfigs.isEmpty();
                Set<TypeElement> foundServlets = new HashSet<>();
                for (TypeElement servlet : vaadinServletConfigs) {
                    AnnotationMirror annotation =
                            getAnnotation(servlet, VAADIN_SERVLET_CONFIGURATION);
                    Object id = null;

                    AnnotationValue widgetsetValue =
                            getAnnotationValue(annotation, WIDGETSET);
                    if (widgetsetValue != null) {
                        Object value = widgetsetValue.getValue();
                        if (value != null) {
                            id = value.toString();
                        }
                    }
                    if (id == null) {
                        id = new Object();
                    }
                    foundServlets.add(servlet);

                    vaadinAnnotations.add(id);
                    files.put(id, SourceUtils.getFile(
                            ElementHandle.create(servlet),
                            controller.getClasspathInfo()));
                    handles.put(id, ElementHandle.create(servlet));
                }
                List<TypeElement> servletConfigs =
                        findAnnotatedElements(SERVLET_ANNOTATION, controller);
                annotationBased[0] =
                        annotationBased[0] || !servletConfigs.isEmpty();
                for (TypeElement servlet : servletConfigs) {
                    AnnotationMirror annotation =
                            getAnnotation(servlet, SERVLET_ANNOTATION);
                    String widgetset = getWidgetsetWebInit(annotation);
                    Object id = null;
                    if (widgetset != null) {
                        id = widgetset;
                    }
                    else if (!foundServlets.contains(servlet)) {
                        id = new Object();
                    }
                    if (id != null) {
                        files.put(id, SourceUtils.getFile(
                                ElementHandle.create(servlet),
                                controller.getClasspathInfo()));
                        handles.put(id, ElementHandle.create(servlet));
                    }
                }
            }
        }, true);

        for (Entry<Object, FileObject> entry : files.entrySet()) {
            Object key = entry.getKey();
            boolean requireUpdate = false;
            if (key instanceof String) {
                String widgetXml = key.toString();
                if (getWidgetsetFiles(Collections.singletonList(widgetXml),
                        project).isEmpty())
                {
                    requireUpdate = true;
                }
            }
            else {
                requireUpdate = true;
            }
            if (!requireUpdate) {
                continue;
            }
            if (vaadinAnnotations.contains(key)) {
                SetWidgetsetFix fix =
                        new SetWidgetsetFix(widgetset, entry.getValue(),
                                handles.get(key));
                fix.implement();
            }
            else {
                SetServletWidgetsetFix fix =
                        new SetServletWidgetsetFix(widgetset, entry.getValue(),
                                handles.get(key));
                fix.implement();
            }
        }
        return annotationBased[0];
    }

    public static Set<FileObject> getWidgetsetFiles( List<String> widgetsets,
            Project project )
    {
        if (widgetsets.isEmpty()) {
            return Collections.emptySet();
        }

        SourceGroup[] javaSourceGroups = getJavaSourceGroups(project);
        SourceGroup[] resourcesSourceGroups = getResourcesSourceGroups(project);
        List<SourceGroup> sourceGroups =
                new ArrayList<>(javaSourceGroups.length
                        + resourcesSourceGroups.length);
        sourceGroups.addAll(Arrays.asList(javaSourceGroups));
        sourceGroups.addAll(Arrays.asList(resourcesSourceGroups));

        Set<FileObject> result = new LinkedHashSet<>(widgetsets.size());
        for (String widgetSet : widgetsets) {
            widgetSet = widgetSet.replace('.', '/');
            widgetSet += XmlUtils.GWT_XML;
            for (SourceGroup sourceGroup : sourceGroups) {
                FileObject rootFolder = sourceGroup.getRootFolder();
                FileObject fileObject = rootFolder.getFileObject(widgetSet);
                if (fileObject != null) {
                    result.add(fileObject);
                }
            }
        }
        return result;
    }

    public static String getFreeName( FileObject folder, String namePrefix,
            String suffix )
    {
        String name = namePrefix;
        String fileName = name + suffix;
        int i = 1;
        while (folder.getFileObject(fileName) != null) {
            name = namePrefix + i;
            fileName = name + suffix;
            i++;
        }
        return name;
    }

    public static String getFqn( FileObject javaClassFile ) throws IOException {
        JavaModelElement element = getModlelElement(javaClassFile);
        if (element == null) {
            return null;
        }
        else {
            return element.getFqn();
        }
    }

    public static JavaModelElement getModlelElement( FileObject javaClassFile )
            throws IOException
    {
        JavaSource javaSource = JavaSource.forFileObject(javaClassFile);
        if (javaSource == null) {
            Logger.getLogger(JavaUtils.class.getName()).log(Level.WARNING,
                    "Java source is null for fileObject: {0}",
                    javaClassFile.getPath());
            return null;
        }
        final JavaModelElement[] element = new JavaModelElement[1];
        javaSource.runUserActionTask(new Task<CompilationController>() {

            @Override
            public void run( CompilationController controller )
                    throws Exception
            {
                controller.toPhase(Phase.ELEMENTS_RESOLVED);
                List<? extends TypeElement> topLevelElements =
                        controller.getTopLevelElements();
                if (topLevelElements.size() != 1) {
                    Logger.getLogger(JavaUtils.class.getName()).log(
                            Level.WARNING,
                            "Found {0} top level elements in the file: {1}",
                            new Object[] { topLevelElements.size(),
                                    controller.getFileObject().getPath() });
                }
                if (topLevelElements.size() > 0) {
                    String fqn =
                            topLevelElements.get(0).getQualifiedName()
                                    .toString();
                    String name =
                            topLevelElements.get(0).getSimpleName().toString();
                    element[0] = new JavaModelElement(name, fqn);
                }
            }
        }, true);
        return element[0];
    }

    public static DataObject createDataObjectFromTemplate( String template,
            FileObject targetFolder, String targetName,
            Map<String, String> params ) throws IOException
    {
        assert template != null;
        assert targetFolder != null;

        String name = targetName;
        FileObject templateFileObject = FileUtil.getConfigFile(template);
        if (targetName == null) {
            name = templateFileObject.getName();
        }
        else {
            assert targetName.trim().length() > 0;
        }

        DataObject templateDataObject = DataObject.find(templateFileObject);
        DataFolder dataFolder = DataFolder.findFolder(targetFolder);

        if (params != null) {
            for (Entry<String, String> entry : params.entrySet()) {
                templateFileObject.setAttribute(entry.getKey(),
                        entry.getValue());
            }
        }

        return templateDataObject.createFromTemplate(dataFolder, name, params);
    }

    public static SourceGroup[] getJavaSourceGroups( Project project ) {
        return getSourceGroups(project, JavaProjectConstants.SOURCES_TYPE_JAVA);
    }

    public static SourceGroup[] getResourcesSourceGroups( Project project ) {
        return getSourceGroups(project,
                JavaProjectConstants.SOURCES_TYPE_RESOURCES);
    }

    public static SourceGroup[] getSourceGroups( Project project,
            String sourcesType )
    {
        SourceGroup[] sourceGroups =
                ProjectUtils.getSources(project).getSourceGroups(sourcesType);
        Set<FileObject> testRoots = getTestRoots(project, sourceGroups);
        List<SourceGroup> list = new ArrayList<>(sourceGroups.length);
        for (SourceGroup sourceGroup : sourceGroups) {
            if (!testRoots.contains(sourceGroup.getRootFolder())) {
                list.add(sourceGroup);
            }
        }
        return list.toArray(new SourceGroup[list.size()]);
    }

    public static Set<FileObject> getTestRoots( Project project ) {
        return getTestRoots(project, JavaProjectConstants.SOURCES_TYPE_JAVA);
    }

    public static Set<FileObject> getTestRoots( Project project,
            String sourcesType )
    {
        SourceGroup[] sourceGroups =
                ProjectUtils.getSources(project).getSourceGroups(sourcesType);
        return getTestRoots(project, sourceGroups);
    }

    public static List<TypeElement> findAnnotatedElements(
            final String searchedTypeName, CompilationInfo controller )
            throws InterruptedException
    {
        TypeElement searchedType =
                controller.getElements().getTypeElement(searchedTypeName);
        if (searchedType == null) {
            Logger.getLogger(JavaUtils.class.getName()).log(Level.FINE,
                    "Annotation type {0} is not found", searchedTypeName); // NOI18N
            return Collections.emptyList();
        }
        ElementHandle<TypeElement> searchedTypeHandle =
                ElementHandle.create(searchedType);
        final Set<ElementHandle<TypeElement>> elementHandles =
                controller
                        .getClasspathInfo()
                        .getClassIndex()
                        .getElements(
                                searchedTypeHandle,
                                EnumSet.of(SearchKind.TYPE_REFERENCES),
                                EnumSet.of(SearchScope.SOURCE,
                                        SearchScope.DEPENDENCIES));
        if (elementHandles == null) {
            throw new InterruptedException(
                    "ClassIndex.getElements() was interrupted"); // NOI18N
        }
        List<TypeElement> result = new ArrayList<>(elementHandles.size());
        for (ElementHandle<TypeElement> elementHandle : elementHandles) {
            Logger.getLogger(JavaUtils.class.getName()).log(Level.FINE,
                    "found element {0}", elementHandle.getQualifiedName()); // NOI18N
            TypeElement typeElement = elementHandle.resolve(controller);

            /*
             * Top level class is returned in the result in case if inner type
             * has annotation. Don't include it.
             */
            if (typeElement == null
                    || getAnnotation(typeElement, searchedTypeName) == null)
            {
                continue;
            }
            result.add(typeElement);
        }
        return result;
    }

    public static AnnotationMirror getAnnotation( TypeElement type,
            String annotaiton )
    {
        List<? extends AnnotationMirror> annotations =
                type.getAnnotationMirrors();
        for (AnnotationMirror annotationMirror : annotations) {
            Element element = annotationMirror.getAnnotationType().asElement();
            if (element instanceof TypeElement) {
                Name fqn = ((TypeElement) element).getQualifiedName();
                if (fqn.contentEquals(annotaiton)) {
                    return annotationMirror;
                }
            }
        }
        return null;
    }

    public static String getValue( AnnotationMirror annotation, String method )
    {
        AnnotationValue annotationValue =
                getAnnotationValue(annotation, method);
        if (annotationValue == null) {
            return null;
        }
        else {
            return annotationValue.getValue().toString();
        }
    }

    public static AnnotationValue getAnnotationValue(
            AnnotationMirror annotation, String method )
    {
        Map<? extends ExecutableElement, ? extends AnnotationValue> map =
                annotation.getElementValues();
        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : map
                .entrySet())
        {
            ExecutableElement executableElement = entry.getKey();
            if (executableElement.getSimpleName().contentEquals(method)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static List<?> getArrayValue( AnnotationMirror annotation,
            String method )
    {
        AnnotationValue annotationValue =
                getAnnotationValue(annotation, method);
        if (annotationValue == null) {
            return null;
        }
        Object value = annotationValue.getValue();
        if (value instanceof List<?>) {
            return (List<?>) value;
        }
        else {
            return null;
        }
    }

    public static String getWebInitParamValue( AnnotationMirror annotation,
            String paramName )
    {
        String value = null;
        List<?> params =
                JavaUtils.getArrayValue(annotation, JavaUtils.INIT_PARAMS);
        if (params == null) {
            return null;
        }
        for (Object param : params) {
            if (param instanceof AnnotationMirror) {
                AnnotationMirror mirror = (AnnotationMirror) param;
                String name = JavaUtils.getValue(mirror, JavaUtils.NAME);
                if (paramName.equals(name)) {
                    value = JavaUtils.getValue(mirror, JavaUtils.VALUE);
                    break;
                }
            }
        }
        return value;
    }

    public static String getWidgetsetWebInit( AnnotationMirror annotation ) {
        return getWebInitParamValue(annotation, JavaUtils.WIDGETSET);
    }

    public static AnnotationMirror getAnnotation( Element element,
            String annotationFqn )
    {
        List<? extends AnnotationMirror> annotations =
                element.getAnnotationMirrors();
        for (AnnotationMirror annotation : annotations) {
            Element annotationElement =
                    annotation.getAnnotationType().asElement();
            if (annotationElement instanceof TypeElement) {
                String fqn =
                        ((TypeElement) annotationElement).getQualifiedName()
                                .toString();
                if (fqn.equals(annotationFqn)) {
                    return annotation;
                }
            }
        }
        return null;
    }

    public static boolean hasAnnotation( Element element,
            String... annotationFqns )
    {
        List<? extends AnnotationMirror> annotations =
                element.getAnnotationMirrors();
        for (AnnotationMirror annotation : annotations) {
            Element annotationElement =
                    annotation.getAnnotationType().asElement();
            if (annotationElement instanceof TypeElement) {
                String fqn =
                        ((TypeElement) annotationElement).getQualifiedName()
                                .toString();
                for (String annotationFqn : annotationFqns) {
                    if (fqn.equals(annotationFqn)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static Set<TypeElement> getSubclasses( TypeElement typeElement,
            CompilationInfo info ) throws InterruptedException
    {
        if (typeElement == null) {
            return Collections.emptySet();
        }
        return discoverHierarchy(typeElement, EnumSet.of(ElementKind.CLASS),
                info);
    }

    public static Set<TypeElement> getSubinterfaces( TypeElement typeElement,
            CompilationInfo info ) throws InterruptedException
    {
        if (typeElement == null) {
            return Collections.emptySet();
        }
        return discoverHierarchy(typeElement,
                EnumSet.of(ElementKind.INTERFACE), info);
    }

    public static Collection<? extends TypeMirror> getSupertypes(
            TypeMirror type, CompilationInfo info )
    {
        return getSupertypes(type, null, info);
    }

    public static Collection<? extends TypeMirror> getSupertypes(
            TypeMirror type, ElementKind kind, CompilationInfo info )
    {
        Collection<TypeMirror> result = new LinkedList<>();
        List<TypeMirror> walkThrough = new LinkedList<>();
        walkThrough.add(type);
        Set<Element> visited = new HashSet<>();
        collectSupertypes(walkThrough, result, visited, kind, info);
        return result;
    }

    private static void collectSupertypes( List<TypeMirror> types,
            Collection<TypeMirror> superTypes, Set<Element> visited,
            ElementKind kind, CompilationInfo info )
    {
        while (!types.isEmpty()) {
            TypeMirror typeMirror = types.remove(0);
            Element typeElement = info.getTypes().asElement(typeMirror);

            List<? extends TypeMirror> directSupertypes =
                    info.getTypes().directSupertypes(typeMirror);
            for (TypeMirror directSupertype : directSupertypes) {
                typeElement = info.getTypes().asElement(directSupertype);
                if (kind != null && kind.equals(ElementKind.CLASS)) {
                    if (!typeElement.getKind().equals(kind)) {
                        visited.add(typeElement);
                        continue;
                    }
                }
                if (!visited.contains(typeElement)) {
                    types.add(directSupertype);
                    superTypes.add(directSupertype);
                    visited.add(typeElement);
                }
            }
        }
    }

    private static Set<FileObject> getTestRoots( Project project,
            SourceGroup[] sourceGroups )
    {
        Set<FileObject> result = new HashSet<>();
        for (SourceGroup sourceGroup : sourceGroups) {
            result.addAll(getTestRoots(sourceGroup));
        }
        return result;
    }

    private static Set<FileObject> getTestRoots( SourceGroup group ) {
        final URL[] rootURLs =
                UnitTestForSourceQuery.findUnitTests(group.getRootFolder());
        if (rootURLs.length == 0) {
            return Collections.emptySet();
        }
        List<FileObject> sourceRoots = getFileObjects(rootURLs);
        if (sourceRoots.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(sourceRoots);
    }

    private static List<FileObject> getFileObjects( URL[] urls ) {
        List<FileObject> result = new ArrayList<>(urls.length);
        for (URL url : urls) {
            FileObject sourceRoot = URLMapper.findFileObject(url);
            if (sourceRoot != null) {
                result.add(sourceRoot);
            }
            else {
                Logger.getLogger(NewWidgetWizardIterator.class.getName()).log(
                        Level.INFO,
                        "No FileObject found for the following URL: {0}", url);
            }
        }
        return result;
    }

    private static Set<TypeElement> discoverHierarchy( TypeElement typeElement,
            Set<ElementKind> kinds, CompilationInfo info )
            throws InterruptedException
    {
        Set<TypeElement> result = new HashSet<TypeElement>();
        result.add((TypeElement) typeElement);

        Set<TypeElement> toProcess = new HashSet<TypeElement>();
        toProcess.add((TypeElement) typeElement);
        Set<ElementKind> requiredKinds = EnumSet.copyOf(kinds);
        requiredKinds.retainAll(TYPE_KINDS);
        while (toProcess.size() > 0) {
            TypeElement element = toProcess.iterator().next();
            toProcess.remove(element);
            Set<TypeElement> set =
                    doDiscoverHierarchy(element, requiredKinds, info);
            if (set.size() == 0) {
                continue;
            }
            result.addAll(set);
            for (TypeElement impl : set) {
                toProcess.add(impl);
            }
        }
        result.remove(typeElement);
        return result;
    }

    private static Set<TypeElement> doDiscoverHierarchy(
            TypeElement typeElement, Set<ElementKind> kinds,
            CompilationInfo info ) throws InterruptedException
    {
        Set<TypeElement> result = new HashSet<>();
        ElementHandle<TypeElement> handle = ElementHandle.create(typeElement);
        final Set<ElementHandle<TypeElement>> handles =
                info.getClasspathInfo()
                        .getClassIndex()
                        .getElements(
                                handle,
                                EnumSet.of(SearchKind.IMPLEMENTORS),
                                EnumSet.of(SearchScope.SOURCE,
                                        SearchScope.DEPENDENCIES));
        if (handles == null) {
            throw new InterruptedException(
                    "ClassIndex.getElements() was interrupted"); // NOI18N
        }
        for (ElementHandle<TypeElement> elementHandle : handles) {
            Logger.getLogger(JavaUtils.class.getName()).log(Level.FINE,
                    elementHandle.getQualifiedName()); // NOI18N
            TypeElement derivedElement = elementHandle.resolve(info);
            if (derivedElement == null
                    || !kinds.contains(derivedElement.getKind()))
            {
                continue;
            }
            result.add(derivedElement);
        }
        return result;
    }

    public static class JavaModelElement {

        public JavaModelElement( String name, String fqn ) {
            myName = name;
            myFqn = fqn;
        }

        public String getFqn() {
            return myFqn;
        }

        public String getName() {
            return myName;
        }

        private final String myName;

        private final String myFqn;
    }

}
