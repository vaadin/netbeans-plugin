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
package org.vaadin.netbeans.maven.directory;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.openide.util.NbBundle;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.Maturity;
import org.vaadin.netbeans.maven.editor.completion.AddOn;
import org.vaadin.netbeans.maven.editor.completion.AddOnProvider;
import org.vaadin.netbeans.maven.editor.completion.SearchQuery;
import org.vaadin.netbeans.maven.editor.completion.SearchQuery.Field;
import org.vaadin.netbeans.maven.editor.completion.SearchResult;

/**
 * @author denis
 */
public class SearchPanel extends JPanel {

    enum AnyMaturity {
        ANY;

        @Override
        public String toString() {
            return NbBundle.getMessage(SearchPanel.class, "ITEM_AnyMaturity"); // NOI18N
        }
    }

    enum LicenseType {
        ALL("ITEM_LicenseAll"), // NOI18N
        FREE("ITEM_Free"), // NOI18N
        COMERCIAL("ITEM_Commercial");// NOI18N

        LicenseType( String key ) {
            myKey = key;
        }

        @Override
        public String toString() {
            return NbBundle.getMessage(SearchPanel.class, myKey);
        }

        private String myKey;
    }

    enum TextSearchFields {
        ALL("ITEM_FieldsAll"), // NOI18N
        NAME("ITEM_NameField"), // NOI18N
        DESCRIPTION("ITEM_DescrField");// NOI18N

        TextSearchFields( String key ) {
            myKey = key;
        }

        @Override
        public String toString() {
            return NbBundle.getMessage(SearchPanel.class, myKey);
        }

        private String myKey;
    }

    @NbBundle.Messages("tableHeaderTooltip=Click to sort")
    public SearchPanel() {
        initComponents();

        myListeners = new LinkedList<>();

        myMaturity.setModel(new DefaultComboBoxModel<>(getMaturities()));
        myLicenseType
                .setModel(new DefaultComboBoxModel<>(LicenseType.values()));
        myFields.setModel(new DefaultComboBoxModel<>(TextSearchFields.values()));

        myAddons.setAutoCreateRowSorter(true);
        myAddons.getTableHeader().setToolTipText(Bundle.tableHeaderTooltip());
        myAddons.getSelectionModel().setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        myAddons.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {

                    @Override
                    public void valueChanged( ListSelectionEvent e ) {
                        AddonsModel model = (AddonsModel) myAddons.getModel();
                        int index = myAddons.getSelectedRow();
                        SearchResult result =
                                index < 0 ? null : model.getResult(index);
                        mySelected =
                                AddOnProvider.getInstance().getAddOn(result);
                        ((AddOnDocPane) myDocPane).setAddOn(mySelected);

                        fireChangeEvent();
                    }
                });
        adjustColumnWidth(true);

