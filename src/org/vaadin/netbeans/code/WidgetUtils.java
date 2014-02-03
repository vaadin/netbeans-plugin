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
package org.vaadin.netbeans.code;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import org.netbeans.api.java.source.ClassIndex.SearchScope;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.project.Project;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.utils.JavaUtils;

/**
 * @author denis
 */
public final class WidgetUtils {

    private static final String GET_WIDGET = "getWidget"; // NOI18N

    public static final String GET_STATE = "getState"; // NOI18N

    public static final String CONNECTOR = "com.vaadin.shared.ui.Connect"; // NOI18N

    public static final String COMPOSITE_WIDGET =
            "com.google.gwt.user.client.ui.Composite"; // NOI18N

    public static final String WIDGET_CLASS =
            "com.google.gwt.user.client.ui.Widget"; // NOI18N

    public static final String ABSTRACT_COMPONENT_CONNECTOR =
            "com.vaadin.client.ui.AbstractComponentConnector"; // NOI18N

    public static final String ABSTRACT_COMPONENT =
            "com.vaadin.ui.AbstractComponent"; // NOI18N

    private WidgetUtils() {
    }

    public static TypeElement getConnector( TypeElement serverComponent,
            CompilationInfo info, boolean onlySource )
    {
        if (serverComponent == null) {
            return null;
        }
        try {
            List<TypeElement> connectors;
            if (onlySource) {
                connectors = JavaUtils.findAnnotatedElements(CONNECTOR, info);
            }
            else {
                connectors =
                        JavaUtils.findAnnotatedElements(CONNECTOR, info,
                                SearchScope.DEPENDENCIES);
            }

            for (TypeElement connector : connectors) {
                TypeElement serverPair = getServerComponent(connector, info);
                if (serverComponent.equals(serverPair)) {
                    return connector;
                }
            }
        }
        catch (InterruptedException ignore) {
        }
        return null;
    }

    public static TypeElement getServerComponent( TypeElement clientWidget,
            CompilationInfo info )
    {
        AnnotationMirror annotation =
                JavaUtils.getAnnotation(clientWidget, CONNECTOR);
        if (annotation == null) {
            return null;
        }
        AnnotationValue value =
                JavaUtils.getAnnotationValue(annotation, JavaUtils.VALUE);
        if (value == null) {
            return null;
        }
        Object clazz = value.getValue();
        if (clazz instanceof TypeMirror) {
            TypeMirror type = (TypeMirror) clazz;
            Element element = info.getTypes().asElement(type);
            if (element instanceof TypeElement) {
                return (TypeElement) element;
            }
        }
        return null;
    }

    public static TypeMirror getStateMethodReturnType( Element element ) {
        if (element == null) {
            return null;
        }
        List<ExecutableElement> methods =
                ElementFilter.methodsIn(element.getEnclosedElements());
        for (ExecutableElement method : methods) {
            if (GET_STATE.contentEquals(method.getSimpleName())
                    && method.getParameters().isEmpty())
            {
                return method.getReturnType();
            }
        }
        return null;
    }

    public static TypeElement getState( Element clazz,
            CompilationController controller )
    {
        TypeElement state = doGetState(clazz, controller);
        if (state != null) {
            return state;
        }
        Collection<? extends TypeMirror> supertypes =
                JavaUtils.getSupertypes(clazz.asType(), controller);
        for (TypeMirror typeMirror : supertypes) {
            Element superClass = controller.getTypes().asElement(typeMirror);
            state = doGetState(superClass, controller);
            if (state != null) {
                return state;
            }
        }
        return null;
    }

    public static String getWidgetFqn( Element connector, CompilationInfo info )
    {
        ElementHandle<TypeElement> handle = getWidgetHandle(connector, info);
        if (handle == null) {
            return null;
        }
        return handle.getQualifiedName();
    }

    public static ElementHandle<TypeElement> getWidgetHandle(
            Element connector, CompilationInfo info )
    {
        ExecutableElement getWidget = getWidgetGetter(connector);
        if (getWidget == null) {
            Collection<? extends TypeMirror> superclasses =
                    JavaUtils.getSupertypes(connector.asType(), info);
            for (TypeMirror superType : superclasses) {
                Element type = info.getTypes().asElement(superType);
                if (type != null) {
                    getWidget = getWidgetGetter(type);
                }
                if (getWidget != null) {
                    break;
                }
            }
        }
        if (getWidget == null) {
            return null;
        }

        Element returnElement =
                info.getTypes().asElement(getWidget.getReturnType());
        if (returnElement instanceof TypeElement) {
            TypeElement returnType = (TypeElement) returnElement;
            return ElementHandle.create(returnType);
        }
        return null;
    }

