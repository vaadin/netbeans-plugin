/**
 *
 */
package org.vaadin.netbeans.refactoring;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.project.Project;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.vaadin.netbeans.code.generator.JavaUtils;
import org.vaadin.netbeans.code.generator.XmlUtils;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;

/**
 * @author denis
 */
abstract class AbstractCopyRefactoringPlugin<R extends AbstractRefactoring>
        extends AbstractRefactoringPlugin<R>
{

    AbstractCopyRefactoringPlugin( R refactoring ) {
        super(refactoring);
    }

    @Override
    public Problem checkParameters() {
        return doCheckParameters();
    }

    @Override
    public Problem fastCheckParameters() {
        return doCheckParameters();
    }

    protected abstract File getTarget();

    protected Problem doCheckParameters() {
        File target = getTarget();
        if (target == null) {
            return null;
        }

        if (target.exists()) {
            return new Problem(true, Bundle.fileAlreadyExists(getGwtName(),
                    getTargetPackageFqn()));
        }
        return null;
    }

    protected abstract Project getTargetProject();

    protected GwtModuleAcceptor getTargetAcceptor() {
        return new NullValueGwtModuleAcceptor();
    }

    protected FileObject getTargetPackage() {
        File target = getTarget();
        if (target == null) {
            return null;
        }
        if (target.isDirectory()) {
            return FileUtil.toFileObject(FileUtil.normalizeFile(target));
        }
        else {
            return FileUtil.toFileObject(FileUtil.normalizeFile(target
                    .getParentFile()));
        }
    }

    protected String getTargetPackageFqn() {
        FileObject folder = getTargetPackage();
        if (folder == null) {
            return null;
        }
        ClassPath classPath = ClassPath.getClassPath(folder, ClassPath.SOURCE);
        return classPath.getResourceName(folder, '.', true);
    }

    protected ServletWidgetsetPresence getWidgetsetPresence( TypeElement type,
            CompilationController controller )
    {
        AnnotationMirror annotation = JavaUtils.getAnnotation(type,
                JavaUtils.SERVLET_ANNOTATION);
        if (annotation == null) {
            return null;
        }
        List<?> params = JavaUtils.getArrayValue(annotation,
                JavaUtils.INIT_PARAMS);
        if (params != null) {
            boolean hasUi = false;
            boolean emptyWidgetset = false;
            boolean noWidgetset = true;
            for (Object param : params) {
                if (param instanceof AnnotationMirror) {
                    AnnotationMirror mirror = (AnnotationMirror) param;
                    String name = JavaUtils.getValue(mirror, JavaUtils.NAME);
                    if (JavaUtils.UI.equalsIgnoreCase(name)) {
                        String widgetset = JavaUtils.getValue(mirror,
                                JavaUtils.VALUE);
                        TypeElement typeElement = controller.getElements()
                                .getTypeElement(widgetset);
                        TypeElement ui = controller.getElements()
                                .getTypeElement(JavaUtils.VAADIN_UI_FQN);
                        if (typeElement != null
                                && ui != null
                                && controller.getTypes().isSubtype(
                                        typeElement.asType(), ui.asType()))
                        {
                            hasUi = true;
                        }
                    }
                    else if (JavaUtils.WIDGETSET.equalsIgnoreCase(name)) {
                        String widgetset = JavaUtils.getValue(mirror,
                                JavaUtils.VALUE);
                        noWidgetset = widgetset == null;
                        emptyWidgetset = widgetset != null
                                && widgetset.trim().length() == 0;
                    }
                }
            }
            if (hasUi) {
                if (noWidgetset) {
                    return ServletWidgetsetPresence.NO_WIDGETSET;
                }
                else if (emptyWidgetset) {
                    return ServletWidgetsetPresence.EMPTY_WIDGETSET;
                }
            }
        }
        return null;
    }

    protected String getGwtNameExt() {
        FileObject gwtXml = getRefactoring().getRefactoringSource().lookup(
                FileObject.class);
        return gwtXml.getNameExt();
    }

    protected String getGwtName() {
        String name = getGwtNameExt();
        if (name.endsWith(XmlUtils.GWT_XML)) {
            name = name.substring(0, name.length() - XmlUtils.GWT_XML.length());
        }
        return name;
    }

    abstract class AbstractCopyGwtTransformTask extends AbstractTransformTask {

        AbstractCopyGwtTransformTask() {
            super(getAcceptor());
        }

        @Override
        protected String getNewWidgetsetFqn( String oldName ) {
            return getNewWidgetsetFqn();
        }

        protected abstract String getNewWidgetsetFqn();

        protected void addServletWidgetsetWebInit( TypeElement type,
                WorkingCopy copy, AnnotationMirror annotation )
        {
            TreeMaker treeMaker = copy.getTreeMaker();
            Tree tree = copy.getTrees().getTree(type, annotation);
            Tree oldTree = null;
            Tree newTree = null;
            if (tree instanceof AnnotationTree) {
                AnnotationTree annotationTree = (AnnotationTree) tree;
                ExpressionTree expressionTree = getAnnotationTreeAttribute(
                        annotationTree, JavaUtils.INIT_PARAMS);
                if (expressionTree instanceof AssignmentTree) {
                    AssignmentTree assignmentTree = (AssignmentTree) expressionTree;
                    ExpressionTree expression = assignmentTree.getExpression();
                    if (expression instanceof AnnotationTree) {
                        oldTree = expression;
                        List<ExpressionTree> initializers = new ArrayList<>(2);
                        initializers.add(expression);
                        initializers.add(createWidgetsetWebInit(treeMaker));

                        newTree = treeMaker.NewArray(null,
                                Collections.<ExpressionTree> emptyList(),
                                initializers);
                    }
                    else if (expression instanceof NewArrayTree) {
                        NewArrayTree arrayTree = (NewArrayTree) expression;
                        oldTree = arrayTree;
                        newTree = treeMaker.addNewArrayInitializer(arrayTree,
                                createWidgetsetWebInit(treeMaker));
                    }
                }
            }
            if (oldTree != null && newTree != null) {
                copy.rewrite(oldTree, newTree);
            }
        }

        protected ExpressionTree createWidgetsetWebInit( TreeMaker treeMaker ) {
            ExpressionTree nameAttrTree = treeMaker.Assignment(
                    treeMaker.Identifier(JavaUtils.NAME),
                    treeMaker.Literal(JavaUtils.WIDGETSET));
            ExpressionTree widgetsetTree = treeMaker.Assignment(
                    treeMaker.Identifier(JavaUtils.VALUE),
                    treeMaker.Literal(getNewWidgetsetFqn()));

            List<ExpressionTree> expressions = new ArrayList<>(2);
            expressions.add(nameAttrTree);
            expressions.add(widgetsetTree);
            return treeMaker.Annotation(
                    treeMaker.Type(JavaUtils.WEB_INIT_PARAM), expressions);
        }

        protected void renameVaadinServletAnnotation( TypeElement type,
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

        protected void renameWebServletAnnotation( TypeElement type,
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

    }

    static enum ServletWidgetsetPresence {
        EMPTY_WIDGETSET, NO_WIDGETSET
    }

}
