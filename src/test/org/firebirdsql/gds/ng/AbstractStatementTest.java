/*
 * Firebird Open Source JavaEE Connector - JDBC Driver
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
 * can be obtained from a source control history command.
 *
 * All rights reserved.
 */
package org.firebirdsql.gds.ng;

import org.firebirdsql.common.DdlHelper;
import org.firebirdsql.common.FBTestProperties;
import org.firebirdsql.common.rules.UsesDatabase;
import org.firebirdsql.encodings.Encoding;
import org.firebirdsql.gds.ISCConstants;
import org.firebirdsql.gds.JaybirdErrorCodes;
import org.firebirdsql.gds.TransactionParameterBuffer;
import org.firebirdsql.gds.impl.TransactionParameterBufferImpl;
import org.firebirdsql.gds.ng.fields.FieldDescriptor;
import org.firebirdsql.gds.ng.fields.RowDescriptor;
import org.firebirdsql.gds.ng.fields.RowValue;
import org.firebirdsql.gds.ng.wire.SimpleStatementListener;
import org.firebirdsql.util.FirebirdSupportInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLNonTransientException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.firebirdsql.common.FBTestProperties.DB_PASSWORD;
import static org.firebirdsql.common.FBTestProperties.DB_USER;
import static org.firebirdsql.common.FBTestProperties.getDefaultSupportInfo;
import static org.firebirdsql.common.matchers.SQLExceptionMatchers.fbMessageStartsWith;
import static org.firebirdsql.util.FirebirdSupportInfo.supportInfoFor;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

/**
 * Generic tests for FbStatement.
 * <p>
 * This abstract class is subclassed by the tests for specific FbStatement implementations.
 * </p>
 *
 * @author <a href="mailto:mrotteveel@users.sourceforge.net">Mark Rotteveel</a>
 * @since 3.0
 */
public abstract class AbstractStatementTest {

    //@formatter:off
    private static final String CREATE_EXECUTABLE_STORED_PROCEDURE =
            "CREATE PROCEDURE increment " +
            " (intvalue INTEGER) " +
            "RETURNS " +
            " (outvalue INTEGER) " +
            "AS " +
            "BEGIN " +
            " outvalue = intvalue + 1; " +
            "END";

    protected static final String EXECUTE_EXECUTABLE_STORED_PROCEDURE =
            "EXECUTE PROCEDURE INCREMENT(?)";

    private static final String CREATE_SELECTABLE_STORED_PROCEDURE =
            "CREATE PROCEDURE range " +
            " (startvalue INTEGER, rowcount INTEGER) " +
            "RETURNS " +
            " (outvalue INTEGER) " +
            "AS " +
            "DECLARE VARIABLE actualcount INTEGER; " +
            "BEGIN " +
            "  actualcount = 0; " +
            "  WHILE (actualcount < rowcount) DO " +
            "  BEGIN " +
            "    outvalue = startvalue + actualcount; " +
            "    suspend; " +
            "    actualcount = actualcount + 1; " +
            "  END " +
            "END";

    protected static final String EXECUTE_SELECTABLE_STORED_PROCEDURE =
            "SELECT OUTVALUE FROM RANGE(?, ?)";

    private static final String CREATE_KEY_VALUE_TABLE =
            "CREATE TABLE keyvalue ( " +
            " thekey INTEGER PRIMARY KEY, " +
            " thevalue VARCHAR(5), " +
            " theUTFVarcharValue VARCHAR(5) CHARACTER SET UTF8, " +
            " theUTFCharValue CHAR(5) CHARACTER SET UTF8 " +
            ")";

    protected static final String INSERT_RETURNING_KEY_VALUE =
            "INSERT INTO keyvalue (thevalue) VALUES (?) RETURNING thekey";

    protected static final String INSERT_THEUTFVALUE =
            "INSERT INTO keyvalue (thekey, theUTFVarcharValue, theUTFCharValue) VALUES (?, ?, ?)";

    protected static final String SELECT_THEUTFVALUE =
            "SELECT theUTFVarcharValue, theUTFCharValue FROM keyvalue WHERE thekey = ?";
    //@formatter:on

    protected final SimpleStatementListener listener = new SimpleStatementListener();
    protected FbDatabase db;
    private FbTransaction transaction;
    protected FbStatement statement;
    protected final FbConnectionProperties connectionInfo;
    {
        connectionInfo = new FbConnectionProperties();
        connectionInfo.setServerName(FBTestProperties.DB_SERVER_URL);
        connectionInfo.setPortNumber(FBTestProperties.DB_SERVER_PORT);
        connectionInfo.setUser(DB_USER);
        connectionInfo.setPassword(DB_PASSWORD);
        connectionInfo.setDatabaseName(FBTestProperties.getDatabasePath());
        connectionInfo.setEncoding("NONE");
    }

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public final UsesDatabase usesDatabase = UsesDatabase.usesDatabase();

