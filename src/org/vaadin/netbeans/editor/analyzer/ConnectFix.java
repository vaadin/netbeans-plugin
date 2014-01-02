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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javax.lang.model.element.TypeElement;

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
import org.vaadin.netbeans.editor.analyzer.ui.ClientConnectPanel;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;

/**
 * @author denis
 */
public class ConnectFix extends AbstractJavaFix {

    ConnectFix( FileObject fileObject, ElementHandle<TypeElement> handle,
            List<String> components )
    {
        super(fileObject);
        myHandle = handle;
        myComponentFqns = new ArrayList<>(components);
        Collections.sort(myComponentFqns);
        myComponentFqns = Collections.unmodifiableList(myComponentFqns);
    }

    ConnectFix( FileObject fileObject, ElementHandle<TypeElement> handle,
            String componentFqn )
    {
        super(fileObject);
        myHandle = handle;
        myComponentFqn = componentFqn;
    }

    @NbBundle.Messages({ "connectToSelected=Connect to existing component",
            "# {0} - component", "connectTo=Connect to {0}" })
    @Override
    public String getText() {
        if (myComponentFqn == null) {
            return Bundle.connectToSelected();
        }
        return Bundle.connectTo(myComponentFqn);
    }

    @Override
    public ChangeInfo implement() throws Exception {
        String selectedComponent = myComponentFqn;
        if (selectedComponent == null) {
            selectedComponent = selectComponent();
        }
        if (selectedComponent == null) {
            return null;
        }
        JavaSource javaSource = JavaSource.forFileObject(getFileObject());
        if (javaSource == null) {
            getLogger().log(Level.WARNING, "JavaSource is null for file {0}", // NOI18N
                    getFileObject().getPath());
            return null;
        }
        final String componentFqn = selectedComponent;
        ModificationResult task =
                javaSource.runModificationTask(new Task<WorkingCopy>() {

                    @Override
                    public void run( WorkingCopy copy ) throws Exception {
                        copy.toPhase(Phase.ELEMENTS_RESOLVED);

                        TypeElement clazz = myHandle.resolve(copy);
                        if (clazz == null) {
                            return;
                        }

                        ClassTree oldTree = copy.getTrees().getTree(clazz);
                        if (oldTree == null) {
                            return;
                        }

                        int index = componentFqn.lastIndexOf('.');
                        String simpleName = componentFqn.substring(index + 1);

                        TreeMaker treeMaker = copy.getTreeMaker();

                        addImport(componentFqn, copy, treeMaker);

                        MemberSelectTree componentClassSelect =
                                treeMaker.MemberSelect(
                                        treeMaker.Identifier(simpleName),
                                        "class"); // NOI18N
                        AnnotationTree connectorAnnotation =
                                treeMaker.Annotation(
                                        treeMaker
                                                .Type(ConnectorAnalyzer.CONNECTOR),
                                        Collections
                                                .singletonList(componentClassSelect));
                        ClassTree newTree =
                                treeMaker.Class(treeMaker
                                        .addModifiersAnnotation(
                                                oldTree.getModifiers(),
                                                connectorAnnotation), oldTree
                                        .getSimpleName(), oldTree
                                        .getTypeParameters(), oldTree
                                        .getExtendsClause(), oldTree
                                        .getImplementsClause(), oldTree
                                        .getMembers());
                        copy.rewrite(oldTree, newTree);
                    }
                });
        ChangeInfo changeInfo = createChangeInfo(task);
        task.commit();
        return changeInfo;
    }

    // TODO : Other commits has moved this method into AbstractJavaFix, so no need to define it here
    protected void addImport( String ifaceFqn, WorkingCopy copy,
            TreeMaker treeMaker )
    {
        ImportTree imprt =
                treeMaker.Import(treeMaker.QualIdent(ifaceFqn), false);

        CompilationUnitTree unitTree = copy.getCompilationUnit();
        CompilationUnitTree withImport =
                treeMaker.addCompUnitImport(copy.getCompilationUnit(), imprt);

        copy.rewrite(unitTree, withImport);
    }

    private String selectComponent() {
        return Mutex.EVENT.readAccess(new Mutex.Action<String>() {

            @Override
            public String run() {
                ClientConnectPanel panel =
                        new ClientConnectPanel(myComponentFqns);
                DialogDescriptor descriptor =
                        new DialogDescriptor(panel, Bundle.setThemeName());
                Object result = DialogDisplayer.getDefault().notify(descriptor);
                if (NotifyDescriptor.OK_OPTION.equals(result)) {
                    return panel.getComponent();
                }
                else {
                    return null;
                }
            }
        });
    }

    private List<String> myComponentFqns;

    private String myComponentFqn;

    private final ElementHandle<TypeElement> myHandle;

}
