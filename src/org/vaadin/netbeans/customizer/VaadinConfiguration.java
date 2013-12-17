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
package org.vaadin.netbeans.customizer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Calendar;
import java.util.Date;
import java.util.prefs.Preferences;

import org.openide.util.NbPreferences;

/**
 * @author denis
 */
public final class VaadinConfiguration {

    public static final String JETTY = "jetty"; // NOI18N

    public static final String CODE_COMPLETION = "add-on-cc"; // NOI18N

    public static final String STATISTICS = "statistics"; // NOI18N

    public static final String TIMEOUT = "timeout"; // NOI18N

    public static final String CONFIRM_FREE_ADD_ON = "confirmFreeAddOn"; // NOI18N

    public static final String VERSIONS_STRATEGY = "versions-strategy"; // NOI18N

    public static final String INDEX_STRATEGY = "index-strategy"; // NOI18N

    public static final String REST_STRATEGY = "rest-strategy"; // NOI18N

    private static final String LAST_VERSIONS_UPDATE = "last-versions"; // NOI18N

    private static final String LAST_INDEX_UPDATE = "last-index"; // NOI18N

    private static final String LAST_REST_UPDATE = "last-rest"; // NOI18N

    private static final VaadinConfiguration INSTANCE =
            new VaadinConfiguration();

    private VaadinConfiguration() {
        mySupport = new PropertyChangeSupport(this);
    }

    public static VaadinConfiguration getInstance() {
        return INSTANCE;
    }

    public void enableJetty( boolean enable ) {
        boolean jettyEnabled;
        synchronized (getPreferences()) {
            jettyEnabled = isJettyEnabled();
            getPreferences().putBoolean(JETTY, enable);
        }
        fireEvent(JETTY, jettyEnabled, enable);
    }

    public boolean isJettyEnabled() {
        synchronized (getPreferences()) {
            return getPreferences().getBoolean(JETTY, true);
        }
    }

    public void enableAddonCodeCompletion( boolean enable ) {
        boolean isEnabled;
        synchronized (getPreferences()) {
            isEnabled = isAddonCodeCompletionEnabled();
            getPreferences().putBoolean(CODE_COMPLETION, enable);
        }
        fireEvent(CODE_COMPLETION, isEnabled, enable);
    }

    public boolean isAddonCodeCompletionEnabled() {
        synchronized (getPreferences()) {
            return getPreferences().getBoolean(CODE_COMPLETION, true);
        }
    }

    public void enableStatistics( boolean enable ) {
        boolean isEnabled;
        synchronized (getPreferences()) {
            isEnabled = isStatisticsEnabled();
            getPreferences().putBoolean(STATISTICS, enable);
        }
        fireEvent(STATISTICS, isEnabled, enable);
    }

    public boolean isStatisticsEnabled() {
        synchronized (getPreferences()) {
            return getPreferences().getBoolean(STATISTICS, true);
        }
    }

    public void setTimeout( int timeout ) {
        int previous;
        synchronized (getPreferences()) {
            previous = getTimeout();
            getPreferences().putInt(TIMEOUT, timeout);
        }
        fireEvent(TIMEOUT, previous, timeout);
    }

    public int getTimeout() {
        synchronized (getPreferences()) {
            return getPreferences().getInt(TIMEOUT, 20);
        }
    }

    public boolean freeAddonRequiresConfirmation() {
        synchronized (getPreferences()) {
            return getPreferences().getBoolean(CONFIRM_FREE_ADD_ON, true);
        }
    }

    public void setFreeAddonRequiresConfirmation( boolean askConfirmation ) {
        boolean freeConfirmation;
        synchronized (getPreferences()) {
            freeConfirmation = freeAddonRequiresConfirmation();
            getPreferences().putBoolean(CONFIRM_FREE_ADD_ON, askConfirmation);
        }
        fireEvent(CONFIRM_FREE_ADD_ON, freeConfirmation, askConfirmation);
    }

    public void setVersionRequestStrategy(
            RemoteDataAccessStrategy versionStrategy )
    {
        RemoteDataAccessStrategy strategy;
        synchronized (getPreferences()) {
            strategy = getVersionRequestStrategy();
            getPreferences().put(VERSIONS_STRATEGY, versionStrategy.getCode());
        }
        fireEvent(VERSIONS_STRATEGY, strategy, versionStrategy);
    }