    protected abstract Class<? extends FbDatabase> getExpectedDatabaseType();

    @Before
    public final void setUp() throws Exception {
        try (Connection con = FBTestProperties.getConnectionViaDriverManager()) {
            DdlHelper.executeDDL(con, CREATE_EXECUTABLE_STORED_PROCEDURE);
            DdlHelper.executeDDL(con, CREATE_SELECTABLE_STORED_PROCEDURE);
            DdlHelper.executeCreateTable(con, CREATE_KEY_VALUE_TABLE);
        }

        db = createDatabase();
        assertEquals("Unexpected FbDatabase implementation", getExpectedDatabaseType(), db.getClass());

        db.attach();
    }

    protected abstract FbDatabase createDatabase() throws SQLException;

    @Test
    public void testSelect_NoParameters_Describe() throws Exception {
        allocateStatement();
        statement.prepare(
                "SELECT RDB$DESCRIPTION AS \"Description\", RDB$RELATION_ID, RDB$SECURITY_CLASS, RDB$CHARACTER_SET_NAME " +
                        "FROM RDB$DATABASE");

        assertEquals("Unexpected StatementType", StatementType.SELECT, statement.getType());

        final RowDescriptor fields = statement.getRowDescriptor();
        assertNotNull("Fields", fields);
        final FirebirdSupportInfo supportInfo = supportInfoFor(db);
        final int metadataCharSetId = supportInfo.reportedMetadataCharacterSetId();
        List<FieldDescriptor> expectedFields =
                Arrays.asList(
                        new FieldDescriptor(0, db.getDatatypeCoder(), ISCConstants.SQL_BLOB | 1, 1,
                                supportInfo.reportsBlobCharSetInDescriptor() ? metadataCharSetId : 0, 8, "Description", null,
                                "RDB$DESCRIPTION", "RDB$DATABASE", "SYSDBA"),
                        new FieldDescriptor(1, db.getDatatypeCoder(), ISCConstants.SQL_SHORT | 1, 0, 0, 2,
                                "RDB$RELATION_ID", null, "RDB$RELATION_ID", "RDB$DATABASE", "SYSDBA"),
                        new FieldDescriptor(2, db.getDatatypeCoder(), ISCConstants.SQL_TEXT | 1, metadataCharSetId, 0,
                                supportInfo.maxReportedIdentifierLengthBytes(), "RDB$SECURITY_CLASS", null,
                                "RDB$SECURITY_CLASS", "RDB$DATABASE", "SYSDBA"),
                        new FieldDescriptor(3, db.getDatatypeCoder(), ISCConstants.SQL_TEXT | 1, metadataCharSetId, 0,
                                supportInfo.maxReportedIdentifierLengthBytes(), "RDB$CHARACTER_SET_NAME", null,
                                "RDB$CHARACTER_SET_NAME", "RDB$DATABASE", "SYSDBA")
                );
        assertEquals("Unexpected values for fields", expectedFields, fields.getFieldDescriptors());
        assertNotNull("Parameters", statement.getParameterDescriptor());
        assertEquals("Unexpected parameter count", 0, statement.getParameterDescriptor().getCount());
    }

    @Test
    public void testSelect_NoParameters_Execute_and_Fetch() throws Exception {
        allocateStatement();
        statement.prepare(
                "SELECT RDB$DESCRIPTION AS \"Description\", RDB$RELATION_ID, RDB$SECURITY_CLASS, RDB$CHARACTER_SET_NAME " +
                        "FROM RDB$DATABASE");

        final SimpleStatementListener statementListener = new SimpleStatementListener();
        statement.addStatementListener(statementListener);

        statement.execute(RowValue.EMPTY_ROW_VALUE);

        assertEquals("Expected hasResultSet to be set to true", Boolean.TRUE, statementListener.hasResultSet());
        assertEquals("Expected hasSingletonResult to be set to false", Boolean.FALSE, statementListener.hasSingletonResult());
        assertNull("Expected allRowsFetched not set yet", statementListener.isAllRowsFetched());
        assertEquals("Expected no rows to be fetched yet", 0, statementListener.getRows().size());

        statement.fetchRows(10);

        assertEquals("Expected allRowsFetched to be set to true", Boolean.TRUE, statementListener.isAllRowsFetched());
        assertEquals("Expected a single row to have been fetched", 1, statementListener.getRows().size());
    }

