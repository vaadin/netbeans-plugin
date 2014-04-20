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
import java.util.logging.Level;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.openide.filesystems.FileObject;
import org.vaadin.netbeans.utils.JavaUtils;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

/**
 * @author denis
 */
public class SetServletWidgetsetFix extends AbstractSetWebInitParamFix {

    public SetServletWidgetsetFix( String widgetset, FileObject fileObject,
            ElementHandle<TypeElement> handle )
    {
        super(fileObject, handle);
        myWidgetset = widgetset;
    }

    @Override
    public String getText() {
        return Bundle.setWidgetsetName(myWidgetset);
    }

    @Override
    public ChangeInfo implement() throws IOException {
        logUiUsage();
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

                        TypeElement clazz = getHandle().resolve(copy);
                        if (clazz == null) {
                            return;
                        }

                        AnnotationMirror servlet =
                                JavaUtils.getAnnotation(clazz,
                                        JavaUtils.SERVLET_ANNOTATION);
                        if (servlet == null) {
                            return;
                        }

                        TreeMaker treeMaker = copy.getTreeMaker();
                        Tree tree = copy.getTrees().getTree(clazz, servlet);
                        Tree oldTree = null;
                        Tree newTree = null;
                        if (tree instanceof AnnotationTree) {
                            AnnotationTree annotationTree =
                                    (AnnotationTree) tree;
                            ExpressionTree expressionTree =
                                    getAnnotationTreeAttribute(annotationTree,
                                            JavaUtils.INIT_PARAMS);
                            if (expressionTree instanceof AssignmentTree) {
                                Tree[] trees =
                                        getWebInitChangedTree(expressionTree,
                                                treeMaker, JavaUtils.WIDGETSET,
                                                myWidgetset);
                                oldTree = trees[0];
                                newTree = trees[1];
                            }
                        }
                        if (oldTree != null && newTree != null) {
                            copy.rewrite(oldTree, newTree);
                        }
                    }
                });
        ChangeInfo changeInfo = createChangeInfo(task);
        task.commit();
        return changeInfo;
    }

    @Override
    protected String getUiLogKey() {
        return "UI_LogSetWidgetsetWebServlet"; // NOI18N
    }

    private final String myWidgetset;

}
