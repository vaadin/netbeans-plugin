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

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;
import org.vaadin.netbeans.utils.JavaUtils;
import org.vaadin.netbeans.utils.XmlUtils;
import org.vaadin.netbeans.utils.JavaUtils.JavaModelElement;

/**
 * @author denis
 */
public class FullFledgedWidgetGenerator implements WidgetGenerator {

    private static final String STYLE_NAME = "style_name";//NOI18N

    private static final String SHARED_STATE = "shared_state";//NOI18N

    private static final String CLIENT_RPC = "client_rpc";//NOI18N

    private static final String SERVER_RPC = "server_rpc";//NOI18N

    private static final String SHARED_STATE_FQN = "shared_state_fqn";//NOI18N

    private static final String SERVER_RPC_FQN = "server_rpc_fqn";//NOI18N

    private static final String CLIENT_RPC_FQN = "client_rpc_fqn";//NOI18N

    private static final String WIDGET = "widget";//NOI18N

    private static final String CONNECTOR_VAR = "connector";//NOI18N

    private static final String COMPONENT_VAR = "component";//NOI18N

    private static final String SERVER_COMPONENT_FQN = "server_component_fqn";//NOI18N

    private static final String SERVER_COMPONENT = "server_component";//NOI18N

    private static final String WIDGET_SUPER = "widget_super";//NOI18N

    private static final String WIDGET_SUPER_FQN = "widget_super_fqn";//NOI18N

    private static final String CONNECTOR_TEMPLATE =
            "Templates/Vaadin/FullFledgedConnector.java"; // NOI18N

    private static final String SHARED_STATE_TEMPLATE =
            "Templates/Vaadin/SharedState.java"; // NOI18N

    private static final String WIDGET_TEMPLATE =
            "Templates/Vaadin/FullFledgedWidget.java";// NOI18N

    private static final String SERVER_RPC_TEMPLATE =
            "Templates/Vaadin/ServerRpc.java";// NOI18N

    private static final String CLIENT_RPC_TEMPLATE =
            "Templates/Vaadin/ClientRpc.java";// NOI18N

    private static final String CLIENT_RPC_SUFFIX = "ClientRpc";// NOI18N

    private static final String SERVER_RPC_SUFFIX = "ServerRpc";// NOI18N

    private static final String SHARED_STATE_SUFFIX = "State";// NOI18N

    private static final String WIDGET_SUFFIX = "Widget";// NOI18N

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
                createSharedState(componentClassName, clientPackage, project,
                        Templates.getTargetName(wizard));
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
                createWidget(componentClassName, clientPackage, project,
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
        dataObject =
                JavaUtils.createDataObjectFromTemplate(CONNECTOR_TEMPLATE,
                        clientPackage, connectorName, map);
        classes.add(dataObject.getPrimaryFile());

        return classes;
    }

    private FileObject createWidget( String componentClassName,
            FileObject clientPackage, Project project, String connectorName,
            String stateName ) throws IOException
    {
        String name =
                JavaUtils.getFreeName(clientPackage, componentClassName
                        + WIDGET_SUFFIX, JavaUtils.JAVA_SUFFIX);
        Map<String, String> map = new HashMap<>();
        map.put(SHARED_STATE, stateName);
        map.put(CONNECTOR_VAR, connectorName);
        map.put(STYLE_NAME, componentClassName.toLowerCase());
        map.put(WIDGET_SUPER, null);
        map.put(WIDGET_SUPER_FQN, null);
        return JavaUtils.createDataObjectFromTemplate(WIDGET_TEMPLATE,
                clientPackage, name, map).getPrimaryFile();
    }

    private FileObject createSharedState( String componentClassName,
            FileObject clientPackage, Project project, String componentName )
            throws IOException
    {
        String name =
                JavaUtils.getFreeName(clientPackage, componentClassName
                        + SHARED_STATE_SUFFIX, JavaUtils.JAVA_SUFFIX);
        return JavaUtils.createDataObjectFromTemplate(SHARED_STATE_TEMPLATE,
                clientPackage, name,
                Collections.singletonMap(COMPONENT_VAR, componentName))
                .getPrimaryFile();
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
}