    public RemoteDataAccessStrategy getVersionRequestStrategy() {
        String code;
        synchronized (getPreferences()) {
            code =
                    getPreferences().get(VERSIONS_STRATEGY,
                            RemoteDataAccessStrategy.PER_IDE_RUN.getCode());
        }
        return RemoteDataAccessStrategy.forString(code);
    }

    public void setIndexRequestStrategy( RemoteDataAccessStrategy indexStrategy )
    {
        RemoteDataAccessStrategy strategy;
        synchronized (getPreferences()) {
            strategy = getIndexRequestStrategy();
            getPreferences().put(INDEX_STRATEGY, indexStrategy.getCode());
        }
        fireEvent(INDEX_STRATEGY, strategy, indexStrategy);
    }

    public RemoteDataAccessStrategy getIndexRequestStrategy() {
        String code;
        synchronized (getPreferences()) {
            code =
                    getPreferences().get(INDEX_STRATEGY,
                            RemoteDataAccessStrategy.PER_IDE_RUN.getCode());
        }
        return RemoteDataAccessStrategy.forString(code);
    }

    public void setDirectoryRequestStrategy(
            RemoteDataAccessStrategy directoryStrategy )
    {
        RemoteDataAccessStrategy strategy;
        synchronized (getPreferences()) {
            strategy = getDirectoryRequestStrategy();
            getPreferences().put(REST_STRATEGY, directoryStrategy.getCode());
        }
        fireEvent(REST_STRATEGY, strategy, directoryStrategy);
    }

    public RemoteDataAccessStrategy getDirectoryRequestStrategy() {
        String code;
        synchronized (getPreferences()) {
            code =
                    getPreferences().get(REST_STRATEGY,
                            RemoteDataAccessStrategy.PER_IDE_RUN.getCode());
        }
        return RemoteDataAccessStrategy.forString(code);
    }

    public Date getLastVersionsUpdate() {
        return getLastUpdate(LAST_VERSIONS_UPDATE);
    }

    public Date getLastIndexUpdate() {
        return getLastUpdate(LAST_INDEX_UPDATE);
    }

    public Date getLastDirectoryUpdate() {
        return getLastUpdate(LAST_REST_UPDATE);
    }

    public void setLastDirectoryUpdate( Date date ) {
        setLastUpdate(LAST_REST_UPDATE, date);
    }

    public void setLastIndexUpdate( Date date ) {
        setLastUpdate(LAST_INDEX_UPDATE, date);
    }

    public void setLastVersionUpdate( Date date ) {
        setLastUpdate(LAST_VERSIONS_UPDATE, date);
    }

    public static boolean requiresUpdate( RemoteDataAccessStrategy strategy,
            Date date )
    {
        boolean requiresUpdate = false;
        Calendar calendar = Calendar.getInstance();
        switch (strategy) {
            case PER_DAY:
                calendar.add(Calendar.DAY_OF_YEAR, -1);
                requiresUpdate = date.before(calendar.getTime());
                break;
            case PER_WEEK:
                calendar.add(Calendar.WEEK_OF_YEAR, -1);
                requiresUpdate = date.before(calendar.getTime());
                break;
            case PER_MONTH:
                calendar.add(Calendar.MONTH, -1);
                requiresUpdate = date.before(calendar.getTime());
                break;
            default:
                break;
        }
        return requiresUpdate;
    }

    private Date getLastUpdate( String property ) {
        synchronized (getPreferences()) {
            long lastUpdate = getPreferences().getLong(property, -1);
            if (lastUpdate == -1) {
                return null;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(lastUpdate);
            return calendar.getTime();
        }
    }

    private void setLastUpdate( String property, Date date ) {
        synchronized (getPreferences()) {
            if (date == null) {
                getPreferences().putLong(property, -1);
            }
            else {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                getPreferences().putLong(property, calendar.getTimeInMillis());
            }
        }
    }

    public void addPropertyChangeListener( PropertyChangeListener listener ) {
        mySupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener( PropertyChangeListener listener )
    {
        mySupport.removePropertyChangeListener(listener);
    }

    private void fireEvent( String property, Object oldValue, Object newValue )
    {
        mySupport.firePropertyChange(property, oldValue, newValue);
    }

    private Preferences getPreferences() {
        return NbPreferences.forModule(VaadinConfiguration.class);
    }

    private PropertyChangeSupport mySupport;
}
