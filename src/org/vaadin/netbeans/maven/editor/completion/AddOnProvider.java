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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.openide.util.RequestProcessor;
import org.vaadin.netbeans.customizer.RemoteDataAccessStrategy;
import org.vaadin.netbeans.customizer.VaadinConfiguration;
import org.vaadin.netbeans.maven.editor.completion.SourceClass.SourceType;

/**
 * @author denis
 */
public final class AddOnProvider {

    private static final AddOnProvider INSTANCE = new AddOnProvider();

    private AddOnProvider() {
        myRest = new DirectoryRestClient();
        myLuceneStrategy = new LuceneSearchStrategy();

        myStrategy = new AtomicReference<SearchStrategy>(myLuceneStrategy);

        isUpdating = new AtomicBoolean(false);

        myRequestProcessor =
                new RequestProcessor(AddOnProvider.class.getName(), 2);

        VaadinConfiguration config = VaadinConfiguration.getInstance();
        config.addPropertyChangeListener(new PropertyChangeListenerImpl());

        if (config.isAddonCodeCompletionEnabled()) {
            runUpdate(
                    config.getDirectoryRequestStrategy().equals(
                            RemoteDataAccessStrategy.PER_IDE_RUN), 0);
        }
    }

    public static AddOnProvider getInstance() {
        return INSTANCE;
    }

    Collection<? extends AddOnClass> searchClasses( String prefix,
            SourceType type )
    {
        SearchStrategy strategy = getSearchStrategy();
        if (strategy == null) {
            return Collections.emptyList();
        }
        checkUpdate();
        return strategy.searchClasses(prefix, type);
    }

    AddOnDoc getDoc( AddOnClass clazz ) {
        SearchStrategy strategy = getSearchStrategy();
        if (strategy == null) {
            return null;
        }
        return strategy.getDoc(clazz);
    }

    void checkUpdate() {
        enable(60000);
    }

    private SearchStrategy getSearchStrategy() {
        return myStrategy.get();
    }

    private boolean updateDirectoryInfo( boolean force, int delay,
            CountDownLatch latch )
    {
        if (force) {
            doUpdateDirectoryInfo(delay, latch);
            return true;
        }
        else {
            Date lastUpdate =
                    VaadinConfiguration.getInstance().getLastDirectoryUpdate();
            RemoteDataAccessStrategy strategy =
                    VaadinConfiguration.getInstance()
                            .getDirectoryRequestStrategy();
            boolean requestUpdate = false;
            if (lastUpdate != null) {
                requestUpdate =
                        VaadinConfiguration
                                .requiresUpdate(strategy, lastUpdate);
            }
            else if (RemoteDataAccessStrategy.PER_IDE_RUN.equals(strategy)) {
                lastUpdate = new Date();
            }
            if (lastUpdate == null || requestUpdate) {
                doUpdateDirectoryInfo(delay, latch);
                return true;
            }
        }
        return false;
    }

    private void updateIndex( boolean force, int delay, CountDownLatch latch ) {
        boolean updateIndex = force;
        if (!force) {
            Date lastUpdate =
                    VaadinConfiguration.getInstance().getLastIndexUpdate();
            RemoteDataAccessStrategy strategy =
                    VaadinConfiguration.getInstance().getIndexRequestStrategy();
            boolean requestUpdate = false;
            if (lastUpdate != null) {
                requestUpdate =
                        VaadinConfiguration
                                .requiresUpdate(strategy, lastUpdate);
            }
            else if (RemoteDataAccessStrategy.PER_IDE_RUN.equals(strategy)) {
                lastUpdate = new Date();
            }
            if (lastUpdate == null || requestUpdate) {
                updateIndex = force;
            }
        }
        doUpdate(updateIndex, delay, latch);
    }

    private void doUpdateDirectoryInfo( int delay, final CountDownLatch latch )
    {
        myRequestProcessor.post(new Runnable() {

            @Override
            public void run() {
                myRest.resetCache();
                latch.countDown();
                VaadinConfiguration.getInstance().setLastDirectoryUpdate(
                        new Date());
            }
        }, delay);
    }

    private LuceneSearchStrategy getLuceneSearchStraregy() {
        return myLuceneStrategy;
    }

    private void doUpdate( final boolean updateIndex, int delay,
            final CountDownLatch latch )
    {
        if (getLuceneSearchStraregy().isInitialized() && latch == null
                && !updateIndex)
        {
            isUpdating.set(false);
            return;
        }
        myRequestProcessor.post(new Runnable() {

            @Override
            public void run() {
                try {
                    reindex(updateIndex, latch);
                }
                finally {
                    isUpdating.set(false);
                }
            }

        }, delay);
    }

    private void reindex( boolean updateIndex, CountDownLatch latch ) {
        assert myRequestProcessor.isRequestProcessorThread();

        AddOnParser parser = new AddOnParser();
        if (updateIndex) {
            parser.resetCache();
        }
        List<AddOn> addons = parser.readAddons();
        Map<String, AddOn> addonsMap = new HashMap<>();
        for (AddOn addOn : addons) {
            addonsMap.put(addOn.getName(), addOn);
        }
        VaadinConfiguration.getInstance().setLastIndexUpdate(new Date());
        boolean update = latch == null;
        if (!update) {
            try {
                update = latch.await(5, TimeUnit.MINUTES);
            }
            catch (InterruptedException ignore) {
            }
        }
        if (update) {
            myRest.initAddons(addonsMap);

            Collection<AddOn> initedAddons = addonsMap.values();
            SearchStrategy strategy = new MemorySearchStrategy(initedAddons);
            myStrategy.set(strategy);

            getLuceneSearchStraregy().index(addonsMap.values());

            myStrategy.set(getLuceneSearchStraregy());
        }
    }

    private void enable( int delay ) {
        if (VaadinConfiguration.getInstance().isAddonCodeCompletionEnabled()) {
            runUpdate(false, delay);
        }
    }

    private void runUpdate( boolean force, int delay ) {
        if (isUpdating.compareAndSet(false, true)) {
            CountDownLatch latch = new CountDownLatch(1);
            boolean hasDirectoryTask = updateDirectoryInfo(false, delay, latch);
            updateIndex(false, delay, hasDirectoryTask ? latch : null);
        }
    }

    private class PropertyChangeListenerImpl implements PropertyChangeListener {

        @Override
        public void propertyChange( PropertyChangeEvent evt ) {
            String propertyName = evt.getPropertyName();
            if (VaadinConfiguration.CODE_COMPLETION.equals(propertyName)) {
                enable(0);
            }
        }

    }

    private final DirectoryRestClient myRest;

    private final RequestProcessor myRequestProcessor;

    private final AtomicBoolean isUpdating;

    private AtomicReference<SearchStrategy> myStrategy;

    private final LuceneSearchStrategy myLuceneStrategy;

}
