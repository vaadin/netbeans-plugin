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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Action;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.modules.refactoring.api.ui.ExplorerContext;
import org.netbeans.modules.refactoring.api.ui.RefactoringActionsFactory;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;
import org.vaadin.netbeans.utils.JavaUtils;

/**
 * @author denis
 */
public class MoveClientClassFix implements Fix {

    MoveClientClassFix( FileObject clientClass, FileObject targetPackage,
            String targetPackageFqn, boolean withReferences )
    {
        myFileObject = clientClass;
        myTargetPackage = targetPackage;
        myTargetPkgFqn = targetPackageFqn;
        doMoveReferences = withReferences;
    }

    @NbBundle.Messages({ "# {0} - package fqn",
            "moveToPackage=Move Class to {0}",
            "moveWithReferences=Move Class and it''s References to {0}" })
    @Override
    public String getText() {
        if (doMoveReferences) {
            return Bundle.moveWithReferences(myTargetPkgFqn);
        }
        else {
            return Bundle.moveToPackage(myTargetPkgFqn);
        }
    }

    @Override
    public ChangeInfo implement() throws Exception {
        FileObject target = myTargetPackage;
        if (target == null) {
            target = createPackage();
        }
        if (target == null) {
            return null;
        }
        Node[] nodes = getSourceNodes();
        DataObject targetDataObject = DataObject.find(target);
        if (nodes.length > 0 && targetDataObject != null) {
            ExplorerContext context = new ExplorerContext();
            context.setTargetNode(targetDataObject.getNodeDelegate());
            Object[] objects = new Object[nodes.length + 1];
            System.arraycopy(nodes, 0, objects, 0, nodes.length);
            objects[nodes.length] = context;
            final Lookup lookup = Lookups.fixed(objects);
            Mutex.EVENT.readAccess(new Runnable() {

                @Override
                public void run() {
                    Action action =
                            RefactoringActionsFactory.moveAction()
                                    .createContextAwareInstance(lookup);
                    action.actionPerformed(RefactoringActionsFactory.DEFAULT_EVENT);
                }
            });
        }
        return null;
    }

    private Node[] getSourceNodes() throws DataObjectNotFoundException {
        if (doMoveReferences) {
            List<Node> nodes = new ArrayList<>();
            for (FileObject fileObject : getReferences()) {
                DataObject dataObject = DataObject.find(fileObject);
                nodes.add(dataObject.getNodeDelegate());
            }
            return nodes.toArray(new Node[nodes.size()]);
        }
        else {
            DataObject dataObject = DataObject.find(myFileObject);
            return new Node[] { dataObject.getNodeDelegate() };
        }
    }

    private Collection<FileObject> getReferences() {
        Set<FileObject> result = new HashSet<>();
        result.add(myFileObject);
        /*
         *  TODO : implement enhancement that scans current file object and 
         *  find references that should be also moved.
         */
        return result;
    }

    private FileObject createPackage() throws IOException {
        String path = myTargetPkgFqn.replace('.', '/');
        Project project = FileOwnerQuery.getOwner(myFileObject);
        SourceGroup[] sourceGroups = JavaUtils.getJavaSourceGroups(project);
        if (sourceGroups.length == 0) {
            return null;
        }
        FileObject root = sourceGroups[0].getRootFolder();
        return FileUtil.createFolder(root, path);
    }

    private final FileObject myFileObject;

    private final FileObject myTargetPackage;

    private final String myTargetPkgFqn;

    private final boolean doMoveReferences;

}
