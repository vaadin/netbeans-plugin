/**
 *
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
public class ConnectorAnalyzer implements TypeAnalyzer {

    private static final String CONNECTOR = "com.vaadin.shared.ui.Connect"; // NOI18N

    private static final String CLIENT_CONNECTOR = "com.vaadin.server.ClientConnector"; // NOI18N

    private static final String SERVER_CONNECTOR = "com.vaadin.client.ServerConnector"; // NOI18N

    private static final String ABSTRACT_COMPONENT_CONNECTOR = "com.vaadin.client.ui.AbstractComponentConnector"; // NOI18N

    @Override
    public void analyze( TypeElement type, CompilationInfo info,
            Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {
        AnnotationMirror annotation = JavaUtils.getAnnotation(type, CONNECTOR);
        if (annotation == null) {
            return;
        }
        checkConnectorValue(type, info, annotation, descriptions);
        checkConnectorClass(type, info, annotation, descriptions);
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
             * TODO: provide fix hints: 
             * - analyze @Connect annotation value. 
             *   If it's AbstactComponent sublcass then add hint to derive class from AbstractComponentConnector
             *   (if it doesn't have superclass )
             *   If it's AbstractExtension  sublcass then add hint to derive class from AbstractExtensionConnector 
             *   (if it doesn't have superclass ) 
             * - if it has already incompatible superclass then add hint to implement ServerConnector
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
