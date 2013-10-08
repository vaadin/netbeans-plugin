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

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

/**
 * @author denis
 */
public class CreateClientRpcFix extends AbstractJavaFix {

    private static final String CLIENT_RPC_TEMPLATE = "Templates/Vaadin/ClientRpcStub.java"; // NOI18N

    protected CreateClientRpcFix( FileObject fileObject,
            ElementHandle<TypeElement> handle )
    {
        super(fileObject);
        myHandle = handle;
    }

    @NbBundle.Messages("createClientRpc=Create Client RPC interface and use it")
    @Override
    public String getText() {
        return Bundle.createClientRpc();
    }

    @Override
    public ChangeInfo implement() throws Exception {
        return null;
    }

    private ElementHandle<TypeElement> myHandle;

}
