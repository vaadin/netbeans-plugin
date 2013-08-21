/**
 *
 */
package org.vaadin.netbeans;

import java.io.IOException;
import java.util.Collection;

import org.netbeans.api.java.source.ClasspathInfo;
import org.openide.execution.ExecutorTask;
import org.vaadin.netbeans.model.ModelOperation;

/**
 * @author denis
 */
public interface VaadinSupport {

    public enum Action {
        RUN_JETTY, DEBUG_JETTY, DEV_MODE, DEBUG_DEV_MODE, SUPER_DEV_MODE;
    }

    boolean isEnabled();

    boolean isWeb();

    void runModelOperation( ModelOperation operation ) throws IOException;

    void addAction( Action action, ExecutorTask task );

    Collection<ExecutorTask> getTasks( Action action );

    ClasspathInfo getClassPathInfo();

}
