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

import javax.swing.event.ChangeListener;

import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.api.RenameRefactoring;
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI;
import org.openide.filesystems.FileObject;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;
import org.vaadin.netbeans.utils.XmlUtils;

/**
 * @author denis
 */
class RenameRefactoringUI implements RefactoringUI {

    RenameRefactoringUI( FileObject gwtXml ) {
        myRefactoring = new RenameRefactoring(Lookups.singleton(gwtXml));
    }

    @Override
    public Problem checkParameters() {
        myRefactoring.setNewName(myPanel.getNewName());
        return myRefactoring.fastCheckParameters();
    }

    @NbBundle.Messages({ "# {0} - oldName", "# {1} - newName",
            "renameDescription=Rename GWT Module {0} to {1}" })
    @Override
    public String getDescription() {
        return Bundle.renameDescription(getModuleName(), myPanel.getNewName());
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @NbBundle.Messages("rename=Rename")
    @Override
    public String getName() {
        return Bundle.rename();
    }

    @Override
    public CustomRefactoringPanel getPanel( ChangeListener listener ) {
        if (myPanel == null) {
            myPanel = new RenamePanel(getModuleName(), listener);
        }
        return myPanel;
    }

    @Override
    public AbstractRefactoring getRefactoring() {
        return myRefactoring;
    }

    @Override
    public boolean hasParameters() {
        return true;
    }

    @Override
    public boolean isQuery() {
        return false;
    }

    @Override
    public Problem setParameters() {
        myRefactoring.setNewName(myPanel.getNewName() + XmlUtils.GWT);
        return myRefactoring.checkParameters();
    }

    private String getModuleName() {
        String name = myRefactoring.getRefactoringSource()
                .lookup(FileObject.class).getName();
        if (name.endsWith(XmlUtils.GWT)) {
            // Should always happen
            name = name.substring(0, name.length() - XmlUtils.GWT.length());
        }
        return name;
    }

    private RenamePanel myPanel;

    private final RenameRefactoring myRefactoring;

}
