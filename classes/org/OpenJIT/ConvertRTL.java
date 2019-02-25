//
// Copyright (C) 1996 1997 1998 1999 by FUJITSU LABORATRIES LTD.
//
// $Revision: 1.14 $
// $Date: 1999/09/27 04:52:02 $
// $Author: kouya $
//

package org.OpenJIT;

public abstract class ConvertRTL extends ParseBytecode {

    private final int convBB(int pc, byte stackTag[], boolean is_subr) {
	BCinfo bc = bcinfo[pc];
	int cur_stack_size;
	int new_stack_size;

	// already converted
	if (!is_subr && (bc.flag & BCinfo.FLAG_CTRLFLOW_DONE) != 0)
	    return -1;

    loop:
	while(true) {
	    if ((debug & DEBUG_RTL) != 0)
		System.err.println("*** BEGIN BASIC BLOCK:" + pc);

	    bc = bcinfo[pc];
	    new_stack_size = bc.stack_size;

	    for (; pc < bytecode_length; pc++) {
		boolean is_converted = false;

		bc = bcinfo[pc];
		if (bc == null)
		    continue;

		// already converted
		if ((bc.flag & BCinfo.FLAG_CTRLFLOW_DONE) != 0) {
		    if (!is_subr)
			return -1;
		    is_converted = true;
		}
		bc.flag |= BCinfo.FLAG_CTRLFLOW_DONE;

		cur_stack_size = new_stack_size;
		new_stack_size = cur_stack_size + bc.stack_delta;
		bc.stack_size = cur_stack_size;

		if ((debug & DEBUG_RTL) != 0)
		    System.err.println(pc + "\t" + cur_stack_size + 
				       "->" + new_stack_size +
				       "\t" + opcName(pc));

		for (ILnode il = bcinfo[pc].next; il != null; il = il.next) {
		    int op = il.op();
		    if (op == ILnode.BRANCH) {
			int cond = il.lval;
			int target_pc = il.rval;
			BCinfo target_bc = bcinfo[target_pc];

			if ((debug & DEBUG_RTL) != 0)
			    System.err.println("*** BRANCH HAS TARGET " + target_pc);

			if (cond == CC_A) { // absolute branch
			    if (is_subr || (target_bc.flag & BCinfo.FLAG_CTRLFLOW_DONE) == 0) {
				target_bc.stack_size = new_stack_size;
				pc = target_pc;
  				continue loop;
			    }
			    return -1;
			}
			if (cond == CC_JSR) { // subroutine call
			    // jsr pushes PC on stack
			    stackTag[new_stack_size] = (byte)TAG_PI;
			    target_bc.stack_size = new_stack_size + 1;
			    new_stack_size = convBB(target_pc, stackTag, true);
			    // FIX ME: if subroutine is ended by return(method return) 
			    //   but ret(internal routine), new_stack_size and stackTag
			    //   must be wrong.
			    if ((debug & DEBUG_RTL) != 0)
				System.err.println("*** END OF SUBROUTINE");
			    if (new_stack_size == -1)
				throw new CompilerError("Failed to traverse subroutine");
			} else {
			    if ((target_bc.flag & BCinfo.FLAG_CTRLFLOW_DONE) == 0) {
				target_bc.stack_size = new_stack_size;

				byte newStackTag[] = new byte [stackTag.length];
				System.arraycopy(stackTag, 0, newStackTag, 0, new_stack_size);
				convBB(target_pc, newStackTag, false);
			    }
			}
		    } else if (op == ILnode.MOVE) {
			if (il.lval == -1) {
			    // determin unknown stack type as dup,swap,...etc
			    int i = il.rval + cur_stack_size;
			    il.lval = 0;
			    // Is this insn a part of double word ?
			    if (i > 0 && (stackTag[i-1] & TYPE_MASK) == TYPE_DOUBLE) {
				il.remove();
				// skip to next insn
				continue;
			    }
			    byte tag = (byte)(TAG_STACK | (stackTag[i] & TYPE_MASK));
			    il.move(tag, il.dval, tag, il.rval);
			}
		    }

		    if (is_converted) {
			if (il.tagD() == TAG_STACK && !il.isSTORE()) {
			    stackTag[il.dval - nlocals] = (byte)il.dtype();
			}
			continue;
		    }

		    if (il.tagL() == TAG_STACK)
			il.lval += cur_stack_size + nlocals;

		    if (il.tagR() == TAG_STACK)
			il.rval += cur_stack_size + nlocals;

		    if (il.tagD() == TAG_STACK) {
			if (!il.isSTORE()) {
			    stackTag[il.dval + cur_stack_size] = (byte)il.dtype();
			}
			il.dval += cur_stack_size + nlocals;
		    }
		    if ((debug & DEBUG_RTL) != 0)
			System.err.println("***\t\t\t"+il);
		}
		if ((bc.flag & BCinfo.FLAG_CTRLFLOW_END) != 0)
		    return new_stack_size;
	    }
	    break;
	}
	return -1;
    }

    final void convertRTL() throws CompilerError {
	BCinfo bc;
	byte stackTag[];

	stackTag = new byte [maxstack + 1];

	// setup for entry point
	bc = bcinfo[0];
	bc.stack_size = 0;

	// setup for exception table
	for (int i = 0; i < exceptionHandler.length; i++) {
	    ExceptionHandler x = exceptionHandler[i];
	    bc = bcinfo[x.handlerPC];
	    bc.flag |= BCinfo.FLAG_BRANCH_TARGET | BCinfo.FLAG_EXCEPTION_HANDLER;
	    bc.stack_size = 1;
	    ILnode il = (new ILnode()).move(TAG_SI,(-1), TAG_PI,(ARG0));
	    il.next = bc.next;
	    bc.next = il;
	    for (int pc = x.startPC; pc <= x.endPC; pc++) {
		bc = bcinfo[pc];
		if (bc != null) {
		    bc.flag |= BCinfo.FLAG_IN_TRY_BLOCK;
		}
	    }
	}

	convBB(0, stackTag, false);

	for (int i = 0; i < exceptionHandler.length; i++) {
	    stackTag[0] = (byte)TAG_SI;
	    convBB(exceptionHandler[i].handlerPC, stackTag, false);
	}
    }
}
