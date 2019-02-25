//
// Copyright (C) 1996 1997 1998 1999 by FUJITSU LABORATRIES LTD.
//
// $Revision: 1.3 $
// $Date: 1999/09/24 04:19:29 $
// $Author: kouya $
//

package org.OpenJIT;

public class ExceptionHandler {
    public int startPC;		
    public int endPC;
    public int handlerPC;
    public int catchType;

    static void Set(Compile compile) {
	BCinfo[] bcinfo = compile.bcinfo;
	ExceptionHandler[] list = compile.exceptionHandler;
	for (int i = 0; i < list.length; i++) {
	    ExceptionHandler x = list[i];

	    x.startPC = bcinfo[x.startPC].native_pc;
	    x.endPC = bcinfo[x.endPC].native_pc;
	    x.handlerPC = bcinfo[x.handlerPC].native_pc;
	}
    }
}
