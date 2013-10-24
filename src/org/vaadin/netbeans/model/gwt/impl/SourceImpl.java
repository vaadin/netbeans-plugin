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

import org.vaadin.netbeans.model.gwt.FilterComponent;
import org.vaadin.netbeans.model.gwt.GwtComponent;
import org.vaadin.netbeans.model.gwt.GwtComponentVisitor;
import org.vaadin.netbeans.model.gwt.Source;
import org.w3c.dom.Element;

/**
 * @author denis
 */
class SourceImpl extends GwtComponentImpl implements Source {

    private static final String YES = "yes"; // NOI18N

    private static final String NO = "no"; // NOI18N

    static final String SOURCE = "source"; // NOI18N

    SourceImpl( GwtModelImpl model, Element e ) {
        super(model, e);
    }

    SourceImpl( GwtModelImpl model ) {
        this(model, createNewElement(SOURCE, model));
    }

    @Override
    public Class<? extends GwtComponent> getComponentType() {
        return Source.class;
    }

    @Override
    public void accept( GwtComponentVisitor visitor ) {
        visitor.visit(this);
    }

    @Override
    public List<FilterComponent> getFilters() {
        return getChildren(FilterComponent.class);
    }

    @Override
    public void removeFilter( FilterComponent component ) {
        removeChild(FilterComponent.class.getName(), component);
    }

    @Override
    public void addFilter( FilterComponent component ) {
        appendChild(FilterComponent.class.getName(), component);
    }

    @Override
    public String getPath() {
        return getAttribute(GwtAttribute.PATH);
    }

    @Override
    public void setPath( String path ) {
        setAttribute(GwtAttribute.PATH, path);
    }

    @Override
    public String getExcludes() {
        return getAttribute(GwtAttribute.EXCLUDES);
    }

    @Override
    public void setExcludes( String excludes ) {
        setAttribute(GwtAttribute.EXCLUDES, excludes);
    }

    @Override
    public String getIncludes() {
        return getAttribute(GwtAttribute.INCLUDES);
    }

    @Override
    public void setIncludes( String includes ) {
        setAttribute(GwtAttribute.INCLUDES, includes);
    }

    @Override
    public boolean isDefaultExcludes() {
        String value = getAttribute(GwtBooleanAttribute.DEFAULT_EXCLUDES);
        return !NO.equals(value);
    }

    @Override
    public void setDefaultExcludes( boolean value ) {
        if (value) {
            setAttribute(GwtBooleanAttribute.DEFAULT_EXCLUDES, YES);
        }
        else {
            setAttribute(GwtBooleanAttribute.DEFAULT_EXCLUDES, NO);
        }
    }

    @Override
    public boolean isCasesensitive() {
        String value = getAttribute(GwtBooleanAttribute.CASESENSITIVE);
        return !Boolean.FALSE.toString().toLowerCase().equals(value);
    }

    @Override
    public void setCasesensitive( boolean casesensitive ) {
        setAttribute(GwtBooleanAttribute.CASESENSITIVE,
                String.valueOf(casesensitive));
    }
}
