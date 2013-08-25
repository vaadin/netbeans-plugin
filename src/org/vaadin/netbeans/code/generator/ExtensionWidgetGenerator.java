package org.vaadin.netbeans.code.generator;

import org.openide.util.NbBundle;

/**
 * @author denis
 */
@NbBundle.Messages({
        "generateServerExtension=Generate Server Side Extension class",
        "generateExtensionConnector=Generate Extension Connector class" })
public class ExtensionWidgetGenerator extends SimpleConnectorGenerator {

    private static final String COMPONENT_TEMPLATE = "Templates/Vaadin/Extension.java"; // NOI18N

    private static final String CONNECTOR_TEMPLATE = "Templates/Vaadin/ExtensionConnector.java"; // NOI18N

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
        return Bundle.generateServerExtension();
    }

    @Override
    protected String getConnectorClassGenerationMessage() {
        return Bundle.generateExtensionConnector();
    }

}
