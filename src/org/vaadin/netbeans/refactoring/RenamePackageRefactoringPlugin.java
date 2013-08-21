/**
 *
 */
package org.vaadin.netbeans.refactoring;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.modules.refactoring.api.RenameRefactoring;
import org.vaadin.netbeans.code.generator.JavaUtils;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.Tree;

/**
 * @author denis
 */
class RenamePackageRefactoringPlugin extends
        AbstractPackageRefactoringPlugin<RenameRefactoring>
{

    RenamePackageRefactoringPlugin( RenameRefactoring refactoring ) {
        super(refactoring);
    }

    @Override
    protected TransformTask getTransformTask() {
        return new RenameGwtTask();
    }

    class RenameGwtTask extends AbstractTransformTask {

        RenameGwtTask() {
            super(getAcceptor());
        }

        @Override
        protected void doUpdateWebServletAnnotation( TypeElement type,
                WorkingCopy copy, AnnotationMirror annotation,
                String widgetsetFqn )
        {
            TreeMaker treeMaker = copy.getTreeMaker();
            Tree tree = copy.getTrees().getTree(type, annotation);
            if (tree instanceof AnnotationTree) {
                AnnotationTree annotationTree = getWidgetsetWebInit((AnnotationTree) tree);
                if (annotationTree == null) {
                    return;
                }
                AnnotationTree newTree = replaceWidgetset(treeMaker,
                        annotationTree, widgetsetFqn, JavaUtils.VALUE);
                copy.rewrite(annotationTree, newTree);
            }
        }

        @Override
        protected void doUpdateVaadinServletAnnotation( TypeElement type,
                WorkingCopy copy, AnnotationMirror annotation,
                String widgetsetFqn )
        {
            TreeMaker treeMaker = copy.getTreeMaker();
            Tree tree = copy.getTrees().getTree(type, annotation);
            AnnotationTree annotationTree = null;
            if (tree instanceof AnnotationTree) {
                annotationTree = (AnnotationTree) tree;
            }
            else {
                return;
            }

            AnnotationTree newTree = replaceWidgetset(treeMaker,
                    annotationTree, widgetsetFqn, JavaUtils.WIDGETSET);

            copy.rewrite(annotationTree, newTree);
        }

        @Override
        protected String getNewWidgetsetFqn( String oldName ) {
            String postfix = oldName.substring(getPackageName().length());
            return getRefactoring().getNewName() + postfix;
        }

    }

}
