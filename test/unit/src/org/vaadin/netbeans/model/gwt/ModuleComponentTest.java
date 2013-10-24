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
package org.vaadin.netbeans.model.gwt;

import java.io.IOException;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.xml.xam.ModelSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.StringReader;
import java.util.List;
import javax.swing.text.BadLocationException;

import javax.swing.text.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * @author denis
 */
public class ModuleComponentTest extends NbTestCase {

    public ModuleComponentTest( String name ) {
        super(name);
    }

    public void testEmpty() throws Exception {
        GwtModel model = Util.loadRegistryModel("empty-module.xml");
        model.sync();

        Module module = model.getModule();
        List<ModuleComponent> elements = module.getComponents();
        assertEquals(0, elements.size());
        assertEquals(0, module.getChildren().size());
    }

    public void testModule() throws Exception {
        GwtModel model = Util.loadRegistryModel("module.xml");
        model.sync();

        Module module = model.getModule();
        List<ModuleComponent> elements = module.getComponents();
        assertEquals(6, elements.size());
        assertEquals(6, module.getChildren().size());

        List<ModuleComponent> components = module.getComponents();
        ModuleComponent component = components.get(0);
        assertTrue(component instanceof Inherits);
        assertEquals(Inherits.class, component.getComponentType());

        assertEquals("com.vaadin.DefaultWidgetSet",
                ((Inherits) component).getName());

        component = components.get(1);
        assertTrue(component instanceof Source);
        assertEquals(Source.class, component.getComponentType());
        Source source = (Source) component;
        assertEquals("a", source.getPath());
        assertEquals("excl", source.getExcludes());
        assertEquals("incl", source.getIncludes());
        assertFalse(source.isDefaultExcludes());
        assertFalse(source.isCasesensitive());
        assertEquals(0, source.getChildren().size());

        component = components.get(2);
        assertTrue(component instanceof Source);
        assertEquals(Source.class, component.getComponentType());
        source = (Source) component;
        assertEquals("b", source.getPath());
        List<FilterComponent> filters = source.getFilters();
        assertEquals(2, filters.size());

        FilterComponent filter = filters.get(0);
        assertTrue(filter instanceof Include);
        assertEquals(Include.class, filter.getComponentType());
        assertEquals("incl", ((Include) filter).getName());

        filter = filters.get(1);
        assertTrue(filter instanceof Exclude);
        assertEquals(Exclude.class, filter.getComponentType());
        assertEquals("excl", ((Exclude) filter).getName());

        component = components.get(3);
        assertTrue(component instanceof SetConfigurationProperty);
        assertEquals(SetConfigurationProperty.class,
                component.getComponentType());
        SetConfigurationProperty configurationProperty = (SetConfigurationProperty) component;
        assertEquals("devModeRedirectEnabled", configurationProperty.getName());
        assertEquals("true", configurationProperty.getValue());

        component = components.get(4);
        assertTrue(component instanceof SetProperty);
        assertEquals(SetProperty.class, component.getComponentType());
        SetProperty setProperty = (SetProperty) component;
        assertEquals("user.agent", setProperty.getName());
        assertEquals("safari", setProperty.getValue());

        component = components.get(5);
        assertTrue(component instanceof StyleSheet);
        assertEquals(StyleSheet.class, component.getComponentType());
        assertEquals("styles.css", ((StyleSheet) component).getSrc());
    }

    public void testAddSource() throws Exception {
        GwtModel model = Util.loadFreshDomainModel("module.xml");
        model.sync();

        Module module = model.getModule();
        Source source = model.getFactory().createSource();
        source.setPath("newPath");
        model.startTransaction();
        module.addComponent(source);
        model.endTransaction();

        Element lastElement = getLastElement(model);
        assertEquals("source", lastElement.getTagName());
        assertEquals("newPath", lastElement.getAttribute("path"));
    }

    public void testAddInherits() throws Exception {
        GwtModel model = Util.loadFreshDomainModel("module.xml");
        model.sync();

        Module module = model.getModule();
        Inherits inherits = model.getFactory().createInherits();
        inherits.setName("newName");
        model.startTransaction();
        module.addComponent(inherits);
        model.endTransaction();

        Element lastElement = getLastElement(model);
        assertEquals("inherits", lastElement.getTagName());
        assertEquals("newName", lastElement.getAttribute("name"));
    }

