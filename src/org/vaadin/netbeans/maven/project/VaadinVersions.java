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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vaadin.netbeans.retriever.AbstractRetriever;

/**
 * @author denis
 */
public final class VaadinVersions extends AbstractRetriever {

    private static final String VERSIONS = "versions"; // NOI18N

    private static final VaadinVersions INSTANCE = new VaadinVersions();

    private static final String VERSIONS_URL =
            "http://vaadin.com/download/VERSIONS_7"; // NOI18N

    private static final Logger LOG = Logger.getLogger(VaadinVersions.class
            .getName()); // NOI18N  

    private VaadinVersions() {
        requestVersions();
    }

    public static VaadinVersions getInstance() {
        return INSTANCE;
    }

    List<String> getVersions() {
        return versions;
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

    private void requestVersions() {
        requestData();

        File cached = getCachedFile();
        if (cached.exists()) {
            try {
                readVersions(cached);
            }
            catch (IOException e) {
                LOG.log(Level.INFO, null, e);
            }
        }
    }

    private void readVersions( File file ) throws IOException {
        synchronized (getFileLock(file)) {
            FileInputStream inputStream = new FileInputStream(file);
            try {
                readVersions(inputStream);
            }
            finally {
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    LOG.log(Level.INFO, null, e);
                }
            }
        }
    }

    private void readVersions( InputStream stream ) throws IOException {
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
        versions = result;
    }

    private volatile List<String> versions;

}
