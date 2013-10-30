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
package org.vaadin.netbeans.maven.project;

import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.xml.namespace.QName;

import org.netbeans.modules.maven.model.pom.Build;
import org.netbeans.modules.maven.model.pom.Configuration;
import org.netbeans.modules.maven.model.pom.POMComponent;
import org.netbeans.modules.maven.model.pom.POMExtensibilityElement;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.model.pom.POMQName;
import org.netbeans.modules.maven.model.pom.Plugin;

/**
 * @author denis
 */
public final class POMUtils {

    private static final String VAADIN_WIDGETSETS = "Vaadin-Widgetsets"; // NOI18N

    private static final String ARCHIVE = "archive"; // NOI18N

    private static final String MANIFEST = "manifestEntries"; // NOI18N

    private static final String MAVEN_PLUGINS_GROUP =
            "org.apache.maven.plugins"; // NOI18N

    private static final String JAR_ARTIFACT_ID = "maven-jar-plugin"; // NOI18N

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

    static Plugin getJarPlugin( POMModel model ) {
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

    static Plugin getVaadinPlugin( POMModel model ) {
        Build build = model.getProject().getBuild();
        if (build == null) {
            return null;
        }
        List<Plugin> plugins = build.getPlugins();
        Plugin vaadinPlugin = null;
        for (Plugin plugin : plugins) {
            if (VaadinCustomizer.VAADIN_GROUP_ID.equals(plugin.getGroupId())
                    && VaadinCustomizer.VAADIN_PLUGIN.equals(plugin
                            .getArtifactId()))
            {
                vaadinPlugin = plugin;
                break;
            }
        }
        return vaadinPlugin;
    }

    static Plugin getJettyPlugin( POMModel model ) {
        Build build = model.getProject().getBuild();
        if (build == null) {
            return null;
        }
        List<Plugin> plugins = build.getPlugins();
        Plugin jettyPlugin = null;
        for (Plugin plugin : plugins) {
            if (VaadinCustomizer.JETTY_GROUP_ID.equals(plugin.getGroupId())
                    && VaadinCustomizer.JETTY_PLUGIN.equals(plugin
                            .getArtifactId()))
            {
                jettyPlugin = plugin;
                break;
            }
        }
        return jettyPlugin;
    }

    static void setTextField( String optionName,
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

    static String getValue( POMExtensibilityElement element ) {
        return element.getElementText().trim();
    }

    static void setBooleanVaue( String optionName,
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

    static POMExtensibilityElement createElement( POMModel model, String name,
            String value )
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
}
