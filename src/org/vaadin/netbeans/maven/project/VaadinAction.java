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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.maven.api.execute.RunConfig;
import org.netbeans.modules.maven.api.execute.RunUtils;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.DynamicMenuContent;
import org.openide.execution.ExecutorTask;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
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

/**
 * @author denis
 */
@NbBundle.Messages("vaadin=Vaadin")
public class VaadinAction extends AbstractAction implements ContextAwareAction {

    static final String UI_LOGGER_NAME = "org.netbeans.ui.vaadin.actions"; // NOI18N

    private static final String SCOPE_TEST = "test"; // NOI18N

    private static final String SCOPE_COMPILE = "compile"; // NOI18N

    private static final String COMPILE = "vaadin:compile"; // NOI18N

    private static final String COMPILE_THEME = "vaadin:compile-theme"; // NOI18N

    public VaadinAction() {
        this(null, null);
        setEnabled(false);
    }

    public VaadinAction( Lookup lookup, Project project ) {
        putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
        myProject = project;
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
    }

    @Override
    public Action createContextAwareInstance( Lookup lookup ) {
        Project project = getProject(lookup);
        if (project == null) {
            return null;
        }
        else {
            VaadinSupport support =
                    project.getLookup().lookup(VaadinSupport.class);
            AbstractAction action = null;
            if (support == null) {
                return null;
            }
            if (support.isWeb()) {
                action = new VaadinWebProjectAction(lookup, project);
            }
            else {
                action = new VaadinJarProjectAction(lookup, project);
            }
            action.setEnabled(support != null && support.isEnabled());
            return action;
        }
    }

    protected Project getProject() {
        return myProject;
    }

    @NbBundle.Messages({ "browseAddonsAction=Open Add-Ons Browser",
            "browseAddons=Browse Add-Ons", "add=Add", "search=Search",
            "close=Close" })
    protected JMenuItem createAddonsBrowserItem() {
        JMenuItem item = new JMenuItem(Bundle.browseAddonsAction());
        item.addActionListener(new AddOnBrowserActionListener(getProject()));
        return item;
    }

    @NbBundle.Messages({ "compileTheme=Compile Theme", "# {0} - project name",
            "compileThemeTask={0}: Compile Theme" })
    protected JMenuItem createCompileThemeItem() {
        JMenuItem item = new JMenuItem(Bundle.compileTheme());
        item.addActionListener(new AbstractActionListener() {

            @Override
            public void run() {
                logUiUsage("UI_LogCompileThemeAction"); // NOI18N

                String name =
                        ProjectUtils.getInformation(getProject())
                                .getDisplayName();
                RunConfig config =
                        RunUtils.createRunConfig(FileUtil.toFile(getProject()
                                .getProjectDirectory()), getProject(), Bundle
                                .compileThemeTask(name), Collections
                                .singletonList(COMPILE_THEME));
                ExecutorTask task = RunUtils.executeMaven(config);
                VaadinSupport support =
                        getProject().getLookup().lookup(VaadinSupport.class);
                if (support != null) {
                    support.addAction(VaadinSupport.Action.COMPILE_THEME, task);
                }
            }
        });
        return item;
    }

    @NbBundle.Messages({ "compileWidgetset=Compile Widgetset and Theme",
            "# {0} - project name", "compile={0}: Compile Widgetset" })
    protected JMenuItem createCompileItem() {
        JMenuItem item = new JMenuItem(Bundle.compileWidgetset());
        item.addActionListener(new AbstractActionListener() {

            @Override
            public void run() {
                logUiUsage("UI_LogCompileWidgetsetAction"); // NOI18N

                String name =
                        ProjectUtils.getInformation(getProject())
                                .getDisplayName();
                RunConfig config =
                        RunUtils.createRunConfig(FileUtil.toFile(getProject()
                                .getProjectDirectory()), getProject(), Bundle
                                .compile(name), Collections
                                .singletonList(COMPILE));
                ExecutorTask task = RunUtils.executeMaven(config);
                VaadinSupport support =
                        getProject().getLookup().lookup(VaadinSupport.class);
                if (support != null) {
                    support.addAction(VaadinSupport.Action.COMPILE, task);
                }
            }
        });
        return item;
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

    private static class AddOnBrowserActionListener extends
            AbstractActionListener
    {

        private AddOnBrowserActionListener( Project project ) {
            myProject = project;
        }

        @Override
        public void actionPerformed( ActionEvent e ) {
            logUiUsage("UI_LogAddonBrowserAction"); // NOI18N

            final JButton add = new JButton(Bundle.add());
            add.setEnabled(false);

            SearchPanel panel = createSearchPanel(add);
            JButton search = createSearchButton(panel);
            JButton close = new JButton(Bundle.close());

            DialogDescriptor descriptor =
                    new DialogDescriptor(panel, Bundle.browseAddons(), true,
                            new Object[] { search, add, close }, search,
                            DialogDescriptor.DEFAULT_ALIGN, null, null);
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

        private SearchPanel createSearchPanel( final JButton add ) {
            final SearchPanel panel = new SearchPanel();
            panel.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged( ChangeEvent e ) {
                    myAddOn = panel.getSelected();
                    add.setEnabled(myAddOn != null);
                }
            });
            return panel;
        }

        private JButton createSearchButton( final SearchPanel panel ) {
            JButton search = new JButton(Bundle.search());
            search.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed( ActionEvent e ) {
                    panel.updateTable();
                }
            });
            return search;
        }

        @Override
        public void run() {
            if (myLicense != null) {
                POMUtils.addDependency(myProject, myLicense.getGroupId(),
                        myLicense.getArtifactId(), myLicense.getVersion(),
                        getScope(myAddOn));
            }
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

        private volatile AddOn myAddOn;

        private volatile License myLicense;

        private final Project myProject;
    }

    private static class AddonHandler extends AbstractLicenseChooser {

        @Override
        public License getLicense( AbstractAddOn addon ) {
            return super.getLicense(addon);
        }

    }

    protected static void logUiUsage( String key, String... params ) {
        UIGestureUtils.logUiUsage(VaadinAction.class, UI_LOGGER_NAME, key,
                params);
    }

    private Project myProject;
}
