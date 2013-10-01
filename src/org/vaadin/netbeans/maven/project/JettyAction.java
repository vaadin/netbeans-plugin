/**
 *
 */
package org.vaadin.netbeans.maven.project;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.maven.api.Constants;
import org.netbeans.modules.maven.api.execute.RunConfig;
import org.netbeans.modules.maven.api.execute.RunUtils;
import org.openide.awt.DynamicMenuContent;
import org.openide.execution.ExecutorTask;
import org.openide.filesystems.FileUtil;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter.Popup;
import org.vaadin.netbeans.VaadinSupport;

/**
 * @author denis
 */
public class JettyAction extends AbstractAction implements Popup,
        ContextAwareAction
{

    private static final String GWT_COMPILER_SKIP = "gwt.compiler.skip";// NOI18N

    private static final String PACKAGE_GOAL = "package"; // NOI18N

    private static final String JETTY_GOAL = "jetty:run";// NOI18N

    public JettyAction() {
        this(null);
    }

    public JettyAction( Lookup lookup ) {
        myProject = VaadinAction.getProject(lookup);
        if (myProject == null) {
            setEnabled(false);
        }
        else {
            VaadinSupport support = myProject.getLookup().lookup(
                    VaadinSupport.class);
            setEnabled(support == null ? false : support.isEnabled()
                    && support.isWeb());
        }
        putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
    }

    @Override
    public void actionPerformed( ActionEvent event ) {
    }

    @Override
    public Action createContextAwareInstance( Lookup lookup ) {
        return new JettyAction(lookup);
    }

    @Override
    @NbBundle.Messages("jetty=Embedded Jetty")
    public JMenu getPopupPresenter() {
        if (isEnabled()) {
            JMenu menu = new JMenu(Bundle.jetty());

            menu.add(createJettyItem());
            menu.add(createDebugJettyItem());
            menu.add(createDevModeJettyItem());

            menu.setEnabled(isEnabled());
            return menu;
        }
        return null;
    }

    @NbBundle.Messages({ "runJetty=Run", "# {0} - project name",
            "runAppInJetty={0}: Jetty Web Server" })
    private JMenuItem createJettyItem() {
        JMenuItem item = new JMenuItem(Bundle.runJetty());
        item.addActionListener(new AbstractActionListener() {

            @Override
            public void run() {
                VaadinSupport support = myProject.getLookup().lookup(
                        VaadinSupport.class);
                if (support != null) {
                    Collection<ExecutorTask> tasks = support
                            .getTasks(VaadinSupport.Action.RUN_JETTY);
                    for (ExecutorTask task : tasks) {
                        task.stop();
                    }
                    tasks = support.getTasks(VaadinSupport.Action.DEBUG_JETTY);
                    for (ExecutorTask task : tasks) {
                        task.stop();
                    }
                }

                List<String> goals = new ArrayList<>(2);
                goals.add(PACKAGE_GOAL);
                goals.add(JETTY_GOAL);
                String name = ProjectUtils.getInformation(myProject)
                        .getDisplayName();
                RunConfig config = RunUtils.createRunConfig(
                        FileUtil.toFile(myProject.getProjectDirectory()),
                        myProject, Bundle.runAppInJetty(name), goals);

                ExecutorTask task = RunUtils.executeMaven(config);
                if (support != null) {
                    support.addAction(VaadinSupport.Action.RUN_JETTY, task);
                }
            }
        });
        return item;
    }

    @NbBundle.Messages("runJettyDevMode=Run with Dev Mode")
    private JMenuItem createDevModeJettyItem() {
        JMenuItem item = new JMenuItem(Bundle.runJettyDevMode());
        item.addActionListener(new AbstractActionListener() {

            @Override
            public void run() {
                VaadinSupport support = myProject.getLookup().lookup(
                        VaadinSupport.class);
                if (support != null) {
                    Collection<ExecutorTask> tasks = support
                            .getTasks(VaadinSupport.Action.RUN_JETTY);
                    for (ExecutorTask task : tasks) {
                        task.stop();
                    }
                }
                List<String> goals = new ArrayList<>(2);
                goals.add(PACKAGE_GOAL);
                goals.add(JETTY_GOAL);
                String name = ProjectUtils.getInformation(myProject)
                        .getDisplayName();
                RunConfig config = RunUtils.createRunConfig(
                        FileUtil.toFile(myProject.getProjectDirectory()),
                        myProject, Bundle.runAppInJetty(name), goals);

                config.setProperty(GWT_COMPILER_SKIP, Boolean.TRUE.toString());

                ExecutorTask task = RunUtils.executeMaven(config);
                boolean runDevMode = true;
                if (support != null) {
                    support.addAction(VaadinSupport.Action.RUN_JETTY, task);
                    runDevMode = support
                            .getTasks(VaadinSupport.Action.DEV_MODE).isEmpty();
                    for (ExecutorTask debugTask : support
                            .getTasks(VaadinSupport.Action.DEBUG_DEV_MODE))
                    {
                        debugTask.stop();
                    }
                }

                if (runDevMode) {
                    config = RunUtils.createRunConfig(FileUtil.toFile(myProject
                            .getProjectDirectory()), myProject, Bundle
                            .runAppInDevMode(ProjectUtils.getInformation(
                                    myProject).getDisplayName()), Collections
                            .singletonList(VaadinAction.VAADIN_DEV_MODE_GOAL));
                    task = RunUtils.executeMaven(config);
                    if (support != null) {
                        support.addAction(VaadinSupport.Action.DEV_MODE, task);
                    }
                }

            }
        });
        return item;

    }

    @NbBundle.Messages({ "debugJetty=Debug", "# {0} - project name",
            "debugAppInJetty={0}: Jetty Web Server" })
    private JMenuItem createDebugJettyItem() {
        JMenuItem item = new JMenuItem(Bundle.debugJetty());
        item.addActionListener(new AbstractActionListener() {

            @Override
            public void run() {
                VaadinSupport support = myProject.getLookup().lookup(
                        VaadinSupport.class);
                if (support != null) {
                    Collection<ExecutorTask> tasks = support
                            .getTasks(VaadinSupport.Action.RUN_JETTY);
                    for (ExecutorTask task : tasks) {
                        task.stop();
                    }
                    tasks = support.getTasks(VaadinSupport.Action.DEBUG_JETTY);
                    for (ExecutorTask task : tasks) {
                        task.stop();
                    }
                }

                String name = ProjectUtils.getInformation(myProject)
                        .getDisplayName();

                List<String> goals = new ArrayList<>();
                goals.add(PACKAGE_GOAL);
                goals.add(JETTY_GOAL);

                RunConfig config = RunUtils.createRunConfig(
                        FileUtil.toFile(myProject.getProjectDirectory()),
                        myProject, Bundle.debugAppInJetty(name), goals);
                config.setProperty(Constants.ACTION_PROPERTY_JPDALISTEN,
                        "maven");// NOI18N
                ExecutorTask task = RunUtils.executeMaven(config);
                if (support != null) {
                    support.addAction(VaadinSupport.Action.DEBUG_JETTY, task);
                }
            }
        });
        return item;
    }

    private Project myProject;
}
