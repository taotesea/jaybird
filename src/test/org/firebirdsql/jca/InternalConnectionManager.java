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
package org.firebirdsql.jca;

import javax.resource.ResourceException;
import javax.resource.spi.*;
import java.io.PrintWriter;
import java.io.Serializable;

public class InternalConnectionManager implements ConnectionManager,
        ConnectionEventListener, Serializable {

    public InternalConnectionManager() {
        super();
    }

    public Object allocateConnection(ManagedConnectionFactory mcf,
            ConnectionRequestInfo cxRequestInfo) throws ResourceException {

        FBManagedConnection mc = (FBManagedConnection) mcf.createManagedConnection(null, cxRequestInfo);
        mc.setManagedEnvironment(true);
        mc.setConnectionSharing(true);
        mc.addConnectionEventListener(this);
        return mc.getConnection(null, cxRequestInfo);
    }

    public void connectionClosed(ConnectionEvent event) {
        PrintWriter externalLog = ((FBManagedConnection) event.getSource()).getLogWriter();
        try {
            ((FBManagedConnection) event.getSource()).destroy(event);
        } catch (ResourceException e) {
            if (externalLog != null) externalLog.println("Exception closing unmanaged connection: " + e);
        }
    }

    public void localTransactionStarted(ConnectionEvent event) {
    }

    public void localTransactionCommitted(ConnectionEvent event) {
    }

    public void localTransactionRolledback(ConnectionEvent event) {
    }

    public void connectionErrorOccurred(ConnectionEvent event) {
        PrintWriter externalLog = ((FBManagedConnection) event.getSource()).getLogWriter();
        try {
            ((FBManagedConnection) event.getSource()).destroy(event);
        } catch (ResourceException e) {
            if (externalLog != null) externalLog.println("Exception closing unmanaged connection: " + e);
        }
    }
}
