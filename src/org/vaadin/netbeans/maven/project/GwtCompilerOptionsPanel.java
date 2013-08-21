/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package org.vaadin.netbeans.maven.project;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSpinner.DefaultEditor;
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

/**
 * @author denis
 */
public class GwtCompilerOptionsPanel extends JPanel {

    private static final String LOCAL_WORKERS = "localWorkers";//NOI18N

    private static final String PERSISTENT_UNIT_CACHEDIR = "persistentunitcachedir";//NOI18N

    private static final String WEBAPP_DIRECTORY = "webappDirectory"; //NOI18N

    private static final String JVM_ARGS = "extraJvmArgs"; // NOI18N

    private static final String COMPILE_REPORT = "compileReport"; // NOI18N

    private static final String OPTIMIZATION_LEVEL = "optimizationLevel";//NOI18N

    private static final String LOG_LEVEL = "logLevel"; //NOI18N

    private static final String STYLE = "style"; //NOI18N

    private static final String STRICT = "strict"; //NOI18N 

    private static final String PERSISTENT_UNIT_CACHE = "persistentunitcache"; //NOI18N 

    private static final String TREE_LOGGER = "treeLogger"; //NOI18N 

    private static final String VALIDATE_ONLY = "validateOnly"; //NOI18N 

    private static final String SKIP = "skip"; //NOI18N 

    private static final String FORCE = "force"; //NOI18N 

    private static final String DRAFT = "draftCompile"; //NOI18N 

    public GwtCompilerOptionsPanel( Lookup context ) {
        initComponents();

        DefaultComboBoxModel<JSStyle> model = new DefaultComboBoxModel<>();
        for (JSStyle style : JSStyle.values()) {
            model.addElement(style);
        }
        myStyle.setModel(model);

        DefaultComboBoxModel<GwtLogLevel> logModel = new DefaultComboBoxModel<>();
        for (GwtLogLevel level : GwtLogLevel.values()) {
            logModel.addElement(level);
        }
        myLogLevel.setModel(logModel);

        setValues(context);

        final GwtOptionsModification operation = new GwtOptionsModification();
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

        myJvmArgs.getDocument().addDocumentListener(documentListener);
        myWebApp.getDocument().addDocumentListener(documentListener);
        myPersistUnitCacheDir.getDocument().addDocumentListener(
                documentListener);

        ((DefaultEditor) myThreads.getEditor()).getTextField().getDocument()
                .addDocumentListener(documentListener);

        ItemListener itemListener = new ItemListener() {

            @Override
            public void itemStateChanged( ItemEvent e ) {
                addModification(handle, operation);
            }
        };

        myStyle.addItemListener(itemListener);
        myLogLevel.addItemListener(itemListener);

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

    private void addModification( ModelHandle2 handle,
            GwtOptionsModification newOperation )
    {
        List<ModelOperation<POMModel>> pomOperations = handle
                .getPOMOperations();
        for (ModelOperation<POMModel> operation : pomOperations) {
            if (operation instanceof GwtOptionsModification) {
                handle.removePOMModification(operation);
            }
        }
        handle.addPOMModification(newOperation);
    }

    @NbBundle.Messages("readingGwtOptions=Retrieving options...")
    private void setValues( final Lookup context ) {
        ProgressUtils.showProgressDialogAndRun(new InitWorker(context),
                Bundle.readingGwtOptions());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jvmArgs = new javax.swing.JLabel();
        myJvmArgs = new javax.swing.JTextField();
        webAppDirLbl = new javax.swing.JLabel();
        myWebApp = new javax.swing.JTextField();
        persistentunitcachedirLbl = new javax.swing.JLabel();
        myPersistUnitCacheDir = new javax.swing.JTextField();
        styleLbl = new javax.swing.JLabel();
        myStyle = new javax.swing.JComboBox();
        optimizationLbl = new javax.swing.JLabel();
        myOptimization = new javax.swing.JSlider();
        myOptimizationValue = new javax.swing.JTextField();
        threadsLbl = new javax.swing.JLabel();
        myThreads = new ThreadsSpinner();
        logLbl = new javax.swing.JLabel();
        myLogLevel = new javax.swing.JComboBox();
        myCompileReport = new javax.swing.JCheckBox();
        separator = new javax.swing.JSeparator();
        myStrict = new javax.swing.JCheckBox();
        myDraft = new javax.swing.JCheckBox();
        myForce = new javax.swing.JCheckBox();
        mySkip = new javax.swing.JCheckBox();
        myValidate = new javax.swing.JCheckBox();
        myTreeLogger = new javax.swing.JCheckBox();
        myPersistenceCache = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(jvmArgs,
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "LBL_JvmArgs")); // NOI18N

        myJvmArgs.setToolTipText(org.openide.util.NbBundle.getMessage(
                GwtCompilerOptionsPanel.class, "TLTP_JvmArgs")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(webAppDirLbl,
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "LBL_WebApp")); // NOI18N

        myWebApp.setToolTipText(org.openide.util.NbBundle.getMessage(
                GwtCompilerOptionsPanel.class, "TLTP_WebApp")); // NOI18N

        persistentunitcachedirLbl.setLabelFor(myPersistUnitCacheDir);
        org.openide.awt.Mnemonics.setLocalizedText(persistentunitcachedirLbl,
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "LBL_Persistent")); // NOI18N
        persistentunitcachedirLbl.setToolTipText("");

