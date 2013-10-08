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
package org.vaadin.netbeans.code.generator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.BadLocationException;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.queries.FileEncodingQuery;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.editor.indent.api.Indent;
import org.netbeans.modules.j2ee.dd.api.common.InitParam;
import org.netbeans.modules.j2ee.dd.api.web.DDProvider;
import org.netbeans.modules.j2ee.dd.api.web.Servlet;
import org.netbeans.modules.j2ee.dd.api.web.WebApp;
import org.netbeans.modules.web.api.webmodule.WebModule;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle;
import org.openide.xml.XMLUtil;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;

/**
 * @author denis
 */
public final class XmlUtils {

    private static final String COMMENT_START = "<!--";// NOI18N

    private static final String COMMENT_END = "-->";// NOI18N

    private static final String CLOSING_POSTFIX = "/>";// NOI18N

    private static final String CLOSING_TAG_PREFIX = "</";// NOI18N

    private static final String LEXICAL_HANDLER = "http://xml.org/sax/properties/lexical-handler";// NOI18N

    public static final String GWT = ".gwt";// NOI18N

    public static final String GWT_XML = GWT + ".xml";// NOI18N

    private static final String MODULE_TEMPLATE = "Templates/Vaadin/WidgetSet.gwt.xml"; // NOI18N

    private static final String MODULE_PREFIX = "App";// NOI18N

    public static final String SUPER_DEV_PROPERTY = "devModeRedirectEnabled"; // NOI18N

    public static final String CONFIG_PROPERTY = "set-configuration-property"; // NOI18N

    private static final String DEV_MODE_REDIRECT_TAG = "\t<set-configuration-property "
            + "name=\"devModeRedirectEnabled\" value=\"true\" />\n";//NOI18N

    public static final String NAME = "name"; // NOI18N

    public static final String VALUE = "value";// NOI18N

    private static final Logger LOG = Logger
            .getLogger(XmlUtils.class.getName());

    private XmlUtils() {
    }

    public static FileObject findGwtXml( Project project ) {
        SourceGroup[] sourceGroups = JavaUtils.getJavaSourceGroups(project);
        FileObject gwtXml = findGwtXml(sourceGroups);
        if (gwtXml == null) {
            sourceGroups = JavaUtils.getResourcesSourceGroups(project);
            gwtXml = findGwtXml(sourceGroups);
        }
        return gwtXml;

    }

    public static FileObject createGwtXml( FileObject folder )
            throws IOException
    {
        FileObject templateFileObject = FileUtil.getConfigFile(MODULE_TEMPLATE);
        String name = templateFileObject.getNameExt();
        if (name.endsWith(GWT_XML)) {
            name = name.substring(0, name.length() - GWT_XML.length());
        }
        String moduleName = JavaUtils.getFreeName(folder, MODULE_PREFIX + name,
                GWT_XML) + GWT;
        return createGwtXml(folder, moduleName);
    }

    public static FileObject createGwtXml( FileObject folder, String name )
            throws IOException
    {
        DataObject dataObject = JavaUtils.createDataObjectFromTemplate(
                MODULE_TEMPLATE, folder, name, null);
        return dataObject.getPrimaryFile();
    }

    public static WebApp getWebApp( Project project ) throws IOException {
        FileObject fileObject = getWebXml(project);
        if (fileObject != null) {
            return DDProvider.getDefault().getDDRoot(fileObject);
        }
        return null;
    }

    public static FileObject getWebXml( Project project ) throws IOException {
        WebModule webModule = WebModule.getWebModule(project
                .getProjectDirectory());
        if (webModule != null) {
            return webModule.getDeploymentDescriptor();
        }
        return null;
    }

    public static FileObject getClientWidgetPackage( FileObject gwtXml,
            String srcPath, boolean create ) throws IOException
    {
        if (srcPath == null) {
            return null;
        }
        Project project = FileOwnerQuery.getOwner(gwtXml);
        SourceGroup[] sourceGroups = JavaUtils
                .getResourcesSourceGroups(project);
        FileObject srcRoot = null;
        for (SourceGroup sourceGroup : sourceGroups) {
            FileObject root = sourceGroup.getRootFolder();
            if (FileUtil.isParentOf(root, gwtXml)) {
                srcRoot = root;
                break;
            }
        }
        String relativePath = null;
        if (srcRoot != null) {
            relativePath = FileUtil.getRelativePath(srcRoot, gwtXml);
            relativePath = removeTrailingName(relativePath, gwtXml);
        }
        sourceGroups = JavaUtils.getJavaSourceGroups(project);
        FileObject targetFolder = null;
        for (SourceGroup sourceGroup : sourceGroups) {
            FileObject root = sourceGroup.getRootFolder();
            if (relativePath != null) {
                FileObject pkg = root.getFileObject(relativePath);
                if (pkg != null) {
                    targetFolder = pkg;
                    break;
                }
            }
            else {
                if (FileUtil.isParentOf(root, gwtXml)) {
                    targetFolder = gwtXml.getParent();
                }
            }
        }
        if (targetFolder == null) {
            Logger.getLogger(XmlUtils.class.getName()).info(
                    "Unable to find Java package (in src roots) "
                            + "for GWT module file");
            if (create && sourceGroups.length > 0 && relativePath != null) {
                Logger.getLogger(XmlUtils.class.getName()).info(
                        "Create package in java source "
                                + "root for GWT module file");
                FileObject root = sourceGroups[0].getRootFolder();
                targetFolder = FileUtil.createFolder(root, relativePath);
            }
        }
        if (targetFolder == null) {
            return null;
        }
        FileObject clientPkg = targetFolder.getFileObject(srcPath);
        if (clientPkg == null && create) {
            clientPkg = FileUtil.createFolder(targetFolder, srcPath);
        }
        return clientPkg;
    }

