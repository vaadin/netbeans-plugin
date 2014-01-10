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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.spi.java.hints.HintContext;
import org.vaadin.netbeans.utils.JavaUtils;

/**
 * @author denis
 */
public class GwtClassesAnalyzer extends ClientClassAnalyzer {

    private static String GWT_USER_CLIENT_PACKAGE =
            "com.google.gwt.user.client."; //NOI18N

    public GwtClassesAnalyzer( HintContext context ) {
        super(context, true);
    }

    @Override
    public void analyze() {
        TypeElement type = getType();
        if (type == null) {
            return;
        }
        if (isClientClass(getType(), getInfo())) {
            checkClientPackage();
        }
    }

    private boolean isClientClass( TypeElement type, CompilationInfo info ) {
        Collection<? extends TypeMirror> supertypes =
                JavaUtils.getSupertypes(type.asType(), info);
        for (TypeMirror typeMirror : supertypes) {
            Logger.getLogger(GwtClassesAnalyzer.class.getName()).log(
                    Level.FINE, "Found super type for {0} : {1}",
                    new Object[] { type, typeMirror });
            Element typeElement = info.getTypes().asElement(typeMirror);
            if (typeElement instanceof TypeElement) {
                String fqn =
                        ((TypeElement) typeElement).getQualifiedName()
                                .toString();
                if (fqn.startsWith(GWT_USER_CLIENT_PACKAGE)) {
                    return true;
                }
            }
        }
        return false;
    }
}
