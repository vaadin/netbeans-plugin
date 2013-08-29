package org.vaadin.netbeans.editor.analyzer;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.code.generator.XmlUtils;
import org.vaadin.netbeans.editor.VaadinTaskFactory;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;

/**
 * @author denis
 */
abstract class ClientClassAnalyzer implements TypeAnalyzer {

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
        if (isClientClass(type, info)) {
            checkClientPackage(type, info, support, descriptions);
            checkClientClass(type, info, descriptions, factory, cancel);
        }
    }

    protected void checkClientClass( TypeElement type, CompilationInfo info,
            Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel )
    {
    }

    protected abstract boolean isClientClass( TypeElement type,
            CompilationInfo info );

    @NbBundle.Messages({
            "# {0} - clientPackage",
            "notClientPackage=Class''s package is incorrect, it must be inside client package {0}" })
    protected void checkClientPackage( TypeElement type, CompilationInfo info,
            VaadinSupport support, Collection<ErrorDescription> descriptions )
    {
        final FileObject[] clientPackage = new FileObject[1];
        final boolean[] hasWidgetset = new boolean[1];
        final String[] clientPkgFqn = new String[1];
        try {
            support.runModelOperation(new ModelOperation() {

                @Override
                public void run( VaadinModel model ) {
                    FileObject gwtXml = model.getGwtXml();
                    hasWidgetset[0] = gwtXml != null;
                    if (gwtXml == null) {
                        return;
                    }
                    try {
                        clientPackage[0] = XmlUtils.getClientWidgetPackage(
                                model.getGwtXml(), model.getSourcePath(), false);
                        clientPkgFqn[0] = AbstractJavaFix
                                .getWidgetsetFqn(gwtXml);
                        clientPkgFqn[0] = clientPkgFqn[0].substring(0,
                                clientPkgFqn[0].length()
                                        - gwtXml.getNameExt().length()
                                        + XmlUtils.GWT_XML.length())
                                + model.getSourcePath();
                    }
                    catch (IOException ignore) {
                    }
                }
            });
            if (!hasWidgetset[0]) {
                // TODO : no widgetset file error
            }
            else if (clientPackage[0] == null
                    || !FileUtil.isParentOf(clientPackage[0],
                            info.getFileObject()))
            {
                if (clientPackage[0] == null) {
                    // TODO : for fix hint. Package has to be created
                }
                // TODO : provide a hint to move class into the client package (using refactoring)
                List<Integer> positions = AbstractJavaFix.getElementPosition(
                        info, type);
                ErrorDescription description = ErrorDescriptionFactory
                        .createErrorDescription(Severity.ERROR,
                                Bundle.notClientPackage(clientPkgFqn[0]),
                                Collections.<Fix> emptyList(),
                                info.getFileObject(), positions.get(0),
                                positions.get(1));
                descriptions.add(description);
            }
        }
        catch (IOException e) {
            Logger.getLogger(ConnectorAnalyzer.class.getName()).log(Level.INFO,
                    null, e);
        }
    }

}
