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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.netbeans.api.project.Project;
import org.vaadin.netbeans.VaadinSupport;
import org.vaadin.netbeans.retriever.CachedResource;
import org.vaadin.netbeans.versions.AbstractVaadinVersions;

/**
 * @author denis
 */
@CachedResource(url = VaadinVersions.VERSIONS_URL,
        resourcePath = VaadinVersions.RESOURCE_VERSIONS)
public final class VaadinVersions extends AbstractVaadinVersions {

    private static final String VERSIONS = "versions"; // NOI18N

    static final String RESOURCE_VERSIONS = VERSIONS + ".txt"; // NOI18N

    private static final VaadinVersions INSTANCE = new VaadinVersions();

    static final String VERSIONS_URL = "http://vaadin.com/download/VERSIONS_8"; // NOI18N

    private VaadinVersions() {
        myVersions = new AtomicReference<>();
        initCache(VaadinVersions.class.getResourceAsStream(RESOURCE_VERSIONS));

        init();
    }

    public static VaadinVersions getInstance() {
        return INSTANCE;
    }

    public boolean isVersionEqualOrHigher( Project project,
            int... versionSpecification )
    {
        String version =
                project.getLookup().lookup(VaadinSupport.class)
                        .getVaadinVersion();
        String[] parts = version.split("\\.");
        try {
            for (int i = 0; i < versionSpecification.length; i++) {
                if (parts.length <= i) {
                    return false;
                }
                else if (Integer.parseInt(parts[i]) > versionSpecification[i]) {
                    return true;
                }
                else if (Integer.parseInt(parts[i]) < versionSpecification[i]) {
                    return false;
                }
            }
        }
        catch (NumberFormatException ingore) {
            Logger.getLogger(VaadinVersions.class.getName()).log(Level.WARNING,
                    "Unexpected version :" + version);
        }
        return true;
    }

    List<String> getVersions() {
        return myVersions.get();
    }

    @Override
    protected String getCachedFileName() {
        return VERSIONS;
    }

    @Override
    protected String getUrl() {
        return VERSIONS_URL;
    }

    @Override
    protected Object getFileLock( File file ) {
        return this;
    }

    @Override
    protected void readVersions( InputStream stream ) throws IOException {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(stream, UTF_8));
        List<String> result = new LinkedList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            String[] versions = line.split(",");
            if (versions.length > 0) {
                result.add(versions[0]);
            }
        }
        myVersions.set(result);
    }

    private final AtomicReference<List<String>> myVersions;

}
