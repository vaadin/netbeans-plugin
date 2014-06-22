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
package org.vaadin.netbeans.maven.ui.wizard;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.xml.namespace.QName;

import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.progress.ProgressUtils;
import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.modules.maven.api.archetype.Archetype;
import org.netbeans.modules.maven.api.archetype.ArchetypeProvider;
import org.netbeans.modules.maven.api.archetype.ArchetypeWizards;
import org.netbeans.modules.maven.model.ModelOperation;
import org.netbeans.modules.maven.model.Utilities;
import org.netbeans.modules.maven.model.pom.Build;
import org.netbeans.modules.maven.model.pom.Configuration;
import org.netbeans.modules.maven.model.pom.POMExtensibilityElement;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.model.pom.POMQName;
import org.netbeans.modules.maven.model.pom.Plugin;
import org.netbeans.modules.maven.model.pom.Project;
import org.openide.WizardDescriptor;
import org.openide.WizardDescriptor.AsynchronousInstantiatingIterator;
import org.openide.WizardDescriptor.InstantiatingIterator;
import org.openide.WizardDescriptor.Panel;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.utils.UIGestureUtils;

/**
 * @author denis
 */
@NbBundle.Messages("retrieveLatestVersion=Retrieve Latest Version...")
public class VaadinProjectWizardIterator implements
        AsynchronousInstantiatingIterator
{

    private static final String NAME_SEPARATOR = " - ";// NOI18N

    private static final String RUN_TARGET = "runTarget";// NOI18N

    private static final Logger LOG = Logger
            .getLogger(VaadinProjectWizardIterator.class.getName());

    private static final String APPLICATION_ARTIFACT_ID =
            "vaadin-archetype-application";// NOI18N

    private static final String PORTLET_ARTIFACT_ID =
            "vaadin-archetype-portlet"; // NOI18N

    private static final String ADD_ON_ARTIFACT_ID = "vaadin-archetype-widget";// NOI18N

    private static final String TOUCHKIT_ARTIFACT_ID =
            "vaadin-archetype-touchkit";// NOI18N

    private static final String GROUP_ID = "com.vaadin"; // NOI18N

    private static final String MAVEN_ARTIFACT_ID = "vaadin-maven-plugin";// NOI18N

    private static final String REPOSITORY =
            "http://repo.maven.apache.org/maven2/";// NOI18N

    private static final String APPLICATION_VERSION = "7.0.7";// NOI18N

    private static final String PORTLET_VERSION = "1.0.0";// NOI18N

    public static final int APPLICATION_MIN_VERSION = Integer
            .parseInt(APPLICATION_VERSION.substring(0, 1));

    private static final int PORTLET_MIN_VERSION = Integer
            .parseInt(PORTLET_VERSION.substring(0, 1));

    private static final String JETTY_ARTIFACT_ID = "jetty-maven-plugin";

    private static final String JETTY_GROUP_ID = "org.mortbay.jetty";

    private static final String WEB_APP = "webApp";// NOI18N

    private static final String SCAN_INTERVAL = "scanIntervalSeconds"; // NOI18N

    private static final String CONTEXT_PATH = "contextPath";// NOI18N

    private static final String UTF_8 = "UTF-8";// NOI18N

    private static final String UI_LOGGER_NAME =
            "org.netbeans.ui.vaadin.project"; // NOI18N

    private static final Logger UI_LOG = Logger.getLogger(UI_LOGGER_NAME);

    private VaadinProjectWizardIterator( InstantiatingIterator<?> iterator ) {
        delegate = iterator;
    }

    @StaticResource
    public static final String PROJECT_ICON =
            "org/vaadin/netbeans/maven/ui/resources/vaadin.png"; // NOI18N

    @TemplateRegistration(folder = "Project/Vaadin",
            displayName = "#VaadinNewServletProject",
            description = "../resources/VaadinServletProjectDescription.html",
            iconBase = PROJECT_ICON, position = 100)
    @NbBundle.Messages({
            "VaadinNewServletProject=Vaadin Web Application Project",
            "vaadinNewProjectTitle=Vaadin Web Application" })
    public static WizardDescriptor.InstantiatingIterator<?> newServletProject()
    {
        logUiUsage("UI_LogCreateWebProject"); // NOI18N
        return newProject(APPLICATION_ARTIFACT_ID,
                Bundle.vaadinNewProjectTitle(), LatestStableVaadinVersion
                        .getInstance().getLatestStableVersion());
    }

    /**
     * Portlet archetype is not available for 7.+ Vaadin versions
     * 
     * @TemplateRegistration(folder = "Project/Vaadin", displayName =
     *                              "#VaadinNewPortletProject", description =
     *                              "../resources/VaadinPortletProjectDescription.html"
     *                              , iconBase = PROJECT_ICON, position = 300)
     */
    @NbBundle.Messages({
            "VaadinNewPortletProject=Vaadin Portlet Application Project",
            "vaadinNewPortletTitle=Vaadin Portlet Application" })
    public static WizardDescriptor.InstantiatingIterator<?> newPortletProject()
    {
        logUiUsage("UI_LogCreatePortletProject"); // NOI18N
        return newProject(PORTLET_ARTIFACT_ID, Bundle.vaadinNewPortletTitle());
    }

    @TemplateRegistration(folder = "Project/Vaadin",
            displayName = "#VaadinTouchkitProject",
            description = "../resources/VaadinTouchkitProjectDescription.html",
            iconBase = PROJECT_ICON, position = 400)
    @NbBundle.Messages({ "VaadinTouchkitProject=Vaadin TouchKit Project",
            "vaadinNewTouckitTitle=Vaadin TouchKit Application" })
    public static WizardDescriptor.InstantiatingIterator<?> newTouckitProject()
    {
        logUiUsage("UI_LogCreateTouchkitProject"); // NOI18N
        return newProject(TOUCHKIT_ARTIFACT_ID, Bundle.vaadinNewTouckitTitle());
    }

    @TemplateRegistration(folder = "Project/Vaadin",
            displayName = "#VaadinAddOnProject",
            description = "../resources/VaadinAddOnProjectDescription.html",
            iconBase = PROJECT_ICON, position = 200)
    @NbBundle.Messages({ "VaadinAddOnProject=Vaadin Add-On Project",
            "vaadinNewAddOnTitle=Vaadin Add-On Project with Test Application" })
    public static WizardDescriptor.InstantiatingIterator<?> newAddOnProject() {
        logUiUsage("UI_LogCreateAddOnProject"); // NOI18N
        return newProject(ADD_ON_ARTIFACT_ID, Bundle.vaadinNewAddOnTitle());
    }

    @Override
    public void addChangeListener( ChangeListener listener ) {
        delegate.addChangeListener(listener);
    }

    @Override
    public Panel<?> current() {
        return delegate.current();
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public boolean hasPrevious() {
        return delegate.hasPrevious();
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public void nextPanel() {
        delegate.nextPanel();
    }

    @Override
    public void previousPanel() {
        delegate.previousPanel();
    }

    @Override
    public void removeChangeListener( ChangeListener listener ) {
        delegate.removeChangeListener(listener);
    }

    @Override
    public void initialize( WizardDescriptor descriptor ) {
        wizard = descriptor;
        delegate.initialize(descriptor);
    }

    @Override
    public Set<?> instantiate() throws IOException {
        Set<?> result = delegate.instantiate();
        if (!result.isEmpty()) {
            FileObject warPom = null;
            List<FileObject> poms = new LinkedList<>();
            for (Object project : result) {
                if (project instanceof FileObject) {
                    FileObject pom =
                            ((FileObject) project).getFileObject("pom.xml"); //NOI18N
                    if (pom == null) {
                        continue;
                    }
                    final String[] packaging = new String[1];
                    ModelOperation<POMModel> operation =
                            new ModelOperation<POMModel>() {

                                @Override
                                public void performOperation( POMModel model ) {
                                    Project project = model.getProject();
                                    if (project != null) {
                                        packaging[0] = project.getPackaging();
                                    }
                                }
                            };
                    Utilities.performPOMModelOperations(pom,
                            Collections.singletonList(operation));
                    if ("war".equals(packaging[0])) { //NOI18N
                        warPom = pom;
                    }
                    else {
                        poms.add(pom);
                    }
                }
            }
            if (warPom == null) {
                LOG.warning("Instantiated set doesn't contain WAR project folder");
            }
            else {
                final boolean prefixName = !poms.isEmpty();
                final String name = wizard.getProperty("name").toString();// NOI18N
                ModelOperation<POMModel> operation =
                        new ModelOperation<POMModel>() {

                            @Override
                            public void performOperation( POMModel model ) {
                                Project project = model.getProject();

                                if (prefixName) {
                                    project.setName(name + NAME_SEPARATOR
                                            + project.getName());
                                }
                                else {
                                    project.setName(name);
                                }

                                try {
                                    String uri = URLEncoder.encode(name, UTF_8);
                                    setJettyContextPath(uri, model);
                                    setScanInterval(5, model);
                                    setRunTarget(uri, model);
                                }
                                catch (UnsupportedEncodingException ignore) {
                                    LOG.log(Level.FINE, null, ignore);
                                }
                            }
                        };
                Utilities.performPOMModelOperations(warPom,
                        Collections.singletonList(operation));

                // modify name for other projects
                operation = new ModelOperation<POMModel>() {

                    @Override
                    public void performOperation( POMModel model ) {
                        Project project = model.getProject();
                        project.setName(name + NAME_SEPARATOR
                                + project.getName());
                    }
                };
                for (FileObject pom : poms) {
                    Utilities.performPOMModelOperations(pom,
                            Collections.singletonList(operation));
                }
            }
        }
        else {
            LOG.warning("Instantiated set is empty"); // NOI18N
        }
        return result;
    }

    private void setRunTarget( String name, POMModel model ) {
        Plugin plugin = getVaadinPlugin(model);
        if (plugin == null) {
            return;
        }
        Configuration configuration = plugin.getConfiguration();
        if (configuration == null) {
            configuration = model.getFactory().createConfiguration();
            configuration.addExtensibilityElement(createRunTarget(name, model));
            plugin.setConfiguration(configuration);
        }
        else {
            List<POMExtensibilityElement> children =
                    configuration.getExtensibilityElements();
            for (POMExtensibilityElement child : children) {
                if (RUN_TARGET.equals(child.getQName().getLocalPart())) {
                    String target = child.getElementText();
                    URI uri;
                    try {
                        uri = new URI(target);
                        URL url = uri.toURL();
                        String file = url.getFile();
                        if (file != null) {
                            if (file.length() == 0) {
                                target = target + '/' + name;
                            }
                            else if (file.length() == 1
                                    && file.charAt(0) == '/')
                            {
                                target += name;
                            }
                            child.setElementText(target);
                        }
                    }
                    catch (URISyntaxException | MalformedURLException e) {
                        LOG.log(Level.INFO, null, e);
                    }
                    return;
                }
            }
            configuration.addExtensibilityElement(createRunTarget(name, model));
        }
    }

    private POMExtensibilityElement createRunTarget( String name, POMModel model )
    {
        QName qname =
                POMQName.createQName(RUN_TARGET, model.getPOMQNames()
                        .isNSAware());
        POMExtensibilityElement runTarget =
                model.getFactory().createPOMExtensibilityElement(qname);
        runTarget.setElementText("http://localhost:8080/" + name); // NOI18N
        return runTarget;
    }

    private void setScanInterval( int interval, POMModel model ) {
        Project project = model.getProject();
        Build build = project.getBuild();
        if (build == null) {
            return;
        }
        List<Plugin> plugins = build.getPlugins();
        for (Plugin plugin : plugins) {
            if (JETTY_ARTIFACT_ID.equals(plugin.getArtifactId())
                    && JETTY_GROUP_ID.equals(plugin.getGroupId()))
            {
                Configuration configuration = plugin.getConfiguration();
                assert configuration != null; // configuration exists or has been created previously by the method setJettyContextPath

                List<POMExtensibilityElement> children =
                        configuration.getExtensibilityElements();
                POMExtensibilityElement scanInterval = null;
                for (POMExtensibilityElement component : children) {
                    if (SCAN_INTERVAL.equals(component.getQName()
                            .getLocalPart()))
                    {
                        scanInterval = component;
                        break;
                    }
                }
                if (scanInterval == null) {
                    scanInterval =
                            model.getFactory().createPOMExtensibilityElement(
                                    POMQName.createQName(SCAN_INTERVAL, model
                                            .getPOMQNames().isNSAware()));
                    scanInterval.setElementText(String.valueOf(interval));
                    configuration.addExtensibilityElement(scanInterval);
                    return;
                }
                else {
                    String value = scanInterval.getElementText();
                    if (value == null) {
                        scanInterval.setElementText(String.valueOf(interval));
                    }
                    else {
                        int currentInterval = 0;
                        try {
                            currentInterval = Integer.parseInt(value);
                        }
                        catch (NumberFormatException ignore) {
                        }
                        if (currentInterval == 0) {
                            scanInterval.setElementText(String
                                    .valueOf(interval));
                        }
                    }
                }
            }
        }
    }

    private void setJettyContextPath( String name, POMModel model ) {
        Project project = model.getProject();
        Build build = project.getBuild();
        if (build == null) {
            return;
        }
        List<Plugin> plugins = build.getPlugins();
        for (Plugin plugin : plugins) {
            if (JETTY_ARTIFACT_ID.equals(plugin.getArtifactId())
                    && JETTY_GROUP_ID.equals(plugin.getGroupId()))
            {
                Configuration configuration = plugin.getConfiguration();
                if (configuration == null) {
                    plugin.setConfiguration(createConfiguration(name, model));
                    return;
                }
                List<POMExtensibilityElement> children =
                        configuration.getExtensibilityElements();
                POMExtensibilityElement webApp = null;
                for (POMExtensibilityElement component : children) {
                    if (WEB_APP.equals(component.getQName().getLocalPart())) {
                        webApp = component;
                        break;
                    }
                }
                if (webApp == null) {
                    configuration.addExtensibilityElement(createWebApp(name,
                            model));
                    return;
                }
                children = webApp.getExtensibilityElements();
                POMExtensibilityElement contextPath = null;
                for (POMExtensibilityElement component : children) {
                    if (CONTEXT_PATH
                            .equals(component.getQName().getLocalPart()))
                    {
                        contextPath = component;
                        break;
                    }
                }
                if (contextPath == null) {
                    webApp.addExtensibilityElement(createContextPath(name,
                            model));
                    return;
                }
                String root = "/"; // NOI18N
                if (contextPath.getElementText() == null
                        || contextPath.getElementText().trim().equals(root))
                {
                    String cPath = root + name;
                    contextPath.setElementText(cPath);
                }
            }
        }
    }

    private POMExtensibilityElement createContextPath( String name,
            POMModel model )
    {
        QName qname =
                POMQName.createQName(CONTEXT_PATH, model.getPOMQNames()
                        .isNSAware());
        POMExtensibilityElement contextPath =
                model.getFactory().createPOMExtensibilityElement(qname);
        String cPath = "/";
        try {
            cPath += URLEncoder.encode(name, UTF_8);
        }
        catch (UnsupportedEncodingException ignore) {
        }
        contextPath.setElementText(cPath);
        return contextPath;
    }

    private POMExtensibilityElement createWebApp( String name, POMModel model )
    {
        POMExtensibilityElement contextPath = createContextPath(name, model);
        QName qname =
                POMQName.createQName(WEB_APP, model.getPOMQNames().isNSAware());
        POMExtensibilityElement webApp =
                model.getFactory().createPOMExtensibilityElement(qname);
        webApp.addExtensibilityElement(contextPath);
        return webApp;
    }

    private Configuration createConfiguration( String name, POMModel model ) {
        POMExtensibilityElement webApp = createWebApp(name, model);
        Configuration configuration = model.getFactory().createConfiguration();
        configuration.addExtensibilityElement(webApp);
        return configuration;
    }

    private Plugin getVaadinPlugin( POMModel model ) {
        Project project = model.getProject();
        Build build = project.getBuild();
        if (build == null) {
            return null;
        }
        List<Plugin> plugins = build.getPlugins();
        for (Plugin plugin : plugins) {
            if (MAVEN_ARTIFACT_ID.equals(plugin.getArtifactId())
                    && GROUP_ID.equals(plugin.getGroupId()))
            {
                return plugin;
            }
        }
        return null;
    }

    @Override
    public void uninitialize( WizardDescriptor descriptor ) {
        delegate.uninitialize(descriptor);
    }

    private static WizardDescriptor.InstantiatingIterator<?> newProject(
            final String artifactId, String title )
    {
        return newProject(artifactId, title, null);
    }

    private static WizardDescriptor.InstantiatingIterator<?> newProject(
            final String artifactId, String title, String version )
    {
        String mavenVersion;
        if (version == null) {
            mavenVersion = "LATEST";
        }
        else {
            mavenVersion = version;
        }
        InstantiatingIterator<?> iterator =
                ArchetypeWizards.definedArchetype(GROUP_ID, artifactId,
                        mavenVersion, REPOSITORY, title); //NOI18N
        return new VaadinProjectWizardIterator(iterator);
    }

    /**
     * Code could be used for wizard having additional panel with Vaadin version
     * selection
     */
    private static WizardDescriptor.InstantiatingIterator<?> newProject(
            final String artifactId, final int minVersion, String version,
            String title )
    {
        assert SwingUtilities.isEventDispatchThread();
        AtomicBoolean isCancelled = new AtomicBoolean();
        final Archetype[] archetype = new Archetype[1];
        ProgressUtils.runOffEventDispatchThread(new Runnable() {

            @Override
            public void run() {
                archetype[0] = getLatestArchetype(artifactId, minVersion, null);
            }
        }, Bundle.retrieveLatestVersion(), isCancelled, false);
        String repository = REPOSITORY;
        String useVersion = version;
        if (isCancelled.get()) {
            LOG.log(Level.INFO, "Latest version retrieval is interrupted, "
                    + "using default version: {0}", version); // NOI18N
        }
        else if (archetype[0] == null) {
            LOG.log(Level.WARNING,
                    "Latest version retrieval is not interrupted but version "
                            + "hasn''t been dtermined, using default version: {0}",
                    version); // NOI18N
        }
        if (!isCancelled.get() && archetype[0] != null) {
            useVersion = archetype[0].getVersion();
            repository = archetype[0].getRepository();
        }

        InstantiatingIterator<?> iterator =
                ArchetypeWizards.definedArchetype(GROUP_ID, artifactId,
                        useVersion, repository, title);
        return new VaadinProjectWizardIterator(iterator);
    }

    private static Archetype getLatestArchetype( String artifactId,
            int minVersion, List<String> versions )
    {
        Archetype result = null;
        for (ArchetypeProvider provider : Lookup.getDefault().lookupAll(
                ArchetypeProvider.class))
        {
            final List<Archetype> archetypes = provider.getArchetypes();
            for (Archetype archetype : archetypes) {
                if (GROUP_ID.equals(archetype.getGroupId())
                        && artifactId.equals(archetype.getArtifactId()))
                {
                    String archVersion = archetype.getVersion();
                    String version = archVersion;
                    LOG.log(Level.FINE,
                            "Found archetype with appropriate group id "
                                    + "and artifactId: {0}. It''s version : {1}",
                            new Object[] { artifactId, archVersion });
                    int index = archVersion.indexOf('.');
                    int majorVersion = 0;
                    try {
                        if (index > -1) {
                            version = archVersion.substring(0, index);
                        }
                        majorVersion = Integer.parseInt(version);
                    }
                    catch (NumberFormatException ignore) {
                        LOG.log(Level.WARNING, "Unable to parse version :{0}",
                                version);
                    }
                    if (majorVersion >= minVersion) {
                        if (versions != null) {
                            versions.add(archVersion);
                        }
                        if (result == null) {
                            result = archetype;
                        }
                        else if (version.compareTo(result.getVersion()) > 0) {
                            result = archetype;
                        }
                    }
                    else {
                        LOG.log(Level.FINE,
                                "Found archetype version {0} is skipped. "
                                        + "It''s less than default version {1}",
                                new Object[] { version, minVersion });
                    }
                }
            }
        }
        return result;
    }

    private static void logUiUsage( String key ) {
        UIGestureUtils.logUiUsage(VaadinProjectWizardIterator.class,
                UI_LOGGER_NAME, key);
    }

    private final InstantiatingIterator<?> delegate;

    private WizardDescriptor wizard;
}
