/*
 * Copyright 2000-2014 Vaadin Ltd.
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
package org.vaadin.netbeans.maven.ui.wizard;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicReference;

import org.vaadin.netbeans.retriever.CachedResource;
import org.vaadin.netbeans.versions.AbstractVaadinVersions;

/**
 * @author denis
 */
@CachedResource(url = LatestStableVaadinVersion.VERSION_URL,
        resourcePath = LatestStableVaadinVersion.RESOURCE_VERSIONS)
public final class LatestStableVaadinVersion extends AbstractVaadinVersions {

    private static final String VERSION = "stable-version"; // NOI18N

    static final String RESOURCE_VERSIONS = VERSION + ".txt"; // NOI18N

    private static final LatestStableVaadinVersion INSTANCE =
            new LatestStableVaadinVersion();

    static final String VERSION_URL = "http://vaadin.com/download/LATEST7"; // NOI18N

    private LatestStableVaadinVersion() {
        myVersion = new AtomicReference<>();
        initCache(LatestStableVaadinVersion.class
                .getResourceAsStream(RESOURCE_VERSIONS));

        init();
    }

    public static LatestStableVaadinVersion getInstance() {
        return INSTANCE;
    }

    String getLatestStableVersion() {
        requestVersions(false, 5000);
        return myVersion.get();
    }

    @Override
    protected String getCachedFileName() {
        return VERSION;
    }

    @Override
    protected String getUrl() {
        return VERSION_URL;
    }

    @Override
    protected Object getFileLock( File file ) {
        return this;
    }

    @Override
    protected void readVersions( InputStream stream ) throws IOException {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(stream, UTF_8));
        String line = reader.readLine();
        if (line != null) {
            line = line.trim();
        }
        myVersion.set(line);
    }

    private final AtomicReference<String> myVersion;

}
