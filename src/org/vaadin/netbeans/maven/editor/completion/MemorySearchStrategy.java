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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.Builder;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.License;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.Maturity;
import org.vaadin.netbeans.maven.editor.completion.SearchQuery.Field;
import org.vaadin.netbeans.maven.editor.completion.SourceClass.SourceType;

/**
 * @author denis
 */
public class MemorySearchStrategy implements SearchStrategy {

    public MemorySearchStrategy( Collection<AddOn> addons ) {
        myAddons = Collections.unmodifiableCollection(addons);
    }

    @Override
    public Collection<AddOnClass> searchClasses( String prefix, SourceType type )
    {
        List<AddOnClass> result = new LinkedList<>();
        for (AddOn addon : myAddons) {
            List<SourceClass> classes = addon.getClasses();
            for (SourceClass clazz : classes) {
                String name = clazz.getName();
                if (type.equals(clazz.getType()) && name.startsWith(prefix)) {
                    AddOnClass cl =
                            new AddOnClass(type, name,
                                    clazz.getQualifiedName(), addon.getName());
                    result.add(cl);
                }
            }
        }
        return result;
    }

    @Override
    public AddOnDoc getDoc( AddOnClass clazz ) {
        for (AddOn addon : myAddons) {
            String name = addon.getName();
            if (name != null && name.equals(clazz.getAddOnName())) {
                AddOnDoc doc =
                        new AddOnDoc(name, clazz.getName(),
                                clazz.getQualifiedName());
                Builder<AddOnDoc> builder = new Builder<>(AddOnDoc.class);
                return builder.build(doc, addon.getMavenVersion(),
                        addon.getDescription(), addon.getRating(),
                        addon.getUrl(), addon.getMaturity(),
                        fillLicenses(addon));
            }
        }
        return null;
    }

    @Override
    public Collection<? extends SearchResult> searchAddons( SearchQuery query )
    {
        List<String> tokens = new LinkedList<>();
        String textSearch = query.getTextSearch();
        if (textSearch != null) {
            if (textSearch.trim().length() > 0) {
                StringTokenizer tokenizer = new StringTokenizer(textSearch);
                while (tokenizer.hasMoreTokens()) {
                    tokens.add(tokenizer.nextToken());
                }
            }
        }
        List<SearchResult> list = new LinkedList<>();
        for (AddOn addon : myAddons) {
            Maturity maturity = addon.getMaturity();
            if (maturity != null) {
                if (!hasMaturity(maturity, addon)) {
                    continue;
                }
            }
            if (!hasLicense(query, addon)) {
                continue;
            }
            if (!hasText(query, tokens, addon)) {
                continue;
            }
            list.add(new SearchResult(addon.getName(), addon.getRating(), addon
                    .getLastUpdate()));
        }
        return list;
    }

    @Override
    public AddOn getAddOn( SearchResult result ) {
        for (AddOn addon : myAddons) {
            if (addon.getName().equals(result.getName())) {
                Builder<AddOn> builder = new Builder<AddOn>(AddOn.class);
                return builder.build(addon, fillLicenses(addon));
            }
        }
        return null;
    }

    private boolean hasMaturity( Maturity maturity, AddOn addon ) {
        Maturity addonMaturity = addon.getMaturity();
        if (maturity.equals(addonMaturity)) {
            return true;
        }
        boolean result = false;
        switch (maturity) {
            case EXPERIMENTAL:
                result = result || Maturity.EXPERIMENTAL.equals(addonMaturity);
                // no break!
            case BETA:
                result = result || Maturity.BETA.equals(addonMaturity);
                // no break!
            case STABLE:
                result = result || Maturity.STABLE.equals(addonMaturity);
                // no break!
            case CERTIFIED:
                result = result || Maturity.CERTIFIED.equals(addonMaturity);
        }
        return result;
    }

    private boolean hasText( SearchQuery query, List<String> tokens, AddOn addon )
    {
        if (tokens.size() == 0) {
            return true;
        }
        else {
            if (query.getFields().contains(Field.NAME)) {
                String name = addon.getName();
                if (name != null) {
                    name = name.trim().toLowerCase();
                }
                if (hasText(query, tokens, name)) {
                    return true;
                }
            }
            if (query.getFields().contains(Field.DESCRIPTION)) {
                String description = addon.getDescription();
                if (description != null) {
                    description = description.trim().toLowerCase();
                }
                if (hasText(query, tokens, description)) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean hasText( SearchQuery query, List<String> tokens,
            String value )
    {
        if (value == null) {
            return false;
        }
        for (String token : tokens) {
            if (!value.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasLicense( SearchQuery query, AddOn addon ) {
        if (query.isFree() == null) {
            return true;
        }
        else {
            List<License> licenses = addon.getLicenses();
            for (License license : licenses) {
                if (!(license.isFree() ^ query.isFree())) {
                    return true;
                }
            }
            return false;
        }
    }

    private List<License> fillLicenses( AbstractAddOn addon ) {
        List<License> newLicenses = new ArrayList<>(addon.getLicenses().size());
        for (License license : addon.getLicenses()) {
            newLicenses.add(fillLicense(addon, license));
        }
        return newLicenses;
    }

    private License fillLicense( AbstractAddOn addon, License license ) {
        String artifactId = license.getArtifactId();
        if (artifactId == null) {
            artifactId = addon.getArtifactId();
        }
        String groupId = license.getGroupId();
        if (groupId == null) {
            groupId = addon.getGroupId();
        }
        String version = license.getVersion();
        if (version == null) {
            version = addon.getMavenVersion();
        }
        return new License(license.isFree(), license.getName(),
                license.getUrl(), groupId, artifactId, version);
    }

    private Collection<AddOn> myAddons;

}
