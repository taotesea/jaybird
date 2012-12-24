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
package org.firebirdsql.common;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import junit.framework.TestCase;

/**
 * Base class for test cases which can be run against only a single GDS
 * implementation.
 */
public class SimpleFBTestBase extends TestCase {
	private static ResourceBundle testDefaults = ResourceBundle.getBundle("unit_test_defaults");

	private static String getProperty(String property, String defaultValue) {
		try {
			return System.getProperty(property, testDefaults
					.getString(property));
		} catch (MissingResourceException ex) {
			return System.getProperty(property, defaultValue);
		}
	}

	/**
	 * Default name of database file to use for the test case.
	 */
	protected final String DB_NAME = "fbtest.fdb";

	protected final String DB_USER = getProperty("test.user", "sysdba");
	protected final String DB_PASSWORD = getProperty("test.password", "masterkey");

	protected static final String DB_PATH = getProperty("test.db.dir", "");
	protected static final String DB_SERVER_URL = getProperty("test.db.host", "localhost");
	protected static final int DB_SERVER_PORT = Integer.parseInt(getProperty("test.db.port", "3050"));

	/**
	 * Builds an firebird database connection string for the supplied database
	 * file.
	 * 
	 * @param name
	 * @return
	 */
	protected String getdbpath(String name) {
		if ("EMBEDDED".equalsIgnoreCase(getProperty("test.gds_type", null)))
			return DB_PATH + "/" + name;
		else if ("LOCAL".equalsIgnoreCase(getProperty("test.gds_type", null)))
			return DB_PATH + "/" + name;
		else
			return DB_SERVER_URL + "/" + DB_SERVER_PORT + ":" + DB_PATH + "/"
					+ name;
	}

	/**
	 * 
	 * @param s
	 */
	protected SimpleFBTestBase(String s) {
		super(s);
	}
}