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
import java.util.EnumSet;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;

/**
 * @author denis
 */
class ClientRpcProxyFix extends AbstractRpcFix {

    ClientRpcProxyFix( FileObject fileObject, ElementHandle<TypeElement> handle )
    {
        super(fileObject, handle, null, null);
    }

    ClientRpcProxyFix( FileObject fileObject, String rpcIfaceFqn,
            ElementHandle<TypeElement> handle )
    {
        super(fileObject, handle, null, rpcIfaceFqn);
    }

    @NbBundle.Messages({
            "generateClientRpcProxy=Generate Client RPC interface and getter for it",
            "# {0} - client rpc",
            "generateGetterClientRpcProxy=Generate getter for Client RPC interface {0}" })
    @Override
    public String getText() {
        if (getRpcVariableType() == null) {
            return Bundle.generateClientRpcProxy();
        }
        else {
            return Bundle.generateGetterClientRpcProxy(getRpcVariableType());
        }
    }

    @Override
    protected String getUiLogKey() {
        return "UI_LogGenClientRpcProxyInterface"; // NOI18N
    }

    @Override
    protected String suggestInterfaceName( FileObject pkg ) {
        return suggestInterfaceName(pkg, getFileObject().getName(), CLIENT_RPC);
    }

    @Override
    protected String getRpcInterfaceTemplate() {
        return CLIENT_RPC_TEMPLATE;
    }

    @Override
    protected String getInterfaceCreationDialogTitle() {
        return Bundle.clientRpcTitle();
    }

    @Override
    protected ChangeInfo generateInterfaceUsage( JavaSource javaSource,
            final String ifaceFqn, final String ifaceName ) throws IOException
    {
        ModificationResult task =
                javaSource.runModificationTask(new Task<WorkingCopy>() {

                    @Override
                    public void run( WorkingCopy copy ) throws Exception {
                        copy.toPhase(Phase.ELEMENTS_RESOLVED);

                        TypeElement clazz = getTypeHandle().resolve(copy);
                        if (clazz == null) {
                            return;
                        }
                        ClassTree oldTree = copy.getTrees().getTree(clazz);
                        if (oldTree == null) {
                            return;
                        }

                        TreeMaker treeMaker = copy.getTreeMaker();

                        addImport(ifaceFqn, copy, treeMaker);

                        createRpcProxyGetter(clazz, oldTree, copy, treeMaker,
                                ifaceName);
                    }

                });
        ChangeInfo info = createChangeInfo(task);
        task.commit();
        return info;
    }

    private void createRpcProxyGetter( TypeElement clazz, ClassTree tree,
            WorkingCopy copy, TreeMaker treeMaker, String ifaceName )
    {
        String getter = findFreeGetterName(clazz, CLIENT_RPC);
        StringBuilder body = new StringBuilder("{return getRpcProxy("); //NOI18N
        body.append(ifaceName);
        body.append(".class);}"); //NOI18N
        MethodTree method =
                treeMaker.Method(
                        treeMaker.Modifiers(EnumSet.of(Modifier.PRIVATE)),
                        getter, treeMaker.Type(ifaceName),
                        Collections.<TypeParameterTree> emptyList(),
                        Collections.<VariableTree> emptyList(),
                        Collections.<ExpressionTree> emptyList(),
                        body.toString(), null);
        ClassTree newTree = treeMaker.addClassMember(tree, method);
        copy.rewrite(tree, newTree);
    }

}
