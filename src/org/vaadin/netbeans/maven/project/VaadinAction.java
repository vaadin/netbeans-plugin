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
package org.vaadin.netbeans.maven.project;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.maven.api.execute.RunConfig;
import org.netbeans.modules.maven.api.execute.RunUtils;
import org.netbeans.modules.maven.spi.debug.MavenDebugger;
import org.netbeans.spi.project.ui.CustomizerProvider2;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.DynamicMenuContent;
import org.openide.execution.ExecutorTask;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter.Popup;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.code.generator.XmlUtils;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;

/**
 * @author denis
 */
public class VaadinAction extends AbstractAction implements Popup,
        ContextAwareAction
{

    static final String VAADIN_DEV_MODE_GOAL = "vaadin:run"; // NOI18N

    static final String VAADIN_DEBUG_DEV_MODE_GOAL = "vaadin:debug"; // NOI18N

    private static final String VAADIN_SUPER_DEV_MODE_GOAL = "vaadin:run-codeserver"; // NOI18N

    private static final String COMPILE = "vaadin:compile"; // NOI18N

    private static final String COMPILE_THEME = "vaadin:compile-theme"; // NOI18N

    private static final Logger LOG = Logger.getLogger(VaadinAction.class
            .getName());

    public VaadinAction() {
        this(null);
    }

    public VaadinAction( Lookup lookup ) {
        myProject = getProject(lookup);
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
        return new VaadinAction(lookup);
    }

    @Override
    @NbBundle.Messages("vaadin=Vaadin")
    public JMenu getPopupPresenter() {
        if (isEnabled()) {
            JMenu menu = new JMenu(Bundle.vaadin());

            menu.add(createCompileItem());
            menu.add(createCompileThemeItem());

            menu.addSeparator();

            menu.add(createDevModeItem());
            menu.add(createDebugDevModeItem());
            menu.add(createSuperDevModeItem());

            /*
             * Maven project doesn't provide CustomizerProvider2 instance.
             * Uncomment this code when it will be available.
             * menu.addSeparator(); menu.add(createVaadinPropertiesItem());
             */

            menu.setEnabled(isEnabled());
            return menu;
        }
        return null;
    }

    public static Project getProject( Lookup lookup ) {
        if (lookup == null) {
            return null;
        }
        Collection<? extends Project> projects = lookup
                .lookupAll(Project.class);
        if (!projects.isEmpty()) {
            return projects.iterator().next();
        }
        Collection<? extends DataObject> dataObjects = lookup
                .lookupAll(DataObject.class);
        if (dataObjects.isEmpty()) {
            return null;
        }
        FileObject fObj = dataObjects.iterator().next().getPrimaryFile();
        return FileOwnerQuery.getOwner(fObj);
    }

    @NbBundle.Messages({ "compileTheme=Compile Theme", "# {0} - project name",
            "compileThemeTask={0}: Compile Theme" })
    private JMenuItem createCompileThemeItem() {
        JMenuItem item = new JMenuItem(Bundle.compileTheme());
        item.addActionListener(new AbstractActionListener() {

            @Override
            public void run() {
                String name = ProjectUtils.getInformation(myProject)
                        .getDisplayName();
                RunConfig config = RunUtils.createRunConfig(
                        FileUtil.toFile(myProject.getProjectDirectory()),
                        myProject, Bundle.compileThemeTask(name),
                        Collections.singletonList(COMPILE_THEME));
                ExecutorTask task = RunUtils.executeMaven(config);
                VaadinSupport support = myProject.getLookup().lookup(
                        VaadinSupport.class);
                if (support != null) {
                    support.addAction(VaadinSupport.Action.COMPILE_THEME, task);
                }
            }
        });
        return item;
    }

    @NbBundle.Messages({ "compileWidgetset=Compile Widgetset and Theme",
            "# {0} - project name", "compile={0}: Compile Widgetset" })
    private JMenuItem createCompileItem() {
        JMenuItem item = new JMenuItem(Bundle.compileWidgetset());
        item.addActionListener(new AbstractActionListener() {

            @Override
            public void run() {
                String name = ProjectUtils.getInformation(myProject)
                        .getDisplayName();
                RunConfig config = RunUtils.createRunConfig(
                        FileUtil.toFile(myProject.getProjectDirectory()),
                        myProject, Bundle.compile(name),
                        Collections.singletonList(COMPILE));
                ExecutorTask task = RunUtils.executeMaven(config);
                VaadinSupport support = myProject.getLookup().lookup(
                        VaadinSupport.class);
                if (support != null) {
                    support.addAction(VaadinSupport.Action.COMPILE, task);
                }
            }
        });
        return item;
    }

    @NbBundle.Messages("vaadinProperties=Vaadin Properties")
    private JMenuItem createVaadinPropertiesItem() {
        JMenuItem item = new JMenuItem(Bundle.vaadinProperties());
        item.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed( ActionEvent e ) {
                CustomizerProvider2 customizer = myProject.getLookup().lookup(
                        CustomizerProvider2.class);
                customizer.showCustomizer(VaadinCustomizer.VAADIN_CATEGORY,
                        null);
            }
        });
        return item;
    }

    @NbBundle.Messages({ "runDevMode=Run Dev Mode", "# {0} - project name",
            "runAppInDevMode={0}: GWT Dev Mode" })
    private JMenuItem createDevModeItem() {
        JMenuItem item = new JMenuItem(Bundle.runDevMode());
        item.addActionListener(new AbstractActionListener() {

            @Override
            public void run() {
                String name = ProjectUtils.getInformation(myProject)
                        .getDisplayName();
                RunConfig config = RunUtils.createRunConfig(
                        FileUtil.toFile(myProject.getProjectDirectory()),
                        myProject, Bundle.runAppInDevMode(name),
                        Collections.singletonList(VAADIN_DEV_MODE_GOAL));
                ExecutorTask task = RunUtils.executeMaven(config);
                VaadinSupport support = myProject.getLookup().lookup(
                        VaadinSupport.class);
                if (support != null) {
                    support.addAction(VaadinSupport.Action.DEV_MODE, task);
                }
            }
        });
        return item;
    }

    @NbBundle.Messages({
            "debugDevMode=Run Dev Mode (debug)",
            "# {0} - host",
            "# {1} - port",
            "unableToAttach=Unable to attach debugger "
                    + "to the host ''{0}'' and port {1}",
            "debugName={0}: GWT Dev Mode" })
    private JMenuItem createDebugDevModeItem() {
        JMenuItem item = new JMenuItem(Bundle.debugDevMode());
        item.addActionListener(new AbstractActionListener() {

            @Override
            public void run() {
                VaadinSupport support = myProject.getLookup().lookup(
                        VaadinSupport.class);
                if (support != null) {
                    Collection<ExecutorTask> tasks = support
                            .getTasks(VaadinSupport.Action.DEBUG_DEV_MODE);
                    for (ExecutorTask task : tasks) {
                        task.stop();
                    }
                }
                String name = ProjectUtils.getInformation(myProject)
                        .getDisplayName();
                RunConfig config = RunUtils.createRunConfig(
                        FileUtil.toFile(myProject.getProjectDirectory()),
                        myProject, Bundle.runAppInDevMode(name),
                        Collections.singletonList(VAADIN_DEBUG_DEV_MODE_GOAL));
                ExecutorTask task = RunUtils.executeMaven(config);
                support = myProject.getLookup().lookup(VaadinSupport.class);
                if (support != null) {
                    support.addAction(VaadinSupport.Action.DEBUG_DEV_MODE, task);
                }
                InetSocketAddress address = DebugUtils
                        .getBindAddress(myProject);
                int seconds = DebugUtils.getAttachDebuggerTimout(myProject);
                boolean doLoop = true;
                while (doLoop) {
                    doLoop = false;
                    boolean ready;
                    try {
                        ready = DebugUtils.waitPort(address.getHostString(),
                                address.getPort(), seconds * 1000, task);
                    }
                    catch (InterruptedException e) {
                        return;
                    }
                    if (ready) {
                        MavenDebugger debugger = myProject.getLookup().lookup(
                                MavenDebugger.class);
                        try {
                            debugger.attachDebugger(null,
                                    Bundle.debugName(name),
                                    DebugUtils.DEBUG_TRANSPORT,
                                    address.getHostString(),
                                    String.valueOf(address.getPort()));
                        }
                        catch (Exception e) {
                            LOG.log(Level.INFO, null, e);
                            NotifyDescriptor descriptor = new NotifyDescriptor.Message(
                                    Bundle.unableToAttach(
                                            address.getHostString(),
                                            String.valueOf(address.getPort())),
                                    NotifyDescriptor.ERROR_MESSAGE);
                            DialogDisplayer.getDefault().notify(descriptor);
                        }
                    }
                    else {
                        if (askContinueWaiting(address, seconds)) {
                            doLoop = true;
                        }
                        else {
                            task.stop();
                        }
                    }
                }
            }

        });
        return item;
    }

    @NbBundle.Messages({
            "waitDebugger=Continue Waiting",
            "stop=Stop Debug",
            "# {0} - host",
            "# {1} - port",
            "# {2} - timeout",
            "debuggerNotStarter=Unable to connect to debugger on host ''{0}'', "
                    + "port {1} within {2} seconds" })
    private boolean askContinueWaiting( InetSocketAddress address, int seconds )
    {
        NotifyDescriptor descriptor = new NotifyDescriptor.Message(
                Bundle.debuggerNotStarter(address.getHostString(),
                        String.valueOf(address.getPort()), seconds),
                NotifyDescriptor.ERROR_MESSAGE);
        JButton wait = new JButton(Bundle.waitDebugger());
        JButton stop = new JButton(Bundle.stop());
        descriptor.setOptions(new Object[] { wait, stop });
        return wait.equals(DialogDisplayer.getDefault().notify(descriptor));
    }

    @NbBundle.Messages({ "runSuperDevMode=Run Super Dev Mode",
            "# {0} - project name",
            "runAppInSuperDevMode={0}: GWT Super Dev Mode" })
    private JMenuItem createSuperDevModeItem() {
        JMenuItem item = new JMenuItem(Bundle.runSuperDevMode());
        item.addActionListener(new AbstractActionListener() {

            @Override
            public void run() {
                VaadinSupport support = myProject.getLookup().lookup(
                        VaadinSupport.class);
                if (support != null) {
                    final FileObject[] gwtXml = new FileObject[1];
                    try {
                        support.runModelOperation(new ModelOperation() {

                            @Override
                            public void run( VaadinModel model ) {
                                if (!model.isSuperDevModeEnabled()) {
                                    gwtXml[0] = model.getGwtXml();
                                }
                            }
                        });
                    }
                    catch (IOException e) {
                        LOG.log(Level.INFO, null, e);
                    }
                    if (gwtXml[0] != null) {
                        XmlUtils.enableSuperDevMode(gwtXml[0]);
                    }
                }
                String name = ProjectUtils.getInformation(myProject)
                        .getDisplayName();
                RunConfig config = RunUtils.createRunConfig(
                        FileUtil.toFile(myProject.getProjectDirectory()),
                        myProject, Bundle.runAppInSuperDevMode(name),
                        Collections.singletonList(VAADIN_SUPER_DEV_MODE_GOAL));
                ExecutorTask task = RunUtils.executeMaven(config);
                if (support != null) {
                    support.addAction(VaadinSupport.Action.SUPER_DEV_MODE, task);
                }
            }
        });
        return item;
    }

    private Project myProject;
}
