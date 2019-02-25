//
// Copyright (C) 1996 1997 1998 1999 by FUJITSU LABORATRIES LTD.
//
// $Revision: 1.38 $
// $Date: 1999/09/27 04:52:02 $
// $Author: kouya $
//

package org.OpenJIT;

class PCList {
  int start_pc;
  int end_pc;
  int offset;
  PCList next;
}

class PCReloc {
  private PCList list, last;

  PCReloc(int size) {
    PCList l = new PCList();
    l.start_pc = 0; l.end_pc = size; l.offset = 0; l.next = null;
    list = last = l;
  }

  int pc_to_virtpc(int pc) throws IllegalArgumentException {
    PCList l;

    for (l = list; l != null; l = l.next) {
      if ((pc >= l.start_pc) &&
	  (pc <= l.end_pc)) return pc + l.offset;
    }

    throw new IllegalArgumentException("Can't translate PC "+pc+" in this PCReloc!");
  }

  void inline_method(int pc, int size) {
    int end_pc = last.end_pc;
    if (pc == end_pc) return; // No need to add new item
    PCList l = new PCList();
    l.start_pc = pc+1;
    l.end_pc = end_pc;
    l.offset = last.offset + size;
    l.next = null;
    last.end_pc = pc;
    last.next = l;
    last = l;
  }
}

class InlineInfo {
  BCinfo[] bcinfo;  // Bytecodes of inlined method
  int origpc;       // PC of original call
  int pc;           // PC of current method at which to inline
  int nextpc;       // PC which should follow inlined method call
  int size;         // Size of inlined method bytecodes
  int origsize;     // Original size of call 
  int extrasize;    // Number of 'extra' bytecodes added (e.g., prologue)
  int pcoffset;     // Offset to add to PCs in inlined method
  int nlocals;      // Number of local variables of inlined method
  int args_size;    // Size of arguments of inlined method
  ExceptionHandler[] exceptionHandler;  // Exception handler list
}

public abstract class ParseBytecode extends Compile {

  private java.util.Vector inlinedInfo;  // List of inlined methods
  private int inlined_pc_offset;         // Offset added to PC after inlining

    private final int ldc(LinkedList ll, int index) {
	switch(ConstantPoolType(index)) {
	case CONSTANT_INTEGER:
	    ll = (new ILnode(ll)).move(TAG_SI, 0, TAG_CONST, ConstantPoolValue(index));
	    return 1;
	case CONSTANT_FLOAT:
	    ll = (new ILnode(ll)).insn(ILnode.FLD, TAG_SF, 0, TAG_CONST, 0, TAG_CONST, ConstantPoolAddress(index));
	    return 1;
	case CONSTANT_STRING:
	    if (ConstantPoolTypeResolved(index)) {
		ll = (new ILnode(ll)).move(TAG_SI, 0, TAG_CONST, ConstantPoolValue(index));
	    } else {
		flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		ll = (new ILnode(ll)).call(TAG_SI, 0, RT_resolveString, index);
		ll = (new ILnode(ll)).move(TAG_SI, 0, TAG_PI, RETI);
	    }
	    return 1;
	case CONSTANT_DOUBLE:
	    ll = (new ILnode(ll)).insn(ILnode.DLD, TAG_SD, 0, TAG_CONST, 0, TAG_CONST, ConstantPoolAddress(index));
	    return 2;
	case CONSTANT_LONG:
	    ll = (new ILnode(ll)).move(TAG_SI, 0, TAG_CONST, ConstantPoolValue(index));
	    ll = (new ILnode(ll)).move(TAG_SI, 1, TAG_CONST, ConstantPoolValue(index+1));
	    return 2;
	default:
	    throw new CompilerError("invalid ldc constants:" + ConstantPoolType(index));
	}
    }

