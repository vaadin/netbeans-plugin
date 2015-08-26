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
package org.vaadin.netbeans.impl;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ClassIndexListener;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.RootsEvent;
import org.netbeans.api.java.source.TypesEvent;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.openide.filesystems.FileObject;
import org.openide.util.Utilities;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;
import org.vaadin.netbeans.utils.JavaUtils;

/**
 * @author denis
 */
@ProjectServiceProvider(service = { VaadinSupport.class,
        ProjectOpenedHook.class },
        projectType = "org-netbeans-modules-maven/war")
public class WebVaadinSupportImpl extends VaadinSupportImpl {

    public WebVaadinSupportImpl( Project project ) {
        super(project, true);
        myRootsListener = new ClassIndexListenerImpl();
    }

    @Override
    public Project getWidgetsetProject() {
        if (hasGwtModel(getProject(), this)) {
            return getProject();
        }
        for (File file : getRuntimeDependecies(getProject())) {
            Project dependency = FileOwnerQuery.getOwner(Utilities.toURI(file));
            if (dependency != null) {
                VaadinSupport support =
                        dependency.getLookup().lookup(VaadinSupport.class);
                if (support != null && support.isEnabled() && !support.isWeb()
                        && hasClientCompilerDependency(dependency)
                        && hasGwtModel(dependency, support))
                {
                    return dependency;
                }
            }
        }
        return getProject();
    }

    @Override
    protected void updateModel( CompilationController controller,
            TypeElement element )
    {
        ElementHandle<TypeElement> handle = ElementHandle.create(element);
        AnnotationMirror annotation =
                JavaUtils.getAnnotation(element,
                        JavaUtils.VAADIN_SERVLET_CONFIGURATION);
        if (annotation != null) {
            String widgetset =
                    JavaUtils.getValue(annotation, JavaUtils.WIDGETSET);
            if (widgetset != null) {
                ServletConfigurationImpl impl = new ServletConfigurationImpl();
                impl.setWidgetset(widgetset);
                getModel().add(handle, impl);
            }
        }
        annotation =
                JavaUtils.getAnnotation(element, JavaUtils.SERVLET_ANNOTATION);
        if (annotation != null) {
            String widgetset = JavaUtils.getWidgetsetWebInit(annotation);
            if (widgetset != null) {
                ServletConfigurationImpl impl = new ServletConfigurationImpl();
                impl.setWidgetset(widgetset);
                getModel().add(handle, impl);
            }
        }
    }

    @Override
    protected void remove( CompilationController controller,
            ElementHandle<TypeElement> handle )
    {
        getModel().remove(handle);
    }

    @Override
    protected void initClassModel( CompilationController controller )
            throws InterruptedException
    {
        List<TypeElement> elements =
                JavaUtils.findAnnotatedElements(
                        JavaUtils.VAADIN_SERVLET_CONFIGURATION, controller);
        for (TypeElement element : elements) {
            updateModel(controller, element);
        }
        elements =
                JavaUtils.findAnnotatedElements(JavaUtils.SERVLET_ANNOTATION,
                        controller);
        for (TypeElement element : elements) {
            updateModel(controller, element);
        }
    }

    @Override
    protected void removeFileSystemListener() {
        Set<Project> allProjects = new HashSet<>();
        removeListener(getProject(), allProjects);
        myRootsListener.clean();
    }

    @Override
    protected void initializeFileSystemListener() {
        Set<Project> allProjects = new HashSet<>();
        addListener(getProject(), allProjects);

        synchronized (myRootsListener) {
            for (Project project : allProjects) {
                if (project.equals(getProject())) {
                    continue;
                }
                ClasspathInfo info =
                        ClasspathInfo.create(
                                getClassPath(project, ClassPath.BOOT),
                                getClassPath(project, ClassPath.COMPILE),
                                getClassPath(project, ClassPath.SOURCE));
                myRootsListener.listenIndex(project, info.getClassIndex());
            }
        }
    }

    private void removeListener( Project project, Set<Project> projects ) {
        if (projects.contains(project)) {
            return;
        }
        projects.add(project);
        doRemoveListener(project);
        for (File file : getRuntimeDependecies(project)) {
            Project dependency = FileOwnerQuery.getOwner(Utilities.toURI(file));
            if (dependency != null) {
                removeListener(dependency, projects);
            }
        }
    }

    private void addListener( Project project, Set<Project> projects ) {
        if (projects.contains(project)) {
            return;
        }
        projects.add(project);
        doAddListener(project);
        for (File file : getRuntimeDependecies(project)) {
            Project dependency = FileOwnerQuery.getOwner(Utilities.toURI(file));
            if (dependency != null) {
                addListener(dependency, projects);
            }
        }
    }

    private boolean hasGwtModel( final Project project, VaadinSupport support )
    {
        final boolean[] result = new boolean[1];
        try {
            support.runModelOperation(new ModelOperation() {

                @Override
                public void run( VaadinModel model ) {
                    FileObject gwtXml = model.getGwtXml();
                    if (gwtXml == null) {
                        return;
                    }
                    result[0] = project.equals(FileOwnerQuery.getOwner(gwtXml));
                }
            });
        }
        catch (IOException e) {
            LOG.log(Level.INFO, null, e);
        }
        return result[0];
    }

    private static Set<Artifact> getDependencies( Project project ) {
        NbMavenProject nbMvnProject =
                project.getLookup().lookup(NbMavenProject.class);
        MavenProject mavenProject = nbMvnProject.getMavenProject();
        return mavenProject.getArtifacts();
    }

    static List<File> getRuntimeDependecies( Project project ) {
        Set<Artifact> artifacts = getDependencies(project);
        List<File> result = new ArrayList<>(artifacts.size());
        for (Artifact artifact : artifacts) {
            if (Artifact.SCOPE_COMPILE.equals(artifact.getScope())
                    || Artifact.SCOPE_RUNTIME.equals(artifact.getScope()))
            {
                result.add(artifact.getFile());
            }
        }
        return result;
    }

    private final class ClassIndexListenerImpl implements ClassIndexListener,
            ChangeListener
    {

        ClassIndexListenerImpl() {
            myIndeces = new WeakHashMap<>();
        }

        @Override
        public void rootsAdded( RootsEvent event ) {
            initializeClassIndex(sourceRootsAffected(event));
        }

        @Override
        public void rootsRemoved( RootsEvent event ) {
            initializeClassIndex(sourceRootsAffected(event));
        }

        @Override
        public void typesAdded( TypesEvent event ) {
        }

        @Override
        public void typesChanged( TypesEvent event ) {
        }

        @Override
        public void typesRemoved( TypesEvent event ) {
        }

        @Override
        public void stateChanged( ChangeEvent e ) {
            initializeClassIndex(true);
        }

        void listenIndex( Project project, ClassIndex index ) {
            synchronized (this) {
                myIndeces.put(project, index);
            }
            index.addClassIndexListener(this);
            ProjectUtils.getSources(project).addChangeListener(this);
        }

        synchronized void clean() {
            for (Entry<Project, ClassIndex> entry : myIndeces.entrySet()) {
                Project project = entry.getKey();
                ClassIndex index = entry.getValue();
                index.removeClassIndexListener(this);
                ProjectUtils.getSources(project).removeChangeListener(this);
            }
            myIndeces.clear();
        }

        private final Map<Project, ClassIndex> myIndeces;

    }

    private final ClassIndexListenerImpl myRootsListener;

}
