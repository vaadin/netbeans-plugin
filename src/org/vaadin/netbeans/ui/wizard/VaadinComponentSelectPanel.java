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
package org.vaadin.netbeans.ui.wizard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.TypeElement;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.code.WidgetUtils;
import org.vaadin.netbeans.utils.JavaUtils;

/**
 * @author denis
 */
class VaadinComponentSelectPanel extends JPanel {

    VaadinComponentSelectPanel( final ComponentSelectPanel panel ) {
        initComponents();
        myInfo = panel.getClassPathInfo();

        myComponents.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        myComponents.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged( ListSelectionEvent e ) {
                panel.fireChange();
            }
        });
    }

    void populateExistingComponents( boolean includeBaseClass ) {
        if (myUseAbstractComponent ^ includeBaseClass) {
            myUseAbstractComponent = includeBaseClass;
            ListModel<?> oldModel = myComponents.getModel();
            if (oldModel instanceof DocumentListener) {
                myFilter.getDocument().removeDocumentListener(
                        (DocumentListener) oldModel);
            }
            myComponents.setModel(new WaitModel());

            ComponentsInitializer worker = new ComponentsInitializer(myInfo);
            worker.execute();
        }
    }

    String getSelectedComponent() {
        if (myComponents.getModel() instanceof WaitModel) {
            return null;
        }
        Object selected = myComponents.getSelectedValue();
        if (selected != null) {
            return selected.toString();
        }
        return null;
    }

    void setTitle( String title ) {
        panel.setBorder(BorderFactory.createTitledBorder(title));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        filterLabel = new javax.swing.JLabel();
        myFilter = new javax.swing.JTextField();
        panel = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();
        myComponents = new javax.swing.JList();

        org.openide.awt.Mnemonics.setLocalizedText(filterLabel,
                org.openide.util.NbBundle.getMessage(
                        VaadinComponentSelectPanel.class, "LBL_Filter")); // NOI18N

        panel.setBorder(javax.swing.BorderFactory
                .createTitledBorder(org.openide.util.NbBundle.getMessage(
                        VaadinComponentSelectPanel.class, "TTL_Components"))); // NOI18N

        scrollPane.setViewportView(myComponents);

        javax.swing.GroupLayout panelLayout =
                new javax.swing.GroupLayout(panel);
        panel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(panelLayout.createParallelGroup(
                javax.swing.GroupLayout.Alignment.LEADING).addComponent(
                scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 388,
                Short.MAX_VALUE));
        panelLayout.setVerticalGroup(panelLayout.createParallelGroup(
                javax.swing.GroupLayout.Alignment.LEADING).addComponent(
                scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 216,
                Short.MAX_VALUE));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(
                        layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(filterLabel)
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(myFilter).addContainerGap()));
        layout.setVerticalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(
                        layout.createSequentialGroup()
                                .addComponent(panel,
                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(filterLabel)
                                                .addComponent(
                                                        myFilter,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        Short.MAX_VALUE)));
    }// </editor-fold>//GEN-END:initComponents

    private class ComponentsInitializer extends SwingWorker<List<String>, Void>
    {

        ComponentsInitializer( ClasspathInfo info ) {
            myInfo = info;
        }

        @Override
        protected List<String> doInBackground() throws Exception {
            JavaSource javaSource = JavaSource.create(myInfo);
            if (javaSource == null) {
                return Collections.emptyList();
            }
            final List<String> result = new LinkedList<>();
            javaSource.runUserActionTask(new Task<CompilationController>() {

                @Override
                public void run( CompilationController controller )
                        throws Exception
                {
                    controller.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);

                    TypeElement baseComponent =
                            controller.getElements().getTypeElement(
                                    WidgetUtils.ABSTRACT_COMPONENT);
                    if (baseComponent == null) {
                        return;
                    }

                    Set<TypeElement> widgets =
                            JavaUtils.getSubclasses(baseComponent, controller);
                    if (myUseAbstractComponent) {
                        result.add(WidgetUtils.ABSTRACT_COMPONENT);
                    }
                    for (TypeElement widget : widgets) {
                        result.add(widget.getQualifiedName().toString());
                    }
                }
            }, true);
            return result;
        }

        @Override
        protected void done() {
            try {
                List<String> list = get();
                Collections.sort(list);
                myWidgets = list;

                ListModelImpl model = new ListModelImpl();
                myComponents.setModel(model);

                if (myUseAbstractComponent) {
                    myComponents.setSelectedValue(
                            WidgetUtils.ABSTRACT_COMPONENT, true);
                }

                myFilter.getDocument().addDocumentListener(model);
            }
            catch (InterruptedException | ExecutionException e) {
                Logger.getLogger(VaadinComponentSelectPanel.class.getName())
                        .log(Level.INFO, null, e);
            }
        }

        private ClasspathInfo myInfo;
    }

    private class ListModelImpl extends AbstractListModel<String> implements
            DocumentListener
    {

        ListModelImpl() {
            widgets = new ArrayList<>(myWidgets);
        }

        @Override
        public int getSize() {
            return widgets.size();
        }

        @Override
        public String getElementAt( int index ) {
            return widgets.get(index);
        }

        @Override
        public void changedUpdate( DocumentEvent e ) {
            filter();
        }

        @Override
        public void removeUpdate( DocumentEvent e ) {
            filter();
        }

        @Override
        public void insertUpdate( DocumentEvent e ) {
            filter();
        }

        public void filter() {
            List<String> result = new ArrayList<>(myWidgets.size());
            String search = myFilter.getText().trim();
            boolean inLowerCase = search.equals(search.toLowerCase());
            for (String widget : myWidgets) {
                if (inLowerCase) {
                    if (widget.toLowerCase().contains(search)) {
                        result.add(widget);
                    }
                }
                else {
                    if (widget.contains(search)) {
                        result.add(widget);
                    }
                }
            }
            widgets = result;
            int oldSize = getSize();
            fireIntervalRemoved(this, 0, oldSize);
            fireIntervalAdded(this, 0, getSize());
        }

        private List<String> widgets;

    };

    private class WaitModel extends AbstractListModel<String> {

        @Override
        public int getSize() {
            return 1;
        }

        @NbBundle.Messages("waitComponents=Search for available Vaadin components ...")
        @Override
        public String getElementAt( int index ) {
            return Bundle.waitComponents();
        }

    };

    private List<String> myWidgets;

    private ClasspathInfo myInfo;

    private boolean myUseAbstractComponent;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel filterLabel;

    private javax.swing.JList myComponents;

    private javax.swing.JTextField myFilter;

    private javax.swing.JPanel panel;

    private javax.swing.JScrollPane scrollPane;

    // End of variables declaration//GEN-END:variables

}
