/**
 *
 */
package org.vaadin.netbeans.refactoring;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.awt.DynamicMenuContent;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter.Popup;
import org.openide.util.lookup.Lookups;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.code.generator.XmlUtils;

/**
 * @author denis
 */
public class CustomRefactoringAction extends AbstractAction implements Popup,
        ContextAwareAction
{

    public CustomRefactoringAction() {
    }

    public CustomRefactoringAction( Lookup lookup ) {
        putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);

        Collection<? extends Node> nodes = lookup.lookupAll(Node.class);

        setEnabled(false);
        if (nodes.size() == 1) {
            Node node = nodes.iterator().next();
            FileObject fileObject = node.getLookup().lookup(FileObject.class);
            if (fileObject != null
                    && fileObject.getNameExt().endsWith(XmlUtils.GWT_XML))
            {
                Project project = FileOwnerQuery.getOwner(fileObject);
                if (project != null) {
                    VaadinSupport support = project.getLookup().lookup(
                            VaadinSupport.class);
                    if (support != null && support.isEnabled()) {
                        setEnabled(true);
                    }
                }
            }
        }
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
    }

    @Override
    public Action createContextAwareInstance( Lookup lookup ) {
        return new CustomRefactoringAction(lookup);
    }

    @Override
    public JMenuItem getPopupPresenter() {
        if (!isEnabled()) {
            return null;
        }
        Object refactorMenuAction = null;
        for (Lookup.Item<Object> item : Lookups.forPath("Actions/Refactoring")
                .lookupResult(Object.class).allItems())
        {
            if (Action.class.isAssignableFrom(item.getType())) {
                String id = item.getId();
                if (id.equals("Actions/Refactoring/RefactoringAll")) {
                    refactorMenuAction = item.getInstance();
                    break;
                }
            }
        }
        JMenu menu = (JMenu) ((Popup) refactorMenuAction).getPopupPresenter();
        JPopupMenu popupMenu = menu.getPopupMenu();
        List<Component> toRemove = new ArrayList<>(
                popupMenu.getComponentCount());
        boolean afterSeparator = false;
        for (int i = 0; i < popupMenu.getComponentCount(); i++) {
            Component component = popupMenu.getComponent(i);
            if (component instanceof JSeparator) {
                afterSeparator = true;
            }
            if (afterSeparator) {
                toRemove.add(component);
            }
        }
        for (Component component : toRemove) {
            popupMenu.remove(component);
        }
        return menu;
    }
}
