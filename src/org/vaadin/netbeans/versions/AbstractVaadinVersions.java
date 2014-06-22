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
package org.vaadin.netbeans.versions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.vaadin.netbeans.customizer.RemoteDataAccessStrategy;
import org.vaadin.netbeans.customizer.VaadinConfiguration;
import org.vaadin.netbeans.retriever.AbstractRetriever;

/**
 * @author denis
 */
public abstract class AbstractVaadinVersions extends AbstractRetriever {

    private static final Logger LOG = Logger
            .getLogger(AbstractVaadinVersions.class.getName());

    protected abstract void readVersions( InputStream stream )
            throws IOException;

    protected void init() {
        try {
            readVersions(getCachedFile());
        }
        catch (IOException e) {
            LOG.log(Level.INFO, null, e);
        }
    }

    protected void updateCache() {
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

    protected void readVersions( File file ) throws IOException {
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

    protected static void requestVersions( boolean force, int delay ) {
        boolean postRequest = force;
        if (!postRequest) {
            Date lastUpdate =
                    VaadinConfiguration.getInstance().getLastVersionsUpdate();
            RemoteDataAccessStrategy strategy =
                    VaadinConfiguration.getInstance()
                            .getVersionRequestStrategy();
            boolean requestUpdate = false;
            if (lastUpdate != null) {
                requestUpdate =
                        VaadinConfiguration
                                .requiresUpdate(strategy, lastUpdate);
            }
            else if (RemoteDataAccessStrategy.PER_IDE_RUN.equals(strategy)) {
                lastUpdate = new Date();
            }
            postRequest = lastUpdate == null || requestUpdate;
        }

        if (postRequest) {
            RequestProcessor processor =
                    new RequestProcessor(AbstractVaadinVersions.class);
            processor.post(new Runnable() {

                @Override
                public void run() {
                    Collection<? extends CachedVersionsProvider> providers =
                            Lookup.getDefault().lookupAll(
                                    CachedVersionsProvider.class);
                    for (CachedVersionsProvider provider : providers) {
                        provider.getVersionsUpdater().updateCache();
                    }
                    VaadinConfiguration.getInstance().setLastVersionUpdate(
                            new Date());
                }
            }, delay);

        }
    }

    private static void initVersions() {
        VaadinConfiguration config = VaadinConfiguration.getInstance();
        if (RemoteDataAccessStrategy.PER_IDE_RUN.equals(config
                .getVersionRequestStrategy()))
        {
            try {
                requestVersions(true, 0);
            }
            catch (Exception e) {
                LOG.log(Level.SEVERE, null, e);
            }
        }
    }

    static {
        initVersions();
    }
}
