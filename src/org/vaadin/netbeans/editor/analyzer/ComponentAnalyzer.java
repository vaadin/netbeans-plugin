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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.netbeans.spi.java.hints.HintContext;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.editor.hints.Analyzer;
import org.vaadin.netbeans.utils.JavaUtils;

/**
 * @author denis
 */
public class ComponentAnalyzer extends Analyzer {

    public ComponentAnalyzer( HintContext context ) {
        super(context);
    }

    public ErrorDescription getNoConnector() {
        if (getDescriptions().isEmpty()) {
            return null;
        }
        else {
            return getDescriptions().iterator().next();
        }
    }

    @NbBundle.Messages("noConnector=No Connector class for component found")
    @Override
    public void analyze() {
        Set<Modifier> modifiers = getType().getModifiers();
        if (isClientConnector() && !modifiers.contains(Modifier.ABSTRACT)) {
            TypeElement connector =
                    StateAccessorAnalyzer.getConnector(getType(), getInfo());
            if (connector == null) {
                List<Integer> positions =
                        AbstractJavaFix
                                .getElementPosition(getInfo(), getType());
                ErrorDescription description =
                        ErrorDescriptionFactory.createErrorDescription(
                                getSeverity(Severity.HINT),
                                Bundle.noConnector(), createConnectorFixes(),
                                getInfo().getFileObject(), positions.get(0),
                                positions.get(1));
                getDescriptions().add(description);
            }
        }
    }

    private List<Fix> createConnectorFixes() {
        Collection<? extends TypeMirror> superclasses =
                JavaUtils.getSupertypes(getType().asType(), getInfo());
        TypeElement connector = null;
        for (TypeMirror type : superclasses) {
            Element element = getInfo().getTypes().asElement(type);
            if (element.getKind().equals(ElementKind.CLASS)
                    && element instanceof TypeElement)
            {
                TypeElement typeElement = (TypeElement) element;
                connector =
                        StateAccessorAnalyzer.getConnector(typeElement,
                                getInfo());
                if (connector != null) {
                    break;
                }
            }
        }

        ElementHandle<TypeElement> connectorHandle = null;
        if (connector != null) {
            connectorHandle = ElementHandle.create(connector);
        }

        List<Fix> fixes = new ArrayList<>(2);
        fixes.add(new CreateConnectorFix(ElementHandle.create(getType()),
                connectorHandle, getInfo().getFileObject(), false));
        fixes.add(new CreateConnectorFix(ElementHandle.create(getType()),
                connectorHandle, getInfo().getFileObject(), true));
        return fixes;
    }

    private boolean isClientConnector() {
        TypeElement clientConnector =
                getInfo().getElements().getTypeElement(
                        ConnectorAnalyzer.CLIENT_CONNECTOR);
        if (clientConnector == null) {
            return false;
        }
        return getInfo().getTypes().isSubtype(getType().asType(),
                clientConnector.asType());
    }

}