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
package org.vaadin.netbeans.editor.analyzer;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.model.ModelOperation;
import org.vaadin.netbeans.model.VaadinModel;
import org.vaadin.netbeans.model.gwt.GwtComponentFactory;
import org.vaadin.netbeans.model.gwt.GwtModel;
import org.vaadin.netbeans.model.gwt.Module;
import org.vaadin.netbeans.model.gwt.Source;
import org.vaadin.netbeans.utils.UIGestureUtils;

/**
 * @author denis
 */
public class AddSourcePathFix implements Fix {

    AddSourcePathFix( String path, FileObject gwtXml ) {
        myPath = path;
        myGwtXml = gwtXml;
    }

    @NbBundle.Messages({ "# {0} - source path",
            "addSourcePath=Add Source Path ''{0}'' to GWT module file" })
    @Override
    public String getText() {
        return Bundle.addSourcePath(myPath);
    }

    @Override
    public ChangeInfo implement() throws Exception {
        logUiUsage();

        Project project = FileOwnerQuery.getOwner(myGwtXml);
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        support.runModelOperation(new ModelOperation() {

            @Override
            public void run( VaadinModel model ) {
                GwtModel gwtModel = model.getGwtModel();
                GwtComponentFactory factory = gwtModel.getFactory();
                Source source = factory.createSource();
                source.setPath(myPath);
                gwtModel.startTransaction();
                try {
                    Module module = gwtModel.getModule();
                    if (module != null) {
                        gwtModel.getModule().addComponent(source);
                    }
                }
                finally {
                    gwtModel.endTransaction();
                }
            }
        });
        DataObject dataObject = DataObject.find(myGwtXml);
        EditorCookie cookie = dataObject.getLookup().lookup(EditorCookie.class);
        if (cookie != null) {
            cookie.open();
        }
        return null;
    }

    private void logUiUsage() {
        UIGestureUtils.logUiUsage(getClass(),
                AbstractJavaFix.UI_FIX_LOGGER_NAME, "UI_LogAddSourcePath"); // NOI18N
    }

    private String myPath;

    private FileObject myGwtXml;

}
