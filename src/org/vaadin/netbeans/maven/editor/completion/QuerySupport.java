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
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.Maturity;
import org.vaadin.netbeans.maven.editor.completion.SearchQuery.Field;
import org.vaadin.netbeans.maven.editor.completion.SourceClass.SourceType;

/**
 * @author denis
 */
class QuerySupport {

    static final String NAME = "name"; // NOI18N

    static final String CLASS_NAME = "className"; // NOI18N

    static final String CLASS_FQN = "classFqn"; // NOI18N

    static final String URL = "url"; // NOI18N

    static final String DESCRIPTION = "description"; // NOI18N 

    static final String RATING = "rating"; // NOI18N

    static final String ARTIFACT_ID = "artifactId"; // NOI18N

    static final String GROUP_ID = "groupId"; // NOI18N

    static final String VERSION = "version"; // NOI18N

    static final String MATURITY = "maturity"; // NOI18N

    static final String SOURCE_TYPE = "sourceType"; // NOI18N

    static final String FREE = "free"; // NOI18N

    static final String DOC_TYPE = "type"; // NOI18N

    static final String ID = "id"; // NOI18N

    static final String LAST_UPDATE = "lastUpdate"; // NOI18N 

    static final String HAS_FREE_LICENSE = "hasFreeLicense"; // NOI18N 

    static final String HAS_COMMERCIAL_LICENSE = "hasCommercialLicense"; // NOI18N 

    static final String NAME_HASH = "nameHash";

    enum DocType {
        CLASS,
        LICENSE,
        INFO;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    Query createAddOnInfoQuery( String id, Analyzer analyzer )
            throws ParseException
    {
        TermQuery infoQuery =
                new TermQuery(new Term(DOC_TYPE, DocType.INFO.toString()));
        TermQuery licenseQuery =
                new TermQuery(new Term(DOC_TYPE, DocType.LICENSE.toString()));
        BooleanQuery typeQuery = new BooleanQuery();
        typeQuery.add(infoQuery, Occur.SHOULD);
        typeQuery.add(licenseQuery, Occur.SHOULD);

        TermQuery nameQuery = new TermQuery(new Term(ID, id));

        BooleanQuery query = new BooleanQuery();
        query.add(nameQuery, Occur.MUST);
        query.add(typeQuery, Occur.MUST);

        QueryParser parser = new QueryParser(Version.LUCENE_35, NAME, analyzer);
        Query docQuery = parser.parse(query.toString());
        return docQuery;
    }

    Query createClassesQuery( String prefix, SourceType type, String fqn,
            Analyzer analyzer ) throws ParseException
    {
        Query prefixQuery = new PrefixQuery(new Term(CLASS_NAME, prefix));
        Query typeQuery =
                new TermQuery(new Term(DOC_TYPE, DocType.CLASS.toString()));
        Query sourceType =
                new TermQuery(new Term(SOURCE_TYPE, type.toString()));
        BooleanQuery query = new BooleanQuery();
        query.add(prefixQuery, Occur.MUST);
        query.add(typeQuery, Occur.MUST);
        query.add(sourceType, Occur.MUST);
        if (fqn != null) {
            query.add(new TermQuery(new Term(CLASS_FQN, fqn)), Occur.MUST);
        }

        QueryParser parser =
                new QueryParser(Version.LUCENE_35, CLASS_NAME, analyzer);
        Query serchQuery = parser.parse(query.toString());
        return serchQuery;
    }

    Query createSearchInfoQuery( SearchQuery searchQuery, Analyzer analyzer )
            throws ParseException
    {
        TermQuery infoQuery =
                new TermQuery(new Term(DOC_TYPE, DocType.INFO.toString()));

        BooleanQuery query = new BooleanQuery();
        query.add(infoQuery, Occur.MUST);
        Query textQuery = createFreeTextQuery(searchQuery, analyzer);
        if (textQuery != null) {
            query.add(textQuery, Occur.MUST);
        }
        Query maturityQuery = createMaturityQuery(searchQuery);
        if (maturityQuery != null) {
            query.add(maturityQuery, Occur.MUST);
        }
        Query licenseQuery = createLicenseQuery(searchQuery);
        if (licenseQuery != null) {
            query.add(licenseQuery, Occur.MUST);
        }

        QueryParser parser = new QueryParser(Version.LUCENE_35, NAME, analyzer);
        parser.setAllowLeadingWildcard(true);
        Query docQuery = parser.parse(query.toString());
        return docQuery;
    }

