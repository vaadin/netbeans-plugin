/**
 *
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
