/**
 *
 */
package org.vaadin.netbeans.editor.analyzer;

import java.util.logging.Level;

import javax.lang.model.element.AnnotationMirror;
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
import org.vaadin.netbeans.code.generator.JavaUtils;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

/**
 * @author denis
 */
class SetServletWidgetsetFix extends AbstractSetWebInitParamFix {

    SetServletWidgetsetFix( String widgetset, FileObject fileObject,
            ElementHandle<TypeElement> handle )
    {
        super(fileObject, handle);
        myWidgetset = widgetset;
    }

    @Override
    public String getText() {
        return Bundle.setWidgetsetName(myWidgetset);
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

                        TypeElement clazz = getHandle().resolve(copy);
                        if (clazz == null) {
                            return;
                        }

                        AnnotationMirror servlet = JavaUtils.getAnnotation(
                                clazz, JavaUtils.SERVLET_ANNOTATION);
                        if (servlet == null) {
                            return;
                        }

                        TreeMaker treeMaker = copy.getTreeMaker();
                        Tree tree = copy.getTrees().getTree(clazz, servlet);
                        Tree oldTree = null;
                        Tree newTree = null;
                        if (tree instanceof AnnotationTree) {
                            AnnotationTree annotationTree = (AnnotationTree) tree;
                            ExpressionTree expressionTree = getAnnotationTreeAttribute(
                                    annotationTree, JavaUtils.INIT_PARAMS);
                            if (expressionTree instanceof AssignmentTree) {
                                Tree[] trees = getWebInitChangedTree(
                                        expressionTree, treeMaker,
                                        JavaUtils.WIDGETSET, myWidgetset);
                                oldTree = trees[0];
                                newTree = trees[1];
                            }
                        }
                        if (oldTree != null && newTree != null) {
                            copy.rewrite(oldTree, newTree);
                        }
                    }
                });
        ChangeInfo changeInfo = createChangeInfo(task);
        task.commit();
        return changeInfo;
    }

    private final String myWidgetset;

}
