/**
 *
 */
package org.vaadin.netbeans.editor;

import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSourceTaskFactory;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.JavaSource.Priority;
import org.netbeans.api.java.source.support.EditorAwareJavaSourceTaskFactory;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author denis
 */
@ServiceProvider(service = JavaSourceTaskFactory.class)
public class VaadinTaskFactory extends EditorAwareJavaSourceTaskFactory {

    public VaadinTaskFactory() {
        super(Phase.RESOLVED, Priority.LOW, "text/x-java"); // NOI18N
    }

    @Override
    @NonNull
    protected CancellableTask<CompilationInfo> createTask( FileObject fileObject )
    {
        return new VaadinEditorTask(this);
    }

    public void restart( FileObject fileObject ) {
        reschedule(fileObject);
    }

}
