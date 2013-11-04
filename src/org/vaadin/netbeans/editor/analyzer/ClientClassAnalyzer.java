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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
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
import org.vaadin.netbeans.editor.VaadinTaskFactory;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;
import org.vaadin.netbeans.utils.XmlUtils;

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
            "notClientPackage=Class''s package is incorrect, it must be inside client package ({0})" })
    protected void checkClientPackage( TypeElement type, CompilationInfo info,
            VaadinSupport support, Collection<ErrorDescription> descriptions )
    {
        final List<FileObject> clientPackage = new LinkedList<>();
        final boolean[] hasWidgetset = new boolean[1];
        final List<String> clientPkgFqn = new LinkedList<>();
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
                        String fqn = AbstractJavaFix.getWidgetsetFqn(gwtXml);
                        for (String path : model.getSourcePaths()) {
                            clientPackage.add(XmlUtils.getClientWidgetPackage(
                                    model.getGwtXml(), path, false));
                            clientPkgFqn.add(fqn.substring(0, fqn.length()
                                    - gwtXml.getNameExt().length()
                                    + XmlUtils.GWT_XML.length())
                                    + path);
                        }
                    }
                    catch (IOException ignore) {
                    }
                }
            });
            if (!hasWidgetset[0]) {
                // TODO : no widgetset file error
            }
            else {
                boolean isInsideClientPkg = false;
                for (FileObject clientPkg : clientPackage) {
                    if (clientPkg == null) {
                        // TODO : for fix hint. Package has to be created
                    }
                    else if (FileUtil.isParentOf(clientPkg,
                            info.getFileObject()))
                    {
                        isInsideClientPkg = true;
                        break;
                    }
                }

                if (!isInsideClientPkg) {
                    // TODO : provide a hint to move class into the client package (using refactoring)
                    List<Integer> positions = AbstractJavaFix
                            .getElementPosition(info, type);
                    ErrorDescription description = ErrorDescriptionFactory
                            .createErrorDescription(
                                    Severity.ERROR,
                                    Bundle.notClientPackage(getPackages(clientPkgFqn)),
                                    Collections.<Fix> emptyList(), info
                                            .getFileObject(), positions.get(0),
                                    positions.get(1));
                    descriptions.add(description);
                }
            }
        }
        catch (IOException e) {
            Logger.getLogger(ConnectorAnalyzer.class.getName()).log(Level.INFO,
                    null, e);
        }
    }

    private String getPackages( List<String> fqns ) {
        StringBuilder result = new StringBuilder();
        for (String fqn : fqns) {
            result.append(fqn);
            result.append(", ");
        }
        if (result.length() > 0) {
            return result.substring(0, result.length() - 2);
        }
        return result.toString();
    }
}
