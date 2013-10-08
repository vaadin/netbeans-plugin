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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.code.generator.JavaUtils;
import org.vaadin.netbeans.editor.analyzer.ui.RpcInterfacePanel;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
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

    private static final String SERVER_RPC = "ServerRpc"; // NOI18N

    private static final String SERVER_RPC_TEMPLATE = "Templates/Vaadin/"
            + SERVER_RPC + "Stub.java"; // NOI18N

    private static final String RPC_FIELD = "rpc"; // NOI18N 

    protected CreateServerRpcFix( FileObject fileObject,
            ElementHandle<TypeElement> handle )
    {
        super(fileObject, handle);
    }

    @NbBundle.Messages("createServerRpc=Create Server RPC interface and use it")
    @Override
    public String getText() {
        return Bundle.createServerRpc();
    }

    @Override
    public ChangeInfo implement() throws Exception {
        FileObject pkg = getClientPackage();
        if (pkg == null) {
            return null;
        }
        String interfaceName = requestInterfaceName(pkg);
        if (interfaceName == null) {
            return null;
        }
        if (!createFileObject(pkg, interfaceName)) {
            return null;
        }
        ClassPath classPath = ClassPath.getClassPath(pkg, ClassPath.SOURCE);
        String ifaceFqn = classPath.getResourceName(pkg, '.', false) + '.'
                + interfaceName;
        JavaSource javaSource = JavaSource.forFileObject(getFileObject());
        if (javaSource == null) {
            getLogger().log(Level.WARNING, "JavaSource is null for file {0}",
                    getFileObject().getPath());
            return null;
        }

        return generateInterfaceUsage(javaSource, ifaceFqn, interfaceName);
    }

    private ChangeInfo generateInterfaceUsage( JavaSource javaSource,
            final String ifaceFqn, final String ifaceName ) throws IOException
    {
        ModificationResult task = javaSource
                .runModificationTask(new Task<WorkingCopy>() {

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

                        String rpcField = getRpcField(clazz);

                        callRegisterRpc(rpcField, clazz, copy, treeMaker);

                        ClassTree newTree = addField(rpcField, ifaceName,
                                oldTree, treeMaker);

                        copy.rewrite(oldTree, newTree);
                    }

                });
        ChangeInfo info = createChangeInfo(task);
        task.commit();
        return info;
    }

    private void callRegisterRpc( String rpcField, TypeElement clazz,
            WorkingCopy copy, TreeMaker treeMaker )
    {
        List<ExecutableElement> ctors = ElementFilter.constructorsIn(clazz
                .getEnclosedElements());
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
            MethodInvocationTree methodInvocation = treeMaker.MethodInvocation(
                    Collections.<ExpressionTree> emptyList(),
                    treeMaker.Identifier(RpcRegistrationAnalyzer.REGISTER_RPC),
                    Collections.singletonList(treeMaker.Identifier(rpcField)));
            ExpressionStatementTree expression = treeMaker
                    .ExpressionStatement(methodInvocation);
            BlockTree newBody = treeMaker.insertBlockStatement(body, index,
                    expression);
            copy.rewrite(body, newBody);
        }
    }

    private void addImport( String ifaceFqn, WorkingCopy copy,
            TreeMaker treeMaker )
    {
        ImportTree imprt = treeMaker.Import(treeMaker.QualIdent(ifaceFqn),
                false);

        CompilationUnitTree unitTree = copy.getCompilationUnit();
        CompilationUnitTree withImport = treeMaker.addCompUnitImport(
                copy.getCompilationUnit(), imprt);

        copy.rewrite(unitTree, withImport);
    }

    private ClassTree addField( String fieldName, String ifaceName,
            ClassTree tree, TreeMaker treeMaker )
    {
        ClassTree body = treeMaker.Class(
                treeMaker.Modifiers(Collections.<Modifier> emptySet()), "",
                Collections.<TypeParameterTree> emptyList(), null,
                Collections.<Tree> emptyList(),
                Collections.<ExpressionTree> emptyList());

        NewClassTree initializer = treeMaker.NewClass(null,
                Collections.<ExpressionTree> emptyList(),
                treeMaker.Identifier(ifaceName),
                Collections.<ExpressionTree> emptyList(), body);

        VariableTree newField = treeMaker.Variable(
                treeMaker.Modifiers(EnumSet.of(Modifier.PRIVATE)), fieldName,
                treeMaker.Identifier(ifaceName), initializer);

        ClassTree newTree = treeMaker.addClassMember(tree, newField);

        return newTree;
    }

    private String getRpcField( TypeElement type ) {
        List<VariableElement> fields = ElementFilter.fieldsIn(type
                .getEnclosedElements());
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

    @NbBundle.Messages({
            "# {0} - className",
            "interfaceIsNotCreated=Unable to create an interface with name {0}.",
            "interfaceAlreadyExists=Interface {0} already exists" })
    private boolean createFileObject( FileObject pkg, String interfaceName ) {
        if (pkg.getFileObject(interfaceName, JavaUtils.JAVA_SUFFIX) != null) {
            NotifyDescriptor descriptor = new NotifyDescriptor.Message(
                    Bundle.interfaceAlreadyExists(interfaceName),
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(descriptor);
            return false;
        }
        try {
            JavaUtils.createDataObjectFromTemplate(SERVER_RPC_TEMPLATE, pkg,
                    interfaceName, null);
        }
        catch (IOException e) {
            Logger.getLogger(CreateServerRpcFix.class.getName()).log(
                    Level.INFO, null, e);
            NotifyDescriptor descriptor = new NotifyDescriptor.Message(
                    Bundle.interfaceIsNotCreated(interfaceName),
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(descriptor);
            return false;
        }
        return true;
    }

    @NbBundle.Messages("serverRpcTitle=Set a name for Server RPC intreface")
    private String requestInterfaceName( FileObject targetPkg ) {
        final String name = suggestInterfaceName(targetPkg);
        String interfaceName = Mutex.EVENT
                .readAccess(new Mutex.Action<String>() {

                    @Override
                    public String run() {
                        RpcInterfacePanel panel = new RpcInterfacePanel(name);
                        DialogDescriptor descriptor = new DialogDescriptor(
                                panel, Bundle.serverRpcTitle());
                        Object result = DialogDisplayer.getDefault().notify(
                                descriptor);
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

    private String suggestInterfaceName( FileObject pkg ) {
        StringBuilder name = new StringBuilder(getFileObject().getName());
        name.append(SERVER_RPC);
        int i = 1;
        String suggestedName = name.toString();
        while (pkg.getFileObject(suggestedName, JavaUtils.JAVA_SUFFIX) != null)
        {
            StringBuilder current = new StringBuilder(name);
            suggestedName = current.append(i).toString();
            i++;
        }
        return suggestedName;
    }

}
