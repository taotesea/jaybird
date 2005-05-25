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

import org.firebirdsql.gds.*;
import org.firebirdsql.jgds.*;
import java.sql.SQLException;
import java.sql.Statement;
import org.firebirdsql.logging.Logger;
import org.firebirdsql.logging.LoggerFactory;

/**
 * Statement fetcher for read-only case. It differs from updatable cursor case
 * by the cursor position after {@link #next()} call. This class changes cursor
 * position to point to the next row.
 */
class FBStatementFetcher implements FBFetcher {

    private FBConnection c;

    protected FBStatement fbStatement;
    protected FBResultSet rs;

    private isc_stmt_handle_impl stmt;
          
    private Object[] rowsArray;
    private int size;

    protected byte[][] nextRow;

    private final static Logger log = LoggerFactory.getLogger(FBStatementFetcher.class,false);

    private int rowNum = 0;
    private int rowPosition = 0;
    private boolean isEmpty = false;     
    private boolean isBeforeFirst = false;
    private boolean isFirst = false;
    private boolean isLast = false;
    private boolean isAfterLast = false;
	 
    FBStatementFetcher(FBConnection c, FBStatement fbStatement, 
        isc_stmt_handle stmth, FBResultSet rs) throws SQLException 
    {
        this.c = c;
        this.fbStatement = fbStatement;
        this.stmt = (isc_stmt_handle_impl) stmth;
        this.rs = rs;
            
        c.registerStatement(fbStatement);
            
        isEmpty = false;
        isBeforeFirst = false;
        isFirst = false;
        isLast = false;
        isAfterLast = false;
            
        try {
            // stored procedures
            if (stmt.getAllRowsFetched()){
                rowsArray = stmt.getRows();
                size = stmt.size();
            }
            fetch();
            if (nextRow==null)
                isEmpty = true;
            else 
                isBeforeFirst = true;
        }
        catch (SQLException sqle) {
            throw sqle;
        }
    }

    public boolean next() throws SQLException {
        setIsBeforeFirst(false);
        setIsFirst(false);
        setIsLast(false);
        setIsAfterLast(false);

        if (getIsEmpty())
            return false;
        else if (nextRow == null || (fbStatement.maxRows!=0 && getRowNum()==fbStatement.maxRows)){
            setIsAfterLast(true);
            setRowNum(0);
            return false;
        }
        else {
            try {
                rs.row = nextRow;
                fetch();
                setRowNum(getRowNum() + 1);

                if(getRowNum() == 1)
                    setIsFirst(true);

                if((nextRow==null) || (fbStatement.maxRows!=0 && getRowNum() == fbStatement.maxRows))
                    setIsLast(true);

                return true;
            }
            catch (SQLException sqle) {
                throw sqle;
            }
        }
    }

    public void fetch() throws SQLException {
        int maxRows = 0;
        if (fbStatement.maxRows != 0)
            maxRows = fbStatement.maxRows - rowNum;
        int fetchSize = fbStatement.fetchSize;
        if (fetchSize == 0)
            fetchSize = MAX_FETCH_ROWS;				
        if (maxRows != 0 && fetchSize > maxRows)
            fetchSize = maxRows;
        //
        if (!stmt.getAllRowsFetched() && (rowsArray == null || size == rowPosition)){
            try {
                c.fetch(stmt, fetchSize);
                rowPosition = 0;
                rowsArray = stmt.getRows();
                size = stmt.size();
            }
            catch (GDSException ge) {
                throw new FBSQLException(ge);
            }
        }
        if (rowsArray!=null && size > rowPosition) {
            nextRow = (byte[][]) rowsArray[rowPosition];
            // help the garbage collector
            rowsArray[rowPosition] = null;
            rowPosition++;
        }
        else
            nextRow = null;
    }
	 
    public void close() throws SQLException {
        fbStatement.closeResultSet();
    }
    public Statement getStatement() {
        return fbStatement;
    }
    public int getRowNum() {
        return rowNum;
    }

    public void setRowNum(int rowNumValue) {
        this.rowNum = rowNumValue;
    }
    public boolean getIsEmpty() {
        return isEmpty;
    }

    public void setIsEmpty(boolean isEmptyValue) {
        this.isEmpty = isEmptyValue;
    }
    public boolean getIsBeforeFirst() {
        return isBeforeFirst;
    }

    public void setIsBeforeFirst(boolean isBeforeFirstValue) {
        this.isBeforeFirst = isBeforeFirstValue;
    }
    public boolean getIsFirst() {
        return isFirst;
    }

    public void setIsFirst(boolean isFirstValue) {
        this.isFirst = isFirstValue;
    }

    public boolean getIsLast() throws SQLException {
        return isLast;
    }
    
    public void setIsLast(boolean isLastValue) {
        this.isLast = isLastValue;
    }
    public boolean getIsAfterLast() {
        return isAfterLast;
    }

    public void setIsAfterLast(boolean isAfterLastValue) {
        this.isAfterLast = isAfterLastValue;
    }
    
    
}