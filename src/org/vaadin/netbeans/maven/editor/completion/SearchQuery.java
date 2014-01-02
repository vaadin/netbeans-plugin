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
import java.util.Set;

import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.Maturity;

/**
 * @author denis
 */
public class SearchQuery {

    public enum Field {
        ANY,
        NAME,
        DESCRIPTION;
    }

    public SearchQuery( Maturity maturity, Boolean free, String textSearch,
            Set<Field> fields )
    {
        myMaturity = maturity;
        isFree = free;
        myTextSearch = textSearch;
        myFields = Collections.unmodifiableSet(fields);
    }

    public Maturity getMaturity() {
        return myMaturity;
    }

    public Boolean isFree() {
        return isFree;
    }

    public String getTextSearch() {
        return myTextSearch;
    }

    public Set<Field> getFields() {
        return myFields;
    }

    private Maturity myMaturity;

    private Boolean isFree;

    private String myTextSearch;

    private Set<Field> myFields;
}
