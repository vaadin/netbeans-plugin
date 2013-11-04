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

import javax.swing.event.ChangeListener;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.utils.XmlUtils;

/**
 * @author denis
 */
abstract class AbstractRefactoringUI<R extends AbstractRefactoring> implements
        RefactoringUI
{

    AbstractRefactoringUI( FileObject gwtXml, FileObject target ) {
        myRefactoring = createRefactoring(gwtXml);
        myTarget = target;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @NbBundle.Messages({ "# {0} - module name",
            "moveModule=Move GWT Module {0}", "copyModule=Copy GWT Module {0}" })
    @Override
    public CustomRefactoringPanel getPanel( ChangeListener listener ) {
        if (myPanel == null) {
            FileObject source = myRefactoring.getRefactoringSource().lookup(
                    FileObject.class);
            String title;
            String moduleName = getModuleName();
            if (isCopy()) {
                title = Bundle.copyModule(moduleName);
            }
            else {
                title = Bundle.moveModule(moduleName);
            }
            Project project;
            FileObject target = myTarget;
            if (target == null) {
                target = source.getParent();
                project = FileOwnerQuery.getOwner(source);
            }
            else {
                project = FileOwnerQuery.getOwner(target);
            }
            myPanel = new MovePanel(moduleName, project, target, listener,
                    isCopy(), title);
        }
        return myPanel;
    }

    @Override
    public R getRefactoring() {
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
        return setParameters(false);
    }

    @Override
    public Problem checkParameters() {
        return setParameters(false);
    }

    protected abstract boolean isCopy();

    protected MovePanel getPanel() {
        return myPanel;
    }

    protected Problem setParameters( boolean check ) {
        String targetPackage = getPanel().getTargetPackage();

        SourceGroup targeGroup = getPanel().getTargetSourceGroup();
        FileObject targetRoot = targeGroup.getRootFolder();
        Project targetProject = FileOwnerQuery.getOwner(targetRoot);

        File file = FileUtil.toFile(targeGroup.getRootFolder());
        file = new File(file, targetPackage.replace('.', '/'));

        if (isCopy()) {
            file = new File(file, getPanel().getTargetName() + XmlUtils.GWT_XML);
        }

        setTarget(file, targetProject);

        if (check) {
            return getRefactoring().fastCheckParameters();
        }
        else {
            return getRefactoring().checkParameters();
        }
    }

    protected String getModuleName() {
        String name = myRefactoring.getRefactoringSource()
                .lookup(FileObject.class).getName();
        if (name.endsWith(XmlUtils.GWT)) {
            // Should always happen
            name = name.substring(0, name.length() - XmlUtils.GWT.length());
        }
        return name;
    }

    protected abstract R createRefactoring( FileObject gwtXml );

    protected abstract void setTarget( File file, Project project );

    private final R myRefactoring;

    private final FileObject myTarget;

    private MovePanel myPanel;
}
