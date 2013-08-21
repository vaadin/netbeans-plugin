/**
 *
 */
package org.vaadin.netbeans.refactoring;

import java.util.Collection;

import org.netbeans.api.java.source.ui.ScanDialog;
import org.netbeans.modules.refactoring.api.ui.ExplorerContext;
import org.netbeans.modules.refactoring.spi.ui.ActionsImplementationProvider;
import org.netbeans.modules.refactoring.spi.ui.UI;
import org.openide.actions.DeleteAction;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.ServiceProvider;
import org.vaadin.netbeans.code.generator.XmlUtils;

/**
 * @author denis
 */
@ServiceProvider(service = ActionsImplementationProvider.class)
public class GwtRefactoringActionsProvider extends
        ActionsImplementationProvider
{

    private static final String NODE_ATTRIBUTE_NAME = GwtRefactoringActionsProvider.class
            .getName();

    @Override
    public boolean canRename( Lookup lookup ) {
        return getGwtXml(lookup) != null;
    }

    @Override
    public void doRename( final Lookup lookup ) {
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                UI.openRefactoringUI(new RenameRefactoringUI(getGwtXml(lookup)));
            }
        };
        ScanDialog.runWhenScanFinished(runnable, Bundle.rename());
    }

    @Override
    public boolean canMove( Lookup lookup ) {
        return getGwtXml(lookup) != null;
    }

    @Override
    public void doMove( final Lookup lookup ) {
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                UI.openRefactoringUI(new MoveRefactoringUI(getGwtXml(lookup),
                        getContextTarget(lookup)));
            }
        };
        ScanDialog.runWhenScanFinished(runnable, Bundle.move());
    }

    @Override
    public boolean canCopy( Lookup lookup ) {
        return getGwtXml(lookup) != null;
    }

    @Override
    public void doCopy( final Lookup lookup ) {
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                UI.openRefactoringUI(new CopyRefactoringUI(getGwtXml(lookup),
                        getContextTarget(lookup)));
            }
        };
        ScanDialog.runWhenScanFinished(runnable, Bundle.copy());
    }

    @Override
    public boolean canDelete( Lookup lookup ) {
        if (getGwtXml(lookup) != null) {
            Node node = lookup.lookup(Node.class);
            if (Boolean.TRUE.equals(node.getValue(NODE_ATTRIBUTE_NAME))) {
                node.setValue(NODE_ATTRIBUTE_NAME, Boolean.FALSE);
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void doDelete( final Lookup lookup ) {
        if (lookup.lookup(ExplorerContext.class) != null) {
            Node node = lookup.lookup(Node.class);
            node.setValue(NODE_ATTRIBUTE_NAME, Boolean.TRUE);
            try {
                SystemAction.get(DeleteAction.class).actionPerformed(null);
                return;
            }
            finally {
                node.setValue(NODE_ATTRIBUTE_NAME, Boolean.FALSE);
            }
        }
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                UI.openRefactoringUI(new DeleteRefactoringUI(getGwtXml(lookup)));
            }
        };
        ScanDialog.runWhenScanFinished(runnable, Bundle.delete());
    }

    private FileObject getContextTarget( final Lookup lookup ) {
        ExplorerContext explorerContext = lookup.lookup(ExplorerContext.class);
        FileObject target = null;
        if (explorerContext != null) {
            Node targetNode = explorerContext.getTargetNode();
            if (targetNode != null) {
                target = targetNode.getLookup().lookup(FileObject.class);
            }
        }
        return target;
    }

    private FileObject getGwtXml( Lookup lookup ) {
        Collection<? extends Node> nodes = lookup.lookupAll(Node.class);
        if (nodes.size() == 1) {
            Node node = nodes.iterator().next();
            FileObject fileObject = node.getLookup().lookup(FileObject.class);

            if (fileObject != null
                    && fileObject.getNameExt().endsWith(XmlUtils.GWT_XML))
            {
                return fileObject;
            }
        }
        return null;
    }

}
