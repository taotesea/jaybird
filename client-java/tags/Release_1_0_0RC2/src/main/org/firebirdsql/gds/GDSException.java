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


/*
 * CVS modification log:
 * $Log$
 * Revision 1.6  2002/12/12 18:54:02  d_jencks
 * Added fatal error checking, so ConnectionErrorOccurred notifications are sent when a connection becomes unusable.  Improved the ability  to destroy a dead or dying connection.  Throw more correct XAExceptions.  Fixed npe in ResultSetMetaData.
 *
 * Revision 1.5  2002/11/22 02:30:38  brodsom
 * 1.- Make most variables private in stmt_handle, blob_handle, db_handle and others
 * 2.- Move constants from GDS to ISCConstants (class,1100 lines)
 *
 * Revision 1.4  2002/10/18 14:21:28  rrokytskyy
 * fixed warnings handling
 *
 * Revision 1.3  2002/10/07 18:54:56  rrokytskyy
 * improved exception handling
 *
 * Revision 1.2  2002/09/18 23:10:29  rrokytskyy
 * f
 *
 * Revision 1.1  2002/08/29 13:41:01  d_jencks
 * Changed to lgpl only license.  Moved driver to subdirectory to make build system more consistent.
 *
 * Revision 1.9  2002/08/09 21:25:56  rrokytskyy
 * improved exception handling
 *
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


/**
 * Describe class <code>GDSException</code> here.
 *
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @author <a href="mailto:rrokytskyy@users.sourceforge.net">Roman Rokytskyy</a>
 * @version 1.0
 */
public class GDSException extends Exception {

    protected int type;
    protected int intParam;
    protected String strParam;

    /**
     * The variable <code>xaErrorCode</code> is used to allow the same
     * code to be used for transaction control from the XAResource,
     * LocalTransaction, and Connection.  This code may be added to
     * the GDSException without obscuring the message: only at the
     * final level is the GDSException converted to the spec-required
     * exception.
     *
     */
    protected int xaErrorCode;

    /**
     * My child
     */
    protected GDSException next;

    public static GDSException createWithXAErrorCode(String message, int xaErrorCode)
    {
        GDSException gdse = new GDSException(message);
        gdse.setXAErrorCode(xaErrorCode);
        return gdse;
    }

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
        setNext(new GDSException(ISCConstants.isc_arg_string, strParam));
    }

    public GDSException(int fbErrorCode) {
        // this.fbErrorCode = fbErrorCode;
        this.intParam = fbErrorCode;
        this.type = ISCConstants.isc_arg_gds;
    }

    public GDSException(String message) {
        super(message);
        this.type = ISCConstants.isc_arg_string;
    }

    public int getFbErrorCode() {
        //return fbErrorCode;
        if (type == ISCConstants.isc_arg_number)
            return intParam;
        else
            return -1;
    }

    public int getIntParam() {
        return intParam;
    }


    /**
     * Get the XaErrorCode value.
     * @return the XaErrorCode value.
     */
    public int getXAErrorCode()
    {
        return xaErrorCode;
    }

    /**
     * Set the XaErrorCode value.
     * @param newXaErrorCode The new XaErrorCode value.
     */
    public void setXAErrorCode(int xaErrorCode)
    {
        this.xaErrorCode = xaErrorCode;
    }

    
    
    public void setNext(GDSException e) {
        next = e;
    }

    public GDSException getNext() {
        return next;
    }
    
    public boolean isWarning() {
        return type == ISCConstants.isc_arg_warning;
    }

    /**
     * Returns a string representation of this exception.
     */
    public String getMessage() {
        String msg;
        
        GDSException child = this.next;
        
        // If I represent a GDSMessage code, then let's format it nicely.
        if (type == ISCConstants.isc_arg_gds || type == ISCConstants.isc_arg_warning) {
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
        //while (child != null) {
        if (child != null)
            msg += "\n" + child.getMessage();
        //    child = child.next;
        //}
 
        return msg;
    }


    public boolean isFatal()
    {
        return isThisFatal() || (next != null && next.isFatal());
    }


    private boolean isThisFatal()
    {
        for (int i = 0; i < ISCConstants.FATAL_ERRORS.length 
                 && intParam >= ISCConstants.FATAL_ERRORS[i]; i++)
        {
            if (intParam == ISCConstants.FATAL_ERRORS[i]) 
            {
                return true;
            } // end of if ()
            
        } // end of for ()
        return false;
    }


    /**
     * Returns the parameter depending on the type of the
     * error code.
     */
    protected String getParam() {
        if ((type == ISCConstants.isc_arg_interpreted) ||
                (type == ISCConstants.isc_arg_string))
            return strParam;
        else
        if (type == ISCConstants.isc_arg_number)
            return "" + intParam;
        else
            return "";
    }

}
