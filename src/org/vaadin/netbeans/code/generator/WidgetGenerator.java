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

    Set<FileObject> generate( WizardDescriptor wizard, ProgressHandle handle )
            throws IOException;

}
