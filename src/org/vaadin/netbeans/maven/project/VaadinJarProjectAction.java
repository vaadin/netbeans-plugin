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
package org.vaadin.netbeans.maven.project;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.netbeans.api.project.Project;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter.Popup;

/**
 * @author denis
 */
public class VaadinJarProjectAction extends VaadinAction implements Popup {

    public VaadinJarProjectAction( Lookup lookup, Project project ) {
        super(lookup, project);
    }

    @Override
    public JMenuItem getPopupPresenter() {
        if (isEnabled()) {
            JMenu menu = new JMenu(Bundle.vaadin());

            menu.add(createAddonsBrowserItem());

            menu.setEnabled(isEnabled());
            return menu;
        }
        return null;
    }

}