        myPersistUnitCacheDir.setToolTipText(org.openide.util.NbBundle
                .getMessage(GwtCompilerOptionsPanel.class, "TLTP_Persistent")); // NOI18N

        styleLbl.setLabelFor(myStyle);
        org.openide.awt.Mnemonics.setLocalizedText(styleLbl,
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "LBL_Style")); // NOI18N

        myStyle.setToolTipText(org.openide.util.NbBundle.getMessage(
                GwtCompilerOptionsPanel.class, "TLTP_Style")); // NOI18N

        optimizationLbl.setLabelFor(myOptimization);
        org.openide.awt.Mnemonics.setLocalizedText(optimizationLbl,
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "LBL_Optimization")); // NOI18N

        myOptimization.setMaximum(9);
        myOptimization.setMinimum(-1);
        myOptimization.setToolTipText(org.openide.util.NbBundle.getMessage(
                GwtCompilerOptionsPanel.class, "TLTP_Optimization")); // NOI18N
        myOptimization.setValue(-1);

        myOptimizationValue.setEditable(false);
        myOptimizationValue
                .setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        myOptimizationValue.setText(org.openide.util.NbBundle.getMessage(
                GwtCompilerOptionsPanel.class,
                "GwtCompilerOptionsPanel.myOptimizationValue.text")); // NOI18N

        threadsLbl.setLabelFor(myThreads);
        org.openide.awt.Mnemonics.setLocalizedText(threadsLbl,
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "LBL_Threads")); // NOI18N

        myThreads.setToolTipText(org.openide.util.NbBundle.getMessage(
                GwtCompilerOptionsPanel.class, "TLTP_Threads")); // NOI18N

        logLbl.setLabelFor(myLogLevel);
        org.openide.awt.Mnemonics.setLocalizedText(logLbl,
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "LBL_Logging")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(myCompileReport,
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "LBL_CompileReport")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(myStrict,
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "LBL_StrictMode")); // NOI18N
        myStrict.setToolTipText(org.openide.util.NbBundle.getMessage(
                GwtCompilerOptionsPanel.class, "TLTP_Sctrict")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(myDraft,
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "LBL_Draft")); // NOI18N
        myDraft.setToolTipText(org.openide.util.NbBundle.getMessage(
                GwtCompilerOptionsPanel.class, "TLTP_Draft")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(myForce,
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "LBL_Force")); // NOI18N
        myForce.setToolTipText(org.openide.util.NbBundle.getMessage(
                GwtCompilerOptionsPanel.class, "TLTP_Force")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(mySkip,
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "LBL_Skip")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(myValidate,
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "LBL_Validate")); // NOI18N
        myValidate.setToolTipText(org.openide.util.NbBundle.getMessage(
                GwtCompilerOptionsPanel.class, "TLTP_Validate")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(myTreeLogger,
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "LBL_TreeLogger")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(myPersistenceCache,
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "LBL_PersistenceCache")); // NOI18N

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
                                                                .addGroup(
                                                                        layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
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
                                                                                                                        jvmArgs)
                                                                                                                .addComponent(
                                                                                                                        styleLbl)
                                                                                                                .addComponent(
                                                                                                                        optimizationLbl)
                                                                                                                .addComponent(
                                                                                                                        threadsLbl)
                                                                                                                .addComponent(
                                                                                                                        logLbl))
                                                                                                .addPreferredGap(
                                                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addGroup(
                                                                                                        layout.createParallelGroup(
                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
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
                                                                                                                                        25,
                                                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                                                .addComponent(
                                                                                                                        myStyle,
                                                                                                                        0,
                                                                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                        Short.MAX_VALUE)
                                                                                                                .addComponent(
                                                                                                                        myPersistUnitCacheDir)
                                                                                                                .addComponent(
                                                                                                                        myWebApp)
                                                                                                                .addComponent(
                                                                                                                        myJvmArgs,
                                                                                                                        javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                                                .addComponent(
                                                                                                                        myLogLevel,
                                                                                                                        0,
                                                                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                        Short.MAX_VALUE)
                                                                                                                .addGroup(
                                                                                                                        layout.createSequentialGroup()
                                                                                                                                .addComponent(
                                                                                                                                        myThreads,
                                                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                        41,
                                                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                                .addGap(0,
                                                                                                                                        0,
                                                                                                                                        Short.MAX_VALUE))))
                                                                                .addComponent(
                                                                                        separator))
                                                                .addContainerGap())
                                                .addGroup(
                                                        layout.createSequentialGroup()
                                                                .addGroup(
                                                                        layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(
                                                                                        myDraft)
                                                                                .addComponent(
                                                                                        mySkip)
                                                                                .addComponent(
                                                                                        myCompileReport)
                                                                                .addComponent(
                                                                                        myTreeLogger))
                                                                .addGap(25, 25,
                                                                        25)
                                                                .addGroup(
                                                                        layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(
                                                                                        myPersistenceCache)
                                                                                .addComponent(
                                                                                        myStrict)
                                                                                .addComponent(
                                                                                        myForce)
                                                                                .addComponent(
                                                                                        myValidate))
                                                                .addGap(0,
                                                                        98,
                                                                        Short.MAX_VALUE)))));
        layout.setVerticalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(
                        layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(
                                                        myJvmArgs,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jvmArgs))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
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
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(styleLbl)
                                                .addComponent(
                                                        myStyle,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(
                                                        layout.createSequentialGroup()
                                                                .addComponent(
                                                                        optimizationLbl)
                                                                .addGap(18, 18,
                                                                        18)
                                                                .addComponent(
                                                                        threadsLbl))
                                                .addComponent(
                                                        myOptimizationValue,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGroup(
                                                        layout.createSequentialGroup()
                                                                .addComponent(
                                                                        myOptimization,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(
                                                                        myThreads,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(
                                                        myLogLevel,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(logLbl))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(separator,
                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(myCompileReport)
                                                .addComponent(myStrict))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(myDraft)
                                                .addComponent(myForce))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(myValidate)
                                                .addComponent(mySkip))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(myTreeLogger)
                                                .addComponent(
                                                        myPersistenceCache))
                                .addContainerGap(
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        Short.MAX_VALUE)));

        jvmArgs.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSN_JvmArgs")); // NOI18N
        jvmArgs.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSD_JvmArgs")); // NOI18N
        myJvmArgs.getAccessibleContext().setAccessibleName(
                jvmArgs.getAccessibleContext().getAccessibleName());
        myJvmArgs.getAccessibleContext().setAccessibleDescription(
                jvmArgs.getAccessibleContext().getAccessibleDescription());
        webAppDirLbl.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSN_WebApp")); // NOI18N
        webAppDirLbl.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSD_WebApp")); // NOI18N
        myWebApp.getAccessibleContext().setAccessibleName(
                webAppDirLbl.getAccessibleContext().getAccessibleName());
        myWebApp.getAccessibleContext().setAccessibleDescription(
                webAppDirLbl.getAccessibleContext().getAccessibleDescription());
        persistentunitcachedirLbl.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSN_Persistent")); // NOI18N
        persistentunitcachedirLbl.getAccessibleContext()
                .setAccessibleDescription(
                        org.openide.util.NbBundle.getMessage(
                                GwtCompilerOptionsPanel.class,
                                "ACSD_Persistent")); // NOI18N
        myPersistUnitCacheDir.getAccessibleContext().setAccessibleName(
                persistentunitcachedirLbl.getAccessibleContext()
                        .getAccessibleName());
        myPersistUnitCacheDir.getAccessibleContext().setAccessibleDescription(
                persistentunitcachedirLbl.getAccessibleContext()
                        .getAccessibleDescription());
        styleLbl.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSN_Style")); // NOI18N
        styleLbl.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSD_Style")); // NOI18N
        myStyle.getAccessibleContext().setAccessibleName(
                styleLbl.getAccessibleContext().getAccessibleName());
        myStyle.getAccessibleContext().setAccessibleDescription(
                styleLbl.getAccessibleContext().getAccessibleDescription());
        optimizationLbl.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSN_Optimization")); // NOI18N
        optimizationLbl.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSD_Optimization")); // NOI18N
        myOptimization.getAccessibleContext().setAccessibleName(
                optimizationLbl.getAccessibleContext().getAccessibleName());
        myOptimization.getAccessibleContext().setAccessibleDescription(
                optimizationLbl.getAccessibleContext()
                        .getAccessibleDescription());
        myOptimizationValue.getAccessibleContext().setAccessibleName(
                optimizationLbl.getAccessibleContext().getAccessibleName());
        myOptimizationValue.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle
                        .getMessage(GwtCompilerOptionsPanel.class,
                                "ACSD_OptimizationLevel")); // NOI18N
        threadsLbl.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSN_CompilerThreads")); // NOI18N
        threadsLbl.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSD_CompilerThreads")); // NOI18N
        myThreads.getAccessibleContext().setAccessibleName(
                threadsLbl.getAccessibleContext().getAccessibleName());
        myThreads.getAccessibleContext().setAccessibleDescription(
                threadsLbl.getAccessibleContext().getAccessibleDescription());
        logLbl.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSN_LogLevel")); // NOI18N
        logLbl.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSD_LogLevel")); // NOI18N
        myLogLevel.getAccessibleContext().setAccessibleName(
                logLbl.getAccessibleContext().getAccessibleName());
        myLogLevel.getAccessibleContext().setAccessibleDescription(
                logLbl.getAccessibleContext().getAccessibleDescription());
        myCompileReport.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSN_CompileReport")); // NOI18N
        myCompileReport.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSD_CompileReport")); // NOI18N
        myStrict.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSN_Strict")); // NOI18N
        myStrict.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSD_Strict")); // NOI18N
        myDraft.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSN_Draft")); // NOI18N
        myDraft.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSD_Draft")); // NOI18N
        myForce.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSN_Force")); // NOI18N
        myForce.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSD_Force")); // NOI18N
        mySkip.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSN_Skip")); // NOI18N
        mySkip.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSD_Skip")); // NOI18N
        myValidate.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSN_Validate")); // NOI18N
        myValidate.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSD_Validate")); // NOI18N
        myTreeLogger.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSN_TreeLogger")); // NOI18N
        myTreeLogger.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSD_TreeLogger")); // NOI18N
        myPersistenceCache.getAccessibleContext()
                .setAccessibleName(
                        org.openide.util.NbBundle.getMessage(
                                GwtCompilerOptionsPanel.class,
                                "ACSN_PersistenceCache")); // NOI18N
        myPersistenceCache.getAccessibleContext()
                .setAccessibleDescription(
                        org.openide.util.NbBundle.getMessage(
                                GwtCompilerOptionsPanel.class,
                                "ACSD_PersistenceCache")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents
     // Variables declaration - do not modify//GEN-BEGIN:variables

    private javax.swing.JLabel jvmArgs;

    private javax.swing.JLabel logLbl;

    private javax.swing.JCheckBox myCompileReport;

    private javax.swing.JCheckBox myDraft;

    private javax.swing.JCheckBox myForce;

    private javax.swing.JTextField myJvmArgs;

    private javax.swing.JComboBox myLogLevel;

    private javax.swing.JSlider myOptimization;

    private javax.swing.JTextField myOptimizationValue;

    private javax.swing.JTextField myPersistUnitCacheDir;

    private javax.swing.JCheckBox myPersistenceCache;

    private javax.swing.JCheckBox mySkip;

    private javax.swing.JCheckBox myStrict;

    private javax.swing.JComboBox myStyle;

    private javax.swing.JSpinner myThreads;

    private javax.swing.JCheckBox myTreeLogger;

    private javax.swing.JCheckBox myValidate;

    private javax.swing.JTextField myWebApp;

    private javax.swing.JLabel optimizationLbl;

    private javax.swing.JLabel persistentunitcachedirLbl;

    private javax.swing.JSeparator separator;

    private javax.swing.JLabel styleLbl;

    private javax.swing.JLabel threadsLbl;

    private javax.swing.JLabel webAppDirLbl;

    // End of variables declaration//GEN-END:variables

    private final class GwtOptionsModification implements
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

            POMUtils.setTextField(JVM_ARGS, values, myJvmArgs, configuration);
            POMUtils.setTextField(WEBAPP_DIRECTORY, values, myWebApp,
                    configuration);

            if (myPersistenceCache.isSelected()) {
                POMUtils.setTextField(PERSISTENT_UNIT_CACHEDIR, values,
                        myPersistUnitCacheDir, configuration);
            }

            POMExtensibilityElement style = values.get(STYLE);
            JSStyle item = (JSStyle) myStyle.getSelectedItem();
            if (item != null) {
                if (style == null) {
                    configuration.addExtensibilityElement(POMUtils
                            .createElement(model, STYLE, item.getValue()));
                }
                else {
                    String value = POMUtils.getValue(style);
                    JSStyle oldValue = JSStyle.forString(value);
                    if (!oldValue.equals(item)) {
                        style.setElementText(item.getValue());
                    }
                }
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

            POMExtensibilityElement logging = values.get(LOG_LEVEL);
            GwtLogLevel logItem = (GwtLogLevel) myLogLevel.getSelectedItem();
            if (logItem != null) {
                if (logging == null) {
                    if (!logItem.equals(GwtLogLevel.INFO)) {
                        configuration
                                .addExtensibilityElement(POMUtils
                                        .createElement(model, LOG_LEVEL,
                                                logItem.name()));
                    }
                }
                else {
                    if (!POMUtils.getValue(logging).equals(logItem.name())) {
                        logging.setElementText(logItem.name());
                    }
                }
            }

            try {
                String threadsValue = ((DefaultEditor) myThreads.getEditor())
                        .getTextField().getText();
                if (threadsValue.length() > 0) {
                    Integer newValue = Integer.parseInt(threadsValue);
                    if (((Integer) myThreads.getValue()) > 0) {
                        POMExtensibilityElement threads = values
                                .get(LOCAL_WORKERS);
                        if (threads == null) {
                            configuration.addExtensibilityElement(POMUtils
                                    .createElement(model, LOCAL_WORKERS,
                                            threadsValue));
                        }
                        else {
                            if (!POMUtils.getValue(threads).equals(String.valueOf(newValue))) {
                                threads.setElementText(threadsValue);
                            }
                        }
                    }
                }
            }
            catch (NumberFormatException ignore) {
            }

            POMUtils.setBooleanVaue(COMPILE_REPORT, values, myCompileReport,
                    configuration);
            POMUtils.setBooleanVaue(STRICT, values, myStrict, configuration);
            POMUtils.setBooleanVaue(DRAFT, values, myDraft, configuration);
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
                    case JVM_ARGS:
                        jvmArgs = value;
                        break;
                    case WEBAPP_DIRECTORY:
                        webApp = value;
                        break;
                    case PERSISTENT_UNIT_CACHEDIR:
                        persistentUnitDir = value;
                        break;
                    case STYLE:
                        style = JSStyle.forString(value);
                        break;
                    case LOG_LEVEL:
                        logLevel = GwtLogLevel.forString(value);
                        break;
                    case OPTIMIZATION_LEVEL:
                        try {
                            optimization = Integer.parseInt(value);
                        }
                        catch (NumberFormatException e) {
                            optimization = -1;
                        }
                        break;
                    case LOCAL_WORKERS:
                        try {
                            threads = Integer.parseInt(value);
                        }
                        catch (NumberFormatException ignore) {
                        }
                        break;
                    case COMPILE_REPORT:
                        compileReport = Boolean.parseBoolean(value);
                        break;
                    case STRICT:
                        strict = Boolean.parseBoolean(value);
                        break;
                    case DRAFT:
                        draft = Boolean.parseBoolean(value);
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

            myJvmArgs.setText(jvmArgs);
            myWebApp.setText(webApp);
            myPersistUnitCacheDir.setText(persistentUnitDir);
            myStyle.setSelectedItem(style);
            myLogLevel.setSelectedItem(logLevel);
            myOptimization.setValue(optimization);
            myOptimizationValue.setText(String.valueOf(optimization));
            if (threads == -1) {
                ((DefaultEditor) myThreads.getEditor()).getTextField().setText(
                        "");
            }
            else {
                myThreads.setValue(threads);
            }
            myCompileReport.setSelected(compileReport);
            myStrict.setSelected(strict);
            myDraft.setSelected(draft);
            myForce.setSelected(force);
            mySkip.setSelected(skip);
            myValidate.setSelected(validate);
            myTreeLogger.setSelected(treeLogger);
            myPersistenceCache.setSelected(usePersistentUnitCache);

            myPersistUnitCacheDir.setEditable(myPersistenceCache.isSelected());
        }

        private final Lookup myLookup;

        private String jvmArgs;

        private String webApp;

        private String persistentUnitDir;

        private JSStyle style;

        private int optimization = -1;

        private GwtLogLevel logLevel = GwtLogLevel.INFO;

        private int threads = -1;

        private boolean compileReport;

        private boolean strict;

        private boolean draft;

        private boolean force;

        private boolean skip;

        private boolean validate;

        private boolean treeLogger;

        private boolean usePersistentUnitCache;
    }

    enum JSStyle {
        OBFUSCATED, PRETTY, DETAILED;

        @Override
        public String toString() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }

        String getValue() {
            if (this.equals(OBFUSCATED)) {
                return name().substring(0, 3);
            }
            else {
                return name();
            }
        }

        static JSStyle forString( String value ) {
            for (JSStyle style : values()) {
                if (style.getValue().equals(value)) {
                    return style;
                }
            }
            return OBFUSCATED;
        }
    }

    enum GwtLogLevel {
        ERROR, WARN, INFO, TRACE, DEBUG, SPAM, ALL;

        static GwtLogLevel forString( String value ) {
            for (GwtLogLevel level : values()) {
                if (level.name().equals(value)) {
                    return level;
                }
            }
            return INFO;
        }
    }
}
