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
import java.util.logging.Logger;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.code.WidgetUtils;
import org.vaadin.netbeans.code.WidgetUtils.ConnectorInfo;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;
import org.vaadin.netbeans.utils.JavaUtils;
import org.vaadin.netbeans.utils.XmlUtils;

/**
 * @author denis
 */
abstract class SimpleConnectorGenerator implements WidgetGenerator {

    private static final String WIDGET_PARAM = "widget";//NOI18N

    @NbBundle.Messages("generateGwtXml=Generate GWT Module XML file")
    @Override
    public Set<FileObject> generate( WizardDescriptor wizard,
            ProgressHandle handle ) throws IOException
    {
        Set<FileObject> classes = new LinkedHashSet<>();
        FileObject targetPackage = Templates.getTargetFolder(wizard);
        String componentClassName = Templates.getTargetName(wizard);

        // Generate server component
        handle.progress(getComponentClassGenerationMessage());

        Map<String, String> params = initComponentParameters(wizard);
        DataObject dataObject =
                JavaUtils.createDataObjectFromTemplate(getComponentTemplate(),
                        targetPackage, componentClassName, params);

        FileObject serverComponent = dataObject.getPrimaryFile();
        classes.add(dataObject.getPrimaryFile());

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
                FullFledgedWidgetGenerator.waitGwtXml(support, srcPaths);
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

        if (clientPackage == null) {
            Logger.getLogger(ConnectorOnlyWidgetGenerator.class.getName())
                    .severe("Unable to detect package for client side classes");
            return classes;
        }

        // Generate connector
        String connectorName =
                JavaUtils.getFreeName(clientPackage, componentClassName
                        + CONNECTOR, JavaUtils.JAVA_SUFFIX);

        handle.progress(getConnectorClassGenerationMessage());
        Map<String, String> map = new HashMap<>();
        FileObject widget =
                createWidget(wizard, componentClassName, serverComponent,
                        clientPackage, project, map);
        if (widget != null) {
            classes.add(widget);
        }
        dataObject =
                JavaUtils.createDataObjectFromTemplate(getConnectorTemplate(),
                        clientPackage, connectorName, map);
        classes.add(dataObject.getPrimaryFile());

        return classes;
    }

    private FileObject createWidget( String componentClassName,
            FileObject clientPackage, String widgetSuper, Project project )
            throws IOException
    {
        String name =
                JavaUtils.getFreeName(clientPackage, componentClassName
                        + WIDGET_SUFFIX, JavaUtils.JAVA_SUFFIX);
        Map<String, String> map = new HashMap<>();
        map.put(SHARED_STATE, null);
        map.put(CONNECTOR_VAR, null);
        map.put(STYLE_NAME, componentClassName.toLowerCase());
        int index = widgetSuper.lastIndexOf('.');
        map.put(WIDGET_SUPER, widgetSuper.substring(index + 1));
        map.put(WIDGET_SUPER_FQN, widgetSuper);
        return JavaUtils.createDataObjectFromTemplate(WIDGET_TEMPLATE,
                clientPackage, name, map).getPrimaryFile();
    }

    private FileObject createWidget( WizardDescriptor wizard,
            String componentClassName, FileObject serverComponent,
            FileObject clientPackage, Project project, Map<String, String> map )
            throws IOException
    {
        map.put(getServerClassNameParam(), componentClassName);
        map.put(getServerClassFqnParam(), JavaUtils.getFqn(serverComponent));

        String superConnectorFqn = null;
        String superConnector = null;
        String widget = null;

        Object component = wizard.getProperty(COMPONENT_PROPERTY);
        FileObject widgetFile = null;
        if (component != null) {
            String fqn = component.toString();
            ConnectorInfo connectorInfo =
                    WidgetUtils.getConnectorInfo(Templates.getProject(wizard),
                            fqn);
            if (connectorInfo != null) {
                ElementHandle<TypeElement> connectorHandle =
                        connectorInfo.getConnector();
                superConnectorFqn = connectorHandle.getQualifiedName();
                int index = superConnectorFqn.lastIndexOf('.');
                superConnector = superConnectorFqn.substring(index + 1);

                ElementHandle<TypeElement> widgetHandle =
                        connectorInfo.getWidget();
                if (widgetHandle != null
                        && !WidgetUtils.isDefaultWidget(superConnectorFqn,
                                widgetHandle.getQualifiedName()))
                {
                    widgetFile =
                            createWidget(componentClassName, clientPackage,
                                    widgetHandle.getQualifiedName(), project);
                    widget = widgetFile.getName();
                }
            }
        }

        map.put(SUPER_CONNECTOR_FQN, superConnectorFqn);
        map.put(SUPER_CONNECTOR, superConnector);
        map.put(WIDGET_PARAM, widget);
        return widgetFile;
    }

    private Map<String, String> initComponentParameters( WizardDescriptor wizard )
    {
        Object component = wizard.getProperty(COMPONENT_PROPERTY);
        Map<String, String> params = new HashMap<>();
        if (component != null) {
            String fqn = component.toString();
            int index = fqn.lastIndexOf('.');
            String name = fqn;
            if (index != -1) {
                name = fqn.substring(index + 1);
            }
            params.put(SELECTED_COMPONENT, name);
            params.put(SELECTED_COMPONENT_FQN, fqn);
        }
        else {
            params.put(SELECTED_COMPONENT, null);
            params.put(SELECTED_COMPONENT_FQN, null);
        }
        return params;
    }

    protected String getServerClassNameParam() {
        return SERVER_COMPONENT;
    }

    protected String getServerClassFqnParam() {
        return SERVER_COMPONENT_FQN;
    }

    protected abstract String getComponentTemplate();

    protected abstract String getConnectorTemplate();

    protected abstract String getComponentClassGenerationMessage();

    protected abstract String getConnectorClassGenerationMessage();
}
