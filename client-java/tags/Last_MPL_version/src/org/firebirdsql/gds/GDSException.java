/*
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Original developer David Jencks
 *
 * Contributor(s):
 *  Roman Rokytskyy
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the GNU Lesser General Public License Version 2.1 or later
 * (the "LGPL"), in which case the provisions of the LGPL are applicable
 * instead of those above.  If you wish to allow use of your
 * version of this file only under the terms of the LGPL and not to
 * allow others to use your version of this file under the MPL,
 * indicate your decision by deleting the provisions above and
 * replace them with the notice and other provisions required by
 * the LGPL.  If you do not delete the provisions above, a recipient
 * may use your version of this file under either the MPL or the
 * LGPL.
 */

/*
 * CVS modification log:
 * $Log$
 * Revision 1.8  2002/07/10 23:11:31  rrokytskyy
 * committed improvements in exception handling by Todd Jonker
 *
 * Revision 1.7  2002/06/02 09:56:38  rrokytskyy
 * added method to obtain IB error code, thanks to Ken Richard
 *
 * Revision 1.6  2002/02/26 20:46:20  rrokytskyy
 * switched from toString() to getMessage() use
 *
 * Revision 1.5  2001/10/16 18:11:41  alberola
 * Fixed a bug in toString()
 *
 * Revision 1.4  2001/08/28 17:13:23  d_jencks
 * Improved formatting slightly, removed dos cr's
 *
 * Revision 1.3  2001/07/18 20:07:31  d_jencks
 * Added better GDSExceptions, new NativeSQL, and CallableStatement test from Roman Rokytskyy
 *
 */

package org.firebirdsql.gds;

public class GDSException extends Exception {

    // protected int fbErrorCode = 0;
    protected int type;
    protected int intParam;
    protected String strParam;

    /**
     * Returns the parameter depending on the type of the
     * error code.
     */
    protected String getParam() {
        if ((type == GDS.isc_arg_interpreted) ||
                (type == GDS.isc_arg_string))
            return strParam;
        else
        if (type == GDS.isc_arg_number)
            return "" + intParam;
        else
            return "";
    }

    /**
     * My child
     */
    protected GDSException next;

    public GDSException(int type, int intParam) {
        this.type = type;
        this.intParam = intParam;
    }

    public GDSException(int type, String strParam) {
        this.type = type;
        this.strParam = strParam;
    }
    
    /**
     * Construct instance of this class. This method correctly constructs
     * chain of exceptions for one string parameter.
     * 
     * @param type type of the exception, should be always 
     * {@link GDS#isc_arg_gds}, otherwise no message will be displayed.
     * 
     * @param fbErrorCode Firebird error code, one of the constants declared
     * in {@link GDS} interface.
     * 
     * @param strParam value of the string parameter that will substitute 
     * <code>{0}</code> entry in error message corresponding to the specified
     * error code.
     */
    public GDSException(int type, int fbErrorCode, String strParam) {
        this.type = type;
        this.intParam = fbErrorCode;
        setNext(new GDSException(GDS.isc_arg_string, strParam));
    }

    public GDSException(int fbErrorCode) {
        // this.fbErrorCode = fbErrorCode;
        this.intParam = fbErrorCode;
        this.type = GDS.isc_arg_gds;
    }

    public GDSException(String message) {
        super(message);
        this.type = GDS.isc_arg_string;
    }

    public int getFbErrorCode() {
        //return fbErrorCode;
        if (type == GDS.isc_arg_number)
            return intParam;
        else
            return -1;
    }

    public int getIntParam() {
        return intParam;
    }
    
    public void setNext(GDSException e) {
        next = e;
    }

    public GDSException getNext() {
        return next;
    }

    /*
    public String toString() {
        //this should really include the message, too
        String s = "GDSException: " + fbErrorCode + ": ";
        s += GDSExceptionHelper.getMessage(fbErrorCode);
        s += "\n";
        if (next != null) {
            s += next.toString();
        }
        return s;
    }
    */

    /**
     * Returns a string representation of this exception.
     */
    public String getMessage() {
        String msg;
        
        GDSException child = this.next;
        
        // If I represent a GDSMessage code, then let's format it nicely.
        if (type == GDS.isc_arg_gds) {
            // get message
            GDSExceptionHelper.GDSMessage message =
                GDSExceptionHelper.getMessage(intParam);

            // substitute parameters using my children
            int paramCount = message.getParamCount();
            for(int i = 0; i < paramCount; i++) {
                if (child == null) break;
                message.setParameter(i, child.getParam());
                child = child.next;
            }

            // convert message to string
            msg = message.toString();
        }
        else {
            // No GDSMessage code, so use the default message.
            msg = super.getMessage();
        }
  
        // Do we have more children? Then include their messages too.
        while (child != null) {
            msg += "\n" + child.getMessage();
            child = child.next;
        }
 
        return msg;
    }

}
