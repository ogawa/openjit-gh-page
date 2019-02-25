//
// Copyright (C) 1996 1997 1998 1999 by FUJITSU LABORATRIES LTD.
//
// $Revision: 1.3 $
// $Date: 1999/09/24 04:19:28 $
// $Author: kouya $
//

package org.OpenJIT;

public
class CompilerError extends Error {
    Throwable e;

    /**
     * Constructor
     */
    public CompilerError(String msg) {
	super(msg);
	this.e = this;
    }

    /**
     * Create an exception given another exception.
     */
    public CompilerError(Exception e) {
	super(e.getMessage());
	this.e = e;
    }

    public void printStackTrace() {
	if (e == this)
	    super.printStackTrace();
	else
	    e.printStackTrace();
    }
}
