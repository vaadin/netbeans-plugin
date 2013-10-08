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

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.openide.filesystems.FileObject;

/**
 * @author denis
 */
abstract class AbstractPackageRefactoringPlugin<R extends AbstractRefactoring>
        extends AbstractRefactoringPlugin<R>
{

    AbstractPackageRefactoringPlugin( R refactoring ) {
        super(refactoring);
    }

    protected String getPackageName() {
        FileObject folder = getFolder();
        ClassPath classPath = ClassPath.getClassPath(folder, ClassPath.SOURCE);
        return classPath.getResourceName(folder, '.', false);
    }

    @Override
    protected GwtModuleAcceptor getAcceptor() {
        return new PackageNameAcceptor();
    }

    class PackageNameAcceptor implements GwtModuleAcceptor {

        @Override
        public boolean accept( String moduleFqn ) {
            return moduleFqn != null
                    && moduleFqn.startsWith(getPackageName() + '.');
        }
    }
}
