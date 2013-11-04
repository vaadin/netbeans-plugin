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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.xml.namespace.QName;

import org.apache.maven.project.MavenProject;
import org.netbeans.api.progress.ProgressUtils;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.modules.maven.api.customizer.ModelHandle2;
import org.netbeans.modules.maven.model.ModelOperation;
import org.netbeans.modules.maven.model.Utilities;
import org.netbeans.modules.maven.model.pom.Configuration;
import org.netbeans.modules.maven.model.pom.POMExtensibilityElement;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.model.pom.Plugin;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.utils.POMUtils;

/**
 * @author denis
 */
public class AdvancedGwtOptionsPanel extends JPanel {

    private static final String PERSISTENT_UNIT_CACHEDIR = "persistentunitcachedir";//NOI18N

    private static final String WEBAPP_DIRECTORY = "webappDirectory"; //NOI18N

    private static final String COMPILE_REPORT = "compileReport"; // NOI18N

    private static final String OPTIMIZATION_LEVEL = "optimizationLevel";//NOI18N

    private static final String STRICT = "strict"; //NOI18N 

    private static final String PERSISTENT_UNIT_CACHE = "persistentunitcache"; //NOI18N 

    private static final String TREE_LOGGER = "treeLogger"; //NOI18N 

    private static final String VALIDATE_ONLY = "validateOnly"; //NOI18N 

    private static final String SKIP = "skip"; //NOI18N 

    private static final String FORCE = "force"; //NOI18N 

    public AdvancedGwtOptionsPanel( Lookup context ) {
        initComponents();

        setValues(context);

        final AdvancedOptionsModification operation = new AdvancedOptionsModification();
        final ModelHandle2 handle = context.lookup(ModelHandle2.class);
        DocumentListener documentListener = new DocumentListener() {

            @Override
            public void removeUpdate( DocumentEvent e ) {
                update(e);
            }

            @Override
            public void insertUpdate( DocumentEvent e ) {
                update(e);
            }

            @Override
            public void changedUpdate( DocumentEvent e ) {
                update(e);
            }

            private void update( DocumentEvent e ) {
                addModification(handle, operation);
            }
        };

        myWebApp.getDocument().addDocumentListener(documentListener);
        myPersistUnitCacheDir.getDocument().addDocumentListener(
                documentListener);

        ChangeListener changeListener = new ChangeListener() {

            @Override
            public void stateChanged( ChangeEvent e ) {
                myOptimizationValue.setText(String.valueOf(myOptimization
                        .getValue()));
                addModification(handle, operation);
            }
        };

        myOptimization.addChangeListener(changeListener);
        ActionListener actionListener = new ActionListener() {

            @Override
            public void actionPerformed( ActionEvent e ) {
                if (e.getSource().equals(myPersistenceCache)) {
                    myPersistUnitCacheDir.setEditable(myPersistenceCache
                            .isSelected());
                }
                addModification(handle, operation);
            }
        };
        for (int i = 0; i < getComponentCount(); i++) {
            Component child = getComponent(i);
            if (child instanceof JCheckBox) {
                ((JCheckBox) child).addActionListener(actionListener);
            }
        }
    }

    @NbBundle.Messages("readingAdvancedOptions=Retrieving options...")
    private void setValues( final Lookup context ) {
        ProgressUtils.showProgressDialogAndRun(new InitWorker(context),
                Bundle.readingGwtOptions());
    }

