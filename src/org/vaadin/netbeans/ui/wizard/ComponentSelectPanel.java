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

import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.code.generator.WidgetGenerator;

/**
 * @author denis
 */
class ComponentSelectPanel implements WizardDescriptor.Panel<WizardDescriptor> {

    ComponentSelectPanel( WizardDescriptor wizard ) {
        Project project = Templates.getProject(wizard);
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        myInfo = support.getClassPathInfo();
    }

    @NbBundle.Messages("additionalConfiguration=Additional Configuration")
    @Override
    public Component getComponent() {
        if (myComponent == null) {
            myComponent = new VaadinComponentSelectPanel(this);
            myComponent.setName(Bundle.additionalConfiguration());
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
        settings.putProperty(WidgetGenerator.COMPONENT_PROPERTY,
                myComponent.getSelectedComponent());
    }

    @NbBundle.Messages("noSelectedComponent=Select a component from list.")
    @Override
    public boolean isValid() {
        if (myDescriptor == null) {
            return true;
        }
        if (myComponent.getSelectedComponent() == null) {
            myDescriptor.putProperty(WizardDescriptor.PROP_INFO_MESSAGE,
                    Bundle.noSelectedComponent());
            return false;
        }
        return true;
    }

    @Override
    public void addChangeListener( ChangeListener listener ) {
        myListeners.add(listener);
    }

    @Override
    public void removeChangeListener( ChangeListener listener ) {
        myListeners.remove(listener);
    }

    ClasspathInfo getClassPathInfo() {
        return myInfo;
    }

    void fireChange() {
        for (ChangeListener listener : myListeners) {
            listener.stateChanged(new ChangeEvent(this));
        }
    }

    private List<ChangeListener> myListeners = new CopyOnWriteArrayList<>();

    private VaadinComponentSelectPanel myComponent;

    private WizardDescriptor myDescriptor;

    private ClasspathInfo myInfo;

}