    @Test
    public void testSelect_WithParameters_Describe() throws Exception {
        allocateStatement();
        statement.prepare(
                "SELECT a.RDB$CHARACTER_SET_NAME " +
                        "FROM RDB$CHARACTER_SETS a " +
                        "WHERE a.RDB$CHARACTER_SET_ID = ? OR a.RDB$BYTES_PER_CHARACTER = ?");

        assertEquals("Unexpected StatementType", StatementType.SELECT, statement.getType());

        final RowDescriptor fields = statement.getRowDescriptor();
        assertNotNull("Fields", fields);
        final FirebirdSupportInfo supportInfo = supportInfoFor(db);
        final boolean supportsTableAlias = supportInfo.supportsTableAlias();
        final int metadataCharSetId = supportInfo.reportedMetadataCharacterSetId();

        List<FieldDescriptor> expectedFields =
                Collections.singletonList(
                        new FieldDescriptor(0, db.getDatatypeCoder(), ISCConstants.SQL_TEXT | 1, metadataCharSetId, 0,
                                supportInfo.maxReportedIdentifierLengthBytes(), "RDB$CHARACTER_SET_NAME",
                                supportsTableAlias ? "A" : null, "RDB$CHARACTER_SET_NAME", "RDB$CHARACTER_SETS",
                                "SYSDBA")
                );
        assertEquals("Unexpected values for fields", expectedFields, fields.getFieldDescriptors());

        final RowDescriptor parameters = statement.getParameterDescriptor();
        assertNotNull("Parameters", parameters);
        List<FieldDescriptor> expectedParameters =
                Arrays.asList(
                        new FieldDescriptor(0, db.getDatatypeCoder(), ISCConstants.SQL_SHORT | 1, 0, 0, 2,
                                null, null, null, null, null),
                        new FieldDescriptor(1, db.getDatatypeCoder(), ISCConstants.SQL_SHORT | 1, 0, 0, 2,
                                null, null, null, null, null)
                );
        assertEquals("Unexpected values for parameters", expectedParameters, parameters.getFieldDescriptors());
    }

    @Test
    public void testSelect_WithParameters_Execute_and_Fetch() throws Exception {
        allocateStatement();
        statement.addStatementListener(listener);
        statement.prepare(
                "SELECT a.RDB$CHARACTER_SET_NAME " +
                        "FROM RDB$CHARACTER_SETS a " +
                        "WHERE a.RDB$CHARACTER_SET_ID = ? OR a.RDB$BYTES_PER_CHARACTER = ?");

        final DatatypeCoder coder = db.getDatatypeCoder();
        RowValue rowValue = RowValue.of(
                coder.encodeShort(3),  // smallint = 3 (id of UNICODE_FSS)
                coder.encodeShort(1)); // smallint = 1 (single byte character sets)
        
        statement.execute(rowValue);

        assertEquals("Expected hasResultSet to be set to true", Boolean.TRUE, listener.hasResultSet());
        assertEquals("Expected hasSingletonResult to be set to false", Boolean.FALSE, listener.hasSingletonResult());
        assertNull("Expected allRowsFetched not set yet", listener.isAllRowsFetched());
        assertEquals("Expected no rows to be fetched yet", 0, listener.getRows().size());
        assertNull("Expected no SQL counts yet", listener.getSqlCounts());

        // 100 should be sufficient to fetch all character sets
        statement.fetchRows(100);

        assertEquals("Expected allRowsFetched to be set to true", Boolean.TRUE, listener.isAllRowsFetched());
        // Number is database dependent (unicode_fss + all single byte character sets)
        assertTrue("Expected more than two rows", listener.getRows().size() > 2);

        assertNull("expected no SQL counts immediately after retrieving all rows", listener.getSqlCounts());

        statement.getSqlCounts();

        assertNotNull("Expected SQL counts", listener.getSqlCounts());
        assertEquals("Unexpected select count", listener.getRows().size(), listener.getSqlCounts().getLongSelectCount());
    }

    @Test
    public void test_PrepareExecutableStoredProcedure() throws Exception {
        allocateStatement();
        statement.prepare(EXECUTE_EXECUTABLE_STORED_PROCEDURE);

        assertEquals("Unexpected StatementType", StatementType.STORED_PROCEDURE, statement.getType());

        final RowDescriptor fields = statement.getRowDescriptor();
        assertNotNull("Fields", fields);
        List<FieldDescriptor> expectedFields =
                Collections.singletonList(
                        new FieldDescriptor(0, db.getDatatypeCoder(), ISCConstants.SQL_LONG | 1, 0, 0, 4, "OUTVALUE", null,
                                "OUTVALUE", "INCREMENT", "SYSDBA")
                );
        assertEquals("Unexpected values for fields", expectedFields, fields.getFieldDescriptors());

        final RowDescriptor parameters = statement.getParameterDescriptor();
        assertNotNull("Parameters", parameters);
        List<FieldDescriptor> expectedParameters =
                Collections.singletonList(
                        new FieldDescriptor(0, db.getDatatypeCoder(), ISCConstants.SQL_LONG | 1, 0, 0, 4, null, null, null,
                                null, null)
                );
        assertEquals("Unexpected values for parameters", expectedParameters, parameters.getFieldDescriptors());
    }

