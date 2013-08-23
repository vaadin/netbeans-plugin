/**
 *
 */
package org.vaadin.netbeans.impl;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.ClassIndexListener;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ClasspathInfo.PathKind;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.RootsEvent;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TypesEvent;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.openide.execution.ExecutorTask;
import org.openide.filesystems.FileObject;
import org.openide.util.RequestProcessor;
import org.openide.util.TaskListener;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.code.generator.JavaUtils;
import org.vaadin.netbeans.model.ModelOperation;

/**
 * @author denis
 */
@ProjectServiceProvider(service = { VaadinSupport.class,
        ProjectOpenedHook.class },
        projectType = "org-netbeans-modules-maven/jar")
public class VaadinSupportImpl extends ProjectOpenedHook implements
        VaadinSupport
{

    static RequestProcessor REQUEST_PROCESSOR = new RequestProcessor(
            VaadinSupportImpl.class);

    private static final String VAADIN_CHECK_CLASS = "com.vaadin.server.VaadinRequest";//NOI18N

    private static final String VAADIN_REQUEST_FQN = VAADIN_CHECK_CLASS
            .replace('.', '/');

    static final Logger LOG = Logger.getLogger(VaadinSupportImpl.class
            .getName());

    public VaadinSupportImpl( Project project ) {
        this(project, false);
    }

    protected VaadinSupportImpl( Project project, boolean web ) {
        myProject = project;
        myModel = new VaadinModelImpl(project, web);
        isEnabled = new AtomicReference<>();
        myResourcesListener = new ResourcesListener(this);
        myActions = new ConcurrentHashMap<>();
        isWeb = web;
        myDownloadListener = new DownloadDepsListener();
    }

    @Override
    public boolean isWeb() {
        return isWeb;
    }

    @Override
    public void addAction( final Action action, ExecutorTask task ) {
        TaskListener listener = new TaskListener() {

            @Override
            public void taskFinished( org.openide.util.Task task ) {
                task.removeTaskListener(this);
                Set<ExecutorTask> set = myActions.get(action);
                if (set != null) {
                    set.remove(task);
                }
                myResourcesListener.removeOutputResources();
            }

        };
        task.addTaskListener(listener);
        if (task.isFinished()) {
            task.removeTaskListener(listener);
            myResourcesListener.removeOutputResources();
        }
        Set<ExecutorTask> set = myActions.get(action);
        if (set == null) {
            set = new CopyOnWriteArraySet<>();
            Set<ExecutorTask> result = myActions.putIfAbsent(action, set);
            if (result != null) {
                set = result;
            }
        }
        set.add(task);
    }

    @Override
    public Collection<ExecutorTask> getTasks( Action action ) {
        Set<ExecutorTask> set = myActions.get(action);
        if (set == null) {
            return Collections.emptyList();
        }
        return set;
    }

    @Override
    public boolean isEnabled() {
        Boolean enabled = isEnabled.get();
        if (enabled == null) {
            ClassPath classPath = myClasspathInfo
                    .getClassPath(PathKind.COMPILE);
            if (classPath.findResource(VAADIN_REQUEST_FQN + ".class") != null) { // NOI18N 
                return true;
            }
            classPath = myClasspathInfo.getClassPath(PathKind.SOURCE);
            return classPath.findResource(VAADIN_REQUEST_FQN + ".java") != null;
        }
        return enabled;
    }

    @Override
    public void runModelOperation( final ModelOperation operation )
            throws IOException
    {
        Future<Void> future = invoke(new Task<CompilationController>() {

            @Override
            public void run( CompilationController controller )
                    throws Exception
            {
                controller.toPhase(Phase.ELEMENTS_RESOLVED);
                operation.run(myModel);
            }
        });
        if (future != null) {
            try {
                future.get();
            }
            catch (InterruptedException | ExecutionException e) {
                LOG.log(Level.INFO, null, e);
            }
        }
    }

    @Override
    public ClasspathInfo getClassPathInfo() {
        return myClasspathInfo;
    }

    @Override
    protected void projectClosed() {
        removeFileSystemListener();
        NbMavenProject mvnProject = getProject().getLookup().lookup(
                NbMavenProject.class);
        mvnProject.removePropertyChangeListener(myDownloadListener);

        myClasspathInfo = null;
        myModel.cleanup(false);
    }

    @Override
    protected void projectOpened() {
        myClasspathInfo = ClasspathInfo.create(
                getClassPath(getProject(), ClassPath.BOOT),
                getClassPath(getProject(), ClassPath.COMPILE),
                getClassPath(getProject(), ClassPath.SOURCE));

        NbMavenProject mvnProject = getProject().getLookup().lookup(
                NbMavenProject.class);
        mvnProject.addPropertyChangeListener(myDownloadListener);

        initializeClassIndex(true);
    }

    protected void updateModel( CompilationController controller,
            TypeElement element )
    {
    }

    protected void remove( CompilationController controller, TypeElement element )
    {
    }

    protected void initClassModel( CompilationController controller )
            throws InterruptedException
    {
    }

    protected void removeFileSystemListener() {
        doRemoveListener(getProject());
    }

    protected void initializeFileSystemListener() {
        doAddListener(getProject());
    }

    protected void doRemoveListener( Project project ) {
        removeListener(JavaUtils.getJavaSourceGroups(project));
        removeListener(JavaUtils.getResourcesSourceGroups(project));
    }

    protected void doAddListener( Project project ) {
        addListener(JavaUtils.getJavaSourceGroups(project));
        addListener(JavaUtils.getResourcesSourceGroups(project));
    }

    protected boolean sourceRootsAffected( RootsEvent event ) {
        Iterable<? extends URL> roots = event.getRoots();
        boolean affected = false;
        for (URL url : roots) {
            try {
                Project project = FileOwnerQuery.getOwner(url.toURI());
                if (project != null) {
                    affected = true;
                    break;
                }
            }
            catch (URISyntaxException e) {
                LOG.log(Level.INFO, null, e);
            }
        }
        return affected;
    }

    protected void initializeClassIndex( boolean reinitResourceListener ) {
        try {
            invoke(new InitTask(reinitResourceListener));
        }
        catch (IOException e) {
            LOG.log(Level.INFO, null, e);
        }
    }

    void addListener( FileObject fileObject ) {
        if (fileObject.isFolder()) {
            fileObject.addFileChangeListener(myResourcesListener);
            FileObject[] children = fileObject.getChildren();
            for (FileObject child : children) {
                addListener(child);
            }
        }
    }

    void removeListener( FileObject fileObject ) {
        if (fileObject.isFolder()) {
            fileObject.removeFileChangeListener(myResourcesListener);
            FileObject[] children = fileObject.getChildren();
            for (FileObject child : children) {
                removeListener(child);
            }
        }
    }

    Future<Void> invoke( final Task<CompilationController> task,
            boolean waitScan ) throws IOException
    {
        // myClasspathInfo could be null in the process of project closing
        ClasspathInfo info = myClasspathInfo;
        if (info == null) {
            return null;
        }
        JavaSource javaSource = JavaSource.create(info);
        if (javaSource == null) {
            return null;
        }
        if (waitScan) {
            return javaSource.runWhenScanFinished(task, true);
        }
        else {
            javaSource.runUserActionTask(task, true);
            return null;
        }
    }

    VaadinModelImpl getModel() {
        return myModel;
    }

    Project getProject() {
        return myProject;
    }

    private void addListener( SourceGroup[] sourceGroups ) {
        for (SourceGroup sourceGroup : sourceGroups) {
            FileObject root = sourceGroup.getRootFolder();
            addListener(root);
        }
    }

    private void removeListener( SourceGroup[] sourceGroups ) {
        for (SourceGroup sourceGroup : sourceGroups) {
            FileObject root = sourceGroup.getRootFolder();
            removeListener(root);
        }
    }

    private Future<Void> invoke( final Task<CompilationController> task )
            throws IOException
    {
        return invoke(task, !isInitialized);
    }

    protected static ClassPath getClassPath( Project project, String type ) {
        ClassPathProvider provider = project.getLookup().lookup(
                ClassPathProvider.class);
        SourceGroup[] sourceGroups = JavaUtils.getJavaSourceGroups(project);
        List<ClassPath> classPaths = new ArrayList<>(sourceGroups.length);
        for (SourceGroup sourceGroup : sourceGroups) {
            FileObject rootFolder = sourceGroup.getRootFolder();
            ClassPath path = provider.findClassPath(rootFolder, type);
            classPaths.add(path);
        }
        return ClassPathSupport.createProxyClassPath(classPaths
                .toArray(new ClassPath[classPaths.size()]));
    }

    private final class InitTask implements Task<CompilationController> {

        private final boolean initResourceListener;

        private InitTask( boolean reinitResourceListener ) {
            initResourceListener = reinitResourceListener;
        }

        @Override
        public void run( CompilationController controller ) throws Exception {
            controller.toPhase(Phase.ELEMENTS_RESOLVED);

            if (controller.getElements().getTypeElement(VAADIN_CHECK_CLASS) == null)
            {
                isEnabled.set(false);
                myModel.cleanup(false);
                REQUEST_PROCESSOR.execute(new Runnable() {

                    @Override
                    public void run() {
                        removeFileSystemListener();
                    }
                });
                return;
            }
            isEnabled.set(true);

            if (initResourceListener) {
                REQUEST_PROCESSOR.execute(new Runnable() {

                    @Override
                    public void run() {
                        removeFileSystemListener();
                        initializeFileSystemListener();
                    }
                });
            }

            if (!isInitialized) {
                controller.getClasspathInfo().getClassIndex()
                        .addClassIndexListener(new ClassIndexListenerImpl());
            }
            isInitialized = false;
            myModel.cleanup(true);

            initClassModel(controller);

            isInitialized = true;
        }
    }

    private final class DownloadDepsListener implements PropertyChangeListener {

        @Override
        public void propertyChange( PropertyChangeEvent evt ) {
            if (NbMavenProject.PROP_PROJECT.equals(evt.getPropertyName())) {
                initializeClassIndex(false);
            }
        }

    }

    private final class ClassIndexListenerImpl implements ClassIndexListener {

        @Override
        public void typesAdded( final TypesEvent event ) {
            try {
                invoke(new Task<CompilationController>() {

                    @Override
                    public void run( CompilationController controller )
                            throws Exception
                    {
                        controller.toPhase(Phase.ELEMENTS_RESOLVED);
                        updateModel(event, controller);
                    }
                });
            }
            catch (IOException e) {
                LOG.log(Level.INFO, null, e);
            }
        }

        @Override
        public void typesRemoved( final TypesEvent event ) {
            try {
                invoke(new Task<CompilationController>() {

                    @Override
                    public void run( CompilationController controller )
                            throws Exception
                    {
                        controller.toPhase(Phase.ELEMENTS_RESOLVED);
                        Iterable<? extends ElementHandle<TypeElement>> types = event
                                .getTypes();
                        for (ElementHandle<TypeElement> elementHandle : types) {
                            TypeElement element = elementHandle
                                    .resolve(controller);
                            if (element != null) {
                                remove(controller, element);
                            }
                        }
                    }
                });
            }
            catch (IOException e) {
                LOG.log(Level.INFO, null, e);
            }
        }

        @Override
        public void typesChanged( final TypesEvent event ) {
            try {
                invoke(new Task<CompilationController>() {

                    @Override
                    public void run( CompilationController controller )
                            throws Exception
                    {
                        controller.toPhase(Phase.ELEMENTS_RESOLVED);
                        updateModel(event, controller);
                    }
                });
            }
            catch (IOException e) {
                LOG.log(Level.INFO, null, e);
            }
        }

        @Override
        public void rootsAdded( RootsEvent event ) {
            rootsChanged(sourceRootsAffected(event));
        }

        @Override
        public void rootsRemoved( RootsEvent event ) {
            rootsChanged(sourceRootsAffected(event));
        }

        private void updateModel( TypesEvent event,
                CompilationController controller )
        {
            Iterable<? extends ElementHandle<TypeElement>> types = event
                    .getTypes();
            for (ElementHandle<TypeElement> elementHandle : types) {
                TypeElement element = elementHandle.resolve(controller);
                VaadinSupportImpl.this.updateModel(controller, element);
            }
        }

        private void rootsChanged( boolean reinitResourceListener ) {
            initializeClassIndex(reinitResourceListener);
        }

    }

    private final Project myProject;

    private final VaadinModelImpl myModel;

    private volatile boolean isInitialized;

    private volatile ClasspathInfo myClasspathInfo;

    private volatile AtomicReference<Boolean> isEnabled;

    private final ResourcesListener myResourcesListener;

    private final ConcurrentHashMap<Action, Set<ExecutorTask>> myActions;

    private final PropertyChangeListener myDownloadListener;

    private final boolean isWeb;

}
