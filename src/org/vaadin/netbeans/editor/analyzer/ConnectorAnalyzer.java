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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.netbeans.spi.java.hints.HintContext;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.code.WidgetUtils;
import org.vaadin.netbeans.code.generator.WidgetGenerator;
import org.vaadin.netbeans.utils.JavaUtils;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;

/**
 * @author denis
 */
public class ConnectorAnalyzer extends ClientClassAnalyzer {

    private static final String VAADIN_PKG = "com.vaadin."; // NOI18N

    static final String CLIENT_CONNECTOR = VAADIN_PKG
            + "server.ClientConnector"; // NOI18N

    static final String SERVER_CONNECTOR = VAADIN_PKG
            + "client.ServerConnector"; // NOI18N

    public ConnectorAnalyzer( HintContext context, boolean packageCheckMode ) {
        super(context, packageCheckMode);
    }

    @Override
    public void analyze() {
        if (getType() == null) {
            return;
        }
        if (isCanceled()) {
            return;
        }
        if (isPackageCheckMode()) {
            if (isConnector()) {
                checkClientPackage();
            }
        }
        else {
            AnnotationMirror annotation =
                    JavaUtils.getAnnotation(getType(), WidgetUtils.CONNECTOR);

            if (annotation == null) {
                checkConnectAnnotation();
                return;
            }
            checkConnectorValue(annotation);
            if (isCanceled()) {
                return;
            }
            checkConnectorClass(annotation);
            if (isCanceled()) {
                return;
            }
        }
    }

    public ErrorDescription getBadConnectValue() {
        return myBadConnectValue;
    }

    public ErrorDescription getBadConnectorClass() {
        return myBadConnectorClass;
    }

    public ErrorDescription getNoConnectAnnotation() {
        return myNoConnectAnnotation;
    }

    private boolean isConnector() {
        TypeElement serverConnector =
                getInfo().getElements().getTypeElement(SERVER_CONNECTOR);
        if (serverConnector == null) {
            return false;
        }
        return getInfo().getTypes().isSubtype(getType().asType(),
                serverConnector.asType());
    }

    @NbBundle.Messages("notConnected=No @Connect annotation found")
    private void checkConnectAnnotation() {
        if (isConnector()) {
            Set<Modifier> modifiers = getType().getModifiers();
            if (modifiers.contains(Modifier.ABSTRACT)) {
                return;
            }
            List<Integer> positions =
                    AbstractJavaFix.getElementPosition(getInfo(), getType());

            List<Fix> fixes = new ArrayList<Fix>(2);

            TypeElement clientConnector =
                    getInfo().getElements().getTypeElement(CLIENT_CONNECTOR);
            if (clientConnector != null) {
                Set<FileObject> sourceRoots =
                        IsInSourceQuery.getSourceRoots(getInfo());
                List<String> allSourceComponents = new LinkedList<>();
                String probableComponentFqn = null;
                try {
                    probableComponentFqn =
                            collectExistingComponents(clientConnector,
                                    sourceRoots, allSourceComponents);
                }
                catch (InterruptedException e) {
                    Logger.getLogger(ConnectorAnalyzer.class.getName()).log(
                            Level.INFO, null, e);
                }
                ElementHandle<TypeElement> handle =
                        ElementHandle.create(getType());
                if (probableComponentFqn != null) {
                    fixes.add(new ConnectFix(getInfo().getFileObject(), handle,
                            probableComponentFqn));
                }
                if (!allSourceComponents.isEmpty()) {
                    fixes.add(new ConnectFix(getInfo().getFileObject(), handle,
                            allSourceComponents));
                }
            }

            myNoConnectAnnotation =
                    ErrorDescriptionFactory.createErrorDescription(
                            getSeverity(Severity.WARNING), Bundle
                                    .notConnected(), fixes, getInfo()
                                    .getFileObject(), positions.get(0),
                            positions.get(1));
            getDescriptions().add(myNoConnectAnnotation);
        }
    }

    private String collectExistingComponents( TypeElement clientConnector,
            Set<FileObject> sourceRoots, List<String> sourceComponents )
            throws InterruptedException
    {
        String connectorName = getType().getSimpleName().toString();
        int index = connectorName.indexOf(WidgetGenerator.CONNECTOR);
        if (index >= 0) {
            connectorName = connectorName.substring(0, index);
        }
        String componentFqn = null;
        Set<TypeElement> components =
                JavaUtils.getSubclasses(clientConnector, getInfo());
        for (TypeElement component : components) {
            Set<Modifier> modifiers = component.getModifiers();
            if (modifiers.contains(Modifier.ABSTRACT)) {
                continue;
            }
            String fqn = component.getQualifiedName().toString();
            if (fqn.startsWith(VAADIN_PKG)) {
                continue;
            }
            if (IsInSourceQuery.isInSourceRoots(component, getInfo(),
                    sourceRoots))
            {
                sourceComponents.add(fqn);
                String simpleName = component.getSimpleName().toString();
                if (simpleName.equals(connectorName)) {
                    componentFqn = fqn;
                }
            }
        }
        return componentFqn;
    }

    @NbBundle.Messages("badConnectorClass=@Connect annotation must be attached to ServerConnector subclass")
    private void checkConnectorClass( AnnotationMirror annotation ) {
        CompilationInfo info = getInfo();
        TypeElement serverConnector =
                info.getElements().getTypeElement(SERVER_CONNECTOR);
        if (serverConnector == null) {
            return;
        }
        if (!info.getTypes().isSubtype(getType().asType(),
                serverConnector.asType()))
        {
            List<Integer> positions =
                    AbstractJavaFix.getElementPosition(info, getType());

            myBadConnectorClass =
                    ErrorDescriptionFactory.createErrorDescription(
                            getSeverity(Severity.ERROR),
                            Bundle.badConnectorClass(),
                            Collections.<Fix> emptyList(),
                            info.getFileObject(), positions.get(0),
                            positions.get(1));
            getDescriptions().add(myBadConnectorClass);
        }
    }

    @NbBundle.Messages("badConnectValue=@Connect annotation value must be a ClientConnector subclass")
    private void checkConnectorValue( AnnotationMirror annotation ) {
        CompilationInfo info = getInfo();
        TypeElement clientConnector =
                info.getElements().getTypeElement(CLIENT_CONNECTOR);
        if (clientConnector == null) {
            return;
        }
        AnnotationValue component =
                JavaUtils.getAnnotationValue(annotation, JavaUtils.VALUE);

        if (component == null) {
            return;
        }
        Object value = component.getValue();
        if (value instanceof TypeMirror) {
            if (!info.getTypes().isSubtype((TypeMirror) value,
                    clientConnector.asType()))
            {
                AnnotationTree tree =
                        (AnnotationTree) info.getTrees().getTree(getType(),
                                annotation);
                ExpressionTree expressionTree = tree.getArguments().get(0);
                List<Integer> positions =
                        AbstractJavaFix
                                .getElementPosition(info, expressionTree);
                myBadConnectValue =
                        ErrorDescriptionFactory.createErrorDescription(
                                getSeverity(Severity.ERROR),
                                Bundle.badConnectValue(),
                                Collections.<Fix> emptyList(),
                                info.getFileObject(), positions.get(0),
                                positions.get(1));
                getDescriptions().add(myBadConnectValue);
            }
        }
    }

    private ErrorDescription myBadConnectValue;

    private ErrorDescription myBadConnectorClass;

    private ErrorDescription myNoConnectAnnotation;
}
