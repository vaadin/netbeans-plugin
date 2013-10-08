package org.vaadin.netbeans.editor.analyzer;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.netbeans.api.java.source.CompilationInfo;
import org.vaadin.netbeans.code.generator.JavaUtils;

/**
 * @author denis
 */
public class GwtClassesAnalyzer extends ClientClassAnalyzer {

    private static String GWT_USER_CLIENT_PACKAGE = "com.google.gwt.user.client."; //NOI18N

    @Override
    protected boolean isClientClass( TypeElement type, CompilationInfo info ) {
        Collection<? extends TypeMirror> supertypes = JavaUtils.getSupertypes(
                type.asType(), info);
        for (TypeMirror typeMirror : supertypes) {
            Logger.getLogger(GwtClassesAnalyzer.class.getName()).log(
                    Level.INFO, "Found super type for {0} : {1}",
                    new Object[] { type, typeMirror });
            Element typeElement = info.getTypes().asElement(typeMirror);
            if (typeElement instanceof TypeElement) {
                String fqn = ((TypeElement) typeElement).getQualifiedName()
                        .toString();
                if (fqn.startsWith(GWT_USER_CLIENT_PACKAGE)) {
                    return true;
                }
            }
        }
        return false;
    }
}
