/*
 * Firebird Open Source J2ee connector - jdbc driver
 *
 * Distributable under LGPL license.
 * You may obtain a copy of the License at http://www.gnu.org/copyleft/lgpl.html
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * LGPL License for more details.
 *
 * This file was created by members of the firebird development team.
 * All individual contributions remain the Copyright (C) of those
 * individuals.  Contributors to this file are either listed here or
 * can be obtained from a CVS history command.
 *
 * All rights reserved.
 */

package org.firebirdsql.jdbc;


import java.sql.*;
import java.util.*;

import javax.resource.ResourceException;

import org.firebirdsql.gds.GDSType;
import org.firebirdsql.gds.ISCConstants;
import org.firebirdsql.jca.*;
import org.firebirdsql.logging.Logger;
import org.firebirdsql.logging.LoggerFactory;

/**
 * Describe class <code>FBDriver</code> here.
 *
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @version 1.0
 */
public class FBDriver implements Driver {

    private final static Logger log;

    public static final String FIREBIRD_PROTOCOL = "jdbc:firebirdsql:";
    public static final String FIREBIRD_PROTOCOL_NATIVE= FIREBIRD_PROTOCOL + "native:";
    public static final String FIREBIRD_PROTOCOL_NATIVE_EMBEDDED = FIREBIRD_PROTOCOL + "embedded";
    public static final String FIREBIRD_PROTOCOL_NATIVE_LOCAL = FIREBIRD_PROTOCOL + "local:";
    public static final String FIREBIRD_PROTOCOL_ORACLE_MODE = FIREBIRD_PROTOCOL + "oracle:";

    public static final String CHARSET = "charSet";
    public static final String USE_TRANSLATION = "useTranslation";
    public static final String USER = "user";
    public static final String USER_NAME = "user_name";
    public static final String PASSWORD = "password";
    public static final String DATABASE = "database";
    public static final String BLOB_BUFFER_LENGTH = "blob_buffer_length";
    public static final String TPB_MAPPING = "tpb_mapping";
    

    /**
     * @todo implement the default subject for the
     * standard connection.
     */

    private Map mcfToDataSourceMap = new HashMap();

    static{
       log = LoggerFactory.getLogger(FBDriver.class,false);
        try{
            DriverManager.registerDriver(new FBDriver());
        } catch(Exception ex) {
           if (log!=null) log.error("Could not register with driver manager", ex);
        }
    }


