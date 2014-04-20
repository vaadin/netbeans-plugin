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

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.utils.JavaUtils;

/**
 * @author denis
 */
public class CreateClientAcceptCriterion extends AbstractCreateFix {

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
        logUiUsage();
        searchClientPackage(false);
        String clientClassName = requestClientClassName(getClientPackage());

        if (clientClassName == null) {
            return null;
        }

        if (getClientPackage() == null) {
            createClientPackage();
        }

        Map<String, String> map = new HashMap<>();
        map.put(SERVER_CLASS, getFileObject().getName());
        map.put(SERVER_CLASS_FQN, myHandle.getQualifiedName());

        DataObject dataObject =
                JavaUtils.createDataObjectFromTemplate(TEMPLATE,
                        getClientPackage(), clientClassName, map);
        if (dataObject != null) {
            EditorCookie cookie =
                    dataObject.getLookup().lookup(EditorCookie.class);
            if (cookie != null) {
                cookie.open();
            }
        }
        return null;
    }

    @Override
    protected String getUiLogKey() {
        return "UI_LogClientAcceptCriterion"; // NOI18N
    }

    @NbBundle.Messages("acceptCriterionDialogTitle=Set Up Client AcceptCriterion")
    private String requestClientClassName( FileObject targetPkg ) {
        return requestClassName(suggestClientClassName(targetPkg, SUFFIX));
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

    private ElementHandle<TypeElement> myHandle;

}
