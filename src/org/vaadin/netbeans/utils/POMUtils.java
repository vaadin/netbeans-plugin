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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.TypeElement;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.xml.namespace.QName;

import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.api.ModelUtils;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.modules.maven.model.pom.Build;
import org.netbeans.modules.maven.model.pom.Configuration;
import org.netbeans.modules.maven.model.pom.POMComponent;
import org.netbeans.modules.maven.model.pom.POMExtensibilityElement;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.model.pom.POMQName;
import org.netbeans.modules.maven.model.pom.Plugin;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.customizer.VaadinConfiguration;
import org.vaadin.netbeans.editor.analyzer.IsInSourceQuery;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;

/**
 * @author denis
 */
public final class POMUtils {

    public static final String VAADIN_GROUP_ID = "com.vaadin"; // NOI18N

    public static final String VAADIN_PLUGIN = "vaadin-maven-plugin"; // NOI18N

    public static final String JETTY_GROUP_ID = "org.mortbay.jetty"; // NOI18N

    public static final String JETTY_PLUGIN = "jetty-maven-plugin"; // NOI18N

    private static final String VAADIN_WIDGETSETS = "Vaadin-Widgetsets"; // NOI18N

    private static final String ARCHIVE = "archive"; // NOI18N

    private static final String MANIFEST = "manifestEntries"; // NOI18N

    private static final String MAVEN_PLUGINS_GROUP =
            "org.apache.maven.plugins"; // NOI18N

    private static final String JAR_ARTIFACT_ID = "maven-jar-plugin"; // NOI18N

    private static final String STATISTICS_URL =
            "https://vaadin.com/stats/nbplugin?"
                    + "pluginver={0}&addon={1}&addonver={2}"; // NOI18N

    private static final String CURRENT_VERSION = "1.1"; // NOI18N

    private POMUtils() {
    }

    public static void createWidgetset( POMModel model, String widgetset ) {
        POMExtensibilityElement manifest = getManifestEntries(model);
        if (manifest == null) {
            Plugin plugin = getJarPlugin(model);
            if (plugin != null) {
                Configuration configuration = plugin.getConfiguration();
                if (configuration == null) {
                    configuration = model.getFactory().createConfiguration();
                    createArchive(configuration, widgetset);
                    plugin.setConfiguration(configuration);
                }
                else {
                    createWidgetset(configuration, widgetset);
                }
            }
        }
        else {
            createWidgetset(manifest, widgetset);
        }
    }

    public static void createWidgetset( Configuration configuration,
            String widgetset )
    {
        POMExtensibilityElement archive = getArchive(configuration);
        if (archive == null) {
            archive = createArchive(configuration, widgetset);
            configuration.addExtensibilityElement(archive);
        }
        else {
            POMExtensibilityElement manifestEntries =
                    getManifestEntries(archive);
            if (manifestEntries == null) {
                manifestEntries = createManifestEntries(archive, widgetset);
                archive.addExtensibilityElement(manifestEntries);
            }
            else {
                createWidgetset(manifestEntries, widgetset);
            }
        }
    }

    public static POMExtensibilityElement createArchive(
            Configuration configuration, String widgetset )
    {
        POMExtensibilityElement archive =
                POMUtils.createElement(configuration.getModel(), ARCHIVE, null);
        POMExtensibilityElement manifestEntries =
                createManifestEntries(archive, widgetset);
        archive.addExtensibilityElement(manifestEntries);
        return archive;
    }

    public static POMExtensibilityElement createManifestEntries(
            POMExtensibilityElement archive, String widgetset )
    {
        POMExtensibilityElement manifest =
                POMUtils.createElement(archive.getModel(), MANIFEST, null);
        createWidgetset(manifest, widgetset);
        return manifest;
    }

    public static void createWidgetset( POMExtensibilityElement manifest,
            String widget )
    {
        POMExtensibilityElement widgetset =
                POMUtils.createElement(manifest.getModel(), VAADIN_WIDGETSETS,
                        widget);
        manifest.addExtensibilityElement(widgetset);
    }

    public static void setWidgetset( POMExtensibilityElement widgetsets,
            String widget )
    {
        String oldValue = POMUtils.getValue(widgetsets);
        if (oldValue == null || !oldValue.equals(widget)) {
            widgetsets.setElementText(widget);
        }
    }

