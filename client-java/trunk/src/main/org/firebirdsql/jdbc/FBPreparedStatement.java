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

import java.io.*;
import java.math.*;
import java.net.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;

import org.firebirdsql.gds.*;
import org.firebirdsql.logging.*;

/**
 *
 *   @see <related>
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @author <a href="mailto:rrokytskyy@users.sourceforge.net">Roman Rokytskyy</a>
 *   @version $ $
 */



public class FBPreparedStatement extends FBStatement implements PreparedStatement {

    // this array contains either true or false indicating if parameter
    // was initialized, executeQuery, executeUpdate and execute methods
    // will throw an exception if this array contains at least one false value.
    private boolean[] isParamSet;
     
    private FBField[] fields= null;
    private boolean[] isBlob = null;
    private boolean hasBlobs = false;
    
    // we need to handle procedure execution separately, 
    // because in this case we must send out_xsqlda to the server.
    private boolean isExecuteProcedureStatement;
    
    private final static Logger log = LoggerFactory.getLogger(FBStatement.class,false);
    
    FBPreparedStatement(FBConnection c, String sql) throws SQLException {
        super(c);
        
        Object syncObject = getSynchronizationObject();
        synchronized(syncObject) {
            try {
                c.ensureInTransaction();
                prepareFixedStatement(sql, true);
            } catch (GDSException ge) {
                if (log != null)
                    log.info("GDSException in PreparedStatement constructor",
                        ge);
                throw new FBSQLException(ge);
            } finally {
                c.checkEndTransaction();
            } // end of try-catch-finally
        }
    }



    /**
     * Executes the SQL query in this <code>PreparedStatement</code> object
     * and returns the result set generated by the query.
     *
     * @return a <code>ResultSet</code> object that contains the data produced by the
     * query; never <code>null</code>
     * @exception SQLException if a database access error occurs
     */
    public ResultSet executeQuery() throws  SQLException {
        Object syncObject = getSynchronizationObject();
        
        synchronized(syncObject) {
            try {
                c.ensureInTransaction();
                if (!internalExecute(isExecuteProcedureStatement)) {
                    throw new SQLException("No resultset for sql");
                }
                if (c.willEndTransaction()) {
                    return getCachedResultSet(false);
                } else { 
                    return getResultSet();
                }
            } finally {
                c.checkEndTransaction();
            }
        }
    }


    /**
     * Executes the SQL INSERT, UPDATE or DELETE statement
     * in this <code>PreparedStatement</code> object.
     * In addition,
     * SQL statements that return nothing, such as SQL DDL statements,
     * can be executed.
     *
     * @return either the row count for INSERT, UPDATE or DELETE statements;
     * or 0 for SQL statements that return nothing
     * @exception SQLException if a database access error occurs
     */
    public int executeUpdate() throws  SQLException {
        
        Object syncObject = getSynchronizationObject();
        
        synchronized(syncObject) {
            try {
                c.ensureInTransaction();
                if (internalExecute(isExecuteProcedureStatement)) {
                    throw new SQLException("update statement returned results!");
                }
                return getUpdateCount();
            } finally {
                c.checkEndTransaction();
            }
        }
    }


    /**
     * Sets the designated parameter to SQL <code>NULL</code>.
     *
     * <P><B>Note:</B> You must specify the parameter's SQL type.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param sqlType the SQL type code defined in <code>java.sql.Types</code>
     * @exception SQLException if a database access error occurs
     */
    public void setNull(int parameterIndex, int sqlType) throws  SQLException {
        if (parameterIndex > fields.length)
            throw new SQLException("invalid column index");
        getField(parameterIndex).setNull();
    }

