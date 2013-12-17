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

import org.openide.util.NbBundle;

/**
 * @author denis
 */
public enum RemoteDataAccessStrategy {

    CACHED("ITEM_UseCached"),
    PER_IDE_RUN("ITEM_RequestOnEachStart"),
    PER_DAY("ITEM_RequestDaily"),
    PER_WEEK("ITEM_RequestWeekly"),
    PER_MONTH("ITEM_RequestMonthly");

    private RemoteDataAccessStrategy( String key ) {
        myKey = key;
    }

    public String getCode() {
        return super.toString();
    }

    @Override
    public String toString() {
        return NbBundle.getMessage(RemoteDataAccessStrategy.class, myKey);
    }

    public static RemoteDataAccessStrategy forString( String code ) {
        return valueOf(code);
    }

    private String myKey;
}
