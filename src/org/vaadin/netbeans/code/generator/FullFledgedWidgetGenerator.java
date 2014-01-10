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
package org.vaadin.netbeans.code.generator;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.code.WidgetUtils;
import org.vaadin.netbeans.code.WidgetUtils.ConnectorInfo;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;
import org.vaadin.netbeans.utils.JavaUtils;
import org.vaadin.netbeans.utils.JavaUtils.JavaModelElement;
import org.vaadin.netbeans.utils.XmlUtils;

/**
 * @author denis
 */
public class FullFledgedWidgetGenerator implements WidgetGenerator {

    private static final String CLIENT_RPC = "client_rpc";//NOI18N

    private static final String SERVER_RPC = "server_rpc";//NOI18N

    private static final String SHARED_STATE_FQN = "shared_state_fqn";//NOI18N

    private static final String SERVER_RPC_FQN = "server_rpc_fqn";//NOI18N

    private static final String CLIENT_RPC_FQN = "client_rpc_fqn";//NOI18N

    private static final String WIDGET = "widget";//NOI18N

    private static final String CONNECTOR_TEMPLATE =
            "Templates/Vaadin/FullFledgedConnector.java"; // NOI18N

    private static final String SHARED_STATE_TEMPLATE =
            "Templates/Vaadin/SharedState.java"; // NOI18N

    private static final String SERVER_RPC_TEMPLATE =
            "Templates/Vaadin/ServerRpc.java";// NOI18N

    private static final String CLIENT_RPC_TEMPLATE =
            "Templates/Vaadin/ClientRpc.java";// NOI18N

    private static final String CLIENT_RPC_SUFFIX = "ClientRpc";// NOI18N

    private static final String SERVER_RPC_SUFFIX = "ServerRpc";// NOI18N

    private static final String SHARED_STATE_SUFFIX = "State";// NOI18N

    @Override
    @NbBundle.Messages({ "generateWidget=Generate Widget class",
            "generateState=Generate Shared State class",
            "generateClientRpc=Generate Client RPC class",
            "generateServerRpc=Generate Server RPC class" })
    public Set<FileObject> generate( WizardDescriptor wizard,
            ProgressHandle handle ) throws IOException
    {
        Set<FileObject> classes = new LinkedHashSet<>();
        FileObject targetPackage = Templates.getTargetFolder(wizard);
        String componentClassName = Templates.getTargetName(wizard);

        // Find/create gwt.xml and sources package
        Project project = Templates.getProject(wizard);
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        final FileObject[] gwtXml = new FileObject[1];
        final List<String> srcPaths = new LinkedList<>();
        if (support != null) {
            support.runModelOperation(new ModelOperation() {

                @Override
                public void run( VaadinModel model ) {
                    gwtXml[0] = model.getGwtXml();
                    if (gwtXml[0] != null) {
                        srcPaths.addAll(model.getSourcePaths());
                    }
                }
            });
        }

        if (gwtXml[0] == null) {
            handle.progress(Bundle.generateGwtXml());
            gwtXml[0] = XmlUtils.createGwtXml(targetPackage);
            classes.add(gwtXml[0]);
            if (support != null) {
                waitGwtXml(support, srcPaths);
            }
        }
        else if (!XmlUtils.checkServerPackage(targetPackage, srcPaths,
                gwtXml[0]))
        {
            return Collections.emptySet();
        }

        FileObject clientPackage =
                XmlUtils.getClientWidgetPackage(gwtXml[0], srcPaths.get(0),
                        true);

        ComponentBean bean = getComponentData(wizard);

        // Generate rpcs, shared state
        handle.progress(Bundle.generateClientRpc());
        FileObject clientRpc =
                createClientRpc(componentClassName, clientPackage, project);
        classes.add(clientRpc);
        handle.progress(Bundle.generateServerRpc());
        FileObject serverRpc =
                createServerRpc(componentClassName, clientPackage, project);
        classes.add(serverRpc);
        handle.progress(Bundle.generateState());

        FileObject sharedState =
                createSharedState(componentClassName, clientPackage, bean,
                        project, Templates.getTargetName(wizard));
        classes.add(sharedState);

        JavaModelElement clientRpcElement =
                JavaUtils.getModlelElement(clientRpc);
        JavaModelElement serverRpcElement =
                JavaUtils.getModlelElement(serverRpc);
        JavaModelElement stateElement = JavaUtils.getModlelElement(sharedState);

        // Generate server component
        FileObject template = Templates.getTemplate(wizard);

        template.setAttribute(CLIENT_RPC_FQN, clientRpcElement.getFqn());
        template.setAttribute(SERVER_RPC_FQN, serverRpcElement.getFqn());
        template.setAttribute(SHARED_STATE_FQN, stateElement.getFqn());

        template.setAttribute(SERVER_RPC, serverRpc.getName());
        template.setAttribute(CLIENT_RPC, clientRpc.getName());
        template.setAttribute(SHARED_STATE, stateElement.getName());
        template.setAttribute(SELECTED_COMPONENT, bean.getComponent());
        template.setAttribute(SELECTED_COMPONENT_FQN, bean.getComponentFqn());
        DataObject dataTemplate = DataObject.find(template);
        DataFolder dataFolder = DataFolder.findFolder(targetPackage);
        DataObject dataObject =
                dataTemplate.createFromTemplate(dataFolder, componentClassName);
        FileObject serverComponent = dataObject.getPrimaryFile();
        classes.add(dataObject.getPrimaryFile());

        handle.progress(Bundle.generateConnector());
        String connectorName =
                JavaUtils.getFreeName(clientPackage, componentClassName
                        + CONNECTOR, JavaUtils.JAVA_SUFFIX);

        // Generate widget
        handle.progress(Bundle.generateWidget());
        FileObject widget =
                createWidget(componentClassName, clientPackage, bean, project,
                        connectorName, sharedState.getName());
        classes.add(widget);

        // Generate connector
        handle.progress(Bundle.generateConnector());
        Map<String, String> map = new HashMap<>();
        map.put(SERVER_COMPONENT, componentClassName);
        map.put(SERVER_COMPONENT_FQN, JavaUtils.getFqn(serverComponent));
        map.put(SERVER_RPC, serverRpc.getName());
        map.put(CLIENT_RPC, clientRpc.getName());
        map.put(SHARED_STATE, stateElement.getName());
        map.put(WIDGET, widget.getName());
        map.put(SUPER_CONNECTOR, bean.getConnector());
        map.put(SUPER_CONNECTOR_FQN, bean.getConnectorFqn());
        dataObject =
                JavaUtils.createDataObjectFromTemplate(CONNECTOR_TEMPLATE,
                        clientPackage, connectorName, map);
        classes.add(dataObject.getPrimaryFile());

        return classes;
    }

