/**
 *
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
