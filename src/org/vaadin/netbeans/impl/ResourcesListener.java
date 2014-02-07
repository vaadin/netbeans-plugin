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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.vaadin.netbeans.VaadinSupport.Action;
import org.vaadin.netbeans.utils.JavaUtils;
import org.vaadin.netbeans.utils.XmlUtils;

final class ResourcesListener extends FileChangeAdapter implements
        FileChangeListener
{

    private static final String GWT_DEBUG = "gwt-debug";// NOI18N

    private static final Logger LOG = Logger.getLogger(ResourcesListener.class
            .getName());

    ResourcesListener( VaadinSupportImpl support ) {
        mySupport = support;
    }

    @Override
    public void fileDeleted( FileEvent fe ) {
        FileObject file = fe.getFile();
        if (file != null) {
            if (file.getNameExt().endsWith(XmlUtils.GWT_XML)) {
                reinitGwtModule(file);
            }

            mySupport.removeListener(file);
            if (!isTracked()) {
                return;
            }
            File classFile = getBuildResourcePath(file);
            if (classFile == null) {
                return;
            }
            FileObject fileObject = FileUtil.toFileObject(classFile);
            if (fileObject != null) {
                try {
                    fileObject.delete();
                }
                catch (IOException e) {
                    LOG.log(Level.FINE, null, e);
                }
            }
        }
    }

    @Override
    public void fileChanged( FileEvent fe ) {
        updateResourceClassFile(fe.getFile());
    }

    @Override
    public void fileDataCreated( FileEvent fe ) {
        updateResourceClassFile(fe.getFile());
        FileObject file = fe.getFile();
        if (file != null && file.getNameExt().endsWith(XmlUtils.GWT_XML)) {
            reinitGwtModule(null);
        }
    }

    @Override
    public void fileFolderCreated( FileEvent fe ) {
        mySupport.addListener(fe.getFile());
        updateResourceClassFile(fe.getFile());
    }

    @Override
    public void fileRenamed( FileRenameEvent fe ) {
        FileObject file = fe.getFile();
        if (file != null) {
            if (file.getNameExt().endsWith(XmlUtils.GWT_XML)) {
                reinitGwtModule(file);
            }

            if (!isTracked()) {
                return;
            }
            File classResource = getBuildResourcePath(file.getParent());
            if (classResource == null) {
                return;
            }
            classResource =
                    new File(classResource, fe.getName() + '.' + fe.getExt());
            FileObject oldClassResource =
                    FileUtil.toFileObject(FileUtil.normalizeFile(classResource));
            if (oldClassResource != null) {
                try {
                    oldClassResource.delete();
                }
                catch (IOException e) {
                    LOG.log(Level.INFO, null, e);
                }
            }
            updateResourceClassFile(file);
        }
    }

    void removeOutputResources() {
        FileObject output = getClassesFolder();
        doRemoveResource(output);
        if (isTracked()) {
            return;
        }
        removeEmptyFolders(output);
    }

    private void removeEmptyFolders( FileObject file ) {
        if (isTracked() || file == null) {
            return;
        }
        if (file.isFolder()) {
            FileObject[] children = file.getChildren();
            if (children.length == 0) {
                try {
                    file.delete();
                }
                catch (IOException e) {
                    LOG.log(Level.INFO, null, e);
                }
            }
            else {
                for (FileObject child : children) {
                    removeEmptyFolders(child);
                }
            }
        }
    }

    private void doRemoveResource( FileObject file ) {
        if (isTracked() || file == null) {
            return;
        }
        if (!file.isFolder() && file.getAttribute(GWT_DEBUG) != null) {
            try {
                file.delete();
            }
            catch (IOException e) {
                LOG.log(Level.INFO, null, e);
            }
        }
        else if (file.isFolder()) {
            FileObject[] children = file.getChildren();
            for (FileObject child : children) {
                doRemoveResource(child);
            }
        }
    }

    private boolean isTracked() {
        if (mySupport.isWeb()) {
            return !mySupport.getTasks(Action.DEV_MODE).isEmpty()
                    || !mySupport.getTasks(Action.DEBUG_DEV_MODE).isEmpty();
        }
        else {
            return false;
        }
    }

    private void updateResourceClassFile( FileObject fileObject ) {
        if (!isTracked()) {
            return;
        }
        if (fileObject != null) {
            if (fileObject.isFolder()) {
                try {
                    File folder = getBuildResourcePath(fileObject);
                    if (folder != null) {
                        FileUtil.createFolder(folder);
                    }
                }
                catch (IOException e) {
                    LOG.log(Level.INFO, null, e);
                }
                FileObject[] children = fileObject.getChildren();
                for (FileObject child : children) {
                    updateResourceClassFile(child);
                }
            }
            else {
                FileObject resource =
                        copy(fileObject, getBuildResourcePath(fileObject));
                if (resource != null) {
                    try {
                        resource.setAttribute(GWT_DEBUG, Boolean.TRUE);
                    }
                    catch (IOException e) {
                        LOG.log(Level.INFO, null, e);
                    }
                }
            }
        }
    }

    private void reinitGwtModule( final FileObject file ) {
        VaadinSupportImpl.REQUEST_PROCESSOR.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    mySupport.invoke(new Task<CompilationController>() {

                        @Override
                        public void run( CompilationController arg0 )
                                throws Exception
                        {
                            if (file == null
                                    || file.equals(mySupport.getModel()
                                            .getGwtXml(false)))
                            {
                                mySupport.getModel().initGwtXml();
                            }
                        }
                    }, false);
                }
                catch (IOException e) {
                    LOG.log(Level.FINE, null, e);
                }
            }
        });
    }

    private FileObject copy( FileObject fileObject, File file ) {
        if (file == null) {
            return null;
        }
        FileObject dest = FileUtil.toFileObject(file);
        OutputStream outputStream = null;
        InputStream inputStream = null;
        FileLock lock = null;
        try {
            if (dest == null) {
                File parent = file.getParentFile();
                FileObject parentFolder = FileUtil.createFolder(parent);
                return FileUtil.copyFile(fileObject, parentFolder,
                        fileObject.getName());
            }
            else {
                lock = dest.lock();
                outputStream = dest.getOutputStream(lock);
                inputStream = fileObject.getInputStream();
                FileUtil.copy(inputStream, outputStream);
                return dest;
            }
        }
        catch (IOException e) {
            LOG.log(Level.INFO, null, e);
        }
        finally {
            if (lock != null) {
                lock.releaseLock();
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
            catch (IOException e) {
                LOG.log(Level.FINE, null, e);
            }
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            }
            catch (IOException e) {
                LOG.log(Level.FINE, null, e);
            }
        }
        return null;
    }

    private FileObject getClassesFolder() {
        NbMavenProject project =
                mySupport.getProject().getLookup().lookup(NbMavenProject.class);
        String classes =
                project.getMavenProject().getBuild().getOutputDirectory();
        /*
         * Object classes = PluginPropertyUtils.createEvaluator(
         * mySupport.getProject()).evaluate(
         * "${project.build.outputDirectory}");
         */
        if (classes != null) {
            File file = new File(classes.toString());
            return FileUtil.toFileObject(FileUtil.normalizeFile(file));
        }
        return null;
    }

    private File getBuildResourcePath( FileObject src ) {
        FileObject classesFolder = getClassesFolder();
        if (classesFolder == null) {
            return null;
        }

        Project project = FileOwnerQuery.getOwner(src);
        if (project == null) {
            return null;
        }

        // skip copying module gwt.xml file to external project
        if (src.getNameExt().endsWith(XmlUtils.GWT_XML)
                && !project.equals(mySupport.getProject()))
        {
            return null;
        }
        FileObject srcRoot =
                getSrcRoot(src, JavaUtils.getJavaSourceGroups(project));
        if (srcRoot == null) {
            srcRoot =
                    getSrcRoot(src, JavaUtils.getResourcesSourceGroups(project));
        }
        if (srcRoot == null) {
            return null;
        }
        String relativePath = FileUtil.getRelativePath(srcRoot, src);
        File classes = FileUtil.toFile(classesFolder);
        return new File(classes, relativePath);
    }

    private FileObject getSrcRoot( FileObject fileObject, SourceGroup[] groups )
    {
        for (SourceGroup group : groups) {
            FileObject root = group.getRootFolder();
            if (root.equals(fileObject)
                    || FileUtil.isParentOf(root, fileObject))
            {
                return root;
            }
        }
        return null;
    }

    private final VaadinSupportImpl mySupport;

}