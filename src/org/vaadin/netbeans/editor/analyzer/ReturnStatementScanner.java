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

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

import org.netbeans.api.java.source.CompilationInfo;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePathScanner;

/**
 * @author denis
 */
class ReturnStatementScanner extends TreePathScanner<Tree, CompilationInfo> {

    ReturnStatementScanner( VariableElement field ) {
        myField = field;
    }

    @Override
    public Tree visitReturn( ReturnTree returnTree, CompilationInfo info ) {
        boolean previous = isInsideReturn;
        isInsideReturn = true;
        Tree tree = super.visitReturn(returnTree, info);
        isInsideReturn = previous;
        return tree;
    }

    @Override
    public Tree visitIdentifier( IdentifierTree identifierTree,
            CompilationInfo info )
    {
        Element element =
                info.getTrees().getElement(
                        info.getTrees().getPath(info.getCompilationUnit(),
                                identifierTree));
        boolean fieldFound = myField.equals(element);
        if (isInsideReturn && fieldFound) {
            isFound = true;
        }
        else if (element != null) {
            if (fieldFound) {
                if (isFieldUsed != null) {
                    isFieldUsed = true;
                }
            }
            else {
                isFieldUsed = null;
            }
        }
        return super.visitIdentifier(identifierTree, info);
    }

    boolean isFound() {
        if (isFound) {
            return true;
        }
        else {
            return Boolean.TRUE.equals(isFieldUsed);
        }
    }

    private VariableElement myField;

    private boolean isFound;

    private boolean isInsideReturn;

    private Boolean isFieldUsed = false;

}
