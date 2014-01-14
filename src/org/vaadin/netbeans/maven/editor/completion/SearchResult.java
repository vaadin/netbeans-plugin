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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author denis
 */
public class SearchResult {

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssX");

    SearchResult( String name, String rating, String updateDate ) {
        myName = name;
        myRating = rating;
        myUpdatedDate = updateDate;

        try {
            if (rating != null) {
                myRatingValue = Double.parseDouble(rating);
            }
        }
        catch (NumberFormatException ignore) {
        }

        try {
            if (updateDate != null) {
                myDate = FORMAT.parse(updateDate);
            }
        }
        catch (ParseException ignore) {
        }
    }

    public String getName() {
        return myName;
    }

    public String getRating() {
        return myRating;
    }

    public Double rating() {
        return myRatingValue;
    }

    public String getUpdatedDate() {
        return myUpdatedDate;
    }

    public Date lastUpdated() {
        return myDate;
    }

    private String myName;

    private String myRating;

    private String myUpdatedDate;

    private Double myRatingValue;

    private Date myDate;
}
