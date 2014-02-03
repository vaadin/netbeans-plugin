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

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;

import org.netbeans.spi.java.hints.HintContext;
import org.openide.util.NbBundle;

/**
 * @author denis
 */
public class SharedStateAnalyzer extends AbstractJavaBeanAnalyzer {

    private static final String JAVA_SCRIPT_STATE =
            "com.vaadin.shared.ui.JavaScriptComponentState"; // NOI18N

    static final String SHARED_STATE =
            "com.vaadin.shared.communication.SharedState"; // NOI18N

    public SharedStateAnalyzer( HintContext context, Mode mode ) {
        super(context, mode);
    }

    @Override
    public void analyze() {
        if (getType() == null) {
            return;
        }
        if (isSharedState()) {
            if (isPackageCheckMode()) {
                checkClientPackage();
            }
            else {
                checkJavaBean(null, (DeclaredType) getType().asType());
            }
        }
    }

    @Override
    protected void checkClientPackage() {
        TypeElement jsState =
                getInfo().getElements().getTypeElement(JAVA_SCRIPT_STATE);
        if (jsState == null
                || !getInfo().getTypes().isSubtype(getType().asType(),
                        jsState.asType()))
        {
            super.checkClientPackage();
        }
    }

    @NbBundle.Messages({ "# {0} - classFqn",
            "notSerializable=Not serializable class {0} is used in field declaration" })
    @Override
    protected String getNotSerializableFieldMessage(
            VariableElement checkTarget, VariableElement field, String fqn )
    {
        return Bundle.notSerializable(fqn);
    }

    @NbBundle.Messages("noGetter=JavaBeans specification is violated. Couldn't find getter method")
    @Override
    protected String getNoGetterMessage( VariableElement checkTarget,
            VariableElement field )
    {
        return Bundle.noGetter();
    }

    @NbBundle.Messages("noSetter=JavaBeans specification is violated. Couldn't find setter method")
    @Override
    protected String getNoSetterMessage( VariableElement checkTarget,
            VariableElement field )
    {
        return Bundle.noSetter();
    }

    @NbBundle.Messages({ "noAccessors=JavaBeans specification is violated. Couldn't find accessor methods" })
    @Override
    protected String getNoAccessorsMessage( VariableElement target,
            VariableElement field )
    {
        return Bundle.noAccessors();
    }

    private boolean isSharedState() {
        TypeElement sharedState =
                getInfo().getElements().getTypeElement(SHARED_STATE);
        if (sharedState == null) {
            return false;
        }
        return getInfo().getTypes().isSubtype(getType().asType(),
                sharedState.asType());
    }
}
