package org.vaadin.netbeans.editor.analyzer.ui;

import javax.swing.SwingUtilities;

/**
 * @author denis
 */
public class RpcInterfacePanel extends javax.swing.JPanel {

    /**
     * Creates new form RpcInterfacePanel
     */
    public RpcInterfacePanel( String iface ) {
        initComponents();
        myName.setText(iface);

        selectText();
    }

    public String getIfaceName() {
        return myName.getText().toString();
    }

    private void selectText() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (SwingUtilities.getWindowAncestor(RpcInterfacePanel.this) == null)
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

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        nameLbl = new javax.swing.JLabel();
        myName = new javax.swing.JTextField();

        nameLbl.setLabelFor(myName);
        org.openide.awt.Mnemonics.setLocalizedText(nameLbl,
                org.openide.util.NbBundle.getMessage(RpcInterfacePanel.class,
                        "LBL_RpcInterfaceName")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(
                        layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(nameLbl)
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(myName,
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        337, Short.MAX_VALUE).addContainerGap()));
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
                                .addContainerGap(
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        Short.MAX_VALUE)));

        nameLbl.getAccessibleContext().setAccessibleName(
                org.openide.util.NbBundle.getMessage(RpcInterfacePanel.class,
                        "ACSN_RpcInterfaceName")); // NOI18N
        nameLbl.getAccessibleContext().setAccessibleDescription(
                org.openide.util.NbBundle.getMessage(RpcInterfacePanel.class,
                        "ACSD_RpcInterfaceName")); // NOI18N
        myName.getAccessibleContext().setAccessibleName(
                nameLbl.getAccessibleContext().getAccessibleName());
        myName.getAccessibleContext().setAccessibleDescription(
                nameLbl.getAccessibleContext().getAccessibleDescription());
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField myName;

    private javax.swing.JLabel nameLbl;
    // End of variables declaration//GEN-END:variables
}