        // Fix for #13142 - Remove the "License Type" filter from Add-Ons browser dialog.
        myLicenseType.setVisible(false);
        license.setVisible(false);
    }

    private void adjustColumnWidth( boolean initial ) {
        TableColumn name = myAddons.getColumnModel().getColumn(0);
        TableColumn rating = myAddons.getColumnModel().getColumn(1);
        TableColumn date = myAddons.getColumnModel().getColumn(2);

        int dividerLocation = mySplitPanel.getDividerLocation();
        if (initial) {
            myNameColumnWidth = dividerLocation * 5 / 12;
            myRatingColumnWidth = dividerLocation / 6;
            myDateColumnWidth = dividerLocation / 4;
        }

        name.setPreferredWidth(myNameColumnWidth);
        name.setWidth(myNameColumnWidth);

        rating.setPreferredWidth(myRatingColumnWidth);
        rating.setWidth(myRatingColumnWidth);

        date.setPreferredWidth(myDateColumnWidth);
        date.setWidth(myDateColumnWidth);
    }

    public AddOn getSelected() {
        return mySelected;
    }

    public void addChangeListener( ChangeListener listener ) {
        assert SwingUtilities.isEventDispatchThread();
        myListeners.add(listener);
    }

    public void removeChangeListener( ChangeListener listener ) {
        assert SwingUtilities.isEventDispatchThread();
        myListeners.remove(listener);
    }

    public void updateTable() {
        MaturityWrapper maturity =
                (MaturityWrapper) myMaturity.getSelectedItem();
        LicenseType licenseType = (LicenseType) myLicenseType.getSelectedItem();
        Boolean free = null;
        switch (licenseType) {
            case ALL:
                free = null;
                break;
            case COMERCIAL:
                free = false;
                break;
            case FREE:
                free = true;
                break;
        }
        TextSearchFields field = (TextSearchFields) myFields.getSelectedItem();
        EnumSet<Field> fields = null;
        switch (field) {
            case ALL:
                fields = EnumSet.allOf(Field.class);
                break;
            case NAME:
                fields = EnumSet.of(Field.NAME);
                break;
            case DESCRIPTION:
                fields = EnumSet.of(Field.DESCRIPTION);
                break;
        }
        SearchQuery query =
                new SearchQuery(maturity.getMaturity(), free,
                        mySearch.getText(), fields);
        Collection<? extends SearchResult> results =
                AddOnProvider.getInstance().searchAddons(query);

        setData(results);
    }

    private void setData( Collection<? extends SearchResult> results ) {
        TableColumn name = myAddons.getColumnModel().getColumn(0);
        TableColumn rating = myAddons.getColumnModel().getColumn(1);
        TableColumn date = myAddons.getColumnModel().getColumn(2);

        myNameColumnWidth = name.getWidth();
        myRatingColumnWidth = rating.getWidth();
        myDateColumnWidth = date.getWidth();

        AddonsModel model = new AddonsModel(results);
        myAddons.setModel(model);

        mySelected = null;

        adjustColumnWidth(false);

        fireChangeEvent();
    }

    private MaturityWrapper[] getMaturities() {
        Maturity[] values = Maturity.values();
        MaturityWrapper[] result = new MaturityWrapper[values.length + 1];
        result[0] = new MaturityWrapper(AnyMaturity.ANY);
        //System.arraycopy(values, 0, result, 1, values.length);
        int i = 1;
        for (Maturity maturity : values) {
            result[i] = new MaturityWrapper(maturity);
            i++;
        }
        return result;
    }

    private void fireChangeEvent() {
        assert SwingUtilities.isEventDispatchThread();
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener listener : myListeners) {
            listener.stateChanged(event);
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

        maturity = new javax.swing.JLabel();
        myMaturity = new javax.swing.JComboBox<MaturityWrapper>();
        license = new javax.swing.JLabel();
        myLicenseType = new javax.swing.JComboBox<LicenseType>();
        search = new javax.swing.JLabel();
        mySearch = new javax.swing.JTextField();
        myFields = new javax.swing.JComboBox<TextSearchFields>();
        mySplitPanel = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        myAddons = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        myDocPane = new AddOnDocPane();

        maturity.setLabelFor(myMaturity);
        org.openide.awt.Mnemonics.setLocalizedText(maturity, org.openide.util.NbBundle.getMessage(SearchPanel.class, "LBL_Maturity")); // NOI18N

        license.setLabelFor(myLicenseType);
        org.openide.awt.Mnemonics.setLocalizedText(license, org.openide.util.NbBundle.getMessage(SearchPanel.class, "LBL_License")); // NOI18N

        search.setLabelFor(mySearch);
        org.openide.awt.Mnemonics.setLocalizedText(search, org.openide.util.NbBundle.getMessage(SearchPanel.class, "LBL_SearchText")); // NOI18N

        mySplitPanel.setDividerLocation(350);

        myAddons.setModel(new AddonsModel());
        jScrollPane1.setViewportView(myAddons);

        mySplitPanel.setLeftComponent(jScrollPane1);

        myDocPane.setEditable(false);
        myDocPane.setContentType("text/html"); // NOI18N
        jScrollPane2.setViewportView(myDocPane);

        mySplitPanel.setRightComponent(jScrollPane2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mySplitPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 855, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(search)
                            .addComponent(maturity))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(myMaturity, javax.swing.GroupLayout.PREFERRED_SIZE, 189, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(license))
                            .addComponent(mySearch, javax.swing.GroupLayout.PREFERRED_SIZE, 552, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(myFields, 0, 205, Short.MAX_VALUE)
                            .addComponent(myLicenseType, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(maturity)
                    .addComponent(myMaturity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(myLicenseType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(license))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mySearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(search)
                    .addComponent(myFields, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mySplitPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 365, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel license;
    private javax.swing.JLabel maturity;
    private javax.swing.JTable myAddons;
    private javax.swing.JEditorPane myDocPane;
    private javax.swing.JComboBox<TextSearchFields> myFields;
    private javax.swing.JComboBox<LicenseType> myLicenseType;
    private javax.swing.JComboBox<MaturityWrapper> myMaturity;
    private javax.swing.JTextField mySearch;
    private javax.swing.JSplitPane mySplitPanel;
    private javax.swing.JLabel search;
    // End of variables declaration//GEN-END:variables

    private AddOn mySelected;

    private List<ChangeListener> myListeners;

    private int myNameColumnWidth;

    private int myRatingColumnWidth;

    private int myDateColumnWidth;

    @NbBundle.Messages({ "addOnName=Add-On Name", "rating=Rating",
            "lastUpdated=Last Updated" })
    private static class AddonsModel extends DefaultTableModel {

        AddonsModel() {
            this(Collections.<SearchResult> emptyList());
        }

        AddonsModel( Collection<? extends SearchResult> results ) {
            super(createData(results), new String[] { Bundle.addOnName(),
                    Bundle.rating(), Bundle.lastUpdated() });

            myResults = new SearchResult[getRowCount()];
            int i = 0;
            for (SearchResult result : results) {
                myResults[i] = result;
                i++;
            }
        }

        @Override
        public boolean isCellEditable( int row, int column ) {
            return false;
        }

        @Override
        public Class<?> getColumnClass( int columnIndex ) {
            if (columnIndex == 0) {
                return String.class;
            }
            else if (columnIndex == 1) {
                return Double.class;
            }
            else {
                return Date.class;
            }
        }

        SearchResult getResult( int index ) {
            return myResults[index];
        }

        static Object[][] createData( Collection<? extends SearchResult> results )
        {
            List<Object[]> list = new LinkedList<>();
            for (SearchResult result : results) {
                list.add(new Object[] { result.getName(), result.rating(),
                        result.lastUpdated() });
            }

            Object[][] data = new Object[list.size()][];
            int i = 0;
            for (Object[] row : list) {
                data[i] = row;
                i++;
            }
            return data;
        }

        private SearchResult[] myResults;
    }

    private static class MaturityWrapper {

        MaturityWrapper( Object object ) {
            myObject = object;
        }

        @Override
        public String toString() {
            if (myObject.equals(AnyMaturity.ANY)) {
                return myObject.toString();
            }
            else {
                Maturity maturity = (Maturity) myObject;
                return maturity.toString().charAt(0)
                        + maturity.toString().substring(1).toLowerCase();
            }
        }

        Maturity getMaturity() {
            return myObject instanceof Maturity ? (Maturity) myObject : null;
        }

        private Object myObject;
    }

}
