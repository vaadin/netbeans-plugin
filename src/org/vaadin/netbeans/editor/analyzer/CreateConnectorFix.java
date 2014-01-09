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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.code.generator.WidgetGenerator;
import org.vaadin.netbeans.editor.analyzer.ui.ConnectorPanel;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;
import org.vaadin.netbeans.utils.JavaUtils;
import org.vaadin.netbeans.utils.XmlUtils;

/**
 * @author denis
 */
public class CreateConnectorFix extends AbstractJavaFix {

    private static final String WIDGET_SUPER = "widget_super";//NOI18N

    private static final String WIDGET_SUPER_FQN = "widget_super_fqn";//NOI18N

    private static final String WIDGET_PARAM = "widget";//NOI18N

    private static final String STYLE_NAME = "style_name";//NOI18N

    private static final String SERVER_COMPONENT = "server_component";//NOI18N

    private static final String SERVER_COMPONENT_FQN = "server_component_fqn";//NOI18N

    private static final String SUPER_CONNECTOR = "super_connector"; //NOI18N

    private static final String SUPER_CONNECTOR_FQN = "super_connector_fqn"; //NOI18N

    private static final String SHARED_STATE = "shared_state";//NOI18N

    private static final String WIDGET = "Widget"; // NOI18N

    private static final String GET_WIDGET = "getWidget"; // NOI18N

    private static final String COMPOSITE_WIDGET =
            "com.google.gwt.user.client.ui.Composite"; // NOI18N

    private static final String WIDGET_CLASS =
            "com.google.gwt.user.client.ui.Widget"; // NOI18N

    private static final String CONNECTOR_TEMPLATE =
            "Templates/Vaadin/ConnectorConnector.java"; // NOI18N

    private static final String WIDGET_TEMPLATE =
            "Templates/Vaadin/FullFledgedWidget.java";// NOI18N

    public CreateConnectorFix( ElementHandle<TypeElement> handle,
            ElementHandle<TypeElement> connectorHandle, FileObject fileObject,
            boolean createWidget )
    {
        super(fileObject);
        myHandle = handle;
        myConnectorHandle = connectorHandle;
        myCreateWidget = createWidget;
    }

    @NbBundle.Messages({ "generateConnector=Generate Connector",
            "generateConnectorAndWidget=Generate Connector and Widget" })
    @Override
    public String getText() {
        if (myCreateWidget) {
            return Bundle.generateConnectorAndWidget();
        }
        else {
            return Bundle.generateConnector();
        }
    }

    @Override
    public ChangeInfo implement() throws Exception {
        askUserInput();

        if (myConnectorName == null) {
            return null;
        }

        if (myClientPackage == null) {
            if (!hasGwtXml) {
                XmlUtils.createGwtXml(getFileObject().getParent());
            }
            searchClientPackage(true);
        }

        Map<String, String> params = new HashMap<>();
        params.put(SERVER_COMPONENT_FQN, myHandle.getQualifiedName());
        params.put(SERVER_COMPONENT, getFileObject().getName());

        params.put(SUPER_CONNECTOR_FQN, myConnectorSuperClass);
        int index = myConnectorSuperClass.lastIndexOf('.');
        params.put(SUPER_CONNECTOR, myConnectorSuperClass.substring(index + 1));
        params.put(WIDGET_PARAM, myCreateWidget ? myWidgetName : null);

        DataObject connectorDataObject =
                JavaUtils.createDataObjectFromTemplate(CONNECTOR_TEMPLATE,
                        myClientPackage, myConnectorName, params);
        EditorCookie cookie =
                connectorDataObject.getLookup().lookup(EditorCookie.class);
        if (cookie != null) {
            cookie.open();
        }

        if (myCreateWidget) {
            DataObject widgetDataObject =
                    createWidget(getFileObject().getName().toLowerCase());
            cookie = widgetDataObject.getLookup().lookup(EditorCookie.class);
            if (cookie != null) {
                cookie.open();
            }
        }

        return null;
    }

    private DataObject createWidget( String styleName ) throws IOException {
        Map<String, String> map = new HashMap<>();
        map.put(STYLE_NAME, styleName);
        map.put(WIDGET_SUPER_FQN, myWidgetSuperClass);
        int index = myWidgetSuperClass.lastIndexOf('.');
        map.put(WIDGET_SUPER, myWidgetSuperClass.substring(index + 1));
        map.put(SHARED_STATE, null);
        return JavaUtils.createDataObjectFromTemplate(WIDGET_TEMPLATE,
                myClientPackage, myWidgetName, map);
    }

