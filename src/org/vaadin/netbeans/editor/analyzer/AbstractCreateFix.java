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
import java.util.logging.Level;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.util.Mutex;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.editor.analyzer.ui.NamePanel;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;
import org.vaadin.netbeans.utils.XmlUtils;

/**
 * @author denis
 */
abstract class AbstractCreateFix extends AbstractJavaFix {

    protected AbstractCreateFix( FileObject fileObject ) {
        super(fileObject);
    }

    protected void searchClientPackage( final boolean create )
            throws IOException
    {
        Project project = FileOwnerQuery.getOwner(getFileObject());
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        final String[] path = new String[1];
        support.runModelOperation(new ModelOperation() {

            @Override
            public void run( VaadinModel model ) {
                if (model.getGwtXml() == null) {
                    return;
                }
                myGwtXml = model.getGwtXml();
                for (String clientPath : model.getSourcePaths()) {
                    path[0] = clientPath;
                    try {
                        myClientPackage =
                                XmlUtils.getClientWidgetPackage(myGwtXml,
                                        clientPath, create);
                        if (myClientPackage != null) {
                            return;
                        }
                    }
                    catch (IOException e) {
                        getLogger().log(Level.FINE, null, e);
                    }
                }
            }
        });
    }

    protected String requestClassName( final String initialName ) {
        String interfaceName =
                Mutex.EVENT.readAccess(new Mutex.Action<String>() {

                    @Override
                    public String run() {
                        NamePanel panel = new NamePanel(initialName);
                        DialogDescriptor descriptor =
                                new DialogDescriptor(panel, Bundle
                                        .acceptCriterionDialogTitle());
                        Object result =
                                DialogDisplayer.getDefault().notify(descriptor);
                        if (NotifyDescriptor.OK_OPTION.equals(result)) {
                            return panel.getClassName();
                        }
                        else {
                            return null;
                        }
                    }

                });
        return interfaceName;
    }

    protected void createClientPackage() throws IOException {
        if (!hasGetXml()) {
            myGwtXml = XmlUtils.createGwtXml(getFileObject().getParent());
        }
        searchClientPackage(true);
    }

    protected FileObject getClientPackage() {
        return myClientPackage;
    }

    protected FileObject getGwtXml() {
        return myGwtXml;
    }

    protected boolean hasGetXml() {
        return getGwtXml() != null;
    }

    protected void setClientPackage( FileObject pkg ) {
        myClientPackage = pkg;
    }

    private FileObject myClientPackage;

    private FileObject myGwtXml;
}
