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
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.utils.JavaUtils;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;

/**
 * @author denis
 */
public class SetWidgetsetFix extends AbstractJavaFix {

    public SetWidgetsetFix( String widgetset, FileObject fileObject,
            ElementHandle<TypeElement> handle )
    {
        super(fileObject);
        myWidgetsetFqn = widgetset;
        myHandle = handle;
    }

    @Override
    @NbBundle.Messages({ "# {0} - widgetset",
            "setWidgetsetName=Set widgetset name to {0}" })
    public String getText() {
        return Bundle.setWidgetsetName(myWidgetsetFqn);
    }

    @Override
    public ChangeInfo implement() throws IOException {
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

                        TypeElement clazz = myHandle.resolve(copy);
                        if (clazz == null) {
                            return;
                        }

                        AnnotationMirror config =
                                JavaUtils.getAnnotation(clazz,
                                        JavaUtils.VAADIN_SERVLET_CONFIGURATION);
                        if (config == null) {
                            return;
                        }

                        AnnotationTree oldTree =
                                (AnnotationTree) copy.getTrees().getTree(clazz,
                                        config);

                        TreeMaker treeMaker = copy.getTreeMaker();
                        ExpressionTree widgetsetTree =
                                AbstractJavaFix.getAnnotationTreeAttribute(
                                        oldTree, JavaUtils.WIDGETSET);

                        AnnotationTree newTree =
                                treeMaker.removeAnnotationAttrValue(oldTree,
                                        widgetsetTree);

                        ExpressionTree newWidgetsetTree =
                                treeMaker.Assignment(treeMaker
                                        .Identifier(JavaUtils.WIDGETSET),
                                        treeMaker.Literal(myWidgetsetFqn));

                        newTree =
                                treeMaker.addAnnotationAttrValue(newTree,
                                        newWidgetsetTree);
                        copy.rewrite(oldTree, newTree);
                    }
                });
        ChangeInfo changeInfo = createChangeInfo(task);
        task.commit();
        return changeInfo;
    }

    private String myWidgetsetFqn;

    private final ElementHandle<TypeElement> myHandle;

}