    private void addModification( ModelHandle2 handle,
            AdvancedOptionsModification newOperation )
    {
        List<ModelOperation<POMModel>> pomOperations = handle
                .getPOMOperations();
        for (ModelOperation<POMModel> operation : pomOperations) {
            if (operation instanceof AdvancedOptionsModification) {
                handle.removePOMModification(operation);
            }
        }
        handle.addPOMModification(newOperation);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        webAppDirLbl = new javax.swing.JLabel();
        myWebApp = new javax.swing.JTextField();
        persistentunitcachedirLbl = new javax.swing.JLabel();
        myPersistUnitCacheDir = new javax.swing.JTextField();
        optimizationLbl = new javax.swing.JLabel();
        myOptimization = new javax.swing.JSlider();
        myOptimizationValue = new javax.swing.JTextField();
        separator = new javax.swing.JSeparator();
        myPersistenceCache = new javax.swing.JCheckBox();
        myStrict = new javax.swing.JCheckBox();
        myValidate = new javax.swing.JCheckBox();
        myForce = new javax.swing.JCheckBox();
        myCompileReport = new javax.swing.JCheckBox();
        mySkip = new javax.swing.JCheckBox();
        myTreeLogger = new javax.swing.JCheckBox();

        webAppDirLbl.setLabelFor(myWebApp);
        org.openide.awt.Mnemonics.setLocalizedText(webAppDirLbl,
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "LBL_WebApp")); // NOI18N

        myWebApp.setToolTipText(org.openide.util.NbBundle.getMessage(
                AdvancedGwtOptionsPanel.class, "TLTP_WebApp")); // NOI18N

