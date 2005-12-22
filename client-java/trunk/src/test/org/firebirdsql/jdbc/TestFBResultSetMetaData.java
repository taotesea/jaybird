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

import org.firebirdsql.common.FBTestBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Properties;

/**
 * This method tests correctness of {@link FBResultSetMetaData} class.
 *
 * @author <a href="mailto:rrokytskyy@users.sourceforge.net">Roman Rokytskyy</a>
 * @version 1.0
 */
public class TestFBResultSetMetaData extends FBTestBase {
    
    public static String CREATE_TABLE = 
        "CREATE TABLE test_rs_metadata (" + 
        "  id INTEGER NOT NULL PRIMARY KEY, " +
        "  simple_field VARCHAR(60) CHARACTER SET WIN1250, " +
        "  two_byte_field VARCHAR(60) CHARACTER SET BIG_5, " +
        "  three_byte_field VARCHAR(60) CHARACTER SET UNICODE_FSS, " +
        "  long_field NUMERIC(15,2), " +
        "  int_field NUMERIC(8, 2), " +
        "  short_field NUMERIC(4, 2) " +
        ")";
        
    public static final String TEST_QUERY = 
        "SELECT " + 
        "simple_field, two_byte_field, three_byte_field, " + 
        "long_field, int_field, short_field " + 
        "FROM test_rs_metadata";
    
    public static final String TEST_QUERY2 = 
        "SELECT * from RDB$DATABASE";
        
    public static String DROP_TABLE = 
        "DROP TABLE test_rs_metadata";
    
    public TestFBResultSetMetaData(String testName) {
        super(testName);
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        
        Class.forName(FBDriver.class.getName());
        
        Properties props = new Properties();
        props.putAll(this.getDefaultPropertiesForConnection());
        props.put("lc_ctype", "UNICODE_FSS");
        
        Connection connection = DriverManager.getConnection(this.getUrl(), props);
        
        Statement stmt = connection.createStatement();
        try {
            stmt.executeUpdate(DROP_TABLE);
        }
        catch (Exception e) {}

        stmt.executeUpdate(CREATE_TABLE);
        stmt.close();        
        
        connection.close();
    }

    protected void tearDown() throws Exception {
        /*
        Properties props = new Properties();
        props.putAll(DB_INFO);
        props.put("lc_ctype", "NONE");
        
        Connection connection = 
            DriverManager.getConnection(DB_DRIVER_URL, props);
            
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(DROP_TABLE);
        stmt.close();
        connection.close();      
        */
        
        super.tearDown();
    }
    
    public void testResultSetMetaData() throws Exception {
        Properties props = new Properties();
        props.putAll(this.getDefaultPropertiesForConnection());
        props.put("lc_ctype", "UNICODE_FSS");
        
        Connection connection = DriverManager.getConnection(this.getUrl(), props);
        
        Statement stmt = connection.createStatement();
        
        ResultSet rs = stmt.executeQuery(TEST_QUERY);
        
        ResultSetMetaData metaData = rs.getMetaData();
        
        assertTrue("simple_field must have size 60", 
            metaData.getPrecision(1) == 60);
            
        assertTrue("two_byte_field must have size 60", 
            metaData.getPrecision(2) == 60);

        assertTrue("three_byte_field must have size 60", 
            metaData.getPrecision(3) == 60);

        assertTrue("long_field must have precision 15", 
            metaData.getPrecision(4) == 15);

        assertTrue("int_field must have precision 8", 
            metaData.getPrecision(5) == 8);

        assertTrue("short_field must have precision 4", 
            metaData.getPrecision(6) == 4);

        stmt.close();
        connection.close();
    }
    
    public void testResultSetMetaData2() throws Exception {
        Properties props = new Properties();
        props.putAll(this.getDefaultPropertiesForConnection());
        props.put("lc_ctype", "UNICODE_FSS");
        
        Connection connection = DriverManager.getConnection(this.getUrl(), props);
        
        Statement stmt = connection.createStatement();
        
        ResultSet rs = stmt.executeQuery(TEST_QUERY2);
        
        ResultSetMetaData metaData = rs.getMetaData();
        		  
        assertTrue("RDB$SECURITY_CLASS must have display size 31 ",metaData.getColumnDisplaySize(3)==10);
        assertTrue("RDB$CHARACTER_SET_NAME must have display size 31 ",metaData.getColumnDisplaySize(4)==10);
        
        stmt.close();
        connection.close();
    }
    
	public void testColumnTypeName() throws Exception {
		
		Properties props = new Properties();
		props.putAll(this.getDefaultPropertiesForConnection());
		props.put("lc_ctype", "UNICODE_FSS");

		Connection connection = 
			DriverManager.getConnection(this.getUrl(), props);

		Statement stmt = connection.createStatement();

		ResultSet rs = stmt.executeQuery(TEST_QUERY);

		ResultSetMetaData metaData = rs.getMetaData();

		assertTrue("simple_field must be of type VARCHAR",
				metaData.getColumnTypeName(1).equals("VARCHAR"));

		assertTrue("two_byte_field must be of type VARCHAR",
				metaData.getColumnTypeName(2).equals("VARCHAR"));

		assertTrue("three_byte_field must be of type VARCHAR",
				metaData.getColumnTypeName(3).equals("VARCHAR"));

		assertTrue("long_field must be of type NUMERIC",
				metaData.getColumnTypeName(4).equals("NUMERIC"));

		assertTrue("int_field must be of type NUMERIC",
				metaData.getColumnTypeName(5).equals("NUMERIC"));

		assertTrue("short_field must be of type NUMERIC",
				metaData.getColumnTypeName(6).equals("NUMERIC"));

		stmt.close();
		connection.close();
		
	}

}
