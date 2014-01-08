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

package org.vaadin.netbeans.editor.analyzer.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.vaadin.netbeans.common.ui.GroupCellRenderer;
import org.vaadin.netbeans.utils.JavaUtils;

/**
 * @author denis
 */
public class NewGwtModulePanel extends JPanel {

    /**
     * Creates new form NewGwtModulePanel
     */
    public NewGwtModulePanel( String name, Project project, final JButton button )
    {
        myProject = project;
        initComponents();

        myLocation.setRenderer(new GroupCellRenderer());
        myPackage.setRenderer(PackageView.listRenderer());

        myLocation.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed( ActionEvent e ) {
                setPackages();
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
                String pkg = getTargetPackage();
                String name = getGwtName();
                boolean hasName = name != null && name.length() > 0;
                boolean hasPackage = pkg != null && pkg.trim().length() > 0;
                button.setEnabled(hasName && hasPackage);
            }
        };

        setSourceGroups();

        ((JTextField) myPackage.getEditor().getEditorComponent()).getDocument()
                .addDocumentListener(listener);
        myName.getDocument().addDocumentListener(listener);

        myName.setText(name);
        selectText();
    }

    public String getGwtName() {
        return myName.getText().trim();
    }

    public SourceGroup getTargetSourceGroup() {
        return (SourceGroup) myLocation.getSelectedItem();
    }

    public String getTargetPackage() {
        return ((JTextField) myPackage.getEditor().getEditorComponent())
                .getText();
    }

    private void selectText() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (SwingUtilities.getWindowAncestor(NewGwtModulePanel.this) == null)
                {
                    selectText();
                }
                else {
                    myName.requestFocusInWindow();
                    myName.selectAll();
                }
            }
        });
    }

    private void setPackages() {
        SourceGroup group = (SourceGroup) myLocation.getSelectedItem();
        if (group == null) {
            myPackage.setModel(new DefaultComboBoxModel());
        }
        else {
            ComboBoxModel<?> model = PackageView.createListView(group);
            myPackage.setModel(model);
            if (model.getSize() > 0) {
                myPackage.setSelectedIndex(0);
            }
        }
    }

    private void setSourceGroups() {
        SourceGroup[] javaSourceGroups =
                JavaUtils.getJavaSourceGroups(myProject);
        SourceGroup[] resourcesSourceGroups =
                JavaUtils.getResourcesSourceGroups(myProject);
        SourceGroup[] groups =
                new SourceGroup[javaSourceGroups.length
                        + resourcesSourceGroups.length];
        System.arraycopy(javaSourceGroups, 0, groups, 0,
                javaSourceGroups.length);
        System.arraycopy(resourcesSourceGroups, 0, groups,
                javaSourceGroups.length, resourcesSourceGroups.length);
        myLocation.setModel(new DefaultComboBoxModel<>(groups));
        if (groups.length > 0) {
            myLocation.setSelectedIndex(0);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        myPackage = new javax.swing.JComboBox();
        labelPackage = new javax.swing.JLabel();
        myLocation = new javax.swing.JComboBox();
        labelLocation = new javax.swing.JLabel();
        nameLbl = new javax.swing.JLabel();
        myName = new javax.swing.JTextField();

        myPackage.setEditable(true);
        myPackage.setModel(new DefaultComboBoxModel<>());

        labelPackage.setLabelFor(myPackage);
        org.openide.awt.Mnemonics.setLocalizedText(labelPackage,
                org.openide.util.NbBundle.getMessage(NewGwtModulePanel.class,
                        "LBL_GwtModulePackage")); // NOI18N

        myLocation.setModel(new DefaultComboBoxModel<SourceGroup>());

        labelLocation.setLabelFor(myLocation);
        org.openide.awt.Mnemonics.setLocalizedText(labelLocation,
                org.openide.util.NbBundle.getMessage(NewGwtModulePanel.class,
                        "LBL_GwtModuleLocation")); // NOI18N

        nameLbl.setLabelFor(myName);
        org.openide.awt.Mnemonics.setLocalizedText(nameLbl,
                org.openide.util.NbBundle.getMessage(NewGwtModulePanel.class,
                        "LBL_GwtModuleName")); // NOI18N

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
                                                .addComponent(nameLbl)
                                                .addComponent(
                                                        labelLocation,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        52,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(
                                                        labelPackage,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        71,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(myName)
                                                .addComponent(
                                                        myLocation,
                                                        javax.swing.GroupLayout.Alignment.TRAILING,
                                                        0,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        Short.MAX_VALUE)
                                                .addComponent(
                                                        myPackage,
                                                        javax.swing.GroupLayout.Alignment.TRAILING,
                                                        0, 343, Short.MAX_VALUE))
                                .addContainerGap()));
        layout.setVerticalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(
                        layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(nameLbl)
                                                .addComponent(
                                                        myName,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(labelLocation)
                                                .addComponent(
                                                        myLocation,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(labelPackage)
                                                .addComponent(
                                                        myPackage,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        Short.MAX_VALUE)));

        labelPackage.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(NewGwtModulePanel.class,
                        "ACSN_GwtModulePackage")); // NOI18N
        labelPackage.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(NewGwtModulePanel.class,
                        "ACSD_GwtModulePackage")); // NOI18N
        labelLocation.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(NewGwtModulePanel.class,
                        "ACSN_GwtModuleLocation")); // NOI18N
        labelLocation.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(NewGwtModulePanel.class,
                        "ACSD_GwtModuleLocation")); // NOI18N
        nameLbl.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(NewGwtModulePanel.class,
                        "ACSN_GwtModuleName")); // NOI18N
        nameLbl.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(NewGwtModulePanel.class,
                        "ACSD_GwtModuleName")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel labelLocation;

    private javax.swing.JLabel labelPackage;

    private javax.swing.JComboBox myLocation;

    private javax.swing.JTextField myName;

    private javax.swing.JComboBox myPackage;

    private javax.swing.JLabel nameLbl;

    // End of variables declaration//GEN-END:variables

    private final Project myProject;
}
