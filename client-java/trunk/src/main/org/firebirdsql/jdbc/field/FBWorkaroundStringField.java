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

package org.firebirdsql.jdbc.field;

import java.sql.DataTruncation;
import java.sql.SQLException;

import org.firebirdsql.gds.XSQLVAR;
import org.firebirdsql.jdbc.FBResultSet;


/**
 * Class implementing workaround for "operation was cancelled" bug in server.
 * When we send some string data exceeding maximum length of the corresponding
 * field causes "operation was cancelled" in remote module of the server instead
 * of "arithmetic exception..." error. This makes code debugging harder, since
 * error message is not very informative.
 * <p>
 * However we cannot simply check length locally. Maximum allowed length in bytes 
 * is connected with the character set of the field as defined lengh * maximum
 * number of bytes per character in that encoding. However this does not work
 * for system tables which have defined length 31, character set UNICODE_FSS and
 * maximum allowed length of 31 (instead of 31 * 3 = 63).
 * <p>
 * Until this bug is fixed in the engine we will simply check if field belongs 
 * to the system table and do not throw data truncation error locally. 
 * 
 * @author <a href="mailto:rrokytskyy@users.sourceforge.net">Roman Rokytskyy</a>
 */
public class FBWorkaroundStringField extends FBStringField {

    /**
     * Create instance of this class for the specified field and result set.
     * 
     * @param field instance of {@link XSQLVAR} containing field value.
     * @param rs result set to which this field belongs to.
     * @param numCol column number.
     * @param requiredType required type.
     * 
     * @throws SQLException if something went wrong.
     */
    public FBWorkaroundStringField(XSQLVAR field, FBResultSet rs, int numCol,
            int requiredType) throws SQLException 
    {
        super(field, rs, numCol, requiredType);
    }

    public void setString(String value) throws SQLException {
        setStringForced(value);

        if (field.sqldata.length > field.sqllen && !isSystemTable(field.relname))
            throw new DataTruncation(-1, true, false, field.sqldata.length, field.sqllen);
    }    
    
    /**
     * Set string value without any check of its length. This is a workaround 
     * for the problem described above.
     * 
     * @param value value to set.
     * 
     * @throws SQLException if something went wrong.
     */
    public void setStringForced(String value) throws SQLException {
        if (value == null) {
            field.sqldata = null;
            return;
        }
        field.sqldata = field.encodeString(value,javaEncoding);
    }    
    
    /**
     * List of system tables from Firebird 1.5
     */
    private static final String[] SYSTEM_TABLES = new String[] {
        "RDB$CHARACTER_SETS", 
        "RDB$CHECK_CONSTRAINTS", 
        "RDB$COLLATIONS", 
        "RDB$DATABASE", 
        "RDB$DEPENDENCIES", 
        "RDB$EXCEPTIONS", 
        "RDB$FIELDS", 
        "RDB$FIELD_DIMENSIONS", 
        "RDB$FILES", 
        "RDB$FILTERS", 
        "RDB$FORMATS", 
        "RDB$FUNCTIONS", 
        "RDB$FUNCTION_ARGUMENTS", 
        "RDB$GENERATORS", 
        "RDB$INDEX_SEGMENTS", 
        "RDB$INDICES", 
        "RDB$LOG_FILES", 
        "RDB$PAGES", 
        "RDB$PROCEDURES", 
        "RDB$PROCEDURE_PARAMETERS", 
        "RDB$REF_CONSTRAINTS", 
        "RDB$RELATIONS", 
        "RDB$RELATION_CONSTRAINTS", 
        "RDB$RELATION_FIELDS", 
        "RDB$ROLES", 
        "RDB$SECURITY_CLASSES", 
        "RDB$TRANSACTIONS", 
        "RDB$TRIGGERS", 
        "RDB$TRIGGER_MESSAGES", 
        "RDB$TYPES", 
        "RDB$USER_PRIVILEGES", 
        "RDB$VIEW_RELATIONS"
    };
    
    /**
     * Check if specified table is system table. This method simply traverses
     * hardcoded list of system tables and compares table names.
     * 
     * @param tableName name of the table to check.
     * 
     * @return <code>true</code> if specified table is system, otherwise
     * <code>false</code>
     */
    private boolean isSystemTable(String tableName) {
        boolean result = false;
        
        for (int i = 0; i < SYSTEM_TABLES.length; i++) {
            if (SYSTEM_TABLES[i].equals(tableName)) {
                result = true;
                break;
            }
        }
        
        return result;
    }
}