    @Test
    public void test_ExecuteExecutableStoredProcedure() throws Exception {
        allocateStatement();
        statement.addStatementListener(listener);
        statement.prepare(EXECUTE_EXECUTABLE_STORED_PROCEDURE);

        RowValue rowValue = RowValue.of(
                db.getDatatypeCoder().encodeInt(1)); // Byte representation of 1
        statement.execute(rowValue);

        assertTrue("Expected singleton result for executable stored procedure", listener.hasSingletonResult());
        assertFalse("Expected no result set for executable stored procedure", listener.hasResultSet());
        assertTrue("Expected all rows to have been fetched", listener.isAllRowsFetched());
        assertEquals("Expected 1 row", 1, listener.getRows().size());
        RowValue fieldValues = listener.getRows().get(0);
        assertEquals("Expected one field", 1, fieldValues.getCount());
        assertEquals("Expected byte representation of 2", 2,
                db.getDatatypeCoder().decodeInt(fieldValues.getFieldData(0)));
    }

    @Test
    public void test_PrepareSelectableStoredProcedure() throws Exception {
        allocateStatement();
        statement.prepare(EXECUTE_SELECTABLE_STORED_PROCEDURE);

        assertEquals("Unexpected StatementType", StatementType.SELECT, statement.getType());

        final RowDescriptor fields = statement.getRowDescriptor();
        assertNotNull("Fields", fields);
        List<FieldDescriptor> expectedFields =
                Collections.singletonList(
                        new FieldDescriptor(0, db.getDatatypeCoder(), ISCConstants.SQL_LONG | 1, 0, 0, 4, "OUTVALUE", null,
                                "OUTVALUE", "RANGE", "SYSDBA")
                );
        assertEquals("Unexpected values for fields", expectedFields, fields.getFieldDescriptors());

        final RowDescriptor parameters = statement.getParameterDescriptor();
        assertNotNull("Parameters", parameters);
        List<FieldDescriptor> expectedParameters =
                Arrays.asList(
                        new FieldDescriptor(0, db.getDatatypeCoder(), ISCConstants.SQL_LONG | 1, 0, 0, 4, null, null, null,
                                null, null),
                        new FieldDescriptor(1, db.getDatatypeCoder(), ISCConstants.SQL_LONG | 1, 0, 0, 4, null, null, null,
                                null, null)
                );
        assertEquals("Unexpected values for parameters", expectedParameters, parameters.getFieldDescriptors());
    }

    @Test
    public void test_PrepareInsertReturning() throws Exception {
        assumeTrue("Test requires INSERT .. RETURNING ... support", supportInfoFor(db).supportsInsertReturning());

        allocateStatement();
        statement.prepare(INSERT_RETURNING_KEY_VALUE);

        // DML {INSERT, UPDATE, DELETE} ... RETURNING is described as a stored procedure!
        assertEquals("Unexpected StatementType", StatementType.STORED_PROCEDURE, statement.getType());

        final RowDescriptor fields = statement.getRowDescriptor();
        assertNotNull("Fields", fields);
        List<FieldDescriptor> expectedFields =
                Collections.singletonList(
                        new FieldDescriptor(0, db.getDatatypeCoder(), ISCConstants.SQL_LONG | 1, 0, 0, 4, "THEKEY", null,
                                "THEKEY", "KEYVALUE", "SYSDBA")
                );
        assertEquals("Unexpected values for fields", expectedFields, fields.getFieldDescriptors());

        final RowDescriptor parameters = statement.getParameterDescriptor();
        assertNotNull("Parameters", parameters);
        List<FieldDescriptor> expectedParameters =
                Collections.singletonList(
                        new FieldDescriptor(0, db.getDatatypeCoder(), ISCConstants.SQL_VARYING | 1, 0, 0, 5, null, null,
                                null, null, null)
                );
        assertEquals("Unexpected values for parameters", expectedParameters, parameters.getFieldDescriptors());
    }

    @Test
    public void test_GetExecutionPlan_withStatementPrepared() throws Exception {
        allocateStatement();
        statement.prepare(
                "SELECT RDB$DESCRIPTION AS \"Description\", RDB$RELATION_ID, RDB$SECURITY_CLASS, RDB$CHARACTER_SET_NAME " +
                        "FROM RDB$DATABASE");

        String executionPlan = statement.getExecutionPlan();

        assertEquals("Unexpected plan for prepared statement", "PLAN (RDB$DATABASE NATURAL)", executionPlan);
    }

