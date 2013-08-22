package org.vaadin.netbeans.editor.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.TreeMaker;
import org.openide.filesystems.FileObject;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;

/**
 * @author denis
 */
abstract class AbstractSetWebInitParamFix extends AbstractJavaFix {

    AbstractSetWebInitParamFix( FileObject fileObject,
            ElementHandle<TypeElement> handle )
    {
        super(fileObject);
        myHandle = handle;
    }

    protected ElementHandle<TypeElement> getHandle() {
        return myHandle;
    }

    protected Tree[] getWebInitChangedTree( ExpressionTree expressionTree,
            TreeMaker treeMaker, String paramName, String paramValue )
    {
        Tree oldTree = null;
        Tree newTree = null;
        AssignmentTree assignmentTree = (AssignmentTree) expressionTree;
        ExpressionTree expression = assignmentTree.getExpression();
        if (expression instanceof AnnotationTree) {
            oldTree = expression;
            List<ExpressionTree> initializers = new ArrayList<>(2);
            initializers.add(expression);
            initializers.add(createWebInitParam(treeMaker, paramName,
                    paramValue));

            newTree = treeMaker.NewArray(null,
                    Collections.<ExpressionTree> emptyList(), initializers);
        }
        else if (expression instanceof NewArrayTree) {
            NewArrayTree arrayTree = (NewArrayTree) expression;
            oldTree = arrayTree;
            newTree = treeMaker.addNewArrayInitializer(arrayTree,
                    createWebInitParam(treeMaker, paramName, paramValue));
        }
        return new Tree[] { oldTree, newTree };
    }

    private final ElementHandle<TypeElement> myHandle;
}
