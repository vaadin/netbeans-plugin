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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
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
import com.sun.source.tree.Tree;

/**
 * @author denis
 */
public class WebServletAnalyzer extends Analyzer {

    public WebServletAnalyzer( HintContext context ) {
        super(context);
    }

    public ErrorDescription getNoUiVaadinServlet() {
        return myNoUiVaadinServlet;
    }

    public ErrorDescription getNoWidgetsetVaadinServlet() {
        return myNoWidgetsetVaadinServlet;
    }

    public ErrorDescription getNoGwtModule() {
        return myNoGwtModule;
    }

    public ErrorDescription getRequireVaadinServlet() {
        return myRequireVaadinServlet;
    }

    /**
     * @param myRequireVaadinServlet
     *            the myRequireVaadinServlet to set
     */
    public void setMyRequireVaadinServlet(
            ErrorDescription myRequireVaadinServlet )
    {
        this.myRequireVaadinServlet = myRequireVaadinServlet;
    }

    @Override
    public void analyze() {
        if (getType() == null) {
            return;
        }
        AnnotationMirror servlet =
                JavaUtils
                        .getAnnotation(getType(), JavaUtils.SERVLET_ANNOTATION);
        if (servlet == null || isCanceled()) {
            return;
        }

        String widgetset = JavaUtils.getWidgetsetWebInit(servlet);
        String ui = JavaUtils.getWebInitParamValue(servlet, JavaUtils.UI);

        TypeElement vaadinServlet =
                getInfo().getElements()
                        .getTypeElement(JavaUtils.VAADIN_SERVLET);
        if (vaadinServlet == null || isCanceled()) {
            return;
        }

        VaadinSupport support = getSupport();
        if (support == null || !support.isReady()) {
            return;
        }

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

        if (getInfo().getTypes().isSubtype(getType().asType(),
                vaadinServlet.asType()))
        {
            if (ui == null) {
                if (!JavaUtils.hasAnnotation(getType(),
                        JavaUtils.VAADIN_SERVLET_CONFIGURATION))
                {
                    noUiVaadinServlet(servlet, support);
                }
            }
            else {
                if (widgetset == null) {
                    noWidgetsetVaadinServlet(gwtXml[0], servlet);
                }
                else {
                    checkWidgetset(widgetset, gwtXml[0], servlet);
                }
            }
        }
        else {
            if (ui != null || widgetset != null) {
                requireVaadinServlet();
            }
        }
    }

    @NbBundle.Messages("badClassHierarchy=Class has Vaadin specific annotation parameter but is not derived from VaadinServlet")
    private void requireVaadinServlet() {
        ErrorDescription myRequireVaadinServlet =
                AbstractJavaFix.createExtendServletFix(getType(), getInfo(),
                        Bundle.badClassHierarchy(),
                        getSeverity(Severity.WARNING));
        getDescriptions().add(myRequireVaadinServlet);
    }

    private void checkWidgetset( String widgetset, FileObject gwtXml,
            AnnotationMirror servlet )
    {
        CompilationInfo info = getInfo();
        String name = AbstractJavaFix.getWidgetsetFqn(gwtXml);
        if (name == null) {
            List<Integer> positions =
                    AbstractJavaFix.getElementPosition(info,
                            getInitParamsTree(servlet, getType(), info));
            myNoGwtModule =
                    ErrorDescriptionFactory
                            .createErrorDescription(
                                    getSeverity(Severity.ERROR),
                                    Bundle.noGwtModule(widgetset),
                                    Collections
                                            .<Fix> singletonList(new CreateGwtModuleFix(
                                                    widgetset, info
                                                            .getFileObject())),
                                    info.getFileObject(), positions.get(0),
                                    positions.get(1));
            getDescriptions().add(myNoGwtModule);
        }
        else if (!widgetset.equals(name)) {
            myNoGwtModule =
                    createServletWidgetsetDescription(name, servlet, getType(),
                            info, getSeverity(Severity.ERROR),
                            Bundle.noGwtModule(name));
            getDescriptions().add(myNoGwtModule);
        }
    }

