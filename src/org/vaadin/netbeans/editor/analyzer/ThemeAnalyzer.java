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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.code.generator.JavaUtils;
import org.vaadin.netbeans.editor.VaadinTaskFactory;

/**
 * @author denis
 */
public class ThemeAnalyzer implements TypeAnalyzer {

    private static final String VAADIN_UI = "com.vaadin.ui.UI"; // NOI18N

    private static final String THEME = "com.vaadin.annotations.Theme";// NOI18N

    @NbBundle.Messages("noThemeFound=Custom Vaadin Theme is not specified")
    @Override
    public void analyze( TypeElement type, CompilationInfo info,
            Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {
        TypeElement ui = info.getElements().getTypeElement(VAADIN_UI);
        if (info.getTypes().isSubtype(type.asType(), ui.asType())) {
            if (!JavaUtils.hasAnnotation(type, THEME)) {
                List<Integer> positions = AbstractJavaFix.getElementPosition(
                        info, type);
                Fix themeFix = new ThemeFix(info.getFileObject(),
                        ElementHandle.create(type));
                ErrorDescription description = ErrorDescriptionFactory
                        .createErrorDescription(Severity.HINT,
                                Bundle.noThemeFound(),
                                Collections.singletonList(themeFix),
                                info.getFileObject(), positions.get(0),
                                positions.get(1));
                descriptions.add(description);

            }
        }
    }

}
