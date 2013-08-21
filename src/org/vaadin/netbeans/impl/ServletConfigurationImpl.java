/**
 *
 */
package org.vaadin.netbeans.impl;

import org.vaadin.netbeans.model.ServletConfiguration;

/**
 * @author denis
 */
class ServletConfigurationImpl implements ServletConfiguration {

    @Override
    public String getWidgetset() {
        return myWidgetset;
    }

    void setWidgetset( String widgetset ) {
        myWidgetset = widgetset;
    }

    private String myWidgetset;

}
