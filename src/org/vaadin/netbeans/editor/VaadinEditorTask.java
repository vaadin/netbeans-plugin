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

import java.util.concurrent.atomic.AtomicReference;

import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.editor.hints.HintsController;
import org.openide.filesystems.FileObject;
import org.vaadin.netbeans.VaadinSupport;

/**
 * @author denis
 */
class VaadinEditorTask implements CancellableTask<CompilationInfo> {

    VaadinEditorTask( VaadinTaskFactory factory ) {
        myFactory = factory;
    }

    @Override
    public void run( CompilationInfo info ) throws Exception {
        FileObject fileObject = info.getFileObject();

        if (!isApplicable(fileObject, info)) {
            return;
        }

        VaadinScanTask task = new VaadinScanTask(info, myFactory);
        runTask.set(task);
        task.run();
        runTask.compareAndSet(task, null);
        HintsController.setErrors(fileObject, "Vaadin Assistant Scanner", // NOI18N
                task.getDescriptions());

    }

    @Override
    public void cancel() {
        VaadinScanTask scanTask = runTask.getAndSet(null);
        if (scanTask != null) {
            scanTask.stop();
        }
    }

    private boolean isApplicable( FileObject fileObject, CompilationInfo info )
    {
        Project project = FileOwnerQuery.getOwner(fileObject);
        if (project == null) {
            return false;
        }
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        if (support == null) {
            return false;
        }
        return support.isEnabled();
    }

    private final AtomicReference<VaadinScanTask> runTask = new AtomicReference<>();

    private final VaadinTaskFactory myFactory;
}