    public static void enableSuperDevMode( final FileObject gwtXml ) {
        try {
            DataObject dataObject = DataObject.find(gwtXml);
            EditorCookie cookie = dataObject.getLookup().lookup(
                    EditorCookie.class);
            if (cookie == null) {
                return;
            }
            final BaseDocument document = (BaseDocument) cookie.openDocument();
            document.runAtomic(new Runnable() {

                @Override
                public void run() {
                    try {
                        String content = document.getText(0,
                                document.getLength());
                        DefaultHandlerImpl handler = parseGwtXml(content);

                        if (handler.getRootTag() == null || handler.isEnabled())
                        {
                            return;
                        }

                        boolean updated = false;
                        if (handler.getLine() != -1) {
                            updated = updateSuperDevModeContent(document,
                                    handler);
                        }
                        else {
                            updated = createSuperDevModeContent(document,
                                    handler);
                        }

                        if (!updated) {
                            int index = document.getText(0,
                                    document.getLength()).lastIndexOf(
                                    CLOSING_TAG_PREFIX + handler.getRootTag());
                            if (index == -1) {
                                return;
                            }

                            document.insertString(index, DEV_MODE_REDIRECT_TAG,
                                    null);
                            Indent indent = Indent.get(document);
                            indent.lock();
                            try {
                                indent.reindent(index, index
                                        + DEV_MODE_REDIRECT_TAG.length());
                            }
                            finally {
                                indent.unlock();
                            }
                        }
                    }
                    catch (BadLocationException | SAXException | IOException e)
                    {
                        LOG.log(Level.INFO, null, e);
                    }
                }
            });
            cookie.saveDocument();
        }
        catch (FileNotFoundException e) {
            LOG.log(Level.INFO, null, e);
        }
        catch (IOException e) {
            LOG.log(Level.INFO, null, e);
        }
    }

    public static String readFile( FileObject file ) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = file.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            FileUtil.copy(inputStream, outputStream);

