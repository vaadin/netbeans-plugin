/**
 *
 */
package org.vaadin.netbeans.editor.analyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.TreeUtilities;
import org.netbeans.api.java.source.ModificationResult.Difference;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.filesystems.FileObject;
import org.openide.text.PositionRef;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;

/**
 * @author denis
 */
abstract class AbstractJavaFix implements Fix {

    static final String HTTP_SERVLET = "javax.servlet.http.HttpServlet"; // NOI18N

    protected AbstractJavaFix( FileObject fileObject ) {
        myFileObject = fileObject;
    }

    protected ChangeInfo createChangeInfo( ModificationResult... results )
            throws IOException
    {
        ChangeInfo changeInfo = new ChangeInfo();
        for (ModificationResult result : results) {
            List<? extends Difference> differences = result
                    .getDifferences(getFileObject());
            for (Difference difference : differences) {
                PositionRef start = difference.getStartPosition();
                PositionRef end = difference.getEndPosition();
                changeInfo.add(getFileObject(), start.getPosition(),
                        end.getPosition());
            }
        }
        return changeInfo;
    }

    protected FileObject getFileObject() {
        return myFileObject;
    }

    protected Logger getLogger() {
        return Logger.getLogger(getClass().getName());
    }

    static List<Integer> getElementPosition( CompilationInfo info,
            TypeElement type )
    {
        ClassTree classTree = info.getTrees().getTree(type);
        return getElementPosition(info, classTree);
    }

    static List<Integer> getElementPosition( CompilationInfo info, Tree tree ) {
        SourcePositions srcPos = info.getTrees().getSourcePositions();

        int startOffset = (int) srcPos.getStartPosition(
                info.getCompilationUnit(), tree);
        int endOffset = (int) srcPos.getEndPosition(info.getCompilationUnit(),
                tree);

        Tree startTree = null;

        if (TreeUtilities.CLASS_TREE_KINDS.contains(tree.getKind())) {
            startTree = ((ClassTree) tree).getModifiers();

        }
        else if (tree.getKind() == Tree.Kind.METHOD) {
            startTree = ((MethodTree) tree).getReturnType();
        }
        else if (tree.getKind() == Tree.Kind.VARIABLE) {
            startTree = ((VariableTree) tree).getType();
        }

        if (startTree != null) {
            int searchStart = (int) srcPos.getEndPosition(
                    info.getCompilationUnit(), startTree);

            TokenSequence<?> tokenSequence = info.getTreeUtilities().tokensFor(
                    tree);

            if (tokenSequence != null) {
                boolean eob = false;
                tokenSequence.move(searchStart);

                do {
                    eob = !tokenSequence.moveNext();
                }
                while (!eob
                        && tokenSequence.token().id() != JavaTokenId.IDENTIFIER);

                if (!eob) {
                    Token<?> identifier = tokenSequence.token();
                    startOffset = identifier.offset(info.getTokenHierarchy());
                    endOffset = startOffset + identifier.length();
                }
            }
        }

        List<Integer> result = new ArrayList<>(2);
        result.add(startOffset);
        result.add(endOffset);
        return result;
    }

    static AssignmentTree getAnnotationTreeAttribute(
            AnnotationTree annotation, String attributeName )
    {
        List<? extends ExpressionTree> arguments = annotation.getArguments();
        for (ExpressionTree expressionTree : arguments) {
            if (expressionTree instanceof AssignmentTree) {
                AssignmentTree assignmentTree = (AssignmentTree) expressionTree;
                ExpressionTree expression = assignmentTree.getVariable();
                if (expression instanceof IdentifierTree) {
                    IdentifierTree identifier = (IdentifierTree) expression;
                    if (identifier.getName().contentEquals(attributeName)) {
                        return assignmentTree;
                    }
                }
            }
        }
        return null;
    }

    static ErrorDescription createExtendServletFix( TypeElement type,
            CompilationInfo info, String errorText, Severity severity )
    {
        TypeMirror superclass = type.getSuperclass();
        TypeElement superElement = (TypeElement) info.getTypes().asElement(
                superclass);
        List<Integer> positions = AbstractJavaFix
                .getElementPosition(info, type);
        List<Fix> fixes = Collections.emptyList();
        Name qName = superElement.getQualifiedName();
        if (qName.contentEquals(Object.class.getName())
                || qName.contentEquals(AbstractJavaFix.HTTP_SERVLET))
        {
            Fix fix = new ExtendVaadinServletFix(info.getFileObject(),
                    ElementHandle.create(type));
            fixes = Collections.singletonList(fix);
        }
        ErrorDescription description = ErrorDescriptionFactory
                .createErrorDescription(severity, errorText, fixes,
                        info.getFileObject(), positions.get(0),
                        positions.get(1));
        return description;
    }

    private FileObject myFileObject;
}