    public static POMExtensibilityElement getManifestEntries( POMModel model ) {
        Plugin plugin = getJarPlugin(model);
        if (plugin == null) {
            return null;
        }
        Configuration configuration = plugin.getConfiguration();
        if (configuration == null) {
            return null;
        }
        POMExtensibilityElement archive = getArchive(configuration);
        if (archive == null) {
            return null;
        }
        return getManifestEntries(archive);
    }

    public static POMExtensibilityElement getManifestEntries(
            POMExtensibilityElement archive )
    {
        List<POMExtensibilityElement> elements =
                archive.getExtensibilityElements();
        for (POMExtensibilityElement element : elements) {
            String name = element.getQName().getLocalPart();
            if (MANIFEST.equals(name)) {
                return element;
            }
        }
        return null;
    }

    public static POMExtensibilityElement getArchive(
            Configuration configuration )
    {
        List<POMExtensibilityElement> elements =
                configuration.getExtensibilityElements();
        POMExtensibilityElement archive = null;
        for (POMExtensibilityElement element : elements) {
            String name = element.getQName().getLocalPart();
            if (ARCHIVE.equals(name)) {
                archive = element;
            }
        }
        return archive;
    }

    public static POMExtensibilityElement getWidgetsets( POMModel model ) {
        POMExtensibilityElement manifest = getManifestEntries(model);
        if (manifest == null) {
            return null;
        }
        return getWidgetsets(manifest);
    }

    public static POMExtensibilityElement getWidgetsets(
            POMExtensibilityElement manifest )
    {
        List<POMExtensibilityElement> elements =
                manifest.getExtensibilityElements();
        for (POMExtensibilityElement element : elements) {
            String name = element.getQName().getLocalPart();
            if (VAADIN_WIDGETSETS.equals(name)) {
                return element;
            }
        }
        return null;
    }

    public static Plugin getJarPlugin( POMModel model ) {
        Build build = model.getProject().getBuild();
        if (build == null) {
            return null;
        }
        List<Plugin> plugins = build.getPlugins();
        Plugin jarPlugin = null;
        for (Plugin plugin : plugins) {
            if (MAVEN_PLUGINS_GROUP.equals(plugin.getGroupId())
                    && JAR_ARTIFACT_ID.equals(plugin.getArtifactId()))
            {
                jarPlugin = plugin;
                break;
            }
        }
        return jarPlugin;
    }

    public static Plugin getVaadinPlugin( POMModel model ) {
        Build build = model.getProject().getBuild();
        if (build == null) {
            return null;
        }
        List<Plugin> plugins = build.getPlugins();
        Plugin vaadinPlugin = null;
        for (Plugin plugin : plugins) {
            if (VAADIN_GROUP_ID.equals(plugin.getGroupId())
                    && VAADIN_PLUGIN.equals(plugin.getArtifactId()))
            {
                vaadinPlugin = plugin;
                break;
            }
        }
        return vaadinPlugin;
    }

    public static Plugin getJettyPlugin( POMModel model ) {
        Build build = model.getProject().getBuild();
        if (build == null) {
            return null;
        }
        List<Plugin> plugins = build.getPlugins();
        Plugin jettyPlugin = null;
        for (Plugin plugin : plugins) {
            if (JETTY_GROUP_ID.equals(plugin.getGroupId())
                    && JETTY_PLUGIN.equals(plugin.getArtifactId()))
            {
                jettyPlugin = plugin;
                break;
            }
        }
        return jettyPlugin;
    }

    public static void setTextField( String optionName,
            Map<String, POMExtensibilityElement> values, JTextField textField,
            POMComponent component )
    {
        POMExtensibilityElement element = values.get(optionName);
        if (element == null) {
            if (textField.getText().trim().length() > 0) {
                component.addExtensibilityElement(POMUtils.createElement(
                        component.getModel(), optionName, textField.getText()));
            }
        }
        else if (!getValue(element).equals(textField.getText().trim())) {
            element.setElementText(textField.getText());
        }
    }

    public static String getValue( POMExtensibilityElement element ) {
        return element.getElementText().trim();
    }

