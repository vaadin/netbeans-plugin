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

import org.openide.util.NbBundle;

/**
 * @author denis
 */
@NbBundle.Messages({
        "generateServerComponent=Generate Server Side Component class",
        "generateConnector=Generate Connector class" })
public class ConnectorOnlyWidgetGenerator extends SimpleConnectorGenerator {

    private static final String COMPONENT_TEMPLATE = "Templates/Vaadin/ConnectorComponent.java"; // NOI18N

    private static final String CONNECTOR_TEMPLATE = "Templates/Vaadin/ConnectorConnector.java"; // NOI18N

    @Override
    protected String getComponentTemplate() {
        return COMPONENT_TEMPLATE;
    }

    @Override
    protected String getConnectorTemplate() {
        return CONNECTOR_TEMPLATE;
    }

    @Override
    protected String getComponentClassGenerationMessage() {
        return Bundle.generateServerComponent();
    }

    @Override
    protected String getConnectorClassGenerationMessage() {
        return Bundle.generateConnector();
    }

}