    public void setBinaryStream(int parameterIndex, InputStream inputStream,
        int length) throws SQLException
    {
        getField(parameterIndex).setBinaryStream(inputStream, length);
    }
	 
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        getField(parameterIndex).setBytes(x);
    }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        getField(parameterIndex).setBoolean(x);
    }

    public void setByte(int parameterIndex, byte x) throws SQLException {
        getField(parameterIndex).setByte(x);
    }

    public void setDate(int parameterIndex, Date x) throws SQLException {
        getField(parameterIndex).setDate(x);
    }

    public void setDouble(int parameterIndex, double x) throws SQLException {
        getField(parameterIndex).setDouble(x);
    }

    public void setFloat(int parameterIndex, float x) throws SQLException {
        getField(parameterIndex).setFloat(x);
    }

    public void setInt(int parameterIndex, int x) throws SQLException {
        getField(parameterIndex).setInteger(x);
    }

    public void setLong(int parameterIndex, long x) throws SQLException {
        getField(parameterIndex).setLong(x);
    }

    public void setObject(int parameterIndex, Object x) throws SQLException {
        getField(parameterIndex).setObject(x);
    }

    public void setShort(int parameterIndex, short x) throws SQLException {
        getField(parameterIndex).setShort(x);
    }

    public void setString(int parameterIndex, String x) throws SQLException {
        getField(parameterIndex).setString(x);
    }

    public void setTime(int parameterIndex, Time x) throws SQLException {
        getField(parameterIndex).setTime(x);
    }

    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        getField(parameterIndex).setTimestamp(x);
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x) throws  SQLException {
        getField(parameterIndex).setBigDecimal(x);
    }
    /**
     * Returns the XSQLVAR structure for the specified column.
     */
    protected XSQLVAR getXsqlvar(int columnIndex) {
        return fixedStmt.getInSqlda().sqlvar[columnIndex - 1];
    }

    /**
     * Factory method for the field access objects
     */
    protected FBField getField(int columnIndex) throws SQLException {
         if (columnIndex > fields.length)
            throw new SQLException("invalid column index");
            
         isParamSet[columnIndex-1] = true;
         return fields[columnIndex-1];
    }

    /**
     * Sets the designated parameter to the given input stream, which will have
     * the specified number of bytes.
     * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code>. Data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from ASCII to the database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the Java input stream that contains the ASCII parameter value
     * @param length the number of bytes in the stream
     * @exception SQLException if a database access error occurs
     */
    public void setAsciiStream(int parameterIndex, InputStream x,
        int length) throws  SQLException
    {
        setBinaryStream(parameterIndex, x, length);
    }


    /**
     * Sets the designated parameter to the given input stream, which will have
     * the specified number of bytes.
     * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code> object. The data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     * The byte format of the Unicode stream must be Java UTF-8, as
     * defined in the Java Virtual Machine Specification.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the java input stream which contains the
     * UNICODE parameter value
     * @param length the number of bytes in the stream
     * @exception SQLException if a database access error occurs
     * @deprecated

     *I really have no idea if there is anything else we should be doing here
     */
    public void setUnicodeStream(int parameterIndex, InputStream x,
              int length) throws  SQLException {
        setBinaryStream(parameterIndex, x, length);
    }


    /**
     * Clears the current parameter values immediately.
     * <P>In general, parameter values remain in force for repeated use of a
     * statement. Setting a parameter value automatically clears its
     * previous value.  However, in some cases it is useful to immediately
     * release the resources used by the current parameter values; this can
     * be done by calling the method <code>clearParameters</code>.
     *
     * @exception SQLException if a database access error occurs
     */
    public void clearParameters() throws  SQLException {
        for (int i = 0; i < isParamSet.length; i++)
            isParamSet[i] = false;
    }


    //----------------------------------------------------------------------
    // Advanced features:

    /**
     * <p>Sets the value of the designated parameter with the given object. The second
     * argument must be an object type; for integral values, the
     * <code>java.lang</code> equivalent objects should be used.
     *
     * <p>The given Java object will be converted to the given targetSqlType
     * before being sent to the database.
     *
     * If the object has a custom mapping (is of a class implementing the
     * interface <code>SQLData</code>),
     * the JDBC driver should call the method <code>SQLData.writeSQL</code> to write it
     * to the SQL data stream.
     * If, on the other hand, the object is of a class implementing
     * Ref, Blob, Clob, Struct,
     * or Array, the driver should pass it to the database as a value of the
     * corresponding SQL type.
     *
     * <p>Note that this method may be used to pass datatabase-
     * specific abstract data types.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the object containing the input parameter value
     * @param targetSqlType the SQL type (as defined in java.sql.Types) to be
     * sent to the database. The scale argument may further qualify this type.
     * @param scale for java.sql.Types.DECIMAL or java.sql.Types.NUMERIC types,
     *          this is the number of digits after the decimal point.  For all other
     *          types, this value will be ignored.
     * @exception SQLException if a database access error occurs
     * @see Types
     */
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws  SQLException {
        // Workaround for JBuilder DataSets		 
        setObject(parameterIndex, x);
    }


   /**
     * Sets the value of the designated parameter with the given object.
     * This method is like the method <code>setObject</code>
     * above, except that it assumes a scale of zero.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the object containing the input parameter value
     * @param targetSqlType the SQL type (as defined in java.sql.Types) to be
     *                      sent to the database
     * @exception SQLException if a database access error occurs
     */
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws  SQLException {
        //well, for now
        setObject(parameterIndex, x);

    }


    /**
     * Executes any kind of SQL statement.
     * Some prepared statements return multiple results; the <code>execute</code>
     * method handles these complex statements as well as the simpler
     * form of statements handled by the methods <code>executeQuery</code>
     * and <code>executeUpdate</code>.
     *
     * @exception SQLException if a database access error occurs
     * @see Statement#execute
     */
    public boolean execute() throws  SQLException {
        
        Object syncObject = getSynchronizationObject();
        
        synchronized(syncObject) {
            try {
                c.ensureInTransaction();
                boolean hasResultSet = internalExecute(
                    isExecuteProcedureStatement);
                if (hasResultSet && c.willEndTransaction()) {
                    getCachedResultSet(false);
                }
                return hasResultSet;
            } finally {
                c.checkEndTransaction();
            }
        }
    }

    protected boolean internalExecute(boolean sendOutParams) throws  SQLException
    {
        boolean canExecute = true;
        for (int i = 0; i < isParamSet.length; i++){
            canExecute = canExecute && isParamSet[i];
        }

        if (!canExecute)
            throw new SQLException("Not all parameters were set. " +
                "Cannot execute query.");

        Object syncObject = getSynchronizationObject();
        
        synchronized(syncObject) {
            if (hasBlobs) {
                for (int i = 0; i < isParamSet.length; i++) {
                    if (isBlob[i]) {
                        FBFlushableField flushableField =
                            (FBFlushableField)getField(i + 1);

                        flushableField.flushCachedData();
                    }
                }
            }
            
            try {
                closeResultSet();
                c.executeStatement(fixedStmt, sendOutParams);
                isResultSet = (fixedStmt.getOutSqlda().sqld > 0);
                return (fixedStmt.getOutSqlda().sqld > 0);
            } catch (GDSException ge) {
                throw new FBSQLException(ge);
            }
        }
    }


    //--------------------------JDBC 2.0-----------------------------

    /**
     * Adds a set of parameters to this <code>PreparedStatement</code>
     * object's batch of commands.
     *
     * @exception SQLException if a database access error occurs
     * @see Statement#addBatch
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void addBatch() throws  SQLException {
        throw new SQLException("not yet implemented");
    }


    /**
     * Sets the designated parameter to the given <code>Reader</code>
     * object, which is the given number of characters long.
     * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.Reader</code> object. The data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the java reader which contains the UNICODE data
     * @param length the number of characters in the stream
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void setCharacterStream(int parameterIndex, Reader reader,
              int length) throws  SQLException {
        getField(parameterIndex).setCharacterStream(reader, length);
    }


    /**
     * Sets the designated parameter to the given
     *  <code>REF(&lt;structured-type&gt;)</code> value.
     *
     * @param i the first parameter is 1, the second is 2, ...
     * @param x an SQL <code>REF</code> value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void setRef (int i, Ref x) throws  SQLException {
        throw new SQLException("not yet implemented");
    }


    /**
     * Sets the designated parameter to the given
     *  <code>Blob</code> object.
     *
     * @param i the first parameter is 1, the second is 2, ...
     * @param x a <code>Blob</code> object that maps an SQL <code>BLOB</code> value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void setBlob (int parameterIndex, Blob blob) throws  SQLException {
        if (!(blob instanceof FBBlob)) {
            throw new SQLException("You must use FBBlobs with Firebird!");
        }
        getField(parameterIndex).setBlob((FBBlob) blob);
    }


    /**
     * Sets the designated parameter to the given
     *  <code>Clob</code> object.
     *
     * @param i the first parameter is 1, the second is 2, ...
     * @param x a <code>Clob</code> object that maps an SQL <code>CLOB</code> value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void setClob (int i, Clob x) throws  SQLException {
        throw new SQLException("not yet implemented");
    }


    /**
     * Sets the designated parameter to the given
     *  <code>Array</code> object.
     * Sets an Array parameter.
     *
     * @param i the first parameter is 1, the second is 2, ...
     * @param x an <code>Array</code> object that maps an SQL <code>ARRAY</code> value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void setArray (int i, Array x) throws  SQLException {
        throw new SQLException("Arrays are not supported.");
    }


    /**
     * Gets the number, types and properties of a <code>ResultSet</code>
     * object's columns.
     *
     * @return the description of a <code>ResultSet</code> object's columns
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public ResultSetMetaData getMetaData() throws  SQLException {
        return new FBResultSetMetaData(fixedStmt.getOutSqlda().sqlvar, c);
    }


    /**
     * Sets the designated parameter to the given <code>java.sql.Date</code> value,
     * using the given <code>Calendar</code> object.  The driver uses
     * the <code>Calendar</code> object to construct an SQL <code>DATE</code> value,
     * which the driver then sends to the database.  With a
     * a <code>Calendar</code> object, the driver can calculate the date
     * taking into account a custom timezone.  If no
     * <code>Calendar</code> object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @param cal the <code>Calendar</code> object the driver will use
     *            to construct the date
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void setDate(int parameterIndex, Date x, Calendar cal)
        throws  SQLException {
        getField(parameterIndex).setDate(x, cal);
    }


    /**
     * Sets the designated parameter to the given <code>java.sql.Time</code> value,
     * using the given <code>Calendar</code> object.  The driver uses
     * the <code>Calendar</code> object to construct an SQL <code>TIME</code> value,
     * which the driver then sends to the database.  With a
     * a <code>Calendar</code> object, the driver can calculate the time
     * taking into account a custom timezone.  If no
     * <code>Calendar</code> object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @param cal the <code>Calendar</code> object the driver will use
     *            to construct the time
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void setTime(int parameterIndex, Time x, Calendar cal)
        throws  SQLException {
        getField(parameterIndex).setTime(x, cal);
    }


    /**
     * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value,
     * using the given <code>Calendar</code> object.  The driver uses
     * the <code>Calendar</code> object to construct an SQL <code>TIMESTAMP</code> value,
     * which the driver then sends to the database.  With a
     * a <code>Calendar</code> object, the driver can calculate the timestamp
     * taking into account a custom timezone.  If no
     * <code>Calendar</code> object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @param cal the <code>Calendar</code> object the driver will use
     *            to construct the timestamp
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     * @todo change all the methods called to take a Calendar instead of Date object.
     */
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) 
        throws  SQLException {
        getField(parameterIndex).setTimestamp(x, cal);
    }


    /**
     * Sets the designated parameter to SQL <code>NULL</code>.
     * This version of the method <code>setNull</code> should
     * be used for user-defined types and REF type parameters.  Examples
     * of user-defined types include: STRUCT, DISTINCT, JAVA_OBJECT, and
     * named array types.
     *
     * <P><B>Note:</B> To be portable, applications must give the
     * SQL type code and the fully-qualified SQL type name when specifying
     * a NULL user-defined or REF parameter.  In the case of a user-defined type
     * the name is the type name of the parameter itself.  For a REF
     * parameter, the name is the type name of the referenced type.  If
     * a JDBC driver does not need the type code or type name information,
     * it may ignore it.
     *
     * Although it is intended for user-defined and Ref parameters,
     * this method may be used to set a null parameter of any JDBC type.
     * If the parameter does not have a user-defined or REF type, the given
     * typeName is ignored.
     *
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param sqlType a value from <code>java.sql.Types</code>
     * @param typeName the fully-qualified name of an SQL user-defined type;
     *  ignored if the parameter is not a user-defined type or REF
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
     public void setNull (int parameterIndex, int sqlType, String typeName) throws  SQLException {
         setNull(parameterIndex, sqlType); //all nulls are represented the same... a null reference
    }


    /**
     * jdbc 3
     * @param param1 <description>
     * @param param2 <description>
     * @exception java.sql.SQLException <description>
     */
    public void setURL(int param1, URL param2) throws SQLException {
        // TODO: implement this java.sql.PreparedStatement method
        throw new SQLException("Not yet implemented");
    }


    /**
     * jdbc 3
     * @return <description>
     * @exception java.sql.SQLException <description>
     */
    public ParameterMetaData getParameterMetaData() throws SQLException {
        // TODO: implement this java.sql.PreparedStatement method
        throw new SQLException("Not yet implemented");
    }



    /**
     * Prepare fixed statement and initialize parameters.
     */
    protected void prepareFixedStatement(String sql, boolean describeBind)
        throws GDSException, SQLException
    {
        super.prepareFixedStatement(sql, describeBind);

        // initialize isParamSet member
        isParamSet = new boolean[fixedStmt.getInSqlda().sqln];
        fields = new FBField[fixedStmt.getInSqlda().sqln];		  
        isBlob = new boolean[fixedStmt.getInSqlda().sqln];		  
        XSQLVAR[] inVars = fixedStmt.getInSqlda().sqlvar;
        // this is probably redundant, JVM initializes members to false
        for (int i = 0; i < isParamSet.length; i++){
            isParamSet[i] = false;
      		fields[i] = FBField.createField(getXsqlvar(i+1), null, i, false);

      		if (fields[i] instanceof FBBlobField)
      		    ((FBBlobField)fields[i]).setConnection(c);
      		else
      		if (fields[i] instanceof FBStringField)
      		    ((FBStringField)fields[i]).setConnection(c);

            isBlob[i] =
                FBField.isType(getXsqlvar(i+1), Types.BLOB) ||
                FBField.isType(getXsqlvar(i+1), Types.BINARY) ||
                FBField.isType(getXsqlvar(i+1), Types.LONGVARCHAR);
            if (isBlob[i])
                hasBlobs = true;
        }
        
        this.isExecuteProcedureStatement = isExecuteProcedureStatement(sql);
    }

    /**
     * Execute statement internally. This method checks if all parameters
     * were set.
     */
/*	 
    protected boolean internalExecute(String sql) throws GDSException, SQLException {
        boolean canExecute = true;
        for (int i = 0; i < isParamSet.length; i++){
            canExecute = canExecute && isParamSet[i];
          }

        if (!canExecute)
            throw new SQLException("Not all parameters were set. " +
                "Cannot execute query.");

        return super.internalExecute(sql);
    }
*/
    /**
     * Marks that parameter was set.
     */
/*
    protected void parameterWasSet(int parameterIndex) throws SQLException {
        if (parameterIndex < 1 || parameterIndex > isParamSet.length)
            throw new SQLException("Internal driver consistency check: " +
                "Number of available params does not correspond to prepared.");

        isParamSet[parameterIndex - 1] = true;
    }
*/
}
