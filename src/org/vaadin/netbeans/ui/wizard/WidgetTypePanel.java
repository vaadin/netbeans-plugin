/**
 *
 */
package org.vaadin.netbeans.ui.wizard;

import java.awt.Component;

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
        FULL_FLEDGED, CONNECTOR_ONLY, EXTENSION
    }

    @Override
    public Component getComponent() {
        if (myComponent == null) {
            myComponent = new WidgetTemplatePanel();
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
        //listeners.add(listener);
    }

    @Override
    public void removeChangeListener( ChangeListener listener ) {
        //listeners.remove(listener);
    }

    Template getSelectedTemplate() {
        return myComponent.getSelectedTemplate();
    }

    //private List<ChangeListener> listeners = new CopyOnWriteArrayList<ChangeListener>();
    private WidgetTemplatePanel myComponent;

    private WizardDescriptor myDescriptor;
}
