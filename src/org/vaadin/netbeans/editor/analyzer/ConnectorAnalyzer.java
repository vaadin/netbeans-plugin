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

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.code.generator.JavaUtils;
import org.vaadin.netbeans.editor.VaadinTaskFactory;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;

/**
 * @author denis
 */
public class ConnectorAnalyzer extends ClientClassAnalyzer {

    private static final String CONNECTOR = "com.vaadin.shared.ui.Connect"; // NOI18N

    private static final String CLIENT_CONNECTOR = "com.vaadin.server.ClientConnector"; // NOI18N

    private static final String SERVER_CONNECTOR = "com.vaadin.client.ServerConnector"; // NOI18N

    private static final String ABSTRACT_COMPONENT_CONNECTOR = "com.vaadin.client.ui.AbstractComponentConnector"; // NOI18N

    @Override
    public void analyze( TypeElement type, CompilationInfo info,
            Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {
        super.analyze(type, info, descriptions, factory, cancel);
        if (cancel.get()) {
            return;
        }
        AnnotationMirror annotation = JavaUtils.getAnnotation(type, CONNECTOR);
        if (annotation == null) {
            return;
        }
        if (cancel.get()) {
            return;
        }
        checkConnectorValue(type, info, annotation, descriptions);
        if (cancel.get()) {
            return;
        }
        checkConnectorClass(type, info, annotation, descriptions);
    }

    @Override
    protected boolean isClientClass( TypeElement type, CompilationInfo info ) {
        TypeElement serverConnector = info.getElements().getTypeElement(
                SERVER_CONNECTOR);
        if (serverConnector == null) {
            return false;
        }
        return info.getTypes().isSubtype(type.asType(),
                serverConnector.asType());
    }

    @NbBundle.Messages("badConnectorClass=@Connect annotation must attached to ServerConnector subclass")
    private void checkConnectorClass( TypeElement type, CompilationInfo info,
            AnnotationMirror annotation,
            Collection<ErrorDescription> descriptions )
    {
        TypeElement serverConnector = info.getElements().getTypeElement(
                SERVER_CONNECTOR);
        if (serverConnector == null) {
            return;
        }
        if (!info.getTypes().isSubtype(type.asType(), serverConnector.asType()))
        {
            List<Integer> positions = AbstractJavaFix.getElementPosition(info,
                    type);

            /*
             * TODO: provide fix hints: - analyze @Connect annotation value. If
             * it's AbstactComponent subclass then add hint to derive class from
             * AbstractComponentConnector (if it doesn't have superclass ) If
             * it's AbstractExtension subclass then add hint to derive class
             * from AbstractExtensionConnector (if it doesn't have superclass )
             * - if it has already incompatible superclass then add hint to
             * implement ServerConnector
             */
            ErrorDescription description = ErrorDescriptionFactory
                    .createErrorDescription(Severity.ERROR,
                            Bundle.badConnectorClass(),
                            Collections.<Fix> emptyList(),
                            info.getFileObject(), positions.get(0),
                            positions.get(1));
            descriptions.add(description);
        }
    }

    @NbBundle.Messages("badConnectValue=@Connect annotation value must be a ClientConnector subclass")
    private void checkConnectorValue( TypeElement type, CompilationInfo info,
            AnnotationMirror annotation,
            Collection<ErrorDescription> descriptions )
    {
        TypeElement clientConnector = info.getElements().getTypeElement(
                CLIENT_CONNECTOR);
        if (clientConnector == null) {
            return;
        }
        AnnotationValue component = JavaUtils.getAnnotationValue(annotation,
                JavaUtils.VALUE);

        if (component == null) {
            return;
        }
        Object value = component.getValue();
        if (value instanceof TypeMirror) {
            if (!info.getTypes().isSubtype((TypeMirror) value,
                    clientConnector.asType()))
            {
                AnnotationTree tree = (AnnotationTree) info.getTrees().getTree(
                        type, annotation);
                ExpressionTree expressionTree = tree.getArguments().get(0);
                List<Integer> positions = AbstractJavaFix.getElementPosition(
                        info, expressionTree);
                ErrorDescription description = ErrorDescriptionFactory
                        .createErrorDescription(Severity.ERROR,
                                Bundle.badConnectValue(),
                                Collections.<Fix> emptyList(),
                                info.getFileObject(), positions.get(0),
                                positions.get(1));
                descriptions.add(description);
            }
        }
    }
}
