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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.vaadin.netbeans.code.generator.WidgetGenerator;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;

/**
 * @author denis
 */
class ServerRpcProxyFix extends AbstractRpcFix {

    ServerRpcProxyFix( FileObject fileObject, ElementHandle<TypeElement> handle )
    {
        super(fileObject, handle, null, null);
    }

    ServerRpcProxyFix( FileObject fileObject, String ifaceFqn,
            ElementHandle<TypeElement> handle )
    {
        super(fileObject, handle, null, ifaceFqn);
    }

    @NbBundle.Messages({
            "generateServerRpcProxy=Generate Server RPC interface and getter for it",
            "# {0} - server rpc",
            "generateGetterServerRpcProxy=Generate getter for Server RPC interface {0}" })
    @Override
    public String getText() {
        if (getRpcVariableType() == null) {
            return Bundle.generateServerRpcProxy();
        }
        else {
            return Bundle.generateGetterServerRpcProxy(getRpcVariableType());
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
        return suggestInterfaceName(pkg, name, SERVER_RPC);
    }

    @Override
    protected String getRpcInterfaceTemplate() {
        return SERVER_RPC_TEMPLATE;
    }

    @Override
    protected String getInterfaceCreationDialogTitle() {
        return Bundle.serverRpcTitle();
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
                        addImport(ServerRpcProxyScanner.RPC_PROXY_FQN, copy,
                                treeMaker);

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
        List<ExpressionTree> args = new ArrayList<>(2);
        args.add(treeMaker.MemberSelect(treeMaker.Identifier(ifaceName),
                "class")); //NOI18N
        args.add(treeMaker.Identifier("this")); //NOI18N
        MethodInvocationTree invocationTree =
                treeMaker.MethodInvocation(Collections
                        .<ExpressionTree> emptyList(), treeMaker.MemberSelect(
                        treeMaker.Identifier("RpcProxy"), //NOI18N
                        ServerRpcProxyScanner.CREATE), args);
        String field = findFreeFieldName(clazz, "rpc"); // NOI18N
        VariableTree varTree =
                treeMaker.Variable(
                        treeMaker.Modifiers(EnumSet.of(Modifier.PRIVATE)),
                        field, treeMaker.Type(ifaceName), invocationTree);
        ClassTree newTree = treeMaker.addClassMember(tree, varTree);

        String getter = findFreeGetterName(clazz, SERVER_RPC);
        StringBuilder body = new StringBuilder("{return "); //NOI18N
        body.append(field);
        body.append(";}"); //NOI18N
        MethodTree method =
                treeMaker.Method(
                        treeMaker.Modifiers(EnumSet.of(Modifier.PRIVATE)),
                        getter, treeMaker.Type(ifaceName),
                        Collections.<TypeParameterTree> emptyList(),
                        Collections.<VariableTree> emptyList(),
                        Collections.<ExpressionTree> emptyList(),
                        body.toString(), null);
        newTree = treeMaker.addClassMember(newTree, method);
        copy.rewrite(tree, newTree);
    }

    protected String findFreeFieldName( TypeElement clazz, String name ) {
        List<VariableElement> fields =
                ElementFilter.fieldsIn(clazz.getEnclosedElements());
        Set<String> names = new HashSet<>();
        for (VariableElement field : fields) {
            names.add(field.getSimpleName().toString());
        }
        String freeName = name;
        int i = 1;
        while (names.contains(freeName)) {
            freeName = name + i;
            i++;
        }
        return freeName;
    }
}
