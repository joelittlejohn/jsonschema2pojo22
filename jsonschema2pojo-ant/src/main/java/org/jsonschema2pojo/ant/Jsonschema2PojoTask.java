/**
 * Copyright © 2010-2014 Nokia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jsonschema2pojo.ant;

import static org.apache.commons.lang3.StringUtils.*;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.jsonschema2pojo.AllFileFilter;
import org.jsonschema2pojo.AnnotationStyle;
import org.jsonschema2pojo.Annotator;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jsonschema2Pojo;
import org.jsonschema2pojo.NoopAnnotator;
import org.jsonschema2pojo.SourceType;
import org.jsonschema2pojo.URLProtocol;
import org.jsonschema2pojo.rules.RuleFactory;
import org.jsonschema2pojo.util.URLUtil;

/**
 * When invoked, this task reads one or more
 * <a href="http://json-schema.org/">JSON Schema</a> documents and generates DTO
 * style Java classes for data binding.
 * <p>
 * See <a href=
 * 'http://jsonschema2pojo.googlecode.com'>jsonschema2pojo.googlecode.com</a>.
 *
 * @see <a href="http://ant.apache.org/manual/develop.html">Writing Your Own
 *      Task</a>
 */
public class Jsonschema2PojoTask extends Task implements GenerationConfig {

    private boolean generateBuilders;

    private boolean includeConstructors = false;

    private boolean usePrimitives;

    private String source;

    private File targetDirectory;

    private String targetPackage;

    private boolean skip;

    private char[] propertyWordDelimiters = new char[] { '-', ' ', '_' };

    private boolean useLongIntegers;

    private boolean useDoubleNumbers = true;

    private boolean useBigDecimals = false;

    private boolean includeHashcodeAndEquals = true;

    private boolean includeToString = true;

    private AnnotationStyle annotationStyle = AnnotationStyle.JACKSON;

    private Class<? extends Annotator> customAnnotator = NoopAnnotator.class;

    private Class<? extends RuleFactory> customRuleFactory = RuleFactory.class;

    private boolean includeJsr303Annotations = false;

    private SourceType sourceType = SourceType.JSONSCHEMA;

    private Path classpath;

    private boolean removeOldOutput = false;

    private String outputEncoding = "UTF-8";

    private boolean useJodaDates = false;

    private boolean useJodaLocalDates = false;

    private boolean useJodaLocalTimes = false;

    private boolean useCommonsLang3 = false;

    private boolean parcelable = false;

    private boolean serializable = false;

    private boolean initializeCollections = true;

    private String classNamePrefix = "";

    private String classNameSuffix = "";

    private boolean constructorsRequiredPropertiesOnly = false;

    private boolean includeAdditionalProperties = true;

    private boolean includeAccessors = true;

    private String targetVersion = "1.6";

    private boolean includeDynamicAccessors = false;

    private String dateTimeType = null;

    private String timeType = null;

    private String dateType = null;


    /**
     * Execute this task (it's expected that all relevant setters will have been
     * called by Ant to provide task configuration <em>before</em> this method
     * is called).
     *
     * @throws BuildException
     *             if this task cannot be completed due to some error reading
     *             schemas, generating types or writing output .java files.
     */
    @Override
    public void execute() throws BuildException {

        if (skip) {
            return;
        }

        if (source == null) {
            log("source attribute is required but was not set");
            return;
        }

        // attempt to parse the url
        URL sourceURL;
        try {
            sourceURL = URLUtil.parseURL(source);
        } catch (IllegalArgumentException e) {
            log(String.format("Invalid schema source provided: %s", source));
            return;
        }

        // if url is a file, ensure it exists
        if (URLUtil.parseProtocol(sourceURL.toString()) == URLProtocol.FILE) {
            File sourceFile = new File(sourceURL.getFile());
            if (!sourceFile.exists()) {
                log(sourceFile.getAbsolutePath() + " cannot be found");
                return;
            }
        }

        if (targetDirectory == null) {
            log("targetDirectory attribute is required but was not set");
            return;
        }

        ClassLoader extendedClassloader = buildExtendedClassloader();
        Thread.currentThread().setContextClassLoader(extendedClassloader);

        try {
            Jsonschema2Pojo.generate(this);
        } catch (IOException e) {
            throw new BuildException("Error generating classes from JSON Schema file(s) " + source, e);
        }
    }

