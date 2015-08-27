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

import java.util.Collections;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ClasspathInfo.PathKind;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.netbeans.spi.java.hints.HintContext;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.editor.hints.Analyzer;
import org.vaadin.netbeans.utils.JavaUtils;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;

/**
 * @author denis
 */
public class ThemeAnalyzer extends Analyzer {

    private static final String VAADIN_THEMES = "VAADIN/themes"; // NOI18N

    public ThemeAnalyzer( HintContext context ) {
        super(context);
    }

    private static final String VAADIN_UI = "com.vaadin.ui.UI"; // NOI18N

    private static final String THEME = "com.vaadin.annotations.Theme";// NOI18N

    @Override
    @NbBundle.Messages({
            "noThemeAnnotationFound=Custom Vaadin Theme is not specified",
            "# {0} - theme name",
            "notThemeFound=Theme ''{0}'' is not found in the web resources folder" })
    public void analyze() {
        TypeElement type = getType();
        if (type == null) {
            return;
        }
        CompilationInfo info = getInfo();
        TypeElement ui = info.getElements().getTypeElement(VAADIN_UI);
        if (ui != null && info.getTypes().isSubtype(type.asType(), ui.asType()))
        {
            AnnotationMirror theme = JavaUtils.getAnnotation(type, THEME);
            if (theme == null) {
                List<Integer> positions =
                        AbstractJavaFix.getElementPosition(info, type);
                Fix themeFix =
                        new ThemeFix(info.getFileObject(),
                                ElementHandle.create(type));
                ErrorDescription description =
                        ErrorDescriptionFactory.createErrorDescription(
                                getSeverity(Severity.HINT),
                                Bundle.noThemeAnnotationFound(),
                                Collections.singletonList(themeFix),
                                info.getFileObject(), positions.get(0),
                                positions.get(1));
                getDescriptions().add(description);
                myNoThemeAnnotation = description;
            }
            else {
                String value = JavaUtils.getValue(theme, JavaUtils.VALUE);
                if (value == null) {
                    return;
                }
                if (checkBundledTheme(getSupport(), value)) {
                    return;
                }
                if (ThemeFix.getThemeFolder(getInfo().getFileObject(), value) == null)
                {
                    AnnotationTree annotationTree =
                            (AnnotationTree) info.getTrees().getTree(type,
                                    theme);
                    AssignmentTree assignment =
                            AbstractJavaFix.getAnnotationTreeAttribute(
                                    annotationTree, JavaUtils.VALUE);
                    List<Integer> positions =
                            AbstractJavaFix
                                    .getElementPosition(info, assignment);
                    Fix themeFix =
                            new ThemeFix(info.getFileObject(),
                                    ElementHandle.create(type), value);
                    ErrorDescription description =
                            ErrorDescriptionFactory.createErrorDescription(
                                    getSeverity(Severity.HINT),
                                    Bundle.notThemeFound(value),
                                    Collections.singletonList(themeFix),
                                    info.getFileObject(), positions.get(0),
                                    positions.get(1));
                    getDescriptions().add(description);
                    myNoSpecifiedTheme = description;
                }
            }
        }
    }

    public ErrorDescription getNoThemeAnnotation() {
        return myNoThemeAnnotation;
    }

    public ErrorDescription getNoSpecifiedTheme() {
        return myNoSpecifiedTheme;
    }

    public static boolean checkBundledTheme( VaadinSupport support, String theme )
    {
        if (support == null) {
            return false;
        }
        ClasspathInfo classPathInfo = support.getClassPathInfo();
        if (classPathInfo == null) {
            // can happen when the project is closing
            return false;
        }
        ClassPath classPath = classPathInfo.getClassPath(PathKind.COMPILE);
        List<FileObject> themesFolders =
                classPath.findAllResources(VAADIN_THEMES);
        for (FileObject themeFolder : themesFolders) {
            FileObject[] children = themeFolder.getChildren();
            for (FileObject fileObject : children) {
                if (theme.equals(fileObject.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private ErrorDescription myNoThemeAnnotation;

    private ErrorDescription myNoSpecifiedTheme;

}
