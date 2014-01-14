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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.SourceUtils;
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
import org.vaadin.netbeans.utils.JavaUtils;
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

    public ErrorDescription getNoGwtModule() {
        return myNoGwtModule;
    }

    protected boolean isPackageCheckMode() {
        return isPackageCheckMode;
    }

    @NbBundle.Messages({
            "absentGwtModule=No GWT module file found",
            "# {0} - clientPackage",
            "notClientPackage=Class''s package is incorrect, it must be inside client package ({0})" })
    protected void checkClientPackage() {
        if (!isPackageCheckMode()) {
            return;
        }
        final List<FileObject> clientPackage = new LinkedList<>();
        final FileObject[] gwtXml = new FileObject[1];
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
                    gwtXml[0] = model.getGwtXml();
                    if (gwtXml[0] == null) {
                        return;
                    }
                    try {
                        String fqn = AbstractJavaFix.getWidgetsetFqn(gwtXml[0]);
                        for (String path : model.getSourcePaths()) {
                            clientPackage.add(XmlUtils.getClientWidgetPackage(
                                    gwtXml[0], path, false));
                            clientPkgFqn.add(fqn.substring(0, fqn.length()
                                    - gwtXml[0].getNameExt().length()
                                    + XmlUtils.GWT_XML.length())
                                    + path.replace('/', '.'));
                        }
                    }
                    catch (IOException ignore) {
                    }
                }
            });
            boolean hasWidgetset = gwtXml[0] != null;
            if (!hasWidgetset) {
                List<Integer> positions =
                        AbstractJavaFix.getElementPosition(info, type);
                myNoGwtModule =
                        ErrorDescriptionFactory.createErrorDescription(
                                getSeverity(Severity.ERROR),
                                Bundle.absentGwtModule(),
                                createGwtModuleFixes(), info.getFileObject(),
                                positions.get(0), positions.get(1));
                getDescriptions().add(myNoGwtModule);
            }
            else {
                boolean isInsideClientPkg = false;
                for (FileObject clientPkg : clientPackage) {
                    if (clientPkg != null
                            && FileUtil.isParentOf(clientPkg,
                                    info.getFileObject()))
                    {
                        isInsideClientPkg = true;
                        break;
                    }
                }

                if (!isInsideClientPkg) {
                    List<Integer> positions =
                            AbstractJavaFix.getElementPosition(info, type);
                    myNotClientPackage =
                            ErrorDescriptionFactory
                                    .createErrorDescription(
                                            getSeverity(Severity.ERROR),
                                            Bundle.notClientPackage(getPackages(clientPkgFqn)),
                                            createRelocationFixes(gwtXml[0],
                                                    clientPackage, clientPkgFqn),
                                            info.getFileObject(), positions
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

    private List<Fix> createGwtModuleFixes() {
        TypeElement ui =
                getInfo().getElements().getTypeElement(JavaUtils.VAADIN_UI_FQN);
        List<Fix> fixes = new LinkedList<>();
        if (ui != null) {
            try {
                Set<TypeElement> uis = JavaUtils.getSubclasses(ui, getInfo());
                Set<FileObject> sourceRoots =
                        IsInSourceQuery.getSourceRoots(getInfo());
                for (TypeElement vaadinUi : uis) {
                    if (IsInSourceQuery.isInSourceRoots(vaadinUi, getInfo(),
                            sourceRoots))
                    {
                        FileObject fileObject =
                                SourceUtils.getFile(
                                        ElementHandle.create(vaadinUi),
                                        getInfo().getClasspathInfo());
                        fixes.add(new CreateGwtModuleFix(getInfo()
                                .getFileObject(), fileObject.getParent()));
                    }
                }
            }
            catch (InterruptedException e) {
                Logger.getLogger(ClientClassAnalyzer.class.getName()).log(
                        Level.INFO, null, e);
            }
        }
        fixes.add(new CreateGwtModuleFix(getInfo().getFileObject(), null));
        return fixes;
    }

    private List<Fix> createRelocationFixes( FileObject gwtXml,
            List<FileObject> clientPackages, List<String> clientPkgFqns )
    {
        List<Fix> fixes = new LinkedList<>();
        FileObject baseFolder = gwtXml.getParent();
        FileObject currentPkg = getInfo().getFileObject().getParent();

        if (FileUtil.isParentOf(baseFolder, currentPkg)) {
            String relativePath =
                    FileUtil.getRelativePath(baseFolder, currentPkg);
            fixes.add(new AddSourcePathFix(relativePath, gwtXml));
        }
        int i = 0;
        for (FileObject clientPackage : clientPackages) {
            if (clientPackage != null) {
                fixes.add(new MoveClientClassFix(getInfo().getFileObject(),
                        clientPackage, clientPkgFqns.get(i), false));
            }
            else {
                String fqn = clientPkgFqns.get(i);
                fixes.add(new RenamePackageFix(currentPkg, fqn));
                fixes.add(new MoveClientClassFix(getInfo().getFileObject(),
                        null, fqn, false));
            }
            i++;
        }
        return fixes;
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

    private ErrorDescription myNoGwtModule;

    private boolean isPackageCheckMode;
}
