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
public class AddOn extends AbstractAddOn {

    AddOn( String name, List<SourceClass> classes ) {
        super(name);
        myClasses = classes;
    }

    public List<SourceClass> getClasses() {
        return Collections.unmodifiableList(myClasses);
    }

    public String getLastUpdate() {
        return myLastUpdate;
    }

    private void setLastUpdated( String lastUpdate ) {
        myLastUpdate = lastUpdate;
    }

    public static class Builder extends AbstractAddOn.Builder<AddOn> {

        public Builder() {
            super(AddOn.class);
        }

        public AddOn build( AddOn original, String groupId, String artifactId,
                String version, String description, String rating, String url,
                String maturity, String lastUpdated, List<License> licenses )
        {
            AddOn addOn =
                    super.build(original, groupId, artifactId, version,
                            description, rating, url, maturity, licenses);
            addOn.setLastUpdated(lastUpdated);
            return addOn;
        }
    }

    private String myLastUpdate;

    private List<SourceClass> myClasses;

}
