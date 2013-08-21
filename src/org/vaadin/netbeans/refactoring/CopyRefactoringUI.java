/**
 *
 */
package org.vaadin.netbeans.refactoring;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.netbeans.api.project.Project;
import org.netbeans.modules.refactoring.api.SingleCopyRefactoring;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.lookup.Lookups;
import org.vaadin.netbeans.code.generator.XmlUtils;

/**
 * @author denis
 */
class CopyRefactoringUI extends AbstractRefactoringUI<SingleCopyRefactoring> {

    CopyRefactoringUI( FileObject gwtXml, FileObject target ) {
        super(gwtXml, target);
    }

    @NbBundle.Messages({ "# {0} - oldName", "# {1} -newName",
            "copyDescription=Copy GWT Module {0} to {1}" })
    @Override
    public String getDescription() {
        return Bundle.copyDescription(getModuleName(), getPanel()
                .getTargetPackage() + '.' + getPanel().getTargetName());
    }

    @NbBundle.Messages("copy=Copy")
    @Override
    public String getName() {
        return Bundle.copy();
    }

    @Override
    protected boolean isCopy() {
        return true;
    }

    @Override
    protected SingleCopyRefactoring createRefactoring( FileObject gwtXml ) {
        return new SingleCopyRefactoring(Lookups.singleton(gwtXml));
    }

    @Override
    protected void setTarget( File file, Project project ) {
        URL url = null;
        try {
            url = Utilities.toURI(file.getParentFile()).toURL();
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
        String name = file.getName();
        name = name.substring(0, name.length() - XmlUtils.GWT_XML.length());
        getRefactoring().setNewName(name + XmlUtils.GWT);
        getRefactoring().setTarget(lookup);
    }
}
