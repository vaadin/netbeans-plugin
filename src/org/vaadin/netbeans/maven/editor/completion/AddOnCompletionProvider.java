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
package org.vaadin.netbeans.maven.editor.completion;

import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.openide.filesystems.FileObject;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.customizer.VaadinConfiguration;

/**
 * @author denis
 */
@MimeRegistration(mimeType = "text/x-java", service = CompletionProvider.class,
        position = 50)
public class AddOnCompletionProvider implements CompletionProvider {

    @Override
    public CompletionTask createTask( int queryType, JTextComponent component )
    {
        if (VaadinConfiguration.getInstance().isAddonCodeCompletionEnabled()) {
            VaadinSupport support = getSupport(component.getDocument());
            if (support == null || !support.isEnabled()) {
                return null;
            }

            if (queryType == CompletionProvider.COMPLETION_ALL_QUERY_TYPE) {
                return new AsyncCompletionTask(new AddonCompletionQuery(
                        component.getSelectionStart()), component);
            }
        }
        return null;
    }

    @Override
    public int getAutoQueryTypes( JTextComponent component, String text ) {
        return 0;
    }

    static Project getProject( Document document ) {
        FileObject fileObject = NbEditorUtilities.getFileObject(document);
        if (fileObject == null) {
            return null;
        }
        return FileOwnerQuery.getOwner(fileObject);
    }

    static VaadinSupport getSupport( Document document ) {
        Project project = getProject(document);
        if (project == null) {
            return null;
        }
        return project.getLookup().lookup(VaadinSupport.class);
    }

}
