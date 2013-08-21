/**
 *
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
