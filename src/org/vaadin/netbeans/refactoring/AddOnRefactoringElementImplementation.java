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
package org.vaadin.netbeans.refactoring;

import java.util.Collections;
import java.util.List;

import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.modules.refactoring.spi.SimpleRefactoringElementImplementation;
import org.openide.filesystems.FileObject;
import org.openide.text.PositionBounds;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.VaadinSupport;

class AddOnRefactoringElementImplementation extends
        SimpleRefactoringElementImplementation
{

    AddOnRefactoringElementImplementation( VaadinSupport support,
            List<String> currentWidgetset, String newWidgetset )
    {
        mySupport = support;
        myOldWidgetsets = currentWidgetset;
        myNewWidgetset = newWidgetset;
    }

    AddOnRefactoringElementImplementation( VaadinSupport support,
            List<String> widgetsets )
    {
        mySupport = support;
        myOldWidgetsets = widgetsets;
        myNewWidgetset = null;
    }

    @NbBundle.Messages({ "# {0} - widgetset",
            "setWidgetset=Set Vaadin-Widgetsets manifest entry to ''{0}''",
            "removeWidgetset=Remove Vaadin-Widgetsets manifest entry" })
    @Override
    public String getDisplayText() {
        if (myNewWidgetset == null) {
            return Bundle.removeWidgetset();
        }
        return Bundle.setWidgetset(myNewWidgetset);
    }

    @Override
    public Lookup getLookup() {
        return Lookup.EMPTY;
    }

    @Override
    @NonNull
    public FileObject getParentFile() {
        return mySupport.getAddOnConfigFile();
    }

    @Override
    public PositionBounds getPosition() {
        return null;
    }

    @Override
    public String getText() {
        return getDisplayText();
    }

    @Override
    public void performChange() {
        mySupport.setAddonWidgetsets(Collections.singletonList(myNewWidgetset));
    }

    @Override
    public void undoChange() {
        mySupport.setAddonWidgetsets(myOldWidgetsets);
    }

    private List<String> myOldWidgetsets;

    private String myNewWidgetset;

    private VaadinSupport mySupport;
}