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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.apache.maven.project.MavenProject;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.modules.maven.model.ModelOperation;
import org.netbeans.modules.maven.model.Utilities;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.model.pom.Properties;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 * @author denis
 */
abstract class VersionPanel extends JPanel {

    private static final Logger LOG = Logger.getLogger(VaadinOptionsPanel.class
            .getName());

    protected static final String VAADIN_PLUGIN_VERSION = "vaadin.version"; // NOI18N

    protected void initVersions( Lookup context,
            JComboBox<StringWrapper> comboBox )
    {
        DefaultComboBoxModel<StringWrapper> model = new DefaultComboBoxModel<>();
        model.addElement(StringWrapper.WAIT);
        comboBox.setModel(model);

        setCurrentVersion(context, comboBox);

        retrieveVersions(comboBox);
    }

    protected String getCurrentVersion( Lookup context ) {
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

    private void setCurrentVersion( final Lookup context,
            final JComboBox<StringWrapper> comboBox )
    {
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                return getCurrentVersion(context);
            }

            @Override
            protected void done() {
                try {
                    String version = get();
                    if (version == null) {
                        return;
                    }
                    DefaultComboBoxModel<StringWrapper> model = (DefaultComboBoxModel<StringWrapper>) comboBox
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

    private void retrieveVersions( final JComboBox<StringWrapper> comboBox ) {
        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>()
        {

            @Override
            protected List<String> doInBackground() throws Exception {
                return VaadinVersions.getInstance().getVersions();
            }

            @Override
            protected void done() {
                try {
                    List<String> versions = get();
                    DefaultComboBoxModel<StringWrapper> model = (DefaultComboBoxModel<StringWrapper>) comboBox
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

    protected static class StringWrapper {

        @NbBundle.Messages("waitVersion=Loading Available Versions...")
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
