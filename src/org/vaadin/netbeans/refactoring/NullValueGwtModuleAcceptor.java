package org.vaadin.netbeans.refactoring;

import org.vaadin.netbeans.refactoring.AbstractRefactoringPlugin.GwtModuleAcceptor;

/**
 * @author denis
 */
public class NullValueGwtModuleAcceptor implements GwtModuleAcceptor {

    @Override
    public boolean accept( String moduleFqn ) {
        return moduleFqn == null || moduleFqn.trim().length() == 0;
    }

}