    private FileObject createWidget( String componentClassName,
            FileObject clientPackage, ComponentBean bean, Project project,
            String connectorName, String stateName ) throws IOException
    {
        String name =
                JavaUtils.getFreeName(clientPackage, componentClassName
                        + WIDGET_SUFFIX, JavaUtils.JAVA_SUFFIX);
        Map<String, String> map = new HashMap<>();
        map.put(SHARED_STATE, stateName);
        map.put(CONNECTOR_VAR, connectorName);
        map.put(STYLE_NAME, componentClassName.toLowerCase());
        map.put(WIDGET_SUPER, bean.getWidget());
        map.put(WIDGET_SUPER_FQN, bean.getWidgetFqn());
        return JavaUtils.createDataObjectFromTemplate(WIDGET_TEMPLATE,
                clientPackage, name, map).getPrimaryFile();
    }

    private FileObject createSharedState( String componentClassName,
            FileObject clientPackage, ComponentBean bean, Project project,
            String componentName ) throws IOException
    {
        String name =
                JavaUtils.getFreeName(clientPackage, componentClassName
                        + SHARED_STATE_SUFFIX, JavaUtils.JAVA_SUFFIX);
        Map<String, String> map = new HashMap<>();
        map.put(COMPONENT_VAR, componentName);
        map.put(STATE_SUPER_CLASS_FQN, bean.getStateFqn());
        map.put(STATE_SUPER_CLASS, bean.getState());
        return JavaUtils.createDataObjectFromTemplate(SHARED_STATE_TEMPLATE,
                clientPackage, name, map).getPrimaryFile();
    }

    private boolean isDefaultConnector( String connectorFqn ) {
        return connectorFqn == null
                || WidgetUtils.ABSTRACT_COMPONENT_CONNECTOR
                        .equals(connectorFqn);
    }

    private FileObject createServerRpc( String componentClassName,
            FileObject clientPackage, Project project ) throws IOException
    {
        String name =
                JavaUtils.getFreeName(clientPackage, componentClassName
                        + SERVER_RPC_SUFFIX, JavaUtils.JAVA_SUFFIX);
        return JavaUtils.createDataObjectFromTemplate(SERVER_RPC_TEMPLATE,
                clientPackage, name, null).getPrimaryFile();
    }

