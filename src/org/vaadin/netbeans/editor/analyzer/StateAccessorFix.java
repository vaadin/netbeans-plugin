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
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.code.WidgetUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;

/**
 * @author denis
 */
class StateAccessorFix extends AbstractCreateFix {

    StateAccessorFix( FileObject fileObject, String stateFqn,
            ElementHandle<TypeElement> handle,
            ElementHandle<TypeElement> pairHandle )
    {
        super(fileObject);
        myHandle = handle;
        myPairHandle = pairHandle;
        myStateFqn = stateFqn;
    }

    StateAccessorFix( FileObject fileObject, String stateFqn,
            ElementHandle<TypeElement> handle )
    {
        this(fileObject, stateFqn, handle, null);
        pairHasState = true;
    }

    protected StateAccessorFix( FileObject fileObject,
            ElementHandle<TypeElement> handle,
            ElementHandle<TypeElement> pairHandle )
    {
        this(fileObject, null, handle, pairHandle);
    }

    @Override
    @NbBundle.Messages({ "#{0} - state",
            "useStateOnlyHere=Use {0} state only for this class",
            "useStateHere=Use {0} state",
            "useState=Use {0} state in both server and client classes" })
    public String getText() {
        if (getPairHandle() == null) {
            if (pairHasState) {
                return Bundle.useStateHere(getStateFqn());
            }
            else {
                return Bundle.useStateOnlyHere(getStateFqn());
            }
        }
        else {
            return Bundle.useState(getStateFqn());
        }
    }

    @Override
    public ChangeInfo implement() throws Exception {
        return doImplement(getStateFqn());
    }

    protected ChangeInfo doImplement( String stateFqn ) throws IOException {
        ModificationResult result =
                addGetStateMethod(JavaSource.forFileObject(getFileObject()),
                        getHandle(), stateFqn);
        if (result == null) {
            return null;
        }
        if (getPairHandle() == null) {
            return createChangeInfo(result);
        }
        else {
            ModificationResult pairResult =
                    addGetStateMethod(
                            JavaSource.forFileObject(myPairFileObject),
                            getPairHandle(), stateFqn);
            return createChangeInfo(result, pairResult);
        }
    }

    protected ModificationResult addGetStateMethod( JavaSource javaSource,
            final ElementHandle<TypeElement> type, final String stateFqn )
            throws IOException
    {
        if (javaSource == null) {
            return null;
        }
        ModificationResult task =
                javaSource.runModificationTask(new Task<WorkingCopy>() {

                    @Override
                    public void run( WorkingCopy copy ) throws Exception {
                        copy.toPhase(Phase.ELEMENTS_RESOLVED);

                        TypeElement clazz = type.resolve(copy);
                        if (clazz == null) {
                            return;
                        }
                        if (getPairHandle() != null) {
                            myPairFileObject =
                                    SourceUtils.getFile(getPairHandle(),
                                            copy.getClasspathInfo());
                        }
                        ClassTree oldTree = copy.getTrees().getTree(clazz);
                        if (oldTree == null) {
                            return;
                        }

                        TreeMaker treeMaker = copy.getTreeMaker();

                        addImport(stateFqn, type, copy, treeMaker);

                        String stateSimpleName = stateFqn;
                        int index = stateSimpleName.lastIndexOf('.');
                        stateSimpleName = stateSimpleName.substring(index + 1);

                        StringBuilder builder = new StringBuilder("{ return (");
                        builder.append(stateSimpleName);
                        builder.append(") super.getState(); }");

                        ModifiersTree modifiers =
                                treeMaker.Modifiers(
                                        EnumSet.of(Modifier.PUBLIC),
                                        Collections.singletonList(treeMaker.Annotation(
                                                treeMaker.Type(Override.class
                                                        .getName()),
                                                Collections
                                                        .<ExpressionTree> emptyList())));
                        MethodTree method =
                                treeMaker.Method(
                                        modifiers,
                                        WidgetUtils.GET_STATE,
                                        treeMaker.Identifier(stateSimpleName),
                                        Collections
                                                .<TypeParameterTree> emptyList(),
                                        Collections.<VariableTree> emptyList(),
                                        Collections
                                                .<ExpressionTree> emptyList(),
                                        builder.toString(), null);
                        ClassTree newTree =
                                treeMaker.addClassMember(oldTree, method);
                        copy.rewrite(oldTree, newTree);
                    }

                });
        task.commit();
        return task;
    }

    protected ElementHandle<TypeElement> getHandle() {
        return myHandle;
    }

    protected ElementHandle<TypeElement> getPairHandle() {
        return myPairHandle;
    }

    private String getStateFqn() {
        return myStateFqn;
    }

    private final ElementHandle<TypeElement> myHandle;

    private final ElementHandle<TypeElement> myPairHandle;

    private String myStateFqn;

    private boolean pairHasState;

    private FileObject myPairFileObject;
}