    /**
     * Attempts to make a database connection to the given URL.
     * The driver should return "null" if it realizes it is the wrong kind
     * of driver to connect to the given URL.  This will be common, as when
     * the JDBC driver manager is asked to connect to a given URL it passes
     * the URL to each loaded driver in turn.
     *
     * <P>The driver should raise a SQLException if it is the right
     * driver to connect to the given URL, but has trouble connecting to
     * the database.
     *
     * <P>The java.util.Properties argument can be used to passed arbitrary
     * string tag/value pairs as connection arguments.
     * Normally at least "user" and "password" properties should be
     * included in the Properties.
     *
     * @param url the URL of the database to which to connect
     * @param info a list of arbitrary string tag/value pairs as
     * connection arguments. Normally at least a "user" and
     * "password" property should be included.
     * @return a <code>Connection</code> object that represents a
     *         connection to the URL
     * @exception SQLException if a database access error occurs
     */
    public Connection connect(String url, Properties info)
        throws SQLException
    {
        if (url == null || !url.startsWith(FIREBIRD_PROTOCOL))
        {
            return null;
        } // end of if ()

        final GDSType type = getDriverType(url);

        Integer blobBufferLength = null;
        try {
            int iQuestionMark = url.indexOf("?");
            if (iQuestionMark > -1) {
                if(info == null) info = new Properties();
                String propString = url.substring(iQuestionMark+1);
                StringTokenizer st = new StringTokenizer(propString,"&");
                while(st.hasMoreTokens()) {
                    String propertyString = st.nextToken();
                    int iIs = propertyString.indexOf("=");
                    if(iIs > -1) {
                        String property = propertyString.substring(0, iIs);
                        String value = propertyString.substring(iIs+1);
                        info.setProperty(property,value);
                        if (property.equals(BLOB_BUFFER_LENGTH)) 
                        {
                            try 
                            {
                                blobBufferLength = new Integer(value);
                            }
                            catch (NumberFormatException e)
                            {
                                throw new FBSQLException(
                                    "Blob buffer length " + value + 
                                    " could not be converted to an integer",
                                    FBSQLException.SQL_STATE_INVALID_CONN_ATTR);
                            }
                            
                        } 
                        
                    } else {
                        info.setProperty(propertyString, "");
                    }
                }
                url = url.substring(0,iQuestionMark);
            }


            FBManagedConnectionFactory mcf = new FBManagedConnectionFactory(type);
            
            FBConnectionRequestInfo conCri =
                FBConnectionHelper.getCri(info, mcf.getDefaultConnectionRequestInfo());

            handleEncoding(info, conCri);

            FBTpbMapper tpbMapper = FBConnectionHelper.getTpbMapper(info);

            // extract the user
            String user = info.getProperty(USER);

            if (user == null)
                user = conCri.getStringProperty(ISCConstants.isc_dpb_user_name);

            if (user == null)
                throw new FBSQLException(
                    "User for database connection not specified.",
                    FBSQLException.SQL_STATE_INVALID_CONN_ATTR);

            // extract the password
            String password = info.getProperty(PASSWORD);

            if (password == null)
                password = conCri.getStringProperty(ISCConstants.isc_dpb_password);

            if (password == null)
                throw new FBSQLException(
                    "Password for database connection not specified.",
                    FBSQLException.SQL_STATE_INVALID_CONN_ATTR);
            
            // extract the database URL

            String databaseURL;
            if( url.startsWith(FIREBIRD_PROTOCOL_NATIVE) )
                databaseURL = url.substring(FIREBIRD_PROTOCOL_NATIVE.length());
            else if( url.startsWith(FIREBIRD_PROTOCOL_NATIVE_EMBEDDED) )
                databaseURL = url.substring(FIREBIRD_PROTOCOL_NATIVE_EMBEDDED.length()+1);
            else if (url.startsWith(FIREBIRD_PROTOCOL_NATIVE_LOCAL))
                databaseURL = url.substring(FIREBIRD_PROTOCOL_NATIVE_LOCAL.length());
            else if (url.startsWith(FIREBIRD_PROTOCOL_ORACLE_MODE))
                databaseURL = url.substring(FIREBIRD_PROTOCOL_ORACLE_MODE.length());
            else
                databaseURL = url.substring(FIREBIRD_PROTOCOL.length());

            mcf.setDatabase(databaseURL);
            mcf.setConnectionRequestInfo(conCri);
            
            if (tpbMapper != null)
                mcf.setTpbMapper(tpbMapper);
                
            if (blobBufferLength != null) 
            {
                mcf.setBlobBufferLength(blobBufferLength);                
            } // end of if ()
            mcf = mcf.canonicalize();

            FBDataSource dataSource = null;
            synchronized (mcfToDataSourceMap)
            {
                dataSource = (FBDataSource)mcfToDataSourceMap.get(mcf);
                
                if (dataSource == null) {
                    dataSource = (FBDataSource)mcf.createConnectionFactory();
                    mcfToDataSourceMap.put(mcf, dataSource);
                } // end of if ()
            }

            return dataSource.getConnection(user, password);
        } catch(ResourceException resex) {
            throw new FBSQLException(resex);
        }
    }


    /**
     * Handle character encoding parameters. This method ensures that both
     * java encoding an client connection encodings are correctly set. 
     * Additionally method handles the character translation stuff.
     * 
     * @param info connection properties
     * @param cri mapping connection request info.
     * 
     * @throws SQLException if both isc_dpb_local_encoding and charSet are
     * specified.
     */
    private void handleEncoding(Properties info, FBConnectionRequestInfo cri) throws SQLException {
        String iscEncoding = cri.getStringProperty(ISCConstants.isc_dpb_lc_ctype);
        String localEncoding = cri.getStringProperty(ISCConstants.isc_dpb_local_encoding);
        String charSet = info.getProperty(CHARSET);
        
        if (localEncoding != null && charSet != null && !localEncoding.equals(charSet))
            throw new FBSQLException("Property charSet is an alias to " +
                    "isc_dpb_local_encoding, but specified values are different.");

        // charSet is actually an alias for isc_dpb_local_encoding
        if (localEncoding == null && charSet != null) {
            localEncoding = charSet;
            cri.setProperty(ISCConstants.isc_dpb_local_encoding, localEncoding);
        }
        
        if (iscEncoding != null && (charSet == null && localEncoding == null)) {
            String javaEncoding = FBConnectionHelper.getJavaEncoding(iscEncoding);
            
            if (javaEncoding != null)
                cri.setProperty(ISCConstants.isc_dpb_local_encoding, javaEncoding);
        }
        
        if (iscEncoding == null && localEncoding != null) {
            iscEncoding = FBConnectionHelper.getIscEncoding(charSet); 
            cri.setProperty(ISCConstants.isc_dpb_lc_ctype, iscEncoding);
        }
        
        // handle the character translation: mapping_path property specifies
        // a path to the resource with the character mapping
        
        String useTranslation = info.getProperty(USE_TRANSLATION);
        String mappingPath = cri.getStringProperty(ISCConstants.isc_dpb_mapping_path);
        
        if (useTranslation != null && mappingPath == null)
            cri.setProperty(ISCConstants.isc_dpb_mapping_path, useTranslation);
        
        if (useTranslation != null && mappingPath != null && !useTranslation.equals(mappingPath))
            throw new FBSQLException("Property useTranslation is an alias to " +
            "isc_dpb_mapping_path, but specified values are different.");
    }


