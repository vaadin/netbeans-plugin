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
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.maven.api.execute.RunConfig;
import org.netbeans.modules.maven.api.execute.RunUtils;
import org.netbeans.modules.maven.spi.debug.MavenDebugger;
import org.netbeans.spi.project.ui.CustomizerProvider2;
import org.openide.DialogDescriptor;
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
import org.vaadin.netbeans.maven.directory.AbstractLicenseChooser;
import org.vaadin.netbeans.maven.directory.SearchPanel;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.License;
import org.vaadin.netbeans.maven.editor.completion.AddOn;
import org.vaadin.netbeans.maven.editor.completion.SourceClass;
import org.vaadin.netbeans.maven.editor.completion.SourceClass.SourceType;
import org.vaadin.netbeans.utils.POMUtils;
import org.vaadin.netbeans.utils.UIGestureUtils;
import org.vaadin.netbeans.utils.XmlUtils;

/**
 * @author denis
 */
public class VaadinAction extends AbstractAction implements Popup,
        ContextAwareAction
{

    static final String VAADIN_DEV_MODE_GOAL = "vaadin:run"; // NOI18N

    static final String VAADIN_DEBUG_DEV_MODE_GOAL = "vaadin:debug"; // NOI18N

    static final String UI_LOGGER_NAME = "org.netbeans.ui.vaadin.actions"; // NOI18N

    static final Logger UI_LOG = Logger.getLogger(UI_LOGGER_NAME);

    private static final String VAADIN_SUPER_DEV_MODE_GOAL =
            "vaadin:run-codeserver"; // NOI18N

    private static final String COMPILE = "vaadin:compile"; // NOI18N

    private static final String COMPILE_THEME = "vaadin:compile-theme"; // NOI18N

    private static final String SCOPE_TEST = "test"; // NOI18N

    private static final String SCOPE_COMPILE = "compile"; // NOI18N

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
            VaadinSupport support =
                    myProject.getLookup().lookup(VaadinSupport.class);
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
            menu.addSeparator();
            menu.add(createAddonsBrowserItem());

            menu.setEnabled(isEnabled());
            return menu;
        }
        return null;
    }

    public static Project getProject( Lookup lookup ) {
        if (lookup == null) {
            return null;
        }
        Collection<? extends Project> projects =
                lookup.lookupAll(Project.class);
        if (!projects.isEmpty()) {
            return projects.iterator().next();
        }
        Collection<? extends DataObject> dataObjects =
                lookup.lookupAll(DataObject.class);
        if (dataObjects.isEmpty()) {
            return null;
        }
        FileObject fObj = dataObjects.iterator().next().getPrimaryFile();
        return FileOwnerQuery.getOwner(fObj);
    }

    @NbBundle.Messages({ "browseAddonsAction=Open Add-Ons Browser",
            "browseAddons=Browse Add-Ons", "add=Add", "search=Search",
            "close=Close" })
    private JMenuItem createAddonsBrowserItem() {
        JMenuItem item = new JMenuItem(Bundle.browseAddonsAction());
        item.addActionListener(new AbstractActionListener() {

            @Override
            public void actionPerformed( ActionEvent e ) {
                logUiUsage("UI_LogAddonBrowserAction"); // NOI18N

                final JButton add = new JButton(Bundle.add());
                add.setEnabled(false);

                final SearchPanel panel = new SearchPanel();
                panel.addChangeListener(new ChangeListener() {

                    @Override
                    public void stateChanged( ChangeEvent e ) {
                        myAddOn = panel.getSelected();
                        add.setEnabled(myAddOn != null);
                    }
                });

                JButton search = new JButton(Bundle.search());
                search.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed( ActionEvent e ) {
                        panel.updateTable();
                    }
                });
                JButton close = new JButton(Bundle.close());

                DialogDescriptor descriptor =
                        new DialogDescriptor(panel, Bundle.browseAddons(),
                                true, new Object[] { search, add, close },
                                search, DialogDescriptor.DEFAULT_ALIGN, null,
                                null);
                descriptor.setClosingOptions(new Object[] { add, close });
                Object option = DialogDisplayer.getDefault().notify(descriptor);
                if (option == add) {
                    myLicense = null;
                    if (myAddOn != null) {
                        AddonHandler handler = new AddonHandler();
                        myLicense = handler.getLicense(myAddOn);
                    }
                    if (myLicense != null) {
                        runOutAwt();
                    }
                }
            }

            @Override
            public void run() {
                if (myLicense != null) {
                    POMUtils.addDependency(myProject, myLicense.getGroupId(),
                            myLicense.getArtifactId(), myLicense.getVersion(),
                            getScope(myAddOn));
                }
            }

            private volatile AddOn myAddOn;

            private volatile License myLicense;
        });

        return item;
    }

    @NbBundle.Messages({ "compileTheme=Compile Theme", "# {0} - project name",
            "compileThemeTask={0}: Compile Theme" })
    private JMenuItem createCompileThemeItem() {
        JMenuItem item = new JMenuItem(Bundle.compileTheme());
        item.addActionListener(new AbstractActionListener() {

            @Override
            public void run() {
                logUiUsage("UI_LogCompileThemeAction"); // NOI18N

                String name =
                        ProjectUtils.getInformation(myProject).getDisplayName();
                RunConfig config =
                        RunUtils.createRunConfig(FileUtil.toFile(myProject
                                .getProjectDirectory()), myProject, Bundle
                                .compileThemeTask(name), Collections
                                .singletonList(COMPILE_THEME));
                ExecutorTask task = RunUtils.executeMaven(config);
                VaadinSupport support =
                        myProject.getLookup().lookup(VaadinSupport.class);
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
                logUiUsage("UI_LogCompileWidgetsetAction"); // NOI18N

                String name =
                        ProjectUtils.getInformation(myProject).getDisplayName();
                RunConfig config =
                        RunUtils.createRunConfig(FileUtil.toFile(myProject
                                .getProjectDirectory()), myProject, Bundle
                                .compile(name), Collections
                                .singletonList(COMPILE));
                ExecutorTask task = RunUtils.executeMaven(config);
                VaadinSupport support =
                        myProject.getLookup().lookup(VaadinSupport.class);
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
                CustomizerProvider2 customizer =
                        myProject.getLookup().lookup(CustomizerProvider2.class);
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
                logUiUsage("UI_LogRunDevModeAction"); // NOI18N

                String name =
                        ProjectUtils.getInformation(myProject).getDisplayName();
                RunConfig config =
                        RunUtils.createRunConfig(FileUtil.toFile(myProject
                                .getProjectDirectory()), myProject, Bundle
                                .runAppInDevMode(name), Collections
                                .singletonList(VAADIN_DEV_MODE_GOAL));
                ExecutorTask task = RunUtils.executeMaven(config);
                VaadinSupport support =
                        myProject.getLookup().lookup(VaadinSupport.class);
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
                logUiUsage("UI_LogDebugDevModeAction"); // NOI18N

                VaadinSupport support =
                        myProject.getLookup().lookup(VaadinSupport.class);
                if (support != null) {
                    Collection<ExecutorTask> tasks =
                            support.getTasks(VaadinSupport.Action.DEBUG_DEV_MODE);
                    for (ExecutorTask task : tasks) {
                        task.stop();
                    }
                }
                String name =
                        ProjectUtils.getInformation(myProject).getDisplayName();
                RunConfig config =
                        RunUtils.createRunConfig(FileUtil.toFile(myProject
                                .getProjectDirectory()), myProject, Bundle
                                .runAppInDevMode(name), Collections
                                .singletonList(VAADIN_DEBUG_DEV_MODE_GOAL));
                ExecutorTask task = RunUtils.executeMaven(config);
                support = myProject.getLookup().lookup(VaadinSupport.class);
                if (support != null) {
                    support.addAction(VaadinSupport.Action.DEBUG_DEV_MODE, task);
                }
                InetSocketAddress address =
                        DebugUtils.getBindAddress(myProject);
                int seconds = DebugUtils.getAttachDebuggerTimout(myProject);
                boolean doLoop = true;
                while (doLoop) {
                    doLoop = false;
                    boolean ready;
                    try {
                        ready =
                                DebugUtils.waitPort(address.getHostString(),
                                        address.getPort(), seconds * 1000, task);
                    }
                    catch (InterruptedException e) {
                        return;
                    }
                    if (ready) {
                        MavenDebugger debugger =
                                myProject.getLookup().lookup(
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
                            NotifyDescriptor descriptor =
                                    new NotifyDescriptor.Message(
                                            Bundle.unableToAttach(address
                                                    .getHostString(), String
                                                    .valueOf(address.getPort())),
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
        NotifyDescriptor descriptor =
                new NotifyDescriptor.Message(Bundle.debuggerNotStarter(
                        address.getHostString(),
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
                logUiUsage("UI_LogSuperDevModeAction"); // NOI18N

                VaadinSupport support =
                        myProject.getLookup().lookup(VaadinSupport.class);
                if (support != null) {
                    XmlUtils.enableSuperDevMode(support);
                }
                String name =
                        ProjectUtils.getInformation(myProject).getDisplayName();
                RunConfig config =
                        RunUtils.createRunConfig(FileUtil.toFile(myProject
                                .getProjectDirectory()), myProject, Bundle
                                .runAppInSuperDevMode(name), Collections
                                .singletonList(VAADIN_SUPER_DEV_MODE_GOAL));
                ExecutorTask task = RunUtils.executeMaven(config);
                if (support != null) {
                    support.addAction(VaadinSupport.Action.SUPER_DEV_MODE, task);
                }
            }
        });
        return item;
    }

    private String getScope( AddOn addon ) {
        List<SourceClass> classes = addon.getClasses();
        if (classes == null || classes.isEmpty()) {
            return SCOPE_COMPILE;
        }
        for (SourceClass clazz : classes) {
            SourceType type = clazz.getType();
            if (!SourceType.TEST.equals(type)) {
                return SCOPE_COMPILE;
            }
        }
        return SCOPE_TEST;
    }

    protected static void logUiUsage( String key, String... params ) {
        UIGestureUtils.logUiUsage(VaadinAction.class, UI_LOGGER_NAME, key,
                params);
    }

    private static class AddonHandler extends AbstractLicenseChooser {

        @Override
        public License getLicense( AbstractAddOn addon ) {
            return super.getLicense(addon);
        }

    }

    private Project myProject;
}
