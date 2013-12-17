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
package org.vaadin.netbeans.maven.editor.completion;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openide.xml.XMLUtil;
import org.vaadin.netbeans.maven.editor.completion.SourceClass.SourceType;
import org.vaadin.netbeans.retriever.AbstractRetriever;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author denis
 */
class AddOnParser extends AbstractRetriever {

    private static final String CLASSES = "classes"; // NOI18N

    private static final String QNAME = "qname"; // NOI18N

    private static final String TYPE = "type"; // NOI18N

    private static final String CLASS = "class"; // NOI18N

    static final String NAME = "name"; // NOI18N

    static final String ADDON = "addon"; // NOI18N

    private static final String ADDON_CLASS_INDEX = "netbeans-addon.xml";// NOI18N

    private static final String NETBEANS_ADDON = "resources/"
            + ADDON_CLASS_INDEX; // NOI18N

    AddOnParser() {
        myAddons = new LinkedList<>();

        initCache(AddOnParser.class.getResourceAsStream(NETBEANS_ADDON));
    }

    @Override
    protected String getCachedFileName() {
        return ADDON_CLASS_INDEX;
    }

    @Override
    protected String getUrl() {
        return null;
    }

    @Override
    protected Object getFileLock( File file ) {
        return this;
    }

    void resetCache() {
        // TODO : download recent classes' index when it will be available via REST
    }

    List<AddOn> readAddons() {
        try {
            Document document = null;
            synchronized (getFileLock(getCachedFile())) {
                String content = readFile(getCachedFile());
                document =
                        XMLUtil.parse(
                                new InputSource(new StringReader(content)),
                                false, false, null, null);
            }
            collectAddons(document.getDocumentElement());
        }
        catch (IOException | SAXException e) {
            Logger.getLogger(AddOnParser.class.getName()).log(Level.INFO, null,
                    e);
        }

        return Collections.unmodifiableList(myAddons);
    }

    private void collectAddons( Element addonElement ) {
        NodeList children = addonElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Element element = toElement(children.item(i), ADDON);
            if (element != null) {
                parseAddon(element);
            }
        }
    }

    private void parseAddon( Element element ) {
        List<SourceClass> classes = collectClasses(element);

        myAddons.add(new AddOn(getText(getElement(element, NAME)), classes));
    }

    private List<SourceClass> collectClasses( Element element ) {
        Element classesElement = getElement(element, CLASSES);
        if (classesElement == null) {
            return Collections.emptyList();
        }
        NodeList children = classesElement.getChildNodes();
        List<SourceClass> list = new ArrayList<>(children.getLength());
        for (int i = 0; i < children.getLength(); i++) {
            Element clazz = toElement(children.item(i), CLASS);
            if (clazz != null) {
                list.add(parseClass(clazz));
            }
        }
        return list;
    }

    private SourceClass parseClass( Element element ) {
        String type = element.getAttribute(TYPE);
        Element qname = getElement(element, QNAME);
        Element name = getElement(element, NAME);
        return new SourceClass(parseType(type), getText(name), getText(qname));
    }

    private SourceType parseType( String type ) {
        for (SourceType sourceType : SourceType.values()) {
            if (sourceType.toString().toLowerCase().equals(type)) {
                return sourceType;
            }
        }
        return null;
    }

    private Element getElement( Element parent, String name ) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Element element = toElement(children.item(i), name);
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    private String getText( Element element ) {
        if (element == null) {
            return null;
        }
        NodeList children = element.getChildNodes();
        StringBuilder builder = new StringBuilder();
        boolean hasText = false;
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                hasText = true;
                builder.append(child.getNodeValue());
            }
        }
        if (hasText) {
            return builder.toString().trim();
        }
        return null;
    }

    private Element toElement( Node node, String expectedName ) {
        if (node instanceof Element) {
            if (expectedName.equals(node.getNodeName())) {
                return (Element) node;
            }
        }
        return null;
    }

    private List<AddOn> myAddons;

}
