/**
 *
 */
package org.vaadin.netbeans.refactoring;

import org.netbeans.modules.refactoring.api.SafeDeleteRefactoring;
import org.openide.filesystems.FileObject;

/**
 * @author denis
 */
class DeleteRefactoringPlugin extends DeletePackageRefactoringPlugin {

    DeleteRefactoringPlugin( SafeDeleteRefactoring refactoring ) {
        super(refactoring);
    }

    @Override
    protected GwtModuleAcceptor getAcceptor() {
        FileObject gwtXml = getRefactoring().getRefactoringSource().lookup(
                FileObject.class);
        return new GwtNameAcceptor(gwtXml);
    }

}