    Query createInfoQuery( String name ) {
        TermQuery infoQuery =
                new TermQuery(new Term(DOC_TYPE,
                        QuerySupport.DocType.INFO.toString()));
        TermQuery hashQuery = new TermQuery(new Term(NAME_HASH, getHash(name)));
        BooleanQuery query = new BooleanQuery();
        query.add(infoQuery, Occur.MUST);
        query.add(hashQuery, Occur.MUST);
        return query;
    }

    private Query createLicenseQuery( SearchQuery searchQuery ) {
        Boolean isFree = searchQuery.isFree();
        if (isFree == null) {
            return null;
        }
        if (isFree) {
            return new TermQuery(new Term(HAS_FREE_LICENSE,
                    Boolean.TRUE.toString()));
        }
        else {
            return new TermQuery(new Term(HAS_COMMERCIAL_LICENSE,
                    Boolean.TRUE.toString()));
        }
    }

    private Query createMaturityQuery( SearchQuery searchQuery ) {
        if (searchQuery.getMaturity() == null) {
            return null;
        }
        Query originalMaturity =
                new TermQuery(new Term(MATURITY, searchQuery.getMaturity()
                        .toString()));
        BooleanQuery query = new BooleanQuery();
        query.add(originalMaturity, Occur.SHOULD);
        Query aboveQuery;
        switch (searchQuery.getMaturity()) {
            case CERTIFIED:
                return originalMaturity;
            case EXPERIMENTAL:
                aboveQuery =
                        new TermQuery(new Term(MATURITY,
                                Maturity.BETA.toString()));
                query.add(aboveQuery, Occur.SHOULD);
                // no break !
            case BETA:
                aboveQuery =
                        new TermQuery(new Term(MATURITY,
                                Maturity.STABLE.toString()));
                query.add(aboveQuery, Occur.SHOULD);
                // no break !
            case STABLE:
                aboveQuery =
                        new TermQuery(new Term(MATURITY,
                                Maturity.CERTIFIED.toString()));
                query.add(aboveQuery, Occur.SHOULD);
        }
        return query;
    }

    private Query createFreeTextQuery( SearchQuery searchQuery,
            Analyzer analyzer ) throws ParseException
    {
        Query nameQuery = null;
        Query descrQuery = null;
        if (searchQuery.getFields().contains(Field.NAME)) {
            nameQuery = createFreeTextQuery(searchQuery, NAME, analyzer);
        }
        if (searchQuery.getFields().contains(Field.DESCRIPTION)) {
            descrQuery =
                    createFreeTextQuery(searchQuery, DESCRIPTION, analyzer);
        }
        if (nameQuery == null && descrQuery == null) {
            return null;
        }
        if (nameQuery == null) {
            return descrQuery;
        }
        else if (descrQuery == null) {
            return nameQuery;
        }
        else {
            BooleanQuery query = new BooleanQuery();
            query.add(nameQuery, Occur.SHOULD);
            query.add(descrQuery, Occur.SHOULD);
            return query;
        }
    }

    private Query createFreeTextQuery( SearchQuery searchQuery,
            String fieldName, Analyzer analyzer ) throws ParseException
    {
        List<String> freeTextQueryString =
                createFreeTextQueryString(searchQuery, fieldName, analyzer);
        if (freeTextQueryString.isEmpty()) {
            return null;
        }
        BooleanQuery query = new BooleanQuery();
        QueryParser parser = new QueryParser(Version.LUCENE_35, NAME, analyzer);
        parser.setAllowLeadingWildcard(true);
        for (String textQuery : freeTextQueryString) {
            Query tokenQuery = parser.parse(textQuery);
            query.add(tokenQuery, Occur.MUST);
        }
        return query;
    }

    private List<String> createFreeTextQueryString( SearchQuery query,
            String fieldName, Analyzer analyzer )
    {
        String textSearch = query.getTextSearch();
        if (textSearch == null || textSearch.trim().length() == 0) {
            return Collections.emptyList();
        }
        List<String> result = new LinkedList<>();
        StringBuilder builder = new StringBuilder();
        StringTokenizer tokenizer = new StringTokenizer(textSearch);
        while (tokenizer.hasMoreTokens()) {
            builder.setLength(0);
            builder.append(fieldName);
            builder.append(":*"); // NOI18N
            builder.append(tokenizer.nextToken());
            builder.append('*'); // NOI18N
            result.add(builder.toString());
        }
        return result;
    }

    static String getHash( String str ) {
        String hash = String.valueOf(str.hashCode());
        return hash.replace('-', '0');
    }
}
