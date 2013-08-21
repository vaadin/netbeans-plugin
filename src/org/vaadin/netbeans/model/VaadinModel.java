/**
 *
 */
package org.vaadin.netbeans.model;

import java.util.Collection;

import org.openide.filesystems.FileObject;

/**
 * @author denis
 */
public interface VaadinModel {

    Collection<ServletConfiguration> getServletConfigurations();

    FileObject getGwtXml();

    boolean isSuperDevModeEnabled();

    String getSourcePath();

}
