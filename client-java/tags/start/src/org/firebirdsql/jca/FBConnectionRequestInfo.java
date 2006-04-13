/*   The contents of this file are subject to the Mozilla Public
 *   License Version 1.1 (the "License"); you may not use this file
 *   except in compliance with the License. You may obtain a copy of
 *   the License at http://www.mozilla.org/MPL/
 *   Alternatively, the contents of this file may be used under the
 *   terms of the GNU Lesser General Public License Version 2 or later (the
 *   "LGPL"), in which case the provisions of the GPL are applicable
 *   instead of those above. You may obtain a copy of the Licence at
 *   http://www.gnu.org/copyleft/lgpl.html
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    relevant License for more details.
 *
 *    This file was created by members of the firebird development team.
 *    All individual contributions remain the Copyright (C) of those
 *    individuals.  Contributors to this file are either listed here or
 *    can be obtained from a CVS history command.
 *
 *    All rights reserved.

 */

package org.firebirdsql.jca;


// imports --------------------------------------
import javax.resource.spi.ConnectionRequestInfo;

import javax.resource.ResourceException;


/**
 *
 *   @see <related>
 *   @author David Jencks (davidjencks@earthlink.net)
 *   @version $ $
 */
 
/**The ConnectionRequestInfo interface enables a resource adapter to pass its own request specific data
structure across the connection request flow. A resource adapter extends the empty interface to
supports its own data structures for connection request. 

A typical use allows a resource adapter to handle application component specified per-connection
request properties (example - client ID, language). The application server passes these properties back
across to match/createManagedConnection calls on the resource adapter. These properties remain
opaque to the application server during the connection request flow. 

Once the ConnectionRequestInfo reaches match/createManagedConnection methods on the
ManagedConnectionFactory instance, resource adapter uses this additional per-request information
to do connection creation and matching. 
**/

public class FBConnectionRequestInfo implements ConnectionRequestInfo {
     
    /**
     Checks whether this instance is equal to another. Since connectionRequestInfo is defined
     specific to a resource adapter, the resource adapter is required to implement this method. The
     conditions for equality are specific to the resource adapter.
     Overrides:
         equals in class java.lang.Object
     Returns:
         True if the two instances are equal.
    **/


    public boolean equals(java.lang.Object other) {
        return false;//not yet implemented
    }

    /**
     Returns the hashCode of the ConnectionRequestInfo.
     Overrides:
         hashCode in class java.lang.Object
     Returns:
         hash code os this instance
    **/
     
    public int hashCode() {
        return 0;//not yet implemented;
    }

}