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

package org.vaadin.netbeans.code.generator;

import java.io.IOException;
import java.util.Set;

import org.netbeans.api.progress.ProgressHandle;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;

/**
 * @author denis
 */
public interface WidgetGenerator {

    String CONNECTOR = "Connector"; // NOI18N

    String COMPONENT_PROPERTY = "vaadinComponent"; // NOI18N

    String WIDGET_SUFFIX = "Widget";// NOI18N

    String SHARED_STATE = "shared_state";//NOI18N

    String CONNECTOR_VAR = "connector";//NOI18N

    String STYLE_NAME = "style_name";//NOI18N

    String WIDGET_SUPER = "widget_super";//NOI18N

    String WIDGET_SUPER_FQN = "widget_super_fqn";//NOI18N

    String STATE_SUPER_CLASS = "state_super_class"; // NOI18N

    String STATE_SUPER_CLASS_FQN = "state_super_class_fqn"; // NOI18N

    String COMPONENT_VAR = "component"; // NOI18N

    String SUPER_CONNECTOR_FQN = "super_connector_fqn"; //NOI18N

    String SUPER_CONNECTOR = "super_connector"; //NOI18N

    String SERVER_COMPONENT = "server_component";//NOI18N

    String SERVER_COMPONENT_FQN = "server_component_fqn";//NOI18N

    String SELECTED_COMPONENT_FQN = "selected_component_fqn"; //NOI18N

    String SELECTED_COMPONENT = "selected_component"; //NOI18N

    String WIDGET_TEMPLATE = "Templates/Vaadin/FullFledgedWidget.java";// NOI18N

    Set<FileObject> generate( WizardDescriptor wizard, ProgressHandle handle )
            throws IOException;

}
