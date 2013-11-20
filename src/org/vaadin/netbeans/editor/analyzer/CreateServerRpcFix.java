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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
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

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;

/**
 * @author denis
 */
public class CreateServerRpcFix extends AbstractRpcFix {

    private static final String RPC_FIELD = "rpc"; // NOI18N 

    protected CreateServerRpcFix( FileObject fileObject,
            ElementHandle<TypeElement> handle, String varName, String varType )
    {
        super(fileObject, handle, varName, varType);
    }

    public CreateServerRpcFix( FileObject fileObject,
            ElementHandle<TypeElement> handle, String varType )
    {
        super(fileObject, handle, null, varType);
    }

    @NbBundle.Messages({
            "createServerRpc=Create Server RPC interface and use it",
            "# {0} - server rpc",
            "registerDeclaredServerRpc=Register Server RPC interface declared by ''{0}''",
            "registerExistingServerRpc=Register Server RPC interface {0}" })
    @Override
    public String getText() {
        if (getRpcVariable() == null) {
            if (getRpcVariableType() == null) {
                return Bundle.createServerRpc();
            }
            else {
                return Bundle.registerExistingServerRpc(getRpcVariableType());
            }
        }
        else {
            return Bundle.registerDeclaredServerRpc(getRpcVariable());
        }
    }

    @NbBundle.Messages("serverRpcTitle=Set a name for Server RPC interface")
    @Override
    protected String getInterfaceCreationDialogTitle() {
        return Bundle.serverRpcTitle();
    }

    @Override
    protected String suggestInterfaceName( FileObject pkg ) {
        return suggestInterfaceName(pkg, getFileObject().getName(), SERVER_RPC);
    }

    @Override
    protected String getRpcInterfaceTemplate() {
        return SERVER_RPC_TEMPLATE;
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

                        String rpcField = getRpcVariable();
                        if (rpcField == null) {
                            addImport(ifaceFqn, copy, treeMaker);

                            rpcField = getRpcField(clazz);
                        }

                        callRegisterRpc(rpcField, clazz, copy, treeMaker);

                        if (getRpcVariable() == null) {
                            VariableTree varTree =
                                    createField(rpcField, ifaceName, ifaceFqn,
                                            treeMaker, copy);

                            ClassTree newTree =
                                    treeMaker.addClassMember(oldTree, varTree);
                            copy.rewrite(oldTree, newTree);

                        }
                    }

                });
        ChangeInfo info = createChangeInfo(task);
        task.commit();
        return info;
    }

    private void callRegisterRpc( String rpcField, TypeElement clazz,
            WorkingCopy copy, TreeMaker treeMaker )
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
            MethodInvocationTree methodInvocation =
                    treeMaker.MethodInvocation(Collections
                            .<ExpressionTree> emptyList(), treeMaker
                            .Identifier(RpcRegistrationAnalyzer.REGISTER_RPC),
                            Collections.singletonList(treeMaker
                                    .Identifier(rpcField)));
            ExpressionStatementTree expression =
                    treeMaker.ExpressionStatement(methodInvocation);
            BlockTree newBody =
                    treeMaker.insertBlockStatement(body, index, expression);
            copy.rewrite(body, newBody);
        }
    }

    private VariableTree createField( String fieldName, String ifaceName,
            String ifaceFqn, TreeMaker treeMaker, WorkingCopy copy )
    {
        ClassTree body =
                treeMaker.Class(
                        treeMaker.Modifiers(Collections.<Modifier> emptySet()),
                        "", Collections.<TypeParameterTree> emptyList(), null,
                        Collections.<Tree> emptyList(),
                        Collections.<ExpressionTree> emptyList());

        body = implement(ifaceFqn, treeMaker, copy, body);

        NewClassTree initializer =
                treeMaker.NewClass(null,
                        Collections.<ExpressionTree> emptyList(),
                        treeMaker.Identifier(ifaceName),
                        Collections.<ExpressionTree> emptyList(), body);

        return treeMaker.Variable(
                treeMaker.Modifiers(EnumSet.of(Modifier.PRIVATE)), fieldName,
                treeMaker.Identifier(ifaceName), initializer);

    }

    private String getRpcField( TypeElement type ) {
        List<VariableElement> fields =
                ElementFilter.fieldsIn(type.getEnclosedElements());
        Set<String> names = new HashSet<>();
        for (VariableElement field : fields) {
            names.add(field.getSimpleName().toString());
        }
        String name = RPC_FIELD;
        int i = 1;
        while (names.contains(name)) {
            name = new StringBuilder(RPC_FIELD).append(i).toString();
            i++;
        }
        return name;
    }

}
