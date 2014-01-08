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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
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
public class AcceptCriterionAnalyzer extends Analyzer {

    private static final String ACCEPT_CRITERION_ANNOTATION =
            "com.vaadin.shared.ui.dd.AcceptCriterion"; // NOI18N

    private static final String ACCEPT_CRITERION =
            "com.vaadin.event.dd.acceptcriteria.AcceptCriterion"; // NOI18N

    public AcceptCriterionAnalyzer( HintContext context ) {
        super(context);
    }

    @NbBundle.Messages("noAcceptCriterionClient=No client side class found for this AcceptCriterion")
    @Override
    public void analyze() {
        TypeElement acceptCriterionSuper =
                getInfo().getElements().getTypeElement(ACCEPT_CRITERION);
        if (acceptCriterionSuper == null) {
            return;
        }
        if (!getInfo().getTypes().isSubtype(getType().asType(),
                acceptCriterionSuper.asType()))
        {
            return;
        }
        try {
            if (!hasClientClass()) {
                List<Integer> positions =
                        AbstractJavaFix
                                .getElementPosition(getInfo(), getType());
                ErrorDescription description =
                        ErrorDescriptionFactory
                                .createErrorDescription(
                                        getSeverity(Severity.ERROR),
                                        Bundle.noAcceptCriterionClient(),
                                        Collections
                                                .<Fix> singletonList(getCreateClientFix()),
                                        getInfo().getFileObject(), positions
                                                .get(0), positions.get(1));
                getDescriptions().add(description);
            }
        }
        catch (InterruptedException e) {
            Logger.getLogger(AcceptCriterionAnalyzer.class.getName()).log(
                    Level.INFO, null, e);
        }
    }

    private Fix getCreateClientFix() {
        return new CreateClientAcceptCriterion(ElementHandle.create(getType()),
                getInfo().getFileObject());
    }

    private boolean hasClientClass() throws InterruptedException {
        List<TypeElement> clientClasses =
                JavaUtils.findAnnotatedElements(ACCEPT_CRITERION_ANNOTATION,
                        getInfo());
        for (TypeElement type : clientClasses) {
            TypeMirror criteriaType = getCriteria(type);
            if (getInfo().getTypes().isSameType(getType().asType(),
                    criteriaType))
            {
                return true;
            }
        }
        return false;
    }

    private TypeMirror getCriteria( TypeElement clientClass ) {
        AnnotationMirror annotation =
                JavaUtils.getAnnotation(clientClass,
                        ACCEPT_CRITERION_ANNOTATION);
        return getCriteria(JavaUtils.getAnnotationValue(annotation,
                JavaUtils.VALUE));

    }

    private TypeMirror getCriteria( AnnotationValue value ) {
        if (value != null) {
            Object object = value.getValue();
            if (object instanceof TypeMirror) {
                return (TypeMirror) object;
            }
        }
        return null;
    }

    public ErrorDescription getNoClientClass() {
        if (getDescriptions().isEmpty()) {
            return null;
        }
        return getDescriptions().iterator().next();
    }

}
