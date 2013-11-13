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

import org.netbeans.api.java.source.CompilationInfo;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;

/**
 * @author denis
 */
abstract class AbstractRpcProxyScanner extends
        TreeScanner<Tree, CompilationInfo>
{

    @Override
    public Tree visitMethodInvocation( MethodInvocationTree tree,
            CompilationInfo info )
    {
        TreePath path =
                info.getTrees().getPath(info.getCompilationUnit(), tree);
        if (path != null) {
            Element element = info.getTrees().getElement(path);
            if (element instanceof ExecutableElement) {
                ExecutableElement method = (ExecutableElement) element;
                if (isGetRpcProxy(method)) {
                    hasRpcProxyAccess = true;
                }
            }
        }
        return super.visitMethodInvocation(tree, info);
    }

    protected abstract boolean isGetRpcProxy( ExecutableElement method );

    boolean hasRpcProxyAccess() {
        return hasRpcProxyAccess;
    }

    private boolean hasRpcProxyAccess;
}
