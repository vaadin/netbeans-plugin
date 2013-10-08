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

import java.awt.Component;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.maven.project.MavenProject;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.modules.maven.api.customizer.ModelHandle2;
import org.netbeans.modules.maven.model.ModelOperation;
import org.netbeans.modules.maven.model.Utilities;
import org.netbeans.modules.maven.model.pom.Build;
import org.netbeans.modules.maven.model.pom.Configuration;
import org.netbeans.modules.maven.model.pom.POMExtensibilityElement;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.model.pom.Plugin;
import org.netbeans.modules.maven.model.pom.Properties;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeAcceptor;
import org.openide.nodes.NodeOperation;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.UserCancelException;
import org.vaadin.netbeans.code.generator.JavaUtils;
import org.vaadin.netbeans.code.generator.XmlUtils;

/**
 * @author denis
 */
public class AddOnOptionsPanel extends VersionPanel {

    private static final String VAADIN_WIDGETSETS = "Vaadin-Widgetsets"; // NOI18N

    private static final String IMPLEMENTATION_VENDOR = "Implementation-Vendor"; // NOI18N

    private static final String IMPLEMENTATION_TITLE = "Implementation-Title"; // NOI18N

    private static final String IMPLEMENTATION_VERSION = "Implementation-Version"; // NOI18N

    private static final String MAVEN_PLUGINS_GROUP = "org.apache.maven.plugins"; // NOI18N

    private static final String JAR_ARTIFACT_ID = "maven-jar-plugin"; // NOI18N

    private static final String ARCHIVE = "archive"; // NOI18N

    private static final String MANIFEST = "manifestEntries"; // NOI18N

