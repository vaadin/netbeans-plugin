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
package org.vaadin.netbeans.maven.project;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.xml.namespace.QName;

import org.apache.maven.project.MavenProject;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.modules.maven.model.ModelOperation;
import org.netbeans.modules.maven.model.Utilities;
import org.netbeans.modules.maven.model.pom.Configuration;
import org.netbeans.modules.maven.model.pom.POMExtensibilityElement;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.model.pom.Plugin;
import org.openide.execution.ExecutorTask;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.vaadin.netbeans.customizer.VaadinConfiguration;
import org.vaadin.netbeans.utils.POMUtils;

/**
 * @author denis
 */
public final class DebugUtils {

    static final String DEBUG_TRANSPORT = "dt_socket"; // NOI18N

    static final String BIND_ADDRESS = "bindAddress"; // NOI18N

    static final String DEBUG_PORT = "debugPort"; // NOI18N

    private static final String ATTACH_TIMEOUT = "debugger-attach-timeout"; // NOI18N 

    private static final int DEFAULT_DEBUG_PORT = 8000;

    private DebugUtils() {
    }

    public static boolean waitPort( String host, int port, int timeout,
            ExecutorTask task ) throws InterruptedException
    {
        long current = System.currentTimeMillis();
        long end = current + timeout;
        while (current < end) {
            if (task != null && task.isFinished()) {
                throw new InterruptedException();
            }
            if (checkPort(host, port)) {
                return true;
            }
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException ignore) {
            }
            current = System.currentTimeMillis();
        }
        return false;
    }

    public static int getAttachDebuggerTimout( Project project ) {
        Preferences prefs =
                ProjectUtils.getPreferences(project, DebugUtils.class, true);
        return prefs.getInt(ATTACH_TIMEOUT, VaadinConfiguration.getInstance()
                .getTimeout());
    }

    public static void setAttachDebuggerTimout( Project project, int timeout ) {
        Preferences prefs =
                ProjectUtils.getPreferences(project, DebugUtils.class, true);
        prefs.putInt(ATTACH_TIMEOUT, timeout);
    }

    public static InetSocketAddress getBindAddress( POMModel model ) {
        Plugin plugin = POMUtils.getVaadinPlugin(model);
        if (plugin == null) {
            return getDefaultBindAddress();
        }

        Configuration configuration = plugin.getConfiguration();
        if (configuration == null) {
            return getDefaultBindAddress();
        }
        List<POMExtensibilityElement> params =
                configuration.getExtensibilityElements();
        String host = null;
        String port = null;
        for (POMExtensibilityElement param : params) {
            QName qName = param.getQName();
            String name = qName.getLocalPart();
            if (BIND_ADDRESS.equals(name)) {
                host = param.getElementText().trim();
            }
            else if (DEBUG_PORT.equals(name)) {
                port = param.getElementText().trim();
            }
        }
        InetAddress inetAddress = null;
        if (host == null) {
            inetAddress = InetAddress.getLoopbackAddress();
            host = inetAddress.getHostName();
        }
        int debugPort;
        if (port == null) {
            debugPort = DEFAULT_DEBUG_PORT;
        }
        else {
            try {
                debugPort = Integer.parseInt(port);
            }
            catch (NumberFormatException e) {
                Logger.getLogger(DebugUtils.class.getName()).log(Level.INFO,
                        null, e);
                debugPort = DEFAULT_DEBUG_PORT;
            }
        }
        try {
            if (inetAddress != null) {
                return new InetSocketAddress(inetAddress, debugPort);
            }
            else {
                return new InetSocketAddress(host, debugPort);
            }
        }
        catch (IllegalArgumentException e) {
            // wrong port number
            return new InetSocketAddress(host, DEFAULT_DEBUG_PORT);
        }
    }

    public static InetSocketAddress getBindAddress( Project project ) {
        NbMavenProject mvnProject =
                project.getLookup().lookup(NbMavenProject.class);
        MavenProject mavenProject = mvnProject.getMavenProject();
        File file = mavenProject.getFile();
        FileObject pom = FileUtil.toFileObject(FileUtil.normalizeFile(file));
        final InetSocketAddress[] address = new InetSocketAddress[1];
        ModelOperation<POMModel> operation = new ModelOperation<POMModel>() {

            @Override
            public void performOperation( POMModel model ) {
                address[0] = getBindAddress(model);
            }
        };
        Utilities.performPOMModelOperations(pom,
                Collections.singletonList(operation));
        return address[0];
    }

    static InetSocketAddress getDefaultBindAddress() {
        return new InetSocketAddress(InetAddress.getLoopbackAddress(),
                DEFAULT_DEBUG_PORT);
    }

    static boolean checkPort( String host, int port ) {
        InetSocketAddress address = new InetSocketAddress(host, port);
        Socket socket = new Socket();
        try {
            socket.connect(address);
        }
        catch (IOException e) {
            return false;
        }
        finally {
            try {
                socket.close();
            }
            catch (IOException ignore) {
            }
        }
        return true;
    }
}