    @Test
    public void test_GetExecutionPlan_noStatementPrepared() throws Exception {
        allocateStatement();
        expectedException.expect(SQLNonTransientException.class);
        expectedException.expectMessage("Statement not yet allocated");

        statement.getExecutionPlan();
    }

    @Test
    public void test_GetExecutionPlan_StatementClosed() throws Exception {
        expectedException.expect(SQLNonTransientException.class);
        expectedException.expectMessage("Statement closed");
        allocateStatement();
        statement.prepare(
                "SELECT RDB$DESCRIPTION AS \"Description\", RDB$RELATION_ID, RDB$SECURITY_CLASS, RDB$CHARACTER_SET_NAME " +
                        "FROM RDB$DATABASE");
        statement.close();

        statement.getExecutionPlan();
    }

    @Test
    public void test_GetExplainedExecutionPlan_unsupportedVersion() throws Exception {
        assumeFalse("Test expects explained execution plan not supported",
                getDefaultSupportInfo().supportsExplainedExecutionPlan());

        allocateStatement();
        statement.prepare(
                "SELECT RDB$DESCRIPTION AS \"Description\", RDB$RELATION_ID, RDB$SECURITY_CLASS, RDB$CHARACTER_SET_NAME " +
                        "FROM RDB$DATABASE");

        expectedException.expect(SQLFeatureNotSupportedException.class);
        expectedException.expect(fbMessageStartsWith(JaybirdErrorCodes.jb_explainedExecutionPlanNotSupported));

        statement.getExplainedExecutionPlan();
    }

    @Test
    public void test_GetExplainedExecutionPlan_withStatementPrepared() throws Exception {
        assumeTrue("Test requires explained execution plan support",
                getDefaultSupportInfo().supportsExplainedExecutionPlan());

        allocateStatement();
        statement.prepare(
                "SELECT RDB$DESCRIPTION AS \"Description\", RDB$RELATION_ID, RDB$SECURITY_CLASS, RDB$CHARACTER_SET_NAME " +
                        "FROM RDB$DATABASE");

        String executionPlan = statement.getExplainedExecutionPlan();

        assertEquals("Unexpected plan for prepared statement", "Select Expression\n" +
                "    -> Table \"RDB$DATABASE\" Full Scan", executionPlan);
    }

    @Test
    public void test_GetExplainedExecutionPlan_noStatementPrepared() throws Exception {
        allocateStatement();
        expectedException.expect(SQLNonTransientException.class);
        expectedException.expectMessage("Statement not yet allocated");

        statement.getExplainedExecutionPlan();
    }

    @Test
    public void test_GetExplainedExecutionPlan_StatementClosed() throws Exception {
        expectedException.expect(SQLNonTransientException.class);
        expectedException.expectMessage("Statement closed");
        allocateStatement();
        statement.prepare(
                "SELECT RDB$DESCRIPTION AS \"Description\", RDB$RELATION_ID, RDB$SECURITY_CLASS, RDB$CHARACTER_SET_NAME " +
                        "FROM RDB$DATABASE");
        statement.close();

        statement.getExplainedExecutionPlan();
    }

    @Test
    public void test_ExecuteInsert() throws Exception {
        allocateStatement();
        statement.addStatementListener(listener);
        statement.prepare("INSERT INTO keyvalue (thekey, thevalue) VALUES (?, ?)");

        RowValue rowValue = RowValue.of(
                db.getDatatypeCoder().encodeInt(4096),
                db.getEncoding().encodeToCharset("test"));
        
        statement.execute(rowValue);

        assertNull("expected no SQL counts immediately after execute", listener.getSqlCounts());

        statement.getSqlCounts();

        assertNotNull("Expected SQL counts on listener", listener.getSqlCounts());
        assertEquals("Expected one row to have been inserted", 1, listener.getSqlCounts().getLongInsertCount());
    }

    /**
     * Test calling {@link org.firebirdsql.gds.ng.FbStatement#closeCursor()} on statement with state NEW,
     * expectation: no error, state unchanged
     */
    @Test
    public void test_CloseCursor_State_NEW() throws Exception {
        statement = db.createStatement(null);
        assumeThat(statement.getState(), equalTo(StatementState.NEW));

        statement.closeCursor();
        assertEquals(StatementState.NEW, statement.getState());
    }

    /**
     * Test calling {@link org.firebirdsql.gds.ng.FbStatement#closeCursor()} on statement with state PREPARED,
     * expectation: no error, state unchanged
     */
    @Test
    public void test_CloseCursor_State_PREPARED() throws Exception {
        allocateStatement();
        statement.prepare("SELECT * FROM RDB$DATABASE");
        assumeThat(statement.getState(), equalTo(StatementState.PREPARED));

        statement.closeCursor();
        assertEquals(StatementState.PREPARED, statement.getState());
    }

