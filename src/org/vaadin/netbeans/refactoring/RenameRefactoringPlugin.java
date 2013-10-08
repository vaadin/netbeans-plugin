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
import java.io.IOException;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.api.RenameRefactoring;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.code.generator.XmlUtils;

/**
 * @author denis
 */
class RenameRefactoringPlugin extends RenamePackageRefactoringPlugin {

    RenameRefactoringPlugin( RenameRefactoring refactoring ) {
        super(refactoring);
    }

    @Override
    public Problem checkParameters() {
        return doCheckParameters();
    }

    @Override
    public Problem fastCheckParameters() {
        return doCheckParameters();
    }

    @Override
    protected GwtNameAcceptor getAcceptor() {
        return new GwtNameAcceptor(getRefactoring().getRefactoringSource()
                .lookup(FileObject.class));
    }

    @Override
    protected TransformTask getTransformTask() {
        return new RenameGwtTransformTask();
    }

    @NbBundle.Messages({
            "# {0} - file",
            "# {1} - folder",
            "fileAlreadyExists=GWT Module file {0} already exists in package {1}",
            "invalidFileName=Invalid file name {0}" })
    private Problem doCheckParameters() {
        FileObject fileObject = getRefactoring().getRefactoringSource().lookup(
                FileObject.class);
        FileObject folder = fileObject.getParent();

        ClassPath classPath = ClassPath.getClassPath(folder, ClassPath.SOURCE);
        String pkg = classPath.getResourceName(folder, '.', true);

        String newName = getRefactoring().getNewName() + XmlUtils.GWT_XML;
        if (folder.getFileObject(newName) == null) {
            File file = new File(FileUtil.toFile(folder), newName);
            try {
                file.getCanonicalPath();
            }
            catch (IOException e) {
                return new Problem(true, Bundle.invalidFileName(newName));
            }
            return null;
        }
        else {
            return new Problem(true, Bundle.fileAlreadyExists(newName, pkg));
        }
    }

    class RenameGwtTransformTask extends RenameGwtTask {

        @Override
        protected String getNewWidgetsetFqn( String oldFqn ) {
            FileObject gwtXml = getRefactoring().getRefactoringSource().lookup(
                    FileObject.class);
            String name = gwtXml.getNameExt();
            if (name.endsWith(XmlUtils.GWT_XML)) {
                name = name.substring(0,
                        name.length() - XmlUtils.GWT_XML.length());
            }
            String prefix = oldFqn
                    .substring(0, oldFqn.length() - name.length());
            String newName = getRefactoring().getNewName();
            if (newName.endsWith(XmlUtils.GWT)) {
                // should always happen
                newName = newName.substring(0,
                        newName.length() - XmlUtils.GWT.length());
            }
            return prefix + newName;
        }
    }
}
