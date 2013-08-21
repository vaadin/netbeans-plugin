/**
 *
 */
package org.vaadin.netbeans.editor.analyzer;

import java.util.logging.Level;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.code.generator.JavaUtils;

import com.sun.source.tree.ClassTree;

/**
 * @author denis
 */
class ExtendVaadinServletFix extends AbstractJavaFix {

    ExtendVaadinServletFix( FileObject fileObject,
            ElementHandle<TypeElement> handle )
    {
        super(fileObject);
        myHandle = handle;
    }

    @NbBundle.Messages("extendServlet=Derive servlet class from VaadinServlet")
    @Override
    public String getText() {
        return Bundle.extendServlet();
    }

    @Override
    public ChangeInfo implement() throws Exception {
        JavaSource javaSource = JavaSource.forFileObject(getFileObject());
        if (javaSource == null) {
            getLogger().log(Level.WARNING, "JavaSource is null for file {0}",
                    getFileObject().getPath());
            return null;
        }
        ModificationResult task = javaSource
                .runModificationTask(new Task<WorkingCopy>() {

                    @Override
                    public void run( WorkingCopy copy ) throws Exception {
                        copy.toPhase(Phase.ELEMENTS_RESOLVED);

                        TypeElement clazz = myHandle.resolve(copy);
                        if (clazz == null) {
                            return;
                        }
                        ClassTree oldTree = copy.getTrees().getTree(clazz);
                        if (oldTree == null) {
                            return;
                        }

                        TreeMaker treeMaker = copy.getTreeMaker();
                        ClassTree newTree = treeMaker.setExtends(oldTree,
                                treeMaker.QualIdent(JavaUtils.VAADIN_SERVLET));
                        copy.rewrite(oldTree, newTree);
                    }
                });
        ChangeInfo info = createChangeInfo(task);
        task.commit();
        return info;
    }

    private ElementHandle<TypeElement> myHandle;

}
