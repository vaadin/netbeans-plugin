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

import java.util.HashSet;
import java.util.Set;

import org.netbeans.modules.xml.xam.ComponentUpdater;
import org.netbeans.modules.xml.xam.ModelSource;
import org.netbeans.modules.xml.xam.dom.AbstractDocumentModel;
import org.vaadin.netbeans.model.gwt.GwtComponent;
import org.vaadin.netbeans.model.gwt.GwtModel;
import org.vaadin.netbeans.model.gwt.Module;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author denis
 */
public class GwtModelImpl extends AbstractDocumentModel<GwtComponent> implements
        GwtModel
{

    private static final Set<String> GWT_ELEMENT_NAMES = new HashSet<>();

    static {
        GWT_ELEMENT_NAMES.add(ExcludeImpl.EXCLUDE);
        GWT_ELEMENT_NAMES.add(IncludeImpl.INCLUDE);
        GWT_ELEMENT_NAMES.add(InheritsImpl.INHERITS);
        GWT_ELEMENT_NAMES.add(ModuleImpl.MODULE);
        GWT_ELEMENT_NAMES
                .add(SetConfigurationPropertyImpl.SET_CONFIGURATION_PROPERTY);
        GWT_ELEMENT_NAMES.add(SetPropertyImpl.SET_PROPERTY);
        GWT_ELEMENT_NAMES.add(SourceImpl.SOURCE);
        GWT_ELEMENT_NAMES.add(StyleSheetImpl.STYLESHEET);
    }

    public GwtModelImpl( ModelSource source ) {
        super(source);
        myFactory = new GwtComponentFactoryImpl(this);
    }

    @Override
    public GwtComponent createComponent( GwtComponent parent, Element element )
    {
        return getFactory().createComponent(parent, element);
    }

    @Override
    public Module getRootComponent() {
        return myModule;
    }

    @Override
    public GwtComponentFactoryImpl getFactory() {
        return myFactory;
    }

    @Override
    public Module getModule() {
        return getRootComponent();
    }

    @Override
    public GwtComponent createRootComponent( Element element ) {
        ModuleImpl module =
                (ModuleImpl) getFactory().createComponent(null, element);
        if (module != null) {
            myModule = module;
        }
        else {
            return null;
        }
        return getModule();
    }

    @Override
    protected ComponentUpdater<GwtComponent> getComponentUpdater() {
        if (mySyncVisitor == null) {
            mySyncVisitor = new SyncUpdateVisitor();
        }
        return mySyncVisitor;
    }

    @Override
    protected boolean isDomainElement( Node node ) {
        if (!(node instanceof Element)) {
            return false;
        }
        Element element = (Element) node;
        return GWT_ELEMENT_NAMES.contains(element.getLocalName());
    }

    private Module myModule;

    private GwtComponentFactoryImpl myFactory;

    private SyncUpdateVisitor mySyncVisitor;
}
