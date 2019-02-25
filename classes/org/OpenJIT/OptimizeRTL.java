//
// Copyright (C) 1996 1997 1998 1999 by FUJITSU LABORATRIES LTD.
//
// $Revision: 1.26 $
// $Date: 1999/10/06 08:06:01 $
// $Author: kouya $
//

package org.OpenJIT;

final class Var {
    int defTime;
    int useTime;
    ILnode defIL;
    ILnode useIL;
    int flag;

    final static int USE_D = 0xff << ILnode.OFFSET_D;
    final static int USE_L = 0xff << ILnode.OFFSET_L;
    final static int USE_R = 0xff << ILnode.OFFSET_R;

    final void reset() {
	defTime = 0;
	useTime = 0;
	defIL = null;
	useIL = null;
	flag = 0;
    }

    final void use(int curTime, ILnode il, int flag) {
	this.useTime = curTime;
	if (this.useIL != il) {
	    this.useIL = il;
	    this.flag = flag;
	} else {
	    this.flag |= flag;
	}
    }

    final void use(ILnode il, int flag) {
	this.useIL = il;
	this.flag = flag;
    }
}

public abstract class OptimizeRTL extends ConvertRTL {
    ILnode thisHandle;

    // The order of setting argument is strict.
    // For example, x86 uses "push" to set args.
    static boolean strict_args_order = false;

    // Return register is used for other operations.
    static boolean clobber_return_reg = false;

    static final boolean debug_optimize = false;

    private static void eliminateIL(ILnode il) {
	if ((il != null) && il.op() == ILnode.MOVE && il.tagD() == TAG_STACK && il.isNotReferD()) {
	    if (debug_optimize)
		System.err.println("eliminate:" + il);
	    il.remove();
	}
    }

    private static int srcDefTime(ILnode il, Var var[], int callTime) {
	if (il.op() != ILnode.MOVE)	// Don't replace
	    return 0x7fffffff;

	int tag = il.tagR();
	if (tag == TAG_CONST)	// OK. can replace
	    return 0;

	if (tag == TAG_PARAM) {
	    if (clobber_return_reg)
		return 0x7fffffff;
	    else
		return callTime;
	}

	int defTime1 = var[il.rval].defTime;

	if (il.typeD() != TYPE_DOUBLE) {
	    return defTime1;
	} else {
	    /* check the rest half of double data. */
	    if (var[il.rval+1].defIL == null)
		return defTime1;
	    /* return max defTime */
	    int defTime2 = var[il.rval+1].defTime;
	    return (defTime1 > defTime2) ? defTime1 : defTime2;
	}
    }

    //
    // moveIL: Stack -> Local
    //
    private static boolean moveUpSV(ILnode moveIL, Var var[], int callTime) {
	Var src_var = var[moveIL.rval];
	Var dst_var = var[moveIL.dval];
	ILnode srcIL = src_var.defIL;

	if (srcIL == null)
	    return false;
	if (!srcIL.isNotReferD())
	    return false;
	if (src_var.defTime < callTime)
	    return false;
	if (src_var.defTime < dst_var.useTime)
	    return false;
	if (srcIL.op() == ILnode.MOVE)
	    return false;

	srcIL.setD(moveIL.dtype(), moveIL.dval);
	dst_var.defTime = src_var.defTime;
	dst_var.defIL = srcIL;
	moveIL.remove();
	return true;
    }

    //
    // moveIL: Stack -> Param
    //
    private static boolean moveUpSP(ILnode moveIL, Var var[], int callTime, int paramUseTime) {
	Var src_var = var[moveIL.rval];
	ILnode srcIL = src_var.defIL;
	int regno = moveIL.dval;

	if (srcIL == null)
	    return false;
	if (moveIL.typeD() != TYPE_INT)
	    return false;
	if (!srcIL.isNotReferD())
	    return false;
	if (src_var.defTime < callTime)
	    return false;

	// ........ -> %si1 <--+
	// %ri0 ... -> ...     | inhibit this case
	//     %si1 -> %ri0 ---+ (only return register %ri0, %ri1)

	if (src_var.defTime < paramUseTime) {
	    return false;
	}

	if (!strict_args_order && srcIL.op() == ILnode.MOVE)
	    return false;

	srcIL.setD(moveIL.dtype(), regno);
	moveIL.remove();
	return true;
    }

    private final static int log2(int val) {
	int log2;
	for (log2 = 0; log2 < 32; log2++) {
	    int v = val >> log2;
	    if ((v & 1) != 0) {
		if (v == 1)
		    break;
		return -1;
	    }
	}
	return log2;
    }

