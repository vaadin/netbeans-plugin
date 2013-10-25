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
package org.vaadin.netbeans.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.modules.j2ee.dd.api.common.InitParam;
import org.netbeans.modules.j2ee.dd.api.web.Servlet;
import org.netbeans.modules.j2ee.dd.api.web.WebApp;
import org.netbeans.modules.xml.retriever.catalog.Utilities;
import org.netbeans.modules.xml.xam.ModelSource;
import org.netbeans.modules.xml.xam.locator.CatalogModelException;
import org.openide.filesystems.FileObject;
import org.vaadin.netbeans.code.generator.JavaUtils;
import org.vaadin.netbeans.code.generator.XmlUtils;
import org.vaadin.netbeans.model.ServletConfiguration;
import org.vaadin.netbeans.model.VaadinModel;
import org.vaadin.netbeans.model.gwt.GwtModel;
import org.vaadin.netbeans.model.gwt.GwtModelFactory;
import org.vaadin.netbeans.model.gwt.Module;
import org.vaadin.netbeans.model.gwt.Source;

/**
 * @author denis
 */
class VaadinModelImpl implements VaadinModel {

    private static final String CLIENT = "client"; // NOI18N

    private static final Logger LOG = Logger.getLogger(VaadinModelImpl.class
            .getName());

    VaadinModelImpl( Project project, boolean web ) {
        myConfigs = new HashMap<>();
        myProject = project;
        isWeb = web;
    }

    @Override
    public Collection<ServletConfiguration> getServletConfigurations() {
        if (!isWeb) {
            return Collections.emptyList();
        }
        Collection<ServletConfiguration> configs = myConfigs.values();
        int size = configs.size();
        Collection<ServletConfiguration> webXmlConfigs = getWebXmlConfigs();
        if (webXmlConfigs != null) {
            size += webXmlConfigs.size();
        }
        List<ServletConfiguration> result = new ArrayList<>(size);
        if (webXmlConfigs != null) {
            result.addAll(webXmlConfigs);
        }
        result.addAll(configs);
        return result;
    }

    @Override
    public List<String> getSourcePaths() {
        if (myGwtModel == null) {
            return null;
        }
        else {
            Module module = myGwtModel.getModule();
            if (module == null) {
                return Collections.singletonList(CLIENT);
            }
            List<Source> sources = module.getChildren(Source.class);
            if (sources.isEmpty()) {
                return Collections.singletonList(CLIENT);
            }
            else {
                List<String> paths = new ArrayList<>();
                for (Source source : sources) {
                    paths.add(source.getPath());
                }
                return paths;
            }
        }
    }

    @Override
    public GwtModel getGwtModel() {
        return myGwtModel;
    }

    @Override
    public FileObject getGwtXml() {
        return getGwtXml(true);
    }

    void initGwtXml() {
        FileObject gwtXml = findGwtXml();
        if (gwtXml == null) {
            myGwtModel = null;
        }
        else if (!gwtXml.equals(myGwtXml)) {
            myGwtModel =
                    GwtModelFactory.getInstance().getModel(
                            getModelSource(gwtXml));
            try {
                myGwtModel.sync();
            }
            catch (IOException e) {
                LOG.log(Level.INFO, null, e);
            }
        }
        myGwtXml = gwtXml;
    }

    void add( ElementHandle<TypeElement> handle, ServletConfigurationImpl impl )
    {
        if (myConfigs.get(handle) != null) {
            return;
        }
        myConfigs.put(handle, impl);
        initGwtXml();
    }

    void cleanup( boolean reinit ) {
        myConfigs.clear();
        if (reinit) {
            initGwtXml();
        }
        else {
            myGwtXml = null;
            myGwtModel = null;
        }
    }

    void remove( ElementHandle<TypeElement> handle ) {
        myConfigs.remove(handle);
        initGwtXml();
    }

    FileObject findGwtXml() {
        try {
            Set<FileObject> webWidgetsetFiles = getWebWidgetsetFiles();
            if (webWidgetsetFiles.isEmpty()) {
                LOG.log(Level.INFO,
                        "WEB configuration is not available for the project {0}, "
                                + "proceed with recursive GWT Modules search",
                        myProject.getProjectDirectory());//NOI18N
            }
            else {
                return webWidgetsetFiles.iterator().next();
            }
        }
        catch (IOException e) {
            LOG.log(Level.INFO,
                    "Unable to find widgetsets via WEB configuration, "
                            + "proceed with recursive GWT Modules ", e);//NOI18N
        }
        FileObject fileObject = XmlUtils.findGwtXml(myProject);
        if (fileObject != null && fileObject.isValid()) {
            return fileObject;
        }
        else {
            return null;
        }
    }

    public FileObject getGwtXml( boolean init ) {
        if (init && myGwtXml == null) {
            return XmlUtils.findGwtXml(myProject);
        }
        return myGwtXml;
    }

    private ModelSource getModelSource( FileObject fileObject ) {
        try {
            return Utilities.createModelSource(fileObject, true);
        }
        catch (CatalogModelException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return null;
    }

    private List<ServletConfiguration> getWebXmlConfigs() {
        WebApp webApp = null;
        try {
            webApp = XmlUtils.getWebApp(myProject);
        }
        catch (IOException e) {
            LOG.log(Level.INFO, null, e);
        }
        if (webApp == null) {
            return null;
        }
        Servlet[] servlets = webApp.getServlet();
        List<ServletConfiguration> result = new ArrayList<>(servlets.length);
        for (Servlet servlet : servlets) {
            InitParam[] initParams = servlet.getInitParam();
            for (InitParam initParam : initParams) {
                if (JavaUtils.WIDGETSET.equals(initParam.getParamName())
                        && initParam.getParamValue() != null)
                {
                    ServletConfigurationImpl impl =
                            new ServletConfigurationImpl();
                    impl.setWidgetset(initParam.getParamValue());
                    result.add(impl);
                }
            }
        }
        return result;
    }

    private Set<FileObject> getWebWidgetsetFiles() throws IOException {
        List<String> webWidgetset = getWebWidgetset();
        if (webWidgetset.isEmpty()) {
            return Collections.emptySet();
        }

        SourceGroup[] javaSourceGroups =
                JavaUtils.getJavaSourceGroups(myProject);
        SourceGroup[] resourcesSourceGroups =
                JavaUtils.getResourcesSourceGroups(myProject);
        List<SourceGroup> sourceGroups =
                new ArrayList<>(javaSourceGroups.length
                        + resourcesSourceGroups.length);
        sourceGroups.addAll(Arrays.asList(javaSourceGroups));
        sourceGroups.addAll(Arrays.asList(resourcesSourceGroups));

        Set<FileObject> result = new LinkedHashSet<>(webWidgetset.size());
        for (String widgetSet : webWidgetset) {
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

    private List<String> getWebWidgetset() throws IOException {
        Collection<ServletConfiguration> configs = getServletConfigurations();
        List<String> result = new ArrayList<>(configs.size());
        for (ServletConfiguration configuration : configs) {
            result.add(configuration.getWidgetset());
        }
        return result;
    }

    private final Map<ElementHandle<TypeElement>, ServletConfiguration> myConfigs;

    private FileObject myGwtXml;

    private final Project myProject;

    private final boolean isWeb;

    private GwtModel myGwtModel;

}