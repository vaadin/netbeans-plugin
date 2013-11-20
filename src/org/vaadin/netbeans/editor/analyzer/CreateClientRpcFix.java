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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

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
import org.vaadin.netbeans.code.generator.WidgetGenerator;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;

/**
 * @author denis
 */
public class CreateClientRpcFix extends AbstractRpcFix {

    protected CreateClientRpcFix( FileObject fileObject,
            ElementHandle<TypeElement> handle, String varName, String varType )
    {
        super(fileObject, handle, varName, varType);
    }

    protected CreateClientRpcFix( FileObject fileObject,
            ElementHandle<TypeElement> handle, String varType )
    {
        super(fileObject, handle, null, varType);
    }

    @NbBundle.Messages({
            "createClientRpc=Create Client RPC interface and use it",
            "# {0} - server rpc",
            "registerExistingClientRpc=Register Client RPC interface {0}",
            "registerDeclaredClientRpc=Register Client RPC interface declared by ''{0}''" })
    @Override
    public String getText() {
        if (getRpcVariable() == null) {
            if (getRpcVariableType() == null) {
                return Bundle.createClientRpc();
            }
            else {
                return Bundle.registerExistingClientRpc(getRpcVariableType());
            }
        }
        else {
            return Bundle.registerDeclaredClientRpc(getRpcVariable());
        }
    }

    @Override
    protected String suggestInterfaceName( FileObject pkg ) {
        String name = getFileObject().getName();
        if (name.endsWith(WidgetGenerator.CONNECTOR)) {
            name =
                    name.substring(0,
                            name.length() - WidgetGenerator.CONNECTOR.length());
        }
        return suggestInterfaceName(pkg, name, CLIENT_RPC);
    }

    @Override
    protected String getRpcInterfaceTemplate() {
        return CLIENT_RPC_TEMPLATE;
    }

    @NbBundle.Messages("clientRpcTitle=Set a name for Client RPC intreface")
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

                        if (getRpcVariable() == null) {
                            addImport(ifaceFqn, copy, treeMaker);
                        }

                        callRegisterRpc(clazz, copy, treeMaker, ifaceName,
                                ifaceFqn);
                    }

                });
        ChangeInfo info = createChangeInfo(task);
        task.commit();
        return info;
    }

    private void callRegisterRpc( TypeElement clazz, WorkingCopy copy,
            TreeMaker treeMaker, String ifaceName, String ifaceFqn )
    {
        List<ExecutableElement> ctors =
                ElementFilter.constructorsIn(clazz.getEnclosedElements());
        for (ExecutableElement ctor : ctors) {
            CtorScanner visitor = new CtorScanner();
            visitor.scan(copy.getTrees().getPath(ctor), null);
            if (visitor.hasThis()) {
                continue;
            }
            MethodTree tree = copy.getTrees().getTree(ctor);
            BlockTree body = tree.getBody();
            int index = 0;
            if (visitor.hasSuper()) {
                index = 1;
            }
            MethodInvocationTree methodInvocation = null;
            if (getRpcVariable() == null) {
                List<ExpressionTree> args = new ArrayList<>(2);
                args.add(treeMaker.MemberSelect(
                        treeMaker.Identifier(ifaceName), "class")); // NOI18N
                ClassTree newClassBody =
                        treeMaker.Class(treeMaker.Modifiers(Collections
                                .<Modifier> emptySet()), "", Collections
                                .<TypeParameterTree> emptyList(), null,
                                Collections.<Tree> emptyList(), Collections
                                        .<ExpressionTree> emptyList());
                newClassBody =
                        implement(ifaceFqn, treeMaker, copy, newClassBody);
                args.add(treeMaker.NewClass(null,
                        Collections.<ExpressionTree> emptyList(),
                        treeMaker.Identifier(ifaceName),
                        Collections.<ExpressionTree> emptyList(), newClassBody));
                methodInvocation =
                        treeMaker
                                .MethodInvocation(
                                        Collections
                                                .<ExpressionTree> emptyList(),
                                        treeMaker
                                                .Identifier(RpcRegistrationAnalyzer.REGISTER_RPC),
                                        args);
            }
            else {
                methodInvocation =
                        treeMaker
                                .MethodInvocation(
                                        Collections
                                                .<ExpressionTree> emptyList(),
                                        treeMaker
                                                .Identifier(RpcRegistrationAnalyzer.REGISTER_RPC),
                                        Collections.singletonList(treeMaker
                                                .Identifier(getRpcVariable())));
            }
            ExpressionStatementTree expression =
                    treeMaker.ExpressionStatement(methodInvocation);
            BlockTree newBody =
                    treeMaker.insertBlockStatement(body, index, expression);
            copy.rewrite(body, newBody);
        }
    }
}
