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

import org.netbeans.modules.xml.xam.ComponentUpdater;
import org.vaadin.netbeans.model.gwt.Exclude;
import org.vaadin.netbeans.model.gwt.FilterComponent;
import org.vaadin.netbeans.model.gwt.GwtComponent;
import org.vaadin.netbeans.model.gwt.GwtComponentVisitor;
import org.vaadin.netbeans.model.gwt.Include;
import org.vaadin.netbeans.model.gwt.Inherits;
import org.vaadin.netbeans.model.gwt.Module;
import org.vaadin.netbeans.model.gwt.ModuleComponent;
import org.vaadin.netbeans.model.gwt.SetConfigurationProperty;
import org.vaadin.netbeans.model.gwt.SetProperty;
import org.vaadin.netbeans.model.gwt.Source;
import org.vaadin.netbeans.model.gwt.StyleSheet;

/**
 * @author denis
 */
class SyncUpdateVisitor implements GwtComponentVisitor,
        ComponentUpdater<GwtComponent>
{

    @Override
    public void update( GwtComponent target, GwtComponent child,
            Operation operation )
    {
        update(target, child, -1, operation);
    }

    @Override
    public void update( GwtComponent target, GwtComponent child, int index,
            Operation operation )
    {
        myParent = (GwtComponentImpl) target;
        myIndex = index;
        myOperation = operation;
        child.accept(this);
    }

    @Override
    public void visit( Exclude exclude ) {
        visitFilter(exclude);
    }

    @Override
    public void visit( Include include ) {
        visitFilter(include);
    }

    @Override
    public void visit( Inherits inherits ) {
        visitModuleComponent(inherits);
    }

    @Override
    public void visit( Module module ) {
        assert false : "module element is a root and shouldn't be an argument";
    }

    @Override
    public void visit( SetConfigurationProperty setConfiguration ) {
        visitModuleComponent(setConfiguration);
    }

    @Override
    public void visit( SetProperty setProperty ) {
        visitModuleComponent(setProperty);
    }

    @Override
    public void visit( Source source ) {
        visitModuleComponent(source);
    }

    @Override
    public void visit( StyleSheet styleSheet ) {
        visitModuleComponent(styleSheet);
    }

    private void visitFilter( FilterComponent component ) {
        assert getParent() instanceof SourceImpl;
        SourceImpl source = (SourceImpl) getParent();
        if (isRemove()) {
            source.removeFilter(component);
        }
        else if (isAdd()) {
            source.insertAtIndex(FilterComponent.class.getName(), component,
                    getIndex());
        }
    }

    private void visitModuleComponent( ModuleComponent component ) {
        assert getParent() instanceof ModuleImpl;
        ModuleImpl module = (ModuleImpl) getParent();
        if (isRemove()) {
            module.removeComponent(component);
        }
        else if (isAdd()) {
            module.insertAtIndex(ModuleComponent.class.getName(), component,
                    getIndex());
        }
    }

    private boolean isAdd() {
        return getOperation() == Operation.ADD;
    }

    private boolean isRemove() {
        return getOperation() == Operation.REMOVE;
    }

    private GwtComponentImpl getParent() {
        return myParent;
    }

    private int getIndex() {
        return myIndex;
    }

    private Operation getOperation() {
        return myOperation;
    }

    private GwtComponentImpl myParent;

    private int myIndex;

    private Operation myOperation;

}
