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

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * @author denis
 */
class ServerRpcProxyScanner extends AbstractRpcProxyScanner {

    static final String CREATE = "create"; // NOI18N

    static final String RPC_PROXY_FQN =
            "com.vaadin.client.communication.RpcProxy"; // NOI18N

    @Override
    protected boolean isGetRpcProxy( ExecutableElement method ) {
        if (method.getSimpleName().contentEquals(CREATE)) {
            Element enclosingElement = method.getEnclosingElement();
            if (!method.getModifiers().contains(Modifier.STATIC)) {
                return false;
            }
            if (enclosingElement instanceof TypeElement) {
                TypeElement type = (TypeElement) enclosingElement;
                return type.getQualifiedName().contentEquals(RPC_PROXY_FQN);
            }
        }
        return false;
    }

}
