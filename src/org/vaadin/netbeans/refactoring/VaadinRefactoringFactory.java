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

import org.netbeans.api.fileinfo.NonRecursiveFolder;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.MoveRefactoring;
import org.netbeans.modules.refactoring.api.RenameRefactoring;
import org.netbeans.modules.refactoring.api.SafeDeleteRefactoring;
import org.netbeans.modules.refactoring.api.SingleCopyRefactoring;
import org.netbeans.modules.refactoring.spi.RefactoringPlugin;
import org.netbeans.modules.refactoring.spi.RefactoringPluginFactory;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.utils.XmlUtils;

/**
 * @author denis
 */
@ServiceProvider(service = RefactoringPluginFactory.class)
public class VaadinRefactoringFactory implements RefactoringPluginFactory {

    @Override
    public RefactoringPlugin createInstance( AbstractRefactoring refactoring ) {

        NonRecursiveFolder pkg = refactoring.getRefactoringSource().lookup(
                NonRecursiveFolder.class);
        if (pkg != null) {
            Project project = FileOwnerQuery.getOwner(pkg.getFolder());
            if (project == null) {
                return null;
            }
            VaadinSupport support = project.getLookup().lookup(
                    VaadinSupport.class);
            if (support == null || !support.isEnabled()) {
                return null;
            }
            if (refactoring instanceof RenameRefactoring) {
                return new RenamePackageRefactoringPlugin(
                        (RenameRefactoring) refactoring);
            }
            else if (refactoring instanceof SafeDeleteRefactoring) {
                return new DeletePackageRefactoringPlugin(
                        (SafeDeleteRefactoring) refactoring);
            }
        }

        FileObject fileObject = refactoring.getRefactoringSource().lookup(
                FileObject.class);
        if (fileObject == null) {
            return null;
        }
        Project project = FileOwnerQuery.getOwner(fileObject);
        if (project == null) {
            return null;
        }
        if (!fileObject.getNameExt().endsWith(XmlUtils.GWT_XML)) {
            return null;
        }
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        if (support == null || !support.isEnabled()) {
            return null;
        }

        if (refactoring instanceof RenameRefactoring) {
            return new RenameRefactoringPlugin((RenameRefactoring) refactoring);
        }
        else if (refactoring instanceof MoveRefactoring) {
            return new MoveRefactoringPlugin((MoveRefactoring) refactoring);
        }
        else if (refactoring instanceof SingleCopyRefactoring) {
            return new CopyRefactoringPlugin(
                    (SingleCopyRefactoring) refactoring);
        }
        else if (refactoring instanceof SafeDeleteRefactoring) {
            return new DeleteRefactoringPlugin(
                    (SafeDeleteRefactoring) refactoring);
        }

        return null;
    }
}
