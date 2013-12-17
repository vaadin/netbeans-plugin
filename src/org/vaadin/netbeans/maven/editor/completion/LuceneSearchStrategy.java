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
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.AbstractField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.openide.modules.Places;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.Builder;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.License;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.Maturity;
import org.vaadin.netbeans.maven.editor.completion.SourceClass.SourceType;
import org.vaadin.netbeans.retriever.AbstractRetriever;

/**
 * @author denis
 */
class LuceneSearchStrategy implements SearchStrategy {

    private static final String ADD_ON_INDEX = "addOnIndex"; // NOI18N

    private static final Logger LOG = Logger
            .getLogger(LuceneSearchStrategy.class.getName());

    private static final String NAME = "name"; // NOI18N

    private static final String CLASS_NAME = "className"; // NOI18N

    private static final String CLASS_FQN = "classFqn"; // NOI18N

    private static final String URL = "url"; // NOI18N

    private static final String DESCRIPTION = "description"; // NOI18N 

    private static final String RATING = "rating"; // NOI18N

    private static final String ARTIFACT_ID = "artifactId"; // NOI18N

    private static final String GROUP_ID = "groupId"; // NOI18N

    private static final String VERSION = "version"; // NOI18N

    private static final String MATURITY = "maturity"; // NOI18N

    private static final String SOURCE_TYPE = "sourceType"; // NOI18N

    private static final String FREE = "free"; // NOI18N

    private static final String DOC_TYPE = "type"; // NOI18N

    private static final String ID = "id"; // NOI18N

    private enum DocType {
        CLASS,
        LICENSE,
        INFO;
    }

    LuceneSearchStrategy() {
        myLock = new ReentrantReadWriteLock();
        File vaadinFolder =
                Places.getCacheSubdirectory(AbstractRetriever.VAADIN);
        myIndexDir = new File(vaadinFolder, ADD_ON_INDEX);
    }

    @Override
    public Collection<? extends AddOnClass> searchClasses( String prefix,
            SourceType type )
    {
        myLock.readLock().lock();
        try {
            if (isInitialized()) {
                return doSearchClasses(prefix, type, null);
            }
            else {
                return Collections.emptyList();
            }
        }
        finally {
            myLock.readLock().unlock();
        }
    }

    @Override
    public AddOnDoc getDoc( AddOnClass clazz ) {
        myLock.readLock().lock();
        try {
            if (isInitialized()) {
                return doGetDoc(clazz);
            }
            else {
                return null;
            }
        }
        finally {
            myLock.readLock().unlock();
        }
    }

    void index( Collection<AddOn> addons ) {
        myLock.writeLock().lock();
        try {
            doIndex(addons);
        }
        finally {
            myLock.writeLock().unlock();
        }
    }

    boolean isInitialized() {
        return myIndexDir.exists();
    }

