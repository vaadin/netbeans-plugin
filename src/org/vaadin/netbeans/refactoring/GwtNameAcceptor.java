/**
 *
 */
package org.vaadin.netbeans.refactoring;

import org.netbeans.api.java.classpath.ClassPath;
import org.openide.filesystems.FileObject;
import org.vaadin.netbeans.code.generator.XmlUtils;
import org.vaadin.netbeans.refactoring.AbstractRefactoringPlugin.GwtModuleAcceptor;

/**
 * @author denis
 */
class GwtNameAcceptor implements GwtModuleAcceptor {

    GwtNameAcceptor( FileObject gwtXml ) {
        if (gwtXml == null) {
            return;
        }
        ClassPath classPath = ClassPath.getClassPath(gwtXml, ClassPath.SOURCE);
        String name = classPath.getResourceName(gwtXml, '.', true);
        if (name.endsWith(XmlUtils.GWT_XML)) {
            myGwtXmlFqn = name.substring(0,
                    name.length() - XmlUtils.GWT_XML.length());
        }
    }

    @Override
    public boolean accept( String moduleFqn ) {
        return moduleFqn != null && moduleFqn.equals(myGwtXmlFqn);
    }

    String getGwtFqn() {
        return myGwtXmlFqn;
    }

    private String myGwtXmlFqn;

}
