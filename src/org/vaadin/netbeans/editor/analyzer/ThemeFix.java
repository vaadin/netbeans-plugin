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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.web.api.webmodule.WebProjectConstants;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.editor.analyzer.ui.ThemePanel;
import org.vaadin.netbeans.utils.JavaUtils;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;

/**
 * @author denis
 */
class ThemeFix extends AbstractJavaFix {

    private static final String VAADIN = "VAADIN"; // NOI18N

    private static final String THEMES = "themes"; // NOI18N

    private static final String DEFAULT_THEME_NAME = "newTheme"; // NOI18N

    private static final String THEME_FQN = "com.vaadin.annotations.Theme"; // NOI18N

    private static final String ADDONS_TEMPLATE =
            "Templates/Vaadin/addons.scss";// NOI18N

    private static final String THEME_TEMPLATE = "Templates/Vaadin/theme.scss";// NOI18N

    private static final String STYLES_TEMPLATE =
            "Templates/Vaadin/styles.scss";// NOI18N

    private static final String FAVORITE_ICON = "Templates/Vaadin/favicon.ico";// NOI18N

    private static final String THEME_PARAM_NAME = "theme"; // NOI18N

    private static final Logger LOG = Logger
            .getLogger(ThemeFix.class.getName()); // NOI18N

    public ThemeFix( FileObject fileObject,
            ElementHandle<TypeElement> classHandle, String themeName )
    {
        super(fileObject);
        myHandle = classHandle;
        myThemeName = themeName;
    }

    public ThemeFix( FileObject fileObject,
            ElementHandle<TypeElement> classHandle )
    {
        this(fileObject, classHandle, null);
    }

    @Override
    @NbBundle.Messages({ "addTheme=Create and Use Vaadin Theme",
            "# {0} - theme name", "createTheme=Create Vaadin Theme ''{0}''" })
    public String getText() {
        if (myThemeName == null) {
            return Bundle.addTheme();
        }
        else {
            return Bundle.createTheme(myThemeName);
        }
    }

