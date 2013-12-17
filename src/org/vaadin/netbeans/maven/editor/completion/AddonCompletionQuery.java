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
package org.vaadin.netbeans.maven.editor.completion;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.Document;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.ClasspathInfo.PathKind;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.maven.editor.completion.SourceClass.SourceType;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;
import org.vaadin.netbeans.utils.JavaUtils;
import org.vaadin.netbeans.utils.XmlUtils;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

/**
 * @author denis
 */
class AddonCompletionQuery extends AsyncCompletionQuery implements
        Task<CompilationController>
{

    private static final Logger LOG = Logger
            .getLogger(AddonCompletionQuery.class.getName());

    AddonCompletionQuery( int offset ) {
        myOffset = offset;
    }

    @Override
    public void run( CompilationController controller ) throws Exception {
        if (isTaskCancelled()) {
            return;
        }
        controller.toPhase(Phase.PARSED);

        if (isTaskCancelled()) {
            return;
        }

        TokenSequence<JavaTokenId> tokenSequence =
                controller.getTokenHierarchy().tokenSequence(
                        JavaTokenId.language());
        if (tokenSequence.move(myOffset) == 0 || !tokenSequence.moveNext()) {
            tokenSequence.movePrevious();
        }
        int length = myOffset - tokenSequence.offset();
        if (length >= 3 && tokenSequence.token().length() >= length
                && tokenSequence.token().id() == JavaTokenId.IDENTIFIER)
        {
            TreePath path = controller.getTreeUtilities().pathFor(myOffset);
            if (path != null
                    && path.getLeaf().getKind().equals(Tree.Kind.IDENTIFIER))
            {
                myPrefix =
                        tokenSequence.token().text().toString()
                                .substring(0, length);
                myTokenOffset = tokenSequence.offset();
            }
        }
    }

    @Override
    protected void query( CompletionResultSet resultSet, Document document,
            int caretOffset )
    {
        myOffset = caretOffset;
        List<CompletionItem> items = new LinkedList<>();

        JavaSource javaSource = JavaSource.forDocument(document);
        if (javaSource == null) {
            return;
        }
        try {
            javaSource.runUserActionTask(this, true);

            SourceType type = getSourceType(document);
            if (myPrefix != null) {
                VaadinSupport support =
                        AddOnCompletionProvider.getSupport(document);
                Collection<? extends AddOnClass> classes =
                        AddOnProvider.getInstance().searchClasses(myPrefix,
                                type);
                Set<String> addonsInClassPath = new HashSet<>();
                List<AddonCompletionItem> addonItems = new LinkedList<>();
                for (AddOnClass clazz : classes) {
                    if (addonsInClassPath.contains(clazz.getAddOnName())) {
                        continue;
                    }
                    if (isClassInClassPath(clazz, support)) {
                        addonsInClassPath.add(clazz.getAddOnName());
                        continue;
                    }
                    AddonCompletionItem item =
                            new AddonCompletionItem(myTokenOffset, clazz);
                    addonItems.add(item);
                }
                for (Iterator<AddonCompletionItem> iterator =
                        addonItems.iterator(); iterator.hasNext();)
                {
                    AddonCompletionItem next = iterator.next();
                    if (addonsInClassPath.contains(next.getAddOnClass()
                            .getAddOnName()))
                    {
                        iterator.remove();
                    }

                }
                items.addAll(addonItems);
            }

            resultSet.addAllItems(items);
            resultSet.setAnchorOffset(myOffset);
        }
        catch (IOException e) {
            LOG.log(Level.INFO, null, e);
        }
        finally {
            resultSet.finish();
        }
    }

    private boolean isClassInClassPath( AddOnClass clazz, VaadinSupport support )
    {
        ClassPath classPath =
                support.getClassPathInfo().getClassPath(PathKind.COMPILE);
        if (classPath.findResource(clazz.getQualifiedName().replace('.', '/')
                + JavaUtils.CLASS_SUFFIX) != null)// NOI18N 
        {
            return true;
        }
        classPath = support.getClassPathInfo().getClassPath(PathKind.SOURCE);
        if (classPath.findResource(clazz.getQualifiedName().replace('.', '/')
                + JavaUtils.JAVA_SUFFIX) != null)// NOI18N 
        {
            return true;
        }
        return false;
    }

    private SourceType getSourceType( Document document ) {
        FileObject fileObject = NbEditorUtilities.getFileObject(document);
        Project project = FileOwnerQuery.getOwner(fileObject);
        Set<FileObject> testRoots = JavaUtils.getTestRoots(project);
        for (FileObject testRoot : testRoots) {
            if (FileUtil.isParentOf(testRoot, fileObject)) {
                return SourceType.TEST;
            }
        }
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        final List<FileObject> clientPkgs = new LinkedList<>();
        try {
            support.runModelOperation(new ModelOperation() {

                @Override
                public void run( VaadinModel model ) {
                    FileObject gwtXml = model.getGwtXml();
                    List<String> sourcePaths = model.getSourcePaths();
                    for (String srcPath : sourcePaths) {
                        try {
                            FileObject clientPkg =
                                    XmlUtils.getClientWidgetPackage(gwtXml,
                                            srcPath, false);
                            if (clientPkg != null) {
                                clientPkgs.add(clientPkg);
                            }
                        }
                        catch (IOException e) {
                            LOG.log(Level.INFO, null, e);
                        }
                    }
                }
            });
        }
        catch (IOException e) {
            LOG.log(Level.INFO, null, e);
        }
        for (FileObject clientPkg : clientPkgs) {
            if (FileUtil.isParentOf(clientPkg, fileObject)) {
                return SourceType.CLIENT;
            }
        }
        return SourceType.SERVER;
    }

    private int myOffset;

    private int myTokenOffset;

    private String myPrefix;

}
