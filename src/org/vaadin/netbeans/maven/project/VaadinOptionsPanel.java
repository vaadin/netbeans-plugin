/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package org.vaadin.netbeans.maven.project;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.xml.namespace.QName;

import org.apache.maven.project.MavenProject;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.modules.maven.api.customizer.ModelHandle2;
import org.netbeans.modules.maven.model.ModelOperation;
import org.netbeans.modules.maven.model.Utilities;
import org.netbeans.modules.maven.model.pom.POMExtensibilityElement;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.model.pom.POMQName;
import org.netbeans.modules.maven.model.pom.Properties;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.maven.ui.wizard.VaadinProjectWizardIterator;

/**
 * @author denis
 */
public class VaadinOptionsPanel extends JPanel {

    private static final String VAADIN_PLUGIN_VERSION = "vaadin.version"; // NOI18N

    private static final Logger LOG = Logger.getLogger(VaadinOptionsPanel.class
            .getName());

    public VaadinOptionsPanel( Lookup context ) {
        initComponents();

        final ModelHandle2 handle = context.lookup(ModelHandle2.class);

        DefaultComboBoxModel<StringWrapper> model = new DefaultComboBoxModel<>();
        model.addElement(StringWrapper.WAIT);
        myVaadinVersion.setModel(model);

        myVaadinVersion.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged( ItemEvent e ) {
                final StringWrapper version = (StringWrapper) myVaadinVersion
                        .getModel().getSelectedItem();
                if (version.equals(StringWrapper.WAIT)) {
                    return;
                }
                ModelOperation<POMModel> operation = new VersionModificationOperation(
                        version);
                List<ModelOperation<POMModel>> pomOperations = handle
                        .getPOMOperations();
                for (ModelOperation<POMModel> modelOperation : pomOperations) {
                    if (modelOperation instanceof VersionModificationOperation)
                    {
                        handle.removePOMModification(modelOperation);
                    }
                }
                handle.addPOMModification(operation);
            }

        });

        setCurrentVersion(context);

        retrieveVersions();
    }

    private void retrieveVersions() {
        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>()
        {

            @Override
            protected List<String> doInBackground() throws Exception {
                return VaadinProjectWizardIterator
                        .getVaadinVersoins(VaadinProjectWizardIterator.APPLICATION_MIN_VERSION);
            }

            @Override
            protected void done() {
                try {
                    List<String> versions = get();
                    DefaultComboBoxModel<StringWrapper> model = (DefaultComboBoxModel<StringWrapper>) myVaadinVersion
                            .getModel();
                    model.removeElement(StringWrapper.WAIT);
                    if (model.getSize() == 1) {
                        versions.remove(model.getElementAt(0).toString());
                    }
                    for (String version : versions) {
                        model.addElement(new StringWrapper(version));
                    }
                }
                catch (InterruptedException | ExecutionException e) {
                    LOG.log(Level.INFO, null, e);
                }
            }

        };

        worker.execute();
    }

    private void setCurrentVersion( final Lookup context ) {
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                return doSetCurrentVersion(context);
            }

            @Override
            protected void done() {
                try {
                    String version = get();
                    if (version == null) {
                        return;
                    }
                    DefaultComboBoxModel<StringWrapper> model = (DefaultComboBoxModel<StringWrapper>) myVaadinVersion
                            .getModel();
                    StringWrapper selectedVersion = null;
                    if (model.getSize() == 1
                            && model.getElementAt(0).equals(StringWrapper.WAIT))
                    {
                        selectedVersion = new StringWrapper(version);
                        model.insertElementAt(selectedVersion, 0);
                    }
                    else {
                        for (int i = 0; i < model.getSize(); i++) {
                            StringWrapper element = model.getElementAt(i);
                            if (element.toString().equals(version)) {
                                selectedVersion = element;
                                break;
                            }
                        }
                        if (selectedVersion == null) {
                            selectedVersion = new StringWrapper(version);
                            model.insertElementAt(selectedVersion, 0);
                        }
                    }
                    model.setSelectedItem(selectedVersion);
                }
                catch (InterruptedException | ExecutionException e) {
                    LOG.log(Level.INFO, null, e);
                }
            }
        };
        worker.execute();
    }

    private String doSetCurrentVersion( Lookup context ) {
        Project project = context.lookup(Project.class);
        NbMavenProject mvnProject = project.getLookup().lookup(
                NbMavenProject.class);
        MavenProject mavenProject = mvnProject.getMavenProject();
        File file = mavenProject.getFile();
        FileObject pom = FileUtil.toFileObject(FileUtil.normalizeFile(file));

        final String[] version = new String[1];
        ModelOperation<POMModel> operation = new ModelOperation<POMModel>() {

            @Override
            public void performOperation( POMModel model ) {
                Properties properties = model.getProject().getProperties();
                if (properties != null) {
                    version[0] = properties.getProperty(VAADIN_PLUGIN_VERSION);
                }
            }
        };
        Utilities.performPOMModelOperations(pom,
                Collections.singletonList(operation));

        return version[0];
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
        myVaadinVersion = new javax.swing.JComboBox();

        versionLbl.setLabelFor(myVaadinVersion);
        org.openide.awt.Mnemonics.setLocalizedText(versionLbl,
                org.openide.util.NbBundle.getMessage(VaadinOptionsPanel.class,
                        "LBL_VaadinVersion")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(
                        layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(versionLbl)
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(myVaadinVersion, 0, 360,
                                        Short.MAX_VALUE).addContainerGap()));
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
                                .addContainerGap(
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        Short.MAX_VALUE)));

        versionLbl.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(VaadinOptionsPanel.class,
                        "ACSN_VaadinVersion")); // NOI18N
        versionLbl.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(VaadinOptionsPanel.class,
                        "ACSD_VaadinVersion")); // NOI18N
        myVaadinVersion.getAccessibleContext().setAccessibleName(
                versionLbl.getAccessibleContext().getAccessibleName());
        myVaadinVersion.getAccessibleContext().setAccessibleDescription(
                versionLbl.getAccessibleContext().getAccessibleDescription());
    }// </editor-fold>//GEN-END:initComponents
     // Variables declaration - do not modify//GEN-BEGIN:variables

    private javax.swing.JComboBox<StringWrapper> myVaadinVersion;

    private javax.swing.JLabel versionLbl;

    // End of variables declaration//GEN-END:variables

    private final class VersionModificationOperation implements
            ModelOperation<POMModel>
    {

        private VersionModificationOperation( StringWrapper version ) {
            this.myVersion = version;
        }

        @Override
        public void performOperation( POMModel model ) {
            Properties properties = model.getProject().getProperties();
            if (properties == null) {
                model.getProject().setProperties(
                        createProperties(model, myVersion.toString()));
            }
            else {
                List<POMExtensibilityElement> props = properties
                        .getExtensibilityElements();
                boolean versionSet = false;
                for (POMExtensibilityElement prop : props) {
                    if (prop.getQName().getLocalPart()
                            .equals(VAADIN_PLUGIN_VERSION))
                    {
                        String newVersion = myVersion.toString();
                        if (newVersion.equals(prop.getElementText())) {
                            return;
                        }
                        prop.setElementText(newVersion);
                        versionSet = true;
                        break;
                    }
                }
                if (!versionSet) {
                    properties.addExtensibilityElement(createVersion(model,
                            myVersion.toString()));
                }
            }
        }

        private Properties createProperties( POMModel model, String version ) {
            Properties properties = model.getFactory().createProperties();
            properties.addExtensibilityElement(createVersion(model, version));
            return properties;
        }

        private POMExtensibilityElement createVersion( POMModel model,
                String value )
        {
            QName qname = POMQName.createQName(VAADIN_PLUGIN_VERSION, model
                    .getPOMQNames().isNSAware());
            POMExtensibilityElement version = model.getFactory()
                    .createPOMExtensibilityElement(qname);
            version.setElementText(value);
            return version;
        }

        private final StringWrapper myVersion;
    }

    private static class StringWrapper {

        @NbBundle.Messages("waitVersion=Loading Availbale Versions...")
        static final StringWrapper WAIT = new StringWrapper(
                Bundle.waitVersion());

        public StringWrapper( String orignal ) {
            myOriginal = orignal;
        }

        @Override
        public String toString() {
            return myOriginal;
        }

        private final String myOriginal;
    }
}
