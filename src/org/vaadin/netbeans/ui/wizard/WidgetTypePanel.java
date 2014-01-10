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

import java.awt.Component;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.VaadinSupport;

/**
 * @author denis
 */
class WidgetTypePanel implements WizardDescriptor.Panel<WizardDescriptor> {

    enum Template {
        FULL_FLEDGED,
        CONNECTOR_ONLY,
        EXTENSION
    }

    @Override
    public Component getComponent() {
        if (myComponent == null) {
            myComponent = new WidgetTemplatePanel(this);
        }
        return myComponent;
    }

    @Override
    public HelpCtx getHelp() {
        return null;
    }

    @Override
    public void readSettings( WizardDescriptor settings ) {
        myDescriptor = settings;
    }

    @Override
    public void storeSettings( WizardDescriptor settings ) {
        if (myDescriptor != null) {
            myDescriptor.putProperty(NewWidgetWizardIterator.SELECTED_TEMPLATE,
                    getSelectedTemplate());
        }
    }

    @NbBundle.Messages("noVaadinSupport=Chosen project has no Vaadin support.")
    @Override
    public boolean isValid() {
        if (myDescriptor == null) {
            return true;
        }
        Project project = Templates.getProject(myDescriptor);
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        if (support != null && support.isEnabled()) {
            return true;
        }
        else {
            myDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE,
                    Bundle.noVaadinSupport());
            return false;
        }
    }

    @Override
    public void addChangeListener( ChangeListener listener ) {
        myListeners.add(listener);
    }

    @Override
    public void removeChangeListener( ChangeListener listener ) {
        myListeners.remove(listener);
    }

    void fireChange() {
        for (ChangeListener listener : myListeners) {
            listener.stateChanged(new ChangeEvent(this));
        }
    }

    boolean isConfigured() {
        return !getSelectedTemplate().equals(Template.EXTENSION);
    }

    Template getSelectedTemplate() {
        return myComponent.getSelectedTemplate();
    }

    private List<ChangeListener> myListeners = new CopyOnWriteArrayList<>();

    private WidgetTemplatePanel myComponent;

    private WizardDescriptor myDescriptor;

}
