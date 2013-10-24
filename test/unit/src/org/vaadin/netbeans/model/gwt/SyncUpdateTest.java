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

import static org.junit.Assert.*;

import java.util.List;

import org.netbeans.junit.NbTestCase;

/**
 * @author denis
 */
public class SyncUpdateTest extends NbTestCase {

    public SyncUpdateTest( String name ) {
        super(name);
    }

    public void testSyncModuleComponents() throws Exception {
        GwtModel model = Util.loadRegistryModel("module-orig.xml");

        Util.setDocumentContentTo(model, "module-modified.xml");

        List<ModuleComponent> elements = model.getModule().getComponents();
        assertEquals(9, elements.size());

        List<ModuleComponent> components = model.getModule().getComponents();
        ModuleComponent component = components.get(0);
        assertTrue(component instanceof Inherits);

        component = components.get(1);
        assertTrue(component instanceof Source);
        assertEquals(1, ((Source) component).getChildren().size());

        component = components.get(2);
        assertTrue(component instanceof SetConfigurationProperty);

        component = components.get(3);
        assertTrue(component instanceof SetProperty);

        component = components.get(4);
        assertTrue(component instanceof StyleSheet);

        component = components.get(5);
        assertTrue(component instanceof Inherits);
        assertEquals("widgetset", ((Inherits) component).getName());

        component = components.get(6);
        assertTrue(component instanceof Source);
        assertEquals(0, ((Source) component).getChildren().size());
        assertEquals("newPath", ((Source) component).getPath());

        component = components.get(7);
        assertTrue(component instanceof StyleSheet);
        assertEquals("newStylesheet", ((StyleSheet) component).getSrc());

        component = components.get(8);
        assertTrue(component instanceof SetProperty);
        assertEquals("newName", ((SetProperty) component).getName());
    }

    public void testSyncSource() throws Exception {
        GwtModel model = Util.loadRegistryModel("module-orig.xml");

        Util.setDocumentContentTo(model, "module-source-modified.xml");

        List<ModuleComponent> elements = model.getModule().getComponents();
        assertEquals(5, elements.size());

        List<ModuleComponent> components = model.getModule().getComponents();

        Source source = (Source) components.get(1);
        List<FilterComponent> filters = source.getFilters();
        assertEquals(3, filters.size());

        FilterComponent filter = filters.get(0);
        assertTrue(filter instanceof Include);
        assertEquals("incl", filter.getName());

        filter = filters.get(1);
        assertTrue(filter instanceof Exclude);
        assertEquals("excl", filter.getName());

        filter = filters.get(2);
        assertTrue(filter instanceof Include);
        assertEquals("newIncl", filter.getName());
    }
}
