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

import javax.swing.Action;

import org.netbeans.api.fileinfo.NonRecursiveFolder;
import org.netbeans.modules.refactoring.api.ui.ExplorerContext;
import org.netbeans.modules.refactoring.api.ui.RefactoringActionsFactory;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 * @author denis
 */
public class RenamePackageFix implements Fix {

    public RenamePackageFix( FileObject pkg, String toFqn ) {
        myPkg = pkg;
        myTargetFqn = toFqn;
    }

    @NbBundle.Messages({ "# {0} - target fqn",
            "renameClientPackage=Rename package {0}" })
    @Override
    public String getText() {
        return Bundle.renameClientPackage(myTargetFqn);
    }

    @Override
    public ChangeInfo implement() throws Exception {
        DataObject dataObject = DataObject.find(myPkg);
        if (dataObject != null) {
            ExplorerContext context = new ExplorerContext();
            context.setNewName(myTargetFqn);
            NonRecursiveFolder folder = new NonRecursiveFolder() {

                @Override
                public FileObject getFolder() {
                    return myPkg;
                }

            };
            Node nodeDelegate = dataObject.getNodeDelegate();
            Node node =
                    new FilterNode(nodeDelegate, Children.LEAF,
                            new ProxyLookup(nodeDelegate.getLookup(),
                                    Lookups.singleton(folder)));
            Lookup lookup = Lookups.fixed(node, context);
            Action action =
                    RefactoringActionsFactory.renameAction()
                            .createContextAwareInstance(lookup);
            action.actionPerformed(RefactoringActionsFactory.DEFAULT_EVENT);
        }
        return null;
    }

    private final FileObject myPkg;

    private final String myTargetFqn;

}
