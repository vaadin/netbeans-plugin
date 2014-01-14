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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.Task;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.code.WidgetUtils;
import org.vaadin.netbeans.code.generator.WidgetGenerator;
import org.vaadin.netbeans.editor.analyzer.ui.StatePanel;
import org.vaadin.netbeans.utils.JavaUtils;

/**
 * @author denis
 */
class CreateSharedState extends StateAccessorFix {

    private static final String STATE_SUFFIX = "State"; // NOI18N

    private static final String SHARED_STATE_TEMPLATE =
            "Templates/Vaadin/SharedState.java"; // NOI18N

    CreateSharedState( FileObject fileObject, boolean serverSide,
            ElementHandle<TypeElement> handle,
            ElementHandle<TypeElement> pairHandle )
    {
        super(fileObject, handle, pairHandle);
        isServer = serverSide;
    }

    @Override
    @NbBundle.Messages({
            "generateStateHere=Generate new state and its getter only for this class",
            "generateState=Generate new state and its getter in both server and client classes" })
    public String getText() {
        if (getPairHandle() == null) {
            return Bundle.generateStateHere();
        }
        else {
            return Bundle.generateState();
        }
    }

    @Override
    public ChangeInfo implement() throws Exception {
        String name = getStateName();
        if (name == null) {
            return null;
        }
        if (getClientPackage() == null) {
            createClientPackage();
        }
        Map<String, String> map = new HashMap<String, String>();
        map.put(WidgetGenerator.COMPONENT_VAR, null);
        if (mySuperClass != null) {
            int index = mySuperClass.lastIndexOf('.');
            String simpleSuperClassName = mySuperClass.substring(index + 1);
            map.put(WidgetGenerator.STATE_SUPER_CLASS_FQN, mySuperClass);
            map.put(WidgetGenerator.STATE_SUPER_CLASS, simpleSuperClassName);
        }
        else {
            map.put(WidgetGenerator.STATE_SUPER_CLASS_FQN, null);
            map.put(WidgetGenerator.STATE_SUPER_CLASS, null);
        }
        DataObject dataObject =
                JavaUtils.createDataObjectFromTemplate(SHARED_STATE_TEMPLATE,
                        getClientPackage(), name, map);
        if (dataObject != null) {
            EditorCookie cookie =
                    dataObject.getLookup().lookup(EditorCookie.class);
            if (cookie != null) {
                cookie.open();
            }
            FileObject stateFile = dataObject.getPrimaryFile();

            return doImplement(JavaUtils.getFqn(stateFile));
        }
        return null;
    }

    @NbBundle.Messages("stateName=Shared State Name")
    private String getStateName() {
        String suggestedName = null;
        try {
            suggestedName = suggestName();
        }
        catch (IOException ignore) {
        }
        try {
            initStateSuperclass();
        }
        catch (IOException ignore) {
        }

        final String name = suggestedName;
        return Mutex.EVENT.readAccess(new Mutex.Action<String>() {

            @Override
            public String run() {
                StatePanel panel = new StatePanel(name, mySuperClass);
                DialogDescriptor descriptor =
                        new DialogDescriptor(panel, Bundle.stateName());
                Object result = DialogDisplayer.getDefault().notify(descriptor);
                if (NotifyDescriptor.OK_OPTION.equals(result)) {
                    return panel.getIfaceName();
                }
                else {
                    return null;
                }
            }
        });
    }

    private void initStateSuperclass() throws IOException {
        JavaSource javaSource = JavaSource.forFileObject(getFileObject());
        javaSource.runUserActionTask(new Task<CompilationController>() {

            @Override
            public void run( CompilationController controller )
                    throws Exception
            {
                controller.toPhase(Phase.ELEMENTS_RESOLVED);

                TypeElement type = getHandle().resolve(controller);
                if (type == null) {
                    return;
                }

                TypeElement state = WidgetUtils.getState(type, controller);
                if (state != null) {
                    mySuperClass = state.getQualifiedName().toString();
                }
            }

        }, true);
    }

    private String suggestName() throws IOException {
        if (isServer) {
            JavaSource javaSource = JavaSource.forFileObject(getFileObject());
            if (getPairHandle() != null && javaSource != null) {
                javaSource.runUserActionTask(new Task<CompilationController>() {

                    @Override
                    public void run( CompilationController controller )
                            throws Exception
                    {
                        controller.toPhase(Phase.ELEMENTS_RESOLVED);

                        FileObject fileObject =
                                SourceUtils.getFile(getPairHandle(),
                                        controller.getClasspathInfo());
                        if (fileObject != null) {
                            setClientPackage(fileObject.getParent());
                        }

                    }
                }, true);
                if (getClientPackage() != null) {
                    return findFreeName(getClientPackage());
                }
            }
            searchClientPackage(false);
            if (getClientPackage() != null) {
                return findFreeName(getClientPackage());
            }
            return null;
        }
        else {
            return findFreeName(getFileObject().getParent());
        }
    }

    private String findFreeName( FileObject folder ) {
        setClientPackage(folder);
        StringBuilder freeName = new StringBuilder(getFileObject().getName());
        if (!isServer
                && freeName.toString().endsWith(WidgetGenerator.CONNECTOR))
        {
            freeName =
                    freeName.delete(freeName.length()
                            - WidgetGenerator.CONNECTOR.length(),
                            freeName.length());
        }
        freeName.append(STATE_SUFFIX);
        String name = freeName.toString();

        freeName.append(JavaUtils.JAVA_SUFFIX);
        int i = 1;
        while (folder.getFileObject(freeName.toString()) != null) {
            freeName.setLength(0);
            freeName.append(name);
            freeName.append(i);
            freeName.append(JavaUtils.JAVA_SUFFIX);
            i++;
        }
        return freeName.substring(0,
                freeName.length() - JavaUtils.JAVA_SUFFIX.length());
    }

    private boolean isServer;

    private String mySuperClass;

}
