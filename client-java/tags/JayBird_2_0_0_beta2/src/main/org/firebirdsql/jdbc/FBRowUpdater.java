/*
 *
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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.plaf.metal.MetalBorders.Flush3DBorder;

import org.firebirdsql.gds.GDSException;
import org.firebirdsql.gds.XSQLVAR;
import org.firebirdsql.gds.impl.AbstractIscStmtHandle;
import org.firebirdsql.gds.impl.GDSHelper;
import org.firebirdsql.jdbc.field.FBField;
import org.firebirdsql.jdbc.field.FBFlushableField;
import org.firebirdsql.jdbc.field.FieldDataProvider;


/**
 * Class responsible for modifying updatable result sets.
 * 
 * A result set is updatable if and only if:
 * <ul>
 * <li>It is a subset of a single table and includes all columns from the
 * table's primary key (in other words, includes all best row identifiers) or
 * RDB$DB_KEY column (in this case tables  without primary key can be updated 
 * too).
 * 
 * <li>If base table columns not included in the result set allow NULL values,
 * result set allows inserting rows into it.
 * 
 * <li>The result set�s SELECT statement does not contain subqueries, a
 * DISTINCT predicate, a HAVING clause, aggregate functions, joined tables,
 * user-defined functions, or stored procedures.
 * </ul>
 * 
 * If the result set definition does not meet these conditions, it is considered
 * read-only.
 * 
 * @author <a href="mailto:rrokytskyy@users.sourceforge.net">Roman Rokytskyy</a>
 */
public class FBRowUpdater  {

    private GDSHelper gdsHelper;
    private Synchronizable syncProvider;
    private XSQLVAR[] xsqlvars;
    
    private FBField[] fields;
    
    private boolean inInsertRow;
    
    private byte[][] newRow;
    private byte[][] oldRow;
    private byte[][] insertRow;
    private boolean[] updatedFlags;

    private String tableName;
    
    private AbstractIscStmtHandle updateStatement;
    private AbstractIscStmtHandle deleteStatement;
    private AbstractIscStmtHandle insertStatement;
    private AbstractIscStmtHandle selectStatement;

    public FBRowUpdater(GDSHelper connection, XSQLVAR[] xsqlvars, 
            Synchronizable syncProvider, boolean cached) throws SQLException {
        
        this.gdsHelper = connection;
        this.syncProvider = syncProvider;
        
        this.xsqlvars = new XSQLVAR[xsqlvars.length];
        this.fields = new FBField[xsqlvars.length];
        
        for (int i = 0; i < xsqlvars.length; i++) {
            XSQLVAR xsqlvar = xsqlvars[i].deepCopy();
            this.xsqlvars[i] = xsqlvar;
        }
        
        newRow = new byte[xsqlvars.length][];
        updatedFlags = new boolean[xsqlvars.length];
        
        for (int i = 0; i < this.xsqlvars.length; i++) {
            
            final int fieldPos = i;
            
            // implementation of the FieldDataProvider interface
            FieldDataProvider dataProvider = new FieldDataProvider() {
                public byte[] getFieldData() {
                    if (updatedFlags[fieldPos]) {
                        
                        if (inInsertRow)
                            return insertRow[fieldPos];
                        else
                            return newRow[fieldPos];
                    } else
                        return oldRow[fieldPos];
                }
                public void setFieldData(byte[] data) {
                    if (inInsertRow)
                        insertRow[fieldPos] = data;
                    else
                        newRow[fieldPos] = data;
                    
                    updatedFlags[fieldPos] = true;
                }

            };

            fields[i] = FBField.createField(this.xsqlvars[i], dataProvider, connection, cached);
        }
        
        // find the table name (there can be only one table per result set)
        for (int i = 0; i < xsqlvars.length; i++) {
            if (tableName == null)
                tableName = xsqlvars[i].relname;
            else
            if (!tableName.equals(xsqlvars[i].relname))
                throw new FBResultSetNotUpdatableException(
                    "Underlying result set references at least two relations: " + 
                    tableName + " and " + xsqlvars[i].relname + ".");
        }
    }
    