    /**
     * Test calling {@link org.firebirdsql.gds.ng.FbStatement#closeCursor()} on statement with state CURSOR_OPEN,
     * expectation: no error, state PREPARED
     */
    @Test
    public void test_CloseCursor_State_CURSOR_OPEN() throws Exception {
        allocateStatement();
        statement.prepare("SELECT * FROM RDB$DATABASE");
        statement.execute(RowValue.EMPTY_ROW_VALUE);
        assumeThat(statement.getState(), equalTo(StatementState.CURSOR_OPEN));

        statement.closeCursor();
        assertEquals(StatementState.PREPARED, statement.getState());
    }

    /**
     * Test calling {@link org.firebirdsql.gds.ng.FbStatement#closeCursor()} on statement with state CLOSED,
     * expectation: no error, state unchanged
     */
    @Test
    public void test_CloseCursor_State_CLOSED() throws Exception {
        statement = db.createStatement(null);
        statement.close();
        assumeThat(statement.getState(), equalTo(StatementState.CLOSED));

        statement.closeCursor();
        assertEquals(StatementState.CLOSED, statement.getState());
    }

    @Test
    public void testMultipleExecute() throws Exception {
        allocateStatement();
        statement.prepare(
                "SELECT RDB$DESCRIPTION AS \"Description\", RDB$RELATION_ID, RDB$SECURITY_CLASS, RDB$CHARACTER_SET_NAME " +
                        "FROM RDB$DATABASE");

        final SimpleStatementListener statementListener = new SimpleStatementListener();
        statement.addStatementListener(statementListener);

        statement.execute(RowValue.EMPTY_ROW_VALUE);

        assertEquals("Expected hasResultSet to be set to true", Boolean.TRUE, statementListener.hasResultSet());
        assertEquals("Expected hasSingletonResult to be set to false", Boolean.FALSE, statementListener.hasSingletonResult());
        assertNull("Expected allRowsFetched not set yet", statementListener.isAllRowsFetched());
        assertEquals("Expected no rows to be fetched yet", 0, statementListener.getRows().size());

        statement.fetchRows(10);

        assertEquals("Expected allRowsFetched to be set to true", Boolean.TRUE, statementListener.isAllRowsFetched());
        assertEquals("Expected a single row to have been fetched", 1, statementListener.getRows().size());

        statement.closeCursor();
        final SimpleStatementListener statementListener2 = new SimpleStatementListener();
        statement.addStatementListener(statementListener2);
        statement.execute(RowValue.EMPTY_ROW_VALUE);

        assertEquals("Expected hasResultSet to be set to true", Boolean.TRUE, statementListener2.hasResultSet());
        assertEquals("Expected hasSingletonResult to be set to false", Boolean.FALSE, statementListener2.hasSingletonResult());
        assertNull("Expected allRowsFetched not set yet", statementListener2.isAllRowsFetched());
        assertEquals("Expected no rows to be fetched yet", 0, statementListener2.getRows().size());

        statement.fetchRows(10);

        assertEquals("Expected allRowsFetched to be set to true", Boolean.TRUE, statementListener2.isAllRowsFetched());
        assertEquals("Expected a single row to have been fetched", 1, statementListener2.getRows().size());
    }

    @Test
    public void testMultiplePrepare() throws Exception {
        allocateStatement();
        statement.prepare(
                "SELECT RDB$DESCRIPTION AS \"Description\", RDB$RELATION_ID, RDB$SECURITY_CLASS, RDB$CHARACTER_SET_NAME " +
                        "FROM RDB$DATABASE");

        final SimpleStatementListener statementListener = new SimpleStatementListener();
        statement.addStatementListener(statementListener);

        statement.execute(RowValue.EMPTY_ROW_VALUE);

        assertEquals("Expected hasResultSet to be set to true", Boolean.TRUE, statementListener.hasResultSet());
        assertEquals("Expected hasSingletonResult to be set to false", Boolean.FALSE, statementListener.hasSingletonResult());
        assertNull("Expected allRowsFetched not set yet", statementListener.isAllRowsFetched());
        assertEquals("Expected no rows to be fetched yet", 0, statementListener.getRows().size());

        statement.fetchRows(10);

        assertEquals("Expected allRowsFetched to be set to true", Boolean.TRUE, statementListener.isAllRowsFetched());
        assertEquals("Expected a single row to have been fetched", 1, statementListener.getRows().size());

        statement.closeCursor();

        statement.prepare(
                "SELECT RDB$DESCRIPTION AS \"Description\", RDB$RELATION_ID, RDB$SECURITY_CLASS, RDB$CHARACTER_SET_NAME " +
                        "FROM RDB$DATABASE");

        final SimpleStatementListener statementListener2 = new SimpleStatementListener();
        statement.addStatementListener(statementListener2);
        statement.execute(RowValue.EMPTY_ROW_VALUE);

        assertEquals("Expected hasResultSet to be set to true", Boolean.TRUE, statementListener2.hasResultSet());
        assertEquals("Expected hasSingletonResult to be set to false", Boolean.FALSE, statementListener2.hasSingletonResult());
        assertNull("Expected allRowsFetched not set yet", statementListener2.isAllRowsFetched());
        assertEquals("Expected no rows to be fetched yet", 0, statementListener2.getRows().size());

        statement.fetchRows(10);

        assertEquals("Expected allRowsFetched to be set to true", Boolean.TRUE, statementListener2.isAllRowsFetched());
        assertEquals("Expected a single row to have been fetched", 1, statementListener2.getRows().size());
    }