    private AddOnDoc doGetDoc( AddOnClass clazz ) {
        LuceneAddOnClass addOnClass = null;
        if (clazz instanceof LuceneAddOnClass) {
            addOnClass = (LuceneAddOnClass) clazz;
        }
        else {
            // in rare case <code>clazz</code> could be created by other strategy
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
        assert addOnClass != null;

        AddOnDoc result =
                new AddOnDoc(addOnClass.getAddOnName(), addOnClass.getName(),
                        addOnClass.getQualifiedName());
        IndexReader reader = null;
        IndexSearcher searcher = null;
        Directory directory = null;
        Analyzer analyzer = null;
        try {
            analyzer = new StandardAnalyzer(Version.LUCENE_35);
            directory = FSDirectory.open(myIndexDir);
            reader = IndexReader.open(directory);
            searcher = new IndexSearcher(reader);

            Query query = createDocQuery(addOnClass, analyzer);
            result = searchDoc(result, searcher, query);
        }
        catch (ParseException e) {
            LOG.log(Level.FINE, null, e);
        }
        catch (IOException e) {
            LOG.log(Level.FINE, null, e);
        }
        finally {
            closeReadIndex(reader, searcher, directory, analyzer);
        }
        return result;
    }

    private AddOnDoc searchDoc( AddOnDoc result, IndexSearcher searcher,
            Query query ) throws IOException, CorruptIndexException
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

        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = searcher.doc(hits[i].doc);
            String type = getField(DOC_TYPE, hitDoc);
            if (DocType.LICENSE.toString().equals(type)) {
                licenses.add(buildLicense(hitDoc));
            }
            else if (DocType.INFO.toString().equals(type)) {
                assert !infoFound;
                infoFound = true;
                artifactId = getField(ARTIFACT_ID, hitDoc);
                groupId = getField(GROUP_ID, hitDoc);
                description = getField(DESCRIPTION, hitDoc);
                maturity = getField(MATURITY, hitDoc);
                version = getField(VERSION, hitDoc);
                rating = getField(RATING, hitDoc);
                url = getField(URL, hitDoc);
            }
        }
        assert infoFound;
        Builder<AddOnDoc> builder = new Builder<>(AddOnDoc.class);
        result =
                builder.build(result, groupId, artifactId, version,
                        description, rating, url, maturity,
                        fillLicenses(licenses, groupId, artifactId, version));
        return result;
    }

    private Query createDocQuery( LuceneAddOnClass clazz, Analyzer analyzer )
            throws ParseException
    {
        TermQuery infoQuery =
                new TermQuery(new Term(DOC_TYPE, DocType.INFO.toString()));
        TermQuery licenseQuery =
                new TermQuery(new Term(DOC_TYPE, DocType.LICENSE.toString()));
        BooleanQuery typeQuery = new BooleanQuery();
        typeQuery.add(infoQuery, Occur.SHOULD);
        typeQuery.add(licenseQuery, Occur.SHOULD);

        TermQuery nameQuery =
                new TermQuery(new Term(ID, clazz.getId().toString()));

        BooleanQuery query = new BooleanQuery();
        query.add(nameQuery, Occur.MUST);
        query.add(typeQuery, Occur.MUST);

        QueryParser parser = new QueryParser(Version.LUCENE_35, NAME, analyzer);
        Query docQuery = parser.parse(query.toString());
        return docQuery;
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
        return new License(Boolean.valueOf(getField(FREE, doc)), getField(NAME,
                doc), getField(URL, doc));
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
            directory = FSDirectory.open(myIndexDir);
            reader = IndexReader.open(directory);
            searcher = new IndexSearcher(reader);

            Query query = createClassesQuery(prefix, type, fqn, analyzer);
            ScoreDoc[] hits =
                    searcher.search(query, Integer.MAX_VALUE).scoreDocs;
            for (int i = 0; i < hits.length; i++) {
                Document hitDoc = searcher.doc(hits[i].doc);
                LuceneAddOnClass clazz =
                        new LuceneAddOnClass(type,
                                getField(CLASS_NAME, hitDoc), getField(
                                        CLASS_FQN, hitDoc), getField(NAME,
                                        hitDoc), getField(ID, hitDoc));
                classes.add(clazz);
            }
        }
        catch (ParseException e) {
            // could happen in case bad prefix
            LOG.log(Level.INFO, null, e);
        }
        catch (IOException e) {
            LOG.log(Level.FINE, null, e);
        }
        finally {
            closeReadIndex(reader, searcher, directory, analyzer);
        }
        return classes;
    }

    private Query createClassesQuery( String prefix, SourceType type,
            String fqn, Analyzer analyzer ) throws ParseException
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

    private String getField( String name, Document document ) {
        return document.get(name);
    }

    private void closeReadIndex( IndexReader reader, IndexSearcher searcher,
            Directory directory, Analyzer analyzer )
    {
        if (reader != null) {
            try {
                reader.close();
            }
            catch (IOException e) {
                LOG.log(Level.FINE, null, e);
            }
        }
        if (searcher != null) {
            try {
                searcher.close();
            }
            catch (IOException e) {
                LOG.log(Level.FINE, null, e);
            }
        }
        if (directory != null) {
            try {
                directory.close();
            }
            catch (IOException e) {
                LOG.log(Level.FINE, null, e);
            }
        }
        if (analyzer != null) {
            analyzer.close();
        }
    }

    private void doIndex( Collection<AddOn> addons ) {
        Directory directory = null;
        IndexWriter writer = null;
        Analyzer analyzer = null;
        try {
            if (!myIndexDir.exists() && !myIndexDir.mkdirs()) {
                LOG.log(Level.WARNING, "Unable to create index directory"); // NOI18N
                return;
            }
            analyzer = new StandardAnalyzer(Version.LUCENE_35);

            directory = FSDirectory.open(myIndexDir);
            IndexWriterConfig conf =
                    new IndexWriterConfig(Version.LUCENE_35, analyzer);
            conf.setOpenMode(OpenMode.CREATE);
            writer = new IndexWriter(directory, conf);
            writer.deleteAll();

            int i = 0;
            for (AddOn addon : addons) {
                Collection<Document> docs = createDocument(addon, i);
                for (Document document : docs) {
                    writer.addDocument(document);
                }
                writer.commit();
                i++;
            }
        }
        catch (IOException e) {
            LOG.log(Level.INFO, null, e);
        }
        finally {
            if (writer != null) {
                try {
                    writer.close();
                }
                catch (IOException e) {
                    LOG.log(Level.FINE, null, e);
                }
            }
            if (directory != null) {
                try {
                    directory.close();
                }
                catch (IOException e) {
                    LOG.log(Level.FINE, null, e);
                }
            }
            if (analyzer != null) {
                analyzer.close();
            }
        }
    }

    private Collection<Document> createDocument( AddOn addon, int id ) {
        List<Document> result = new LinkedList<>();

        List<SourceClass> classes = addon.getClasses();
        for (SourceClass clazz : classes) {
            result.add(createClassDocument(addon, clazz, id));
        }

        List<License> licenses = addon.getLicenses();
        for (License license : licenses) {
            Document document = createLicenseDocument(addon, license, id);
            if (document != null) {
                result.add(document);
            }
        }

        result.add(createInfoDocument(addon, id));

        return result;
    }

    private Document createInfoDocument( AddOn addon, int id ) {
        Document doc = new Document();

        String artifactId = addon.getArtifactId();
        String groupId = addon.getGroupId();
        String description = addon.getDescription();
        Maturity maturity = addon.getMaturity();
        String version = addon.getMavenVersion();
        String rating = addon.getRating();
        String url = addon.getUrl();

        add(doc, new IndexedField(NAME, addon.getName()));
        add(doc, new IndexedField(ID, String.valueOf(id)));
        add(doc, new StoredField(ARTIFACT_ID, artifactId));
        add(doc, new StoredField(GROUP_ID, groupId));
        add(doc, new IndexedField(DESCRIPTION, description));
        add(doc,
                new IndexedField(MATURITY, maturity == null ? null : maturity
                        .toString()));
        add(doc, new StoredField(VERSION, version));
        add(doc, new StoredField(RATING, rating));
        add(doc, new StoredField(URL, url));
        add(doc, new IndexedField(DOC_TYPE, DocType.INFO.toString()));

        return doc;
    }

    private Document createLicenseDocument( AddOn addon, License license, int id )
    {
        String name = license.getName();
        String url = license.getUrl();

        String artifactId = license.getArtifactId();
        String groupId = license.getGroupId();
        boolean isFree = license.isFree();

        Document doc = new Document();

        add(doc, new IndexedField(ID, String.valueOf(id), false));
        add(doc, new StoredField(NAME, name));
        add(doc, new StoredField(URL, url));
        if (!add(doc, new StoredField(ARTIFACT_ID, artifactId))
                && addon.getArtifactId() == null)
        {
            return null;
        }
        if (!add(doc, new StoredField(GROUP_ID, groupId))
                && addon.getGroupId() == null)
        {
            return null;
        }
        add(doc, new IndexedField(FREE, Boolean.valueOf(isFree).toString()));
        add(doc, new IndexedField(DOC_TYPE, DocType.LICENSE.toString()));

        return doc;
    }

    private Document createClassDocument( AddOn addon, SourceClass clazz, int id )
    {
        String name = clazz.getName();
        String fqn = clazz.getQualifiedName();
        SourceType type = clazz.getType();
        Document doc = new Document();

        add(doc, new IndexedField(ID, String.valueOf(id)));
        add(doc, new StoredField(NAME, addon.getName()));
        add(doc, new IndexedField(CLASS_NAME, name));
        add(doc, new IndexedField(CLASS_FQN, fqn));
        add(doc, new IndexedField(DOC_TYPE, DocType.CLASS.toString()));
        add(doc,
                new IndexedField(SOURCE_TYPE, type == null ? null : type
                        .toString()));
        return doc;
    }

    private boolean add( Document doc, Fieldable field ) {
        if (field instanceof StoredField) {
            StoredField stored = (StoredField) field;
            if (!stored.isEmpty()) {
                doc.add(field);
                return true;
            }
        }
        else {
            doc.add(field);
            return true;
        }
        return false;
    }

    private ReentrantReadWriteLock myLock;

    private File myIndexDir;

    private static class StoredField extends AbstractField {

        StoredField( String name, String value ) {
            this.name = name;
            fieldsData = value;

            isStored = true;
        }

        @Override
        public Reader readerValue() {
            return null;
        }

        @Override
        public String stringValue() {
            return fieldsData.toString();
        }

        @Override
        public TokenStream tokenStreamValue() {
            return null;
        }

        boolean isEmpty() {
            return fieldsData == null;
        }

    }

    private static class IndexedField extends StoredField {

        IndexedField( String name, String value ) {
            super(name, value);

            isIndexed = true;
            isTokenized = true;
        }

        IndexedField( String name, String value, boolean stored ) {
            this(name, value);

            isStored = stored;
        }

    }

    private static class LuceneAddOnClass extends AddOnClass {

        LuceneAddOnClass( SourceType type, String name, String fqn,
                String addonName, Object id )
        {
            super(type, name, fqn, addonName);
            myId = id;
        }

        public Object getId() {
            return myId;
        }

        private final Object myId;

    }

}
