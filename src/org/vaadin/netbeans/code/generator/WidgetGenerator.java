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

    Set<FileObject> generate( WizardDescriptor wizard, ProgressHandle handle )
            throws IOException;

}
