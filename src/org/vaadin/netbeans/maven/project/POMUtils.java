/**
 *
 */
package org.vaadin.netbeans.maven.project;

import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.xml.namespace.QName;

import org.netbeans.modules.maven.model.pom.Build;
import org.netbeans.modules.maven.model.pom.Configuration;
import org.netbeans.modules.maven.model.pom.POMComponent;
import org.netbeans.modules.maven.model.pom.POMExtensibilityElement;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.model.pom.POMQName;
import org.netbeans.modules.maven.model.pom.Plugin;

/**
 * @author denis
 */
public final class POMUtils {

    private POMUtils() {
    }

    static Plugin getVaadinPlugin( POMModel model ) {
        Build build = model.getProject().getBuild();
        if (build == null) {
            return null;
        }
        List<Plugin> plugins = build.getPlugins();
        Plugin vaadinPlugin = null;
        for (Plugin plugin : plugins) {
            if (VaadinCustomizer.VAADIN_GROUP_ID.equals(plugin.getGroupId())
                    && VaadinCustomizer.VAADIN_PLUGIN.equals(plugin
                            .getArtifactId()))
            {
                vaadinPlugin = plugin;
                break;
            }
        }
        return vaadinPlugin;
    }

    static Plugin getJettyPlugin( POMModel model ) {
        Build build = model.getProject().getBuild();
        if (build == null) {
            return null;
        }
        List<Plugin> plugins = build.getPlugins();
        Plugin jettyPlugin = null;
        for (Plugin plugin : plugins) {
            if (VaadinCustomizer.JETTY_GROUP_ID.equals(plugin.getGroupId())
                    && VaadinCustomizer.JETTY_PLUGIN.equals(plugin
                            .getArtifactId()))
            {
                jettyPlugin = plugin;
                break;
            }
        }
        return jettyPlugin;
    }

    static void setTextField( String optionName,
            Map<String, POMExtensibilityElement> values, JTextField textField,
            POMComponent component )
    {
        POMExtensibilityElement element = values.get(optionName);
        if (element == null) {
            if (textField.getText().trim().length() > 0) {
                component.addExtensibilityElement(POMUtils.createElement(
                        component.getModel(), optionName, textField.getText()));
            }
        }
        else if (!getValue(element).equals(textField.getText().trim())) {
            element.setElementText(textField.getText());
        }
    }

    static String getValue( POMExtensibilityElement element ) {
        return element.getElementText().trim();
    }

    static void setBooleanVaue( String optionName,
            Map<String, POMExtensibilityElement> values, JCheckBox checkBox,
            Configuration configuration )
    {
        POMExtensibilityElement element = values.get(optionName);
        boolean isEnabled = checkBox.isSelected();
        if (element == null) {
            if (isEnabled) {
                configuration.addExtensibilityElement(POMUtils.createElement(
                        configuration.getModel(), optionName,
                        Boolean.TRUE.toString()));
            }
        }
        else {
            String value = getValue(element);
            if (!value.equals(Boolean.toString(isEnabled))) {
                element.setElementText(Boolean.toString(isEnabled));
            }
        }
    }

    static POMExtensibilityElement createElement( POMModel model, String name,
            String value )
    {
        QName qname = POMQName.createQName(name, model.getPOMQNames()
                .isNSAware());
        POMExtensibilityElement element = model.getFactory()
                .createPOMExtensibilityElement(qname);
        if (value != null) {
            element.setElementText(value);
        }
        return element;
    }
}