    public void setRow(byte[][] row) {
        this.oldRow = row;
        this.updatedFlags = new boolean[xsqlvars.length];
    }

    public void cancelRowUpdates() {
        this.newRow = new byte[xsqlvars.length][];
        this.updatedFlags = new boolean[xsqlvars.length];
    }
    
    public FBField getField(int fieldPosition) {
        return fields[fieldPosition];
    }
    
    /**
     * This method gets the parameter mask for the UPDATE or DELETE statement.
     * Parameter mask is an array of booleans, where array item is set to true,
     * if the appropriate field should be included in WHERE clause of the 
     * UPDATE or DELETE statement.
     * <p>
     * This method obtains the parameter mask from the best row identifiers, in
     * other words set of columns that form "best row identifiers" must be a 
     * subset of the selected columns (no distinction is made whether columns
     * are real or are pseudo-columns). If no  
     * 
     * @return array of booleans that represent parameter mask.
     */
    private boolean[] getParameterMask() throws SQLException {

        // loop through the "best row identifiers" and set appropriate falgs.
        FBDatabaseMetaData metaData = new FBDatabaseMetaData(gdsHelper);
        
        ResultSet bestRowIdentifier = metaData.getBestRowIdentifier(
            "", "", tableName, DatabaseMetaData.bestRowSession, true);

        try {
            boolean[] result = new boolean[xsqlvars.length];
            boolean hasParams = false;
            while(bestRowIdentifier.next()) {
                String columnName = bestRowIdentifier.getString(2);
                
                if (columnName == null)
                    continue;
                
                boolean found = false;
                for (int i = 0; i < xsqlvars.length; i++) {
                    
                    if (columnName.equals(xsqlvars[i].sqlname)) {
                        result[i] = true;
                        found = true;
                    }
                }
                
                // if we did not found a column from the best row identifier
                // in our result set, throw an exception, since we cannot
                // reliably identify the row.
                if (!found)
                    throw new FBResultSetNotUpdatableException(
                        "Underlying result set does not contain all columns " +
                        "that form 'best row identifier'.");
                else
                    hasParams = true;
            }
            
            if (!hasParams)
                throw new FBResultSetNotUpdatableException(
                    "No columns that can be used in WHERE clause could be " +
                    "found.");
            
            return result;
        } finally {
            bestRowIdentifier.close();
        }
    }
    
    private void appendWhereClause(StringBuffer sb, boolean[] parameterMask) {
        sb.append("WHERE");
        sb.append("\n");
        
        boolean first = true;
        for (int i = 0; i < xsqlvars.length; i++) {
            if (!parameterMask[i])
                continue;
            
            if (!first)
                sb.append("AND");
            
            sb.append("\n\t");
            sb.append(xsqlvars[i].sqlname).append(" = ").append("?");
            
            first = false;
        }
    }
    
    private String buildUpdateStatement(boolean[] parameterMask) {
        StringBuffer sb = new StringBuffer();
        
        sb.append("UPDATE ").append(tableName).append("\n");
        sb.append("SET").append("\n");
        
        boolean first = true;
        for (int i = 0; i < xsqlvars.length; i++) {
            if (!updatedFlags[i])
                continue;
            
            if (!first)
                sb.append(",");
            
            sb.append("\n\t");
            sb.append(xsqlvars[i].sqlname).append(" = ").append("?");
            
            first = false;
        }
        
        sb.append("\n");
        appendWhereClause(sb, parameterMask);
        
        return sb.toString();
    }
    
    private String buildDeleteStatement(boolean[] parameterMask) {
        StringBuffer sb = new StringBuffer();
        
        sb.append("DELETE FROM ").append(tableName).append("\n");
        appendWhereClause(sb, parameterMask);
        
        return sb.toString();
    }
    
