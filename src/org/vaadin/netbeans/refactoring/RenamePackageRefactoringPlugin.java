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
import org.netbeans.modules.refactoring.api.RenameRefactoring;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import org.openide.filesystems.FileObject;
import org.vaadin.netbeans.VaadinSupport;
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
        String targetWidgetset = getTargetWidgetset(support);
        if (targetWidgetset == null) {
            return null;
        }
        else {
            return bag.add(
                    getRefactoring(),
                    new AddOnRefactoringElementImplementation(support, support
                            .getAddonWidgetsets(), targetWidgetset));
        }
    }

    protected String getTargetWidgetset( VaadinSupport support ) {
        List<String> widgetsets = support.getAddonWidgetsets();
        if (widgetsets == null || widgetsets.size() != 1) {
            return null;
        }
        if (getAcceptor().accept(widgetsets.get(0))) {
            String postfix =
                    widgetsets.get(0).substring(getPackageName().length());
            return getRefactoring().getNewName() + postfix;
        }
        else {
            return null;
        }
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
                AnnotationTree annotationTree =
                        getWidgetsetWebInit((AnnotationTree) tree);
                if (annotationTree == null) {
                    return;
                }
                AnnotationTree newTree =
                        replaceWidgetset(treeMaker, annotationTree,
                                widgetsetFqn, JavaUtils.VALUE);
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

            AnnotationTree newTree =
                    replaceWidgetset(treeMaker, annotationTree, widgetsetFqn,
                            JavaUtils.WIDGETSET);

            copy.rewrite(annotationTree, newTree);
        }

        @Override
        protected String getNewWidgetsetFqn( String oldName ) {
            String postfix = oldName.substring(getPackageName().length());
            return getRefactoring().getNewName() + postfix;
        }

    }

}