    /* Optimize int mul and div to shift left/right if possible */
    private final void optimizeMulDiv(ILnode il, Var var[]) {
	int lval, rval, ltype, rtype, shift_val;

	if (il.op() == ILnode.MUL) {
	    if ((il.typeL() == TYPE_INT) && (il.tagR() == TAG_CONST)) {
		lval = il.lval; ltype = il.ltype();
		rval = il.rval; rtype = il.rtype();
	    } else if ((il.op() == ILnode.MUL) && (il.typeR() == TYPE_INT) && (il.tagL() == TAG_CONST)) {
		/* Swap left and right */
		lval = il.rval; ltype = il.rtype();
		rval = il.lval; rtype = il.ltype();

		Var v = var[il.rval];
		if (v != null && v.useIL == il) {
		    if ((v.flag & Var.USE_R) != 0)
			v.flag &= ~Var.USE_R;
		    v.flag |= Var.USE_L;
		}
	    } else {
		return;
	    }

	    if (rval == 1) {
		/* replace to move */
		il.move(il.dtype(), il.dval, ltype, lval);
		return;
	    }
	    for (shift_val = 0; shift_val < 32; shift_val++) {
		int v = rval >> shift_val;
		if ((v & 1) != 0) {
		    if (v == 1) {
			break;
		    } else {
			return;
		    }
		}
	    }

	    /* replace to shift */
	    il.insn(ILnode.SLL, il.dtype(), il.dval, ltype, lval, rtype, shift_val);
	}
    }

    /* stupid java compiler creates such a bytecode. :-< */
    final void optimizeCMP(ILnode cmpIL) {
	ILnode il = cmpIL;
	if (il.lval > il.rval) {
	    for(il = il.next; il != null; il = il.next) {
		if (il.op() == ILnode.CMP)
		    break;
		if (il.op() == ILnode.BRANCH) {
		    switch(il.lval) {
		    case CC_A:	break;
		    case CC_E:	il.remove(); break;
		    case CC_NE:	il.lval = CC_A; il.next = null; break;
		    case CC_L:	il.remove(); break;
		    case CC_LE:	il.remove(); break;
		    case CC_TBLOB:
			il.lval = CC_A;
			il.next = null;
			cmpIL.remove();
			return;
		    case CC_G:	il.lval = CC_A; il.next = null; break;
		    case CC_GE:	il.lval = CC_A; il.next = null; break;
		    default:
			throw new CompilerError("const folding >:" + cmpIL + "\t" + il);
		    }
		}
	    }
	} else if (il.lval < il.rval) {
	    for(il = il.next; il != null; il = il.next) {
		if (il.op() == ILnode.CMP)
		    break;
		if (il.op() == ILnode.BRANCH) {
		    switch(il.lval) {
		    case CC_A:	break;
		    case CC_E:	il.remove(); break;
		    case CC_NE:	il.lval = CC_A; il.next = null; break;
		    case CC_L:	il.lval = CC_A; il.next = null; break;
		    case CC_LE:	il.lval = CC_A; il.next = null; break;
		    case CC_TBLOB:
			if (cmpIL.lval >= 0) {
			    il.remove();
			} else {
			    il.lval = CC_A;
			    il.next = null;
			}
			cmpIL.remove();
			return;
		    case CC_G:	il.remove(); break;
		    case CC_GE:	il.remove(); break;
		    default:
			throw new CompilerError("const folding <:" + cmpIL + "\t" + il);
		    }
		}
	    }
	} else {
	    for(il = il.next; il != null; il = il.next) {
		if (il.op() == ILnode.CMP)
		    break;
		if (il.op() == ILnode.BRANCH) {
		    switch(il.lval) {
		    case CC_A:	break;
		    case CC_E:	il.lval = CC_A; il.next = null; break;
		    case CC_NE:	il.remove(); break;
		    case CC_L:	il.remove(); break;
		    case CC_LE:	il.lval = CC_A; il.next = null; break;
		    case CC_TBLOB:
			cmpIL.remove();
			il.remove();
			return;
		    case CC_G:	il.remove(); break;
		    case CC_GE:	il.lval = CC_A; il.next = null; break;
		    default:
			throw new CompilerError("const folding =:" + cmpIL + "\t" + il);
		    }
		}
	    }
	}
	cmpIL.remove();
    }

