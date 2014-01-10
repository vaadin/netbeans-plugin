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
package org.vaadin.netbeans.ui.wizard;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.event.ChangeListener;

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.spi.java.project.support.ui.templates.JavaTemplates;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.WizardDescriptor.Panel;
import org.openide.WizardDescriptor.ProgressInstantiatingIterator;
import org.openide.filesystems.FileObject;
import org.vaadin.netbeans.code.generator.ConnectorOnlyWidgetGenerator;
import org.vaadin.netbeans.code.generator.ExtensionWidgetGenerator;
import org.vaadin.netbeans.code.generator.FullFledgedWidgetGenerator;
import org.vaadin.netbeans.code.generator.WidgetGenerator;
import org.vaadin.netbeans.ui.wizard.WidgetTypePanel.Template;
import org.vaadin.netbeans.utils.JavaUtils;

/**
 * @author denis
 */
public class NewWidgetWizardIterator implements
        ProgressInstantiatingIterator<WizardDescriptor>
{

    static final String SELECTED_TEMPLATE = "selectedTemplate"; // NOI18N

    private static final Map<Template, WidgetGenerator> TEMPLATE_GENERATORS =
            new EnumMap<>(WidgetTypePanel.Template.class);

    static {
        TEMPLATE_GENERATORS.put(Template.FULL_FLEDGED,
                new FullFledgedWidgetGenerator());
        TEMPLATE_GENERATORS.put(Template.CONNECTOR_ONLY,
                new ConnectorOnlyWidgetGenerator());
        TEMPLATE_GENERATORS.put(Template.EXTENSION,
                new ExtensionWidgetGenerator());
    }

    @Override
    public Set<?> instantiate() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void initialize( WizardDescriptor wizard ) {
        myWizard = wizard;
        Project project = Templates.getProject(wizard);

        SourceGroup[] sourceGroups = JavaUtils.getJavaSourceGroups(project);
        myWidgetPanel = new WidgetTypePanel();
        WizardDescriptor.Panel<WizardDescriptor> firstPanel =
                new FinishableWizardPanel(JavaTemplates.createPackageChooser(
                        project, sourceGroups, myWidgetPanel, true),
                        myWidgetPanel);
        mySelectPanel = new ComponentSelectPanel(wizard);
        myPanels = new WizardDescriptor.Panel[] { firstPanel, mySelectPanel };
        setSteps();
    }

    @Override
    public void uninitialize( WizardDescriptor wizard ) {
        myPanels = null;
        myWidgetPanel = null;
    }

    @Override
    public void addChangeListener( ChangeListener listener ) {
    }

    @Override
    public Panel<WizardDescriptor> current() {
        return myPanels[myIndex];
    }

    @Override
    public boolean hasNext() {
        return myIndex < myPanels.length - 1;
    }

    @Override
    public boolean hasPrevious() {
        return myIndex > 0;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public void nextPanel() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        myIndex++;
    }

    @Override
    public void previousPanel() {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        }
        myIndex--;
    }

    @Override
    public void removeChangeListener( ChangeListener listener ) {
    }

    @Override
    public Set<?> instantiate( ProgressHandle handle ) throws IOException {
        handle.start();

        Template template = myWidgetPanel.getSelectedTemplate();
        WidgetGenerator generator = TEMPLATE_GENERATORS.get(template);
        Set<FileObject> files = generator.generate(myWizard, handle);

        handle.finish();
        return files;
    }

    private void setSteps() {
        Object contentData =
                myWizard.getProperty(WizardDescriptor.PROP_CONTENT_DATA);
        if (contentData instanceof String[]) {
            String existingSteps[] = (String[]) contentData;
            int existingCount = 1;
            String steps[] = new String[myPanels.length + existingCount];
            System.arraycopy(existingSteps, 0, steps, 0, existingCount);
            for (int i = 0; i < myPanels.length; i++) {
                steps[existingCount + i] = myPanels[i].getComponent().getName();
                Panel<?> panel = myPanels[i];
                JComponent component = (JComponent) panel.getComponent();
                component.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA,
                        steps);
                component.putClientProperty(
                        WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i);
                component.putClientProperty(
                        WizardDescriptor.PROP_AUTO_WIZARD_STYLE, true);
                component.putClientProperty(
                        WizardDescriptor.PROP_CONTENT_DISPLAYED, true);
                component.putClientProperty(
                        WizardDescriptor.PROP_CONTENT_NUMBERED, true);
            }
        }
    }

    private WizardDescriptor myWizard;

    private WizardDescriptor.Panel<WizardDescriptor>[] myPanels;

    private WidgetTypePanel myWidgetPanel;

    private ComponentSelectPanel mySelectPanel;

    private int myIndex;

}
