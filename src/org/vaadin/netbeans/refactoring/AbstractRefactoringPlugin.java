/**
 *
 */
package org.vaadin.netbeans.refactoring;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import org.netbeans.api.fileinfo.NonRecursiveFolder;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.j2ee.dd.api.common.InitParam;
import org.netbeans.modules.j2ee.dd.api.web.DDProvider;
import org.netbeans.modules.j2ee.dd.api.web.Servlet;
import org.netbeans.modules.j2ee.dd.api.web.WebApp;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.java.spi.JavaRefactoringPlugin;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import org.openide.filesystems.FileObject;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.code.generator.JavaUtils;
import org.vaadin.netbeans.code.generator.XmlUtils;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.NewArrayTree;

/**
 * @author denis
 */
abstract class AbstractRefactoringPlugin<R extends AbstractRefactoring> extends
        JavaRefactoringPlugin
{

    private static final Logger LOG = Logger
            .getLogger(AbstractRefactoringPlugin.class.getName());

    AbstractRefactoringPlugin( R refactoring ) {
        myRefactoring = refactoring;
    }

    @Override
    public Problem prepare( RefactoringElementsBag bag ) {
        Project project = getProject();

        GwtModuleAcceptor acceptor = getAcceptor();
        Set<FileObject> files = getAffectedJavaFiles(project, acceptor);
        Set<FileObject> additionalFiles = getAffectedConfigFiles(project,
                acceptor);
        fireProgressListenerStart(AbstractRefactoring.PREPARE, files.size()
                + additionalFiles.size());
        Problem problem = createAndAddElements(files, getTransformTask(), bag,
                getRefactoring());
        Problem configProblem = createAndAddConfigElements(additionalFiles, bag);
        fireProgressListenerStop();
        if (problem != null) {
            problem.setNext(configProblem);
            return problem;
        }
        else {
            return configProblem;
        }
    }

    protected Problem createAndAddConfigElements(
            Set<FileObject> additionalFiles, RefactoringElementsBag bag )
    {
        // TODO : add transaction and element implementations for web.xml if any
        return null;
    }

    @Override
    public void cancelRequest() {
    }

    @Override
    public Problem checkParameters() {
        return null;
    }

    @Override
    public Problem fastCheckParameters() {
        return null;
    }

    @Override
    public Problem preCheck() {
        return null;
    }

    protected Project getProject() {
        FileObject pkg = getFolder();
        if (pkg != null) {
            return FileOwnerQuery.getOwner(pkg);
        }

        FileObject gwtXml = getRefactoring().getRefactoringSource().lookup(
                FileObject.class);
        assert gwtXml != null : "Unable to get current project: "
                + "both Package and FileObject queries failed";
        return FileOwnerQuery.getOwner(gwtXml);
    }

    protected FileObject getFolder() {
        NonRecursiveFolder pkg = getRefactoring().getRefactoringSource()
                .lookup(NonRecursiveFolder.class);
        if (pkg != null) {
            return pkg.getFolder();
        }
        return null;
    }

    protected abstract GwtModuleAcceptor getAcceptor();

    protected abstract TransformTask getTransformTask();

    @Override
    protected JavaSource getJavaSource( JavaRefactoringPlugin.Phase phase ) {
        // it's using only for various check methods: fast-, pre-, check
        return null;
    }

    protected Set<FileObject> getAffectedJavaFiles( final Project project,
            final GwtModuleAcceptor acceptor )
    {
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        assert support != null;

        ClasspathInfo classPathInfo = support.getClassPathInfo();
        JavaSource javaSource = JavaSource.create(classPathInfo);
        if (javaSource == null) {
            LOG.warning("JavaSource is null for project "
                    + project.getProjectDirectory().getPath());
            return null;
        }

        final Set<FileObject> files = new HashSet<FileObject>();
        try {
            javaSource.runUserActionTask(new Task<CompilationController>() {

                @Override
                public void run( CompilationController controller )
                        throws Exception
                {
                    controller.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                    collectVaadinServlets(files, controller, acceptor, project);
                    collectServlets(files, controller, acceptor, project);
                }

            }, true);
        }
        catch (IOException e) {
            LOG.log(Level.INFO, null, e);
        }

        return files;
    }

    protected void collectServlets( Set<FileObject> files,
            CompilationController controller, GwtModuleAcceptor acceptor,
            Project project ) throws InterruptedException
    {
        List<TypeElement> servlets = JavaUtils.findAnnotatedElements(
                JavaUtils.SERVLET_ANNOTATION, controller);
        for (TypeElement typeElement : servlets) {
            if (acceptServletWidgetAnnoation(typeElement, controller, acceptor,
                    project))
            {
                files.add(SourceUtils.getFile(
                        ElementHandle.create(typeElement),
                        controller.getClasspathInfo()));
            }
        }
    }

    protected boolean acceptServletWidgetAnnoation( TypeElement type,
            CompilationController controller, GwtModuleAcceptor acceptor,
            Project project )
    {
        return getServletWidgetAnnotation(type, acceptor) != null;
    }

    protected AnnotationMirror getServletWidgetAnnotation( TypeElement type,
            GwtModuleAcceptor acceptor )
    {
        AnnotationMirror annotation = JavaUtils.getAnnotation(type,
                JavaUtils.SERVLET_ANNOTATION);
        if (annotation == null) {
            return null;
        }
        List<?> params = JavaUtils.getArrayValue(annotation,
                JavaUtils.INIT_PARAMS);
        if (params != null) {
            for (Object param : params) {
                if (param instanceof AnnotationMirror) {
                    AnnotationMirror mirror = (AnnotationMirror) param;
                    String name = JavaUtils.getValue(mirror, JavaUtils.NAME);
                    if (JavaUtils.WIDGETSET.equalsIgnoreCase(name)) {
                        String widgetset = JavaUtils.getValue(mirror,
                                JavaUtils.VALUE);
                        if (acceptor.accept(widgetset)) {
                            return annotation;
                        }
                    }
                }
            }
        }
        return null;
    }

    protected Set<FileObject> getAffectedConfigFiles( Project project,
            GwtModuleAcceptor acceptor )
    {
        try {
            FileObject webXml = XmlUtils.getWebXml(project);
            if (webXml != null) {
                WebApp webApp = DDProvider.getDefault().getDDRoot(webXml);
                if (webApp != null) {
                    Servlet[] servlets = webApp.getServlet();
                    for (Servlet servlet : servlets) {
                        InitParam[] initParams = servlet.getInitParam();
                        for (InitParam initParam : initParams) {
                            String name = initParam.getParamName();
                            String value = initParam.getParamValue();
                            if (JavaUtils.WIDGETSET.equals(name)
                                    && acceptor.accept(value))
                            {
                                return Collections.singleton(webXml);
                            }
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            LOG.log(Level.INFO, null, e);
        }
        return Collections.emptySet();
    }

    protected void collectVaadinServlets( final Set<FileObject> files,
            CompilationController controller, GwtModuleAcceptor acceptor,
            Project project ) throws InterruptedException
    {
        List<TypeElement> servlets = JavaUtils.findAnnotatedElements(
                JavaUtils.VAADIN_SERVLET_CONFIGURATION, controller);

        for (TypeElement typeElement : servlets) {
            if (getVaadinServletWidgetAnnotation(typeElement, acceptor) != null)
            {
                files.add(SourceUtils.getFile(
                        ElementHandle.create(typeElement),
                        controller.getClasspathInfo()));
            }
        }
    }

    protected AnnotationMirror getVaadinServletWidgetAnnotation(
            TypeElement type, GwtModuleAcceptor acceptor )
    {
        AnnotationMirror annotation = JavaUtils.getAnnotation(type,
                JavaUtils.VAADIN_SERVLET_CONFIGURATION);
        if (annotation == null) {
            return null;
        }
        String value = JavaUtils.getValue(annotation, JavaUtils.WIDGETSET);
        if (acceptor.accept(value)) {
            return annotation;
        }
        return null;
    }

    protected R getRefactoring() {
        return myRefactoring;
    }

    protected static AssignmentTree getAnnotationTreeAttribute(
            AnnotationTree annotation, String attributeName )
    {
        List<? extends ExpressionTree> arguments = annotation.getArguments();
        for (ExpressionTree expressionTree : arguments) {
            if (expressionTree instanceof AssignmentTree) {
                AssignmentTree assignmentTree = (AssignmentTree) expressionTree;
                ExpressionTree expression = assignmentTree.getVariable();
                if (expression instanceof IdentifierTree) {
                    IdentifierTree identifier = (IdentifierTree) expression;
                    if (identifier.getName().contentEquals(attributeName)) {
                        return assignmentTree;
                    }
                }
            }
        }
        return null;
    }

    protected static String getWidgetset( AnnotationTree tree ) {
        AssignmentTree assignment = getAnnotationTreeAttribute(tree,
                JavaUtils.NAME);
        ExpressionTree name = assignment.getExpression();
        if (name instanceof LiteralTree) {
            LiteralTree literal = (LiteralTree) name;
            if (literal != null
                    && JavaUtils.WIDGETSET
                            .equals(literal.getValue().toString()))
            {
                assignment = getAnnotationTreeAttribute(tree, JavaUtils.VALUE);
                ExpressionTree value = assignment.getExpression();
                if (value instanceof LiteralTree) {
                    literal = (LiteralTree) value;
                    return literal == null ? null : literal.getValue()
                            .toString();
                }
            }
        }
        return null;
    }

    static interface GwtModuleAcceptor {

        boolean accept( String moduleFqn );
    }

    abstract class AbstractTransformTask extends TransformTask {

        public AbstractTransformTask( GwtModuleAcceptor acceptor ) {
            super(null, null);
            myAcceptor = acceptor;
        }

        @Override
        public void run( WorkingCopy compiler ) throws IOException {
            compiler.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
            try {
                List<? extends TypeElement> typeElements = compiler
                        .getTopLevelElements();
                for (TypeElement typeElement : typeElements) {
                    processTypeElement(typeElement, compiler);
                }
            }
            finally {
                fireProgressListenerStep();
            }
        }

        protected abstract void doUpdateWebServletAnnotation( TypeElement type,
                WorkingCopy copy, AnnotationMirror annotation,
                String widgetsetFqn );

        protected abstract void doUpdateVaadinServletAnnotation(
                TypeElement type, WorkingCopy copy,
                AnnotationMirror annotation, String widgetsetFqn );

        protected void processTypeElement( TypeElement type, WorkingCopy copy )
        {
            doProcessTypeElement(type, copy);
            List<TypeElement> subtypes = ElementFilter.typesIn(type
                    .getEnclosedElements());
            for (TypeElement subType : subtypes) {
                processTypeElement(subType, copy);
            }
        }

        protected void doProcessTypeElement( TypeElement type, WorkingCopy copy )
        {
            updateVaadinServletAnnotation(type, copy);
            updateWebServletAnnotation(type, copy);
        }

        protected void updateWebServletAnnotation( TypeElement type,
                WorkingCopy copy )
        {
            AnnotationMirror annotation = getServletWidgetAnnotation(type,
                    myAcceptor);
            if (annotation != null) {
                String currentValue = JavaUtils.getWidgetsetWebInit(annotation);
                doUpdateWebServletAnnotation(type, copy, annotation,
                        currentValue);
            }
        }

        protected void updateVaadinServletAnnotation( TypeElement type,
                WorkingCopy copy )
        {
            AnnotationMirror annotation = getVaadinServletWidgetAnnotation(
                    type, myAcceptor);
            if (annotation != null) {
                String currentValue = JavaUtils.getValue(annotation,
                        JavaUtils.WIDGETSET);
                doUpdateVaadinServletAnnotation(type, copy, annotation,
                        currentValue);
            }
        }

        protected AnnotationTree getWidgetsetWebInit(
                AnnotationTree annotationTree )
        {
            ExpressionTree expressionTree = getAnnotationTreeAttribute(
                    annotationTree, JavaUtils.INIT_PARAMS);
            if (expressionTree instanceof AssignmentTree) {
                AssignmentTree assignmentTree = (AssignmentTree) expressionTree;
                ExpressionTree expression = assignmentTree.getExpression();
                if (expression instanceof AnnotationTree) {
                    AnnotationTree tree = (AnnotationTree) expression;
                    if (getWidgetset(tree) != null) {
                        return tree;
                    }
                }
                else if (expression instanceof NewArrayTree) {
                    NewArrayTree arrayTree = (NewArrayTree) expression;
                    List<? extends ExpressionTree> expressions = arrayTree
                            .getInitializers();
                    for (ExpressionTree webInitAnnotation : expressions) {
                        if (webInitAnnotation instanceof AnnotationTree) {
                            AnnotationTree tree = (AnnotationTree) webInitAnnotation;
                            if (getWidgetset(tree) != null) {
                                return tree;
                            }
                        }
                    }
                }
            }
            return null;
        }

        protected AnnotationTree replaceWidgetset( TreeMaker treeMaker,
                AnnotationTree annotationTree, String widgetset, String attrName )
        {
            ExpressionTree widgetsetTree = getAnnotationTreeAttribute(
                    annotationTree, attrName);

            AnnotationTree newTree = treeMaker.removeAnnotationAttrValue(
                    annotationTree, widgetsetTree);

            ExpressionTree newWidgetsetTree = treeMaker.Assignment(
                    treeMaker.Identifier(attrName),
                    treeMaker.Literal(getNewWidgetsetFqn(widgetset)));

            newTree = treeMaker.addAnnotationAttrValue(newTree,
                    newWidgetsetTree);
            return newTree;
        }

        protected String getNewWidgetsetFqn( String oldName ) {
            return oldName;
        }

        private GwtModuleAcceptor myAcceptor;
    }

    private R myRefactoring;
}
