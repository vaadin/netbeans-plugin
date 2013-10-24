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

import java.util.List;

/**
 * Root model element that correspond to root GWT XML root element "module".
 * It contains children elements represented by ModuleComponent interface.
 * Any specific child element implements this interface, f.e. Include, Inherits,
 * Source, ... <br>
 * See
 * http://google-web-toolkit.googlecode.com/svn/tags/2.5.1/distro-source/core
 * /src/gwt-module.dtd for domain model DTD.
 * 
 * @author denis
 */
public interface Module extends GwtComponent {

    /**
     * Getter for children of Module element
     * 
     * @return children components
     */
    List<ModuleComponent> getComponents();

    /**
     * Removes child component
     * 
     * @param component
     *            child to remove
     */
    void removeComponent( ModuleComponent component );

    /**
     * Adds new child component to the end of list.
     * 
     * @param component
     *            new child component
     */
    void addComponent( ModuleComponent component );
}
