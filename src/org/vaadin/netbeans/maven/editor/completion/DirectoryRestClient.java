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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.Builder;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.License;
import org.vaadin.netbeans.retriever.AbstractRetriever;

/**
 * @author denis
 */
final class DirectoryRestClient extends AbstractRetriever {

    private static final String LICENSES = "licenses"; // NOI18N

    private static final String FREE = "free"; // NOI18N

    private static final String LICENSE_FILE_URI = "licenseFileUri"; // NOI18N

    private static final String MATURITY = "maturity"; // NOI18N

    private static final String LINK_URL = "linkUrl"; // NOI18N

    private static final String RATING = "avgRating"; // NOI18N

    private static final String VERSION = "version"; // NOI18N

    private static final String GROUP_ID = "groupId"; // NOI18N

    private static final String SUMMARY = "summary"; // NOI18N

    private static final String ARTIFACT_ID = "artifactId"; // NOI18N

    private static final String DIRECTORY_URL =
            "https://vaadin.com/Directory/resource/addon/all?detailed=true&vaadin="; // NOI18N

    private static final int VAADIN_VERSION = 7;

    private static final String DIRECTORY_ADDONS = "directoryAddons"; // NOI18N

    private static final String ADDON_INFO = "resources/addons.json"; // NOI18N

    private static final Logger LOG = Logger
            .getLogger(DirectoryRestClient.class.getName()); // NOI18N  

    DirectoryRestClient() {
        initCache(DirectoryRestClient.class.getResourceAsStream(ADDON_INFO));
    }

    @Override
    protected String getCachedFileName() {
        return DIRECTORY_ADDONS;
    }

    @Override
    protected String getUrl() {
        return DIRECTORY_URL + VAADIN_VERSION;
    }

    @Override
    protected Object getFileLock( File file ) {
        return this;
    }

    void resetCache() {
        requestData();
    }

    boolean initAddons( Map<String, AddOn> addons ) {
        File cached = getCachedFile();
        JSONArray array = null;
        if (cached.exists()) {
            try {
                array = readDirectory(cached);
            }
            catch (IOException e) {
                LOG.log(Level.INFO, null, e);
            }
        }
        if (array == null) {
            return false;
        }
        int count = 0;
        Set<String> initializedNames = new HashSet<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject object = (JSONObject) array.get(i);

            String name = getValue(object, AddOnParser.NAME);
            AddOn addOn = addons.get(name);
            if (addOn != null) {
                initializedNames.add(name);
                addons.put(addOn.getName(), initAddon(addOn, object));
                count++;
            }
            if (count == addons.size()) {
                break;
            }
        }
        // clean up not-initialized add-ons if any
        if (addons.size() != initializedNames.size()) {
            for (Iterator<String> iterator = addons.keySet().iterator(); iterator
                    .hasNext();)
            {
                String next = iterator.next();
                if (!initializedNames.contains(next)) {
                    iterator.remove();
                }
            }
        }
        return true;
    }

    private AddOn initAddon( AddOn addOn, JSONObject object ) {
        String groupId = getValue(object, GROUP_ID);
        String artifactId = getValue(object, ARTIFACT_ID);
        if (groupId == null || artifactId == null) {
            return addOn;
        }
        Builder<AddOn> builder = new Builder<AddOn>(AddOn.class);
        return builder.build(addOn, groupId, artifactId,
                getValue(object, VERSION), getValue(object, SUMMARY),
                getValue(object, RATING), getValue(object, LINK_URL),
                getValue(object, MATURITY), getLicenses(object));

    }

    private List<License> getLicenses( JSONObject object ) {
        Object value = object.get(LICENSES);
        if (value instanceof JSONObject) {
            return Collections.singletonList(getLicense((JSONObject) value));
        }
        else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            List<License> result = new ArrayList<AddOn.License>(array.size());
            for (int i = 0; i < array.size(); i++) {
                JSONObject license = (JSONObject) array.get(i);
                result.add(getLicense(license));
            }
            return result;
        }
        else {
            return Collections.emptyList();
        }
    }

    private License getLicense( JSONObject license ) {
        String isFree = getValue(license, FREE);
        return new License(
                Boolean.TRUE.toString().toLowerCase().equals(isFree), getValue(
                        license, AddOnParser.NAME), getValue(license,
                        LICENSE_FILE_URI));
    }

    private String getValue( JSONObject object, String key ) {
        Object value = object.get(key);
        if (value == null) {
            return null;
        }
        else {
            return value.toString();
        }
    }

    private JSONArray readDirectory( File file ) throws IOException {
        String content = null;
        synchronized (getFileLock(file)) {
            content = readFile(file);
        }

        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(content);
            JSONObject o = (JSONObject) obj;
            return (JSONArray) o.get(AddOnParser.ADDON);
        }
        catch (ParseException e) {
            LOG.log(Level.INFO, null, e);
        }
        return null;
    }

}