    public static ExecutableElement getWidgetGetter( Element clazz ) {
        List<ExecutableElement> methods =
                ElementFilter.methodsIn(clazz.getEnclosedElements());
        for (ExecutableElement method : methods) {
            if (method.getParameters().isEmpty()
                    && method.getSimpleName().contentEquals(GET_WIDGET))
            {
                return method;
            }
        }
        return null;
    }

    public static ConnectorInfo getConnectorInfo( Project project,
            final String componentFqn ) throws IOException
    {
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        ClasspathInfo classPathInfo = support.getClassPathInfo();
        JavaSource javaSource = JavaSource.create(classPathInfo);
        final ConnectorInfo[] result = new ConnectorInfo[1];
        if (javaSource != null) {
            javaSource.runUserActionTask(new Task<CompilationController>() {

                @Override
                public void run( CompilationController controller )
                        throws Exception
                {
                    controller.toPhase(Phase.ELEMENTS_RESOLVED);

                    TypeElement component =
                            controller.getElements().getTypeElement(
                                    componentFqn);
                    if (component == null) {
                        return;
                    }

                    result[0] = createConnectorInfo(controller, component);
                    if (result[0] != null) {
                        return;
                    }

                    Collection<? extends TypeMirror> superclasses =
                            JavaUtils.getSupertypes(component.asType(),
                                    ElementKind.CLASS, controller);
                    TypeElement abstractComponent =
                            controller.getElements().getTypeElement(
                                    WidgetUtils.ABSTRACT_COMPONENT);
                    for (TypeMirror superType : superclasses) {
                        Element element =
                                controller.getTypes().asElement(superType);
                        if (element.equals(abstractComponent)) {
                            break;
                        }
                        if (element.getKind().equals(ElementKind.CLASS)
                                && element instanceof TypeElement)
                        {
                            TypeElement superClass = (TypeElement) element;
                            result[0] =
                                    createConnectorInfo(controller, superClass);
                            if (result[0] != null) {
                                return;
                            }
                        }
                    }

                }

            }, true);
        }
        return result[0];
    }

    public static boolean isDefaultWidget( String connectorFqn, String widgetFqn )
    {
        return WidgetUtils.ABSTRACT_COMPONENT_CONNECTOR.equals(connectorFqn)
                || WidgetUtils.WIDGET_CLASS.equals(widgetFqn);
    }

    private static TypeElement doGetState( Element clazz,
            CompilationController controller )
    {
        if (clazz.getKind().equals(ElementKind.CLASS)) {
            TypeMirror returnType = getStateMethodReturnType(clazz);
            if (returnType != null) {
                Element returnElement =
                        controller.getTypes().asElement(returnType);
                if (returnElement instanceof TypeElement) {
                    return (TypeElement) returnElement;
                }
            }
        }
        return null;
    }

    private static ConnectorInfo createConnectorInfo(
            CompilationController controller, TypeElement component )
    {
        TypeElement connector =
                WidgetUtils.getConnector(component, controller, false);
        if (connector != null) {
            ElementHandle<TypeElement> connectorHandle =
                    ElementHandle.create(connector);
            ElementHandle<TypeElement> widgetHandle =
                    WidgetUtils.getWidgetHandle(connector, controller);
            TypeElement state = WidgetUtils.getState(connector, controller);
            ElementHandle<TypeElement> stateHandle =
                    state == null ? null : ElementHandle.create(state);
            return new ConnectorInfo(connectorHandle, widgetHandle, stateHandle);
        }
        return null;
    }

    public static final class ConnectorInfo {

        ConnectorInfo( ElementHandle<TypeElement> connector,
                ElementHandle<TypeElement> widget,
                ElementHandle<TypeElement> state )
        {
            myConnector = connector;
            myWidget = widget;
            myState = state;
        }

        public ElementHandle<TypeElement> getConnector() {
            return myConnector;
        }

        public ElementHandle<TypeElement> getWidget() {
            return myWidget;
        }

        public ElementHandle<TypeElement> getState() {
            return myState;
        }

        private final ElementHandle<TypeElement> myConnector;

        private final ElementHandle<TypeElement> myWidget;

        private final ElementHandle<TypeElement> myState;
    }
}
