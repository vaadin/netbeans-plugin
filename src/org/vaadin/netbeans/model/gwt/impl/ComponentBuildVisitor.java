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

import org.vaadin.netbeans.model.gwt.Exclude;
import org.vaadin.netbeans.model.gwt.GwtComponent;
import org.vaadin.netbeans.model.gwt.GwtComponentVisitor;
import org.vaadin.netbeans.model.gwt.Include;
import org.vaadin.netbeans.model.gwt.Inherits;
import org.vaadin.netbeans.model.gwt.Module;
import org.vaadin.netbeans.model.gwt.SetConfigurationProperty;
import org.vaadin.netbeans.model.gwt.SetProperty;
import org.vaadin.netbeans.model.gwt.Source;
import org.vaadin.netbeans.model.gwt.StyleSheet;
import org.w3c.dom.Element;

/**
 * @author denis
 */
class ComponentBuildVisitor implements GwtComponentVisitor {

    ComponentBuildVisitor( GwtModelImpl model, GwtComponent parent,
            Element element )
    {
        myModel = model;
        myElement = element;

        if (parent == null) {
            if (ModuleImpl.MODULE.equals(element.getLocalName())) {
                myResult = new ModuleImpl(myModel, element);
            }
        }
        else {
            parent.accept(this);
        }
    }

    @Override
    public void visit( Exclude exclude ) {
    }

    @Override
    public void visit( Include include ) {
    }

    @Override
    public void visit( Inherits inherits ) {
    }

    @Override
    public void visit( Module module ) {
        if (isAcceptable(SourceImpl.SOURCE)) {
            myResult = new SourceImpl(myModel, myElement);
        }
        else if (isAcceptable(InheritsImpl.INHERITS)) {
            myResult = new InheritsImpl(myModel, myElement);
        }
        else if (isAcceptable(SetConfigurationPropertyImpl.SET_CONFIGURATION_PROPERTY))
        {
            myResult = new SetConfigurationPropertyImpl(myModel, myElement);
        }
        else if (isAcceptable(SetPropertyImpl.SET_PROPERTY)) {
            myResult = new SetPropertyImpl(myModel, myElement);
        }
        else if (isAcceptable(StyleSheetImpl.STYLESHEET)) {
            myResult = new StyleSheetImpl(myModel, myElement);
        }
    }

    @Override
    public void visit( SetConfigurationProperty setConfiguration ) {
    }

    @Override
    public void visit( SetProperty setProperty ) {
    }

    @Override
    public void visit( Source source ) {
        if (isAcceptable(IncludeImpl.INCLUDE)) {
            myResult = new IncludeImpl(myModel, myElement);
        }
        else if (isAcceptable(ExcludeImpl.EXCLUDE)) {
            myResult = new ExcludeImpl(myModel, myElement);
        }
    }

    @Override
    public void visit( StyleSheet styleSheet ) {
    }

    GwtComponent get() {
        return myResult;
    }

    private boolean isAcceptable( String tagName ) {
        return myElement.getLocalName().equals(tagName);
    }

    private GwtModelImpl myModel;

    private Element myElement;

    private GwtComponentImpl myResult;
}
