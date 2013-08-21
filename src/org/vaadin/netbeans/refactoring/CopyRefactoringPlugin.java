/**
 *
 */
package org.vaadin.netbeans.refactoring;

import java.io.File;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.project.Project;
import org.netbeans.modules.refactoring.api.SingleCopyRefactoring;
import org.openide.filesystems.FileObject;
import org.vaadin.netbeans.code.generator.JavaUtils;
import org.vaadin.netbeans.code.generator.XmlUtils;

/**
 * @author denis
 */
class CopyRefactoringPlugin extends
        AbstractCopyRefactoringPlugin<SingleCopyRefactoring>
{

    private static final GwtModuleAcceptor NO_ACCEPTOR = new GwtModuleAcceptor()
    {

        @Override
        public boolean accept( String moduleFqn ) {
            return false;
        }
    };

    CopyRefactoringPlugin( SingleCopyRefactoring refactoring ) {
        super(refactoring);
    }

    @Override
    protected File getTarget() {
        return getRefactoring().getTarget().lookup(File.class);
    }

    @Override
    protected Set<FileObject> getAffectedJavaFiles( Project project,
            GwtModuleAcceptor acceptor )
    {
        Set<FileObject> files = super.getAffectedJavaFiles(project, acceptor);
        files.addAll(super.getAffectedJavaFiles(getTargetProject(),
                getTargetAcceptor()));
        return files;
    }

    @Override
    protected Project getTargetProject() {
        return getRefactoring().getTarget().lookup(Project.class);
    }

    @Override
    protected boolean acceptServletWidgetAnnoation( TypeElement type,
            CompilationController controller, GwtModuleAcceptor acceptor,
            Project project )
    {
        if (!getTargetProject().equals(getProject())
                && project.equals(getProject()))
        {
            return false;
        }
        return getWidgetsetPresence(type, controller) != null;
    }

    @Override
    protected GwtModuleAcceptor getAcceptor() {
        return NO_ACCEPTOR;
    }

    @Override
    protected TransformTask getTransformTask() {
        return new CopyGwtTransformTask();
    }

    class CopyGwtTransformTask extends AbstractCopyGwtTransformTask {

        @Override
        protected String getNewWidgetsetFqn() {
            ClassPath classPath = ClassPath.getClassPath(getTargetPackage(),
                    ClassPath.SOURCE);
            String pkg = classPath.getResourceName(getTargetPackage(), '.',
                    true);
            String name = getTarget().getName();
            assert name.endsWith(XmlUtils.GWT_XML);
            name = name.substring(0, name.length() - XmlUtils.GWT_XML.length());
            if (pkg.length() == 0) {
                return name;
            }
            else {
                return pkg + '.' + name;
            }
        }

        @Override
        protected void updateWebServletAnnotation( TypeElement type,
                WorkingCopy copy )
        {
            // set annotation for target project
            ServletWidgetsetPresence presence = getWidgetsetPresence(type, copy);
            if (presence == null) {
                return;
            }
            AnnotationMirror annotation = JavaUtils.getAnnotation(type,
                    JavaUtils.SERVLET_ANNOTATION);
            if (ServletWidgetsetPresence.EMPTY_WIDGETSET.equals(presence)) {
                renameWebServletAnnotation(type, copy, annotation, null);
            }
            else {
                addServletWidgetsetWebInit(type, copy, annotation);
            }
        }

        @Override
        protected void updateVaadinServletAnnotation( TypeElement type,
                WorkingCopy copy )
        {
            AnnotationMirror annotation = getVaadinServletWidgetAnnotation(
                    type, getTargetAcceptor());
            if (annotation != null) {
                renameVaadinServletAnnotation(type, copy, annotation, null);
            }
        }

        @Override
        protected void doUpdateVaadinServletAnnotation( TypeElement type,
                WorkingCopy copy, AnnotationMirror annotation,
                String widgetsetFqn )
        {
        }

        @Override
        protected void doUpdateWebServletAnnotation( TypeElement type,
                WorkingCopy copy, AnnotationMirror annotation,
                String widgetsetFqn )
        {
        }

    }

}