    private final ILnode array_intro(LinkedList ll, int aref, int index, int size) {
	flags |= FLAG_HAS_EXCEPTION;
	ll = (new ILnode(ll)).insn(ILnode.ARRAYCHK, TAG_CONST, 0, TAG_SI, aref, TAG_SI, index);
	if (size > 0) {
	    // load handle
	    ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI, aref, TAG_SI, aref, TAG_CONST, 0);
	    return (new ILnode(ll)).insn(ILnode.SLL, TAG_SI, index, TAG_SI, index, TAG_CONST, size);
	} else {
	    // load handle
	    return (new ILnode(ll)).insn(ILnode.LD, TAG_SI, aref, TAG_SI, aref, TAG_CONST, 0);
	}
    }

    private final int branch(int pc, int offset) {
	int target_pc = pc + offset;
	if (offset > 0) {
	    BCinfo bc = new BCinfo();
	    bcinfo[target_pc] = bc;
	}
	if (bcinfo[target_pc] == null) {
	    throw new CompilerError("jumped into optimized sequence?");
	}
	bcinfo[target_pc].flag |= BCinfo.FLAG_BRANCH_TARGET;
	return target_pc;
    }

    // Rewrite bytecode after inlining
    private final void rewriteForInline(InlineInfo iinfo) {
      int pc;
      BCinfo p_bcinfo[] = iinfo.bcinfo;
      BCinfo bc; ILnode il;
      
      for (pc = 0; pc < p_bcinfo.length; pc++) {
	if (p_bcinfo[pc] != null) {
	  bc = p_bcinfo[pc];
	  
	  for (il = bc.next; il != null; il = il.next) {

	    //if ((debug & DEBUG_INLINE) != 0) System.out.println("REWRITE: "+il.toString());
	    
	    int il_op = il.op();
	    switch (il_op) {
	      
	    case ILnode.RETURN:
	      if (pc == p_bcinfo.length-1) {
		// Last bytecode in method
		il = il.nop();
		bc.flag &= ~(BCinfo.FLAG_CTRLFLOW_END);
		bc.stack_delta = 0;
	      } else {
		// Need to jump to end of inlined method
		il = il.branch(CC_A, branch(iinfo.origpc, iinfo.nextpc - iinfo.pc));

		// Jump to epilogue
		il.rval = (iinfo.pc + iinfo.size + 1); 
		bc.flag |= BCinfo.FLAG_CTRLFLOW_END;
		bc.stack_delta = 0;
	      }
	      break;

	    case ILnode.BRANCH:
	      il.rval += iinfo.pcoffset;
	      break;
	      
	    case ILnode.MOVE:
	      if (il.tagD() == ILnode.TAG_VI) {
		il.dval += nlocals;
	      }
	      if (il.tagR() == ILnode.TAG_VI) {
		il.rval += nlocals;
	      }
	      break;
	      
	    }
	    //if ((debug & DEBUG_INLINE) != 0) System.out.println("\t\tNOW: "+il.toString());
	  }
	}
      }
    }

    // Fold inlined method bcinfo into our bcinfo array
    private final void foldBCinfo(InlineInfo iinfo) {
      BCinfo p_bcinfo[] = iinfo.bcinfo;
      
      BCinfo new_bcinfo[] = new BCinfo[bcinfo.length + p_bcinfo.length + iinfo.extrasize - iinfo.origsize];
      int i, n = 0;
      for (i = 0; i <= iinfo.pc; i++) {
	if (bcinfo[i] != null) new_bcinfo[n] = bcinfo[i];
	n++;
      }
      for (i = 0; i < p_bcinfo.length; i++) {
	if (p_bcinfo[i] != null) new_bcinfo[n] = p_bcinfo[i];
	n++;
      }
      for (i = iinfo.nextpc; i < bcinfo.length; i++) {
	if (bcinfo[i] != null) new_bcinfo[n] = bcinfo[i];
	n++;
      }
      bcinfo = new_bcinfo;
    }

    // Fix branch instructions in this method
    // Call before foldBCInfo
    private final void fixBranch(InlineInfo iinfo) {
      int i;
      BCinfo bc; ILnode il;
      int offset = iinfo.size + iinfo.extrasize - iinfo.origsize;
      
      for (i = 0; i < bcinfo.length; i++) {
	bc = bcinfo[i]; if (bc == null) continue;
	for (il = bc.next; il != null; il = il.next) {
	  if (il.op() == ILnode.BRANCH) {
	    if (il.rval > iinfo.pc) {
	      il.rval += offset;
	    }
	  }
	}
      }
    }

    // Add inlined exception handlers to this method; fix up
    // offsets in this method's exceptions
    private final void fixExceptions(InlineInfo iinfo) {
      int i;
      for (i = 0; i < exceptionHandler.length; i++) {
	int offset = iinfo.size - iinfo.origsize + iinfo.extrasize;
	ExceptionHandler x = exceptionHandler[i];
	if (x.startPC > iinfo.pc) {
	  x.startPC += offset;
	}
	
	if (x.endPC > iinfo.pc) {
	  x.endPC += offset;
	} else if (x.endPC == iinfo.pc) {
	  // Needs to be inclusive...
	  x.endPC += iinfo.size + iinfo.extrasize;
	}

	if (x.handlerPC > iinfo.pc) {
	  x.handlerPC += offset;
	}
      }

      if (iinfo.exceptionHandler.length == 0) return;

      ExceptionHandler newArr[] = new ExceptionHandler[exceptionHandler.length + iinfo.exceptionHandler.length];
      for (i = 0; i < exceptionHandler.length; i++) {
	newArr[i] = exceptionHandler[i];
      }
      for (i = 0; i < iinfo.exceptionHandler.length; i++) {
	iinfo.exceptionHandler[i].startPC += iinfo.pcoffset;
	iinfo.exceptionHandler[i].endPC += iinfo.pcoffset;
	iinfo.exceptionHandler[i].handlerPC += iinfo.pcoffset;
	newArr[i + exceptionHandler.length] = iinfo.exceptionHandler[i];
      }
      exceptionHandler = newArr;
    }

    // Fixup of bytecode after all parsing is complete 
    private final void doInlineFixup() {
      if (inlinedInfo == null) return; // Nothing inlined
      int i;
      int size_increased = 0;
      for (i = 0; i < inlinedInfo.size(); i++) {
	InlineInfo iinfo = (InlineInfo)inlinedInfo.elementAt(i);
	fixBranch(iinfo);
	foldBCinfo(iinfo);
	fixExceptions(iinfo);
	size_increased += (iinfo.size - iinfo.origsize + iinfo.extrasize);
      }
      bytecode_length += size_increased;
    }

    // Try to inline the given method; returns true if successful
    private final boolean doInline(BCinfo bc, int pc, int index, boolean is_static, boolean is_interface) {
      // Uncomment the following line to disable inlining
      // optimize_inline = false; 
      if (optimize_inline == false) return false;

      if (is_interface) return false; // XXX Don't inline this (yet)

      String methodname = ConstantPoolName(index);
      byte sig[] = ConstantPoolMethodDescriptor(index);
      // XXX mdw: For some reason this breaks when is_interface is true?
      int access = ConstantPoolAccess(index);

      if ((access & (ACC_STATIC | ACC_FINAL | ACC_PRIVATE)) == 0) {
	// Don't inline
	return false;
      }
      if ((access & ACC_NATIVE) != 0) {
	// Don't inline
	return false;
      }
      if ((access & ACC_SYNCHRONIZED) != 0) { 
        // Don't inline
	return false;
      }

      try {
	Compile parser = createParser(index);
	// if ((debug & DEBUG_INLINE) != 0) System.out.println("Want to inline "+methodname+", length "+parser.bytecode_length+", max is "+inline_maxinsns);
	if (parser.bytecode_length > inline_maxinsns) return false;
	parser.inline_maxinsns = 0; // inline_maxinsns / 2; // XXX mdw
	
	if ((debug & DEBUG_INLINE) != 0) System.out.println("\t\tINLINING: "+methodname);
	parser.parseOnly();
	BCinfo[] p_bcinfo = parser.bcinfo;
	
	if (inlinedInfo == null) {
	  inlinedInfo = new java.util.Vector();
	  inlined_pc_offset = 0;
	}

	// invokestatic/invokevirtual -> 3rd byte is epilogue
	// For invokeinterface should be pc+4
	int my_nextpc = pc+2; // Next PC to jump to
	
	InlineInfo iinfo = new InlineInfo();
	iinfo.bcinfo = p_bcinfo;
	iinfo.size = p_bcinfo.length;
	iinfo.origpc = pc;
	iinfo.pc = pc + inlined_pc_offset;
	iinfo.nextpc = my_nextpc + inlined_pc_offset;
	iinfo.origsize = 3; // XXX Should be 5 for invokeinterface
	iinfo.extrasize = 2;    // One each for prologue and epilogue
	iinfo.pcoffset = iinfo.pc + 1;  // Starting PC plus prologue
	iinfo.nlocals = parser.nlocals;
	iinfo.args_size = parser.args_size;
	iinfo.exceptionHandler = parser.exceptionHandler;
	
	inlinedInfo.addElement(iinfo);
	inlined_pc_offset += iinfo.size + iinfo.extrasize - iinfo.origsize;

	rewriteForInline(iinfo);

	// Prepend inline-prologue code
	LinkedList ll = bc;
	ll = new ILnode(ll).inline_prologue(parser.methodblock);
	
	// Prepend code to pop arguments into 'new' local variables
	if (iinfo.args_size > 0) {

	  //System.out.println("MY NLOCALS IS "+nlocals+", MY ARGS SIZE IS "+args_size+", INLINED NLOCALS IS "+iinfo.nlocals+", INLINED ARGS SIZE IS "+iinfo.args_size);
	  
	  int a = 0;

	  //System.out.println("INLINED SIG LENGTH IS "+sig.length);
	  //System.out.println("INLINED SIG IS "+new String(sig));

	  if (!is_static) {
	    // Pop 'this'
	    ll = new ILnode(ll).move(TAG_VI, (nlocals), TAG_SI, -1*(iinfo.args_size));
	    a++;
	  }
	  
	loop:
	  for (int i = 1; i < sig.length; i++) {
	    
	    switch(sig[i]) {
	    case SIGC_ENDMETHOD:
	      // return_type = sig[i+1];
	      break loop;
	    case SIGC_CLASS:
	      while (sig[i] != SIGC_ENDCLASS) i++;
	      ll = new ILnode(ll).move(TAG_VI, (nlocals+a), TAG_SI, (a-iinfo.args_size));
	      a++;
	      break;
	    case SIGC_ARRAY:
	      while ((sig[i] == SIGC_ARRAY)) i++;
	      /* If an array of classes, skip over class name, too. */
	      if (sig[i] == SIGC_CLASS) { 
		while (sig[i] != SIGC_ENDCLASS) 
		  i++;
	      }
	      ll = new ILnode(ll).move(TAG_VI, (nlocals+a), TAG_SI, (a-iinfo.args_size));
	      a++;
	      break;
	    case SIGC_LONG:
	      // XXX mdw This might be in the wrong order
	      ll = new ILnode(ll).move(TAG_VI, (nlocals+a), TAG_SI, (a-iinfo.args_size));
	      a++;
	      ll = new ILnode(ll).move(TAG_VI, (nlocals+a), TAG_SI, (a-iinfo.args_size));
	      a++;
	      break;
	    case SIGC_BOOLEAN:
	    case SIGC_BYTE:
	    case SIGC_CHAR:
	    case SIGC_SHORT:
	    case SIGC_INT:
	      ll = new ILnode(ll).move(TAG_VI, (nlocals+a), TAG_SI, (a-iinfo.args_size));
	      a++;
	      break;
	    case SIGC_FLOAT:
	      ll = new ILnode(ll).move(TAG_VF, (nlocals+a), TAG_SF, (a-iinfo.args_size));
	      a++;
	      break;
	    case SIGC_DOUBLE:
	      ll = new ILnode(ll).move(TAG_VD, (nlocals+a), TAG_SD, (a-iinfo.args_size));
	      a += 2;
	      break;
	    default:
	      throw new CompilerError("invalid signature " + (char)sig[i]);
	    }
	  }
	  
	  bc.stack_delta = -1*iinfo.args_size;
	}

	// Append inline-epilogue code
	if (bcinfo[my_nextpc] == null) 
	  bcinfo[my_nextpc] = new BCinfo();
	bc = bcinfo[my_nextpc];
	if (bc.next != null)
	  throw new CompilerError("Should be no bytecodes at end of method call!");
	ll = bc;
	ll = new ILnode(ll).inline_epilogue(this.methodblock);

	nlocals += iinfo.nlocals;
	maxstack += parser.maxstack;
	
      } catch (Exception e) {
	System.out.println("Exception while inlining method "+methodname+": "+e.getMessage());
	e.printStackTrace();
	return false;
      }

      return true;
    }

    private final void callMethod(BCinfo bc, int pc, int invoker, int invoker_resolve, boolean is_static, boolean is_interface) {
	ILnode il;
	int index = pc2ushort(pc+1);
	byte sig[] = ConstantPoolMethodDescriptor(index);
	int args_size = 0;
	byte return_type = 0;

	if (doInline(bc, pc, index, is_static, is_interface)) {
	  // if ((debug & DEBUG_INLINE) != 0) System.out.println("** INLINED OK");
	  return;
	}

	flags |= FLAG_HAS_INVOKE;

	il = null;

	if (!is_static) {
	    ILnode prev_il = il;
	    (il = new ILnode()).move(TAG_PI, ARG0 + args_size, TAG_SI, args_size);
	    il.next = prev_il;
	    args_size++;
	}

    loop:
	for (int i = 1; i < sig.length; i++) {
	    ILnode prev_il = il;

	    switch(sig[i]) {
	    case SIGC_ENDMETHOD:
		return_type = sig[i+1];
		break loop;
	    case SIGC_CLASS:
		while (sig[i] != SIGC_ENDCLASS) i++;
		(il = new ILnode()).move(TAG_PI, ARG0 + args_size, TAG_SI, args_size);
		il.next = prev_il;
		args_size++;
		break;
	    case SIGC_ARRAY:
		while ((sig[i] == SIGC_ARRAY)) i++;
		/* If an array of classes, skip over class name, too. */
		if (sig[i] == SIGC_CLASS) { 
		    while (sig[i] != SIGC_ENDCLASS) 
			i++;
		} 
		(il = new ILnode()).move(TAG_PI, ARG0 + args_size, TAG_SI, args_size);
		il.next = prev_il;
		args_size++;
		break;
	    case SIGC_LONG:
		(il = new ILnode()).move(TAG_PI, ARG0 + args_size, TAG_SI, args_size);
		il.next = prev_il;
		args_size++;
		prev_il = il;
		(il = new ILnode()).move(TAG_PI, ARG0 + args_size, TAG_SI, args_size);
		il.next = prev_il;
		args_size++;
		break;
	    case SIGC_BOOLEAN:
	    case SIGC_BYTE:
	    case SIGC_CHAR:
	    case SIGC_SHORT:
	    case SIGC_INT:
		(il = new ILnode()).move(TAG_PI, ARG0 + args_size, TAG_SI, args_size);
		il.next = prev_il;
		args_size++;
		break;
	    case SIGC_FLOAT:
		(il = new ILnode()).move(TAG_PF, ARG0 + args_size, TAG_SF, args_size);
		il.next = prev_il;
		args_size++;
		break;
	    case SIGC_DOUBLE:
		(il = new ILnode()).move(TAG_PD, ARG0 + args_size, TAG_SD, args_size);
		il.next = prev_il;
		args_size += 2;
		break;
	    default:
		throw new CompilerError("invalid signature " + (char)sig[i]);
	    }
	}

	// fix up setting parameters
  	for (ILnode p = il; p != null; p = p.next) {
	    p.rval -= args_size;
	    if (p.next == null) {
		bc.next = il;
		il = new ILnode(p);
		break;
	    }
	}

	if (il == null) {
	    il = new ILnode(bc);
	}

	if (invoker != 0 && (invoker_resolve == 0 || ConstantPoolTypeResolved(index))) {
	    index = ConstantPoolValue(index);
	} else {
	    invoker = invoker_resolve;
	}

	switch(return_type) {
	case SIGC_LONG:
	    il.call(TAG_SL,(-args_size), invoker, index);
	    (il = new ILnode(il)).move(TAG_SI,(-args_size + 1), TAG_PI,(RETL));
	    (il = new ILnode(il)).move(TAG_SI,(-args_size), TAG_PI,(RETI));
	    bc.stack_delta = 2 - args_size;
	    break;
	case SIGC_DOUBLE:
	    il.call(TAG_SD,(-args_size), invoker, index);
	    (il = new ILnode(il)).move(TAG_SD,(-args_size), TAG_PD,(RETF));
	    bc.stack_delta = 2 - args_size;
	    break;
	case SIGC_FLOAT:
	    il.call(TAG_SF,(-args_size), invoker, index);
	    (il = new ILnode(il)).move(TAG_SF,(-args_size), TAG_PF,(RETF));
	    bc.stack_delta = 1 - args_size;
	    break;
	case SIGC_VOID:
	    il.call(TAG_CONST,(-args_size), invoker, index);
	    bc.stack_delta = -args_size;
	    break;
	case SIGC_CLASS:
	case SIGC_ARRAY:
	    il.call(TAG_SI,(-args_size), invoker, index);
	    (il = new ILnode(il)).move(TAG_SI,(-args_size), TAG_PI,(RETI));
	    bc.stack_delta = 1 - args_size;
	    break;
	default:
	    il.call(TAG_SI,(-args_size), invoker, index);
	    (il = new ILnode(il)).move(TAG_SI,(-args_size), TAG_PI,(RETI));
	    bc.stack_delta = 1 - args_size;
	    break;
	}
    }

    // 
    // optimize the following sequence
    //    0: opc_ifeq, 0, 7, 
    //    3: opc_iconst_0, 
    //    4: opc_goto, 0, 4, 
    //    7: opc_iconst_1
    //
    private final boolean optim_neg(int pc) {
	if (pc + 8 >= bytecode_length) {
	    // code is smaller than sequence. not optimzed.
	    return false;
	}
	if ((pc2signedshort(pc+1) != 7) ||
	    (pc2uchar(pc+3) != opc_iconst_0) ||
	    (pc2uchar(pc+4) != opc_goto) ||
	    (pc2signedshort(pc+5) != 4) ||
	    (pc2uchar(pc+7) != opc_iconst_1)) {
	    // unmatch sequence
	    return false;
	}
	if (bcinfo[pc + 3] != null ||
	    bcinfo[pc + 4] != null ||
	    bcinfo[pc + 7] != null) {
	    // jumped into sequence. can't optimized
	    return false;
	}
	return true;
    }

    // 
    // optimize the following sequence
    //	0: opc_lcmp
    //	1: op_if** xxx
    //  4: ...
    //
    private final boolean optim_lcmp(int pc) {
	if (pc + 4 >= bytecode_length) {
	    // code is smaller than sequence. not optimzed.
	    return false;
	}
	if (bcinfo[pc + 1] != null) {
	    // jumped into "1: op_if**". can't optimized
	    return false;
	}

	LinkedList ll = bcinfo[pc];
	++pc;
	int tpc_off = pc2signedshort(pc + 1);

	if (little_endian_long) {
	switch(pc2uchar(pc)) {
	case opc_ifeq:
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-3), TAG_SI,(-1)));
	    ll = (new ILnode(ll).branch(CC_NE, branch(pc, 3)));
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-4), TAG_SI,(-2)));
	    ll = (new ILnode(ll).branch(CC_E, branch(pc, tpc_off)));
	    break;
	case opc_ifne:
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-3), TAG_SI,(-1)));
	    ll = (new ILnode(ll).branch(CC_NE, branch(pc, tpc_off)));
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-4), TAG_SI,(-2)));
	    ll = (new ILnode(ll).branch(CC_NE, branch(pc, tpc_off)));
	    break;
	case opc_iflt:
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-3), TAG_SI,(-1)));
	    ll = (new ILnode(ll).branch(CC_L, branch(pc, tpc_off)));
	    ll = (new ILnode(ll).branch(CC_NE, branch(pc, 3)));
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-4), TAG_SI,(-2)));
	    ll = (new ILnode(ll).branch(CC_LU, branch(pc, tpc_off)));
	    break;
	case opc_ifge:
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-3), TAG_SI,(-1)));
	    ll = (new ILnode(ll).branch(CC_G, branch(pc, tpc_off)));
	    ll = (new ILnode(ll).branch(CC_NE, branch(pc, 3)));
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-4), TAG_SI,(-2)));
	    ll = (new ILnode(ll).branch(CC_GEU, branch(pc, tpc_off)));
	    break;
	case opc_ifgt:
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-3), TAG_SI,(-1)));
	    ll = (new ILnode(ll).branch(CC_G, branch(pc, tpc_off)));
	    ll = (new ILnode(ll).branch(CC_NE, branch(pc, 3)));
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-4), TAG_SI,(-2)));
	    ll = (new ILnode(ll).branch(CC_GU, branch(pc, tpc_off)));
	    break;
	case opc_ifle:
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-3), TAG_SI,(-1)));
	    ll = (new ILnode(ll).branch(CC_L, branch(pc, tpc_off)));
	    ll = (new ILnode(ll).branch(CC_NE, branch(pc, 3)));
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-4), TAG_SI,(-2)));
	    ll = (new ILnode(ll).branch(CC_LEU, branch(pc, tpc_off)));
	    break;
	default:
	    return false;
	}
	} else {
	switch(pc2uchar(pc)) {
	case opc_ifeq:
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-4), TAG_SI,(-2)));
	    ll = (new ILnode(ll).branch(CC_NE, branch(pc, 3)));
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-3), TAG_SI,(-1)));
	    ll = (new ILnode(ll).branch(CC_E, branch(pc, tpc_off)));
	    break;
	case opc_ifne:
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-4), TAG_SI,(-2)));
	    ll = (new ILnode(ll).branch(CC_NE, branch(pc, tpc_off)));
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-3), TAG_SI,(-1)));
	    ll = (new ILnode(ll).branch(CC_NE, branch(pc, tpc_off)));
	    break;
	case opc_iflt:
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-4), TAG_SI,(-2)));
	    ll = (new ILnode(ll).branch(CC_L, branch(pc, tpc_off)));
	    ll = (new ILnode(ll).branch(CC_NE, branch(pc, 3)));
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-3), TAG_SI,(-1)));
	    ll = (new ILnode(ll).branch(CC_LU, branch(pc, tpc_off)));
	    break;
	case opc_ifge:
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-4), TAG_SI,(-2)));
	    ll = (new ILnode(ll).branch(CC_G, branch(pc, tpc_off)));
	    ll = (new ILnode(ll).branch(CC_NE, branch(pc, 3)));
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-3), TAG_SI,(-1)));
	    ll = (new ILnode(ll).branch(CC_GEU, branch(pc, tpc_off)));
	    break;
	case opc_ifgt:
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-4), TAG_SI,(-2)));
	    ll = (new ILnode(ll).branch(CC_G, branch(pc, tpc_off)));
	    ll = (new ILnode(ll).branch(CC_NE, branch(pc, 3)));
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-3), TAG_SI,(-1)));
	    ll = (new ILnode(ll).branch(CC_GU, branch(pc, tpc_off)));
	    break;
	case opc_ifle:
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-4), TAG_SI,(-2)));
	    ll = (new ILnode(ll).branch(CC_L, branch(pc, tpc_off)));
	    ll = (new ILnode(ll).branch(CC_NE, branch(pc, 3)));
	    ll = (new ILnode(ll).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-3), TAG_SI,(-1)));
	    ll = (new ILnode(ll).branch(CC_LEU, branch(pc, tpc_off)));
	    break;
	default:
	    return false;
	}
	}
	/* assertion. never jump into this sequence */
	// bcinfo[pc + 1] = null;
	return true;
    }

    // 
    // optimize the following sequence
    //	0: opc_fcmp, opc_dcmp
    //	1: op_if** xxx
    //  4: ...
    //
    private final ILnode optim_fcmp(int pc, boolean is_cmpg) {
	if (pc + 4 >= bytecode_length) {
	    // code is smaller than sequence. not optimzed.
	    return null;
	}
	if (bcinfo[pc + 1] != null) {
	    // jumped into "1: op_if**". can't optimized
	    return null;
	}

	int cond;
	int op = pc2uchar(pc);
	switch(pc2uchar(++pc)) {
	case opc_ifeq:
	    cond = CC_FE;
	    break;
	case opc_ifne:	
	    cond = CC_FNE;
	    break;
	case opc_iflt:
	    cond = is_cmpg ? CC_NFL : CC_FL;
	    break;
	case opc_ifge:
	    cond = is_cmpg ? CC_FGE : CC_NFGE;
	    break;
	case opc_ifgt:
	    cond = is_cmpg ? CC_FG : CC_NFG;
	    break;
	case opc_ifle:
	    cond = is_cmpg ? CC_NFLE : CC_FLE;
	    break;
	default:
	    return null;
	}
	/* assertion. never jump into this sequence */
	// bcinfo[pc] = null;
	return (new ILnode().branch(cond, branch(pc, pc2signedshort(pc+1))));
    }

    final void parseBytecode() throws CompilerError {
	int pc;
	LinkedList ll;
	BCinfo bc = null;
	int bc_flag = 0;

	for (pc = 0; pc < bytecode_length; pc++) {
	    int opcode = pc2uchar(pc);
	    int index;
	    int offset;

	    if (bcinfo[pc] == null) {
		bc = new BCinfo();
		bc.flag |= bc_flag;
		bcinfo[pc] = bc;
	    } else {
		bc = bcinfo[pc];
	    }
	    ll = bc;
	    bc_flag = 0;

	    switch(opcode) {
	    case opc_nop:
		bc.stack_delta = 0;
		break;
	    case opc_iconst_m1:
		ll = (new ILnode(ll)).move(TAG_SI,(0), TAG_CONST,(-1));
		bc.stack_delta = 1;
		break;
	    case opc_aconst_null:
	    case opc_iconst_0:
		ll = (new ILnode(ll)).move(TAG_SI,(0), TAG_CONST,(0));
		bc.stack_delta = 1;
		break;
	    case opc_iconst_1:
	    case opc_iconst_2:
	    case opc_iconst_3:
	    case opc_iconst_4:
	    case opc_iconst_5:
		ll = (new ILnode(ll)).move(TAG_SI,(0), TAG_CONST,(opcode - opc_iconst_0));
		bc.stack_delta = 1;
		break;
	    case opc_lconst_0:
		ll = (new ILnode(ll)).move(TAG_SI,(0), TAG_CONST,(0));
		ll = (new ILnode(ll)).move(TAG_SI,(1), TAG_CONST,(0));
		bc.stack_delta = 2;
		break;
	    case opc_lconst_1:
		if (little_endian_long) {
		    ll = (new ILnode(ll)).move(TAG_SI,(0), TAG_CONST,(1));
		    ll = (new ILnode(ll)).move(TAG_SI,(1), TAG_CONST,(0));
		} else {
		    ll = (new ILnode(ll)).move(TAG_SI,(0), TAG_CONST,(0));
		    ll = (new ILnode(ll)).move(TAG_SI,(1), TAG_CONST,(1));
		}
		bc.stack_delta = 2;
		break;
	    case opc_fconst_0:
		ll = (new ILnode(ll)).insn(ILnode.FLD, TAG_SF,(0), TAG_CONST,(0), TAG_CONST,(RT_fconst_0));
		bc.stack_delta = 1;
		break;
	    case opc_fconst_1:
		ll = (new ILnode(ll)).insn(ILnode.FLD, TAG_SF,(0), TAG_CONST,(0), TAG_CONST,(RT_fconst_1));
		bc.stack_delta = 1;
		break;
	    case opc_fconst_2:
		ll = (new ILnode(ll)).insn(ILnode.FLD, TAG_SF,(0), TAG_CONST,(0), TAG_CONST,(RT_fconst_2));
		bc.stack_delta = 1;
		break;
	    case opc_dconst_0:
		ll = (new ILnode(ll)).insn(ILnode.DLD, TAG_SD,(0), TAG_CONST,(0), TAG_CONST,(RT_dconst_0));
		bc.stack_delta = 2;
		break;
	    case opc_dconst_1:
		ll = (new ILnode(ll)).insn(ILnode.DLD, TAG_SD,(0), TAG_CONST,(0), TAG_CONST,(RT_dconst_1));
		bc.stack_delta = 2;
		break;
	    case opc_bipush:
		ll = (new ILnode(ll)).move(TAG_SI,(0), TAG_CONST,(pc2signedchar(pc+1)));
		pc++;
		bc.stack_delta = 1;
		break;
	    case opc_sipush:
		ll = (new ILnode(ll)).move(TAG_SI,(0), TAG_CONST,(pc2signedshort(pc+1)));
		pc += 2;
		bc.stack_delta = 1;
		break;
	    case opc_ldc:
	    case opc_ldc_quick:
		index = pc2uchar(pc+1);
		pc++;
		if (ldc(bc, index) != 1) {
		    throw new CompilerError("invalid constants in ldc");
		}
		bc.stack_delta = 1;
		break;
	    case opc_ldc_w:
	    case opc_ldc_w_quick:
		index = pc2ushort(pc+1);
		pc += 2;
		if (ldc(bc, index) != 1) {
		    throw new CompilerError("invalid constants in ldc_w");
		}
		bc.stack_delta = 1;
		break;
	    case opc_ldc2_w:
	    case opc_ldc2_w_quick:
		index = pc2ushort(pc+1);
		pc += 2;
		if (ldc(bc, index) != 2) {
		    throw new CompilerError("invalid constants in ldc2_w");
		}
		bc.stack_delta = 2;
		break;
	    case opc_iload:
	    case opc_aload:
		index = pc2uchar(pc+1);
		pc++;
		ll = (new ILnode(ll)).move(TAG_SI, 0, TAG_VI, index);
		bc.stack_delta = 1;
		break;
	    case opc_iload_0: case opc_iload_1: case opc_iload_2: case opc_iload_3:
		index = opcode - opc_iload_0;
		ll = (new ILnode(ll)).move(TAG_SI, 0, TAG_VI, index);
		bc.stack_delta = 1;
		break;
	    case opc_aload_0: case opc_aload_1: case opc_aload_2: case opc_aload_3:
		index = opcode - opc_aload_0;
		ll = (new ILnode(ll)).move(TAG_SI, 0, TAG_VI, index);
		bc.stack_delta = 1;
		break;
	    case opc_lload:
		index = pc2uchar(pc+1);
		pc++;
		ll = (new ILnode(ll)).move(TAG_SI, 0, TAG_VI, index);
		ll = (new ILnode(ll)).move(TAG_SI, 1, TAG_VI, index+1);
		bc.stack_delta = 2;
		break;
	    case opc_lload_0: case opc_lload_1: case opc_lload_2: case opc_lload_3:
		index = opcode - opc_lload_0;
		ll = (new ILnode(ll)).move(TAG_SI, 0, TAG_VI, index);
		ll = (new ILnode(ll)).move(TAG_SI, 1, TAG_VI, index+1);
		bc.stack_delta = 2;
		break;
	    case opc_fload:
		index = pc2uchar(pc+1);
		pc++;
		ll = (new ILnode(ll)).move(TAG_SF, 0, TAG_VF, index);
		bc.stack_delta = 1;
		break;
	    case opc_fload_0: case opc_fload_1: case opc_fload_2: case opc_fload_3:
		index = opcode - opc_fload_0;
		ll = (new ILnode(ll)).move(TAG_SF, 0, TAG_VF, index);
		bc.stack_delta = 1;
		break;
	    case opc_dload:
		index = pc2uchar(pc+1);
		pc++;
		ll = (new ILnode(ll)).move(TAG_SD, 0, TAG_VD, index);
		bc.stack_delta = 2;
		break;
	    case opc_dload_0: case opc_dload_1: case opc_dload_2: case opc_dload_3:
		index = opcode - opc_dload_0;
		ll = (new ILnode(ll)).move(TAG_SD, 0, TAG_VD, index);
		bc.stack_delta = 2;
		break;
	    case opc_iaload:
	    case opc_aaload:
		ll = array_intro(ll, -2, -1, 2);
		ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-2), TAG_SI,(-2), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;

	    case opc_laload:
		ll = array_intro(ll, -2, -1, 3);
		ll = (new ILnode(ll)).insn(ILnode.ADD, TAG_SI,(((0))), TAG_SI,(-2), TAG_SI,(-1));
		ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-2), TAG_SI,(((0))), TAG_CONST,(0));
		ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-1), TAG_SI,(((0))), TAG_CONST,(4));
		bc.stack_delta = 0;
		break;

	    case opc_faload:
		ll = array_intro(ll, -2, -1, 2);
		ll = (new ILnode(ll)).insn(ILnode.FLD, TAG_SF,(-2), TAG_SI,(-2), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;

	    case opc_daload:
		ll = array_intro(ll, -2, -1, 3);
		ll = (new ILnode(ll)).insn(ILnode.DLD, TAG_SD,(-2), TAG_SI,(-2), TAG_SI,(-1));
		bc.stack_delta = 0;
		break;

	    case opc_baload:
		ll = array_intro(ll, -2, -1, 0);
		ll = (new ILnode(ll)).insn(ILnode.LDSB, TAG_SI,(-2), TAG_SI,(-2), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;

	    case opc_caload:
		ll = array_intro(ll, -2, -1, 1);
		ll = (new ILnode(ll)).insn(ILnode.LDUH, TAG_SI,(-2), TAG_SI,(-2), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;

	    case opc_saload:
		ll = array_intro(ll, -2, -1, 1);
		ll = (new ILnode(ll)).insn(ILnode.LDSH, TAG_SI,(-2), TAG_SI,(-2), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;

	    case opc_astore:
		index = pc2uchar(pc+1);
		pc++;
		ll = (new ILnode(ll)).move(TAG_VI,(index), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_istore:
		index = pc2uchar(pc+1);
		pc++;
		ll = (new ILnode(ll)).move(TAG_VI,(index), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_astore_0: case opc_astore_1:
	    case opc_astore_2: case opc_astore_3:
		index = opcode - opc_astore_0;
		ll = (new ILnode(ll)).move(TAG_VI,(index), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_istore_0: case opc_istore_1:
	    case opc_istore_2: case opc_istore_3:
		index = opcode - opc_istore_0;
		ll = (new ILnode(ll)).move(TAG_VI,(index), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;

	    case opc_lstore:
		index = pc2uchar(pc+1);
		pc++;
		ll = (new ILnode(ll)).move(TAG_VI,(index), TAG_SI,(-2));
		ll = (new ILnode(ll)).move(TAG_VI,(index+1), TAG_SI,(-1));
		bc.stack_delta = -2;
		break;
	    case opc_lstore_0: case opc_lstore_1:
	    case opc_lstore_2: case opc_lstore_3:
		index = opcode - opc_lstore_0;
		ll = (new ILnode(ll)).move(TAG_VI,(index), TAG_SI,(-2));
		ll = (new ILnode(ll)).move(TAG_VI,(index+1), TAG_SI,(-1));
		bc.stack_delta = -2;
		break;

	    case opc_fstore:
		index = pc2uchar(pc+1);
		pc++;
		ll = (new ILnode(ll)).move(TAG_VF,(index), TAG_SF,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_fstore_0: case opc_fstore_1:
	    case opc_fstore_2: case opc_fstore_3:
		index = opcode - opc_fstore_0;
		ll = (new ILnode(ll)).move(TAG_VF,(index), TAG_SF,(-1));
		bc.stack_delta = -1;
		break;

	    case opc_dstore:
		index = pc2uchar(pc+1);
		pc++;
		ll = (new ILnode(ll)).move(TAG_VD,(index), TAG_SD,(-2));
		bc.stack_delta = -2;
		break;
	    case opc_dstore_0: case opc_dstore_1:
	    case opc_dstore_2: case opc_dstore_3:
		index = opcode - opc_dstore_0;
		ll = (new ILnode(ll)).move(TAG_VD,(index), TAG_SD,(-2));
		bc.stack_delta = -2;
		break;

	    case opc_aastore:
		if (!omit_array_store_check) {
		    flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		    ll = (new ILnode(ll)).move(TAG_PI,(ARG1), TAG_SI | TAG_REF,(-3));
		    ll = (new ILnode(ll)).move(TAG_PI,(ARG0), TAG_SI | TAG_REF,(-1));
		    ll = (new ILnode(ll)).call(TAG_CONST,(0), RT_check_aastore);
		}
		/* fall through */
	    case opc_iastore:
		ll = array_intro(ll, -3, -2, 2);
		ll = (new ILnode(ll)).insn(ILnode.ST, TAG_SI,(-1), TAG_SI,(-3), TAG_SI,(-2));
		bc.stack_delta = -3;
		break;

	    case opc_lastore:
		ll = array_intro(ll, -4, -3, 3);
		ll = (new ILnode(ll)).insn(ILnode.ADD, TAG_SI,(-4), TAG_SI,(-4), TAG_SI,(-3));
		ll = (new ILnode(ll)).insn(ILnode.ST, TAG_SI,(-2), TAG_SI,(-4), TAG_CONST,(0));
		ll = (new ILnode(ll)).insn(ILnode.ST, TAG_SI,(-1), TAG_SI,(-4), TAG_CONST,(4));
		bc.stack_delta = -4;
		break;

	    case opc_fastore:
		ll = array_intro(ll, -3, -2, 2);
		ll = (new ILnode(ll)).insn(ILnode.FST, TAG_SF,(-1), TAG_SI,(-3), TAG_SI,(-2));
		bc.stack_delta = -3;
		break;

	    case opc_dastore:
		ll = array_intro(ll, -4, -3, 3);
		ll = (new ILnode(ll)).insn(ILnode.DST, TAG_SD,(-2), TAG_SI,(-4), TAG_SI,(-3));
		bc.stack_delta = -4;
		break;

	    case opc_bastore:
		ll = array_intro(ll, -3, -2, 0);
		ll = (new ILnode(ll)).insn(ILnode.STB, TAG_SI,(-1), TAG_SI,(-3), TAG_SI,(-2));
		bc.stack_delta = -3;
		break;

	    case opc_castore:
	    case opc_sastore:
		ll = array_intro(ll, -3, -2, 1);
		ll = (new ILnode(ll)).insn(ILnode.STH, TAG_SI,(-1), TAG_SI,(-3), TAG_SI,(-2));
		bc.stack_delta = -3;
		break;

	    case opc_pop:
		bc.stack_delta = -1;
		break;
	    case opc_pop2:
		bc.stack_delta = -2;
		break;
	    case opc_dup:
		ll = (new ILnode(ll)).moveStack(( 0), (-1));
		bc.stack_delta = 1;
		break;
	    case opc_dup_x1:
		ll = (new ILnode(ll)).moveStack(( 0), (-1));
		ll = (new ILnode(ll)).moveStack((-1), (-2));
		ll = (new ILnode(ll)).moveStack((-2), ( 0));
		bc.stack_delta = 1;
		break;
	    case opc_dup_x2:
		ll = (new ILnode(ll)).moveStack(( 0), (-1));
		ll = (new ILnode(ll)).moveStack((-1), (-2));
		ll = (new ILnode(ll)).moveStack((-2), (-3));
		ll = (new ILnode(ll)).moveStack((-3), ( 0));
		bc.stack_delta = 1;
		break;
	    case opc_dup2:
		ll = (new ILnode(ll)).moveStack(( 0), (-2));
		ll = (new ILnode(ll)).moveStack(( 1), (-1));
		bc.stack_delta = 2;
		break;
	    case opc_dup2_x1:
		ll = (new ILnode(ll)).moveStack(( 1), (-1));
		ll = (new ILnode(ll)).moveStack(( 0), (-2));
		ll = (new ILnode(ll)).moveStack((-1), (-3));
		ll = (new ILnode(ll)).moveStack((-2), ( 1));
		ll = (new ILnode(ll)).moveStack((-3), ( 0));
		bc.stack_delta = 2;
		break;
	    case opc_dup2_x2:
		ll = (new ILnode(ll)).moveStack(( 1), (-1));
		ll = (new ILnode(ll)).moveStack(( 0), (-2));
		ll = (new ILnode(ll)).moveStack((-1), (-3));
		ll = (new ILnode(ll)).moveStack((-2), (-4));
		ll = (new ILnode(ll)).moveStack((-3), ( 1));
		ll = (new ILnode(ll)).moveStack((-4), ( 0));
		bc.stack_delta = 2;
		break;
	    case opc_swap:
		ll = (new ILnode(ll)).moveStack((((0))), (-2));
		ll = (new ILnode(ll)).moveStack((-2), (-1));
		ll = (new ILnode(ll)).moveStack((-1), (((0))));
		bc.stack_delta = 0;
		break;
	    case opc_iadd:
		ll = (new ILnode(ll)).insn(ILnode.ADD, TAG_SI,(-2), TAG_SI,(-2), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_ladd:
		if (little_endian_long) {
		    ll = (new ILnode(ll)).insn(ILnode.ADDL, TAG_SI,(-4), TAG_SI,(-4), TAG_SI,(-2));
		    ll = (new ILnode(ll)).insn(ILnode.ADDH, TAG_SI,(-3), TAG_SI,(-3), TAG_SI,(-1));
		} else {
		    ll = (new ILnode(ll)).insn(ILnode.ADDL, TAG_SI,(-3), TAG_SI,(-3), TAG_SI,(-1));
		    ll = (new ILnode(ll)).insn(ILnode.ADDH, TAG_SI,(-4), TAG_SI,(-4), TAG_SI,(-2));
		}
		bc.stack_delta = -2;
		break;
	    case opc_fadd:
		ll = (new ILnode(ll)).insn(ILnode.FADD, TAG_SF,(-2), TAG_SF, (-2), TAG_SF,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_dadd:
		ll = (new ILnode(ll)).insn(ILnode.DADD, TAG_SD,(-4), TAG_SD,(-4), TAG_SD,(-2));
		bc.stack_delta = -2;
		break;
	    case opc_isub:
		ll = (new ILnode(ll)).insn(ILnode.SUB, TAG_SI,(-2), TAG_SI,(-2), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_lsub:
		if (little_endian_long) {
		    ll = (new ILnode(ll)).insn(ILnode.SUBL, TAG_SI,(-4), TAG_SI,(-4), TAG_SI,(-2));
		    ll = (new ILnode(ll)).insn(ILnode.SUBH, TAG_SI,(-3), TAG_SI,(-3), TAG_SI,(-1));
		} else {
		    ll = (new ILnode(ll)).insn(ILnode.SUBL, TAG_SI,(-3), TAG_SI,(-3), TAG_SI,(-1));
		    ll = (new ILnode(ll)).insn(ILnode.SUBH, TAG_SI,(-4), TAG_SI,(-4), TAG_SI,(-2));
		}
		bc.stack_delta = -2;
		break;
	    case opc_fsub:
		ll = (new ILnode(ll)).insn(ILnode.FSUB, TAG_SF,(-2), TAG_SF,(-2), TAG_SF,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_dsub:
		ll = (new ILnode(ll)).insn(ILnode.DSUB, TAG_SD,(-4), TAG_SD,(-4), TAG_SD,(-2));
		bc.stack_delta = -2;
		break;
	    case opc_imul:
		ll = (new ILnode(ll)).insn(ILnode.MUL, TAG_SI,(-2), TAG_SI,(-2), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_lmul:
		flags |= FLAG_HAS_CALL;
		ll = (new ILnode(ll)).move(TAG_PI, ARG3, TAG_SI, -1);
		ll = (new ILnode(ll)).move(TAG_PI, ARG2, TAG_SI, -2);
		ll = (new ILnode(ll)).move(TAG_PI, ARG1, TAG_SI, -3);
		ll = (new ILnode(ll)).move(TAG_PI, ARG0, TAG_SI, -4);
		ll = (new ILnode(ll)).call(TAG_SL, -4, RT_lmul);
		ll = (new ILnode(ll)).move(TAG_SI, -3, TAG_PI, RETL);
		ll = (new ILnode(ll)).move(TAG_SI, -4, TAG_PI, RETI);
		bc.stack_delta = -2;
		break;
	    case opc_fmul:
		ll = (new ILnode(ll)).insn(ILnode.FMUL, TAG_SF,(-2), TAG_SF,(-2), TAG_SF,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_dmul:
		ll = (new ILnode(ll)).insn(ILnode.DMUL, TAG_SD,(-4), TAG_SD,(-4), TAG_SD,(-2));
		bc.stack_delta = -2;
		break;
	    case opc_idiv:
		flags |= FLAG_HAS_EXCEPTION;
		ll = (new ILnode(ll)).insn(ILnode.DIV, TAG_SI,(-2), TAG_SI,(-2), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_ldiv:
		flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		ll = (new ILnode(ll)).insn(ILnode.DIV0CHK, TAG_CONST,(0), TAG_SI,(-2), TAG_SI,(-1));
		ll = (new ILnode(ll)).move(TAG_PI, ARG3, TAG_SI, -1);
		ll = (new ILnode(ll)).move(TAG_PI, ARG2, TAG_SI, -2);
		ll = (new ILnode(ll)).move(TAG_PI, ARG1, TAG_SI, -3);
		ll = (new ILnode(ll)).move(TAG_PI, ARG0, TAG_SI, -4);
		ll = (new ILnode(ll)).call(TAG_SL, -4, RT_ldiv);
		ll = (new ILnode(ll)).move(TAG_SI, -3, TAG_PI, RETL);
		ll = (new ILnode(ll)).move(TAG_SI, -4, TAG_PI, RETI);
		bc.stack_delta = -2;
		break;
	    case opc_fdiv:
		ll = (new ILnode(ll)).insn(ILnode.FDIV, TAG_SF,(-2), TAG_SF,(-2), TAG_SF,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_ddiv:
		ll = (new ILnode(ll)).insn(ILnode.DDIV, TAG_SD,(-4), TAG_SD,(-4), TAG_SD,(-2));
		bc.stack_delta = -2;
		break;
	    case opc_irem:
		flags |= FLAG_HAS_EXCEPTION;
		ll = (new ILnode(ll)).insn(ILnode.IREM, TAG_SI,(-2), TAG_SI,(-2), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_lrem:
		flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		ll = (new ILnode(ll)).insn(ILnode.DIV0CHK, TAG_CONST,(0), TAG_SI,(-2), TAG_SI,(-1));
		ll = (new ILnode(ll)).move(TAG_PI, ARG3, TAG_SI, -1);
		ll = (new ILnode(ll)).move(TAG_PI, ARG2, TAG_SI, -2);
		ll = (new ILnode(ll)).move(TAG_PI, ARG1, TAG_SI, -3);
		ll = (new ILnode(ll)).move(TAG_PI, ARG0, TAG_SI, -4);
		ll = (new ILnode(ll)).call(TAG_SL, -4, RT_lrem);
		ll = (new ILnode(ll)).move(TAG_SI, -3, TAG_PI, RETL);
		ll = (new ILnode(ll)).move(TAG_SI, -4, TAG_PI, RETI);
		bc.stack_delta = -2;
		break;
	    case opc_frem:
		flags |= FLAG_HAS_CALL;
  		ll = (new ILnode(ll)).move(TAG_PF, ARG1, TAG_SF, -1);
  		ll = (new ILnode(ll)).move(TAG_PF, ARG0, TAG_SF, -2);
		ll = (new ILnode(ll)).call(TAG_SF, -2, RT_frem);
		ll = (new ILnode(ll)).move(TAG_SF,(-2), TAG_PF,(RETF));
		bc.stack_delta = -1;
		break;
	    case opc_drem:
		flags |= FLAG_HAS_CALL;
  		ll = (new ILnode(ll)).move(TAG_PD, ARG2, TAG_SD, -2);
  		ll = (new ILnode(ll)).move(TAG_PD, ARG0, TAG_SD, -4);
		ll = (new ILnode(ll)).call(TAG_SD, -4, RT_drem);
		ll = (new ILnode(ll)).move(TAG_SD,(-4), TAG_PD,(RETF));
		bc.stack_delta = -2;
		break;
	    case opc_ineg:
		ll = (new ILnode(ll)).insn(ILnode.SUB, TAG_SI,(-1), TAG_CONST,(0), TAG_SI,(-1));
		bc.stack_delta = 0;
		break;
	    case opc_lneg:
		if (little_endian_long) {
		    ll = (new ILnode(ll)).insn(ILnode.SUBL, TAG_SI,(-2), TAG_CONST,(0), TAG_SI,(-2));
		    ll = (new ILnode(ll)).insn(ILnode.SUBH, TAG_SI,(-1), TAG_CONST,(0), TAG_SI,(-1));
		} else {
		    ll = (new ILnode(ll)).insn(ILnode.SUBL, TAG_SI,(-1), TAG_CONST,(0), TAG_SI,(-1));
		    ll = (new ILnode(ll)).insn(ILnode.SUBH, TAG_SI,(-2), TAG_CONST,(0), TAG_SI,(-2));
		}
		bc.stack_delta = 0;
		break;
	    case opc_fneg:
		ll = (new ILnode(ll)).insn(ILnode.FNEG, TAG_SF,(-1), TAG_CONST,(0), TAG_SF,(-1));
		bc.stack_delta = 0;
		break;
	    case opc_dneg:
		ll = (new ILnode(ll)).insn(ILnode.DNEG, TAG_SD,(-2), TAG_CONST,(0), TAG_SD,(-2));
		// ll = (new ILnode(ll)).move(TAG_SF,(-1), TAG_SF,(-1));
		bc.stack_delta = 0;
		break;
	    case opc_ishl:
		ll = (new ILnode(ll)).insn(ILnode.SLL, TAG_SI,(-2), TAG_SI,(-2), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_lshl:
		flags |= FLAG_HAS_CALL;
		ll = (new ILnode(ll)).insn(ILnode.AND, TAG_SI,(-1), TAG_SI,(-1), TAG_CONST,(0x3F));
		ll = (new ILnode(ll)).move(TAG_PI,(ARG2), TAG_SI,(-1));
		ll = (new ILnode(ll)).move(TAG_PI,(ARG1), TAG_SI,(-2));
		ll = (new ILnode(ll)).move(TAG_PI,(ARG0), TAG_SI,(-3));
		ll = (new ILnode(ll)).call(TAG_SL,(-3), RT_lshl);
		ll = (new ILnode(ll)).move(TAG_SI,(-2), TAG_PI,(RETL));
		ll = (new ILnode(ll)).move(TAG_SI,(-3), TAG_PI,(RETI));
		bc.stack_delta = -1;
		break;
	    case opc_ishr:
		ll = (new ILnode(ll)).insn(ILnode.SRA, TAG_SI,(-2), TAG_SI,(-2), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_lshr:
		flags |= FLAG_HAS_CALL;
		ll = (new ILnode(ll)).insn(ILnode.AND, TAG_SI,(-1), TAG_SI,(-1), TAG_CONST,(0x3F));
		ll = (new ILnode(ll)).move(TAG_PI,(ARG2), TAG_SI,(-1));
		ll = (new ILnode(ll)).move(TAG_PI,(ARG1), TAG_SI,(-2));
		ll = (new ILnode(ll)).move(TAG_PI,(ARG0), TAG_SI,(-3));
		ll = (new ILnode(ll)).call(TAG_SL,(-3), RT_lshr);
		ll = (new ILnode(ll)).move(TAG_SI,(-2), TAG_PI,(RETL));
		ll = (new ILnode(ll)).move(TAG_SI,(-3), TAG_PI,(RETI));
		bc.stack_delta = -1;
		break;
	    case opc_iushr:
		ll = (new ILnode(ll)).insn(ILnode.SRL, TAG_SI,(-2), TAG_SI,(-2), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_lushr:
		flags |= FLAG_HAS_CALL;
		ll = (new ILnode(ll)).insn(ILnode.AND, TAG_SI,(-1), TAG_SI,(-1), TAG_CONST,(0x3F));
		ll = (new ILnode(ll)).move(TAG_PI,(ARG2), TAG_SI,(-1));
		ll = (new ILnode(ll)).move(TAG_PI,(ARG1), TAG_SI,(-2));
		ll = (new ILnode(ll)).move(TAG_PI,(ARG0), TAG_SI,(-3));
		ll = (new ILnode(ll)).call(TAG_SL,(-3), RT_lushr);
		ll = (new ILnode(ll)).move(TAG_SI,(-2), TAG_PI,(RETL));
		ll = (new ILnode(ll)).move(TAG_SI,(-3), TAG_PI,(RETI));
		bc.stack_delta = -1;
		break;
	    case opc_iand:
		if (pc + 1 < bytecode_length) {
		    /* check next bytecode */
		    switch(pc2uchar(pc+1)) {
		    case opc_ifeq:
			ll = (new ILnode(ll)).insn(ILnode.TEST, TAG_CONST,(0), TAG_SI,(-2), TAG_SI,(-1));
			ll = (new ILnode(ll)).branch(CC_E, branch(pc+1, pc2signedshort(pc+2)));
			bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
			pc += 3;
			bc.stack_delta = -2;
			break;
		    case opc_ifne:
			ll = (new ILnode(ll)).insn(ILnode.TEST, TAG_CONST,(0), TAG_SI,(-2), TAG_SI,(-1));
			ll = (new ILnode(ll)).branch(CC_NE, branch(pc+1, pc2signedshort(pc+2)));
			bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
			pc += 3;
			bc.stack_delta = -2;
			break;
		    default:
			ll = (new ILnode(ll)).insn(ILnode.AND, TAG_SI,(-2), TAG_SI,(-2), TAG_SI,(-1));
			bc.stack_delta = -1;
		    }
		    break;
		}
		ll = (new ILnode(ll)).insn(ILnode.AND, TAG_SI,(-2), TAG_SI,(-2), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_land:
		ll = (new ILnode(ll)).insn(ILnode.AND, TAG_SI,(-4), TAG_SI,(-4), TAG_SI,(-2));
		ll = (new ILnode(ll)).insn(ILnode.AND, TAG_SI,(-3), TAG_SI,(-3), TAG_SI,(-1));
		bc.stack_delta = -2;
		break;
	    case opc_ior:
		ll = (new ILnode(ll)).insn(ILnode.OR, TAG_SI,(-2), TAG_SI,(-2), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_lor:
		ll = (new ILnode(ll)).insn(ILnode.OR, TAG_SI,(-4), TAG_SI,(-4), TAG_SI,(-2));
		ll = (new ILnode(ll)).insn(ILnode.OR, TAG_SI,(-3), TAG_SI,(-3), TAG_SI,(-1));
		bc.stack_delta = -2;
		break;
	    case opc_ixor:
		ll = (new ILnode(ll)).insn(ILnode.XOR, TAG_SI,(-2), TAG_SI,(-2), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_lxor:
		ll = (new ILnode(ll)).insn(ILnode.XOR, TAG_SI,(-4), TAG_SI,(-4), TAG_SI,(-2));
		ll = (new ILnode(ll)).insn(ILnode.XOR, TAG_SI,(-3), TAG_SI,(-3), TAG_SI,(-1));
		bc.stack_delta = -2;
		break;
	    case opc_iinc:
		index = pc2uchar(pc+1);
		offset = pc2signedchar(pc+2);
		pc += 2;
		ll = (new ILnode(ll)).insn(ILnode.ADD, TAG_VI,(index), TAG_VI,(index), TAG_CONST,(offset));
		bc.stack_delta = 0;
		break;
	    case opc_i2l:
		if (little_endian_long) {
		    /* ll = (newILnode(ll)).move(TAG_SI,(-1), TAG_SI,(-1)); */
		    ll = (new ILnode(ll)).insn(ILnode.SRA, TAG_SI,(0), TAG_SI,(-1), TAG_CONST,(31));
		} else {
		    ll = (new ILnode(ll)).move(TAG_SI,(0), TAG_SI,(-1));
		    ll = (new ILnode(ll)).insn(ILnode.SRA, TAG_SI,(-1), TAG_SI,(-1), TAG_CONST,(31));
		}
		bc.stack_delta = 1;
		break;
	    case opc_i2f:
		ll = (new ILnode(ll)).insn2(ILnode.I2F, TAG_SF,(-1), TAG_SI,(-1));
		bc.stack_delta = 0;
		break;
	    case opc_i2d:
		ll = (new ILnode(ll)).insn2(ILnode.I2D, TAG_SD,(-1), TAG_SI,(-1));
		bc.stack_delta = 1;
		break;
	    case opc_l2i:
		if (!little_endian_long)
		    ll = (new ILnode(ll)).move(TAG_SI,(-2), TAG_SI,(-1));
		bc.stack_delta = -1;
		break;
	    case opc_l2f:
		flags |= FLAG_HAS_CALL;
		ll = (new ILnode(ll)).move(TAG_PI,(ARG1), TAG_SI,(-1));
		ll = (new ILnode(ll)).move(TAG_PI,(ARG0), TAG_SI,(-2));
		ll = (new ILnode(ll)).call(TAG_SF,(-2), RT_l2f);
		ll = (new ILnode(ll)).move(TAG_SF,(-2), TAG_PF,(RETF));
		bc.stack_delta = -1;
		break;
	    case opc_l2d:
		flags |= FLAG_HAS_CALL;
		ll = (new ILnode(ll)).move(TAG_PI,(ARG1), TAG_SI,(-1));
		ll = (new ILnode(ll)).move(TAG_PI,(ARG0), TAG_SI,(-2));
		ll = (new ILnode(ll)).call(TAG_SD,(-2), RT_l2d);
		ll = (new ILnode(ll)).move(TAG_SD,(-2), TAG_PD,(RETF));
		bc.stack_delta = 0;
		break;
	    case opc_f2i:
		ll = (new ILnode(ll)).insn2(ILnode.F2I, TAG_SI,(-1), TAG_SF,(-1));
		bc.stack_delta = 0;
		break;
	    case opc_f2l:
		flags |= FLAG_HAS_CALL;
		ll = (new ILnode(ll)).move(TAG_PF,(ARG0), TAG_SF,(-1));
		ll = (new ILnode(ll)).call(TAG_SL,(-1), RT_f2l);
		ll = (new ILnode(ll)).move(TAG_SI,(0), TAG_PI,(RETL));
		ll = (new ILnode(ll)).move(TAG_SI,(-1), TAG_PI,(RETI));
		bc.stack_delta = 1;
		break;
	    case opc_f2d:
		ll = (new ILnode(ll)).insn2(ILnode.F2D, TAG_SD,(-1), TAG_SF,(-1));
		bc.stack_delta = 1;
		break;
	    case opc_d2i:
		ll = (new ILnode(ll)).insn2(ILnode.D2I, TAG_SI,(-2), TAG_SD,(-2));
		bc.stack_delta = -1;
		break;
	    case opc_d2l:
		flags |= FLAG_HAS_CALL;
		ll = (new ILnode(ll)).move(TAG_PD,(ARG0), TAG_SD,(-2));
		ll = (new ILnode(ll)).call(TAG_SL,(-2), RT_d2l);
		ll = (new ILnode(ll)).move(TAG_SI,(-1), TAG_PI,(RETL));
		ll = (new ILnode(ll)).move(TAG_SI,(-2), TAG_PI,(RETI));
		bc.stack_delta = 0;
		break;
	    case opc_d2f:
		ll = (new ILnode(ll)).insn2(ILnode.D2F, TAG_SF,(-2), TAG_SD,(-2));
		bc.stack_delta = -1;
		break;
	    case opc_i2b:
		ll = (new ILnode(ll)).insn2(ILnode.I2B, TAG_SI,(-1), TAG_SI,(-1));
		bc.stack_delta = 0;
		break;
	    case opc_i2c:
		ll = (new ILnode(ll)).insn2(ILnode.I2C, TAG_SI,(-1), TAG_SI,(-1));
		bc.stack_delta = 0;
		break;
	    case opc_i2s:
		ll = (new ILnode(ll)).insn2(ILnode.I2S, TAG_SI,(-1), TAG_SI,(-1));
		bc.stack_delta = 0;
		break;
	    case opc_lcmp:
		if (optim_lcmp(pc)) {
		    bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		    pc += 3;
		    bc.stack_delta = -4;
		} else {
		    flags |= FLAG_HAS_CALL;
		    ll = (new ILnode(ll)).move(TAG_PI,(ARG3), TAG_SI,(-1));
		    ll = (new ILnode(ll)).move(TAG_PI,(ARG2), TAG_SI,(-2));
		    ll = (new ILnode(ll)).move(TAG_PI,(ARG1), TAG_SI,(-3));
		    ll = (new ILnode(ll)).move(TAG_PI,(ARG0), TAG_SI,(-4));
		    ll = (new ILnode(ll)).call(TAG_SI,(-4), RT_lcmp);
		    ll = (new ILnode(ll)).move(TAG_SI,(-4), TAG_PI,(RETI));
		    bc.stack_delta = -3;
		}
		break;
	    case opc_fcmpl:
		ILnode br = optim_fcmp(pc, false);
		if (br != null) {
		    ll = (new ILnode(ll).insn(ILnode.FCMP, TAG_CONST,(0), TAG_SF,(-2), TAG_SF,(-1)));
		    ll.next = br;
		    bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		    pc += 3;
		    bc.stack_delta = -2;
		} else {
		    flags |= FLAG_HAS_CALL;
		    ll = (new ILnode(ll)).move(TAG_PF,(ARG1), TAG_SF,(-1));
		    ll = (new ILnode(ll)).move(TAG_PF,(ARG0), TAG_SF,(-2));
		    ll = (new ILnode(ll)).call(TAG_SI,(-2), RT_fcmpl);
		    ll = (new ILnode(ll)).move(TAG_SI,(-2), TAG_PI,(RETI));
		    bc.stack_delta = -1;
		}
		break;
	    case opc_fcmpg:
		br = optim_fcmp(pc, true);
		if (br != null) {
		    ll = (new ILnode(ll).insn(ILnode.FCMP, TAG_CONST,(0), TAG_SF,(-2), TAG_SF,(-1)));
		    ll.next = br;
		    bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		    pc += 3;
		    bc.stack_delta = -2;
		} else {
		    flags |= FLAG_HAS_CALL;
		    ll = (new ILnode(ll)).move(TAG_PF,(ARG1), TAG_SF,(-1));
		    ll = (new ILnode(ll)).move(TAG_PF,(ARG0), TAG_SF,(-2));
		    ll = (new ILnode(ll)).call(TAG_SI,(-2), RT_fcmpg);
		    ll = (new ILnode(ll)).move(TAG_SI,(-2), TAG_PI,(RETI));
		    bc.stack_delta = -1;
		}
		break;
	    case opc_dcmpl:
		br = optim_fcmp(pc, false);
		if (br != null) {
		    ll = (new ILnode(ll).insn(ILnode.DCMP, TAG_CONST,(0), TAG_SD,(-4), TAG_SD,(-2)));
		    ll.next = br;
		    bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		    pc += 3;
		    bc.stack_delta = -4;
		} else {
		    flags |= FLAG_HAS_CALL;
		    ll = (new ILnode(ll)).move(TAG_PD,(ARG2), TAG_SD,(-2));
		    ll = (new ILnode(ll)).move(TAG_PD,(ARG0), TAG_SD,(-4));
		    ll = (new ILnode(ll)).call(TAG_SI,(-4), RT_dcmpl);
		    ll = (new ILnode(ll)).move(TAG_SI,(-4), TAG_PI,(RETI));
		    bc.stack_delta = -3;
		}
		break;
	    case opc_dcmpg:
		br = optim_fcmp(pc, true);
		if (br != null) {
		    ll = (new ILnode(ll).insn(ILnode.DCMP, TAG_CONST,(0), TAG_SD,(-4), TAG_SD,(-2)));
		    ll.next = br;
		    bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		    pc += 3;
		    bc.stack_delta = -4;
		} else {
		    flags |= FLAG_HAS_CALL;
		    ll = (new ILnode(ll)).move(TAG_PD,(ARG2), TAG_SD,(-2));
		    ll = (new ILnode(ll)).move(TAG_PD,(ARG0), TAG_SD,(-4));
		    ll = (new ILnode(ll)).call(TAG_SI,(-4), RT_dcmpg);
		    ll = (new ILnode(ll)).move(TAG_SI,(-4), TAG_PI,(RETI));
		    bc.stack_delta = -3;
		}
		break;
	    case opc_ifeq:
		if (optim_neg(pc)) {
		    ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_CONST,(0), TAG_SI,(-1));
		    ll = (new ILnode(ll)).insn(ILnode.SUBH, TAG_SI,(-1), TAG_CONST,(0), TAG_CONST,(-1));
		    bc.stack_delta = 0;
		    pc += 7;
		} else {
		    ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-1), TAG_CONST,(0));
		    ll = (new ILnode(ll)).branch(CC_E, branch(pc, pc2signedshort(pc+1)));
		    bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		    pc += 2;
		    bc.stack_delta = -1;
		}
		break;
	    case opc_ifne:
		ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-1), TAG_CONST,(0));
		ll = (new ILnode(ll)).branch(CC_NE, branch(pc, pc2signedshort(pc+1)));
		bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		pc += 2;
		bc.stack_delta = -1;
		break;
	    case opc_iflt:
		ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-1), TAG_CONST,(0));
		ll = (new ILnode(ll)).branch(CC_L, branch(pc, pc2signedshort(pc+1)));
		bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		pc += 2;
		bc.stack_delta = -1;
		break;
	    case opc_ifge:
		ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-1), TAG_CONST,(0));
		ll = (new ILnode(ll)).branch(CC_GE, branch(pc, pc2signedshort(pc+1)));
		bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		pc += 2;
		bc.stack_delta = -1;
		break;
	    case opc_ifgt:
		ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-1), TAG_CONST,(0));
		ll = (new ILnode(ll)).branch(CC_G, branch(pc, pc2signedshort(pc+1)));
		bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		pc += 2;
		bc.stack_delta = -1;
		break;
	    case opc_ifle:
		ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-1), TAG_CONST,(0));
		ll = (new ILnode(ll)).branch(CC_LE, branch(pc, pc2signedshort(pc+1)));
		bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		pc += 2;
		bc.stack_delta = -1;
		break;
	    case opc_if_icmpeq:
		ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-2), TAG_SI,(-1));
		ll = (new ILnode(ll)).branch(CC_E, branch(pc, pc2signedshort(pc+1)));
		bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		pc += 2;
		bc.stack_delta = -2;
		break;
	    case opc_if_icmpne:
		ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-2), TAG_SI,(-1));
		ll = (new ILnode(ll)).branch(CC_NE, branch(pc, pc2signedshort(pc+1)));
		bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		pc += 2;
		bc.stack_delta = -2;
		break;
	    case opc_if_icmplt:
		ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-2), TAG_SI,(-1));
		ll = (new ILnode(ll)).branch(CC_L, branch(pc, pc2signedshort(pc+1)));
		bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		pc += 2;
		bc.stack_delta = -2;
		break;
	    case opc_if_icmpge:
		ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-2), TAG_SI,(-1));
		ll = (new ILnode(ll)).branch(CC_GE, branch(pc, pc2signedshort(pc+1)));
		bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		pc += 2;
		bc.stack_delta = -2;
		break;
	    case opc_if_icmpgt:
		ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-2), TAG_SI,(-1));
		ll = (new ILnode(ll)).branch(CC_G, branch(pc, pc2signedshort(pc+1)));
		bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		pc += 2;
		bc.stack_delta = -2;
		break;
	    case opc_if_icmple:
		ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-2), TAG_SI,(-1));
		ll = (new ILnode(ll)).branch(CC_LE, branch(pc, pc2signedshort(pc+1)));
		bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		pc += 2;
		bc.stack_delta = -2;
		break;
	    case opc_if_acmpeq:
		ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-2), TAG_SI,(-1));
		ll = (new ILnode(ll)).branch(CC_E, branch(pc, pc2signedshort(pc+1)));
		bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		pc += 2;
		bc.stack_delta = -2;
		break;
	    case opc_if_acmpne:
		ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-2), TAG_SI,(-1));
		ll = (new ILnode(ll)).branch(CC_NE, branch(pc, pc2signedshort(pc+1)));
		bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		pc += 2;
		bc.stack_delta = -2;
		break;

	    case opc_goto:
		index = pc2signedshort(pc+1);
		ll = (new ILnode(ll)).branch(CC_A, branch(pc, index));
		bc.flag |= BCinfo.FLAG_CTRLFLOW_END;
		pc += 2;
		bc.stack_delta = 0;
		break;
	    case opc_goto_w:
		index = pc2signedlong(pc+1);
		ll = (new ILnode(ll)).branch(CC_A, branch(pc, index));
		bc.flag |= BCinfo.FLAG_CTRLFLOW_END;
		pc += 4;
		bc.stack_delta = 0;
		break;

	    case opc_jsr:
		flags |= FLAG_HAS_JSR;
		index = pc2signedshort(pc+1);
		ll = (new ILnode(ll)).jsr(branch(pc, index));
		bc_flag = BCinfo.FLAG_BRANCH_TARGET;
		pc += 2;
		bc.stack_delta = 0;
		break;

	    case opc_jsr_w:
		flags |= FLAG_HAS_JSR;
		index = pc2signedlong(pc+1);
		ll = (new ILnode(ll)).jsr(branch(pc, index));
		bc_flag = BCinfo.FLAG_BRANCH_TARGET;
		pc += 4;
		bc.stack_delta = 0;
		break;

	    case opc_ret:
		flags |= FLAG_HAS_JSR;
		index = pc2uchar(pc+1);
		pc++;
		bc.flag |= BCinfo.FLAG_CTRLFLOW_END;
		ll = (new ILnode(ll)).insn(ILnode.RET, TAG_CONST,(0), TAG_VI,(index), TAG_CONST,(8));
		ll = (new ILnode(ll)).nop();
		bc.stack_delta = 0;
		break;

	    case opc_tableswitch:
		// align word boundary
		int next_pc = ((pc+4)&~3);
		int def = pc2signedlong(next_pc);
		int low = pc2signedlong(next_pc+4);
		int high = pc2signedlong(next_pc+8);

		ll = (new ILnode(ll)).insn(ILnode.SUB, TAG_SI,(-1), TAG_SI,(-1), TAG_CONST,(low));
		ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-1), TAG_CONST,(high - low));
		ll = (new ILnode(ll)).branch(CC_TBLOB, branch(pc, def));
		ll = (new ILnode(ll)).insn(ILnode.SLL, TAG_SI,(-1), TAG_SI,(-1), TAG_CONST,(2));
		ll = (new ILnode(ll)).insn(ILnode.TBLSW, TAG_CONST,(0), TAG_SI,(-1), TAG_CONST,(0 /* pc */));
		next_pc += 12;
		for(int i = 0; i <= high - low; i++) {
		    ll = (new ILnode(ll)).branch(CC_TBLSW, branch(pc, pc2signedlong(next_pc)));
		    next_pc += 4;
		}
		pc = next_pc - 1;
		bc.stack_delta = -1;
		break;
	    case opc_lookupswitch:
		// align word boundary
		next_pc = ((pc+4)&~3);
		int skip = pc2signedlong(next_pc);
		int npairs = pc2signedlong(next_pc+4);

		while (--npairs >= 0) {
		    next_pc += 8;
		    ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-1), TAG_CONST,(pc2signedlong(next_pc)));
		    ll = (new ILnode(ll)).branch(CC_E, branch(pc, (pc2signedlong(next_pc+4))));
		}
		ll = (new ILnode(ll)).branch(CC_A, branch(pc, skip));
		bc.flag |= BCinfo.FLAG_CTRLFLOW_END;
		pc = next_pc + 8 - 1;
		bc.stack_delta = -1;
		break;
	    case opc_ireturn:
	    case opc_areturn:
		ll = (new ILnode(ll)).insn(ILnode.RETURN, TAG_PI, RETI, TAG_CONST, 0, TAG_SI, (-1));
		bc.flag |= BCinfo.FLAG_CTRLFLOW_END;
		bc.stack_delta = -1;
		break;
	    case opc_return:
		ll = (new ILnode(ll)).insn(ILnode.RETURN, TAG_CONST, 0, TAG_CONST, 0, TAG_CONST, 0);
		bc.flag |= BCinfo.FLAG_CTRLFLOW_END;
		bc.stack_delta = 0;
		break;
	    case opc_lreturn:
		ll = (new ILnode(ll)).insn(ILnode.RETURN, TAG_PL, RETL, TAG_SI, (-2), TAG_SI, (-1));
		bc.flag |= BCinfo.FLAG_CTRLFLOW_END;
		bc.stack_delta = -2;
		break;
	    case opc_freturn:
		ll = (new ILnode(ll)).insn(ILnode.RETURN, TAG_PF, RETF, TAG_CONST, 0, TAG_SF, (-1));
		bc.flag |= BCinfo.FLAG_CTRLFLOW_END;
		bc.stack_delta = -1;
		break;
	    case opc_dreturn:
		ll = (new ILnode(ll)).insn(ILnode.RETURN, TAG_PD, RETF, TAG_CONST, 0, TAG_SD, (-2));
		bc.flag |= BCinfo.FLAG_CTRLFLOW_END;
		bc.stack_delta = -2;
		break;
	    case opc_getstatic:
	    case opc_getstatic_quick:
	    case opc_getstatic2_quick:
		index = pc2ushort(pc+1);
		pc += 2;
		if (((opcode != opc_getstatic) || ConstantPoolTypeResolved(index)) &&
		    // check IncompatibleClassChangeError
		    (offset = ConstantPoolFieldAddress(index)) != -1) {
		    switch (ConstantPoolFieldDescriptor(index)) {
		    case SIGC_BOOLEAN:
		    case SIGC_BYTE:
		    case SIGC_CHAR:
		    case SIGC_SHORT:
		    case SIGC_INT:
		    case SIGC_CLASS:
		    case SIGC_ARRAY:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(0), TAG_CONST,(0), TAG_CONST,(offset));
			bc.stack_delta = 1;
			break;
		    case SIGC_FLOAT:
			ll = (new ILnode(ll)).insn(ILnode.FLD, TAG_SF,(0), TAG_CONST,(0), TAG_CONST,(offset));
			bc.stack_delta = 1;
			break;
		    case SIGC_DOUBLE:
			ll = (new ILnode(ll)).insn(ILnode.DLD, TAG_SD,(0), TAG_CONST,(0), TAG_CONST,(offset));
			bc.stack_delta = 2;
			break;
		    case SIGC_LONG:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(0), TAG_CONST,(0), TAG_CONST,(offset));
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(1), TAG_CONST,(0), TAG_CONST,(offset + 4));
			bc.stack_delta = 2;
			break;
		    default:
			/* Indicate an error. */
			throw new CompilerError("invalid signature in getstatic");
		    }
		    break;
		} else {
		    flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		    switch(ConstantPoolFieldDescriptor(index)) {
		    case SIGC_BOOLEAN:
		    case SIGC_BYTE:
		    case SIGC_CHAR:
		    case SIGC_SHORT:
		    case SIGC_INT:
		    case SIGC_CLASS:
		    case SIGC_ARRAY:
			ll = (new ILnode(ll)).call(TAG_SI,(0), RT_getstatic, index);
			ll = (new ILnode(ll)).move(TAG_SI,(0), TAG_PI,(RETI));
			bc.stack_delta = 1;
			break;
		    case SIGC_FLOAT:
			ll = (new ILnode(ll)).call(TAG_SF,(0), RT_getstatic, index);
			ll = (new ILnode(ll)).move(TAG_SF,(0), TAG_PF,(RETF));
			bc.stack_delta = 1;
			break;
		    case SIGC_DOUBLE:
			ll = (new ILnode(ll)).call(TAG_SD,(0), RT_getstatic, index);
			ll = (new ILnode(ll)).move(TAG_SD,(0), TAG_PD,(RETF));
			bc.stack_delta = 2;
			break;
		    case SIGC_LONG:
			ll = (new ILnode(ll)).call(TAG_SL,(0), RT_getstatic, index);
			ll = (new ILnode(ll)).move(TAG_SI,(1), TAG_PI,(RETL));
			ll = (new ILnode(ll)).move(TAG_SI,(0), TAG_PI,(RETI));
			bc.stack_delta = 2;
			break;
		    default:
			/* Indicate an error. */
			throw new CompilerError("invalid signature");
		    }
		    break;
		}
	    case opc_putstatic:
	    case opc_putstatic_quick:
	    case opc_putstatic2_quick:
		index = pc2ushort(pc+1);
		pc += 2;
		if (((opcode != opc_putstatic) || ConstantPoolTypeResolved(index)) &&
		    // check IncompatibleClassChangeError
		    (offset = ConstantPoolFieldAddress(index)) != -1) {
		    switch (ConstantPoolFieldDescriptor(index)) {
		    case SIGC_BOOLEAN:
		    case SIGC_BYTE:
		    case SIGC_CHAR:
		    case SIGC_SHORT:
		    case SIGC_INT:
		    case SIGC_CLASS:
		    case SIGC_ARRAY:
			ll = (new ILnode(ll)).insn(ILnode.ST, TAG_SI,(-1), TAG_CONST,(0), TAG_CONST,(offset));
			bc.stack_delta = -1;
			break;
		    case SIGC_FLOAT:
			ll = (new ILnode(ll)).insn(ILnode.FST, TAG_SF,(-1), TAG_CONST,(0), TAG_CONST,(offset));
			bc.stack_delta = -1;
			break;
		    case SIGC_DOUBLE:
			ll = (new ILnode(ll)).insn(ILnode.DST, TAG_SD,(-2), TAG_CONST,(0), TAG_CONST,(offset));
			bc.stack_delta = -2;
			break;
		    case SIGC_LONG:
			ll = (new ILnode(ll)).insn(ILnode.ST, TAG_SI,(-2), TAG_CONST,(0), TAG_CONST,(offset));
			ll = (new ILnode(ll)).insn(ILnode.ST, TAG_SI,(-1), TAG_CONST,(0), TAG_CONST,(offset + 4));
			bc.stack_delta = -2;
			break;
		    default:
			/* Indicate an error. */
			throw new CompilerError("invalid signature in putstatic");
		    }
		    break;
		} else {
		    flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		    ll = (new ILnode(ll)).call(TAG_SI,(0), RT_resolveStaticField, index);
		    switch(ConstantPoolFieldDescriptor(index)) {
		    case SIGC_BOOLEAN:
		    case SIGC_BYTE:
		    case SIGC_CHAR:
		    case SIGC_SHORT:
		    case SIGC_INT:
		    case SIGC_CLASS:
		    case SIGC_ARRAY:
			ll = (new ILnode(ll)).insn(ILnode.ST, TAG_SI,(-1), TAG_PI,(RETI), TAG_CONST,(0));
			bc.stack_delta = -1;
			break;
		    case SIGC_FLOAT:
			ll = (new ILnode(ll)).insn(ILnode.FST, TAG_SF,(-1), TAG_PI,(RETI), TAG_CONST,(0));
			bc.stack_delta = -1;
			break;
		    case SIGC_DOUBLE:
			ll = (new ILnode(ll)).insn(ILnode.DST, TAG_SD,(-2), TAG_PI,(RETI), TAG_CONST,(0));
			bc.stack_delta = -2;
			break;
		    case SIGC_LONG:
			ll = (new ILnode(ll)).insn(ILnode.ST, TAG_SI,(-2), TAG_PI,(RETI), TAG_CONST,(0));
			ll = (new ILnode(ll)).insn(ILnode.ST, TAG_SI,(-1), TAG_PI,(RETI), TAG_CONST,(4));
			bc.stack_delta = -2;
			break;
		    default:
			/* Indicate an error. */
			throw new CompilerError("invalid signature");
		    }
		    break;
		}
	    case opc_getfield:
	    case opc_getfield_quick_w:
		index = pc2ushort(pc+1);
		pc += 2;
		flags |= FLAG_HAS_EXCEPTION;
		if (((opcode == opc_getfield_quick_w) || ConstantPoolTypeResolved(index)) &&
		    // check IncompatibleClassChangeError
		    (offset = ConstantPoolFieldOffset(index)) >= 0) {
		    switch (ConstantPoolFieldDescriptor(index)) {
		    case SIGC_BOOLEAN:
		    case SIGC_BYTE:
		    case SIGC_CHAR:
		    case SIGC_SHORT:
		    case SIGC_INT:
		    case SIGC_CLASS:
		    case SIGC_ARRAY:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-1), TAG_SI,(-1), TAG_CONST,(0));
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-1), TAG_SI,(-1), TAG_CONST,(offset));
			bc.stack_delta = 0;
			break;
		    case SIGC_FLOAT:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-1), TAG_SI,(-1), TAG_CONST,(0));
			ll = (new ILnode(ll)).insn(ILnode.FLD, TAG_SF,(-1), TAG_SI,(-1), TAG_CONST,(offset));
			bc.stack_delta = 0;
			break;
		    case SIGC_DOUBLE:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(0), TAG_SI,(-1), TAG_CONST,(0));
			ll = (new ILnode(ll)).insn(ILnode.DLD, TAG_SD,(-1), TAG_SI,(0), TAG_CONST,(offset));
			bc.stack_delta = 1;
			break;
		    case SIGC_LONG:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(0), TAG_SI,(-1), TAG_CONST,(0));
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-1), TAG_SI,(0), TAG_CONST,(offset));
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(0), TAG_SI,(0), TAG_CONST,(offset + 4));
			bc.stack_delta = 1;
			break;
		    default:
			/* Indicate an error. */
			throw new CompilerError("invalid signature in getfield");
		    }
		    break;
		} else {
		    flags |= FLAG_HAS_CALL;
		    switch(ConstantPoolFieldDescriptor(index)) {
		    case SIGC_BOOLEAN:
		    case SIGC_BYTE:
		    case SIGC_CHAR:
		    case SIGC_SHORT:
		    case SIGC_INT:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-1), TAG_SI,(-1), TAG_CONST,(0));
			ll = (new ILnode(ll)).call(TAG_SI,(0), RT_resolveField, index);
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-1), TAG_SI,(-1), TAG_PI,(RETI));
			bc.stack_delta = 0;
			break;
		    case SIGC_CLASS:
		    case SIGC_ARRAY:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-1), TAG_SI,(-1), TAG_CONST,(0));
			ll = (new ILnode(ll)).call(TAG_SI,(0), RT_resolveField, index);
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-1), TAG_SI,(-1), TAG_PI,(RETI));
			bc.stack_delta = 0;
			break;
		    case SIGC_FLOAT:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-1), TAG_SI,(-1), TAG_CONST,(0));
			ll = (new ILnode(ll)).call(TAG_SI,(0), RT_resolveField, index);
			ll = (new ILnode(ll)).insn(ILnode.FLD, TAG_SF,(-1), TAG_SI,(-1), TAG_PI,(RETI));
			bc.stack_delta = 0;
			break;
		    case SIGC_DOUBLE:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-1), TAG_SI,(-1), TAG_CONST,(0));
			ll = (new ILnode(ll)).call(TAG_SI,(0), RT_resolveField, index);
			ll = (new ILnode(ll)).insn(ILnode.DLD, TAG_SD,(-1), TAG_SI,(-1), TAG_PI,(RETI));
			bc.stack_delta = 1;
			break;
		    case SIGC_LONG:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(0), TAG_SI,(-1), TAG_CONST,(0));
			ll = (new ILnode(ll)).call(TAG_SI,(0), RT_resolveField, index);
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-1), TAG_SI,(0), TAG_PI,(RETI));
			ll = (new ILnode(ll)).insn(ILnode.ADD, TAG_PI,(RETI), TAG_PI,(RETI), TAG_CONST,(4));
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(0), TAG_SI,(0), TAG_PI,(RETI));
			bc.stack_delta = 1;
			break;
		    default:
			/* Indicate an error. */
			throw new CompilerError("invalid signature");
		    }
		    break;
		}
	    case opc_putfield:
	    case opc_putfield_quick_w:
		index = pc2ushort(pc+1);
		pc += 2;
		flags |= FLAG_HAS_EXCEPTION;
		if (((opcode == opc_putfield_quick_w) || ConstantPoolTypeResolved(index)) &&
		    // check IncompatibleClassChangeError
		    (offset = ConstantPoolFieldOffset(index)) >= 0) {
		    switch (ConstantPoolFieldDescriptor(index)) {
		    case SIGC_BOOLEAN:
		    case SIGC_BYTE:
		    case SIGC_CHAR:
		    case SIGC_SHORT:
		    case SIGC_INT:
		    case SIGC_CLASS:
		    case SIGC_ARRAY:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-2), TAG_SI,(-2), TAG_CONST,(0));
			ll = (new ILnode(ll)).insn(ILnode.ST, TAG_SI,(-1), TAG_SI,(-2), TAG_CONST,(offset));
			bc.stack_delta = -2;
			break;
		    case SIGC_FLOAT:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-2), TAG_SI,(-2), TAG_CONST,(0));
			ll = (new ILnode(ll)).insn(ILnode.FST, TAG_SF,(-1), TAG_SI,(-2), TAG_CONST,(offset));
			bc.stack_delta = -2;
			break;
		    case SIGC_DOUBLE:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-3), TAG_SI,(-3), TAG_CONST,(0));
			ll = (new ILnode(ll)).insn(ILnode.DST, TAG_SD,(-2), TAG_SI,(-3), TAG_CONST,(offset));
			bc.stack_delta = -3;
			break;
		    case SIGC_LONG:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-3), TAG_SI,(-3), TAG_CONST,(0));
			ll = (new ILnode(ll)).insn(ILnode.ST, TAG_SI,(-2), TAG_SI,(-3), TAG_CONST,(offset));
			ll = (new ILnode(ll)).insn(ILnode.ST, TAG_SI,(-1), TAG_SI,(-3), TAG_CONST,(offset + 4));
			bc.stack_delta = -3;
			break;
		    default:
			/* Indicate an error. */
			throw new CompilerError("invalid signature in putfield");
		    }
		    break;
		} else {
		    flags |= FLAG_HAS_CALL;
		    switch(ConstantPoolFieldDescriptor(index)) {
		    case SIGC_BOOLEAN:
		    case SIGC_BYTE:
		    case SIGC_CHAR:
		    case SIGC_SHORT:
		    case SIGC_INT:
		    case SIGC_CLASS:
		    case SIGC_ARRAY:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-2), TAG_SI,(-2), TAG_CONST,(0));
			ll = (new ILnode(ll)).call(TAG_SI,(0), RT_resolveField, index);
			ll = (new ILnode(ll)).insn(ILnode.ST, TAG_SI,(-1), TAG_SI,(-2), TAG_PI,(RETI));
			bc.stack_delta = -2;
			break;
		    case SIGC_FLOAT:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-2), TAG_SI,(-2), TAG_CONST,(0));
			ll = (new ILnode(ll)).call(TAG_SI,(0), RT_resolveField, index);
			ll = (new ILnode(ll)).insn(ILnode.FST, TAG_SF,(-1), TAG_SI,(-2), TAG_PI,(RETI));
			bc.stack_delta = -2;
			break;
		    case SIGC_DOUBLE:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-3), TAG_SI,(-3), TAG_CONST,(0));
			ll = (new ILnode(ll)).call(TAG_SI,(0), RT_resolveField, index);
			ll = (new ILnode(ll)).insn(ILnode.DST, TAG_SD,(-2), TAG_SI,(-3), TAG_PI,(RETI));
			bc.stack_delta = -3;
			break;
		    case SIGC_LONG:
			ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-3), TAG_SI,(-3), TAG_CONST,(0));
			ll = (new ILnode(ll)).call(TAG_SI,(0), RT_resolveField, index);
			ll = (new ILnode(ll)).insn(ILnode.ST, TAG_SI,(-2), TAG_SI,(-3), TAG_PI,(RETI));
			ll = (new ILnode(ll)).insn(ILnode.ADD, TAG_PI,(RETI), TAG_PI,(RETI), TAG_CONST,(4));
			ll = (new ILnode(ll)).insn(ILnode.ST, TAG_SI,(-1), TAG_SI,(-3), TAG_PI,(RETI));
			bc.stack_delta = -3;
			break;
		    default:
			/* Indicate an error. */
			throw new CompilerError("invalid signature");
		    }
		    break;
		}
	    case opc_invokevirtual:
	    case opc_invokevirtual_quick_w:
		flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		callMethod(bc, pc, 0, RT_invokevirtual_resolve, false, false);
		pc += 2;
		break;
	    case opc_invokespecial:
	    case opc_invokenonvirtual_quick:
		flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		callMethod(bc, pc, RT_invokespecial, RT_invokespecial_resolve, false, false);
		pc += 2;
		break;
	    case opc_invokesuper_quick:
		flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		callMethod(bc, pc, RT_invokesuper_quick, 0, false, false);
		pc += 2;
		break;
	    case opc_invokestatic:
		flags |= FLAG_HAS_EXCEPTION;
	    case opc_invokestatic_quick:
		flags |= FLAG_HAS_CALL;
		callMethod(bc, pc, RT_invokestatic, RT_invokestatic_resolve, true, false);
		pc += 2;
		break;
	    case opc_invokeinterface:
		flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		callMethod(bc, pc, 0, RT_invokeinterface, false, true);
		pc += 4;
		break;
	    case opc_invokeinterface_quick:
		flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		callMethod(bc, pc, 0, RT_invokeinterface_quick, false, false);
		pc += 2;
		break;

	    case opc_new:
		flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		index = pc2ushort(pc+1);
		pc += 2;
		ll = (new ILnode(ll)).call(TAG_SI,(0), RT_new, index);
		ll = (new ILnode(ll)).move(TAG_SI,(0), TAG_PI,(RETI));
		bc.stack_delta = 1;
		break;

	    case opc_new_quick:
		flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		index = pc2ushort(pc+1);
		pc += 2;
		ll = (new ILnode(ll)).move(TAG_PI,(ARG0), TAG_CONST,(ConstantPoolValue(index)));
		ll = (new ILnode(ll)).call(TAG_SI,(0), RT_new_quick);
		ll = (new ILnode(ll)).move(TAG_SI,(0), TAG_PI,(RETI));
		bc.stack_delta = 1;
		break;

	    case opc_newarray:
		flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		ll = (new ILnode(ll)).move(TAG_PI,(ARG1), TAG_SI,(-1));
		ll = (new ILnode(ll)).move(TAG_PI,(ARG0), TAG_CONST,pc2uchar(pc+1));
		ll = (new ILnode(ll)).call(TAG_SI,(-1), RT_newarray);
		ll = (new ILnode(ll)).move(TAG_SI,(-1), TAG_PI,(RETI));
		pc++;
		bc.stack_delta = 0;
		break;

	    case opc_anewarray:
	    case opc_anewarray_quick:
		flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		index = pc2ushort(pc+1);
		pc += 2;
		ll = (new ILnode(ll)).move(TAG_PI,(ARG1), TAG_SI,(-1));
		if (ConstantPoolTypeResolved(index)) {
		    ll = (new ILnode(ll)).move(TAG_PI,(ARG0), TAG_CONST,(ConstantPoolValue(index)));
		    ll = (new ILnode(ll)).call(TAG_SI,(-1), RT_anewarray_quick);
		} else {
		    ll = (new ILnode(ll)).call(TAG_SI,(-1), RT_anewarray, index);
		}
		ll = (new ILnode(ll)).move(TAG_SI,(-1), TAG_PI,(RETI));
		bc.stack_delta = 0;
		break;

	    case opc_arraylength:
		flags |= FLAG_HAS_EXCEPTION;
		ll = (new ILnode(ll)).insn(ILnode.LD, TAG_SI,(-1), TAG_SI,(-1), TAG_CONST,(METHOD_OFFSET));
		ll = (new ILnode(ll)).insn(ILnode.SRL, TAG_SI,(-1), TAG_SI,(-1), TAG_CONST,(METHOD_FLAG_BITS));
		bc.stack_delta = 0;
		break;

	    case opc_athrow:
		flags |= FLAG_HAS_THROW; /* FLAG_HAS_CALL */
		bc.flag |= BCinfo.FLAG_CTRLFLOW_END;
		ll = (new ILnode(ll)).move(TAG_PI,(ARG0), TAG_SI,(-1));
		ll = (new ILnode(ll)).call(TAG_CONST,(-1), RT_athrow);
		bc.stack_delta = -1;
		break;

	    case opc_checkcast:
	    case opc_checkcast_quick:
		flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		index = pc2ushort(pc+1);
		pc += 2;
		ll = (new ILnode(ll)).move(TAG_PI,(ARG1), TAG_SI | TAG_REF,(-1));
		if (ConstantPoolTypeResolved(index)) {
		    ll = (new ILnode(ll)).move(TAG_PI,(ARG0), TAG_CONST,(ConstantPoolValue(index)));
		    ll = (new ILnode(ll)).call(TAG_CONST,(0), RT_checkcast_quick);
		} else {
		    ll = (new ILnode(ll)).call(TAG_CONST,(0), RT_checkcast, index);
		}
		bc.stack_delta = 0;
		break;

	    case opc_instanceof:
	    case opc_instanceof_quick:
		flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		index = pc2ushort(pc+1);
		pc += 2;
		ll = (new ILnode(ll)).move(TAG_PI,(ARG1), TAG_SI,(-1));
		if (ConstantPoolTypeResolved(index)) {
		    ll = (new ILnode(ll)).move(TAG_PI,(ARG0), TAG_CONST,(ConstantPoolValue(index)));
		    ll = (new ILnode(ll)).call(TAG_SI,(-1), RT_instanceof_quick);
		} else {
		    ll = (new ILnode(ll)).call(TAG_SI,(-1), RT_instanceof, index);
		}
		ll = (new ILnode(ll)).move(TAG_SI,(-1), TAG_PI,(RETI));
		bc.stack_delta = 0;
		break;

	    case opc_monitorenter:
		flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		ll = (new ILnode(ll)).move(TAG_PI,(ARG0), TAG_SI,(-1));
		ll = (new ILnode(ll)).call(TAG_CONST,(-1), RT_monitorEnter);
		bc.stack_delta = -1;
		break;
	    case opc_monitorexit:
		flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		ll = (new ILnode(ll)).move(TAG_PI,(ARG0), TAG_SI,(-1));
		ll = (new ILnode(ll)).call(TAG_CONST,(-1), RT_monitorExit);
		bc.stack_delta = -1;
		break;
	    case opc_wide: 
		opcode = pc2uchar(pc+1);
		index = pc2ushort(pc+2);
		pc += 3;

		switch(opcode) {
		case opc_iload:
		case opc_aload:
		    ll = (new ILnode(ll)).move(TAG_SI, 0, TAG_VI, index);
		    bc.stack_delta = 1;
		    break;
		case opc_fload: 
		    ll = (new ILnode(ll)).move(TAG_SF, 0, TAG_VF, index);
		    bc.stack_delta = 1;
		    break;
		case opc_lload:
		    ll = (new ILnode(ll)).move(TAG_SI, 0, TAG_VI, index);
		    ll = (new ILnode(ll)).move(TAG_SI, 1, TAG_VI, index+1);
		    bc.stack_delta = 2;
		    break;
		case opc_dload: 
		    ll = (new ILnode(ll)).move(TAG_SD, 0, TAG_VD, index);
		    bc.stack_delta = 2;
		    break;
		case opc_istore:
		case opc_astore:
		    ll = (new ILnode(ll)).move(TAG_VI,(index), TAG_SI,(-1));
		    bc.stack_delta = -1;
		    break;
		case opc_fstore:
		    ll = (new ILnode(ll)).move(TAG_VF,(index), TAG_SF,(-1));
		    bc.stack_delta = -1;
		    break;
		case opc_lstore:
		    ll = (new ILnode(ll)).move(TAG_VI,(index), TAG_SI,(-2));
		    ll = (new ILnode(ll)).move(TAG_VI,(index+1), TAG_SI,(-1));
		    bc.stack_delta = -2;
		    break;
		case opc_dstore: 
		    ll = (new ILnode(ll)).move(TAG_VD,(index), TAG_SD,(-2));
		    bc.stack_delta = -2;
		    break;
		case opc_iinc:
		    offset = pc2signedshort(pc+1);
		    pc += 2;
		    ll = (new ILnode(ll)).insn(ILnode.ADD, TAG_VI,(index), TAG_VI,(index), TAG_CONST,(offset));
		    bc.stack_delta = 0;
		    break;
		case opc_ret:
		    flags |= FLAG_HAS_JSR;
		    bc.flag |= BCinfo.FLAG_CTRLFLOW_END;
		    ll = (new ILnode(ll)).insn(ILnode.RET, TAG_CONST,(0), TAG_VI,(index), TAG_CONST,(8));
		    ll = (new ILnode(ll)).nop();
		    bc.stack_delta = 0;
		    break;
		default:
		    /* InternalError, undefined opcode */
		    throw new CompilerError("opc_wide unknown opcode:" + opcode);
		}
		break;
	    case opc_multianewarray:
	    case opc_multianewarray_quick:
		flags |= FLAG_HAS_EXCEPTION | FLAG_HAS_CALL;
		{
		    int dimensions = pc2uchar(pc+3);

		    bc.stack_delta = 1 - dimensions;
		    index = pc2ushort(pc+1);
		    pc += 3;
		    for (int i = dimensions - 1; i >= 0; i--) {
			ll = (new ILnode(ll)).move(TAG_PI, (ARG2 + i), TAG_SI, i - dimensions);
		    }
		    ll = (new ILnode(ll)).move(TAG_PI,(ARG1), TAG_CONST,(dimensions));
		    if (ConstantPoolTypeResolved(index)) {
			ll = (new ILnode(ll)).move(TAG_PI,(ARG0), TAG_CONST,(ConstantPoolValue(index)));
			ll = (new ILnode(ll)).call(TAG_SI,(-dimensions), RT_multianewarray_quick);
		    } else {
			ll = (new ILnode(ll)).call(TAG_SI,(-dimensions), RT_multianewarray, index);
		    }
		    ll = (new ILnode(ll)).move(TAG_SI,(-dimensions), TAG_PI,(RETI));
		    break;
		}
	    case opc_ifnull:
		ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-1), TAG_CONST,(0));
		ll = (new ILnode(ll)).branch(CC_E, branch(pc, pc2signedshort(pc+1)));
		bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		pc += 2;
		bc.stack_delta = -1;
		break;
	    case opc_ifnonnull:
		ll = (new ILnode(ll)).insn(ILnode.CMP, TAG_CONST,(0), TAG_SI,(-1), TAG_CONST,(0));
		ll = (new ILnode(ll)).branch(CC_NE, branch(pc, pc2signedshort(pc+1)));
		bc_flag = BCinfo.FLAG_BRANCH_NOT_TAKEN;
		pc += 2;
		bc.stack_delta = -1;
		break;

	    case opc_invokeignored_quick:
		int args_size = pc2uchar(pc+1);
		bc.stack_delta = -args_size;
		if (pc2uchar(pc+2) != 0) {	/* check this */
		    flags |= FLAG_HAS_EXCEPTION;
		    /* Dummy load to cause SEGV */
		    ll = (new ILnode(ll)).insn(ILnode.LD, TAG_CONST,(0), TAG_SI,(-args_size), TAG_CONST,(0));
		}
		pc += 2;
		break;

	    case opc_nonnull_quick:
		/* Dummy load to cause SEGV */
		flags |= FLAG_HAS_EXCEPTION;
		ll = (new ILnode(ll)).insn(ILnode.LD, TAG_CONST,(0), TAG_SI,(-1), TAG_CONST,(0));
		bc.stack_delta = -1;
		break;

	    default:
		throw new CompilerError("detect unknown bytecode " + opcode);
	    }
	}

	doInlineFixup();
	
    }
}
