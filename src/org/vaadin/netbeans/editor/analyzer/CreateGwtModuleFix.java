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

import javax.swing.JButton;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.editor.analyzer.ui.NamePanel;
import org.vaadin.netbeans.editor.analyzer.ui.NewGwtModulePanel;
import org.vaadin.netbeans.utils.JavaUtils;
import org.vaadin.netbeans.utils.UIGestureUtils;
import org.vaadin.netbeans.utils.XmlUtils;

/**
 * @author denis
 */
public class CreateGwtModuleFix implements Fix {

    public CreateGwtModuleFix( String widgetsetFqn, FileObject fileObject ) {
        myWidgetsetFqn = widgetsetFqn;
        myFileObject = fileObject;
    }

    public CreateGwtModuleFix( FileObject fileObject, FileObject targetFolder )
    {
        this((String) null, fileObject);
        myTargetFolder = targetFolder;
        if (myTargetFolder != null) {
            initTargetPackageFqn();
        }
    }

    @NbBundle.Messages({ "createNewGwtModule=Create new GWT module file",
            "# {0} - moduleFqn", "createGwtModule=Create GWT Module {0}",
            "createNewGwtModuleInside=Create GWT module file inside {0}" })
    @Override
    public String getText() {
        if (myWidgetsetFqn == null) {
            if (myTargetFolder == null) {
                return Bundle.createNewGwtModule();
            }
            else {
                return Bundle.createNewGwtModuleInside(myTargetFqn);
            }
        }
        else {
            return Bundle.createGwtModule(myWidgetsetFqn);
        }
    }

    @Override
    public ChangeInfo implement() throws Exception {
        logUiUsage();

        FileObject root = getSourceRoot();
        if (root == null) {
            return null;
        }
        String widgetsetFqn = myWidgetsetFqn;
        if (widgetsetFqn == null) {
            String name = getGwtModuleName();
            if (name == null) {
                return null;
            }
            widgetsetFqn = myTargetFqn + '.' + name;
        }
        String widgetPath = widgetsetFqn.replace('.', '/');
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

    private void logUiUsage() {
        UIGestureUtils.logUiUsage(getClass(),
                AbstractJavaFix.UI_FIX_LOGGER_NAME, "UI_LogCreateGwtModule"); // NOI18N
    }

    private void initTargetPackageFqn() {
        ClassPath classPath =
                ClassPath.getClassPath(myTargetFolder, ClassPath.SOURCE);
        myTargetFqn = classPath.getResourceName(myTargetFolder, '.', true);
    }

    private FileObject getSourceRoot() {
        if (mySourceGroup == null) {
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
            return root;
        }
        else {
            return mySourceGroup.getRootFolder();
        }
    }

    @NbBundle.Messages({ "gwtModuleName=GWT Module Name" })
    private String getGwtModuleName() {
        if (myTargetFolder == null) {
            return askTarget();
        }
        else {
            final String name =
                    XmlUtils.getDefaultGwtModuleName(myTargetFolder);
            String gwtXmlName =
                    Mutex.EVENT.readAccess(new Mutex.Action<String>() {

                        @Override
                        public String run() {
                            NamePanel panel = new NamePanel(name);
                            DialogDescriptor descriptor =
                                    new DialogDescriptor(panel, Bundle
                                            .gwtModuleName());
                            Object result =
                                    DialogDisplayer.getDefault().notify(
                                            descriptor);
                            if (NotifyDescriptor.OK_OPTION.equals(result)) {
                                return panel.getClassName();
                            }
                            else {
                                return null;
                            }
                        }

                    });
            return gwtXmlName;
        }
    }

    @NbBundle.Messages({ "gwtTarget=Set GWT Module Name and Target", "ok=OK",
            "cancel=Cancel" })
    private String askTarget() {
        final String name = XmlUtils.getDefaultGwtModuleName(null);
        final Project project = FileOwnerQuery.getOwner(myFileObject);
        String gwtXmlName = Mutex.EVENT.readAccess(new Mutex.Action<String>() {

            @Override
            public String run() {
                JButton ok = new JButton(Bundle.ok());
                ok.setEnabled(false);
                NewGwtModulePanel panel =
                        new NewGwtModulePanel(name, project, ok);
                JButton cancel = new JButton(Bundle.cancel());
                DialogDescriptor descriptor =
                        new DialogDescriptor(panel, Bundle.gwtTarget(), true,
                                new Object[] { ok, cancel }, ok,
                                DialogDescriptor.DEFAULT_ALIGN, null, null);
                Object result = DialogDisplayer.getDefault().notify(descriptor);
                if (ok.equals(result)) {
                    mySourceGroup = panel.getTargetSourceGroup();
                    myTargetFqn = panel.getTargetPackage();
                    return panel.getGwtName();
                }
                else {
                    return null;
                }
            }

        });
        return gwtXmlName;
    }

    private final String myWidgetsetFqn;

    private final FileObject myFileObject;

    private FileObject myTargetFolder;

    private String myTargetFqn;

    private SourceGroup mySourceGroup;

}
