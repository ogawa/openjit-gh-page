//
// Copyright (C) 1996 1997 1998 1999 by FUJITSU LABORATRIES LTD.
//
// $Revision: 1.5 $
// $Date: 1999/09/24 04:19:28 $
// $Author: kouya $
//

package org.OpenJIT;

public final class BCinfo extends LinkedList
{
    final static int FLAG_BRANCH_TARGET	    = (1<<0);
    final static int FLAG_BRANCH_NOT_TAKEN  = (1<<1);
    final static int FLAG_CTRLFLOW_END      = (1<<2);
    final static int FLAG_EXCEPTION_HANDLER = (1<<3);
    final static int FLAG_IN_TRY_BLOCK      = (1<<4);
    final static int FLAG_CTRLFLOW_DONE     = (1<<5);

    int flag;
    int stack_delta;
    int stack_size;
    int native_pc;

    public BCinfo() {
	native_pc = -1;
    }
}