            Charset charset = FileEncodingQuery.getEncoding(file);
            return outputStream.toString(charset.name());
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException ignore) {
                }
            }
        }
    }

    static void waitGwtXml( final VaadinSupport support,
            final List<String> srcPath )
    {
        try {
            final boolean[] gwtXmlCreated = new boolean[1];
            support.runModelOperation(new ModelOperation() {

                @Override
                public void run( VaadinModel model ) {
                    if (model.getGwtXml() != null) {
                        gwtXmlCreated[0] = true;
                        srcPath.add(model.getSourcePaths().get(0));
                    }
                }
            });
            if (!gwtXmlCreated[0]) {
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException ignore) {
                }
                waitGwtXml(support, srcPath);
            }
        }
        catch (IOException e) {
            LOG.log(Level.INFO, null, e);
        }
    }

    @NbBundle.Messages("serverSideInClient=Selected package should be used only for client side classes")
    static boolean checkServerPackage( FileObject serverPkg,
            List<String> srcPaths, FileObject gwtXml ) throws IOException
    {
        for (String srcPath : srcPaths) {
            FileObject clientPkg = XmlUtils.getClientWidgetPackage(gwtXml,
                    srcPath, false);
            if (clientPkg != null) {
                if (serverPkg.equals(clientPkg)
                        || FileUtil.isParentOf(clientPkg, serverPkg))
                {
                    NotifyDescriptor descriptor = new NotifyDescriptor.Message(
                            Bundle.serverSideInClient(),
                            NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notify(descriptor);
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean updateSuperDevModeContent( BaseDocument document,
            DefaultHandlerImpl handler ) throws BadLocationException
    {
        int offset = Utilities.getRowStartFromLineOffset(document,
                handler.getLine() - 1)
                + handler.getColumn();

        String tag = document.getText(0, offset);
        int index = tag.lastIndexOf('<' + CONFIG_PROPERTY);
        if (index == -1) {
            return false;
        }
        document.remove(index, offset - index);
        String newTag = DEV_MODE_REDIRECT_TAG.trim();
        document.insertString(index, tag, null);
        Indent indent = Indent.get(document);
        indent.lock();
        try {
            indent.reindent(index, index + newTag.length());
        }
        finally {
            indent.unlock();
        }
        return true;
    }

    private static boolean createSuperDevModeContent( BaseDocument document,
            DefaultHandlerImpl handler ) throws BadLocationException
    {
        if (handler.getComment() == null) {
            return false;
        }
        String text = document.getText(0, document.getLength());
        int index = text.indexOf(handler.getComment());
        if (index == -1) {
            return false;
        }
        else {
            int afterComment = text.indexOf(COMMENT_END, index);
            if (afterComment == -1) {
                return false;
            }
            else {
                String tag = text.substring(0, index);
                index = tag.lastIndexOf(COMMENT_START);
                if (index == -1) {
                    return false;
                }
                else {
                    document.remove(index, afterComment + COMMENT_END.length()
                            - index);
                    document.insertString(index, handler.getComment(), null);
                    Indent indent = Indent.get(document);
                    indent.lock();
                    try {
                        indent.reindent(index, index
                                + handler.getComment().length());
                    }
                    finally {
                        indent.unlock();
                    }
                }
            }
        }
        return true;
    }

    private static DefaultHandlerImpl parseGwtXml( String xmlContent )
            throws IOException, SAXException
    {
        DefaultHandlerImpl handler = new DefaultHandlerImpl();

        XMLReader reader = XMLUtil.createXMLReader();
        reader.setProperty(LEXICAL_HANDLER, handler);
        reader.setContentHandler(handler);
        reader.setEntityResolver(new EmptyEntityResolver());

        reader.parse(new InputSource(new StringReader(xmlContent)));
        return handler;
    }

    private static String removeTrailingName( String path, FileObject fileObject )
    {
        if (path.indexOf(fileObject.getNameExt()) > 0) {
            String relativePath = path.substring(0, path.length()
                    - fileObject.getNameExt().length());
            if (relativePath.endsWith(File.separator)) {
                return relativePath.substring(0, relativePath.length() - 1);
            }
            else {
                return relativePath;
            }
        }
        return path;
    }

    private static FileObject findGwtXml( SourceGroup[] sourceGroups ) {
        for (SourceGroup sourceGroup : sourceGroups) {
            FileObject root = sourceGroup.getRootFolder();
            FileObject gwtXml = findGwtXml(root);
            if (gwtXml != null) {
                return gwtXml;
            }
        }
        return null;
    }

    private static FileObject findGwtXml( FileObject folder ) {
        if (folder.isFolder()) {
            FileObject[] children = folder.getChildren();
            for (FileObject fileObject : children) {
                String nameExt = fileObject.getNameExt();
                if (nameExt.endsWith(GWT_XML)) {
                    return fileObject;
                }
                else {
                    FileObject gwtXml = findGwtXml(fileObject);
                    if (gwtXml != null) {
                        return gwtXml;
                    }
                }
            }
        }
        return null;
    }

    private static List<String> getInitParam( WebApp webApp, String paramName )
    {
        if (webApp == null) {
            return Collections.emptyList();
        }
        Servlet[] servlets = webApp.getServlet();
        List<String> result = new LinkedList<>();
        for (Servlet servlet : servlets) {
            InitParam[] initParams = servlet.getInitParam();
            for (InitParam initParam : initParams) {
                if (paramName.equals(initParam.getParamName())) {
                    result.add(initParam.getParamValue());
                }
            }
        }
        return result;
    }

    private static final class DefaultHandlerImpl extends DefaultHandler2 {

        @Override
        public void setDocumentLocator( Locator locator ) {
            myLocator = locator;
        }

        @Override
        public void startElement( String uri, String localName, String qName,
                Attributes attributes ) throws SAXException
        {
            if (root == null) {
                root = qName;
            }

            if (CONFIG_PROPERTY.equals(qName) && myLine == -1) {
                if (SUPER_DEV_PROPERTY.equals(attributes.getValue(NAME))
                        && myLine == -1)
                {
                    if (Boolean.FALSE.toString().equals(
                            attributes.getValue(VALUE)))
                    {
                        myLine = myLocator.getLineNumber();
                        myColumn = myLocator.getColumnNumber();
                    }
                    else {
                        isEnabled = true;
                    }
                }
            }
        }

        @Override
        public void comment( char[] chars, int start, int length )
                throws SAXException
        {
            if (superDevModeComment != null) {
                return;
            }
            StringBuilder builder = new StringBuilder();
            for (int i = start; i < length; i++) {
                builder.append(chars[i]);
            }
            String comment = builder.toString().trim();
            if (comment.contains(SUPER_DEV_PROPERTY)
                    && comment.startsWith('<' + CONFIG_PROPERTY)
                    && comment.endsWith(CLOSING_POSTFIX))
            {
                superDevModeComment = comment;
            }
        }

        String getRootTag() {
            return root;
        }

        String getComment() {
            return superDevModeComment;
        }

        int getLine() {
            return myLine;
        }

        int getColumn() {
            return myColumn;
        }

        boolean isEnabled() {
            return isEnabled;
        }

        private String root;

        private String superDevModeComment;

        private Locator myLocator;

        private int myLine = -1;

        private int myColumn;

        private boolean isEnabled;
    }

    public static class EmptyEntityResolver implements EntityResolver {

        @Override
        public InputSource resolveEntity( String publicID, String systemID )
                throws SAXException
        {
            return new InputSource(new StringReader(""));
        }
    }

}