    final void optimizeRTL() throws CompilerError {
	int i;
	int var_sz = nlocals + maxstack + 1 + 1; // work & thisHandle
	Var var[] = new Var[var_sz];
	int curTime = 0;
	int callTime = 0;
	int paramUseTime = 0;
	boolean allowMoveUp = true;
	ILnode il, srcIL;
	ILnode useIL;
	int last_stack = var_sz;
	Var v;

	for (i = 0; i < var_sz; i++) {
	    var[i] = new Var();
	}

	for (int pc = 0; pc < bcinfo.length; pc++) {
	    BCinfo bc = bcinfo[pc];

	    if (bc == null)
		continue;

	    if (debug_optimize)
		System.err.println("***" + pc + "\t" + opcName(pc));

	    if ((bc.flag & BCinfo.FLAG_CTRLFLOW_DONE) == 0) {
		if (debug_optimize)
		    System.err.println("unreached here!!!");
		bcinfo[pc] = null;
		continue;
	    }

	    if ((bc.flag & (BCinfo.FLAG_BRANCH_TARGET | BCinfo.FLAG_BRANCH_NOT_TAKEN)) != 0) {

		if (debug_optimize)
		    System.err.println("eliminate bb end:" + last_stack);
		for (i = last_stack; i < var_sz; i++) {
		    eliminateIL(var[i].defIL);
		    useIL = var[i].useIL;
		    if (useIL != null) {
			if (debug_optimize)
			    System.err.println("freeIL0:" + useIL + "\t" + Integer.toHexString(var[i].flag));
			useIL.free(var[i].flag);
		    }
		}

		/* start of basic block */
		for (i = 0; i < var_sz; i++) {
		    var[i].reset();
		}
		curTime = 0;
		callTime = 0;
		paramUseTime = 0;
	    }

	next_IL:
	    for (il = bc.next; il != null; il = il.next) {

		if (debug_optimize)
		    System.err.println("\t" + il);

		++curTime;

		int il_op = il.op();

		switch(il_op) {
		case ILnode.CALL:
		    allowMoveUp = true;
		    callTime = curTime;
		    continue next_IL;
		    // no reference to variables */
		case ILnode.ELIMINATED:
		case ILnode.NOP:
		case ILnode.BRANCH:
		case ILnode.RET:
		    continue next_IL;
		case ILnode.RETURN:
		    break;
		case ILnode.MOVE:
		    // meaningless move should be checked but never happens.
		    if (false && il.rval == il.dval && il.rtype() == il.dtype()) {
			il.remove();
			continue next_IL;
		    }
		    // can't be moved up beyond try block
		    if ((bc.flag & BCinfo.FLAG_IN_TRY_BLOCK) != 0)
			break;
		    if (il.tagR() != TAG_STACK)
			break;
		    // moveStack ?
		    if ((il.rtype() & TAG_REF) != 0) 
			break;
		    // no more reference to this stack variable
		    int tagD = il.tagD();
		    if (tagD == TAG_LOCAL) {
			if (moveUpSV(il, var, callTime))
			    continue next_IL;
		    } else if (tagD == TAG_PARAM) {
  			if (allowMoveUp && moveUpSP(il, var, callTime, paramUseTime)) {
			    if (strict_args_order)
				allowMoveUp = false;
  			    continue next_IL;
			}
  		    }
		    break;
		}

		int tagL = il.tagL();
		if (tagL == TAG_STACK) {
		    v = var[il.lval];
		    if ((srcIL = v.defIL) != null) {
			if (srcDefTime(srcIL, var, callTime) > v.defTime) {
			    // can't replace L operand
			    srcIL.referD();
			} else {
			    // Ok. replace L operand
			    tagL = srcIL.tagR();
			    il.setL(srcIL.rtype(), srcIL.rval);
			    if (tagL == TAG_PARAM)
				paramUseTime = curTime;
			}
		    }
		} else if (tagL == TAG_PARAM) {
		    paramUseTime = curTime;
		}

		int tagR = il.tagR();
		if (tagR == TAG_STACK) {
		    v = var[il.rval];
		    if ((srcIL = v.defIL) != null) {
			if (srcDefTime(srcIL, var, callTime) > v.defTime) {
			    // can't replace R operand
			    srcIL.referD();
			} else {
			    // Ok. replace R operand
			    tagR = srcIL.tagR();
			    il.setR(srcIL.rtype(), srcIL.rval);
			    if (tagR == TAG_PARAM)
				paramUseTime = curTime;
			}
		    }
		} else if (tagR == TAG_PARAM) {
		    paramUseTime = curTime;
		}

		// constant folding. calculates "const .op. const"
		// and convert to move instruction
		if (tagR == TAG_CONST) {
		    if (tagL == TAG_CONST) {
			switch(il_op) {
			case ILnode.ELIMINATED:
			case ILnode.ARRAYCHK:
			    continue next_IL;
			case ILnode.DIV0CHK:
			    if ((il.lval | il.rval) != 0) {
				il.remove();
			    } else {
				// trap always
				il.dval = 1;
			    }
			    continue next_IL;
			case ILnode.SLL:
			    il.setConst(il.lval << il.rval);
			    break;
			case ILnode.ADD:
			    il.setConst(il.lval + il.rval);
			    break;
			case ILnode.SUB:
			    il.setConst(il.lval - il.rval);
			    break;
			case ILnode.MUL:
			    il.setConst(il.lval * il.rval);
			    break;
			case ILnode.DIV:
			    il.setConst(il.lval / il.rval);
			    break;
			case ILnode.AND:
			    il.setConst(il.lval & il.rval);
			    break;
			case ILnode.OR:
			    il.setConst(il.lval | il.rval);
			    break;
			case ILnode.CMP:
			    optimizeCMP(il);
			    break;
			case ILnode.TBLSW:
			    /* stupid java compiler creates such a bytecode. :-< */
			    break;
			case ILnode.RETURN:
			    break;
			default:
			    if (il.lval != 0) {
				System.err.println("pc:" + pc);
				throw new CompilerError("constant folding:" + il);
			    }
			}
		    } else if (il.rval == 0) {
			switch(il_op) {
			case ILnode.ADD:
			case ILnode.SUB:
			case ILnode.OR:
			case ILnode.XOR:
			    il.move(il.dtype(), il.dval, il.ltype(), il.lval);
			    break;
			case ILnode.AND:
			case ILnode.MUL:
			    il.setConst(0);
			    break;
			}
		    } else if (il.rval == -1) {
			switch(il_op) {
			case ILnode.AND:
			    il.move(il.dtype(), il.dval, il.ltype(), il.lval);
			    break;
			case ILnode.MUL:
			case ILnode.DIV:
			    il.insn(ILnode.SUB, 
				    il.dtype(), il.dval, 
				    TAG_CONST, 0,
				    il.ltype(), il.lval);
			    break;
			}
		    } else if (il.rval == 1) {
			switch(il_op) {
			case ILnode.MUL:
			case ILnode.DIV:
			    il.move(il.dtype(), il.dval, il.ltype(), il.lval);
			    break;
			}
		    } else {
			/* Optimize int mul and div to shift left/right if possible */
			int shift;
			if (il_op == ILnode.MUL) {
			    shift = log2(il.rval);
			    if (shift > 0) {
				/* replace to shift left */
				il.insn(ILnode.SLL,
					il.dtype(), il.dval,
					il.ltype(), il.lval,
					TAG_CONST, shift);
			    }
			} else if (il_op == ILnode.DIV) {
			    shift = log2(il.rval);
			    if (shift > 0) {
				int tmp_v = nlocals + maxstack;
				ILnode div = il;

				/* and %tmp, div. -> %tmp */
				il = (new ILnode(il)).
				    insn(ILnode.AND,
					 TAG_SI, tmp_v,
					 TAG_SI, tmp_v,
					 TAG_CONST, div.rval-1);
				/* add %tmp, %lval -> %tmp */
				il = (new ILnode(il)).
				    insn(ILnode.ADD,
					 TAG_SI, tmp_v,
					 TAG_SI, tmp_v,
					 div.ltype(), div.lval);
				if (tagL == TAG_STACK || tagL == TAG_LOCAL)
				    var[div.lval].use(il, Var.USE_R);
				/* sra %tmp, shift -> %dval */
				il = (new ILnode(il)).
				    insn(ILnode.SRA,
					 div.dtype(), div.dval,
					 TAG_SI, tmp_v,
					 TAG_CONST, shift);
				/* replace div to "sra %lval,31 -> %tmp" */
				div.insn(ILnode.SRA,
					 TAG_SI, tmp_v,
					 div.ltype(), div.lval,
					 TAG_CONST, 31);
			    }
			}
		    }
		}

		/* Optimize imul to shift left if possible */
		if (il.op() == ILnode.MUL && tagL == TAG_CONST) {
		    if (il.lval == 1) {
			il.move(il.dtype(), il.dval, il.rtype(), il.rval);
		    } else {
			int shift = log2(il.lval);
			if (shift > 0) {
			    /* replace to shift left */
			    il.insn(ILnode.SLL,
				    il.dtype(), il.dval,
				    il.rtype(), il.rval,
				    TAG_CONST, shift);
			}
		    }
		}

		tagL = il.tagL();
		if (tagL == TAG_STACK || tagL == TAG_LOCAL)
		    var[il.lval].use(curTime, il, Var.USE_L);
		    
		tagR = il.tagR();
		if (tagR == TAG_STACK || tagR == TAG_LOCAL)
		    var[il.rval].use(curTime, il, Var.USE_R);

		int tagD = il.tagD();

		if (optimize_handle) {
		    // optimize handle access
		    if ((access & ACC_STATIC) == 0) {
			if (il_op == ILnode.LD && tagL == TAG_LOCAL && il.lval == 0) {
			    if (tagR != TAG_CONST || (il.rval != 0)) {
				throw new CompilerError("handle ???:" + il);
			    }
			    if (thisHandle == null) {
				// load [%v0 + 0], %vh
				thisHandle = (new ILnode()).insn(ILnode.LD, TAG_VI, var_sz - 1, TAG_VI,0, TAG_CONST,0);
			    }
			    if (tagD == TAG_CONST) {
				/* invokeignored_quick or nonnull_quick */
				il.remove();
			    } else {
				il.move(il.dtype(), il.dval, TAG_VI, var_sz - 1);
			    }
			}
		    }
		}

		if (il.isSTORE()) {
		    if (tagD == TAG_STACK) {
			if (il.dval < 0) {
			    System.err.println("PC:" + pc + " IL:" + il);
			}
			v = var[il.dval];
			if ((srcIL = v.defIL) != null) {
			    if (srcDefTime(srcIL, var, callTime) > v.defTime) {
				// can't replace D operand
				srcIL.referD();
				v.use(curTime, il, Var.USE_D);
			    } else {
				// Ok. replace D operand
				tagD = srcIL.tagR();
				il.setD(srcIL.rtype(), srcIL.rval);
				if (srcIL.tagR() == TAG_STACK || srcIL.tagR() == TAG_LOCAL)
				    var[srcIL.rval].use(curTime, il, Var.USE_D);
				if (tagD == TAG_PARAM)
				    paramUseTime = curTime;
			    }
			} else {
			    v.use(curTime, il, Var.USE_D);
			}
		    } else if (tagD == TAG_LOCAL) {
			var[il.dval].use(curTime, il, Var.USE_D);
		    } else if (tagD == TAG_PARAM) {
			paramUseTime = curTime;
		    }
		} else {
		    if (tagD == TAG_STACK) {
			eliminateIL(var[il.dval].defIL);
			var[il.dval].defIL = il;
			var[il.dval].defTime = curTime;
			useIL = var[il.dval].useIL;
			if (useIL != null && useIL != il) {
			    if (debug_optimize)
				System.err.println("freeIL1:" + useIL + "\t" + Integer.toHexString(var[il.dval].flag));
			    useIL.free(var[il.dval].flag);
			}
			if (il.typeD() == TYPE_DOUBLE) {
			    eliminateIL(var[il.dval+1].defIL);
			    var[il.dval+1].defTime = curTime;
			    useIL = var[il.dval+1].useIL;
			    if (useIL != null && useIL != il) {
				if (debug_optimize)
				    System.err.println("freeIL2:" + useIL + "\t" + Integer.toHexString(var[il.dval+1].flag));
				useIL.free(var[il.dval+1].flag);
			    }
			}
		    } else if (tagD == TAG_LOCAL) {
			var[il.dval].defIL = il;
			var[il.dval].defTime = curTime;
			useIL = var[il.dval].useIL;
			if (useIL != null && useIL != il) {
			    if (debug_optimize)
				System.err.println("freeIL3:" + useIL + "\t" + Integer.toHexString(var[il.dval].flag));
			    useIL.free(var[il.dval].flag);
			}
			if (il.typeD() == TYPE_DOUBLE) {
			    useIL = var[il.dval+1].useIL;
			    if (useIL != null && useIL != il) {
				if (debug_optimize)
				    System.err.println("freeIL4:" + useIL + "\t" + Integer.toHexString(var[il.dval+1].flag));
				useIL.free(var[il.dval+1].flag);
			    }
			}
		    } else if (tagD == TAG_PARAM) {
			if (strict_args_order)
			    allowMoveUp = false;
		    }
		}
		if (debug_optimize)
		    System.err.println("->\t" + il);

		last_stack = nlocals + bc.stack_size + bc.stack_delta;
	    }
	}
	if (debug_optimize)
	    System.err.println("eliminate bb end:" + last_stack);
	for (i = last_stack; i < var_sz; i++) {
	    eliminateIL(var[i].defIL);
	    useIL = var[i].useIL;
	    if (useIL != null) {
		if (debug_optimize)
		    System.err.println("freeIL5:" + useIL + "\t" + Integer.toHexString(var[i].flag));
		useIL.free(var[i].flag);
	    }
	}
    }
}
