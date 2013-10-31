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
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

/**
 * @author denis
 */
class FinishableWizardPanel implements
        WizardDescriptor.FinishablePanel<WizardDescriptor>
{

    FinishableWizardPanel( WizardDescriptor.Panel<WizardDescriptor> original,
            WidgetTypePanel panel )
    {
        delegate = original;
        myOptionalPanel = panel;
    }

    @Override
    public boolean isFinishPanel() {
        return myOptionalPanel.isConfigured();
    }

    @Override
    public Component getComponent() {
        return delegate.getComponent();
    }

    @Override
    public HelpCtx getHelp() {
        return delegate.getHelp();
    }

    @Override
    public void readSettings( WizardDescriptor settings ) {
        delegate.readSettings(settings);
    }

    @Override
    public void storeSettings( WizardDescriptor settings ) {
        delegate.storeSettings(settings);
    }

    @Override
    public boolean isValid() {
        return delegate.isValid();
    }

    @Override
    public void addChangeListener( ChangeListener listener ) {
        delegate.addChangeListener(listener);
        myOptionalPanel.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener( ChangeListener listener ) {
        delegate.removeChangeListener(listener);
        myOptionalPanel.removeChangeListener(listener);
    }

    private final WizardDescriptor.Panel<WizardDescriptor> delegate;

    private final WidgetTypePanel myOptionalPanel;
}