    public void testAddSetConfigurationProperty() throws Exception {
        GwtModel model = Util.loadFreshDomainModel("module.xml");
        model.sync();

        Module module = model.getModule();
        SetConfigurationProperty setProperty = model.getFactory()
                .createSetConfigurationProperty();
        setProperty.setName("newName");
        setProperty.setValue("newValue");
        model.startTransaction();
        module.addComponent(setProperty);
        model.endTransaction();

        Element lastElement = getLastElement(model);
        assertEquals("set-configuration-property", lastElement.getTagName());
        assertEquals("newName", lastElement.getAttribute("name"));
        assertEquals("newValue", lastElement.getAttribute("value"));
    }

    public void testAddSetProperty() throws Exception {
        GwtModel model = Util.loadFreshDomainModel("module.xml");
        model.sync();

        Module module = model.getModule();
        SetProperty setProperty = model.getFactory().createSetProperty();
        setProperty.setName("newName");
        setProperty.setValue("newValue");
        model.startTransaction();
        module.addComponent(setProperty);
        model.endTransaction();

        Element lastElement = getLastElement(model);
        assertEquals("set-property", lastElement.getTagName());
        assertEquals("newName", lastElement.getAttribute("name"));
        assertEquals("newValue", lastElement.getAttribute("value"));
    }

    public void testAddStylesheet() throws Exception {
        GwtModel model = Util.loadFreshDomainModel("module.xml");
        model.sync();

        Module module = model.getModule();
        StyleSheet stylesheet = model.getFactory().createStyleSheet();
        stylesheet.setSrc("newSrc");
        model.startTransaction();
        module.addComponent(stylesheet);
        model.endTransaction();

        Element lastElement = getLastElement(model);
        assertEquals("stylesheet", lastElement.getTagName());
        assertEquals("newSrc", lastElement.getAttribute("src"));
    }

    public void testAddInclude() throws Exception {
        GwtModel model = Util.loadFreshDomainModel("module.xml");
        model.sync();

        Module module = model.getModule();
        Source source = (Source) module.getComponents().get(1);
        Include include = model.getFactory().createInclude();
        include.setName("newName");
        model.startTransaction();
        source.addFilter(include);
        model.endTransaction();

        Element sourceElement = getSourceElement(model);
        Element filterChild = null;
        int count = 0;
        NodeList nodes = sourceElement.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                count++;
                filterChild = (Element) node;
            }
        }
        assertEquals(count, 1);
        assertEquals("include", filterChild.getTagName());
        assertEquals("newName", filterChild.getAttribute("name"));
    }

    public void testAddExclude() throws Exception {
        GwtModel model = Util.loadFreshDomainModel("module.xml");
        model.sync();

        Module module = model.getModule();
        Source source = (Source) module.getComponents().get(1);
        Exclude exclude = model.getFactory().createExclude();
        exclude.setName("newName");
        model.startTransaction();
        source.addFilter(exclude);
        model.endTransaction();

        Element sourceElement = getSourceElement(model);
        Element filterChild = null;
        int count = 0;
        NodeList nodes = sourceElement.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                count++;
                filterChild = (Element) node;
            }
        }
        assertEquals(count, 1);
        assertEquals("exclude", filterChild.getTagName());
        assertEquals("newName", filterChild.getAttribute("name"));
    }

    private Element getSourceElement( GwtModel model )
            throws ParserConfigurationException, SAXException, IOException,
            BadLocationException
    {
        Element root = getRoot(model);
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                if ("source".equals(element.getTagName())) {
                    return element;
                }
            }
        }
        return null;
    }

    private Element getLastElement( GwtModel model )
            throws ParserConfigurationException, SAXException, IOException,
            BadLocationException
    {
        Element moduleElement = getRoot(model);
        NodeList nodes = moduleElement.getChildNodes();
        Element lastElement = null;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node item = nodes.item(i);
            if (item instanceof Element) {
                lastElement = (Element) item;
            }
        }
        return lastElement;
    }

    private Element getRoot( GwtModel model )
            throws ParserConfigurationException, SAXException, IOException,
            BadLocationException
    {
        Document document = model.getModelSource().getLookup()
                .lookup(Document.class);
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();
        org.w3c.dom.Document xmlDocument = documentBuilder
                .parse(new InputSource(new StringReader(document.getText(0,
                        document.getLength()))));
        return xmlDocument.getDocumentElement();
    }
}