    private String buildInsertStatement() {
        StringBuffer sb = new StringBuffer();
        
        StringBuffer columns = new StringBuffer();
        StringBuffer params = new StringBuffer();
        
        sb.append("INSERT INTO ").append(tableName);
        
        boolean first = true;
        for (int i = 0; i < xsqlvars.length; i++) {
            
            if (!updatedFlags[i])
                continue;
            
            if (!first) {
                columns.append(", ");
                params.append(", ");
            }
            
            columns.append(xsqlvars[i].sqlname);
            params.append("?");
            
            first = false;
        }
        
        sb.append("(\n\t").append(columns).append("\n)");
        sb.append("VALUES");
        sb.append("(\n\t").append(params).append("\n)");
        
        return sb.toString();
    }
    
    private String buildSelectStatement(boolean[] parameterMask) {
        StringBuffer sb = new StringBuffer();
        StringBuffer columns = new StringBuffer();
        
        sb.append("SELECT");
        
        boolean first = true;
        for (int i = 0; i < xsqlvars.length; i++) {
            
            if (!first) 
                columns.append(", ");
            
            columns.append(xsqlvars[i].sqlname);
            
            first = false;
        }
        
        sb.append("(\n\t").append(columns).append("\n)");
        sb.append("FROM");
        sb.append("(\n\t").append(tableName).append("\n)");
        appendWhereClause(sb, parameterMask);
        return sb.toString();
    }
    
    private static final int UPDATE_STATEMENT_TYPE = 1;
    private static final int DELETE_STATEMENT_TYPE = 2;
    private static final int INSERT_STATEMENT_TYPE = 3;
    private static final int SELECT_STATEMENT_TYPE = 4;
    
    public void updateRow() throws SQLException {
        
        Object mutex = syncProvider.getSynchronizationObject();
        synchronized(mutex) {
            try {
                
                if (updateStatement == null)
                    updateStatement = gdsHelper.allocateStatement();
                
                executeStatement(UPDATE_STATEMENT_TYPE, updateStatement);
                
            } catch(GDSException ex) {
                throw new FBSQLException(ex);
            }
        }
    }
    
    public void deleteRow() throws SQLException {
        Object mutex = syncProvider.getSynchronizationObject();
        synchronized(mutex) {
            try {
                if (deleteStatement == null)
                    deleteStatement = gdsHelper.allocateStatement();
                
                executeStatement(DELETE_STATEMENT_TYPE, deleteStatement);
                
            } catch(GDSException ex) {
                throw new FBSQLException(ex);
            }
        }
    }
    
    public void insertRow() throws SQLException {
        Object mutex = syncProvider.getSynchronizationObject();
        synchronized(mutex) {
            try {
                
                if (insertStatement == null)
                    insertStatement = gdsHelper.allocateStatement();
                
                executeStatement(INSERT_STATEMENT_TYPE, insertStatement);
                
            } catch(GDSException ex) {
                throw new FBSQLException(ex);
            }
        }
    }
    
    public void refreshRow() throws SQLException {
        Object mutex = syncProvider.getSynchronizationObject();
        synchronized(mutex) {
            try {
                
                if (selectStatement == null)
                    selectStatement = gdsHelper.allocateStatement();
                
                executeStatement(SELECT_STATEMENT_TYPE, selectStatement);
                
                Object[] rows = selectStatement.getRows();
                if (rows.length == 0)
                    throw new FBSQLException("No rows could be fetched.");
                
                if (rows.length > 1)
                throw new FBSQLException("More then one row fetched.");
                
                setRow((byte[][])rows[0]);
                
            } catch(GDSException ex) {
                throw new FBSQLException(ex);
            }
        }
    }
    
