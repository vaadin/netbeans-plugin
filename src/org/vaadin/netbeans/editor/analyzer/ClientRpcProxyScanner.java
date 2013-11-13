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
import javax.lang.model.element.TypeElement;

/**
 * @author denis
 */
class ClientRpcProxyScanner extends AbstractRpcProxyScanner {

    private static final String GET_RPC_PROXY = "getRpcProxy"; // NOI18N

    @Override
    protected boolean isGetRpcProxy( ExecutableElement method ) {
        if (method.getSimpleName().contentEquals(GET_RPC_PROXY)) {
            Element enclosingElement = method.getEnclosingElement();
            if (enclosingElement instanceof TypeElement) {
                TypeElement type = (TypeElement) enclosingElement;
                return type.getQualifiedName().contentEquals(
                        RpcRegistrationAnalyzer.CLIENT_CONNECTOR);
            }
        }
        return false;
    }

}