    @Test
    public void testSetCursorName() throws Exception {
        allocateStatement();

        statement.prepare(
                "SELECT RDB$DESCRIPTION AS \"Description\", RDB$RELATION_ID, RDB$SECURITY_CLASS, RDB$CHARACTER_SET_NAME " +
                        "FROM RDB$DATABASE");

        statement.setCursorName("abc1");

        // Just checking if this doesn't throw errors
        // TODO: Add/check existing tests using cursorName.
    }

    @Test
    public void testInsertSelectUTF8Value() throws Exception {
        allocateStatement();

        // Insert UTF8 columns
        statement.prepare(INSERT_THEUTFVALUE);
        final Encoding utf8Encoding = db.getEncodingFactory().getEncodingForFirebirdName("UTF8");
        final String aEuro = "a\u20AC";
        final byte[] insertFieldData = utf8Encoding.encodeToCharset(aEuro);
        final RowDescriptor parametersInsert = statement.getParameterDescriptor();
        final RowValue parameterValuesInsert = RowValue.of(parametersInsert,
                db.getDatatypeCoder().encodeInt(1),
                insertFieldData,
                insertFieldData);
        statement.execute(parameterValuesInsert);

        // Retrieve the just inserted UTF8 values from the database for comparison
        statement.prepare(SELECT_THEUTFVALUE);
        final RowDescriptor parametersSelect = statement.getParameterDescriptor();
        final RowValue parameterValuesSelect = RowValue.of(parametersSelect,
                db.getDatatypeCoder().encodeInt(1));
        final SimpleStatementListener statementListener = new SimpleStatementListener();
        statement.addStatementListener(statementListener);
        statement.execute(parameterValuesSelect);
        statement.fetchRows(1);

        final List<RowValue> rows = statementListener.getRows();
        assertEquals("Expected a row", 1, rows.size());
        final RowValue selectResult = rows.get(0);
        final byte[] selectVarcharFieldData = selectResult.getFieldData(0);
        final byte[] selectCharFieldData = selectResult.getFieldData(1);
        assertEquals("Length of selected varchar field data", 4, selectVarcharFieldData.length);
        assertEquals("Length of selected char field data", 20, selectCharFieldData.length);

        String decodedVarchar = utf8Encoding.decodeFromCharset(selectVarcharFieldData);
        String decodedChar = utf8Encoding.decodeFromCharset(selectCharFieldData);

        assertEquals("Unexpected value for varchar", aEuro, decodedVarchar);
        assertEquals("Unexpected value for trimmed char", aEuro, decodedChar.trim());
        // Note artificial result from the way UTF8 is handled
        assertEquals("Unexpected length for char", 18, decodedChar.length());
        char[] spaceChars16 = new char[16];
        Arrays.fill(spaceChars16, ' ');
        assertEquals("Unexpected trailing characters for char", new String(spaceChars16), decodedChar.substring(2));
    }

    @Test
    public void testStatementExecuteAfterExecuteError() throws Exception {
        allocateStatement();
        statement.prepare("INSERT INTO keyvalue (thekey, thevalue) VALUES (?, ?)");

        RowValue rowValue = RowValue.of(
                db.getDatatypeCoder().encodeInt(4096),
                db.getEncoding().encodeToCharset("test"));

        // Insert value
        statement.execute(rowValue);
        try {
            // Insert value again
            statement.execute(rowValue);
            fail("Expected exception");
        } catch (SQLException e) {
            // ignore
        }

        statement.addStatementListener(listener);

        listener.clear();

        // Insert another value
        RowValue differentRowValue = RowValue.of(
                db.getDatatypeCoder().encodeInt(4097),
                db.getEncoding().encodeToCharset("test"));
        statement.execute(differentRowValue);

        assertNull("expected no SQL counts immediately after execute", listener.getSqlCounts());

        statement.getSqlCounts();

        assertNotNull("Expected SQL counts on listener", listener.getSqlCounts());
        assertEquals("Expected one row to have been inserted", 1, listener.getSqlCounts().getLongInsertCount());
    }

