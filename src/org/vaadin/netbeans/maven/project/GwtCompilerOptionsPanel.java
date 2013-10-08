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

    private static final String JVM_ARGS = "extraJvmArgs"; // NOI18N

    private static final String LOG_LEVEL = "logLevel"; //NOI18N

    private static final String STYLE = "style"; //NOI18N

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

        ActionListener actionListener = new ActionListener() {

            @Override
            public void actionPerformed( ActionEvent e ) {
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
        styleLbl = new javax.swing.JLabel();
        myStyle = new javax.swing.JComboBox();
        threadsLbl = new javax.swing.JLabel();
        myThreads = new ThreadsSpinner();
        logLbl = new javax.swing.JLabel();
        myLogLevel = new javax.swing.JComboBox();
        separator = new javax.swing.JSeparator();
        myDraft = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(jvmArgs,
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "LBL_JvmArgs")); // NOI18N

        myJvmArgs.setToolTipText(org.openide.util.NbBundle.getMessage(
                GwtCompilerOptionsPanel.class, "TLTP_JvmArgs")); // NOI18N

        styleLbl.setLabelFor(myStyle);
        org.openide.awt.Mnemonics.setLocalizedText(styleLbl,
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "LBL_Style")); // NOI18N

        myStyle.setToolTipText(org.openide.util.NbBundle.getMessage(
                GwtCompilerOptionsPanel.class, "TLTP_Style")); // NOI18N

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

        org.openide.awt.Mnemonics.setLocalizedText(myDraft,
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "LBL_Draft")); // NOI18N
        myDraft.setToolTipText(org.openide.util.NbBundle.getMessage(
                GwtCompilerOptionsPanel.class, "TLTP_Draft")); // NOI18N

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
                                                .addComponent(separator)
                                                .addGroup(
                                                        layout.createSequentialGroup()
                                                                .addGroup(
                                                                        layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(
                                                                                        jvmArgs)
                                                                                .addComponent(
                                                                                        styleLbl))
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(
                                                                        layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(
                                                                                        myStyle,
                                                                                        0,
                                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                        Short.MAX_VALUE)
                                                                                .addGroup(
                                                                                        layout.createSequentialGroup()
                                                                                                .addComponent(
                                                                                                        myJvmArgs,
                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                        399,
                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addGap(0,
                                                                                                        0,
                                                                                                        Short.MAX_VALUE))))
                                                .addGroup(
                                                        layout.createSequentialGroup()
                                                                .addComponent(
                                                                        logLbl)
                                                                .addGap(50, 50,
                                                                        50)
                                                                .addComponent(
                                                                        myLogLevel,
                                                                        0,
                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                        Short.MAX_VALUE))
                                                .addGroup(
                                                        layout.createSequentialGroup()
                                                                .addGroup(
                                                                        layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(
                                                                                        myDraft)
                                                                                .addGroup(
                                                                                        layout.createSequentialGroup()
                                                                                                .addComponent(
                                                                                                        threadsLbl)
                                                                                                .addGap(25,
                                                                                                        25,
                                                                                                        25)
                                                                                                .addComponent(
                                                                                                        myThreads,
                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                        41,
                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                                .addGap(0,
                                                                        0,
                                                                        Short.MAX_VALUE)))
                                .addContainerGap()));
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
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(
                                                        myStyle,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(styleLbl))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(threadsLbl)
                                                .addComponent(
                                                        myThreads,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
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
                                .addComponent(myDraft)
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
        myDraft.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSN_Draft")); // NOI18N
        myDraft.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(
                        GwtCompilerOptionsPanel.class, "ACSD_Draft")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents
     // Variables declaration - do not modify//GEN-BEGIN:variables

    private javax.swing.JLabel jvmArgs;

    private javax.swing.JLabel logLbl;

    private javax.swing.JCheckBox myDraft;

    private javax.swing.JTextField myJvmArgs;

    private javax.swing.JComboBox myLogLevel;

    private javax.swing.JComboBox myStyle;

    private javax.swing.JSpinner myThreads;

    private javax.swing.JSeparator separator;

    private javax.swing.JLabel styleLbl;

    private javax.swing.JLabel threadsLbl;

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
                            if (!POMUtils.getValue(threads).equals(
                                    String.valueOf(newValue)))
                            {
                                threads.setElementText(threadsValue);
                            }
                        }
                    }
                }
            }
            catch (NumberFormatException ignore) {
            }

            POMUtils.setBooleanVaue(DRAFT, values, myDraft, configuration);
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
                    case STYLE:
                        style = JSStyle.forString(value);
                        break;
                    case LOG_LEVEL:
                        logLevel = GwtLogLevel.forString(value);
                        break;
                    case LOCAL_WORKERS:
                        try {
                            threads = Integer.parseInt(value);
                        }
                        catch (NumberFormatException ignore) {
                        }
                        break;
                    case DRAFT:
                        draft = Boolean.parseBoolean(value);
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
            myStyle.setSelectedItem(style);
            myLogLevel.setSelectedItem(logLevel);
            if (threads == -1) {
                ((DefaultEditor) myThreads.getEditor()).getTextField().setText(
                        "");
            }
            else {
                myThreads.setValue(threads);
            }
            myDraft.setSelected(draft);
        }

        private final Lookup myLookup;

        private String jvmArgs;

        private JSStyle style;

        private GwtLogLevel logLevel = GwtLogLevel.INFO;

        private int threads = -1;

        private boolean draft;

    }

    enum JSStyle {
        OBFUSCATED,
        PRETTY,
        DETAILED;

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
        ERROR,
        WARN,
        INFO,
        TRACE,
        DEBUG,
        SPAM,
        ALL;

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
