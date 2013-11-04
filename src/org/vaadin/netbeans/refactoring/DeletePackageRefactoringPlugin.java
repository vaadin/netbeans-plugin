/*
 * Copyright 2000-2013 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.vaadin.netbeans.refactoring;

import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.project.Project;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.api.SafeDeleteRefactoring;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import org.openide.filesystems.FileObject;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.utils.JavaUtils;

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

    @Override
    protected Problem createAndAddConfigElements(
            Set<FileObject> additionalFiles, RefactoringElementsBag bag )
    {
        return setWidgetsetAddon(bag);
    }

    protected Problem setWidgetsetAddon( RefactoringElementsBag bag ) {
        Project project = getProject();
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        if (support == null || support.isWeb()) {
            return null;
        }
        List<String> widgetsets = support.getAddonWidgetsets();
        if (widgetsets == null || widgetsets.size() != 1) {
            return null;
        }
        if (getAcceptor().accept(widgetsets.get(0))) {
            return bag.add(
                    getRefactoring(),
                    new AddOnRefactoringElementImplementation(support, support
                            .getAddonWidgetsets(), null));
        }
        else {
            return null;
        }
    }

    static void removeWebServletAnnotation( TypeElement type, WorkingCopy copy,
            AnnotationMirror annotation )
    {
        TreeMaker treeMaker = copy.getTreeMaker();
        Tree tree = copy.getTrees().getTree(type, annotation);
        if (tree instanceof AnnotationTree) {
            Tree[] trees =
                    getDeleteWidgetChangedTrees((AnnotationTree) tree,
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

        ExpressionTree widgetsetTree =
                getAnnotationTreeAttribute(annotationTree, JavaUtils.WIDGETSET);

        AnnotationTree newTree =
                treeMaker.removeAnnotationAttrValue(annotationTree,
                        widgetsetTree);

        copy.rewrite(annotationTree, newTree);
    }

    static Tree[] getDeleteWidgetChangedTrees(
            AnnotationTree servletAnnotation, TreeMaker maker )
    {
        Tree oldTree = null;
        Tree newTree = null;
        ExpressionTree expressionTree =
                getAnnotationTreeAttribute(servletAnnotation,
                        JavaUtils.INIT_PARAMS);
        if (expressionTree instanceof AssignmentTree) {
            AssignmentTree assignmentTree = (AssignmentTree) expressionTree;
            ExpressionTree expression = assignmentTree.getExpression();
            if (expression instanceof AnnotationTree) {
                AnnotationTree widgetsetTree = (AnnotationTree) expression;
                if (getWidgetset(widgetsetTree) != null) {
                    oldTree = servletAnnotation;
                    newTree =
                            maker.removeAnnotationAttrValue(servletAnnotation,
                                    expressionTree);
                }
            }
            else if (expression instanceof NewArrayTree) {
                NewArrayTree arrayTree = (NewArrayTree) expression;
                List<? extends ExpressionTree> expressions =
                        arrayTree.getInitializers();
                for (ExpressionTree webInitAnnotation : expressions) {
                    if (webInitAnnotation instanceof AnnotationTree) {
                        AnnotationTree widgetsetTree =
                                (AnnotationTree) webInitAnnotation;
                        if (getWidgetset(widgetsetTree) != null) {
                            oldTree = arrayTree;
                            newTree =
                                    maker.removeNewArrayInitializer(arrayTree,
                                            widgetsetTree);
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
