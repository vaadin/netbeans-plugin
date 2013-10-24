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
package org.vaadin.netbeans.model.gwt.impl;

import java.util.List;

import org.netbeans.modules.xml.xam.dom.AbstractDocumentComponent;
import org.netbeans.modules.xml.xam.dom.Attribute;
import org.vaadin.netbeans.model.gwt.GwtComponent;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author denis
 */
abstract class GwtComponentImpl extends AbstractDocumentComponent<GwtComponent>
        implements GwtComponent
{

    GwtComponentImpl( GwtModelImpl model, Element e ) {
        super(model, e);
    }

    @Override
    public GwtModelImpl getModel() {
        return (GwtModelImpl) super.getModel();
    }

    @Override
    protected Object getAttributeValueOf( Attribute attribute, String value ) {
        return value;
    }

    @Override
    protected void populateChildren( List<GwtComponent> components ) {
        NodeList nodes = getPeer().getChildNodes();
        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node instanceof Element) {
                    GwtComponent comp =
                            (GwtComponent) getModel().getFactory()
                                    .createComponent(this, (Element) node);
                    if (comp != null) {
                        components.add(comp);
                    }
                }
            }
        }
    }

    protected void setAttribute( Attribute attribute, String value ) {
        setAttribute(attribute.getName(), attribute, value);
    }

    protected static Element createNewElement( String name, GwtModelImpl model )
    {
        return model.getDocument().createElement(name);
    }

}
