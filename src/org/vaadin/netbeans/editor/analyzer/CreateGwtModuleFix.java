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

import java.io.File;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.utils.JavaUtils;
import org.vaadin.netbeans.utils.XmlUtils;

/**
 * @author denis
 */
public class CreateGwtModuleFix implements Fix {

    public CreateGwtModuleFix( String widgetsetFqn, FileObject fileObject ) {
        myWidgetsetFqn = widgetsetFqn;
        myFileObject = fileObject;
    }

    @NbBundle.Messages({ "# {0} - moduleFqn",
            "createGwtModule=Create GWT Module {0}" })
    @Override
    public String getText() {
        return Bundle.createGwtModule(myWidgetsetFqn);
    }

    @Override
    public ChangeInfo implement() throws Exception {
        SourceGroup[] sourceGroups =
                JavaUtils.getJavaSourceGroups(FileOwnerQuery
                        .getOwner(myFileObject));
        FileObject root = null;
        for (SourceGroup sourceGroup : sourceGroups) {
            root = sourceGroup.getRootFolder();
            if (FileUtil.isParentOf(myFileObject, root)) {
                break;
            }
        }
        if (root == null) {
            return null;
        }
        String widgetPath = myWidgetsetFqn.replace('.', '/');
        File file = new File(FileUtil.toFile(root), widgetPath);

        String widgetsetName = file.getName() + XmlUtils.GWT;

        file = file.getParentFile();
        FileObject folder = FileUtil.createFolder(file);
        if (folder != null) {
            FileObject gwtXml = XmlUtils.createGwtXml(folder, widgetsetName);
            DataObject dataObject = DataObject.find(gwtXml);
            if (dataObject == null) {
                return null;
            }
            EditorCookie cookie =
                    dataObject.getLookup().lookup(EditorCookie.class);
            if (cookie != null) {
                cookie.open();
            }
        }

        return null;
    }

    private String myWidgetsetFqn;

    private final FileObject myFileObject;

}
