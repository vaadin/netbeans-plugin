package org.vaadin.netbeans.editor.analyzer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.code.generator.XmlUtils;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePathScanner;

/**
 * @author denis
 */
abstract class AbstractRpcFix extends AbstractJavaFix {

    AbstractRpcFix( FileObject fileObject, ElementHandle<TypeElement> handle ) {
        super(fileObject);
        myHandle = handle;
    }

    protected ElementHandle<TypeElement> getTypeHandle() {
        return myHandle;
    }

    protected FileObject getClientPackage() throws IOException {
        Project project = FileOwnerQuery.getOwner(getFileObject());
        if (project == null) {
            return null;
        }
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        if (support == null || !support.isEnabled()) {
            return null;
        }
        final FileObject[] pkg = new FileObject[1];
        support.runModelOperation(new ModelOperation() {

            @Override
            public void run( VaadinModel model ) {
                FileObject gwtXml = model.getGwtXml();
                if (gwtXml == null) {
                    return;
                }
                try {
                    for (String path : model.getSourcePaths()) {
                        FileObject clientPkg = XmlUtils.getClientWidgetPackage(
                                gwtXml, path, false);
                        if (clientPkg != null) {
                            pkg[0] = clientPkg;
                            return;
                        }
                    }
                }
                catch (IOException e) {
                    Logger.getLogger(AbstractRpcFix.class.getName()).log(
                            Level.INFO, null, e);
                }
            }
        });
        return pkg[0];
    }

    protected static class CtorScanner extends
            TreePathScanner<MethodTree, Void>
    {

        private static final String THIS = "this";

        private static final String SUPER = "super";

        @Override
        public MethodTree visitMethodInvocation( MethodInvocationTree tree,
                Void p )
        {
            ExpressionTree methodSelect = tree.getMethodSelect();
            if (methodSelect instanceof IdentifierTree) {
                Name name = ((IdentifierTree) methodSelect).getName();
                if (THIS.contentEquals(name)) {
                    hasThis = true;
                }
                else if (SUPER.contentEquals(name)) {
                    hasSuper = true;
                }
            }
            return super.visitMethodInvocation(tree, p);
        }

        boolean hasThis() {
            return hasThis;
        }

        boolean hasSuper() {
            return hasSuper;
        }

        private boolean hasThis;

        private boolean hasSuper;

    }

    private ElementHandle<TypeElement> myHandle;
}
