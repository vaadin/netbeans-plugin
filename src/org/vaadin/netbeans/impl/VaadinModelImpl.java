/**
 *
 */
package org.vaadin.netbeans.impl;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.TypeElement;
import javax.swing.text.BadLocationException;

import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.j2ee.dd.api.common.InitParam;
import org.netbeans.modules.j2ee.dd.api.web.Servlet;
import org.netbeans.modules.j2ee.dd.api.web.WebApp;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.xml.XMLUtil;
import org.vaadin.netbeans.code.generator.JavaUtils;
import org.vaadin.netbeans.code.generator.XmlUtils;
import org.vaadin.netbeans.model.ServletConfiguration;
import org.vaadin.netbeans.model.VaadinModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author denis
 */
class VaadinModelImpl implements VaadinModel {

    private static final String SOURCE = "source";//NOI18N

    private static final String CLIENT = "client"; // NOI18N

    private static final String PATH = "path"; // NOI18N

    private static final Logger LOG = Logger.getLogger(VaadinModelImpl.class
            .getName());

    VaadinModelImpl( Project project, boolean web ) {
        myConfigs = new HashMap<>();
        myProject = project;
        isWeb = web;
    }

    @Override
    public Collection<ServletConfiguration> getServletConfigurations() {
        if (!isWeb) {
            return Collections.emptyList();
        }
        Collection<ServletConfiguration> configs = myConfigs.values();
        int size = configs.size();
        Collection<ServletConfiguration> webXmlConfigs = getWebXmlConfigs();
        if (webXmlConfigs != null) {
            size += webXmlConfigs.size();
        }
        List<ServletConfiguration> result = new ArrayList<>(size);
        if (webXmlConfigs != null) {
            result.addAll(webXmlConfigs);
        }
        result.addAll(configs);
        return result;
    }

    @Override
    public FileObject getGwtXml() {
        if (myGwtXml == null) {
            return XmlUtils.findGwtXml(myProject);
        }
        return myGwtXml;
    }

    @Override
    public boolean isSuperDevModeEnabled() {
        return isSuperDevModeEnabled;
    }

    @Override
    public String getSourcePath() {
        return mySourcePath;
    }

    void initGwtXml() {
        myGwtXml = findGwtXml();
        reparseGwtXml();
    }

    FileObject doGwtGwtXml() {
        return myGwtXml;
    }

    void setGwtXml( FileObject gwtXml ) {
        myGwtXml = gwtXml;
    }

    void add( ElementHandle<TypeElement> handle, ServletConfigurationImpl impl )
    {
        if (myConfigs.get(handle) != null) {
            return;
        }
        myConfigs.put(handle, impl);
        initGwtXml();
    }

    void cleanup( boolean reinit ) {
        myConfigs.clear();
        if (reinit) {
            initGwtXml();
        }
        else {
            myGwtXml = null;
            reparseGwtXml();
        }
    }

    void remove( ElementHandle<TypeElement> handle ) {
        myConfigs.remove(handle);
        initGwtXml();
    }

    FileObject findGwtXml() {
        try {
            Set<FileObject> webWidgetsetFiles = getWebWidgetsetFiles();
            if (webWidgetsetFiles.isEmpty()) {
                LOG.log(Level.INFO,
                        "WEB configuration is not available for the project {0}, "
                                + "proceed with recursive GWT Modules search",
                        myProject.getProjectDirectory());//NOI18N
            }
            else {
                return webWidgetsetFiles.iterator().next();
            }
        }
        catch (IOException e) {
            LOG.log(Level.INFO,
                    "Unable to find widgetsets via WEB configuration, "
                            + "proceed with recursive GWT Modules ", e);//NOI18N
        }
        FileObject fileObject = XmlUtils.findGwtXml(myProject);
        if (fileObject != null && fileObject.isValid()) {
            return fileObject;
        }
        else {
            return null;
        }
    }

    void reparseGwtXml() {
        try {
            doReparseGwtXml();
        }
        finally {
            if (mySourcePath == null) {
                mySourcePath = CLIENT;
            }
        }
    }

