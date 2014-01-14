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
package org.vaadin.netbeans.editor.analyzer;

import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Element;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.openide.filesystems.FileObject;
import org.vaadin.netbeans.utils.JavaUtils;

/**
 * @author denis
 */
public final class IsInSourceQuery {

    public static boolean isInSource( Element element, CompilationInfo info ) {
        FileObject fileObject =
                SourceUtils.getFile(ElementHandle.create(element),
                        info.getClasspathInfo());
        if (fileObject == null) {
            return false;
        }
        return isInSourceRoots(element, info, getSourceRoots(info));
    }

    public static Set<FileObject> getSourceRoots( CompilationInfo info ) {
        Project project = FileOwnerQuery.getOwner(info.getFileObject());
        return getSourceRoots(project);
    }

    public static Set<FileObject> getSourceRoots( Project project ) {
        SourceGroup[] sourceGroups = JavaUtils.getJavaSourceGroups(project);
        Set<FileObject> roots = new HashSet<>();
        for (SourceGroup sourceGroup : sourceGroups) {
            FileObject rootFolder = sourceGroup.getRootFolder();
            roots.add(rootFolder);
        }
        return roots;
    }

    public static boolean isInSourceRoots( Element element,
            CompilationInfo info, Set<FileObject> roots )
    {
        FileObject fileObject =
                SourceUtils.getFile(ElementHandle.create(element),
                        info.getClasspathInfo());
        return isInSourceRoots(fileObject, roots);
    }

    public static boolean isInSourceRoots( FileObject fileObject,
            Set<FileObject> roots )
    {
        if (fileObject == null) {
            return false;
        }
        ClassPath classPath =
                ClassPath.getClassPath(fileObject, ClassPath.SOURCE);
        if (classPath == null) {
            return false;
        }
        FileObject root = classPath.findOwnerRoot(fileObject);
        return roots.contains(root);
    }
}
