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
package com.vaadin.processor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * @author denis
 */
@SupportedAnnotationTypes(CachedResourceProcessor.CACHED_RESOURCE)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class CachedResourceProcessor extends AbstractProcessor {

    static final String CACHED_RESOURCE =
            "org.vaadin.netbeans.retriever.CachedResource"; // NOI18N

    private static final String URL = "url"; // NOI18N

    private static final String PATH = "resourcePath"; // NOI18N 

    private static final String UTF_8 = "UTF-8"; // NOI18N

    @Override
    public synchronized void init( ProcessingEnvironment env ) {
        super.init(env);
        myProcessingEnv = env;
    }

    @Override
    public boolean process( Set<? extends TypeElement> annotations,
            RoundEnvironment environment )
    {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> elements =
                    environment.getElementsAnnotatedWith(annotation);
            for (Element element : elements) {
                PackageElement pkg =
                        myProcessingEnv.getElementUtils().getPackageOf(element);
                AnnotationMirror annotationMirror =
                        getCachedAnnotation(element, annotation);
                String url = getValue(annotationMirror, URL);
                String path = getValue(annotationMirror, PATH);

                generateResource(url, pkg.getQualifiedName().toString(), path,
                        element);
            }
        }
        return true;
    }

    private void generateResource( String url, String pkg, String path,
            Element element )
    {
        String content = fetch(url);
        if (content == null) {
            myProcessingEnv.getMessager().printMessage(
                    Kind.ERROR,
                    "Resource " + path
                            + " hasn't been generated in the package " + pkg
                            + " for URL '" + url + "'"); // NOI18N
            return;
        }
        Filer filer = myProcessingEnv.getFiler();
        Writer writer = null;
        try {
            FileObject fileObject =
                    filer.createResource(StandardLocation.CLASS_OUTPUT, pkg,
                            path, element);
            writer = fileObject.openWriter();
            writer.write(content);
        }
        catch (IOException e) {
            myProcessingEnv.getMessager().printMessage(Kind.ERROR,
                    e.getMessage());
        }
        finally {
            if (writer != null) {
                try {
                    writer.close();
                }
                catch (IOException e) {
                    myProcessingEnv.getMessager().printMessage(Kind.WARNING,
                            e.getMessage());
                }
            }
        }
    }

    private AnnotationMirror getCachedAnnotation( Element element,
            TypeElement annotationElement )
    {
        List<? extends AnnotationMirror> annotations =
                myProcessingEnv.getElementUtils().getAllAnnotationMirrors(
                        element);
        for (AnnotationMirror annotation : annotations) {
            if (annotationElement.equals(annotation.getAnnotationType()
                    .asElement()))
            {
                return annotation;
            }
        }
        return null;
    }

    private String getValue( AnnotationMirror annotation, String key ) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> values =
                annotation.getElementValues();
        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values
                .entrySet())
        {
            ExecutableElement method = entry.getKey();
            if (method.getSimpleName().contentEquals(key)) {
                AnnotationValue value = entry.getValue();
                if (value != null && value.getValue() != null) {
                    return value.getValue().toString();
                }
            }
        }
        return null;
    }

    private String fetch( String uri ) {
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(uri);
            URLConnection connection = url.openConnection();
            connection.connect();
            inputStream = connection.getInputStream();
            StringBuilder builder = new StringBuilder();

            reader =
                    new BufferedReader(
                            new InputStreamReader(inputStream, UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }

            return builder.toString();
        }
        catch (IOException e) {
            myProcessingEnv.getMessager().printMessage(Kind.ERROR,
                    e.getMessage());
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    myProcessingEnv.getMessager().printMessage(Kind.WARNING,
                            e.getMessage());
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    myProcessingEnv.getMessager().printMessage(Kind.WARNING,
                            e.getMessage());
                }
            }
        }
        return null;
    }

    private ProcessingEnvironment myProcessingEnv;
}