    @Test
    public void testStatementPrepareAfterExecuteError() throws Exception {
        allocateStatement();
        statement.prepare("INSERT INTO keyvalue (thekey, thevalue) VALUES (?, ?)");

        RowValue rowValue = RowValue.of(
                db.getDatatypeCoder().encodeInt(4096),
                db.getEncoding().encodeToCharset("test"));

        // Insert value
        statement.execute(rowValue);
        try {
            // Insert value again
            statement.execute(rowValue);
            fail("Expected exception");
        } catch (SQLException e) {
            // ignore
        }

        statement.prepare("INSERT INTO keyvalue (thekey, theUTFVarcharValue) VALUES (?, ?)");
        RowValue differentRowValue = RowValue.of(
                db.getDatatypeCoder().encodeInt(4097),
                db.getEncoding().encodeToCharset("test"));
        statement.execute(differentRowValue);
    }

    @Test
    public void testStatementPrepareAfterPrepareError() throws Exception {
        allocateStatement();
        try {
            // Prepare statement with typo
            statement.prepare("INSRT INTO keyvalue (thekey, thevalue) VALUES (?, ?)");
            fail("Expected exception");
        } catch (SQLException e) {
            // ignore
        }
        statement.prepare("INSERT INTO keyvalue (thekey, thevalue) VALUES (?, ?)");
    }

    @Test
    public void testStatementPrepareLongObjectNames() throws Exception {
        assumeTrue("Test requires 63 character identifier support",
                supportInfoFor(db).maxIdentifierLengthCharacters() >= 63);

        String tableName = generateIdentifier('A', 63);
        String column1 = generateIdentifier('B', 63);
        String column2 = generateIdentifier('C', 63);
        try (Connection con = FBTestProperties.getConnectionViaDriverManager()) {
            DdlHelper.executeCreateTable(con,
                    "create table " + tableName + " (" + column1 + " varchar(10) character set UTF8, " + column2 + " varchar(20) character set UTF8)");
        }

        allocateStatement();
        statement.prepare(
                "SELECT " + column1 + ", " + column2 + " FROM " + tableName + " where " + column1 + " = ?");

        assertEquals("Unexpected StatementType", StatementType.SELECT, statement.getType());

        final RowDescriptor fields = statement.getRowDescriptor();
        assertNotNull("Fields", fields);
        List<FieldDescriptor> expectedFields =
                Arrays.asList(
                        new FieldDescriptor(0, db.getDatatypeCoder(), ISCConstants.SQL_VARYING | 1, 4,
                                0, 40, column1, null, column1, tableName, "SYSDBA"),
                        new FieldDescriptor(1, db.getDatatypeCoder(), ISCConstants.SQL_VARYING | 1, 4,
                                0, 80, column2, null, column2, tableName, "SYSDBA")
                );
        assertEquals("Unexpected values for fields", expectedFields, fields.getFieldDescriptors());
        RowDescriptor parameters = statement.getParameterDescriptor();
        assertNotNull("Parameters", parameters);

        List<FieldDescriptor> expectedParameters =
                Collections.singletonList(
                        new FieldDescriptor(0, db.getDatatypeCoder(), ISCConstants.SQL_VARYING | 1, 4,
                                0, 40, null, null, null, null, null));
        assertEquals("Unexpected values for parameters", expectedParameters, parameters.getFieldDescriptors());
    }

    private FbTransaction getTransaction() throws SQLException {
        TransactionParameterBuffer tpb = new TransactionParameterBufferImpl();
        tpb.addArgument(ISCConstants.isc_tpb_read_committed);
        tpb.addArgument(ISCConstants.isc_tpb_rec_version);
        tpb.addArgument(ISCConstants.isc_tpb_write);
        tpb.addArgument(ISCConstants.isc_tpb_wait);
        return db.startTransaction(tpb);
    }

    protected void allocateStatement() throws SQLException {
        if (transaction == null || transaction.getState() != TransactionState.ACTIVE) {
            transaction = getTransaction();
        }
        statement = db.createStatement(transaction);
    }

    @After
    public final void tearDown() throws Exception {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ex) {
                System.out.println("Exception on statement close");
                ex.printStackTrace();
            }
        }
        if (transaction != null) {
            try {
                transaction.commit();
            } catch (SQLException ex) {
                System.out.println("Exception on transaction commit");
                ex.printStackTrace();
            }
        }
        if (db != null) {
            try {
                db.close();
            } catch (SQLException ex) {
                System.out.println("Exception on detach");
                ex.printStackTrace();
            }
        }
    }

    protected static String generateIdentifier(final char identifierChar,final int length) {
        StringBuilder sb = new StringBuilder(length);
        int tempLength = length;
        while (tempLength-- > 0) {
            sb.append(identifierChar);
        }
        assert sb.length() == length;
        return sb.toString();
    }
}
