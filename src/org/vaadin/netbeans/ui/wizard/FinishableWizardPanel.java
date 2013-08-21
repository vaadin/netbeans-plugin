/**
 *
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

    FinishableWizardPanel( WizardDescriptor.Panel<WizardDescriptor> original ) {
        delegate = original;
    }

    @Override
    public boolean isFinishPanel() {
        return true;
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
    }

    @Override
    public void removeChangeListener( ChangeListener listener ) {
        delegate.removeChangeListener(listener);
    }

    private final WizardDescriptor.Panel<WizardDescriptor> delegate;
}
