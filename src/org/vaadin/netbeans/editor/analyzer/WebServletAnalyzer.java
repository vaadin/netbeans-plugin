/**
 *
 */
package org.vaadin.netbeans.editor.analyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.code.generator.JavaUtils;
import org.vaadin.netbeans.code.generator.XmlUtils;
import org.vaadin.netbeans.editor.VaadinTaskFactory;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.Tree;

/**
 * @author denis
 */
public class WebServletAnalyzer implements TypeAnalyzer {

    @Override
    public void analyze( TypeElement type, CompilationInfo info,
            Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {
        FileObject fileObject = info.getFileObject();
        Project project = FileOwnerQuery.getOwner(fileObject);
        if (project == null) {
            return;
        }
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        if (support == null || !support.isEnabled()) {
            return;
        }
        AnnotationMirror servlet = JavaUtils.getAnnotation(type,
                JavaUtils.SERVLET_ANNOTATION);
        if (servlet == null || cancel.get()) {
            return;
        }

        String widgetset = JavaUtils.getWidgetsetWebInit(servlet);
        String ui = JavaUtils.getWebInitParamValue(servlet, JavaUtils.UI);

        TypeElement vaadinServlet = info.getElements().getTypeElement(
                JavaUtils.VAADIN_SERVLET);
        if (vaadinServlet == null || cancel.get()) {
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

        if (cancel.get()) {
            return;
        }

        if (info.getTypes().isSubtype(type.asType(), vaadinServlet.asType())) {
            if (ui == null) {
                noUiVaadinServlet(type, info, servlet, descriptions);
            }
            else {
                if (widgetset == null) {
                    noWidgetsetVaadinServlet(gwtXml[0], servlet, type, info,
                            descriptions);
                }
                else {
                    checkWidgetset(widgetset, type, info, descriptions, factory);
                }
            }
        }
        else {
            if (ui != null || widgetset != null) {
                requireVaadinServlet(type, info, descriptions);
            }
        }
    }

    @NbBundle.Messages("badClassHierarchy=Class has Vaadin specific annotation parameter but is not derived from VaadinServlet")
    private void requireVaadinServlet( TypeElement type, CompilationInfo info,
            Collection<ErrorDescription> descriptions )
    {
        descriptions.add(AbstractJavaFix.createExtendServletFix(type, info,
                Bundle.badClassHierarchy(), Severity.WARNING));
    }

    private void checkWidgetset( String widgetset, TypeElement type,
            CompilationInfo info, Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory )
    {
        // TODO : Check widgetset against gwtXml and provide hints for correction/creation if it differs
    }

    private void noWidgetsetVaadinServlet( FileObject gwtXml,
            AnnotationMirror servlet, TypeElement type, CompilationInfo info,
            Collection<ErrorDescription> descriptions )
    {
        if (gwtXml == null) {
            // TODO : add hint to create and set gwt module name (low priority)
        }
        else {
            String name = gwtXml.getNameExt();
            if (name.endsWith(XmlUtils.GWT_XML)) {
                name = name.substring(0,
                        name.length() - XmlUtils.GWT_XML.length());
            }
            AnnotationTree annotationTree = (AnnotationTree) info.getTrees()
                    .getTree(type, servlet);
            AssignmentTree assignment = AbstractJavaFix
                    .getAnnotationTreeAttribute(annotationTree,
                            JavaUtils.INIT_PARAMS);
            List<Integer> positions = AbstractJavaFix.getElementPosition(info,
                    assignment);
            ErrorDescription description = ErrorDescriptionFactory
                    .createErrorDescription(Severity.HINT, Bundle
                            .noGwtModule(name), Collections
                            .<Fix> singletonList(new SetServletWidgetsetFix(
                                    AbstractJavaFix.getWidgetsetFqn(gwtXml),
                                    info.getFileObject(), ElementHandle
                                            .create(type))), info
                            .getFileObject(), positions.get(0), positions
                            .get(1));
            descriptions.add(description);
        }
    }

    @NbBundle.Messages("noUiProvided=Servlet class extends VaadinServlet but there is no UI class specified")
    private void noUiVaadinServlet( TypeElement type, CompilationInfo info,
            AnnotationMirror servlet, Collection<ErrorDescription> descriptions )
    {
        try {
            TypeElement uiClass = info.getElements().getTypeElement(
                    JavaUtils.VAADIN_UI_FQN);
            Set<TypeElement> uis = JavaUtils.getSubclasses(uiClass, info);
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
            Tree annotationTree = info.getTrees().getTree(type, servlet);
            for (TypeElement ui : uis) {
                fixes.add(new SetUiServletParameterFix(ui.getQualifiedName()
                        .toString(), info.getFileObject(), ElementHandle
                        .create(type)));
            }
            List<Integer> positions = AbstractJavaFix.getElementPosition(info,
                    annotationTree);
            ErrorDescription description = ErrorDescriptionFactory
                    .createErrorDescription(Severity.WARNING,
                            Bundle.noUiProvided(), fixes, info.getFileObject(),
                            positions.get(0), positions.get(1));
            descriptions.add(description);
        }
        catch (InterruptedException ignore) {
        }
    }
}
