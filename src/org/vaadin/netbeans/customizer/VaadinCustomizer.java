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

package org.vaadin.netbeans.customizer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.NbBundle;

/**
 * @author denis
 */
@OptionsPanelController.Keywords(keywords = { "vaadin" }, location = "Java",
        tabTitle = "#Vaadin")
public class VaadinCustomizer extends JPanel implements ActionListener,
        DocumentListener, ChangeListener
{

    enum ConfirmationStrategy {
        ALL("ITEM_All"),
        ONLY_COMERCIAL("ITEM_Commercial");

        private ConfirmationStrategy( String key ) {
            myKey = key;
        }

        @Override
        public String toString() {
            return NbBundle.getMessage(ConfirmationStrategy.class, myKey);
        }

        private String myKey;
    }

    public VaadinCustomizer() {
        initComponents();

        Document document = new NumericDocument(myTimeout, -1);
        myTimeout.setDocument(document);

        document.addDocumentListener(this);

        myJetty.addActionListener(this);
        myCodeCompletion.addActionListener(this);
        myStatistics.addActionListener(this);
        myMinPrefixLength.addChangeListener(this);

        ((DefaultEditor) myMinPrefixLength.getEditor()).getTextField()
                .getDocument().addDocumentListener(this);

        DefaultComboBoxModel<ConfirmationStrategy> model =
                new DefaultComboBoxModel<>(ConfirmationStrategy.values());
        myConfirm.setModel(model);

        myVersions.setModel(createRemoteDataModel());
        myIndex.setModel(createRemoteDataModel());
        myRest.setModel(createRemoteDataModel());
    }

    @Override
    public void changedUpdate( DocumentEvent e ) {
        updateTimeout();
    }

    @Override
    public void insertUpdate( DocumentEvent e ) {
        updateTimeout();
    }

    @Override
    public void removeUpdate( DocumentEvent e ) {
        updateTimeout();
    }

    @Override
    public void stateChanged( ChangeEvent e ) {
        if (!isInitialized) {
            return;
        }
        isChanged = true;
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
        if (!isInitialized) {
            return;
        }
        Object source = e.getSource();

        if (!isChanged) {
            isChanged =
                    myJetty.equals(source) || myStatistics.equals(source)
                            || myCodeCompletion.equals(source)
                            || myConfirm.equals(source);
        }
    }

    void cancel() {
        isChanged = false;
    }

    void applyChanges() {
        if (!isInitialized) {
            return;
        }
        VaadinConfiguration.getInstance().enableJetty(myJetty.isSelected());
        VaadinConfiguration.getInstance().enableAddonCodeCompletion(
                myCodeCompletion.isSelected());
        VaadinConfiguration.getInstance().enableStatistics(
                myStatistics.isSelected());
        String text = myTimeout.getText();
        try {
            VaadinConfiguration.getInstance()
                    .setTimeout(Integer.parseInt(text));
        }
        catch (NumberFormatException e) {
        }
        VaadinConfiguration.getInstance().setFreeAddonRequiresConfirmation(
                ConfirmationStrategy.ALL.equals(myConfirm.getSelectedItem()));

        VaadinConfiguration.getInstance().setVersionRequestStrategy(
                (RemoteDataAccessStrategy) myVersions.getSelectedItem());
        VaadinConfiguration.getInstance().setIndexRequestStrategy(
                (RemoteDataAccessStrategy) myIndex.getSelectedItem());
        VaadinConfiguration.getInstance().setDirectoryRequestStrategy(
                (RemoteDataAccessStrategy) myRest.getSelectedItem());

        try {
            myMinPrefixLength.commitEdit();
        }
        catch (ParseException ignore) {
        }
        Integer prefix = (Integer) myMinPrefixLength.getValue();
        VaadinConfiguration.getInstance().setCCPrefixLength(prefix);

        isChanged = false;
    }

    void update() {
        isInitialized = false;
        try {
            myJetty.setSelected(VaadinConfiguration.getInstance()
                    .isJettyEnabled());
            myCodeCompletion.setSelected(VaadinConfiguration.getInstance()
                    .isAddonCodeCompletionEnabled());
            myStatistics.setSelected(VaadinConfiguration.getInstance()
                    .isStatisticsEnabled());
            myTimeout.setText(String.valueOf(VaadinConfiguration.getInstance()
                    .getTimeout()));
            myConfirm.setSelectedItem(VaadinConfiguration.getInstance()
                    .freeAddonRequiresConfirmation() ? ConfirmationStrategy.ALL
                    : ConfirmationStrategy.ONLY_COMERCIAL);

            myVersions.setSelectedItem(VaadinConfiguration.getInstance()
                    .getVersionRequestStrategy());
            myIndex.setSelectedItem(VaadinConfiguration.getInstance()
                    .getIndexRequestStrategy());
            myRest.setSelectedItem(VaadinConfiguration.getInstance()
                    .getDirectoryRequestStrategy());

            myMinPrefixLength.setValue(VaadinConfiguration.getInstance()
                    .getCCPrefixLength());
        }
        finally {
            isInitialized = true;
        }
    }

    boolean isChanged() {
        return isChanged;
    }

    boolean valid() {
        return true;
    }

    private void updateTimeout() {
        if (!isInitialized) {
            return;
        }
        isChanged = true;
    }

    private DefaultComboBoxModel<RemoteDataAccessStrategy> createRemoteDataModel()
    {
        return new DefaultComboBoxModel<>(RemoteDataAccessStrategy.values());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        projectPanel = new javax.swing.JPanel();
        myJetty = new javax.swing.JCheckBox();
        versions = new javax.swing.JLabel();
        myVersions = new javax.swing.JComboBox();
        timeout = new javax.swing.JLabel();
        myTimeout = new javax.swing.JTextField();
        seconds = new javax.swing.JLabel();
        addonPanel = new javax.swing.JPanel();
        myCodeCompletion = new javax.swing.JCheckBox();
        index = new javax.swing.JLabel();
        myIndex = new javax.swing.JComboBox();
        rest = new javax.swing.JLabel();
        myRest = new javax.swing.JComboBox();
        confirmation = new javax.swing.JLabel();
        myConfirm = new javax.swing.JComboBox();
        myStatistics = new javax.swing.JCheckBox();
        myMinPrefixLength = new NumericSpinner(2);
        ;
        minPrefixLength = new javax.swing.JLabel();

        projectPanel.setBorder(javax.swing.BorderFactory
                .createTitledBorder(org.openide.util.NbBundle.getMessage(
                        VaadinCustomizer.class, "TTL_CommonSettings"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(myJetty,
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "LBL_Jetty")); // NOI18N
        myJetty.setActionCommand(org.openide.util.NbBundle.getMessage(
                VaadinCustomizer.class, "LBL_Jetty")); // NOI18N

        versions.setLabelFor(myVersions);
        org.openide.awt.Mnemonics.setLocalizedText(versions,
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "LBL_Versions")); // NOI18N

        timeout.setLabelFor(myTimeout);
        org.openide.awt.Mnemonics.setLocalizedText(timeout,
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "LBL_Timeout")); // NOI18N

        seconds.setLabelFor(myTimeout);
        org.openide.awt.Mnemonics.setLocalizedText(seconds,
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "LBL_Seconds")); // NOI18N

        javax.swing.GroupLayout projectPanelLayout =
                new javax.swing.GroupLayout(projectPanel);
        projectPanel.setLayout(projectPanelLayout);
        projectPanelLayout
                .setHorizontalGroup(projectPanelLayout
                        .createParallelGroup(
                                javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(
                                projectPanelLayout
                                        .createSequentialGroup()
                                        .addContainerGap()
                                        .addGroup(
                                                projectPanelLayout
                                                        .createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(myJetty)
                                                        .addGroup(
                                                                projectPanelLayout
                                                                        .createSequentialGroup()
                                                                        .addGroup(
                                                                                projectPanelLayout
                                                                                        .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                        .addComponent(
                                                                                                timeout)
                                                                                        .addComponent(
                                                                                                versions))
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addGroup(
                                                                                projectPanelLayout
                                                                                        .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                        .addComponent(
                                                                                                myVersions,
                                                                                                0,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                        .addGroup(
                                                                                                projectPanelLayout
                                                                                                        .createSequentialGroup()
                                                                                                        .addComponent(
                                                                                                                myTimeout)
                                                                                                        .addPreferredGap(
                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                        .addComponent(
                                                                                                                seconds)))))
                                        .addContainerGap()));
        projectPanelLayout
                .setVerticalGroup(projectPanelLayout
                        .createParallelGroup(
                                javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(
                                projectPanelLayout
                                        .createSequentialGroup()
                                        .addContainerGap()
                                        .addComponent(myJetty)
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(
                                                projectPanelLayout
                                                        .createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(versions)
                                                        .addComponent(
                                                                myVersions,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGap(18, 18, 18)
                                        .addGroup(
                                                projectPanelLayout
                                                        .createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(timeout)
                                                        .addComponent(
                                                                myTimeout,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(seconds))
                                        .addContainerGap(
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                Short.MAX_VALUE)));

        myJetty.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "ACSN_Jetty")); // NOI18N
        myJetty.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "ACSD_Jetty")); // NOI18N
        versions.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "ACSN_Versions")); // NOI18N
        versions.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "ACSD_Versions")); // NOI18N
        myVersions.getAccessibleContext().setAccessibleName(
                versions.getAccessibleContext().getAccessibleName());
        myVersions.getAccessibleContext().setAccessibleDescription(
                versions.getAccessibleContext().getAccessibleDescription());
        timeout.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "ACSN_Timeout")); // NOI18N
        timeout.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "ACSD_Timeout")); // NOI18N
        myTimeout.getAccessibleContext().setAccessibleName(
                timeout.getAccessibleContext().getAccessibleName());
        myTimeout.getAccessibleContext().setAccessibleDescription(
                timeout.getAccessibleContext().getAccessibleDescription());

        addonPanel.setBorder(javax.swing.BorderFactory
                .createTitledBorder(org.openide.util.NbBundle.getMessage(
                        VaadinCustomizer.class, "TTL_CodeCompletion"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(myCodeCompletion,
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "LBL_CodeCompletionEnable")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(index,
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "LBL_Index")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(rest,
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "LBL_Rest")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(confirmation,
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "LBL_Confirmation")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(myStatistics,
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "LBL_Statistics")); // NOI18N

        minPrefixLength.setLabelFor(myMinPrefixLength);
        org.openide.awt.Mnemonics.setLocalizedText(minPrefixLength,
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "LBL_MinPrefixLength")); // NOI18N

        javax.swing.GroupLayout addonPanelLayout =
                new javax.swing.GroupLayout(addonPanel);
        addonPanel.setLayout(addonPanelLayout);
        addonPanelLayout
                .setHorizontalGroup(addonPanelLayout
                        .createParallelGroup(
                                javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(
                                addonPanelLayout
                                        .createSequentialGroup()
                                        .addContainerGap()
                                        .addGroup(
                                                addonPanelLayout
                                                        .createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(
                                                                addonPanelLayout
                                                                        .createSequentialGroup()
                                                                        .addGroup(
                                                                                addonPanelLayout
                                                                                        .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                        .addComponent(
                                                                                                rest)
                                                                                        .addComponent(
                                                                                                index))
                                                                        .addGap(37,
                                                                                37,
                                                                                37)
                                                                        .addGroup(
                                                                                addonPanelLayout
                                                                                        .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                        .addComponent(
                                                                                                myIndex,
                                                                                                0,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                        .addComponent(
                                                                                                myRest,
                                                                                                0,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)))
                                                        .addGroup(
                                                                addonPanelLayout
                                                                        .createSequentialGroup()
                                                                        .addComponent(
                                                                                myCodeCompletion)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                19,
                                                                                Short.MAX_VALUE)
                                                                        .addComponent(
                                                                                minPrefixLength)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(
                                                                                myMinPrefixLength,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                49,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(
                                                                addonPanelLayout
                                                                        .createSequentialGroup()
                                                                        .addComponent(
                                                                                myStatistics)
                                                                        .addGap(0,
                                                                                0,
                                                                                Short.MAX_VALUE))
                                                        .addGroup(
                                                                addonPanelLayout
                                                                        .createSequentialGroup()
                                                                        .addComponent(
                                                                                confirmation)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(
                                                                                myConfirm,
                                                                                0,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)))
                                        .addContainerGap()));
        addonPanelLayout
                .setVerticalGroup(addonPanelLayout
                        .createParallelGroup(
                                javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(
                                addonPanelLayout
                                        .createSequentialGroup()
                                        .addContainerGap()
                                        .addGroup(
                                                addonPanelLayout
                                                        .createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(
                                                                myCodeCompletion)
                                                        .addGroup(
                                                                addonPanelLayout
                                                                        .createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                        .addComponent(
                                                                                myMinPrefixLength,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(
                                                                                minPrefixLength)))
                                        .addGap(4, 4, 4)
                                        .addComponent(myStatistics)
                                        .addGap(6, 6, 6)
                                        .addGroup(
                                                addonPanelLayout
                                                        .createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(index)
                                                        .addComponent(
                                                                myIndex,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(
                                                addonPanelLayout
                                                        .createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(
                                                                myRest,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(rest))
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(
                                                addonPanelLayout
                                                        .createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(
                                                                confirmation)
                                                        .addComponent(
                                                                myConfirm,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addContainerGap(
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                Short.MAX_VALUE)));

        myCodeCompletion.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "ACSN_EnableAddOnCC")); // NOI18N
        myCodeCompletion.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "ACSD_EnableAddOnCC")); // NOI18N
        myMinPrefixLength.getAccessibleContext().setAccessibleName(
                minPrefixLength.getAccessibleContext().getAccessibleName());
        myMinPrefixLength.getAccessibleContext().setAccessibleDescription(
                minPrefixLength.getAccessibleContext()
                        .getAccessibleDescription());
        minPrefixLength.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "ACSN_MinPrefixLength")); // NOI18N
        minPrefixLength.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(VaadinCustomizer.class,
                        "ACSD_MinPrefixLength")); // NOI18N

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
                                                .addComponent(
                                                        projectPanel,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        Short.MAX_VALUE)
                                                .addComponent(
                                                        addonPanel,
                                                        javax.swing.GroupLayout.Alignment.TRAILING,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        Short.MAX_VALUE))
                                .addContainerGap()));
        layout.setVerticalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(
                        layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(projectPanel,
                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(addonPanel,
                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        Short.MAX_VALUE)));
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel addonPanel;

    private javax.swing.JLabel confirmation;

    private javax.swing.JLabel index;

    private javax.swing.JLabel minPrefixLength;

    private javax.swing.JCheckBox myCodeCompletion;

    private javax.swing.JComboBox myConfirm;

    private javax.swing.JComboBox myIndex;

    private javax.swing.JCheckBox myJetty;

    private javax.swing.JSpinner myMinPrefixLength;

    private javax.swing.JComboBox myRest;

    private javax.swing.JCheckBox myStatistics;

    private javax.swing.JTextField myTimeout;

    private javax.swing.JComboBox myVersions;

    private javax.swing.JPanel projectPanel;

    private javax.swing.JLabel rest;

    private javax.swing.JLabel seconds;

    private javax.swing.JLabel timeout;

    private javax.swing.JLabel versions;

    // End of variables declaration//GEN-END:variables

    private boolean isChanged;

    private boolean isInitialized;

}
