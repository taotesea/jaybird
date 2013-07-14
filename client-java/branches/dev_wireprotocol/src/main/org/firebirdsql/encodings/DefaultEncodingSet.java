/*
 * $Id$
 *
 * Public Firebird Java API.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    1. Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.firebirdsql.encodings;

import org.firebirdsql.encodings.xml.Encodings;
import org.firebirdsql.logging.Logger;
import org.firebirdsql.logging.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The default encoding set for Jaybird.
 * <p>
 * This {@link EncodingSet} loads the definitions from the file <code>default-firebird-encodings.xml</code> in
 * <code>org.firebirdsql.encodings</code>
 * </p>
 * <p>
 * This class can be subclassed to load other definitions
 * </p>
 *
 * @author <a href="mailto:mrotteveel@users.sourceforge.net">Mark Rotteveel</a>
 * @since 2.3
 */
public class DefaultEncodingSet implements EncodingSet {

    private static final Logger logger = LoggerFactory.getLogger(DefaultEncodingSet.class, false);
    private List<EncodingDefinition> encodingDefinitions = null;

    @Override
    public int getPreferenceWeight() {
        return 0;
    }

    @Override
    public final synchronized List<EncodingDefinition> getEncodings() {
        if (encodingDefinitions == null) {
            encodingDefinitions = createEncodingDefinitions(getXmlResourceName());
        }
        return encodingDefinitions;
    }

    /**
     * Relative or absolute resource reference to the xml file to load.
     *
     * @return Path of the XML file
     * @see #loadEncodingsFromXml(String)
     */
    protected String getXmlResourceName() {
        return "default-firebird-encodings.xml";
    }

    /**
     * Loads the {@link Encodings} from the specified file.
     * <p>
     * The loaded file must conform to the <code>http://www.firebirdsql.org/schemas/Jaybird/encodings/1</code> schema
     * (as found in <code>org/firebirdsql/encodings/xml/encodings.xsd</code>)
     * </p>
     * <p>
     * This file is loading using <code>getClass().getResourceAsStream(xmlFileResource)</code>
     * </p>
     *
     * @param xmlFileResource
     *         Absolute or relative path of the resource containing the encodings definition
     * @return Loaded encodings, or <code>null</code> if the resource could not be found
     * @throws JAXBException
     *         For errors unmarshalling the objects from the XML file
     */
    protected final Encodings loadEncodingsFromXml(String xmlFileResource) throws JAXBException {
        InputStream inputStream = null;
        try {
            JAXBContext ctx = JAXBContext.newInstance(Encodings.class);
            Unmarshaller unmarshaller = ctx.createUnmarshaller();
            inputStream = getClass().getResourceAsStream(xmlFileResource);
            if (inputStream == null) {
                logger.fatal(String.format("The encoding definition file %s was not found", xmlFileResource));
                return null;
            }
            return (Encodings) unmarshaller.unmarshal(inputStream);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Creates all encodings listed in xmlFileResource.
     *
     * @param xmlFileResource
     *         Absolute or relative path of the resource containing the encodings definition
     * @return List of {@link Encoding} instances
     * @see #loadEncodingsFromXml(String)
     */
    protected final List<EncodingDefinition> createEncodingDefinitions(String xmlFileResource) {
        try {
            Encodings encodings = loadEncodingsFromXml(xmlFileResource);
            if (encodings == null) {
                return Collections.emptyList();
            }
            List<EncodingDefinition> encodingSet = new ArrayList<EncodingDefinition>();
            for (Encodings.EncodingDefinition definition : encodings.getEncodingDefinition()) {
                final EncodingDefinition encoding = createEncodingDefinition(definition);
                if (encoding != null) {
                    encodingSet.add(encoding);
                }
            }
            return encodingSet;
        } catch (JAXBException e) {
            logger.fatal(String.format("Error loading encoding definition from %s", xmlFileResource), e);
            return Collections.emptyList();
        }
    }

    /**
     * Creates an {@link Encoding} for the <code>definition</code>
     *
     * @param definition
     *         XML definition of the encoding
     * @return Encoding instance or <code>null</code> if creating the instance failed for any reason.
     */
    protected EncodingDefinition createEncodingDefinition(final Encodings.EncodingDefinition definition) {
        try {
            if (definition.getEncodingDefinitionImplementation() != null) {
                return createEncodingDefinitionImplementation(definition);
            } else {
                try {
                    final Charset charset = definition.getJavaName() != null ? Charset.forName(definition.getJavaName()) : null;
                    return new DefaultEncodingDefinition(definition.getFirebirdName(), charset, definition.getMaxBytesPerCharacter(), definition.getCharacterSetId(), definition.isFirebirdOnly());
                } catch (IllegalCharsetNameException e) {
                    logger.warn(String.format("javaName=\"%s\" specified for encoding \"%s\" is an illegal character set name",
                            definition.getJavaName(), definition.getFirebirdName()), e);
                } catch (UnsupportedCharsetException e) {
                    logger.warn(String.format("javaName=\"%s\" specified for encoding \"%s\" is an illegal character set name",
                            definition.getJavaName(), definition.getFirebirdName()), e);
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("Loading information for encoding \"%s\" failed with an Exception", definition.getFirebirdName()), e);
        }
        return null;
    }

    /**
     * Creates an instance of {@link EncodingDefinition} by creating an instance of the class specified by
     * encodingDefinitionImplementation in
     * the xml definition.
     *
     * @param definition
     *         XML definition of the encoding
     * @return Instance of Encoding, or <code>null</code> if the specified class could not be loaded or did not meet
     *         the
     *         expectations.
     */
    protected EncodingDefinition createEncodingDefinitionImplementation(final Encodings.EncodingDefinition definition) {
        assert definition.getEncodingDefinitionImplementation() != null;
        try {
            final Class<?> encodingClazz = Class.forName(definition.getEncodingDefinitionImplementation());
            if (!EncodingDefinition.class.isAssignableFrom(encodingClazz)) {
                logger.warn(String.format("encodingDefinitionImplementation=\"%s\" specified for encoding \"%s\" is not an implementation of org.firebirdsql.encodings.EncodingDefinition",
                        definition.getEncodingDefinitionImplementation(), definition.getFirebirdName()));
            }

            final EncodingDefinition encoding = (EncodingDefinition) encodingClazz.newInstance();
            if (encoding.getFirebirdEncodingName().equals(definition.getFirebirdName())) {
                return encoding;
            } else {
                logger.warn(String.format("Property value FirebirdEncodingName \"%s\" of encodingDefinitionImplementation=\"%s\" specified for encoding \"%s\" does not match",
                        encoding.getFirebirdEncodingName(), definition.getEncodingDefinitionImplementation(), definition.getFirebirdName()));
                return null;
            }
        } catch (ClassNotFoundException e) {
            logger.warn(String.format("encodingDefinitionImplementation=\"%s\" specified for encoding \"%s\" could not be found",
                    definition.getEncodingDefinitionImplementation(), definition.getFirebirdName()), e);
        } catch (InstantiationException e) {
            logger.warn(String.format("encodingDefinitionImplementation=\"%s\" specified for encoding \"%s\" is abstract or does not have a no-arg constructor",
                    definition.getEncodingDefinitionImplementation(), definition.getFirebirdName()), e);
        } catch (IllegalAccessException e) {
            logger.warn(String.format("encodingDefinitionImplementation=\"%s\" specified for encoding \"%s\" or its constructor is not accessible",
                    definition.getEncodingDefinitionImplementation(), definition.getFirebirdName()), e);
        }
        return null;
    }
}