    void doReparseGwtXml() {
        mySourcePath = null;
        isSuperDevModeEnabled = false;
        if (myGwtXml != null) {
            try {
                DataObject dataObject = DataObject.find(myGwtXml);
                EditorCookie cookie = dataObject.getLookup().lookup(
                        EditorCookie.class);
                if (cookie == null) {
                    return;
                }

                final BaseDocument baseDocument = (BaseDocument) cookie
                        .openDocument();
                final String[] content = new String[1];
                baseDocument.runAtomic(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            content[0] = baseDocument.getText(0,
                                    baseDocument.getLength());
                        }
                        catch (BadLocationException ignore) {
                        }
                    }
                });
                if (content[0] == null) {
                    return;
                }

                Document document = XMLUtil.parse(new InputSource(
                        new StringReader(content[0])), false, false, null,
                        new XmlUtils.EmptyEntityResolver());
                Element module = document.getDocumentElement();
                NodeList children = module.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node item = children.item(i);
                    if (item instanceof Element
                            && SOURCE.equals(item.getNodeName())
                            && mySourcePath == null)
                    {
                        mySourcePath = ((Element) item).getAttribute(PATH);
                    }
                    if (item instanceof Element
                            && XmlUtils.CONFIG_PROPERTY.equals(item
                                    .getNodeName()) && !isSuperDevModeEnabled)
                    {
                        String name = ((Element) item)
                                .getAttribute(XmlUtils.NAME);
                        if (XmlUtils.SUPER_DEV_PROPERTY.equals(name)) {
                            isSuperDevModeEnabled = Boolean.TRUE.toString()
                                    .equals(((Element) item)
                                            .getAttribute(XmlUtils.VALUE));
                        }
                    }
                }
            }
            catch (SAXException | IOException e) {
                LOG.log(Level.INFO, null, e);
            }
        }
    }

    private List<ServletConfiguration> getWebXmlConfigs() {
        WebApp webApp = null;
        try {
            webApp = XmlUtils.getWebApp(myProject);
        }
        catch (IOException e) {
            LOG.log(Level.INFO, null, e);
        }
        if (webApp == null) {
            return null;
        }
        Servlet[] servlets = webApp.getServlet();
        List<ServletConfiguration> result = new ArrayList<>(servlets.length);
        for (Servlet servlet : servlets) {
            InitParam[] initParams = servlet.getInitParam();
            for (InitParam initParam : initParams) {
                if (JavaUtils.WIDGETSET.equals(initParam.getParamName())
                        && initParam.getParamValue() != null)
                {
                    ServletConfigurationImpl impl = new ServletConfigurationImpl();
                    impl.setWidgetset(initParam.getParamValue());
                    result.add(impl);
                }
            }
        }
        return result;
    }

    private Set<FileObject> getWebWidgetsetFiles() throws IOException {
        List<String> webWidgetset = getWebWidgetset();
        if (webWidgetset.isEmpty()) {
            return Collections.emptySet();
        }

        SourceGroup[] javaSourceGroups = JavaUtils
                .getJavaSourceGroups(myProject);
        SourceGroup[] resourcesSourceGroups = JavaUtils
                .getResourcesSourceGroups(myProject);
        List<SourceGroup> sourceGroups = new ArrayList<>(
                javaSourceGroups.length + resourcesSourceGroups.length);
        sourceGroups.addAll(Arrays.asList(javaSourceGroups));
        sourceGroups.addAll(Arrays.asList(resourcesSourceGroups));

        Set<FileObject> result = new LinkedHashSet<>(webWidgetset.size());
        for (String widgetSet : webWidgetset) {
            widgetSet = widgetSet.replace('.', '/');
            widgetSet += XmlUtils.GWT_XML;
            for (SourceGroup sourceGroup : sourceGroups) {
                FileObject rootFolder = sourceGroup.getRootFolder();
                FileObject fileObject = rootFolder.getFileObject(widgetSet);
                if (fileObject != null) {
                    result.add(fileObject);
                }
            }
        }
        return result;
    }

    private List<String> getWebWidgetset() throws IOException {
        Collection<ServletConfiguration> configs = getServletConfigurations();
        List<String> result = new ArrayList<>(configs.size());
        for (ServletConfiguration configuration : configs) {
            result.add(configuration.getWidgetset());
        }
        return result;
    }

    private final Map<ElementHandle<TypeElement>, ServletConfiguration> myConfigs;

    private FileObject myGwtXml;

    private String mySourcePath;

    private boolean isSuperDevModeEnabled;

    private final Project myProject;

    private final boolean isWeb;

}