    @NbBundle.Messages("noWebRoot=Unable to find Web document root directory")
    @Override
    public ChangeInfo implement() throws Exception {

        ThemesFolder folder = getThemesFolder(getFileObject());

        if (folder.getWebRoot() == null) {
            NotifyDescriptor descriptor =
                    new NotifyDescriptor.Message(Bundle.noWebRoot(),
                            NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(descriptor);
            return null;
        }

        String theme = myThemeName == null ? DEFAULT_THEME_NAME : myThemeName;
        FileObject themes = folder.getThemes();
        if (myThemeName == null) {
            if (themes != null) {
                FileObject[] children = themes.getChildren();
                for (FileObject child : children) {
                    if (child.isFolder()) {
                        theme = child.getName();
                        break;
                    }
                }
            }

            theme = requestThemeName(theme);
        }
        if (theme != null) {
            if (themes == null || themes.getFileObject(theme) == null) {
                createTheme(folder.getWebRoot(), folder.getVaadinRoot(),
                        themes, theme);
            }

            if (myThemeName == null) {
                return addThemeAnnotation(theme);
            }
        }
        return null;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    private ChangeInfo addThemeAnnotation( final String theme )
            throws IOException
    {
        JavaSource javaSource = JavaSource.forFileObject(getFileObject());
        if (javaSource == null) {
            getLogger().log(Level.WARNING, "JavaSource is null for file {0}",
                    getFileObject().getPath());
            return null;
        }
        ModificationResult task =
                javaSource.runModificationTask(new Task<WorkingCopy>() {

                    @Override
                    public void run( WorkingCopy copy ) throws Exception {
                        copy.toPhase(Phase.ELEMENTS_RESOLVED);

                        TypeElement clazz = myHandle.resolve(copy);
                        if (clazz == null) {
                            return;
                        }
                        ClassTree oldTree = copy.getTrees().getTree(clazz);
                        if (oldTree == null) {
                            return;
                        }
                        TreeMaker treeMaker = copy.getTreeMaker();
                        AnnotationTree themeAnnotation =
                                treeMaker.Annotation(treeMaker.Type(THEME_FQN),
                                        Collections.singletonList(treeMaker
                                                .Literal(theme)));
                        ClassTree newTree =
                                treeMaker.Class(treeMaker
                                        .addModifiersAnnotation(
                                                oldTree.getModifiers(),
                                                themeAnnotation), oldTree
                                        .getSimpleName(), oldTree
                                        .getTypeParameters(), oldTree
                                        .getExtendsClause(), oldTree
                                        .getImplementsClause(), oldTree
                                        .getMembers());
                        copy.rewrite(oldTree, newTree);
                    }
                });

        ChangeInfo changeInfo = createChangeInfo(task);
        task.commit();
        return changeInfo;
    }

    @NbBundle.Messages("setThemeName=Specify a theme's name")
    private String requestThemeName( final String theme ) {
        return Mutex.EVENT.readAccess(new Mutex.Action<String>() {

            @Override
            public String run() {
                ThemePanel panel = new ThemePanel(theme);
                DialogDescriptor descriptor =
                        new DialogDescriptor(panel, Bundle.setThemeName());
                Object result = DialogDisplayer.getDefault().notify(descriptor);
                if (NotifyDescriptor.OK_OPTION.equals(result)) {
                    return panel.getThemeName();
                }
                else {
                    return null;
                }
            }
        });
    }

    private void createTheme( FileObject webRoot, FileObject vaadin,
            FileObject themes, String themeName )
    {
        try {
            FileObject vaadinFolder = vaadin;
            if (vaadin == null) {
                vaadinFolder = webRoot.createFolder(VAADIN);
            }
            FileObject themesFolder = themes;
            if (themes == null) {
                themesFolder = vaadinFolder.createFolder(THEMES);
            }
            FileObject theme = themesFolder.createFolder(themeName);

            Map<String, String> map =
                    Collections.singletonMap(THEME_PARAM_NAME, themeName);
            JavaUtils.createDataObjectFromTemplate(ADDONS_TEMPLATE, theme,
                    null, map);
            JavaUtils.createDataObjectFromTemplate(STYLES_TEMPLATE, theme,
                    null, map);
            JavaUtils.createDataObjectFromTemplate(THEME_TEMPLATE, theme,
                    themeName, map);
            JavaUtils.createDataObjectFromTemplate(FAVORITE_ICON, theme, null,
                    null);
        }
        catch (IOException e) {
            getLogger().log(Level.INFO, null, e);
        }
    }

    static ThemesFolder getThemesFolder( FileObject subject ) {
        Project project = FileOwnerQuery.getOwner(subject);
        if (project == null) {
            return null;
        }
        Sources sources = ProjectUtils.getSources(project);
        if (sources == null) {
            return null;
        }
        SourceGroup[] webGroup =
                sources.getSourceGroups(WebProjectConstants.TYPE_DOC_ROOT);
        FileObject themes = null;
        FileObject webRoot = null;
        FileObject vaadin = null;
        for (SourceGroup sourceGroup : webGroup) {
            webRoot = sourceGroup.getRootFolder();
            vaadin = webRoot.getFileObject(VAADIN);
            if (vaadin != null) {
                themes = vaadin.getFileObject(THEMES);
                if (themes != null) {
                    break;
                }
            }
        }
        return new ThemesFolder(webRoot, vaadin, themes);
    }

    static FileObject getThemeFolder( FileObject subject, String name ) {
        ThemesFolder folder = getThemesFolder(subject);

        FileObject themes = folder.getThemes();
        if (themes == null) {
            return null;
        }
        return themes.getFileObject(name);
    }

    static class ThemesFolder {

        ThemesFolder( FileObject webRoot, FileObject vaadin, FileObject themes )
        {
            myWebRoot = webRoot;
            myVaadinRoot = vaadin;
            myThemesRoot = themes;
        }

        FileObject getWebRoot() {
            return myWebRoot;
        }

        FileObject getVaadinRoot() {
            return myVaadinRoot;
        }

        FileObject getThemes() {
            return myThemesRoot;
        }

        private FileObject myWebRoot;

        private FileObject myVaadinRoot;

        private FileObject myThemesRoot;
    }

    private final ElementHandle<TypeElement> myHandle;

    private final String myThemeName;

}