        persistentunitcachedirLbl.setLabelFor(persistentunitcachedirLbl);
        org.openide.awt.Mnemonics.setLocalizedText(persistentunitcachedirLbl,
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "LBL_Persistent")); // NOI18N

        myPersistUnitCacheDir.setToolTipText(org.openide.util.NbBundle
                .getMessage(AdvancedGwtOptionsPanel.class, "TLTP_Persistent")); // NOI18N

        optimizationLbl.setLabelFor(myOptimization);
        org.openide.awt.Mnemonics.setLocalizedText(optimizationLbl,
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "LBL_Optimization")); // NOI18N

        myOptimization.setMaximum(9);
        myOptimization.setMinimum(-1);
        myOptimization.setToolTipText(org.openide.util.NbBundle.getMessage(
                AdvancedGwtOptionsPanel.class, "TLTP_Optimization")); // NOI18N
        myOptimization.setValue(-1);

        myOptimizationValue.setEditable(false);
        myOptimizationValue
                .setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        myOptimizationValue.setText("-1");

        org.openide.awt.Mnemonics.setLocalizedText(myPersistenceCache,
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "LBL_PersistenceCache")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(myStrict,
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "LBL_StrictMode")); // NOI18N
        myStrict.setToolTipText(org.openide.util.NbBundle.getMessage(
                AdvancedGwtOptionsPanel.class, "TLTP_Sctrict")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(myValidate,
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "LBL_Validate")); // NOI18N
        myValidate.setToolTipText(org.openide.util.NbBundle.getMessage(
                AdvancedGwtOptionsPanel.class, "TLTP_Validate")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(myForce,
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "LBL_Force")); // NOI18N
        myForce.setToolTipText(org.openide.util.NbBundle.getMessage(
                AdvancedGwtOptionsPanel.class, "TLTP_Force")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(myCompileReport,
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "LBL_CompileReport")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(mySkip,
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "LBL_Skip")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(myTreeLogger,
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "LBL_TreeLogger")); // NOI18N

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
                                                                        myTreeLogger)
                                                                .addGap(0,
                                                                        0,
                                                                        Short.MAX_VALUE))
                                                .addComponent(separator)
                                                .addGroup(
                                                        layout.createSequentialGroup()
                                                                .addGroup(
                                                                        layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(
                                                                                        persistentunitcachedirLbl)
                                                                                .addComponent(
                                                                                        webAppDirLbl)
                                                                                .addComponent(
                                                                                        optimizationLbl))
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(
                                                                        layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(
                                                                                        myWebApp)
                                                                                .addComponent(
                                                                                        myPersistUnitCacheDir)
                                                                                .addGroup(
                                                                                        layout.createSequentialGroup()
                                                                                                .addComponent(
                                                                                                        myOptimization,
                                                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                        Short.MAX_VALUE)
                                                                                                .addPreferredGap(
                                                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(
                                                                                                        myOptimizationValue,
                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                        30,
                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))))
                                                .addGroup(
                                                        javax.swing.GroupLayout.Alignment.TRAILING,
                                                        layout.createSequentialGroup()
                                                                .addGroup(
                                                                        layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(
                                                                                        myPersistenceCache)
                                                                                .addComponent(
                                                                                        myValidate)
                                                                                .addComponent(
                                                                                        myCompileReport))
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                        108,
                                                                        Short.MAX_VALUE)
                                                                .addGroup(
                                                                        layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(
                                                                                        mySkip)
                                                                                .addComponent(
                                                                                        myForce)
                                                                                .addComponent(
                                                                                        myStrict))
                                                                .addGap(87, 87,
                                                                        87)))
                                .addContainerGap()));
        layout.setVerticalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(
                        layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(webAppDirLbl)
                                                .addComponent(
                                                        myWebApp,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(
                                                        persistentunitcachedirLbl)
                                                .addComponent(
                                                        myPersistUnitCacheDir,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(
                                                        layout.createSequentialGroup()
                                                                .addGap(14, 14,
                                                                        14)
                                                                .addComponent(
                                                                        optimizationLbl))
                                                .addGroup(
                                                        layout.createSequentialGroup()
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addGroup(
                                                                        layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(
                                                                                        myOptimizationValue,
                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(
                                                                                        myOptimization,
                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(separator,
                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                        10,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(
                                                        myPersistenceCache)
                                                .addComponent(myStrict))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(myValidate)
                                                .addComponent(myForce))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(myCompileReport)
                                                .addComponent(mySkip))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(myTreeLogger)
                                .addContainerGap(
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        Short.MAX_VALUE)));

        webAppDirLbl.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "ACSN_WebApp")); // NOI18N
        webAppDirLbl.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "ACSD_WebApp")); // NOI18N
        myWebApp.getAccessibleContext().setAccessibleName(
                webAppDirLbl.getAccessibleContext().getAccessibleName());
        myWebApp.getAccessibleContext().setAccessibleDescription(
                webAppDirLbl.getAccessibleContext().getAccessibleDescription());
        persistentunitcachedirLbl.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "ACSN_Persistent")); // NOI18N
        persistentunitcachedirLbl.getAccessibleContext()
                .setAccessibleDescription(
                        org.openide.util.NbBundle.getMessage(
                                AdvancedGwtOptionsPanel.class,
                                "ACSD_Persistent")); // NOI18N
        myPersistUnitCacheDir.getAccessibleContext().setAccessibleName(
                persistentunitcachedirLbl.getAccessibleContext()
                        .getAccessibleName());
        myPersistUnitCacheDir.getAccessibleContext().setAccessibleDescription(
                persistentunitcachedirLbl.getAccessibleContext()
                        .getAccessibleDescription());
        optimizationLbl.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "ACSN_Optimization")); // NOI18N
        optimizationLbl.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "ACSD_Optimization")); // NOI18N
        myOptimization.getAccessibleContext().setAccessibleName(
                optimizationLbl.getAccessibleContext().getAccessibleName());
        myOptimization.getAccessibleContext().setAccessibleDescription(
                optimizationLbl.getAccessibleContext()
                        .getAccessibleDescription());
        myOptimizationValue.getAccessibleContext().setAccessibleName(
                optimizationLbl.getAccessibleContext().getAccessibleName());
        myOptimizationValue.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle
                        .getMessage(AdvancedGwtOptionsPanel.class,
                                "ACSD_OptimizationLevel")); // NOI18N
        myPersistenceCache.getAccessibleContext()
                .setAccessibleName(
                        org.openide.util.NbBundle.getMessage(
                                AdvancedGwtOptionsPanel.class,
                                "ACSN_PersistenceCache")); // NOI18N
        myPersistenceCache.getAccessibleContext()
                .setAccessibleDescription(
                        org.openide.util.NbBundle.getMessage(
                                AdvancedGwtOptionsPanel.class,
                                "ACSD_PersistenceCache")); // NOI18N
        myStrict.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "ACSN_Strict")); // NOI18N
        myStrict.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "ACSD_Strict")); // NOI18N
        myValidate.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "ACSN_Validate")); // NOI18N
        myValidate.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "ACSD_Validate")); // NOI18N
        myForce.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "ACSN_Force")); // NOI18N
        myForce.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "ACSD_Force")); // NOI18N
        myCompileReport.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "ACSN_CompileReport")); // NOI18N
        myCompileReport.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "ACSD_CompileReport")); // NOI18N
        mySkip.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "ACSN_Skip")); // NOI18N
        mySkip.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "ACSD_Skip")); // NOI18N
        myTreeLogger.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "ACSN_TreeLogger")); // NOI18N
        myTreeLogger.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        AdvancedGwtOptionsPanel.class, "ACSD_TreeLogger")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox myCompileReport;

    private javax.swing.JCheckBox myForce;

    private javax.swing.JSlider myOptimization;

    private javax.swing.JTextField myOptimizationValue;

    private javax.swing.JTextField myPersistUnitCacheDir;

    private javax.swing.JCheckBox myPersistenceCache;

    private javax.swing.JCheckBox mySkip;

    private javax.swing.JCheckBox myStrict;

    private javax.swing.JCheckBox myTreeLogger;

    private javax.swing.JCheckBox myValidate;

    private javax.swing.JTextField myWebApp;

    private javax.swing.JLabel optimizationLbl;

    private javax.swing.JLabel persistentunitcachedirLbl;

    private javax.swing.JSeparator separator;

    private javax.swing.JLabel webAppDirLbl;

    // End of variables declaration//GEN-END:variables

    private final class InitWorker implements Runnable,
            ModelOperation<POMModel>
    {

        private InitWorker( Lookup context ) {
            myLookup = context;
        }

        @Override
        public void performOperation( POMModel model ) {
            Plugin vaadinPlugin = POMUtils.getVaadinPlugin(model);
            if (vaadinPlugin == null) {
                return;
            }

            Configuration configuration = vaadinPlugin.getConfiguration();
            if (configuration == null) {
                return;
            }
            List<POMExtensibilityElement> params = configuration
                    .getExtensibilityElements();
            for (POMExtensibilityElement param : params) {
                QName qName = param.getQName();
                String name = qName.getLocalPart();
                String value = param.getElementText() == null ? "" : param
                        .getElementText().trim();
                switch (name) {
                    case WEBAPP_DIRECTORY:
                        webApp = value;
                        break;
                    case PERSISTENT_UNIT_CACHEDIR:
                        persistentUnitDir = value;
                        break;
                    case OPTIMIZATION_LEVEL:
                        try {
                            optimization = Integer.parseInt(value);
                        }
                        catch (NumberFormatException e) {
                            optimization = -1;
                        }
                        break;
                    case COMPILE_REPORT:
                        compileReport = Boolean.parseBoolean(value);
                        break;
                    case STRICT:
                        strict = Boolean.parseBoolean(value);
                        break;
                    case FORCE:
                        force = Boolean.parseBoolean(value);
                        break;
                    case SKIP:
                        skip = Boolean.parseBoolean(value);
                        break;
                    case VALIDATE_ONLY:
                        validate = Boolean.parseBoolean(value);
                        break;
                    case TREE_LOGGER:
                        treeLogger = Boolean.parseBoolean(value);
                        break;
                    case PERSISTENT_UNIT_CACHE:
                        usePersistentUnitCache = Boolean.parseBoolean(value);
                        break;
                }
            }
        }

        @Override
        public void run() {
            Project project = myLookup.lookup(Project.class);
            NbMavenProject mvnProject = project.getLookup().lookup(
                    NbMavenProject.class);
            MavenProject mavenProject = mvnProject.getMavenProject();
            File file = mavenProject.getFile();
            FileObject pom = FileUtil
                    .toFileObject(FileUtil.normalizeFile(file));
            Utilities.performPOMModelOperations(pom,
                    Collections.singletonList(this));

            myWebApp.setText(webApp);
            myPersistUnitCacheDir.setText(persistentUnitDir);
            myOptimization.setValue(optimization);
            myOptimizationValue.setText(String.valueOf(optimization));

            myCompileReport.setSelected(compileReport);
            myStrict.setSelected(strict);
            myForce.setSelected(force);
            mySkip.setSelected(skip);
            myValidate.setSelected(validate);
            myTreeLogger.setSelected(treeLogger);
            myPersistenceCache.setSelected(usePersistentUnitCache);

            myPersistUnitCacheDir.setEditable(myPersistenceCache.isSelected());
        }

        private final Lookup myLookup;

        private String webApp;

        private String persistentUnitDir;

        private int optimization = -1;

        private boolean compileReport;

        private boolean strict;

        private boolean force;

        private boolean skip;

        private boolean validate;

        private boolean treeLogger;

        private boolean usePersistentUnitCache;
    }

    private final class AdvancedOptionsModification implements
            ModelOperation<POMModel>
    {

        @Override
        public void performOperation( POMModel model ) {
            Plugin plugin = POMUtils.getVaadinPlugin(model);
            if (plugin == null) {
                /*
                 * This shouldn't happen : customizer shouldn't be available if
                 * there is no vaadin plugin
                 */
                return;
            }
            Configuration configuration = plugin.getConfiguration();
            if (configuration == null) {
                createConfiguration(plugin);
                return;
            }
            setOptions(configuration);
        }

        private void setOptions( Configuration configuration ) {
            POMModel model = configuration.getModel();
            List<POMExtensibilityElement> params = configuration
                    .getExtensibilityElements();
            Map<String, POMExtensibilityElement> values = new HashMap<>();
            for (POMExtensibilityElement param : params) {
                values.put(param.getQName().getLocalPart(), param);
            }

            POMUtils.setTextField(WEBAPP_DIRECTORY, values, myWebApp,
                    configuration);

            if (myPersistenceCache.isSelected()) {
                POMUtils.setTextField(PERSISTENT_UNIT_CACHEDIR, values,
                        myPersistUnitCacheDir, configuration);
            }

            POMExtensibilityElement optimization = values
                    .get(OPTIMIZATION_LEVEL);
            if (optimization == null) {
                if (myOptimization.getValue() > -1) {
                    configuration.addExtensibilityElement(POMUtils
                            .createElement(model, OPTIMIZATION_LEVEL,
                                    myOptimizationValue.getText()));
                }
            }
            else {
                if (myOptimization.getValue() > -1
                        && !POMUtils.getValue(optimization).equals(
                                myOptimizationValue.getText()))
                {
                    optimization.setElementText(myOptimizationValue.getText());
                }
                else {
                    configuration.removeExtensibilityElement(optimization);
                }
            }

            POMUtils.setBooleanVaue(COMPILE_REPORT, values, myCompileReport,
                    configuration);
            POMUtils.setBooleanVaue(STRICT, values, myStrict, configuration);
            POMUtils.setBooleanVaue(FORCE, values, myForce, configuration);
            POMUtils.setBooleanVaue(SKIP, values, mySkip, configuration);
            POMUtils.setBooleanVaue(VALIDATE_ONLY, values, myValidate,
                    configuration);
            POMUtils.setBooleanVaue(TREE_LOGGER, values, myTreeLogger,
                    configuration);
            POMUtils.setBooleanVaue(PERSISTENT_UNIT_CACHE, values,
                    myPersistenceCache, configuration);
        }

        private void createConfiguration( Plugin plugin ) {
            POMModel model = plugin.getModel();
            Configuration configuration = model.getFactory()
                    .createConfiguration();
            setOptions(configuration);
            plugin.setConfiguration(configuration);
        }

    }
}