    @NbBundle.Messages({ "# {0} - widgetset",
            "noWidgetset=GWT module {0} exists but is not declared for VaadinServlet" })
    private void noWidgetsetVaadinServlet( FileObject gwtXml,
            AnnotationMirror servlet )
    {
        if (gwtXml == null) {
            // TODO : add hint to create and set gwt module name (low priority)
        }
        else {
            String name = AbstractJavaFix.getWidgetsetFqn(gwtXml);
            myNoWidgetsetVaadinServlet =
                    createServletWidgetsetDescription(name, servlet, getType(),
                            getInfo(), getSeverity(Severity.HINT),
                            Bundle.noWidgetset(name));
            getDescriptions().add(myNoWidgetsetVaadinServlet);
        }
    }

    private ErrorDescription createServletWidgetsetDescription(
            String widgetset, AnnotationMirror servlet, TypeElement type,
            CompilationInfo info, Severity severity, String descriptionText,
            Fix... moreFixes )
    {
        Tree tree = getInitParamsTree(servlet, type, info);
        List<Integer> positions =
                AbstractJavaFix.getElementPosition(info, tree);
        List<Fix> fixes;
        if (moreFixes.length == 0) {
            fixes =
                    Collections.<Fix> singletonList(new SetServletWidgetsetFix(
                            widgetset, info.getFileObject(), ElementHandle
                                    .create(type)));
        }
        else {
            fixes = new ArrayList<>(moreFixes.length + 1);
            fixes.add(new SetServletWidgetsetFix(widgetset, info
                    .getFileObject(), ElementHandle.create(type)));
            for (Fix fix : moreFixes) {
                fixes.add(fix);
            }
        }
        ErrorDescription description =
                ErrorDescriptionFactory.createErrorDescription(severity,
                        descriptionText, fixes, info.getFileObject(),
                        positions.get(0), positions.get(1));
        return description;
    }

    private Tree getInitParamsTree( AnnotationMirror servlet, TypeElement type,
            CompilationInfo info )
    {
        AnnotationTree annotationTree =
                (AnnotationTree) info.getTrees().getTree(type, servlet);
        AssignmentTree assignment =
                AbstractJavaFix.getAnnotationTreeAttribute(annotationTree,
                        JavaUtils.INIT_PARAMS);
        return assignment.getVariable();
    }

    @NbBundle.Messages("noUiProvided=Servlet class extends VaadinServlet but there is no UI class specified")
    private void noUiVaadinServlet( AnnotationMirror servlet,
            VaadinSupport support )
    {
        CompilationInfo info = getInfo();
        try {
            TypeElement uiClass =
                    info.getElements().getTypeElement(JavaUtils.VAADIN_UI_FQN);
            Collection<TypeElement> uis =
                    support.getDescendantStrategy().getSourceSubclasses(
                            uiClass, info);
            for (Iterator<TypeElement> iterator = uis.iterator(); iterator
                    .hasNext();)
            {
                TypeElement typeElement = iterator.next();
                Set<Modifier> modifiers = typeElement.getModifiers();
                if (modifiers.contains(Modifier.ABSTRACT)
                        || !modifiers.contains(Modifier.PUBLIC))
                {
                    iterator.remove();
                }
            }
            List<Fix> fixes = new ArrayList<>(uis.size());
            Tree annotationTree = info.getTrees().getTree(getType(), servlet);
            for (TypeElement ui : uis) {
                fixes.add(new SetUiServletParameterFix(ui.getQualifiedName()
                        .toString(), info.getFileObject(), ElementHandle
                        .create(getType())));
            }
            List<Integer> positions =
                    AbstractJavaFix.getElementPosition(info, annotationTree);
            myNoUiVaadinServlet =
                    ErrorDescriptionFactory.createErrorDescription(
                            getSeverity(Severity.WARNING),
                            Bundle.noUiProvided(), fixes, info.getFileObject(),
                            positions.get(0), positions.get(1));
            getDescriptions().add(myNoUiVaadinServlet);
        }
        catch (InterruptedException ignore) {
        }
    }

    private ErrorDescription myNoUiVaadinServlet;

    private ErrorDescription myNoWidgetsetVaadinServlet;

    private ErrorDescription myNoGwtModule;

    private ErrorDescription myRequireVaadinServlet;

}
