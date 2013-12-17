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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.xml.namespace.QName;

import org.netbeans.modules.maven.api.customizer.ModelHandle2;
import org.netbeans.modules.maven.model.ModelOperation;
import org.netbeans.modules.maven.model.pom.POMExtensibilityElement;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.model.pom.POMQName;
import org.netbeans.modules.maven.model.pom.Properties;
import org.openide.util.Lookup;

/**
 * @author denis
 */
public class VaadinOptionsPanel extends VersionPanel {

    public VaadinOptionsPanel( Lookup context ) {
        initComponents();

        initVersions(context, myVaadinVersion);

        final ModelHandle2 handle = context.lookup(ModelHandle2.class);
        myVaadinVersion.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged( ItemEvent e ) {
                final String version =
                        (String) myVaadinVersion.getModel().getSelectedItem();
                if (version == null) {
                    return;
                }
                ModelOperation<POMModel> operation =
                        new VersionModificationOperation(version);
                List<ModelOperation<POMModel>> pomOperations =
                        handle.getPOMOperations();
                for (ModelOperation<POMModel> modelOperation : pomOperations) {
                    if (modelOperation instanceof VersionModificationOperation)
                    {
                        handle.removePOMModification(modelOperation);
                    }
                }
                handle.addPOMModification(operation);
            }

        });

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
        myVaadinVersion = new javax.swing.JComboBox<String>();

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

    private javax.swing.JComboBox<String> myVaadinVersion;

    private javax.swing.JLabel versionLbl;

    // End of variables declaration//GEN-END:variables

    private final class VersionModificationOperation implements
            ModelOperation<POMModel>
    {

        private VersionModificationOperation( String version ) {
            this.myVersion = version;
        }

        @Override
        public void performOperation( POMModel model ) {
            Properties properties = model.getProject().getProperties();
            if (properties == null) {
                model.getProject().setProperties(
                        createProperties(model, myVersion));
            }
            else {
                List<POMExtensibilityElement> props =
                        properties.getExtensibilityElements();
                boolean versionSet = false;
                for (POMExtensibilityElement prop : props) {
                    if (prop.getQName().getLocalPart()
                            .equals(VAADIN_PLUGIN_VERSION))
                    {
                        String newVersion = myVersion;
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
                            myVersion));
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
            QName qname =
                    POMQName.createQName(VAADIN_PLUGIN_VERSION, model
                            .getPOMQNames().isNSAware());
            POMExtensibilityElement version =
                    model.getFactory().createPOMExtensibilityElement(qname);
            version.setElementText(value);
            return version;
        }

        private final String myVersion;
    }

}
