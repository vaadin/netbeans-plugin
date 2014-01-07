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

import javax.lang.model.element.Element;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
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
        Project project = FileOwnerQuery.getOwner(info.getFileObject());
        SourceGroup[] groups = JavaUtils.getJavaSourceGroups(project);
        for (SourceGroup sourceGroup : groups) {
            FileObject rootFolder = sourceGroup.getRootFolder();
            if (FileUtil.isParentOf(rootFolder, fileObject)) {
                return true;
            }
        }
        return false;
    }
}
