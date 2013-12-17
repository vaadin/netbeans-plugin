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

import java.beans.PropertyChangeListener;

import javax.swing.JComponent;

import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

/**
 * @author denis
 */
@OptionsPanelController.SubRegistration(location = "Java", id = "Vaadin",
        displayName = "#Vaadin", keywords = "#VaadinKeywords",
        keywordsCategory = "Java/Vaadin")
public class VaadinPanelController extends OptionsPanelController {

    @Override
    public void addPropertyChangeListener( PropertyChangeListener listener ) {
        getCustomizer().addPropertyChangeListener(listener);
    }

    @Override
    public void applyChanges() {
        getCustomizer().applyChanges();
    }

    @Override
    public void cancel() {
        getCustomizer().cancel();
    }

    @Override
    public JComponent getComponent( Lookup arg0 ) {
        return getCustomizer();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @Override
    public boolean isChanged() {
        return getCustomizer().isChanged();
    }

    @Override
    public boolean isValid() {
        return getCustomizer().valid();
    }

    @Override
    public void removePropertyChangeListener( PropertyChangeListener listener )
    {
        getCustomizer().removePropertyChangeListener(listener);
    }

    @Override
    public void update() {
        getCustomizer().update();
    }

    private VaadinCustomizer getCustomizer() {
        if (myCustomizer == null) {
            myCustomizer = new VaadinCustomizer();
        }
        return myCustomizer;
    }

    private VaadinCustomizer myCustomizer;

}
