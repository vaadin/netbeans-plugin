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

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.netbeans.api.project.Project;
import org.openide.util.Lookup;
import org.vaadin.netbeans.VaadinSupport;

/**
 * @author denis
 */
abstract class VersionPanel extends JPanel {

    private static final Logger LOG = Logger.getLogger(VaadinOptionsPanel.class
            .getName());

    protected void initVersions( Lookup context, JComboBox<String> comboBox ) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        comboBox.setModel(model);

        retrieveVersions(comboBox);

        setCurrentVersion(context, comboBox);
    }

    protected String getCurrentVersion( Lookup context ) {
        return context.lookup(Project.class).getLookup()
                .lookup(VaadinSupport.class).getVaadinVersion();
    }

    private void setCurrentVersion( final Lookup context,
            final JComboBox<String> comboBox )
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
                    DefaultComboBoxModel<String> model =
                            (DefaultComboBoxModel<String>) comboBox.getModel();
                    boolean modelHasCurrent = false;
                    for (int i = 0; i < model.getSize(); i++) {
                        String element = model.getElementAt(i);
                        if (version.equals(element)) {
                            modelHasCurrent = true;
                            break;
                        }
                    }
                    if (!modelHasCurrent) {
                        model.insertElementAt(version, 0);
                    }
                    model.setSelectedItem(version);
                }
                catch (InterruptedException | ExecutionException e) {
                    LOG.log(Level.INFO, null, e);
                }
            }
        };
        worker.execute();
    }

    private void retrieveVersions( final JComboBox<String> comboBox ) {
        DefaultComboBoxModel<String> model =
                (DefaultComboBoxModel<String>) comboBox.getModel();
        for (String version : VaadinVersions.getInstance().getVersions()) {
            model.addElement(version);
        }
    }

}
