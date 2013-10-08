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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.editor.VaadinTaskFactory;

/**
 * @author denis
 */
public class SharedStateAnalyzer extends AbstractJavaBeanAnalyzer {

    private static final String SHARED_STATE = "com.vaadin.shared.communication.SharedState"; // NOI18N

    @Override
    protected boolean isClientClass( TypeElement type, CompilationInfo info ) {
        TypeElement sharedState = info.getElements().getTypeElement(
                SHARED_STATE);
        if (sharedState == null) {
            return false;
        }
        return info.getTypes().isSubtype(type.asType(), sharedState.asType());
    }

    @Override
    protected void checkClientClass( TypeElement type, CompilationInfo info,
            Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {
        checkJavaBean(null, (DeclaredType) type.asType(), info, descriptions,
                factory, cancel);
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

}
