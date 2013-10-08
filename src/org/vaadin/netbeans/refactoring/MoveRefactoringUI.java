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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.netbeans.api.project.Project;
import org.netbeans.modules.refactoring.api.MoveRefactoring;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.lookup.Lookups;

/**
 * @author denis
 */
class MoveRefactoringUI extends AbstractRefactoringUI<MoveRefactoring> {

    MoveRefactoringUI( FileObject gwtXml, FileObject target ) {
        super(gwtXml, target);
    }

    @NbBundle.Messages({ "# {0} - oldName", "# {1} - package",
            "moveDescription=Move GWT Module {0} to {1}" })
    @Override
    public String getDescription() {
        return Bundle.moveDescription(getModuleName(), getPanel()
                .getTargetPackage());
    }

    @NbBundle.Messages("move=Move")
    @Override
    public String getName() {
        return Bundle.move();
    }

    @Override
    protected MoveRefactoring createRefactoring( FileObject gwtXml ) {
        return new MoveRefactoring(Lookups.singleton(gwtXml));
    }

    @Override
    protected boolean isCopy() {
        return false;
    }

    @Override
    protected void setTarget( File file, Project project ) {
        URL url = null;
        try {
            url = Utilities.toURI(file).toURL();
        }
        catch (MalformedURLException e) {
            Logger.getLogger(CopyRefactoringUI.class.getName()).log(Level.INFO,
                    null, e);
        }
        Lookup lookup;
        if (url == null) {
            lookup = Lookups.fixed(file, project);
        }
        else {
            lookup = Lookups.fixed(file, project, url);
        }
        getRefactoring().setTarget(lookup);
    }

}
