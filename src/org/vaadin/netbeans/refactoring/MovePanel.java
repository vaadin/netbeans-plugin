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
package org.vaadin.netbeans.refactoring;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.UIResource;

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.code.generator.JavaUtils;

/**
 * @author denis
 */
class MovePanel extends JPanel implements CustomRefactoringPanel {

    MovePanel( String name, Project project, FileObject pkg,
            ChangeListener listener, boolean copy, String panelName )
    {
        setName(panelName);
        initComponents();

        myNewName.setVisible(copy);
        labelNewName.setVisible(copy);
        if (copy) {
            myNewName.setText(name);
        }

        myLocation.setRenderer(new GroupCellRenderer());
        myPackage.setRenderer(PackageView.listRenderer());
        myProject.setRenderer(new ProjectCellRenderer());

        myCurrentProject = project;
        myListener = listener;
        myTargetPackage = pkg;
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void initialize() {
        if (isInitialized) {
            return;
        }
        isInitialized = true;
        doInitialize(false);
    }

    String getTargetName() {
        return myNewName.getText().trim();
    }

    SourceGroup getTargetSourceGroup() {
        return (SourceGroup) myLocation.getSelectedItem();
    }

    String getTargetPackage() {
        return ((JTextField) myPackage.getEditor().getEditorComponent())
                .getText();
    }

    private void doInitialize( final boolean selectNameOnly ) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                selectName();

                if (!selectNameOnly) {
                    setValues();
                    attachListeners();
                }
            }
        });
    }

    private void selectName() {
        if (myNewName.isVisible()) {
            if (SwingUtilities.getWindowAncestor(myNewName) == null) {
                doInitialize(true);
            }
            else {
                myNewName.requestFocusInWindow();
                myNewName.selectAll();
            }
        }
    }

    private void setValues() {
        Project openProjects[] = OpenProjects.getDefault().getOpenProjects();
        List<Project> projects = new ArrayList<>(openProjects.length);
        for (Project project : openProjects) {
            VaadinSupport support = project.getLookup().lookup(
                    VaadinSupport.class);
            if (support != null && support.isEnabled()) {
                projects.add(project);
            }
        }

        Project[] vaadinProjects = projects
                .toArray(new Project[projects.size()]);

        Arrays.sort(vaadinProjects, new ProjectNameComparator());
        DefaultComboBoxModel<Project> projectsModel = new DefaultComboBoxModel<>(
                vaadinProjects);
        myProject.setModel(projectsModel);
        myProject.setSelectedItem(myCurrentProject);

        setSourceGroups();
        setTargetPackage();
    }

    private void attachListeners() {
        ItemListener listener = new ItemListener() {

            @Override
            public void itemStateChanged( ItemEvent e ) {
                Object source = e.getSource();
                if (myProject.equals(source)) {
                    setSourceGroups();
                    setPackages();
                }
                else if (myLocation.equals(source)) {
                    setPackages();
                }

                myListener.stateChanged(null);
            }
        };
        myProject.addItemListener(listener);
        myLocation.addItemListener(listener);

        DocumentListener documentListener = new DocumentListener() {

            @Override
            public void removeUpdate( DocumentEvent e ) {
                fireEvent();
            }

            @Override
            public void insertUpdate( DocumentEvent e ) {
                fireEvent();
            }

            @Override
            public void changedUpdate( DocumentEvent e ) {
                fireEvent();
            }

            private void fireEvent() {
                myListener.stateChanged(null);
            }
        };
        ((JTextField) myPackage.getEditor().getEditorComponent()).getDocument()
                .addDocumentListener(documentListener);
        if (myNewName.isVisible()) {
            myNewName.getDocument().addDocumentListener(documentListener);
        }
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
        Project project = (Project) myProject.getSelectedItem();
        SourceGroup[] javaSourceGroups = JavaUtils.getJavaSourceGroups(project);
        SourceGroup[] resourcesSourceGroups = JavaUtils
                .getResourcesSourceGroups(project);
        SourceGroup[] groups = new SourceGroup[javaSourceGroups.length
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

    private void setTargetPackage() {
        if (myTargetPackage != null) {
            for (SourceGroup group : JavaUtils
                    .getJavaSourceGroups(myCurrentProject))
            {
                if (doSetTargetPackage(group)) {
                    return;
                }
            }
            for (SourceGroup group : JavaUtils
                    .getResourcesSourceGroups(myCurrentProject))
            {
                if (doSetTargetPackage(group)) {
                    return;
                }
            }
        }
    }

    private boolean doSetTargetPackage( SourceGroup group ) {
        FileObject root = group.getRootFolder();
        if (root.equals(myTargetPackage)
                || FileUtil.isParentOf(root, myTargetPackage))
        {
            ComboBoxModel<?> model = myLocation.getModel();
            for (int i = 0; i < model.getSize(); i++) {
                SourceGroup sourceGroup = (SourceGroup) model.getElementAt(i);
                if (sourceGroup.getRootFolder().equals(root)) {
                    myLocation.setSelectedItem(sourceGroup);
                }
            }
            setPackages();
            String path = FileUtil.getRelativePath(root, myTargetPackage);
            myPackage.setSelectedItem(path.replace('/', '.'));
            return true;
        }
        return false;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        labelProject = new javax.swing.JLabel();
        myProject = new javax.swing.JComboBox();
        labelLocation = new javax.swing.JLabel();
        myLocation = new javax.swing.JComboBox();
        labelPackage = new javax.swing.JLabel();
        myPackage = new javax.swing.JComboBox();
        labelNewName = new javax.swing.JLabel();
        myNewName = new javax.swing.JTextField();

        labelProject.setLabelFor(myProject);
        org.openide.awt.Mnemonics.setLocalizedText(labelProject,
                org.openide.util.NbBundle.getMessage(MovePanel.class,
                        "LBL_Project")); // NOI18N

        myProject.setModel(new DefaultComboBoxModel<Project>());

        labelLocation.setLabelFor(myLocation);
        org.openide.awt.Mnemonics.setLocalizedText(labelLocation,
                org.openide.util.NbBundle.getMessage(MovePanel.class,
                        "LBL_Location")); // NOI18N

        myLocation.setModel(new DefaultComboBoxModel<SourceGroup>());

        labelPackage.setLabelFor(myPackage);
        org.openide.awt.Mnemonics.setLocalizedText(labelPackage,
                org.openide.util.NbBundle.getMessage(MovePanel.class,
                        "LBL_Package")); // NOI18N

        myPackage.setEditable(true);
        myPackage.setModel(new DefaultComboBoxModel());

        labelNewName.setLabelFor(myNewName);
        org.openide.awt.Mnemonics.setLocalizedText(labelNewName,
                org.openide.util.NbBundle.getMessage(MovePanel.class,
                        "LBL_NewName")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(
                        layout.createSequentialGroup()
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(
                                                        labelLocation,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        52,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGroup(
                                                        layout.createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addComponent(
                                                                        labelNewName,
                                                                        javax.swing.GroupLayout.Alignment.TRAILING,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                        81,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addComponent(
                                                                        labelProject,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                        45,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
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
                                                .addComponent(myPackage, 0,
                                                        274, Short.MAX_VALUE)
                                                .addComponent(myNewName)
                                                .addComponent(
                                                        myLocation,
                                                        0,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        Short.MAX_VALUE)
                                                .addComponent(
                                                        myProject,
                                                        0,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        Short.MAX_VALUE))));
        layout.setVerticalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(
                        layout.createSequentialGroup()
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(
                                                        myNewName,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(labelNewName))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(labelProject)
                                                .addComponent(
                                                        myProject,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(labelLocation)
                                                .addComponent(
                                                        myLocation,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(10, 10, 10)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(
                                                        myPackage,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(labelPackage))));

        labelProject.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(MovePanel.class,
                        "ACSN_Project")); // NOI18N
        labelProject.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(MovePanel.class,
                        "ACSD_Project")); // NOI18N
        myProject.getAccessibleContext().setAccessibleName(
                labelProject.getAccessibleContext().getAccessibleName());
        myProject.getAccessibleContext().setAccessibleDescription(
                labelProject.getAccessibleContext().getAccessibleDescription());
        labelLocation.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(MovePanel.class,
                        "ACSN_Location")); // NOI18N
        labelLocation.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(MovePanel.class,
                        "ACSD_Location")); // NOI18N
        myLocation.getAccessibleContext().setAccessibleName(
                labelLocation.getAccessibleContext().getAccessibleName());
        myLocation.getAccessibleContext()
                .setAccessibleDescription(
                        labelLocation.getAccessibleContext()
                                .getAccessibleDescription());
        labelPackage.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(MovePanel.class,
                        "ACSN_Package")); // NOI18N
        labelPackage.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(MovePanel.class,
                        "ACSD_Package")); // NOI18N
        myPackage.getAccessibleContext().setAccessibleName(
                labelPackage.getAccessibleContext().getAccessibleName());
        myPackage.getAccessibleContext().setAccessibleDescription(
                labelPackage.getAccessibleContext().getAccessibleDescription());
        labelNewName.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(MovePanel.class,
                        "ACSN_NewName")); // NOI18N
        labelNewName.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(MovePanel.class,
                        "ACSD_NewName")); // NOI18N
        myNewName.getAccessibleContext().setAccessibleName(
                labelNewName.getAccessibleContext().getAccessibleName());
        myNewName.getAccessibleContext().setAccessibleDescription(
                labelNewName.getAccessibleContext().getAccessibleDescription());
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel labelLocation;

    private javax.swing.JLabel labelNewName;

    private javax.swing.JLabel labelPackage;

    private javax.swing.JLabel labelProject;

    private javax.swing.JComboBox myLocation;

    private javax.swing.JTextField myNewName;

    private javax.swing.JComboBox myPackage;

    private javax.swing.JComboBox myProject;

    // End of variables declaration//GEN-END:variables

    private final Project myCurrentProject;

    private final ChangeListener myListener;

    private final FileObject myTargetPackage;

    private boolean isInitialized;

    static class ProjectNameComparator implements Comparator<Project> {

        private static final Comparator<Object> COLLATOR = Collator
                .getInstance();

        @Override
        public int compare( Project p1, Project p2 ) {
            return COLLATOR.compare(ProjectUtils.getInformation(p1)
                    .getDisplayName(), ProjectUtils.getInformation(p2)
                    .getDisplayName());
        }
    }

    static class GroupCellRenderer extends JLabel implements ListCellRenderer,
            UIResource
    {

        GroupCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent( JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus )
        {
            if (!(value instanceof SourceGroup)) {
                return this;
            }
            // #89393: GTK needs name to render cell renderer "natively"
            setName("ComboBox.listRenderer"); // NOI18N

            setText(((SourceGroup) value).getDisplayName());
            setIcon(((SourceGroup) value).getIcon(false));

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            return this;
        }
    }

    static class ProjectCellRenderer extends JLabel implements
            ListCellRenderer, UIResource
    {

        public ProjectCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent( JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus )
        {
            if (!(value instanceof Project)) {
                return this;
            }
            // #89393: GTK needs name to render cell renderer "natively"
            setName("ComboBox.listRenderer"); // NOI18N

            ProjectInformation pi = ProjectUtils
                    .getInformation((Project) value);
            setText(pi.getDisplayName());
            setIcon(pi.getIcon());

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            return this;
        }
    }
}
