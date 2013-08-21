/**
 *
 */
package org.vaadin.netbeans.refactoring;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.modules.refactoring.api.SafeDeleteRefactoring;
import org.vaadin.netbeans.code.generator.JavaUtils;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;

/**
 * @author denis
 */
class DeletePackageRefactoringPlugin extends
        AbstractPackageRefactoringPlugin<SafeDeleteRefactoring>
{

    DeletePackageRefactoringPlugin( SafeDeleteRefactoring refactoring ) {
        super(refactoring);
    }

    @Override
    protected TransformTask getTransformTask() {
        return new DeleteGwtTask();
    }

    static void removeWebServletAnnotation( TypeElement type, WorkingCopy copy,
            AnnotationMirror annotation )
    {
        TreeMaker treeMaker = copy.getTreeMaker();
        Tree tree = copy.getTrees().getTree(type, annotation);
        if (tree instanceof AnnotationTree) {
            Tree[] trees = getDeleteWidgetChangedTrees((AnnotationTree) tree,
                    treeMaker);
            if (trees[0] == null || trees[1] == null) {
                return;
            }

            copy.rewrite(trees[0], trees[1]);
        }
    }

    static void removeVaadinServletAnnotation( TypeElement type,
            WorkingCopy copy, AnnotationMirror annotation )
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

        ExpressionTree widgetsetTree = getAnnotationTreeAttribute(
                annotationTree, JavaUtils.WIDGETSET);

        AnnotationTree newTree = treeMaker.removeAnnotationAttrValue(
                annotationTree, widgetsetTree);

        copy.rewrite(annotationTree, newTree);
    }

    static Tree[] getDeleteWidgetChangedTrees(
            AnnotationTree servletAnnotation, TreeMaker maker )
    {
        Tree oldTree = null;
        Tree newTree = null;
        ExpressionTree expressionTree = getAnnotationTreeAttribute(
                servletAnnotation, JavaUtils.INIT_PARAMS);
        if (expressionTree instanceof AssignmentTree) {
            AssignmentTree assignmentTree = (AssignmentTree) expressionTree;
            ExpressionTree expression = assignmentTree.getExpression();
            if (expression instanceof AnnotationTree) {
                AnnotationTree widgetsetTree = (AnnotationTree) expression;
                if (getWidgetset(widgetsetTree) != null) {
                    oldTree = servletAnnotation;
                    newTree = maker.removeAnnotationAttrValue(
                            servletAnnotation, expressionTree);
                }
            }
            else if (expression instanceof NewArrayTree) {
                NewArrayTree arrayTree = (NewArrayTree) expression;
                List<? extends ExpressionTree> expressions = arrayTree
                        .getInitializers();
                for (ExpressionTree webInitAnnotation : expressions) {
                    if (webInitAnnotation instanceof AnnotationTree) {
                        AnnotationTree widgetsetTree = (AnnotationTree) webInitAnnotation;
                        if (getWidgetset(widgetsetTree) != null) {
                            oldTree = arrayTree;
                            newTree = maker.removeNewArrayInitializer(
                                    arrayTree, widgetsetTree);
                            break;
                        }
                    }
                }
            }
        }
        return new Tree[] { oldTree, newTree };
    }

    class DeleteGwtTask extends AbstractTransformTask {

        DeleteGwtTask() {
            super(getAcceptor());
        }

        @Override
        protected void doUpdateWebServletAnnotation( TypeElement type,
                WorkingCopy copy, AnnotationMirror annotation,
                String widgetsetFqn )
        {
            removeWebServletAnnotation(type, copy, annotation);
        }

        @Override
        protected void doUpdateVaadinServletAnnotation( TypeElement type,
                WorkingCopy copy, AnnotationMirror annotation,
                String widgetsetFqn )
        {
            removeVaadinServletAnnotation(type, copy, annotation);
        }

    }

}
