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

import java.util.Collections;
import java.util.List;

/**
 * @author denis
 */
public abstract class AbstractAddOn implements Cloneable {

    private static final String LATEST = "LATEST"; // NOI18N

    private static final String ADDON_GROUP_ID = "org.vaadin.addons"; // NOI18N

    public enum Maturity {
        CERTIFIED,
        STABLE,
        BETA,
        EXPERIMENTAL;

        static Maturity forString( String value ) {
            for (Maturity maturity : values()) {
                if (maturity.toString().equals(value)) {
                    return maturity;
                }
            }
            return null;
        }
    }

    AbstractAddOn( String addOnName ) {
        myName = addOnName;
    }

    public String getName() {
        return myName;
    }

    public String getUrl() {
        return myUrl;
    }

    public String getDescription() {
        return myDescription;
    }

    public String getRating() {
        return myRating;
    }

    public String getGroupId() {
        return myGroupId;
    }

    public String getArtifactId() {
        return myArtifactId;
    }

    public String getMavenVersion() {
        if (myMavenVersion == null) {
            return LATEST;
        }
        return myMavenVersion;
    }

    public Maturity getMaturity() {
        return myMaturity;
    }

    public List<License> getLicenses() {
        return myLicenses == null ? Collections.<License> emptyList()
                : myLicenses;
    }

    private void setMaturity( String maturity ) {
        setMaturity(Maturity.forString(maturity));
    }

    private void setMaturity( Maturity maturity ) {
        myMaturity = maturity;
    }

    private void setVersion( String version ) {
        myMavenVersion = version;
    }

    private void setArtifactId( String artifactId ) {
        myArtifactId = artifactId;
    }

    private void setRating( String rating ) {
        myRating = rating;
    }

    private void setGroupId( String groupId ) {
        myGroupId = groupId;
    }

    private void setUrl( String url ) {
        myUrl = url;
    }

    private void setDescription( String description ) {
        myDescription = description;
    }

    private void setLicenses( List<License> licenses ) {
        myLicenses = licenses;
    }

    private String myName;

    private String myUrl;

    private String myRating;

    private String myDescription;

    private String myGroupId;

    private String myArtifactId;

    private String myMavenVersion;

    private Maturity myMaturity;

    private List<License> myLicenses;

    public static class Builder<T extends AbstractAddOn> {

        public Builder( java.lang.Class<T> clazz ) {
            myClass = clazz;
        }

        public T build( T original, String groupId, String artifactId,
                String version, String description, String rating, String url,
                String maturity, List<License> licenses )
        {
            AbstractAddOn addon;
            try {
                addon = (AbstractAddOn) original.clone();
            }
            catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
            if (ADDON_GROUP_ID.equals(groupId)) {
                addon.setGroupId(ADDON_GROUP_ID);
            }
            else {
                addon.setGroupId(groupId);
            }
            addon.setArtifactId(artifactId);
            addon.setVersion(version);
            addon.setDescription(description);
            addon.setRating(rating);
            addon.setUrl(url);
            addon.setMaturity(maturity);
            addon.setLicenses(licenses);
            return myClass.cast(addon);
        }

        public T build( T original, String version, String description,
                String rating, String url, Maturity maturity,
                List<License> licenses )
        {
            AbstractAddOn addon;
            try {
                addon = (AbstractAddOn) original.clone();
            }
            catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
            addon.setVersion(version);
            addon.setDescription(description);
            addon.setRating(rating);
            addon.setUrl(url);
            addon.setMaturity(maturity);
            addon.setLicenses(licenses);
            return myClass.cast(addon);
        }

        public T build( T original, List<License> licenses ) {
            AbstractAddOn addon;
            try {
                addon = (AbstractAddOn) original.clone();
            }
            catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
            addon.setLicenses(licenses);
            return myClass.cast(addon);
        }

        private java.lang.Class<T> myClass;
    }

    public static class License {

        public License( boolean free, String name, String url, String artifactId )
        {
            this(free, name, url, null, artifactId, null);
        }

        public License( boolean free, String name, String url, String groupId,
                String artifactId, String version )
        {
            if (name != null) {
                myName = name.intern();
            }
            myUrl = url;
            isFree = free;
            myGroupId = groupId;
            myArtifactId = artifactId;
            myVersion = version;
        }

        public String getName() {
            return myName;
        }

        public String getUrl() {
            return myUrl;
        }

        public boolean isFree() {
            return isFree;
        }

        public String getArtifactId() {
            return myArtifactId;
        }

        public String getGroupId() {
            return myGroupId;
        }

        public String getVersion() {
            return myVersion;
        }

        private String myName;

        private String myUrl;

        private boolean isFree;

        private String myGroupId;

        private String myArtifactId;

        private String myVersion;
    }
}
