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

import java.io.File;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.refactoring.api.MoveRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import org.openide.filesystems.FileObject;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.code.generator.JavaUtils;

/**
 * @author denis
 */
class MoveRefactoringPlugin extends
        AbstractCopyRefactoringPlugin<MoveRefactoring>
{

    MoveRefactoringPlugin( MoveRefactoring refactoring ) {
        super(refactoring);
    }

    @Override
    protected Problem setWidgetsetAddon( RefactoringElementsBag bag ) {
        Project project = getTargetProject();
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        if (support == null || support.isWeb()) {
            return null;
        }
        List<String> widgetsets = support.getAddonWidgetsets();
        if (widgetsets != null && project.equals(getProject())
                && widgetsets.size() == 1
                && getAcceptor().accept(widgetsets.get(0)))
        {
            return bag.add(getRefactoring(),
                    new AddOnRefactoringElementImplementation(support,
                            widgetsets, getTargetWidgetset()));
        }
        else {
            return super.setWidgetsetAddon(bag);
        }
    }

    @Override
    protected File getTarget() {
        File target = getRefactoring().getTarget().lookup(File.class);
        if (target == null) {
            return null;
        }

        return new File(target, getGwtNameExt());
    }

    @Override
    protected GwtModuleAcceptor getAcceptor() {
        return new GwtNameAcceptor(getRefactoring().getRefactoringSource()
                .lookup(FileObject.class));
    }

    @Override
    protected boolean acceptServletWidgetAnnoation( TypeElement type,
            CompilationController controller, GwtModuleAcceptor acceptor,
            Project project )
    {
        if (project.equals(getProject())) {
            return super.acceptServletWidgetAnnoation(type, controller,
                    acceptor, project);
        }
        else {
            return acceptMoveTarget(type, controller);
        }
    }

    @Override
    protected Set<FileObject> getAffectedJavaFiles( Project project,
            GwtModuleAcceptor acceptor )
    {
        Set<FileObject> files = super.getAffectedJavaFiles(project, acceptor);
        if (!getProject().equals(getTargetProject())) {
            files.addAll(super.getAffectedJavaFiles(getTargetProject(),
                    getTargetAcceptor()));
        }
        return files;
    }

    @Override
    protected TransformTask getTransformTask() {
        return new MoveGwtTransformTask();
    }

    @Override
    protected Project getTargetProject() {
        return getRefactoring().getTarget().lookup(Project.class);
    }

    @Override
    protected String getTargetWidgetset() {
        if (getTargetPackageFqn().length() > 0) {
            return getTargetPackageFqn() + '.' + getGwtName();
        }
        else {
            return getGwtName();
        }
    }

    private boolean acceptMoveTarget( TypeElement type,
            CompilationController controller )
    {
        return getWidgetsetPresence(type, controller) != null;
    }

    class MoveGwtTransformTask extends AbstractCopyGwtTransformTask {

        MoveGwtTransformTask() {
            super();
        }

        @Override
        protected void updateVaadinServletAnnotation( TypeElement type,
                WorkingCopy copy )
        {
            Project project = FileOwnerQuery.getOwner(copy.getFileObject());
            if (getProject().equals(getTargetProject())
                    || getProject().equals(project))
            {
                super.updateVaadinServletAnnotation(type, copy);
            }
            else {
                if (!getProject().equals(project)) {
                    // set annotation for target project
                    AnnotationMirror annotation =
                            getVaadinServletWidgetAnnotation(type,
                                    getTargetAcceptor());
                    if (annotation != null) {
                        renameVaadinServletAnnotation(type, copy, annotation,
                                null);
                    }
                }
            }
        }

        @Override
        protected void updateWebServletAnnotation( TypeElement type,
                WorkingCopy copy )
        {
            Project project = FileOwnerQuery.getOwner(copy.getFileObject());
            if (getProject().equals(getTargetProject())
                    || getProject().equals(project))
            {
                super.updateWebServletAnnotation(type, copy);
            }
            else {
                if (!getProject().equals(project)) {
                    // set annotation for target project
                    ServletWidgetsetPresence presence =
                            getWidgetsetPresence(type, copy);
                    if (presence == null) {
                        return;
                    }
                    AnnotationMirror annotation =
                            JavaUtils.getAnnotation(type,
                                    JavaUtils.SERVLET_ANNOTATION);
                    if (ServletWidgetsetPresence.EMPTY_WIDGETSET
                            .equals(presence))
                    {
                        renameWebServletAnnotation(type, copy, annotation, null);
                    }
                    else {
                        addServletWidgetsetWebInit(type, copy, annotation);
                    }
                }
            }
        }

        @Override
        protected void doUpdateWebServletAnnotation( TypeElement type,
                WorkingCopy copy, AnnotationMirror annotation,
                String widgetsetFqn )
        {
            if (getProject().equals(getTargetProject())) {
                renameWebServletAnnotation(type, copy, annotation, widgetsetFqn);
            }
            else {
                // remove annotation from the project with GWT module in the subject
                DeleteRefactoringPlugin.removeWebServletAnnotation(type, copy,
                        annotation);
            }
        }

        @Override
        protected void doUpdateVaadinServletAnnotation( TypeElement type,
                WorkingCopy copy, AnnotationMirror annotation,
                String widgetsetFqn )
        {
            if (getProject().equals(getTargetProject())) {
                renameVaadinServletAnnotation(type, copy, annotation,
                        widgetsetFqn);
            }
            else {
                // remove annotation from the project with GWT module in the subject
                DeleteRefactoringPlugin.removeVaadinServletAnnotation(type,
                        copy, annotation);
            }
        }

        @Override
        protected String getNewWidgetsetFqn() {
            return getTargetWidgetset();
        }

    }

}