    private void executeStatement(int statementType, AbstractIscStmtHandle stmt) throws SQLException {
        try {
            if (!stmt.isValid())
                throw new FBSQLException("Corresponding connection is not valid.",
                    FBSQLException.SQL_STATE_CONNECTION_FAILURE_IN_TX);
            
            if (inInsertRow && statementType != INSERT_STATEMENT_TYPE)
                throw new FBSQLException("Only insertRow() is allowed when " +
                        "result set is positioned on insert row.");
            
            if (statementType != INSERT_STATEMENT_TYPE && oldRow == null)
                throw new FBSQLException("Result set is not positioned on a row.");

            // we have to flush before constructing the parameters
            // since flushable field can update the value, which 
            // in turn can change the parameter distribution
            for (int i = 0; i < xsqlvars.length; i++) {
                if (fields[i] instanceof FBFlushableField)
                    ((FBFlushableField)fields[i]).flushCachedData();
            }
            
            boolean[] parameterMask = getParameterMask();
            
            String sql;
            switch(statementType) {
                case UPDATE_STATEMENT_TYPE :
                    sql = buildUpdateStatement(parameterMask);
                    break;
                    
                case DELETE_STATEMENT_TYPE :
                    sql = buildDeleteStatement(parameterMask);
                    break;
                    
                case INSERT_STATEMENT_TYPE :
                    sql = buildInsertStatement();
                    break;
                    
                case SELECT_STATEMENT_TYPE :
                    sql = buildSelectStatement(parameterMask);
                    break;
                    
                default :
                    throw new IllegalArgumentException(
                        "Incorrect statement type specified.");
            }
            
            gdsHelper.prepareStatement(stmt, sql, true);

            XSQLVAR[] params = stmt.getInSqlda().sqlvar;
            
            int paramIterator = 0;
            
            if (statementType == UPDATE_STATEMENT_TYPE) {
                for (int i = 0; i < xsqlvars.length; i++) {
                    if (!updatedFlags[i])
                        continue;
                    
                    params[paramIterator].copyFrom(xsqlvars[i]);
                    params[paramIterator].sqldata = newRow[i];
                    paramIterator++;
                }
            }
            
            for (int i = 0; i < xsqlvars.length; i++) {
                if (!parameterMask[i] && statementType != INSERT_STATEMENT_TYPE)
                    continue;
                else
                if (!updatedFlags[i] && statementType == INSERT_STATEMENT_TYPE)
                    continue;
                
                params[paramIterator].copyFrom(xsqlvars[i]);
                
                if (statementType == INSERT_STATEMENT_TYPE)
                    params[paramIterator].sqldata = insertRow[i];
                else
                    params[paramIterator].sqldata = oldRow[i];
                
                paramIterator++;
            }
            
            gdsHelper.executeStatement(stmt, false);
            
            // TODO think about adding COMMIT RETAIN in the auto-commit mode
            
        } catch(GDSException ex) {
            throw new FBSQLException(ex);
        }
    }
    
    public boolean rowInserted() throws SQLException {
        return false;
    }
    
    public boolean rowDeleted() throws SQLException {
        return false;
    }
    
    public boolean rowUpdated() throws SQLException {
        return false;
    }
    
    public byte[][] getNewRow() {
        byte[][] result = new byte[oldRow.length][];
        for (int i = 0; i < result.length; i++) {
            if (updatedFlags[i]) {
                result[i] = new byte[newRow[i].length];
                System.arraycopy(newRow[i], 0, result[i], 0, newRow[i].length);
            } else {
                result[i] = new byte[oldRow[i].length];
                System.arraycopy(oldRow[i], 0, result[i], 0, oldRow[i].length);
            }
        }
        
        return result;
    }
    
    public byte[][] getInsertRow() {
        return insertRow;
    }

    public byte[][] getOldRow() {
        return oldRow;
    }
    
    public void moveToInsertRow() throws SQLException {
        inInsertRow = true;
        insertRow = new byte[xsqlvars.length][];
        this.updatedFlags = new boolean[xsqlvars.length];
    }
    
    public void moveToCurrentRow() throws SQLException {
        inInsertRow = false;
        insertRow = new byte[xsqlvars.length][];
        this.updatedFlags = new boolean[xsqlvars.length];
    }
}