    public static void setBooleanVaue( String optionName,
            Map<String, POMExtensibilityElement> values, JCheckBox checkBox,
            Configuration configuration )
    {
        POMExtensibilityElement element = values.get(optionName);
        boolean isEnabled = checkBox.isSelected();
        if (element == null) {
            if (isEnabled) {
                configuration.addExtensibilityElement(POMUtils.createElement(
                        configuration.getModel(), optionName,
                        Boolean.TRUE.toString()));
            }
        }
        else {
            String value = getValue(element);
            if (!value.equals(Boolean.toString(isEnabled))) {
                element.setElementText(Boolean.toString(isEnabled));
            }
        }
    }

    public static POMExtensibilityElement createElement( POMModel model,
            String name, String value )
    {
        QName qname =
                POMQName.createQName(name, model.getPOMQNames().isNSAware());
        POMExtensibilityElement element =
                model.getFactory().createPOMExtensibilityElement(qname);
        if (value != null) {
            element.setElementText(value);
        }
        return element;
    }

    public static void addDependency( Project project, String groupId,
            String artifactId, String version, String scope )
    {
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        if (support != null && support.isWeb()) {
            final boolean[] hasGwtXml = new boolean[1];
            try {
                support.runModelOperation(new ModelOperation() {

                    @Override
                    public void run( VaadinModel model ) {
                        FileObject gwtXml = model.getGwtXml();
                        hasGwtXml[0] = gwtXml != null;
                    }
                });

                if (!hasGwtXml[0]) {
                    createGwtXml(support, project);
                }
            }
            catch (IOException e) {
                Logger.getLogger(POMUtils.class.getName()).log(Level.INFO,
                        null, e);
            }
        }
        NbMavenProject mavenProject =
                project.getLookup().lookup(NbMavenProject.class);
        File file = mavenProject.getMavenProject().getFile();
        FileObject pom = FileUtil.toFileObject(FileUtil.normalizeFile(file));
        ModelUtils.addDependency(pom, groupId, artifactId, version, null,
                scope, null, false);
        mavenProject.downloadDependencyAndJavadocSource(false);
        sendUsageStatistics(artifactId, version);
    }

    private static void sendUsageStatistics( String artifactId, String version )
    {
        if (!VaadinConfiguration.getInstance().isStatisticsEnabled()) {
            return;
        }
        String statistics =
                MessageFormat.format(STATISTICS_URL, getPluginVersion(),
                        artifactId, version);
        InputStream inputStream = null;
        try {
            URL url = new URL(statistics);
            URLConnection connection = url.openConnection();
            connection.connect();
            inputStream = connection.getInputStream();
            byte[] bytes = new byte[100];
            while (inputStream.read(bytes) != -1) {
            }
        }
        catch (MalformedURLException e) {
            Logger.getLogger(POMUtils.class.getName()).log(Level.INFO, null, e);
        }
        catch (IOException e) {
            Logger.getLogger(POMUtils.class.getName()).log(Level.FINE, null, e);
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException ignore) {
                }
            }
        }
    }

    private static String getPluginVersion() {
        return CURRENT_VERSION;
    }

    private static void createGwtXml( VaadinSupport support,
            final Project project ) throws IOException
    {
        JavaSource javaSource = JavaSource.create(support.getClassPathInfo());
        if (javaSource == null) {
            return;
        }
        final FileObject[] uiFolder = new FileObject[1];
        javaSource.runUserActionTask(new Task<CompilationController>() {

            @Override
            public void run( CompilationController controller )
                    throws Exception
            {
                controller.toPhase(Phase.ELEMENTS_RESOLVED);

                TypeElement ui =
                        controller.getElements().getTypeElement(
                                JavaUtils.VAADIN_UI_FQN);
                if (ui == null) {
                    return;
                }
                Set<TypeElement> uis = JavaUtils.getSubclasses(ui, controller);
                Set<FileObject> sourceRoots =
                        IsInSourceQuery.getSourceRoots(project);
                for (TypeElement element : uis) {
                    FileObject fileObject =
                            SourceUtils.getFile(ElementHandle.create(element),
                                    controller.getClasspathInfo());
                    if (!ui.equals(element)
                            && IsInSourceQuery.isInSourceRoots(fileObject,
                                    sourceRoots))
                    {
                        uiFolder[0] = fileObject.getParent();
                        return;
                    }
                }
            }
        }, true);
        if (uiFolder[0] != null) {
            XmlUtils.createGwtXml(uiFolder[0]);
        }
    }

}
