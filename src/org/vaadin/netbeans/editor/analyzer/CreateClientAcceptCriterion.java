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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.editor.analyzer.ui.NamePanel;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;
import org.vaadin.netbeans.utils.JavaUtils;
import org.vaadin.netbeans.utils.XmlUtils;

/**
 * @author denis
 */
public class CreateClientAcceptCriterion extends AbstractJavaFix implements Fix
{

    private static final String TEMPLATE =
            "Templates/Vaadin/AcceptCriterionClient.java"; // NOI18N

    private static final String SERVER_CLASS_FQN = "server_class_fqn"; // NOI18N

    private static final String SERVER_CLASS = "server_class"; // NOI18N

    private static final String SUFFIX = "Client"; // NOI18N

    public CreateClientAcceptCriterion( ElementHandle<TypeElement> handle,
            FileObject fileObject )
    {
        super(fileObject);
        myHandle = handle;
    }

    @NbBundle.Messages("createAcceptCriterionClient=Create client side AcceptCriterion class")
    @Override
    public String getText() {
        return Bundle.createAcceptCriterionClient();
    }

    @Override
    public ChangeInfo implement() throws Exception {
        searchClientPackage(false);
        String clientClassName = requestClientClassName(myClientPackage);

        if (clientClassName == null) {
            return null;
        }

        if (myClientPackage == null) {
            if (!hasGwtXml) {
                XmlUtils.createGwtXml(getFileObject().getParent());
            }
            searchClientPackage(true);
        }

        Map<String, String> map = new HashMap<>();
        map.put(SERVER_CLASS, getFileObject().getName());
        map.put(SERVER_CLASS_FQN, myHandle.getQualifiedName());

        DataObject dataObject =
                JavaUtils.createDataObjectFromTemplate(TEMPLATE,
                        myClientPackage, clientClassName, map);
        if (dataObject != null) {
            EditorCookie cookie =
                    dataObject.getLookup().lookup(EditorCookie.class);
            if (cookie != null) {
                cookie.open();
            }
        }
        return null;
    }

    private void searchClientPackage( final boolean create ) throws IOException
    {
        Project project = FileOwnerQuery.getOwner(getFileObject());
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        support.runModelOperation(new ModelOperation() {

            @Override
            public void run( VaadinModel model ) {
                if (model.getGwtXml() == null) {
                    return;
                }
                hasGwtXml = true;
                for (String clientPath : model.getSourcePaths()) {
                    try {
                        myClientPackage =
                                XmlUtils.getClientWidgetPackage(
                                        model.getGwtXml(), clientPath, create);
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

    @NbBundle.Messages("acceptCriterionDialogTitle=Set Up Client AcceptCriterion")
    private String requestClientClassName( FileObject targetPkg ) {
        final String name = suggestClientClassName(targetPkg, SUFFIX);
        String interfaceName =
                Mutex.EVENT.readAccess(new Mutex.Action<String>() {

                    @Override
                    public String run() {
                        NamePanel panel = new NamePanel(name);
                        DialogDescriptor descriptor =
                                new DialogDescriptor(panel, Bundle
                                        .acceptCriterionDialogTitle());
                        Object result =
                                DialogDisplayer.getDefault().notify(descriptor);
                        if (NotifyDescriptor.OK_OPTION.equals(result)) {
                            return panel.getIfaceName();
                        }
                        else {
                            return null;
                        }
                    }

                });
        return interfaceName;
    }

    private String suggestClientClassName( FileObject pkg, String suffix ) {
        StringBuilder name = new StringBuilder(getFileObject().getName());
        name.append(suffix);
        if (pkg == null) {
            return name.toString();
        }
        int i = 1;
        String suggestedName = name.toString();
        while (pkg.getFileObject(suggestedName, JavaUtils.JAVA) != null) {
            StringBuilder current = new StringBuilder(name);
            suggestedName = current.append(i).toString();
            i++;
        }
        return suggestedName;
    }

    private FileObject myClientPackage;

    private boolean hasGwtXml;

    private ElementHandle<TypeElement> myHandle;

}