    /**
	 *
	 *
	 * @param url
	 * @return
	 */
    private GDSType getDriverType(String url)
    {
        if(url.startsWith(FIREBIRD_PROTOCOL_NATIVE))
        {
            return GDSType.NATIVE;
        } 
        else
        if(url.startsWith(FIREBIRD_PROTOCOL_NATIVE_EMBEDDED))
        {
            return GDSType.NATIVE_EMBEDDED;
        }
        else
        if (url.startsWith(FIREBIRD_PROTOCOL_NATIVE_LOCAL))
        {
            return GDSType.NATIVE_LOCAL;
        }
        else
        if (url.startsWith(FIREBIRD_PROTOCOL_ORACLE_MODE))
        {
            return GDSType.ORACLE_MODE;
        }
        {
            return GDSType.PURE_JAVA;
        }
     }


    /**
     * Returns true if the driver thinks that it can open a connection
     * to the given URL.  Typically drivers will return true if they
     * understand the subprotocol specified in the URL and false if
     * they don't.
     *
     * @param url the URL of the database
     * @return true if this driver can connect to the given URL
     * @exception SQLException if a database access error occurs
     */
    public boolean acceptsURL(String url) throws  SQLException {
        return url.startsWith(FIREBIRD_PROTOCOL);
    }



    /**
     * Gets information about the possible properties for this driver.
     * <p>The getPropertyInfo method is intended to allow a generic GUI tool to
     * discover what properties it should prompt a human for in order to get
     * enough information to connect to a database.  Note that depending on
     * the values the human has supplied so far, additional values may become
     * necessary, so it may be necessary to iterate though several calls
     * to getPropertyInfo.
     *
     * @param url the URL of the database to which to connect
     * @param info a proposed list of tag/value pairs that will be sent on
     *          connect open
     * @return an array of DriverPropertyInfo objects describing possible
     *          properties.  This array may be an empty array if no properties
     *          are required.
     * @exception SQLException if a database access error occurs
     * @todo check the correctness of implementation
     * @todo convert parameters into constants
     */
    public DriverPropertyInfo[] getPropertyInfo(String url,
        Properties info) throws  SQLException
    {
        Vector properties = new Vector();
        String database = url.substring(FIREBIRD_PROTOCOL.length());
        String user = info.getProperty(USER);
        String passwd = info.getProperty(PASSWORD);

        // add non-empty database
        if ((database != null) && (database != "")) {
            DriverPropertyInfo dinfo =
                new DriverPropertyInfo(DATABASE, database);
            dinfo.required = true;
            properties.add(dinfo);
        }

        // add user if it is not null
        if (user != null) {
            DriverPropertyInfo dinfo =
                new DriverPropertyInfo(USER, user);
            dinfo.required = true;
            properties.add(dinfo);
        }

        // add password if it is not null
        if (passwd != null) {
            DriverPropertyInfo dinfo =
                new DriverPropertyInfo(PASSWORD, passwd);
            dinfo.required = true;
            properties.add(dinfo);
        }

        return (DriverPropertyInfo[])
            properties.toArray(new DriverPropertyInfo[0]);
    }



    /**
     * Gets the driver's major version number. Initially this should be 1.
         * @return this driver's major version number
     */
    public int getMajorVersion() {
        return 1;
    }

    /**
     * Gets the driver's minor version number. Initially this should be 0.
         * @return this driver's minor version number
     */
    public int getMinorVersion() {
        return 5;
    }


    /**
     * Reports whether this driver is a genuine JDBC
         * COMPLIANT<sup><font size=-2>TM</font></sup> driver.
     * A driver may only report true here if it passes the JDBC compliance
     * tests; otherwise it is required to return false.
     *
     * JDBC compliance requires full support for the JDBC API and full support
     * for SQL 92 Entry Level.  It is expected that JDBC compliant drivers will
     * be available for all the major commercial databases.
     *
     * This method is not intended to encourage the development of non-JDBC
     * compliant drivers, but is a recognition of the fact that some vendors
     * are interested in using the JDBC API and framework for lightweight
     * databases that do not support full database functionality, or for
     * special databases such as document information retrieval where a SQL
     * implementation may not be feasible.
     */
    public boolean jdbcCompliant() {
        return false; //Lets work to make it true!
    }

    /** @link dependency
     * @stereotype instantiate*/
    /*#AbstractConnection lnkFBConnection;*/
}

