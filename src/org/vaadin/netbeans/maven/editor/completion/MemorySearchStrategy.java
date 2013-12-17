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

import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.Builder;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.License;
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
                        addon.getUrl(), addon.getMaturity(), fillLicenses(doc));
            }
        }
        return null;
    }

    private List<License> fillLicenses( AddOnDoc doc ) {
        List<License> newLicenses = new ArrayList<>(doc.getLicenses().size());
        for (License license : doc.getLicenses()) {
            newLicenses.add(fillLicense(doc, license));
        }
        return newLicenses;
    }

    private License fillLicense( AddOnDoc doc, License license ) {
        String artifactId = license.getArtifactId();
        if (artifactId == null) {
            artifactId = doc.getArtifactId();
        }
        String groupId = license.getGroupId();
        if (groupId == null) {
            groupId = doc.getGroupId();
        }
        String version = license.getVersion();
        if (version == null) {
            version = doc.getMavenVersion();
        }
        return new License(license.isFree(), license.getName(),
                license.getUrl(), groupId, artifactId, version);
    }

    private Collection<AddOn> myAddons;

}