    /**
     * Build a classloader using the additional elements specified in
     * <code>classpath</code> and <code>classpathRef</code>.
     *
     * @return a new classloader that includes the extra path elements found in
     *         the <code>classpath</code> and <code>classpathRef</code> config
     *         values
     */
    private ClassLoader buildExtendedClassloader() {
        final List<URL> classpathUrls = new ArrayList<URL>();
        for (String pathElement : getClasspath().list()) {
            try {
                classpathUrls.add(new File(pathElement).toURI().toURL());
            } catch (MalformedURLException e) {
                throw new BuildException("Unable to use classpath entry as it could not be understood as a valid URL: " + pathElement, e);
            }
        }

        final ClassLoader parentClassloader = Thread.currentThread().getContextClassLoader();

        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return new URLClassLoader(classpathUrls.toArray(new URL[classpathUrls.size()]), parentClassloader);
            }
        });
    }

    /**
     * Sets the 'includeConstructors' property of this class
     *
     * @param includeConstructors
     *            Whether to generate constructors or not.
     */
    public void setIncludeConstructors(boolean includeConstructors) {
        this.includeConstructors = includeConstructors;
    }

    /**
     * Sets the 'constructorsRequiredPropertiesOnly' property of this class.
     *
     * @param constructorsRequiredPropertiesOnly
     *            Whether generated constructors should have parameters for all
     *            properties, or only required ones.
     */
    public void setConstructorsRequiredPropertiesOnly(boolean constructorsRequiredPropertiesOnly) {
        this.constructorsRequiredPropertiesOnly = constructorsRequiredPropertiesOnly;
    }

    /**
     * Sets the 'generateBuilders' property of this class.
     *
     * @param generateBuilders
     *            Whether to generate builder-style methods of the form
     *            <code>withXxx(value)</code> (that return <code>this</code>),
     *            alongside the standard, void-return setters.
     *            <p>
     *            Default: <code>false</code>.
     */
    public void setGenerateBuilders(boolean generateBuilders) {
        this.generateBuilders = generateBuilders;
    }

    /**
     * Sets the 'usePrimitives' property of this class.
     *
     * @param usePrimitives
     *            Whether to use primitives (<code>long</code>,
     *            <code>double</code> , <code>boolean</code>) instead of wrapper
     *            types where possible when generating bean properties (has the
     *            side-effect of making those properties non-null).
     *            <p>
     *            Default: <code>false</code>.
     */
    public void setUsePrimitives(boolean usePrimitives) {
        this.usePrimitives = usePrimitives;
    }

    /**
     * Sets the 'useLongIntegers' property of this class
     *
     * @param useLongIntegers
     *            Whether to use the java type <code>long</code> (or
     *            {@link java.lang.Long}) instead of <code>int</code> (or
     *            {@link java.lang.Integer}) when representing the JSON Schema
     *            type 'integer'.
     */
    public void setUseLongIntegers(boolean useLongIntegers) {
        this.useLongIntegers = useLongIntegers;
    }

    /**
     * Sets the 'useDoubleNumbers' property of this class
     *
     * @param useDoubleNumbers
     *            Whether to use the java type <code>double</code> (or
     *            {@link java.lang.Double}) instead of <code>float</code> (or
     *            {@link java.lang.Float}) when representing the JSON Schema
     *            type 'number'.
     */
    public void setUseDoubleNumbers(boolean useDoubleNumbers) {
        this.useDoubleNumbers = useDoubleNumbers;
    }

    /**
     * Sets the 'useBigDecimals' property of this class
     *
     * @param useBigDecimals
     *            Whether to use the java type <code>BigDecimal</code>
     *            instead of <code>float</code> (or {@link java.lang.Float})
     *            when representing the JSON Schema type 'number'. Note that
     *            this overrides <code>useDoubleNumbers</code>.
     */
    public void setUseBigDecimals(boolean useBigDecimals) {
        this.useBigDecimals = useBigDecimals;
    }

    /**
     * Sets schema file (or directory containing schema files) that should be
     * used for input.
     *
     * @param source
     *            Location of the JSON Schema file(s). Note: this may refer to a
     *            single file or a directory of files.
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Sets the target (output) directory for generated source files.
     *
     * @param targetDirectory
     *            Target directory for generated Java source files.
     */
    public void setTargetDirectory(File targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    /**
     * Sets the target package for generated types.
     *
     * @param targetPackage
     *            Package name used for generated Java classes (for types where
     *            a fully qualified name has not been supplied in the schema
     *            using the 'javaType' property).
     */
    public void setTargetPackage(String targetPackage) {
        this.targetPackage = targetPackage;
    }

    /**
     * Sets the 'skip' property of this task.
     *
     * @param skip
     *            whether to skip execution of this task
     */
    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    /**
     * @param propertyWordDelimiters
     *            a string containing all of the characters that should be
     *            considered as word delimiters when creating Java Bean property
     *            names from JSON property names. If blank or not set, JSON
     *            properties will be considered to contain a single word when
     *            creating Java Bean property names.
     */
    public void setPropertyWordDelimiters(String propertyWordDelimiters) {
        this.propertyWordDelimiters = defaultString(propertyWordDelimiters).toCharArray();
    }

    /**
     * Sets the 'includeHashcodeAndEquals' property of this class
     *
     * @param includeHashcodeAndEquals
     *            Whether to include <code>hashCode</code> and
     *            <code>equals</code> methods in generated Java types.
     */
    public void setIncludeHashcodeAndEquals(boolean includeHashcodeAndEquals) {
        this.includeHashcodeAndEquals = includeHashcodeAndEquals;
    }

    /**
     * Sets the 'includeToString' property of this class
     *
     * @param includeToString
     *            Whether to include a <code>toString</code> method in generated
     *            Java types.
     */
    public void setIncludeToString(boolean includeToString) {
        this.includeToString = includeToString;
    }

    /**
     * Sets the 'annotationStyle' property of this class
     *
     * @param annotationStyle
     *            The style of annotations to use in the generated Java types.
     */
    public void setAnnotationStyle(AnnotationStyle annotationStyle) {
        this.annotationStyle = annotationStyle;
    }

    /**
     * Sets the 'customAnnotator' property of this class
     *
     * @param customAnnotator
     *            A custom annotator to use to annotate the generated types
     */
    @SuppressWarnings("unchecked")
    public void setCustomAnnotator(String customAnnotator) {
        if (isNotBlank(customAnnotator)) {
            try {
                this.customAnnotator = (Class<? extends Annotator>) Class.forName(customAnnotator);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            this.customAnnotator = NoopAnnotator.class;
        }
    }

    @SuppressWarnings("unchecked")
    public void setCustomRuleFactory(String customRuleFactory) {
        if (isNotBlank(customRuleFactory)) {
            try {
                this.customRuleFactory = (Class<? extends RuleFactory>) Class.forName(customRuleFactory);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            this.customRuleFactory = RuleFactory.class;
        }
    }

    /**
     * Sets the 'includeJsr303Annotations' property of this class
     *
     * @param includeJsr303Annotations
     *            Whether to include
     *            <a href="http://jcp.org/en/jsr/detail?id=303">JSR-303</a>
     *            annotations (for schema rules like minimum, maximum, etc) in
     *            generated Java types.
     */
    public void setIncludeJsr303Annotations(boolean includeJsr303Annotations) {
        this.includeJsr303Annotations = includeJsr303Annotations;
    }

    /**
     * Sets the 'sourceType' property of this class
     *
     * @param sourceType
     *            The type of input documents that will be read
     *            <p>
     *            Supported values:
     *            <ul>
     *            <li><code>jsonschema</code></li>
     *            <li><code>json</code></li>
     *            </ul>
     */
    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * Sets the 'removeOldOutput' property of this class
     *
     * @param removeOldOutput
     *            Whether to empty the target directory before generation
     *            occurs, to clear out all source files that have been generated
     *            previously. <strong>Be warned</strong>, when activated this
     *            option will cause jsonschema2pojo to <strong>indiscriminately
     *            delete the entire contents of the target directory (all files
     *            and folders)</strong> before it begins generating sources.
     */
    public void setRemoveOldOutput(boolean removeOldOutput) {
        this.removeOldOutput = removeOldOutput;
    }

    /**
     * Sets the 'outputEncoding' property of this class
     *
     * @param outputEncoding
     *            The character encoding that should be used when writing the
     *            generated Java source files
     */
    public void setOutputEncoding(String outputEncoding) {
        this.outputEncoding = outputEncoding;
    }

    /**
     * Sets the 'useJodaDates' property of this class
     *
     * @param useJodaDates
     *            Whether to use {@link org.joda.time.DateTime} instead of
     *            {@link java.util.Date} when adding date type fields to
     *            generated Java types.
     */
    public void setUseJodaDates(boolean useJodaDates) {
        this.useJodaDates = useJodaDates;
    }

    /**
     * Sets the 'useJodaLocalDates' property of this class
     *
     * @param useJodaLocalDates
     *            Whether to use {@link org.joda.time.LocalDate} instead of
     *            string when adding string fields of format date (not
     *            date-time) to generated Java types.
     */
    public void setUseJodaLocalDates(boolean useJodaLocalDates) {
        this.useJodaLocalDates = useJodaLocalDates;
    }

    /**
     * Sets the 'useJodaLocalTimes' property of this class
     *
     * @param useJodaLocalTimes
     *            Whether to use {@link org.joda.time.LocalTime} instead of
     *            string when adding string fields of format time (not
     *            date-time) to generated Java types.
     */
    public void setUseJodaLocalTimes(boolean useJodaLocalTimes) {
        this.useJodaLocalTimes = useJodaLocalTimes;
    }

    /**
     * Sets the 'useCommonsLang3' property of this class
     *
     * @param useCommonsLang3
     *            Whether to use commons-lang 3.x imports instead of
     *            commons-lang 2.x imports when adding equals, hashCode and
     *            toString methods.
     */
    public void setUseCommonsLang3(boolean useCommonsLang3) {
        this.useCommonsLang3 = useCommonsLang3;
    }

    /**
     * Sets the 'parcelable' property of this class
     *
     * @param parcelable
     *            Whether to make the generated types 'parcelable' (for Android
     *            development).
     */
    public void setParcelable(boolean parcelable) {
        this.parcelable = parcelable;
    }

    /**
     * Sets the 'serializable' property of this class
     *
     * @param serializable
     *            Whether to make the generated types 'serializable'.
     */
    public void setSerializable(boolean serializable) {
        this.serializable = serializable;
    }

    /**
     * Sets the 'initializeCollections' property of this class
     *
     * @param initializeCollections
     *            Whether to initialize collections with empty instance or null.
     */
    public void setInitializeCollections(boolean initializeCollections) {
        this.initializeCollections = initializeCollections;
    }

    /**
     * Sets the 'classNamePrefix' property of this class
     *
     * @param classNamePrefix
     *            Whether to add a prefix to generated classes.
     */
    public void setClassNamePrefix(String classNamePrefix) {
        this.classNamePrefix = classNamePrefix;
    }

    /**
     * Sets the 'classNameSuffix' property of this class
     *
     * @param classNameSuffix
     *            Whether to add a suffix to generated classes.
     */
    public void setClassNameSuffix(String classNameSuffix) {
        this.classNameSuffix = classNameSuffix;
    }

    /**
     * Sets the 'includeAdditionalProperties' property of this class
     *
     * @param includeAdditionalProperties
     *            Whether to allow 'additional properties' support in objects.
     *            Setting this to false will disable additional properties
     *            support, regardless of the input schema(s).
     */
    public void setIncludeAdditionalProperties(boolean includeAdditionalProperties) {
        this.includeAdditionalProperties = includeAdditionalProperties;
    }

    /**
     * Sets the 'includeAccessors' property of this class
     *
     * @param includeAccessors
     *            Whether to include getters/setters or to omit these accessor
     *            methods and create public fields instead.
     */
    public void setIncludeAccessors(boolean includeAccessors) {
        this.includeAccessors = includeAccessors;
    }

    /**
     * Sets the 'targetVersion' property of this class
     *
     * @param targetVersion
     *            The target version for generated source files.
     *            <p>
     *            Default: <code>1.6</code>.
     */
    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    /**
     * Sets the 'includeDynamicAccessors' property of this class
     *
     * @param includeDynamicAccessors
     *            Whether to include dynamic getters, setters, and builders or to omit these methods.
     */
    public void setIncludeDynamicAccessors(boolean includeDynamicAccessors) {
        this.includeDynamicAccessors = includeDynamicAccessors;
    }

    @Override
    public boolean isGenerateBuilders() {
        return generateBuilders;
    }

    @Override
    public boolean isUsePrimitives() {
        return usePrimitives;
    }

    @Override
    public Iterator<URL> getSource() {
        return Collections.singleton(URLUtil.parseURL(source)).iterator();
    }

    @Override
    public File getTargetDirectory() {
        return targetDirectory;
    }

    @Override
    public String getTargetPackage() {
        return targetPackage;
    }

    @Override
    public char[] getPropertyWordDelimiters() {
        return propertyWordDelimiters.clone();
    }

    /**
     * Should this task be skipped? (don't read schemas, don't generate types)
     *
     * @return <code>true</code> if this task is disabled
     */
    public boolean isSkip() {
        return skip;
    }

    @Override
    public boolean isUseLongIntegers() {
        return useLongIntegers;
    }

    @Override
    public boolean isUseDoubleNumbers() {
        return useDoubleNumbers;
    }

    @Override
    public boolean isIncludeHashcodeAndEquals() {
        return includeHashcodeAndEquals;
    }

    @Override
    public boolean isIncludeToString() {
        return includeToString;
    }

    @Override
    public AnnotationStyle getAnnotationStyle() {
        return annotationStyle;
    }

    @Override
    public Class<? extends Annotator> getCustomAnnotator() {
        return customAnnotator;
    }

    @Override
    public Class<? extends RuleFactory> getCustomRuleFactory() {
        return customRuleFactory;
    }

    @Override
    public boolean isIncludeJsr303Annotations() {
        return includeJsr303Annotations;
    }

    @Override
    public SourceType getSourceType() {
        return sourceType;
    }

    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath.createPath();
    }

    public void setClasspath(Path classpath) {
        if (this.classpath == null) {
            this.classpath = classpath;
        } else {
            this.classpath.append(classpath);
        }
    }

    public void setClasspathRef(Reference classpathRef) {
        createClasspath().setRefid(classpathRef);
    }

    public Path getClasspath() {
        return (classpath == null) ? new Path(getProject()) : classpath;
    }

    @Override
    public boolean isRemoveOldOutput() {
        return removeOldOutput;
    }

    @Override
    public String getOutputEncoding() {
        return outputEncoding;
    }

    @Override
    public boolean isUseJodaDates() {
        return useJodaDates;
    }

    @Override
    public boolean isUseJodaLocalDates() {
        return useJodaLocalDates;
    }

    @Override
    public boolean isUseJodaLocalTimes() {
        return useJodaLocalTimes;
    }

    @Override
    public boolean isUseCommonsLang3() {
        return useCommonsLang3;
    }

    @Override
    public boolean isParcelable() {
        return parcelable;
    }

    @Override
    public boolean isSerializable() {
        return serializable;
    }

    @Override
    public FileFilter getFileFilter() {
        return new AllFileFilter();
    }

    @Override
    public boolean isInitializeCollections() {
        return initializeCollections;
    }

    @Override
    public String getClassNamePrefix() {
        return classNamePrefix;
    }

    @Override
    public String getClassNameSuffix() {
        return classNameSuffix;
    }

    @Override
    public boolean isIncludeConstructors() {
        return includeConstructors;
    }

    @Override
    public boolean isConstructorsRequiredPropertiesOnly() {
        return constructorsRequiredPropertiesOnly;
    }

    @Override
    public boolean isIncludeAdditionalProperties() {
        return includeAdditionalProperties;
    }

    @Override
    public boolean isIncludeAccessors() {
        return includeAccessors;
    }

    @Override
    public String getTargetVersion() {
        return targetVersion;
    }

    @Override
    public boolean isIncludeDynamicAccessors() {
        return includeDynamicAccessors;
    }

    @Override
    public String getDateTimeType() {
        return dateTimeType;
    }

    @Override
    public String getDateType() {
        return dateType;
    }

    @Override
    public String getTimeType() {
        return timeType;
    }

    @Override
    public boolean isUseBigDecimals() {
        return useBigDecimals;
    }
}
