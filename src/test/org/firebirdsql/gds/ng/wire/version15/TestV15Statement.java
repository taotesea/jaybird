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
package org.firebirdsql.gds.ng.wire.version15;

import org.firebirdsql.common.rules.RequireProtocol;
import org.firebirdsql.gds.ng.wire.version13.TestV13Statement;
import org.junit.ClassRule;

import static org.firebirdsql.common.rules.RequireProtocol.requireProtocolVersion;

/**
 * Tests for {@link org.firebirdsql.gds.ng.wire.version12.V12Statement} in the V15 protocol, reuses test for V13.
 *
 * @author <a href="mailto:mrotteveel@users.sourceforge.net">Mark Rotteveel</a>
 * @since 4.0
 */
public class TestV15Statement extends TestV13Statement {

    @ClassRule
    public static final RequireProtocol requireProtocol = requireProtocolVersion(15);

    public TestV15Statement() {
        this(new V15CommonConnectionInfo());
    }

    protected TestV15Statement(V15CommonConnectionInfo commonConnectionInfo) {
        super(commonConnectionInfo);
    }
}
