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
import org.vaadin.netbeans.model.gwt.GwtComponentFactory;
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
class GwtComponentFactoryImpl implements GwtComponentFactory {

    GwtComponentFactoryImpl( GwtModelImpl model ) {
        myModel = model;
    }

    GwtComponent createComponent( GwtComponent parent, Element element ) {
        ComponentBuildVisitor visitor =
                new ComponentBuildVisitor(myModel, parent, element);
        return visitor.get();
    }

    @Override
    public Module createModule() {
        return new ModuleImpl(myModel);
    }

    @Override
    public Source createSource() {
        return new SourceImpl(myModel);
    }

    @Override
    public Exclude createExclude() {
        return new ExcludeImpl(myModel);
    }

    @Override
    public Include createInclude() {
        return new IncludeImpl(myModel);
    }

    @Override
    public Inherits createInherits() {
        return new InheritsImpl(myModel);
    }

    @Override
    public SetConfigurationProperty createSetConfigurationProperty() {
        return new SetConfigurationPropertyImpl(myModel);
    }

    @Override
    public SetProperty createSetProperty() {
        return new SetPropertyImpl(myModel);
    }

    @Override
    public StyleSheet createStyleSheet() {
        return new StyleSheetImpl(myModel);
    }

    private GwtModelImpl myModel;

}