    private FileObject createClientRpc( String componentClassName,
            FileObject clientPackage, Project project ) throws IOException
    {
        String name =
                JavaUtils.getFreeName(clientPackage, componentClassName
                        + CLIENT_RPC_SUFFIX, JavaUtils.JAVA_SUFFIX);
        return JavaUtils.createDataObjectFromTemplate(CLIENT_RPC_TEMPLATE,
                clientPackage, name, null).getPrimaryFile();
    }

    private ComponentBean getComponentData( WizardDescriptor wizard )
            throws IOException
    {
        Object component = wizard.getProperty(COMPONENT_PROPERTY);
        ComponentBean bean = new ComponentBean();
        if (component != null) {
            String fqn = component.toString();
            ConnectorInfo connectorInfo =
                    WidgetUtils.getConnectorInfo(Templates.getProject(wizard),
                            fqn);

            if (connectorInfo == null) {
                return bean;
            }

            int index = fqn.lastIndexOf('.');
            String componentName = fqn.substring(index + 1);
            bean.setComponent(componentName);
            bean.setComponentFqn(fqn);

            ElementHandle<TypeElement> connectorHandle =
                    connectorInfo.getConnector();
            String connectorFqn = connectorHandle.getQualifiedName();
            index = connectorFqn.lastIndexOf('.');
            String connector = connectorFqn.substring(index + 1);
            bean.setConnectorFqn(connectorFqn);
            bean.setConnector(connector);

            ElementHandle<TypeElement> widgetHandle = connectorInfo.getWidget();
            if (widgetHandle != null
                    && !WidgetUtils.isDefaultWidget(connectorFqn,
                            widgetHandle.getQualifiedName()))
            {
                String widgetFqn = widgetHandle.getQualifiedName();
                index = widgetFqn.lastIndexOf('.');
                String widget = widgetFqn.substring(index + 1);
                bean.setWidget(widget);
                bean.setWidgetFqn(widgetFqn);
            }

            ElementHandle<TypeElement> stateHandle = connectorInfo.getState();
            if (stateHandle != null) {
                String stateFqn = stateHandle.getQualifiedName();
                index = stateFqn.lastIndexOf('.');
                String state = stateFqn.substring(index + 1);
                bean.setStateFqn(stateFqn);
                bean.setState(state);
            }

        }
        return bean;
    }

    static void waitGwtXml( final VaadinSupport support,
            final List<String> srcPath )
    {
        try {
            final boolean[] gwtXmlCreated = new boolean[1];
            support.runModelOperation(new ModelOperation() {

                @Override
                public void run( VaadinModel model ) {
                    gwtXmlCreated[0] = model.getGwtXml() != null;
                    if (gwtXmlCreated[0]) {
                        srcPath.add(model.getSourcePaths().get(0));
                    }
                }
            });
            if (!gwtXmlCreated[0]) {
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException ignore) {
                }
                waitGwtXml(support, srcPath);
            }
        }
        catch (IOException e) {
            Logger.getLogger(FullFledgedWidgetGenerator.class.getName()).log(
                    Level.INFO, null, e);
        }
    }

    private static class ComponentBean {

        public String getConnector() {
            return myConnector;
        }

        public String getConnectorFqn() {
            return myConnectorFqn;
        }

        public String getWidget() {
            return myWidget;
        }

        public String getWidgetFqn() {
            return myWidgetFqn;
        }

        public String getState() {
            return myState;
        }

        public String getStateFqn() {
            return myStateFqn;
        }

        public String getComponent() {
            return myComponent;
        }

        public String getComponentFqn() {
            return myComponentFqn;
        }

        private void setConnector( String connector ) {
            myConnector = connector;
        }

        private void setConnectorFqn( String connectorFqn ) {
            myConnectorFqn = connectorFqn;
        }

        private void setWidget( String widget ) {
            myWidget = widget;
        }

        private void setWidgetFqn( String widgetFqn ) {
            myWidgetFqn = widgetFqn;
        }

        private void setState( String state ) {
            myState = state;
        }

        private void setStateFqn( String stateFqn ) {
            myStateFqn = stateFqn;
        }

        private void setComponentFqn( String componentFqn ) {
            myComponentFqn = componentFqn;
        }

        private void setComponent( String component ) {
            myComponent = component;
        }

        private String myConnector;

        private String myConnectorFqn;

        private String myWidget;

        private String myWidgetFqn;

        private String myState;

        private String myStateFqn;

        private String myComponent;

        private String myComponentFqn;
    }
}
