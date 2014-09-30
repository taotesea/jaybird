/*
 * $Id$
 *
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
package org.firebirdsql.jna.fbclient;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * JNA wrapper for GDS_QUAD_t.
 * <p>
 * This file was initially autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>, a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.
 * </p>
 * <p>
 * This file was modified manually, <strong>do not automatically regenerate!</strong>
 * </p>
 * @since 3.0
 */
public class GDS_QUAD_t extends Structure {
	/// C type : ISC_LONG
	public int gds_quad_high;
	/// C type : ISC_ULONG
	public int gds_quad_low;
	public GDS_QUAD_t() {
		super();
	}

    @Override
    protected List getFieldOrder() {
        return Arrays.asList("gds_quad_high", "gds_quad_low");
    }

    /**
	 * @param gds_quad_high C type : ISC_LONG<br>
	 * @param gds_quad_low C type : ISC_ULONG
	 */
	public GDS_QUAD_t(int gds_quad_high, int gds_quad_low) {
		super();
		this.gds_quad_high = gds_quad_high;
		this.gds_quad_low = gds_quad_low;
	}
	public static class ByReference extends GDS_QUAD_t implements Structure.ByReference {
	}
	public static class ByValue extends GDS_QUAD_t implements Structure.ByValue {
	}
}