    @NbBundle.Messages("connectorPanelTitle=Set Up New Connector Class")
    private void askUserInput() {
        try {
            searchClientPackage(false);
        }
        catch (IOException e) {
            getLogger().log(Level.INFO, null, e);
        }
        try {
            initSuperClasses();
        }
        catch (IOException e) {
            getLogger().log(Level.INFO, null, e);
        }

        final String connectorName = suggestConnectorName();
        final String widgetName = suggestWidgetName();

        Mutex.EVENT.readAccess(new Mutex.Action<Void>() {

            @Override
            public Void run() {
                ConnectorPanel panel =
                        new ConnectorPanel(connectorName, myConnectorSupers,
                                myCreateWidget ? widgetName : null);
                DialogDescriptor descriptor =
                        new DialogDescriptor(panel, Bundle
                                .connectorPanelTitle());
                Object result = DialogDisplayer.getDefault().notify(descriptor);
                if (NotifyDescriptor.OK_OPTION.equals(result)) {
                    myConnectorName = panel.getConnectorName();
                    myConnectorSuperClass = panel.getConnectorSuperClass();
                    myWidgetName = panel.getWidgetName();
                    myWidgetSuperClass = panel.getWidgetSuperclass();
                }
                else {
                    myConnectorName = null;
                }
                return null;
            }
        });
    }

    private String getWidgetSuper( String widget ) {
        return widget == null || widget.equals(WIDGET_CLASS) ? COMPOSITE_WIDGET
                : widget;
    }

    private void initSuperClasses() throws IOException {
        JavaSource javaSource = JavaSource.forFileObject(getFileObject());
        myConnectorSupers = new LinkedHashMap<>();
        if (myConnectorHandle != null && javaSource != null) {
            javaSource.runUserActionTask(new Task<CompilationController>() {

                @Override
                public void run( CompilationController controller )
                        throws Exception
                {
                    controller.toPhase(Phase.ELEMENTS_RESOLVED);

                    TypeElement connector =
                            myConnectorHandle.resolve(controller);
                    if (connector == null) {
                        return;
                    }
                    Collection<? extends TypeMirror> superclasses =
                            JavaUtils.getSupertypes(connector.asType(),
                                    controller);
                    TypeElement abstractConnector =
                            controller
                                    .getElements()
                                    .getTypeElement(
                                            ConnectorAnalyzer.ABSTRACT_COMPONENT_CONNECTOR);

                    myConnectorSupers
                            .put(connector.getQualifiedName().toString(),
                                    getWidgetSuper(getWidgetFqn(connector,
                                            controller)));

                    for (TypeMirror superType : superclasses) {
                        Element element =
                                controller.getTypes().asElement(superType);
                        if (element instanceof TypeElement) {
                            String connectorFqn =
                                    ((TypeElement) element).getQualifiedName()
                                            .toString();
                            String widgetFqn =
                                    getWidgetFqn(element, controller);
                            myConnectorSupers.put(connectorFqn,
                                    getWidgetSuper(widgetFqn));
                        }
                        if (element.equals(abstractConnector)) {
                            break;
                        }
                    }
                }
            }, true);
        }
        if (myConnectorSupers.isEmpty()) {
            myConnectorSupers.put(
                    ConnectorAnalyzer.ABSTRACT_COMPONENT_CONNECTOR,
                    COMPOSITE_WIDGET);
        }
    }

    private String suggestWidgetName() {
        if (myCreateWidget) {
            String name = getFileObject().getName();
            name = name + WIDGET;
            return findFreeName(name, myClientPackage);
        }
        return null;
    }

    private String suggestConnectorName() {
        String name = getFileObject().getName();
        name = name + WidgetGenerator.CONNECTOR;
        return findFreeName(name, myClientPackage);
    }

    private String findFreeName( String initialName, FileObject folder ) {
        StringBuilder freeName = new StringBuilder(initialName);
        String name = freeName.toString();

        if (folder == null) {
            return name;
        }

        freeName.append(JavaUtils.JAVA_SUFFIX);
        int i = 1;
        while (folder.getFileObject(freeName.toString()) != null) {
            freeName.setLength(0);
            freeName.append(name);
            freeName.append(i);
            freeName.append(JavaUtils.JAVA_SUFFIX);
            i++;
        }
        return freeName.substring(0,
                freeName.length() - JavaUtils.JAVA_SUFFIX.length());
    }

    private void searchClientPackage( final boolean create ) throws IOException
    {
        Project project = FileOwnerQuery.getOwner(getFileObject());
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        support.runModelOperation(new ModelOperation() {

            @Override
            public void run( VaadinModel model ) {
                if (model.getGwtXml() == null) {
                    return;
                }
                hasGwtXml = true;
                for (String clientPath : model.getSourcePaths()) {
                    try {
                        myClientPackage =
                                XmlUtils.getClientWidgetPackage(
                                        model.getGwtXml(), clientPath, create);
                        if (myClientPackage != null) {
                            return;
                        }
                    }
                    catch (IOException e) {
                        getLogger().log(Level.FINE, null, e);
                    }
                }
            }
        });
    }

    static String getWidgetFqn( Element connector, CompilationInfo info ) {
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
            return ((TypeElement) returnElement).getQualifiedName().toString();
        }
        return null;
    }

    static ExecutableElement getWidgetGetter( Element clazz ) {
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

    private final ElementHandle<TypeElement> myHandle;

    private final ElementHandle<TypeElement> myConnectorHandle;

    private final boolean myCreateWidget;

    private Map<String, String> myConnectorSupers;

    private volatile String myConnectorName;

    private volatile String myConnectorSuperClass;

    private volatile String myWidgetSuperClass;

    private volatile String myWidgetName;

    private FileObject myClientPackage;

    private boolean hasGwtXml;

}
