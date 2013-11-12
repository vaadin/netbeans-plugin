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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.netbeans.spi.java.hints.HintContext;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.editor.hints.Analyzer;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;
import org.vaadin.netbeans.utils.XmlUtils;

/**
 * @author denis
 */
public abstract class ClientClassAnalyzer extends Analyzer {

    ClientClassAnalyzer( HintContext context, boolean packageCheckMode ) {
        super(context);
        isPackageCheckMode = packageCheckMode;
    }

    public ErrorDescription getNotClientPackage() {
        return myNotClientPackage;
    }

    protected boolean isPackageCheckMode() {
        return isPackageCheckMode;
    }

    @NbBundle.Messages({
            "# {0} - clientPackage",
            "notClientPackage=Class''s package is incorrect, it must be inside client package ({0})" })
    protected void checkClientPackage() {
        if (!isPackageCheckMode()) {
            return;
        }
        final List<FileObject> clientPackage = new LinkedList<>();
        final boolean[] hasWidgetset = new boolean[1];
        final List<String> clientPkgFqn = new LinkedList<>();

        VaadinSupport support = getSupport();
        if (support == null || !support.isEnabled() || !support.isReady()) {
            return;
        }
        CompilationInfo info = getInfo();
        TypeElement type = getType();
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
                                    gwtXml, path, false));
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
                    List<Integer> positions =
                            AbstractJavaFix.getElementPosition(info, type);
                    myNotClientPackage =
                            ErrorDescriptionFactory
                                    .createErrorDescription(
                                            getSeverity(Severity.ERROR),
                                            Bundle.notClientPackage(getPackages(clientPkgFqn)),
                                            Collections.<Fix> emptyList(), info
                                                    .getFileObject(), positions
                                                    .get(0), positions.get(1));
                    getDescriptions().add(myNotClientPackage);
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

    private ErrorDescription myNotClientPackage;

    private boolean isPackageCheckMode;
}
