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