    public AddOnOptionsPanel( Lookup context ) {
        myProject = context.lookup(Project.class);
        initComponents();

        initVersions(context, myVaadinVersion);

        readOptions(context);
        final ModelHandle2 handle = context.lookup(ModelHandle2.class);
        final AddonModification modification = new AddonModification();

        myVaadinVersion.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged( ItemEvent e ) {
                final StringWrapper version = (StringWrapper) myVaadinVersion
                        .getModel().getSelectedItem();
                if (version == null || version.equals(StringWrapper.WAIT)) {
                    return;
                }
                addModification(handle, modification);
            }

        });

        DocumentListener listener = new DocumentListener() {

            @Override
            public void removeUpdate( DocumentEvent e ) {
                update();
            }

            @Override
            public void insertUpdate( DocumentEvent e ) {
                update();
            }

            @Override
            public void changedUpdate( DocumentEvent e ) {
                update();
            }

            private void update() {
                addModification(handle, modification);
            }
        };
        for (Component component : getComponents()) {
            if (component instanceof JTextField) {
                ((JTextField) component).getDocument().addDocumentListener(
                        listener);
            }
        }
    }

    private void readOptions( Lookup context ) {
        Project project = context.lookup(Project.class);
        NbMavenProject mvnProject = project.getLookup().lookup(
                NbMavenProject.class);
        MavenProject mavenProject = mvnProject.getMavenProject();
        File file = mavenProject.getFile();
        FileObject pom = FileUtil.toFileObject(FileUtil.normalizeFile(file));

        final String[] version = new String[1];
        final String[] title = new String[1];
        final String[] vendor = new String[1];
        final String[] widgetset = new String[1];
        ModelOperation<POMModel> operation = new ModelOperation<POMModel>() {

            @Override
            public void performOperation( POMModel model ) {
                Properties properties = model.getProject().getProperties();
                if (properties != null) {
                    version[0] = properties.getProperty(IMPLEMENTATION_VERSION);
                    title[0] = properties.getProperty(IMPLEMENTATION_TITLE);
                    vendor[0] = properties.getProperty(IMPLEMENTATION_VENDOR);
                }
                POMExtensibilityElement widgetsets = getWidgetsets(model);
                if (widgetsets != null) {
                    widgetset[0] = widgetsets.getElementText();
                    if (widgetset[0] != null) {
                        widgetset[0] = widgetset[0].trim();
                    }
                }
            }
        };
        Utilities.performPOMModelOperations(pom,
                Collections.singletonList(operation));

        myImplTitle.setText(title[0]);
        myImplVersion.setText(version[0]);
        myImplVendor.setText(vendor[0]);
        myWidgetset.setText(widgetset[0]);
    }

    private void addModification( ModelHandle2 handle,
            AddonModification newOperation )
    {
        List<ModelOperation<POMModel>> pomOperations = handle
                .getPOMOperations();
        for (ModelOperation<POMModel> operation : pomOperations) {
            if (operation instanceof AddonModification) {
                handle.removePOMModification(operation);
            }
        }
        handle.addPOMModification(newOperation);
    }

    private Plugin getJarPlugin( POMModel model ) {
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

    private POMExtensibilityElement getManifestEntries( POMModel model ) {
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

    private POMExtensibilityElement getManifestEntries(
            POMExtensibilityElement archive )
    {
        List<POMExtensibilityElement> elements = archive
                .getExtensibilityElements();
        for (POMExtensibilityElement element : elements) {
            String name = element.getQName().getLocalPart();
            if (MANIFEST.equals(name)) {
                return element;
            }
        }
        return null;
    }

    private POMExtensibilityElement getArchive( Configuration configuration ) {
        List<POMExtensibilityElement> elements = configuration
                .getExtensibilityElements();
        POMExtensibilityElement archive = null;
        for (POMExtensibilityElement element : elements) {
            String name = element.getQName().getLocalPart();
            if (ARCHIVE.equals(name)) {
                archive = element;
            }
        }
        return archive;
    }

    private POMExtensibilityElement getWidgetsets( POMModel model ) {
        POMExtensibilityElement manifest = getManifestEntries(model);
        if (manifest == null) {
            return null;
        }
        return getWidgetset(manifest);
    }

    private POMExtensibilityElement getWidgetset(
            POMExtensibilityElement manifest )
    {
        List<POMExtensibilityElement> elements = manifest
                .getExtensibilityElements();
        for (POMExtensibilityElement element : elements) {
            String name = element.getQName().getLocalPart();
            if (VAADIN_WIDGETSETS.equals(name)) {
                return element;
            }
        }
        return null;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        versionLbl = new javax.swing.JLabel();
        myVaadinVersion = new javax.swing.JComboBox<StringWrapper>();
        implVersionLbl = new javax.swing.JLabel();
        myImplVersion = new javax.swing.JTextField();
        implTitle = new javax.swing.JLabel();
        myImplTitle = new javax.swing.JTextField();
        implVendorLbl = new javax.swing.JLabel();
        myImplVendor = new javax.swing.JTextField();
        widgetsetLbl = new javax.swing.JLabel();
        myWidgetset = new javax.swing.JTextField();
        myBrowse = new javax.swing.JButton();

        versionLbl.setLabelFor(myVaadinVersion);
        org.openide.awt.Mnemonics.setLocalizedText(versionLbl,
                org.openide.util.NbBundle.getMessage(AddOnOptionsPanel.class,
                        "LBL_VaadinVersion")); // NOI18N

        implVersionLbl.setLabelFor(myImplVersion);
        org.openide.awt.Mnemonics.setLocalizedText(implVersionLbl,
                org.openide.util.NbBundle.getMessage(AddOnOptionsPanel.class,
                        "LBL_ImplVersion")); // NOI18N

        implTitle.setLabelFor(myImplTitle);
        org.openide.awt.Mnemonics.setLocalizedText(implTitle,
                org.openide.util.NbBundle.getMessage(AddOnOptionsPanel.class,
                        "LBL_ImplTitle")); // NOI18N

        implVendorLbl.setLabelFor(myImplVendor);
        org.openide.awt.Mnemonics.setLocalizedText(implVendorLbl,
                org.openide.util.NbBundle.getMessage(AddOnOptionsPanel.class,
                        "LBL_ImplVendor")); // NOI18N

        widgetsetLbl.setLabelFor(myWidgetset);
        org.openide.awt.Mnemonics.setLocalizedText(widgetsetLbl,
                org.openide.util.NbBundle.getMessage(AddOnOptionsPanel.class,
                        "LBL_Widgetset")); // NOI18N

        myWidgetset.setEditable(false);

        org.openide.awt.Mnemonics.setLocalizedText(myBrowse,
                org.openide.util.NbBundle.getMessage(AddOnOptionsPanel.class,
                        "LBL_Browse")); // NOI18N
        myBrowse.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed( java.awt.event.ActionEvent evt ) {
                browse(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(
                        layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(
                                                        layout.createSequentialGroup()
                                                                .addComponent(
                                                                        implVendorLbl)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(
                                                                        myImplVendor))
                                                .addGroup(
                                                        layout.createSequentialGroup()
                                                                .addGroup(
                                                                        layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(
                                                                                        implVersionLbl)
                                                                                .addComponent(
                                                                                        versionLbl)
                                                                                .addComponent(
                                                                                        implTitle)
                                                                                .addComponent(
                                                                                        widgetsetLbl))
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(
                                                                        layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(
                                                                                        myVaadinVersion,
                                                                                        0,
                                                                                        412,
                                                                                        Short.MAX_VALUE)
                                                                                .addComponent(
                                                                                        myImplVersion)
                                                                                .addComponent(
                                                                                        myImplTitle)
                                                                                .addGroup(
                                                                                        layout.createSequentialGroup()
                                                                                                .addComponent(
                                                                                                        myWidgetset)
                                                                                                .addPreferredGap(
                                                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(
                                                                                                        myBrowse)))))
                                .addContainerGap()));
        layout.setVerticalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(
                        layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(versionLbl)
                                                .addComponent(
                                                        myVaadinVersion,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(widgetsetLbl)
                                                .addComponent(
                                                        myWidgetset,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(myBrowse))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(implVersionLbl)
                                                .addComponent(
                                                        myImplVersion,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(implTitle)
                                                .addComponent(
                                                        myImplTitle,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(implVendorLbl)
                                                .addComponent(
                                                        myImplVendor,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        Short.MAX_VALUE)));

        versionLbl.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(AddOnOptionsPanel.class,
                        "ACSN_VaadinVersion")); // NOI18N
        versionLbl.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(AddOnOptionsPanel.class,
                        "ACSD_VaadinVersion")); // NOI18N
        myVaadinVersion.getAccessibleContext().setAccessibleName(
                versionLbl.getAccessibleContext().getAccessibleName());
        myVaadinVersion.getAccessibleContext().setAccessibleDescription(
                versionLbl.getAccessibleContext().getAccessibleDescription());
        implVersionLbl.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(AddOnOptionsPanel.class,
                        "ACSN_ImplVersion")); // NOI18N
        implVersionLbl.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(AddOnOptionsPanel.class,
                        "ACSD_ImplVersion")); // NOI18N
        myImplVersion.getAccessibleContext().setAccessibleName(
                implVersionLbl.getAccessibleContext().getAccessibleName());
        myImplVersion.getAccessibleContext().setAccessibleDescription(
                implVersionLbl.getAccessibleContext()
                        .getAccessibleDescription());
        implTitle.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(AddOnOptionsPanel.class,
                        "ACSN_ImplTitle")); // NOI18N
        implTitle.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(AddOnOptionsPanel.class,
                        "ACSD_ImplTitle")); // NOI18N
        myImplTitle.getAccessibleContext().setAccessibleName(
                implTitle.getAccessibleContext().getAccessibleName());
        myImplTitle.getAccessibleContext().setAccessibleDescription(
                implTitle.getAccessibleContext().getAccessibleDescription());
        implVendorLbl.getAccessibleContext().setAccessibleName(
                implVendorLbl.getAccessibleContext().getAccessibleName());
        implVendorLbl.getAccessibleContext()
                .setAccessibleDescription(
                        implVendorLbl.getAccessibleContext()
                                .getAccessibleDescription());
        widgetsetLbl.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(AddOnOptionsPanel.class,
                        "ACSN_Widgetset")); // NOI18N
        widgetsetLbl.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(AddOnOptionsPanel.class,
                        "ACSD_Widgetset")); // NOI18N
        myWidgetset.getAccessibleContext().setAccessibleName(
                widgetsetLbl.getAccessibleContext().getAccessibleName());
        myWidgetset.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(AddOnOptionsPanel.class,
                        "ACSD_WidgetsetValue")); // NOI18N
        myBrowse.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(AddOnOptionsPanel.class,
                        "ACSN_Browse")); // NOI18N
        myBrowse.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(AddOnOptionsPanel.class,
                        "ACSD_Browse")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    @NbBundle.Messages({ "selectWidgetset=Select Widgetset",
            "existingWidgetset=Existing Widgetsets:" })
    private void browse( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_browse
        List<Node> nodes = new LinkedList<>();
        collectNodes(JavaUtils.getJavaSourceGroups(myProject), nodes);
        collectNodes(JavaUtils.getResourcesSourceGroups(myProject), nodes);

        Children children = new Children.Array();
        children.add(nodes.toArray(new Node[nodes.size()]));
        final Icon icon = ProjectUtils.getInformation(myProject).getIcon();
        AbstractNode root = new AbstractNode(children) {

            @Override
            public Image getIcon( int type ) {
                return ImageUtilities.icon2Image(icon);
            }

            @Override
            public Image getOpenedIcon( int type ) {
                return getIcon(type);
            }
        };
        root.setDisplayName(ProjectUtils.getInformation(myProject)
                .getDisplayName());
        try {
            Node[] result = NodeOperation.getDefault().select(
                    Bundle.selectWidgetset(), Bundle.existingWidgetset(), root,
                    new NodeAcceptor() {

                        @Override
                        public boolean acceptNodes( Node[] nodes ) {
                            if (nodes.length != 1) {
                                return false;
                            }
                            FileObject fileObject = nodes[0].getLookup()
                                    .lookup(FileObject.class);
                            if (fileObject == null) {
                                return false;
                            }
                            return fileObject.getNameExt().endsWith(
                                    XmlUtils.GWT_XML);
                        }
                    });
            if (result.length == 1) {
                FileObject widgetset = result[0].getLookup().lookup(
                        FileObject.class);
                ClassPath classPath = ClassPath.getClassPath(widgetset,
                        ClassPath.SOURCE);
                String name = classPath.getResourceName(widgetset, '.', true);
                name = name.substring(0,
                        name.length() - XmlUtils.GWT_XML.length());
                myWidgetset.setText(name);
            }
        }
        catch (UserCancelException ignore) {
        }

    }//GEN-LAST:event_browse

    private void collectNodes( SourceGroup[] groups, List<Node> nodes ) {
        for (SourceGroup sourceGroup : groups) {
            FileObject rootFolder = sourceGroup.getRootFolder();
            try {
                FilterNode node = new FilterNode(DataObject.find(rootFolder)
                        .getNodeDelegate());
                nodes.add(node);
            }
            catch (DataObjectNotFoundException ignore) {
            }
        }
    }

    private final class AddonModification implements ModelOperation<POMModel> {

        @Override
        public void performOperation( POMModel model ) {
            Properties properties = model.getProject().getProperties();
            boolean needProperties = properties == null;
            if (needProperties) {
                properties = model.getFactory().createProperties();
            }
            List<POMExtensibilityElement> props = properties
                    .getExtensibilityElements();
            Map<String, POMExtensibilityElement> values = new HashMap<>();
            for (POMExtensibilityElement param : props) {
                values.put(param.getQName().getLocalPart(), param);
            }
            StringWrapper version = (StringWrapper) myVaadinVersion.getModel()
                    .getSelectedItem();
            if (version != null && !version.equals(StringWrapper.WAIT)) {
                POMExtensibilityElement versionElement = values
                        .get(VAADIN_PLUGIN_VERSION);
                if (versionElement == null) {
                    versionElement = POMUtils.createElement(model,
                            VAADIN_PLUGIN_VERSION, version.toString());
                    properties.addExtensibilityElement(versionElement);
                }
                else {
                    String oldValue = POMUtils.getValue(versionElement);
                    if (oldValue == null
                            || !oldValue.equals(version.toString()))
                    {
                        versionElement.setElementText(version.toString());
                    }
                }
            }
            POMUtils.setTextField(IMPLEMENTATION_TITLE, values, myImplTitle,
                    properties);
            POMUtils.setTextField(IMPLEMENTATION_VERSION, values,
                    myImplVersion, properties);
            POMUtils.setTextField(IMPLEMENTATION_VENDOR, values, myImplVendor,
                    properties);
            if (needProperties) {
                model.getProject().setProperties(properties);
            }

            POMExtensibilityElement widgetsets = getWidgetsets(model);
            if (widgetsets == null) {
                createWidgetset(model);
            }
            else {
                setWidgetset(widgetsets);
            }
        }

        private void createWidgetset( POMModel model ) {
            POMExtensibilityElement manifest = getManifestEntries(model);
            if (manifest == null) {
                Plugin plugin = getJarPlugin(model);
                if (plugin != null) {
                    Configuration configuration = plugin.getConfiguration();
                    if (configuration == null) {
                        configuration = model.getFactory()
                                .createConfiguration();
                        createArchive(configuration);
                        plugin.setConfiguration(configuration);
                    }
                    else {
                        createWidgetset(configuration);
                    }
                }
            }
            else {
                createWidgetset(manifest);
            }
        }

        private void createWidgetset( Configuration configuration ) {
            POMExtensibilityElement archive = getArchive(configuration);
            if (archive == null) {
                archive = createArchive(configuration);
                configuration.addExtensibilityElement(archive);
            }
            else {
                POMExtensibilityElement manifestEntries = getManifestEntries(archive);
                if (manifestEntries == null) {
                    manifestEntries = createManifestEntries(archive);
                    archive.addExtensibilityElement(manifestEntries);
                }
                else {
                    createWidgetset(manifestEntries);
                }
            }
        }

        private POMExtensibilityElement createArchive(
                Configuration configuration )
        {
            POMExtensibilityElement archive = POMUtils.createElement(
                    configuration.getModel(), ARCHIVE, null);
            POMExtensibilityElement manifestEntries = createManifestEntries(archive);
            archive.addExtensibilityElement(manifestEntries);
            return archive;
        }

        private POMExtensibilityElement createManifestEntries(
                POMExtensibilityElement archive )
        {
            POMExtensibilityElement manifest = POMUtils.createElement(
                    archive.getModel(), MANIFEST, null);
            createWidgetset(manifest);
            return manifest;
        }

        private void createWidgetset( POMExtensibilityElement manifest ) {
            POMExtensibilityElement widgetset = POMUtils.createElement(manifest
                    .getModel(), VAADIN_WIDGETSETS, myWidgetset.getText()
                    .trim());
            manifest.addExtensibilityElement(widgetset);
        }

        private void setWidgetset( POMExtensibilityElement widgetsets ) {
            String newValue = myWidgetset.getText().trim();
            String oldValue = POMUtils.getValue(widgetsets);
            if (oldValue == null || !oldValue.equals(newValue)) {
                widgetsets.setElementText(newValue);
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel implTitle;

    private javax.swing.JLabel implVendorLbl;

    private javax.swing.JLabel implVersionLbl;

    private javax.swing.JButton myBrowse;

    private javax.swing.JTextField myImplTitle;

    private javax.swing.JTextField myImplVendor;

    private javax.swing.JTextField myImplVersion;

    private javax.swing.JComboBox<StringWrapper> myVaadinVersion;

    private javax.swing.JTextField myWidgetset;

    private javax.swing.JLabel versionLbl;

    private javax.swing.JLabel widgetsetLbl;

    // End of variables declaration//GEN-END:variables

    private Project myProject;
}
