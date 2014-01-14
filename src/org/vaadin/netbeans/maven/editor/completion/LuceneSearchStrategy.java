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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.Builder;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.License;
import org.vaadin.netbeans.maven.editor.completion.SourceClass.SourceType;

/**
 * @author denis
 */
class LuceneSearchStrategy extends LuceneIndexing implements SearchStrategy {

    private static final Logger LOG = Logger
            .getLogger(LuceneSearchStrategy.class.getName());

    LuceneSearchStrategy() {
        myQuerySupport = new QuerySupport();
    }

    @Override
    public Collection<? extends AddOnClass> searchClasses( String prefix,
            SourceType type )
    {
        getLock().readLock().lock();
        try {
            if (isInitialized()) {
                return doSearchClasses(prefix, type, null);
            }
            else {
                return Collections.emptyList();
            }
        }
        finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    public AddOnDoc getDoc( AddOnClass clazz ) {
        getLock().readLock().lock();
        try {
            if (isInitialized()) {
                return doGetDoc(clazz);
            }
            else {
                return null;
            }
        }
        finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    public Collection<? extends SearchResult> searchAddons( SearchQuery query )
    {
        getLock().readLock().lock();
        try {
            if (isInitialized()) {
                return doSearchAddons(query);
            }
            else {
                return Collections.emptyList();
            }
        }
        finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    public AddOn getAddOn( SearchResult result ) {
        getLock().readLock().lock();
        try {
            if (isInitialized()) {
                return doGetAddon(result);
            }
            else {
                return null;
            }
        }
        finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    private Collection<? extends SearchResult> doSearchAddons( SearchQuery query )
    {
        IndexReader reader = null;
        IndexSearcher searcher = null;
        Directory directory = null;
        Analyzer analyzer = null;
        try {
            analyzer = new StandardAnalyzer(Version.LUCENE_35);
            directory = FSDirectory.open(getIndexDir());
            reader = IndexReader.open(directory);
            searcher = new IndexSearcher(reader);

            Query searchQuery =
                    myQuerySupport.createSearchInfoQuery(query, analyzer);
            return doSearchAddons(searchQuery, analyzer, reader, searcher);
        }
        catch (ParseException e) {
            getLogger().log(Level.FINE, null, e);
        }
        catch (IOException e) {
            getLogger().log(Level.FINE, null, e);
        }
        finally {
            closeReadIndex(reader, searcher, directory, analyzer);
        }
        return Collections.emptyList();
    }

    private Collection<? extends SearchResult> doSearchAddons(
            Query searchQuery, Analyzer analyzer, IndexReader reader,
            IndexSearcher searcher ) throws IOException
    {
        List<LuceneSearchResult> result = new LinkedList<>();

        ScoreDoc[] hits =
                searcher.search(searchQuery, Integer.MAX_VALUE).scoreDocs;
        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = searcher.doc(hits[i].doc);
            String name = getField(QuerySupport.NAME, hitDoc);
            String rating = getField(QuerySupport.RATING, hitDoc);
            String date = getField(QuerySupport.LAST_UPDATE, hitDoc);
            String id = getField(QuerySupport.ID, hitDoc);
            LuceneSearchResult res =
                    new LuceneSearchResult(name, rating, date, id,
                            getSessionSeq());
            result.add(res);
        }
        return result;
    }

    private AddOn doGetAddon( SearchResult result ) {
        AddOn addOn = new AddOn(result.getName());

        String id = getInfoId(result);
        if (id == null) {
            LOG.log(Level.WARNING, "Unable to find id for search result, name="//NOI18N
                    + result.getName());
            return null;
        }
        return doGetAddOnInfo(addOn, AddOn.class, id);
    }

    private String getInfoId( SearchResult result ) {
        String id = null;
        if (result instanceof LuceneSearchResult) {
            LuceneSearchResult luceneResult = (LuceneSearchResult) result;
            if (getSessionSeq() == luceneResult.getSessionId()) {
                id = luceneResult.getId().toString();
            }
        }
        if (id == null) {
            /*
             * In rare case <code>result</code> could be created by other 
             * strategy or based on different indexing session.
             */
            id = getInfoIdByName(result.getName());
        }
        return id;
    }

    private String getInfoIdByName( String name ) {
        IndexReader reader = null;
        IndexSearcher searcher = null;
        Directory directory = null;
        String id = null;
        try {
            directory = FSDirectory.open(getIndexDir());
            reader = IndexReader.open(directory);
            searcher = new IndexSearcher(reader);

            ScoreDoc[] hits =
                    searcher.search(myQuerySupport.createInfoQuery(name),
                            Integer.MAX_VALUE).scoreDocs;
            for (int i = 0; i < hits.length; i++) {
                Document hitDoc = searcher.doc(hits[i].doc);
                String docName = getField(QuerySupport.NAME, hitDoc);
                if (name.equals(docName)) {
                    id = getField(QuerySupport.ID, hitDoc);
                    break;
                }
            }
        }
        catch (IOException e) {
            getLogger().log(Level.FINE, null, e);
        }
        finally {
            closeReadIndex(reader, searcher, directory, null);
        }
        return id;
    }

    private AddOnDoc doGetDoc( AddOnClass clazz ) {
        LuceneAddOnClass addOnClass = getLuceneAddOnClass(clazz);
        if (addOnClass == null) {
            LOG.log(Level.WARNING,
                    "Unable to find lucene data for addon class, addon name={0}, "
                            + "class name={1}", //NOI18N
                    new Object[] { clazz.getAddOnName(),
                            clazz.getQualifiedName() });
            return null;
        }

        AddOnDoc result =
                new AddOnDoc(addOnClass.getAddOnName(), addOnClass.getName(),
                        addOnClass.getQualifiedName());
        return doGetAddOnInfo(result, AddOnDoc.class, addOnClass.getId()
                .toString());
    }

    private LuceneAddOnClass getLuceneAddOnClass( AddOnClass clazz ) {
        LuceneAddOnClass addOnClass = null;
        if (clazz instanceof LuceneAddOnClass) {
            addOnClass = (LuceneAddOnClass) clazz;
            if (getSessionSeq() != addOnClass.getSessionId()) {
                addOnClass = null;
            }
        }

        if (addOnClass == null) {
            /*
             *  In rare case <code>clazz</code> could be created by other 
             *  strategy or based on different indexing session.
             */
            Collection<LuceneAddOnClass> classes =
                    doSearchClasses(clazz.getName(), clazz.getType(),
                            clazz.getQualifiedName());
            for (LuceneAddOnClass luceneClass : classes) {
                if (clazz.getAddOnName().equals(luceneClass.getAddOnName())
                        && clazz.getName().equals(luceneClass.getName()))
                {
                    addOnClass = luceneClass;
                    break;
                }
            }
        }
        return addOnClass;
    }

    private <T extends AbstractAddOn> T doGetAddOnInfo( T addon,
            Class<T> clazz, String id )
    {
        IndexReader reader = null;
        IndexSearcher searcher = null;
        Directory directory = null;
        Analyzer analyzer = null;
        try {
            analyzer = new StandardAnalyzer(Version.LUCENE_35);
            directory = FSDirectory.open(getIndexDir());
            reader = IndexReader.open(directory);
            searcher = new IndexSearcher(reader);

            Query query = myQuerySupport.createAddOnInfoQuery(id, analyzer);
            addon = searchAddOnInfo(addon, clazz, searcher, query);
        }
        catch (ParseException e) {
            getLogger().log(Level.FINE, null, e);
        }
        catch (IOException e) {
            getLogger().log(Level.FINE, null, e);
        }
        finally {
            closeReadIndex(reader, searcher, directory, analyzer);
        }
        return addon;
    }

    private <T extends AbstractAddOn> T searchAddOnInfo( T result,
            Class<T> clazz, IndexSearcher searcher, Query query )
            throws IOException, CorruptIndexException
    {
        ScoreDoc[] hits = searcher.search(query, Integer.MAX_VALUE).scoreDocs;
        List<License> licenses = new ArrayList<>(hits.length);
        boolean infoFound = false;
        String artifactId = null;
        String groupId = null;
        String description = null;
        String maturity = null;
        String version = null;
        String url = null;
        String rating = null;
        String lastUpdated = null;

        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = searcher.doc(hits[i].doc);
            String type = getField(QuerySupport.DOC_TYPE, hitDoc);
            if (QuerySupport.DocType.LICENSE.toString().equals(type)) {
                licenses.add(buildLicense(hitDoc));
            }
            else if (QuerySupport.DocType.INFO.toString().equals(type)) {
                assert !infoFound;
                infoFound = true;
                artifactId = getField(QuerySupport.ARTIFACT_ID, hitDoc);
                groupId = getField(QuerySupport.GROUP_ID, hitDoc);
                description = getField(QuerySupport.DESCRIPTION, hitDoc);
                maturity = getField(QuerySupport.MATURITY, hitDoc);
                version = getField(QuerySupport.VERSION, hitDoc);
                rating = getField(QuerySupport.RATING, hitDoc);
                url = getField(QuerySupport.URL, hitDoc);
                lastUpdated = getField(QuerySupport.LAST_UPDATE, hitDoc);
            }
        }
        assert infoFound;

        if (clazz.equals(AddOn.class)) {
            AddOn.Builder builder = new AddOn.Builder();
            result =
                    clazz.cast(builder.build(
                            AddOn.class.cast(result),
                            groupId,
                            artifactId,
                            version,
                            description,
                            rating,
                            url,
                            maturity,
                            lastUpdated,
                            fillLicenses(licenses, groupId, artifactId, version)));
        }
        else {
            Builder<T> builder = new Builder<>(clazz);
            result =
                    builder.build(
                            result,
                            groupId,
                            artifactId,
                            version,
                            description,
                            rating,
                            url,
                            maturity,
                            fillLicenses(licenses, groupId, artifactId, version));
        }
        return result;
    }

    private List<License> fillLicenses( List<License> licenses, String groupId,
            String artifactId, String version )
    {
        List<License> result = new ArrayList<>(licenses.size());
        for (License license : licenses) {
            result.add(new License(license.isFree(), license.getName(), license
                    .getUrl(), license.getGroupId() == null ? groupId : license
                    .getGroupId(), license.getArtifactId() == null ? artifactId
                    : license.getArtifactId(),
                    license.getVersion() == null ? version : license
                            .getVersion()));
        }
        return result;
    }

    private License buildLicense( Document doc ) {
        return new License(Boolean.valueOf(getField(QuerySupport.FREE, doc)),
                getField(QuerySupport.NAME, doc), getField(QuerySupport.URL,
                        doc), getField(QuerySupport.ARTIFACT_ID, doc));
    }

    private Collection<LuceneAddOnClass> doSearchClasses( String prefix,
            SourceType type, String fqn )
    {
        Collection<LuceneAddOnClass> classes = new LinkedList<>();
        IndexReader reader = null;
        IndexSearcher searcher = null;
        Directory directory = null;
        Analyzer analyzer = null;
        try {
            analyzer = new StandardAnalyzer(Version.LUCENE_35);
            directory = FSDirectory.open(getIndexDir());
            reader = IndexReader.open(directory);
            searcher = new IndexSearcher(reader);

            Query query =
                    myQuerySupport.createClassesQuery(prefix, type, fqn,
                            analyzer);
            ScoreDoc[] hits =
                    searcher.search(query, Integer.MAX_VALUE).scoreDocs;
            for (int i = 0; i < hits.length; i++) {
                Document hitDoc = searcher.doc(hits[i].doc);
                LuceneAddOnClass clazz =
                        new LuceneAddOnClass(type, getField(
                                QuerySupport.CLASS_NAME, hitDoc), getField(
                                QuerySupport.CLASS_FQN, hitDoc), getField(
                                QuerySupport.NAME, hitDoc), getField(
                                QuerySupport.ID, hitDoc), getSessionSeq());
                classes.add(clazz);
            }
        }
        catch (ParseException e) {
            // could happen in case bad prefix
            getLogger().log(Level.INFO, null, e);
        }
        catch (IOException e) {
            getLogger().log(Level.FINE, null, e);
        }
        finally {
            closeReadIndex(reader, searcher, directory, analyzer);
        }
        return classes;
    }

    private QuerySupport myQuerySupport;

    private static class LuceneAddOnClass extends AddOnClass {

        LuceneAddOnClass( SourceType type, String name, String fqn,
                String addonName, Object id, int seq )
        {
            super(type, name, fqn, addonName);
            myId = id;
            mySessionId = seq;
        }

        public Object getId() {
            return myId;
        }

        int getSessionId() {
            return mySessionId;
        }

        private final Object myId;

        private final int mySessionId;

    }

    private static class LuceneSearchResult extends SearchResult {

        LuceneSearchResult( String name, String rating, String updateDate,
                Object id, int seq )
        {
            super(name, rating, updateDate);
            myId = id;
            mySessionId = seq;
        }

        public Object getId() {
            return myId;
        }

        int getSessionId() {
            return mySessionId;
        }

        private final Object myId;

        private final int mySessionId;
    }

}
