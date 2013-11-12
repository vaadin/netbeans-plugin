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

import java.util.logging.Level;

import javax.lang.model.element.ExecutableElement;

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

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;

/**
 * @author denis
 */
class SetVoidMethodFix extends AbstractJavaFix {

    SetVoidMethodFix( FileObject fileObject,
            ElementHandle<ExecutableElement> handle )
    {
        super(fileObject);
        myHandle = handle;
    }

    @NbBundle.Messages("removeReturnType=Remove method's return type")
    @Override
    public String getText() {
        return Bundle.removeReturnType();
    }

    @Override
    public ChangeInfo implement() throws Exception {
        JavaSource javaSource = JavaSource.forFileObject(getFileObject());
        if (javaSource == null) {
            getLogger().log(Level.WARNING, "JavaSource is null for file {0}",
                    getFileObject().getPath());
            return null;
        }
        ModificationResult task =
                javaSource.runModificationTask(new Task<WorkingCopy>() {

                    @Override
                    public void run( WorkingCopy copy ) throws Exception {
                        copy.toPhase(Phase.ELEMENTS_RESOLVED);

                        ExecutableElement method = myHandle.resolve(copy);
                        if (method == null) {
                            return;
                        }

                        MethodTree oldTree = copy.getTrees().getTree(method);

                        TreeMaker treeMaker = copy.getTreeMaker();
                        MethodTree newTree =
                                treeMaker.Method(oldTree.getModifiers(),
                                        oldTree.getName(), treeMaker
                                                .Type("void"), oldTree
                                                .getTypeParameters(), oldTree
                                                .getParameters(), oldTree
                                                .getThrows(),
                                        oldTree.getBody(),
                                        (ExpressionTree) oldTree
                                                .getDefaultValue());

                        copy.rewrite(oldTree, newTree);
                    }
                });

        ChangeInfo info = createChangeInfo(task);
        task.commit();
        return info;
    }

    private ElementHandle<ExecutableElement> myHandle;

}
