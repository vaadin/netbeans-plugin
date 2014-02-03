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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.netbeans.spi.java.hints.HintContext;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.editor.hints.Analyzer;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;
import org.vaadin.netbeans.utils.JavaUtils;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;

/**
 * @author denis
 */
public class VaadinServletConfigurationAnalyzer extends Analyzer {

    public VaadinServletConfigurationAnalyzer( HintContext context ) {
        super(context);
    }

    public ErrorDescription getNoGwtModule() {
        return myNoGwtModule;
    }

    public ErrorDescription getExtendsError() {
        return myExtendsError;
    }

    public ErrorDescription getNoWidgetset() {
        return myNoWidgetset;
    }

    @NbBundle.Messages({
            "configAnnotationError=@VaadinServletConfiguration is intended to use only with VaadinServlet subclass",
            "# {0} - module", "noGwtModule=GWT module {0} does not exist" })
    @Override
    public void analyze() {
        TypeElement type = getType();
        if (type == null) {
            return;
        }
        CompilationInfo info = getInfo();
        FileObject fileObject = info.getFileObject();
        VaadinSupport support = getSupport();
        if (support == null || !support.isEnabled()) {
            return;
        }
        AnnotationMirror config =
                JavaUtils.getAnnotation(type,
                        JavaUtils.VAADIN_SERVLET_CONFIGURATION);
        if (config == null || isCanceled()) {
            return;
        }

        TypeElement servlet =
                info.getElements().getTypeElement(JavaUtils.VAADIN_SERVLET);
        if (servlet != null) {
            if (!info.getTypes().isSubtype(type.asType(), servlet.asType())) {
                myExtendsError =
                        AbstractJavaFix.createExtendServletFix(type, info,
                                Bundle.configAnnotationError(),
                                getSeverity(Severity.ERROR));
                getDescriptions().add(myExtendsError);
            }
        }
        if (isCanceled() || !support.isReady()) {
            return;
        }

        String widgetset = JavaUtils.getValue(config, JavaUtils.WIDGETSET);
        final FileObject[] gwtXml = new FileObject[1];
        try {
            support.runModelOperation(new ModelOperation() {

                @Override
                public void run( VaadinModel model ) {
                    gwtXml[0] = model.getGwtXml();
                }
            });
        }
        catch (IOException e) {
            Logger.getLogger(VaadinServletConfigurationAnalyzer.class.getName())
                    .log(Level.INFO, null, e);
        }

        if (isCanceled()) {
            return;
        }

        if (gwtXml[0] == null) {
            if (widgetset != null) {
                AnnotationTree annotationTree =
                        (AnnotationTree) info.getTrees().getTree(type, config);
                AssignmentTree assignment =
                        AbstractJavaFix.getAnnotationTreeAttribute(
                                annotationTree, JavaUtils.WIDGETSET);
                List<Integer> positions =
                        AbstractJavaFix.getElementPosition(info, assignment);
                myNoGwtModule =
                        ErrorDescriptionFactory
                                .createErrorDescription(
                                        getSeverity(Severity.ERROR),
                                        Bundle.noGwtModule(widgetset),
                                        Collections
                                                .<Fix> singletonList(new CreateGwtModuleFix(
                                                        widgetset, fileObject)),
                                        info.getFileObject(), positions.get(0),
                                        positions.get(1));
                getDescriptions().add(myNoGwtModule);
            }
            else {
                // TODO : add hint to create GWT module (low priority)
            }
            return;
        }
        String foundWidgetset = AbstractJavaFix.getWidgetsetFqn(gwtXml[0]);
        if (widgetset == null) {
            AnnotationTree annotationTree =
                    (AnnotationTree) info.getTrees().getTree(type, config);
            List<Integer> positions =
                    AbstractJavaFix.getElementPosition(info, annotationTree);
            myNoWidgetset =
                    ErrorDescriptionFactory.createErrorDescription(
                            getSeverity(Severity.HINT), Bundle
                                    .noWidgetset(foundWidgetset), Collections
                                    .<Fix> singletonList(new SetWidgetsetFix(
                                            foundWidgetset, fileObject,
                                            ElementHandle.create(type))), info
                                    .getFileObject(), positions.get(0),
                            positions.get(1));
            getDescriptions().add(myNoWidgetset);
        }
        else if (!foundWidgetset.equals(widgetset)) {
            AnnotationTree annotationTree =
                    (AnnotationTree) info.getTrees().getTree(type, config);
            AssignmentTree assignment =
                    AbstractJavaFix.getAnnotationTreeAttribute(annotationTree,
                            JavaUtils.WIDGETSET);
            List<Integer> positions =
                    AbstractJavaFix.getElementPosition(info, assignment);
            List<Fix> fixes = new ArrayList<>(1);
            fixes.add(new SetWidgetsetFix(foundWidgetset, fileObject,
                    ElementHandle.create(type)));
            // don't add fix if there is already GWT.xml available
            //fixes.add(new CreateGwtModuleFix(widgetset, fileObject, factory));
            myNoGwtModule =
                    ErrorDescriptionFactory.createErrorDescription(
                            getSeverity(Severity.ERROR),
                            Bundle.noGwtModule(widgetset), fixes,
                            info.getFileObject(), positions.get(0),
                            positions.get(1));
            getDescriptions().add(myNoGwtModule);
        }
    }

    private ErrorDescription myNoGwtModule;

    private ErrorDescription myExtendsError;

    private ErrorDescription myNoWidgetset;
}
