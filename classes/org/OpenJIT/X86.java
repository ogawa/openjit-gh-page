//
// Copyright (C) 1996 1997 1998 1999 by FUJITSU LABORATRIES LTD.
//
// $Revision: 1.93 $
// $Date: 1999/10/25 03:24:35 $
// $Author: kouya $
//

package org.OpenJIT;

class LinkageInfo {

  LinkageInfo() {
    flag = 0;
    next = null;
  }

  LinkageInfo(int from_pc, int from_npc, int target_pc, int target_npc) {
    this();
    this.from_pc = from_pc;
    this.from_npc = from_npc;
    this.target_pc = target_pc;
    this.target_npc = target_npc;
  }

  public String toString() {
    return "LINK ["+from_pc+" -> "+target_pc+"] nat:(0x"+Integer.toHexString(from_npc)+" -> 0x"+Integer.toHexString(target_npc)+"), flag 0x"+Integer.toHexString(flag);
  }

  static final int JUMP = 0x1;      // Compressable jump
  static final int JCC  = 0x2;      // Compressable jcc
  static final int SHORTJMP = 0x4;       // Short jump (one-byte offset)
  static final int OFFSET_ABSOLUTE = 0x8; // Target should be absolute
  static final int TARGET_RELOCATED = 0x10; // Target already relocated

  // If this is a JCC, set flag |= (op << JCC_OP_SHIFT)
  static final int JCC_OP_SHIFT = 8;
  static final int JCC_OP_MASK = 0xff00;
  
  int from_pc;                  // linkage bytecode PC (-1 if prologue)
  int from_npc;			// linkage native PC
  int target_pc;		// target bytecode PC (-1 if none)
  int target_npc;               // target native PC (-1 if unresolved)
  
  int flag;
  LinkageInfo next;
}

abstract class X86code extends OptimizeRTL {
    boolean save_eflags;

    final static byte R_EAX = 0;
    final static byte R_ECX = 1;
    final static byte R_EDX = 2;
    final static byte R_EBX = 3;
    final static byte R_ESP = 4;
    final static byte R_EBP = 5;
    final static byte R_ESI = 6;
    final static byte R_EDI = 7;

    final static byte MODRM_M0 = 0x00; // offset: +0
    final static byte MODRM_MS = 0x40; // offset: +d8
    final static byte MODRM_ML = (byte)0x80; // offset: +d32
    final static byte MODRM_R  = (byte)0xc0; // register

    final static int MODRM_SCALE_2 = 0x40;
    final static int MODRM_SCALE_4 = 0x80;
    final static int MODRM_SCALE_8 = 0xc0;
    /* MODRM_SCALE is set to where bits in il.rval(il.lval) */
    final static int MODRM_SCALE_BITS = 16;
    final static int MODRM_SCALE_MASK = 0xffff;

    final static int I_MOVIR	= 0xb8;
    final static int I_MOVIM	= 0xc6;
    final static int I_MOV	= 0x89;
    final static int I_MOVB	= 0x88;

    final static int I_MOVSB	= 0xbe;
    final static int I_MOVSH	= 0xbf;
    final static int I_MOVZH	= 0xb7;

    final static int I_ADD	= 0x01;
    final static int I_ADC	= 0x11;
    final static int I_SUB	= 0x29;
    final static int I_SBB	= 0x19;

    final static int I_CMP	= 0x39;
    final static int I_TEST	= 0x85;

    final static int I_AND	= 0x21;
    final static int I_OR	= 0x09;
    final static int I_XOR	= 0x31;

    final static int I_SHL	= 0x4 << 3;
    final static int I_SHR	= 0x5 << 3;
    final static int I_SAR	= 0x7 << 3;

    final static int I_JMP_8	= 0xeb;
    final static int I_JMP	= 0xe9;

    final static int I_JCC	= 0x70;
    final static int I_JCC1	= 0x0f;
    final static int I_JCC2	= 0x80;

    final static int I_JO	= 0x00;
    final static int I_JNO	= 0x01;
    final static int I_JB	= 0x02;
    final static int I_JNB	= 0x03;
    final static int I_JE	= 0x04;
    final static int I_JNE	= 0x05;
    final static int I_JBE	= 0x06;
    final static int I_JNBE	= 0x07;
    final static int I_JS	= 0x08;
    final static int I_JNS	= 0x09;
    final static int I_JP	= 0x0a;
    final static int I_JNP	= 0x0b;
    final static int I_JL	= 0x0c;
    final static int I_JNL	= 0x0d;
    final static int I_JLE	= 0x0e;
    final static int I_JNLE	= 0x0f;

    final static int I_CALL	= 0xe8;

    final static int I_PUSH	= 0x50;
    final static int I_LEAVE	= 0xc9;
    final static int I_RET	= 0xc3;

    final static int I_NOP	= 0x90;
    final static int I_INT3	= 0xcc;
    final static int I_INT	= 0xcd;

    final static int I_FLOAT_S  = 0xd9;	// short precision
    final static int I_FLOAT_L  = 0xdd; // long precision

    final void genImm16(int i) {
	genCode(i & 0xff);
	i >>= 8;
	genCode(i & 0xff);
    }

    final void genImm32(int i) {
	genCode(i & 0xff);
	i >>= 8;
	genCode(i & 0xff);
	i >>= 8;
	genCode(i & 0xff);
	i >>= 8;
	genCode(i & 0xff);
    }

    final void genRR(int op, int rs, int rd) {
	genCode(op);
	genCode(MODRM_R | (rs << 3) | rd);
    }

    final void genModBaseOff(int op, int base, int offset) {
	if (offset == 0) {
	    genCode(MODRM_M0 | op | base);
	} else if (offset >= -128 && offset <= 127) {
	    genCode(MODRM_MS | op | base);
	    genCode(offset);
	} else {
	    genCode(MODRM_ML | op | base);
	    genImm32(offset);
	}
    }

    // scale must be one of MODRM_SCALE_[248]
    final void genModBaseIndex(int op, int base, int indx, int scale) {
	genCode(MODRM_M0 | 0x04 | op);
	genCode(scale | (indx << 3) | base);
    }

    final void genMod(int op, int offset, int base, int indx, int scale) {
	if (offset == 0) {
	    genCode(MODRM_M0 | 0x04 | op);
	    genCode(scale | (indx << 3) | base);
	} else if (offset >= -128 && offset <= 127) {
	    genCode(MODRM_MS | 0x04 | op);
	    genCode(scale | (indx << 3) | base);
	    genCode(offset);
	} else {
	    genCode(MODRM_ML | 0x04 | op);
	    genCode(scale | (indx << 3) | base);
	    genImm32(offset);
	}
    }

    final void genRM(int op, int rs, int rd, int offset) {
	genCode(op);
	genModBaseOff((rs << 3), rd, offset);
    }

    final void genMR(int op, int rs, int offset, int rd) {
	genCode(op | 0x2); 
	genModBaseOff((rd << 3), rs, offset);
    }

    final void genIR(int op, int imm, int rd) {
	op &= ~0x01;
	if (imm >= -128 && imm <= 127) {
	    genCode(0x83);
	    genCode(op | MODRM_R | rd);
	    genCode(imm);
	} else {
	    genCode(0x81);
	    genCode(op | MODRM_R | rd);
	    genImm32(imm);
	}
    }

    final void genIM(int op, int imm, int base, int offset) {
	op &= ~0x01;
	if (imm >= -128 && imm <= 127) {
	    genCode(0x83);
	    genModBaseOff(op, base, offset);
	    genCode(imm);
	} else {
	    genCode(0x81);
	    genModBaseOff(op, base, offset);
	    genImm32(imm);
	}
    }

    final void genPUSH32(int imm) {
	genCode(0x68);
	genImm32(imm);
    }

    final void genPUSHI(int imm) {
	if (imm >= -128 && imm <= 127) {
	    genCode(0x6a);
	    genCode(imm);
	} else {
	    genCode(0x68);
	    genImm32(imm);
	}
    }

    final void genPUSHR(int reg) {
	genCode(0x50 | reg);
    }

    final void genPUSHM(int base, int offset) {
	genCode(0xff);
	genModBaseOff(0x30, base, offset);
    }

    final void genPOPM(int base, int offset) {
	genCode(0x8f);
	genModBaseOff(0x00, base, offset);
    }

}

abstract class X86reg extends X86code {

    final static int INVALID_REG = -1;

    final static int REG_ANY    = 0xcf; // except R_ESP,R_EBP
    final static int REG_LH     = 0x0f; // R_EAX,R_ECX,R_EDX,R_EBX
    final static int REG_NOT_A  = 0xce; // except R_ESP,R_EBP,R_EAX
    final static int REG_NOT_AD = 0xca; // except R_ESP,R_EBP,R_EDX,R_EAX
    final static int REG_NOT_C  = 0xcd; // except R_ESP,R_EBP,R_EDX,R_EAX

    private int regalloc_usevec;
    private int regalloc_lockvec;
    private int regalloc_modvec;
    private int regalloc_offset[];

    final int base_offset(int regno) {
	if (regno < args_size) {
	    return regno * 4 + 12;
	} else {
	    int offset = (regno - nlocals - maxstack - 1) * 4;
	    return offset;
	}
    }

    final void regAlloc() {
	regalloc_offset = new int[8];
    }

    final void regAssign(int regno, int offset) {
	int reg;
	for (reg = 0; reg < 8; reg++) {
	    if (regalloc_offset[reg] == offset) {
		regalloc_offset[reg] = 0;
		if (regno != reg) {
		    int vec = (1<<reg);
		    if ((regalloc_modvec & vec) != 0) {
			regalloc_modvec &= ~vec;
		    }
		}
	    }
	}
	regalloc_offset[regno] = offset;
	// System.err.println("assign:" + regno + " " + offset);
    }

    final void regSave(int reg) {
	int vec = (1<<reg);
	if ((regalloc_modvec & vec) != 0) {
	    //	    if (true) throw new CompilerError("aho");
	    int offset = regalloc_offset[reg];
	    if (offset == 0) {
		throw new CompilerError("something wrong:" + reg);
	    }
	    genRM(I_MOV, reg, R_EBP, offset);
	    regalloc_modvec &= ~vec;
	    if ((debug & DEBUG_NATIVE) != 0)
		System.err.println("spill:" + reg + " " + offset);
	}
    }

    final void regSave() {
	int reg = 0;
	while(regalloc_modvec != 0) {
	    if ((regalloc_modvec & 1) != 0) {
		int offset = regalloc_offset[reg];
		if (offset == 0) {
		    throw new CompilerError("something wrong:" + reg);
		}
		genRM(I_MOV, reg, R_EBP, offset);
		if ((debug & DEBUG_NATIVE) != 0)
		    System.err.println("save all:" + reg);
	    }
	    ++reg;
	    regalloc_modvec >>>= 1;
	}
    }


    final int regSearch(int offset) {
	int reg;
	for (reg = 0; reg < 8; reg++) {
	    if (regalloc_offset[reg] == offset) {
//  		System.err.println("reg:" + reg + " offset:" + offset);
  		return reg;
	    }
	}
	return INVALID_REG;
    }

    final void regInvalidate(int reg) {
	regalloc_offset[reg] = 0; // 0 offset is never used.
    }

    final void regInvalidate() {
	int reg;
	for (reg = 0; reg < 8; reg++)
	    regalloc_offset[reg] = 0;
    }

    final void regDiscard() {
	regalloc_modvec = 0;
    }

    private void discard(int val) {
	int reg;
	int offset = base_offset(val);
	for (reg = 0; reg < 8; reg++) {
	    if (offset == regalloc_offset[reg]) {
		if ((debug & DEBUG_NATIVE) != 0)
		    System.err.println("discard:" + reg + " %si" + 
				       (nlocals + maxstack + 1 + (offset>>2)));
		regalloc_offset[reg] = 0;
		regalloc_modvec &= ~(1<<reg);
	    }
	}
    }

    final void freeRegs(ILnode il) {
	regalloc_usevec = 0;
	regalloc_lockvec = 0;

	if (il.isSTORE() && (il.dtype() & TAG_FREE) != 0)
	    discard(il.dval & 0x1ffff);
	if ((il.ltype() & TAG_FREE) != 0)
	    discard(il.lval & 0x1ffff);
	if ((il.rtype() & TAG_FREE) != 0)
	    discard(il.rval & 0x1ffff);
    }

    final void freeTmpReg(int reg) {
	int mask = ~(1<<reg);
	regalloc_usevec &= mask;
	regalloc_lockvec &= mask;
	regalloc_offset[reg] = 0; // 0 offset is never used.
	if ((debug & DEBUG_NATIVE) != 0) {
	    System.err.println("free:" + reg);
	}
    }

    final void regUse(int reg) {
	regalloc_lockvec |= (1<<reg);
	//	System.err.println("reg lock:" + reg);
    }

    final void regDump() {
	int reg;
	int vec;
	System.err.print("dump L");
	vec = regalloc_lockvec;
	for (reg = 0; reg < 8; reg++) {
	    System.err.print(vec & 1);
	    vec >>= 1;
	}
	System.err.print(" M");
	vec = regalloc_modvec;
	for (reg = 0; reg < 8; reg++) {
	    System.err.print(vec & 1);
	    vec >>= 1;
	}
	System.err.println();
	for (int i = 0; i < 8; i++) {
	    System.err.println("offset:" + i + " = " + regalloc_offset[i]);
	}
    }

    final void regalloc0(int reg) {
	regalloc_usevec |= (1<<reg);
	regSave(reg);
    }

    final int regalloc0(int type, int val) {
	switch(type & TAG_MASK) {
	case TAG_CONST:
	    return INVALID_REG;
	case TAG_PARAM:
	    if (val == 0) {
		regalloc_usevec |= (1<<R_EAX);
		return R_EAX;
	    } else if (val == 1) {
		regalloc_usevec |= (1<<R_EDX);
		return R_EDX;
	    } else {
		throw new CompilerError("parm:" + val);
	    }
	default:
	    int offset = base_offset(val);
	    int reg = regSearch(offset);
	    if (reg != INVALID_REG) {
		regalloc_usevec |= (1<<reg);
		// System.err.println("regalloc0:" + reg + " 0x" + Integer.toHexString(offset));
	    }
	    return reg;
	}
    }

    final int regalloc1(int type, int val) {
	switch(type & TAG_MASK) {
	case TAG_CONST:
	    return INVALID_REG;
	case TAG_PARAM:
	    return INVALID_REG;
	default:
	    int offset = base_offset(val);
	    int reg = regSearch(offset);
	    if (reg != INVALID_REG) {
		regalloc_usevec |= (1<<reg);
		// System.err.println("regalloc1:" + reg + " 0x" + Integer.toHexString(offset));
	    }
	    return reg;
	}
    }

    final int regalloc(int regvec) {
	int reg;
	int unlock_reg = -1;
	int unuse_reg = -1;

	for (reg = 0; reg < 8; reg++) {
	    int vec = (1<<reg);
	    if ((regvec & vec) == 0)
		continue;
	    if ((regalloc_lockvec & vec) == 0) {
		unlock_reg = reg;
		if ((regalloc_usevec & vec) == 0) {
		    unuse_reg = reg;
		    if (regalloc_offset[reg] == 0) {
			// regDump();
			// System.err.println("reg found new:" + reg);
			regalloc_lockvec |= vec;
			regInvalidate(reg);
			if ((regalloc_modvec & (1<<reg)) != 0)
			    throw new CompilerError("reg modified:" + reg + " " + this);
			return reg;
		    }
		}
	    }
	}
	if (unlock_reg < 0) {
	    regDump();
	    throw new CompilerError("all registers are unavailable:" + Integer.toHexString(regvec));
	}
	reg = (unuse_reg < 0) ? unlock_reg : unuse_reg;
	// regDump();
	// System.err.println("reg found old:" + reg);
	regalloc_lockvec |= (1<<reg);
	regSave(reg);
	regInvalidate(reg);
	return reg;
    }

    final int regalloc(int regvec, int type, int val) {
	int reg;
	type &= TAG_MASK;
	if (type == TAG_CONST) {
	    reg = regalloc(regvec);
	    if (val == 0 && !save_eflags) {
		genRR(I_XOR, reg, reg);
	    } else {
		genCode(I_MOVIR | reg);
		genImm32(val);
	    }
	    return reg;
	} else if (type == TAG_PARAM) {
	    if (val == 0) {
		regUse(R_EAX);
		regSave(R_EAX);
		return R_EAX;
	    } else if (val == 1) {
		regUse(R_EDX);
		regSave(R_EDX);
		return R_EDX;
	    } else {
		throw new CompilerError("param Lval:" + val);
	    }
	} else {
	    int offset = base_offset(val);
	    int oldreg = regSearch(offset);

	    if (oldreg != INVALID_REG) {
		// System.err.println("search:" + oldreg + " off:" + offset);
		if ((regvec & (1 << oldreg)) != 0) {
		    regUse(oldreg);
		    return oldreg;
		}
		int vec = (1<<oldreg);
		if ((regalloc_modvec & vec) != 0) {
		    regalloc_modvec &= ~(1<<oldreg);
		    reg = regalloc(regvec);
		    if ((debug & DEBUG_NATIVE) != 0)
			System.err.print("mod chg:" + oldreg + "->" + reg);
		} else {
		    reg = regalloc(regvec);
		}
		genRR(I_MOV, oldreg, reg);
		regAssign(reg, offset);
		return reg;
	    } else {
		reg = regalloc(regvec);
		genMR(I_MOV, R_EBP, offset, reg);
		regAssign(reg, offset);
		return reg;
	    }
	}
    }

    final int regallocD(int regvec, ILnode il, int lreg, int dreg) {
	if (lreg == INVALID_REG) {
	    dreg = regalloc(regvec, il.ltype(), il.lval);
	} else {
	    if ((il.ltype() & TAG_FREE) != 0) {
		regalloc_modvec &= ~(1<<lreg);
		dreg = lreg;
	    } else {
		if (dreg == INVALID_REG) {
		    dreg = regalloc(regvec);
		} else {
		    dreg = regalloc(regvec, il.dtype(), il.dval);
		}
		if (lreg != dreg)
		    genRR(I_MOV, lreg, dreg);
	    }
	}
	return dreg;
    }

    final int base_offset_read(int val) {
	int offset = base_offset(val);
	int reg = regSearch(offset);
	if (reg != INVALID_REG)
	    regSave(reg);
	return offset;
    }

    final int base_offset_write(int type, int val) {
	int reg;
	int offset = base_offset(val);

	for (reg = 0; reg < 8; reg++) {
	    if (regalloc_offset[reg] == offset) {
		regalloc_modvec &= ~(1<<reg);
		regalloc_offset[reg] = 0;
	    }
	}
	return offset;
    }

    final void fill(int reg, int type, int val) {
	regUse(reg);
	if ((type & TAG_MASK) == TAG_CONST) {
	    regSave(reg);
	    regInvalidate(reg);
	    if (val == 0 && !save_eflags) {
		genRR(I_XOR, reg, reg);
	    } else {
		genCode(I_MOVIR | reg);
		genImm32(val);
	    }
	} else if ((type & TAG_MASK) == TAG_PARAM) {
	    if (val == 0) {
		if (reg != R_EAX) {
		    regSave(reg);
		    regInvalidate(reg);
		    genRR(I_MOV, R_EAX, reg);
		}
	    } else if (val == 1) {
		if (reg != R_EDX) {
		    regSave(reg);
		    regInvalidate(reg);
		    genRR(I_MOV, R_EDX, reg);
		}
	    } else {
		throw new CompilerError("param Lval:" + val);
	    }
	} else {
	    int offset = base_offset(val);
	    int oldreg = regSearch(offset);

	    if (oldreg != INVALID_REG) {
		// already allocated
		if (reg != oldreg) {
		    regSave(reg);
		    regInvalidate(reg);
		    genRR(I_MOV, oldreg, reg);
		}
		return;
	    }
	    regSave(reg);
	    regInvalidate(reg);
	    genMR(I_MOV, R_EBP, offset, reg);
	    regAssign(reg, offset);
	}
    }

    final void fillF(int type, int val) {
	int tag = (type & TAG_MASK);
	if (tag == TAG_CONST || tag == TAG_PARAM)
	    throw new CompilerError("tag:" + tag);
	int offset = base_offset_read(val);
	/* flds */
	genCode(I_FLOAT_S);
	genModBaseOff(0x0, R_EBP, offset);
    }

    final void fillD(int type, int val) {
	int tag = (type & TAG_MASK);
	if (tag == TAG_CONST || tag == TAG_PARAM)
	    throw new CompilerError("tag:" + tag);
	int offset = base_offset_read(val);
	/* flds */
	genCode(I_FLOAT_L);
	genModBaseOff(0x0, R_EBP, offset);
    }

    final void save(int reg, int type, int val) {
	int offset;
	switch(type & TAG_MASK) {
	case TAG_CONST:
	    break;
	case TAG_PARAM:
	    genPUSHR(reg);
	    break;
	case TAG_LOCAL:
	    offset = base_offset(val);
	    genRM(I_MOV, reg, R_EBP, offset);
	    regAssign(reg, offset);
	    break;
	default: /* TAG_STACK */
	    offset = base_offset(val);
	    regalloc_modvec |= (1<<reg);
	    if ((debug & DEBUG_NATIVE) != 0)
		System.err.println("alloc:" + reg + " %si" +
				   (nlocals + maxstack + 1 + (offset>>2)));
	    regAssign(reg, offset);
	}
    }

    final void saveF(int type, int val) {
	int tag = (type & TAG_MASK);
	if (tag == TAG_CONST || tag == TAG_PARAM)
	    throw new CompilerError("tag:" + tag);
	int offset = base_offset(val);
	int reg;
	for (reg = 0; reg < 8; reg++) {
	    if (regalloc_offset[reg] == offset) {
		regalloc_offset[reg] = 0;
		regalloc_modvec &= ~(1<<reg);
	    }
	}
	genCode(I_FLOAT_S);
	genModBaseOff(0x18, R_EBP, offset);
    }

    final void saveD(int type, int val) {
	int tag = (type & TAG_MASK);
	if (tag == TAG_CONST || tag == TAG_PARAM)
	    throw new CompilerError("tag:" + tag);
	int offset = base_offset(val);
	int reg;
	for (reg = 0; reg < 8; reg++) {
	    if (regalloc_offset[reg] == offset ||
		regalloc_offset[reg] == offset + 4) {
		regalloc_offset[reg] = 0;
		regalloc_modvec &= ~(1<<reg);
	    }
	}
	genCode(I_FLOAT_L);
	genModBaseOff(0x18, R_EBP, offset);
    }

}

public final class X86 extends X86reg {

    static {
	optimize_handle = false;
	little_endian_long = true;
	strict_args_order = true;
	clobber_return_reg = true;
    }

    LinkageInfo linkage = null, lastlink = null;

    /* set false if you want to disable compression */
    final static boolean disable_compression = true;

    final static int RT_ATTR_ARGCMASK = 0xff;
    /* virtual function call */
    final static int RT_ATTR_VIRTBL = (1<<8);
    /* require fixed length instruction for code patch */
    final static int RT_ATTR_PUSH32 = (1<<9);
    /* unnecessary to pop off stack when returns */
    final static int RT_ATTR_STDCALL = (1<<10);
    /* %eax is used for 1st param */
    final static int RT_ATTR_REGPARM = (1<<11);
    /* variable arguments */
    final static int RT_ATTR_VARARGC = (1<<12);

    // Number of bytes saved by long jcc -> short jcc
    final static int SIZE_SHORTJCC_COMPRESS = 4;
    final static int SIZE_SHORTJMP_COMPRESS = 3;

    final LinkageInfo newLinkage(int from_pc, int from_npc, int target_pc, int target_npc) {
      
      //      System.err.println("newLinkage called: from_pc "+from_pc+", target_pc "+target_pc);
      
      LinkageInfo newlink = new LinkageInfo(from_pc, from_npc, target_pc, target_npc);
      if (linkage == null) {
	linkage = lastlink = newlink; 
      } else {
	lastlink.next = newlink;
	lastlink = newlink;
      }
      return newlink;
    }
      
    final void genCALL(int func, int pc) {
	genCode(I_CALL);
	int target = func - code_area - native_pc - 4;
	LinkageInfo newlink = newLinkage(pc, native_pc, -1, target);
	newlink.flag |= LinkageInfo.TARGET_RELOCATED;
	genImm32(target);
	regInvalidate();
    }

    final void genCALLnotINV(int func, int pc) {
	genCode(I_CALL);
	int target = func - code_area - native_pc - 4;
	LinkageInfo newlink = newLinkage(pc, native_pc, -1, target);
	newlink.flag |= LinkageInfo.TARGET_RELOCATED;
	genImm32(target);
    }

    static boolean matchLandD(ILnode il) {
//  	if (il.tagD() != TAG_LOCAL)
//  	    return false;
	if ((il.ltype() & (TAG_MASK | TYPE_MASK)) !=
	    (il.dtype() & (TAG_MASK | TYPE_MASK)))
	    return false;
	return il.lval == il.dval;
    }

    final void emitARRAYCHK(ILnode il) {
	int tmpreg, rreg, lreg;
	rreg = regalloc0(il.rtype(), il.rval);
	lreg = regalloc0(il.ltype(), il.lval);

	if (lreg == INVALID_REG) {
	    tmpreg = regalloc(REG_ANY, il.ltype(), il.lval);
	} else {
	    lreg = regalloc(REG_ANY, il.ltype(), il.lval);
	    tmpreg = regalloc(REG_ANY);
	    genRR(I_MOV, lreg, tmpreg);
	}
	genMR(I_MOV, tmpreg, METHOD_OFFSET, tmpreg);
	genCode(0xc1);
	genCode(MODRM_R | I_SHR | tmpreg);
	genCode(METHOD_FLAG_BITS);
	if (il.tagR() == TAG_CONST) {
	    genIR(I_CMP, il.rval, tmpreg);
	} else {
	    rreg = regalloc(REG_ANY, il.rtype(), il.rval);
	    genRR(I_CMP, rreg, tmpreg);
	}
	genCode(I_JCC | I_JNBE);
	genCode(2);
	genCode(I_INT);
	genCode(0x10);
	freeTmpReg(tmpreg);
    }

    final void emitDIV0CHK(ILnode il) {
	int lreg = regalloc0(il.ltype(), il.lval);
	int rreg = regalloc0(il.rtype(), il.rval);
	int tmpreg;

	lreg = regalloc(REG_ANY, il.ltype(), il.lval);
	tmpreg = regalloc(REG_ANY);

	genRR(I_MOV, lreg, tmpreg);
	if (il.tagR() == TAG_CONST) {
	    genIR(I_OR, il.rval, tmpreg);
	} else if (rreg == INVALID_REG) {
	    genMR(I_OR, R_EBP, base_offset(il.rval), tmpreg);
	} else {
	    genRR(I_OR, rreg, tmpreg);
	}
	genCode(I_JCC | I_JNE);
	genCode(2);
	genCode(I_INT);
	genCode(0x00);
	freeTmpReg(tmpreg);
    }

    final boolean eliminateIndexScale(ILnode il) {
	if (il.next != null && il.tagR() == TAG_CONST) {
	    switch(il.next.op()) {
	    case ILnode.TBLSW:
		if (il.rval != 2 ||
		    il.next.tagL() != il.tagD() || il.next.lval != il.dval)
		    throw new CompilerError("SLL:" + il + "\t" + il.next);
		il.next.setL(il.ltype(), il.lval|(MODRM_SCALE_4 << MODRM_SCALE_BITS));
		return true;
	    case ILnode.LDSH:
	    case ILnode.LDUH:
	    case ILnode.STH:
		if (il.rval != 1 || 
		    il.next.tagR() != il.tagD() || il.next.rval != il.dval)
		    throw new CompilerError("SLL:" + il + "\t" + il.next);
		il.next.setR(il.ltype(), il.lval|(MODRM_SCALE_2 << MODRM_SCALE_BITS));
		return true;
	    case ILnode.LD:
	    case ILnode.ST:
	    case ILnode.FLD:
	    case ILnode.FST:
		if (il.rval != 2 || 
		    il.next.tagR() != il.tagD() || il.next.rval != il.dval)
		    throw new CompilerError("SLL:" + il + "\t" + il.next);
		il.next.setR(il.ltype(), il.lval|(MODRM_SCALE_4 << MODRM_SCALE_BITS));
		return true;
	    case ILnode.DLD:
	    case ILnode.DST:
		if (il.rval != 3 ||
		    il.next.tagR() != il.tagD() || il.next.rval != il.dval)
		    throw new CompilerError("SLL:" + il + "\t" + il.next);
		il.next.setR(il.ltype(), il.lval|(MODRM_SCALE_8 << MODRM_SCALE_BITS));
		return true;
  	    case ILnode.ADD:
		if (il.rval != 3 ||
		    il.next.tagR() != il.tagD() || il.next.rval != il.dval)
		    throw new CompilerError("SLL:" + il + "\t" + il.next);

		int dreg, lreg, rreg;

		if (il.next.tagL() == TAG_CONST)
		    throw new CompilerError("leal:" + il + "\t" + il.next);
		if (il.tagL() == TAG_CONST)
		    throw new CompilerError("leal:" + il + "\t" + il.next);

		lreg = regalloc0(il.next.ltype(), il.next.lval);
		rreg = regalloc0(il.ltype(), il.lval);

		dreg = regalloc(REG_ANY);
		lreg = regalloc(REG_ANY, il.next.ltype(), il.next.lval);
		rreg = regalloc(REG_ANY, il.ltype(), il.lval);
		/* leal (%lreg,%rreg,8),%dreg */
		genCode(0x8d);
		genModBaseIndex(0x04|(dreg<<3), lreg, rreg, MODRM_SCALE_8);
		save(dreg, il.next.dtype(), il.next.dval);
		il.next.remove();
		return true;
	    }
	}
	return false;
    }

    final void emitLD(ILnode il, int op) {
	int dreg, indx, base;

	if (il.tagL() == TAG_CONST) {
	    if (op != ILnode.LD)
		throw new CompilerError("ld ???:" + il);
	    if (il.tagR() != TAG_CONST || il.lval != 0)
		throw new CompilerError("ld ???" + il);

 	    if (il.tagD() == TAG_PARAM) {
 		/* pushl address */
 		genCode(0xff); genCode(0x35); genImm32(il.rval);
 		return;
	    }
	    dreg = regalloc(REG_ANY);
	    if (dreg == R_EAX) {
		genCode(0xa1);	// mem -> EAX
		genImm32(il.rval);
		save(R_EAX, il.dtype(), il.dval);
	    } else {
		genCode(I_MOV | 0x2);
		genCode(MODRM_M0 | 0x05 | (dreg << 3));
		genImm32(il.rval);
		save(dreg, il.dtype(), il.dval);
	    }
	    return;
	}

	regalloc0(il.ltype(), il.lval);
	regalloc0(il.rtype(), il.rval & MODRM_SCALE_MASK);

	base = regalloc(REG_ANY, il.ltype(), il.lval);

 	if (il.tagD() == TAG_PARAM && op == ILnode.LD) {
	    if (il.tagR() == TAG_CONST) {
		genPUSHM(base, il.rval);
	    } else {
		indx = regalloc(REG_ANY, il.rtype(), il.rval & MODRM_SCALE_MASK);
		genCode(0xff);
		genModBaseIndex(0x30, base, indx, il.rval >>> MODRM_SCALE_BITS);
	    }
	    return;
	}

	dreg = regalloc(REG_ANY);

	if (il.tagR() == TAG_CONST) {
	    switch(op) {
	    case ILnode.LD:
		genMR(I_MOV, base, il.rval, dreg);
		break;
	    case ILnode.LDSH:
		genCode(0x0f); genMR(I_MOVSH, base, il.rval, dreg);
		break;
	    case ILnode.LDSB:
		genCode(0x0f); genMR(I_MOVSB, base, il.rval, dreg);
		break;
	    case ILnode.LDUH:
		genCode(0x0f); genMR(I_MOVZH, base, il.rval, dreg);
		break;
	    }
	} else {
	    int ss = 0;		// scale factor
	    indx = regalloc(REG_ANY, il.rtype(), il.rval & MODRM_SCALE_MASK);
	    switch(op) {
	    case ILnode.LD:
		genCode(I_MOV | 0x2);
		ss = 2;
		break;
	    case ILnode.LDSH:
		genCode(0x0f); genCode(I_MOVSH | 0x2);
		ss = 1;
		break;
	    case ILnode.LDSB:
		genCode(0x0f); genCode(I_MOVSB | 0x2);
		break;
	    case ILnode.LDUH:
		genCode(0x0f); genCode(I_MOVZH | 0x2);
		ss = 1;
		break;
	    }
	    genModBaseIndex((dreg << 3), base, indx, il.rval >>> MODRM_SCALE_BITS);
	}
	save(dreg, il.dtype(), il.dval);
    }

    final void emitFLD(ILnode il) {
	regalloc0(il.ltype(), il.lval);
	regalloc0(il.rtype(), il.rval & MODRM_SCALE_MASK);

	if (il.tagL() == TAG_CONST) {
	    if (il.tagR() != TAG_CONST || il.lval != 0)
		throw new CompilerError("ld float ???" + il);
	    if (il.rval == RT_fconst_0) {
		/* fldz */
		genCode(0xd9); genCode(0xee);
	    } else if (il.rval == RT_fconst_1) {
		/* fld1 */
		genCode(0xd9); genCode(0xe8);
	    } else {
		/* flds */
		genCode(I_FLOAT_S);
		genCode(0x05);
		genImm32(il.rval);
	    }
	} else {
	    int base = regalloc(REG_ANY, il.ltype(), il.lval);
	    if (il.tagR() == TAG_CONST) {
		/* flds */
		genCode(I_FLOAT_S);
		genModBaseOff(0x0, base, il.rval);
	    } else {
		int indx = regalloc(REG_ANY, il.rtype(), il.rval & MODRM_SCALE_MASK);
		/* flds */
		genCode(I_FLOAT_S);
		genModBaseIndex(0x00, base, indx, il.rval >>> MODRM_SCALE_BITS);
	    }
	}
	saveF(il.dtype(), il.dval);
    }

    final void emitDLD(ILnode il) {
	regalloc0(il.ltype(), il.lval);
	regalloc0(il.rtype(), il.rval & MODRM_SCALE_MASK);
	
	if (il.tagL() == TAG_CONST) {
	    if (il.tagR() != TAG_CONST || il.lval != 0)
		throw new CompilerError("ld double ???" + il);
	    if (il.rval == RT_dconst_0) {
		/* fldz */
		genCode(0xd9); genCode(0xee);
	    } else if (il.rval == RT_dconst_1) {
		/* fld1 */
		genCode(0xd9); genCode(0xe8);
	    } else {
		/* fldl */
		genCode(I_FLOAT_L);
		genCode(0x05);
		genImm32(il.rval);
	    }
	} else {
	    int base = regalloc(REG_ANY, il.ltype(), il.lval);
	    if (il.tagR() == TAG_CONST) {
		/* fldl */
		genCode(I_FLOAT_L);
		genModBaseOff(0x0, base, il.rval);
	    } else {
		int indx = regalloc(REG_ANY, il.rtype(), il.rval & MODRM_SCALE_MASK);
		/* fldl */
		genCode(I_FLOAT_L);
		genModBaseIndex(0x00, base, indx, il.rval >>> MODRM_SCALE_BITS);
	    }
	}
	saveD(il.dtype(), il.dval);
    }

    final void emitST(ILnode il, int op) {
	int dreg, base, indx;

	dreg = regalloc0(il.ltype(), il.lval);
	base = regalloc0(il.rtype(), il.rval & MODRM_SCALE_BITS);
	indx = regalloc0(il.dtype(), il.dval);

	if (il.tagL() == TAG_CONST) {
	    if (il.tagR() != TAG_CONST || il.lval != 0)
		throw new CompilerError("st ???:" + il);
	    if (op == ILnode.STB) {
		dreg = regalloc(REG_LH, il.dtype(), il.dval);
		if (dreg == R_EAX) {
		    genCode(0xa3);	// EAX -> mem
		} else {
		    genCode(I_MOVB);
		    genCode(MODRM_M0 | (dreg << 3) | R_EBP);
		}
	    } else {
		dreg = regalloc(REG_ANY, il.dtype(), il.dval);
		if (op == ILnode.STH)
		    genCode(0x66);
		if (dreg == R_EAX) {
		    genCode(0xa3);	// EAX -> mem
		} else {
		    genCode(I_MOV);
		    genCode(MODRM_M0 | (dreg << 3) | R_EBP);
		}
	    }
	    genImm32(il.rval);
	} else {
	    base = regalloc(REG_ANY, il.ltype(), il.lval);
	    if (il.tagR() != TAG_CONST) {
		indx = regalloc(REG_ANY, il.rtype(), il.rval & MODRM_SCALE_MASK);
		if (il.tagD() != TAG_CONST) {
		    if (op == ILnode.STB) {
			dreg = regalloc(REG_LH, il.dtype(), il.dval);
			genCode(I_MOVB);
		    } else {
			dreg = regalloc(REG_ANY, il.dtype(), il.dval);
			if (op == ILnode.STH)
			    genCode(0x66);
			genCode(I_MOV);
		    }
		    genModBaseIndex(dreg << 3, base, indx, il.rval >>> MODRM_SCALE_BITS);
		} else {
		    switch(op) {
		    case ILnode.STH:
			genCode(0x66);
			genCode(0xc7);	// imm -> mem
			genModBaseIndex(0x00, base, indx, il.rval >>> MODRM_SCALE_BITS);
			genImm16(il.dval);
			break;
		    case ILnode.ST:
			genCode(0xc7);	// imm -> mem
			genModBaseIndex(0x00, base, indx, il.rval >>> MODRM_SCALE_BITS);
			genImm32(il.dval);
			break;
		    case ILnode.STB:
			genCode(0xc6);	// imm -> mem
			genModBaseIndex(0x00, base, indx, 0);
			genCode(il.dval);
			break;
		    }
		}
	    } else {
		if (il.tagD() != TAG_CONST) {
		    switch(op) {
		    case ILnode.STH:
			dreg = regalloc(REG_ANY, il.dtype(), il.dval);
			genCode(0x66);
			genRM(I_MOV, dreg, base, il.rval);
			break;
		    case ILnode.ST:
			dreg = regalloc(REG_ANY, il.dtype(), il.dval);
			genRM(I_MOV, dreg, base, il.rval);
			break;
		    case ILnode.STB:
			dreg = regalloc(REG_LH, il.dtype(), il.dval);
			genRM(I_MOVB, dreg, base, il.rval);
			break;
		    }
		} else {
		    switch(op) {
		    case ILnode.STH:
			genCode(0x66);
			genCode(0xc7);	// imm -> mem
			genModBaseOff(0, base, il.rval);
			genImm16(il.dval);
			break;
		    case ILnode.ST:
			genCode(0xc7);	// imm -> mem
			genModBaseOff(0, base, il.rval);
			genImm32(il.dval);
			break;
		    case ILnode.STB:
			genCode(0xc6);	// imm -> mem
			genModBaseOff(0, base, il.rval);
			genCode(il.dval);
			break;
		    }
		}
	    }
	}
    }

    final void emitFST(ILnode il) {
	regalloc0(il.ltype(), il.lval);
	regalloc0(il.rtype(), il.rval & MODRM_SCALE_MASK);
	regalloc0(il.dtype(), il.dval);
	
	fillF(il.dtype(), il.dval);
	if (il.tagL() == TAG_CONST) {
	    if (il.tagR() != TAG_CONST || il.lval != 0)
		throw new CompilerError("st ???:" + il);
	    /* fstps */
	    genCode(I_FLOAT_S);
	    genCode(MODRM_M0 | 0x18 | R_EBP);
	    genImm32(il.rval);
	} else {
	    int base = regalloc(REG_ANY, il.ltype(), il.lval);
	    if (il.tagR() == TAG_CONST) {
		/* fstps */
		genCode(I_FLOAT_S);
		genModBaseOff(0x18, base, il.rval);
	    } else {
		int indx = regalloc(REG_ANY, il.rtype(), il.rval & MODRM_SCALE_MASK);
		/* fstps*/
		genCode(I_FLOAT_S);
		genModBaseIndex(0x18, base, indx, il.rval >>> MODRM_SCALE_BITS);
	    }
	}
    }

    final void emitDST(ILnode il) {
	regalloc0(il.ltype(), il.lval);
	regalloc0(il.rtype(), il.rval & MODRM_SCALE_MASK);
	regalloc0(il.dtype(), il.dval);
	
	fillD(il.dtype(), il.dval);
	if (il.tagL() == TAG_CONST) {
	    if (il.tagR() != TAG_CONST || il.lval != 0)
		throw new CompilerError("st ???:" + il);
	    /* fstpl */
	    genCode(I_FLOAT_L);
	    genCode(MODRM_M0 | 0x18 | R_EBP);
	    genImm32(il.rval);
	} else {
	    int base = regalloc(REG_ANY, il.ltype(), il.lval);
	    if (il.tagR() == TAG_CONST) {
		/* fstpl */
		genCode(I_FLOAT_L);
		genModBaseOff(0x18, base, il.rval);
	    } else {
		int indx = regalloc(REG_ANY, il.rtype(), il.rval & MODRM_SCALE_MASK);
		/* fstpl */
		genCode(I_FLOAT_L);
		genModBaseIndex(0x18, base, indx, il.rval >>> MODRM_SCALE_BITS);
	    }
	}
    }

    final void emitMOVE(ILnode il) {
	int rval = il.rval;
	int rreg, dreg;

	rreg = regalloc0(il.rtype(), il.rval);
	
	if (il.typeD() == TYPE_DOUBLE ||
	    il.typeR() == TYPE_DOUBLE) {

	    if (il.tagR() == TAG_PARAM) {
		if (il.tagD() != TAG_PARAM) {
		    saveD(il.dtype(), il.dval);
		    return;
		}
	    }

	    if (il.tagR() == TAG_CONST || il.tagR() == TAG_PARAM)
		throw new CompilerError("move double:" + il);
	    if (il.tagD() == TAG_CONST)
		throw new CompilerError("move double:" + il);
	    if (il.tagD() == TAG_PARAM) {
		int offset = base_offset_read(rval);
		genPUSHM(R_EBP, offset + 4);
		genPUSHM(R_EBP, offset);
	    } else {
		fillD(il.rtype(), il.rval);
		saveD(il.dtype(), il.dval);
	    }
	    return;
	}

	if (il.tagD() == TAG_PARAM) {
	    if (il.tagR() == TAG_CONST) {
		genPUSHI(rval);
	    } else if (il.tagR() == TAG_PARAM) {
		if (rval == 0) {
		    genPUSHR(R_EAX);
		} else if (rval == 1) {
		    genPUSHR(R_EDX);
		} else {
		    throw new CompilerError("push ???:" + il);
		}
	    } else if (rreg != INVALID_REG) {
		genPUSHR(rreg);
	    } else {
		genPUSHM(R_EBP, base_offset(rval));
	    }
	} else if (il.tagR() == TAG_PARAM) {
	    if (rval == 0) {
		switch(il.typeR()) {
		case TYPE_DOUBLE:
		    saveD(il.dtype(), il.dval);
		    break;
		case TYPE_FLOAT:
		    saveF(il.dtype(), il.dval);
		    break;
		default:
		    save(R_EAX, il.dtype(), il.dval);
		}
	    } else if (rval == 1) {
		save(R_EDX, il.dtype(), il.dval);
	    } else {
		throw new CompilerError("push ???:" + il);
	    }
	} else if (il.tagR() == TAG_CONST) {
	    int offset = base_offset_write(il.dtype(), il.dval);
	    genRM(I_MOVIM | 1, 0, R_EBP, offset);
	    genImm32(rval);
	} else {
	    if (rreg != INVALID_REG) {
		if (il.rval == il.dval) {
		    // il.op() == ILnode.CONV
		    return;
		}
		dreg = regalloc(REG_ANY);
		genRR(I_MOV, rreg, dreg);
		save(dreg, il.dtype(), il.dval);
	    } else {
		dreg = regalloc(REG_ANY, il.rtype(), rval);
		save(dreg, il.dtype(), il.dval);
	    }
	}
    }

    final void emitINSN(ILnode il, int op, boolean swappable) {
	int lreg, rreg, dreg;

	if (swappable) {
	    if (il.tagL() == TAG_CONST)
		il.swapLR();
	    else if (((il.tagL() & TAG_FREE) == 0) &&
		     ((il.tagR() & TAG_FREE) != 0))
		il.swapLR();
	}

	lreg = regalloc0(il.ltype(), il.lval);
	rreg = regalloc0(il.rtype(), il.rval);
	dreg = regalloc1(il.dtype(), il.dval);

	if (dreg == INVALID_REG && matchLandD(il)) {
	    int offset = base_offset_write(il.dtype(), il.dval);
	    if (il.tagR() == TAG_CONST) {
		int imm = il.rval;
		op &= ~0x01;
		if (imm >= -128 && imm <= 127) {
		    genCode(0x83);
		    genModBaseOff(op, R_EBP, offset);
		    genCode(imm);
		} else {
		    genCode(0x81);
		    genModBaseOff(op, R_EBP, offset);
		    genImm32(imm);
		}
		return;
	    } else {
		rreg = regalloc(REG_ANY, il.rtype(), il.rval);
		genRM(op, rreg, R_EBP, offset);
		return;
	    }
	}
	if (lreg == INVALID_REG) {
	    dreg = regalloc(REG_ANY, il.ltype(), il.lval);
	} else if (dreg == INVALID_REG) {
	    dreg = regallocD(REG_ANY, il, lreg, dreg);
	} else if (lreg != dreg) {
	    if (rreg == dreg) {
		if (swappable) {
		    genRR(op, lreg, dreg);
		    regInvalidate(dreg);
		    save(dreg, il.dtype(), il.dval);
		    return;
		}
		regInvalidate(dreg);
		dreg = regalloc(REG_ANY);
	    }
	    genRR(I_MOV, lreg, dreg);
	}
	if (il.tagR() == TAG_CONST) {
	    genIR(op, il.rval, dreg);
	} else if (rreg != INVALID_REG) {
	    genRR(op, rreg, dreg);
	} else {
	    genMR(op, R_EBP, base_offset(il.rval), dreg);
	}
	regInvalidate(dreg);
	save(dreg, il.dtype(), il.dval);
    }

    final boolean emitINCDEC(ILnode il, int val) {
	int offset;
	int dreg;

	if (!matchLandD(il))
	    return false;
	if (il.tagD() == TAG_PARAM)
	    throw new CompilerError("unimp:" + il);
	if (val == 1) {
	    dreg = regalloc1(il.dtype(), il.dval);
	    if (dreg == INVALID_REG) {
		offset = base_offset_write(il.dtype(), il.dval);
		genCode(0xff); // inc
		genModBaseOff(0x00, R_EBP, offset);
	    } else {
		genCode(0xff);
		genCode(MODRM_R | dreg);
		save(dreg, il.dtype(), il.dval);
	    }
	    return true;
	} else if (val == -1) {
	    dreg = regalloc1(il.dtype(), il.dval);
	    if (dreg == INVALID_REG) {
		offset = base_offset_write(il.dtype(), il.dval);
		genCode(0xff); // dec
		genModBaseOff(0x08, R_EBP, offset);
	    } else {
		genCode(0xff); // dec
		genCode(MODRM_R | 0x08 | dreg);
		save(dreg, il.dtype(), il.dval);
	    }
	    return true;
	} else {
	    return false;
	}
    }

    final void emitFInsn(ILnode il, int op1, int op2) {
	int tagR = il.tagR();
	if (tagR == TAG_CONST || tagR == TAG_PARAM)
	    throw new CompilerError("emitFInsn:" + il);
	fillF(il.ltype(), il.lval);
	int offset = base_offset_read(il.rval);
	/* op */
	genCode(op1);
	genModBaseOff(op2, R_EBP, offset);
	saveF(il.dtype(), il.dval);
    }

    final void emitDInsn(ILnode il, int op1, int op2) {
	int tagR = il.tagR();
	if (tagR == TAG_CONST || tagR == TAG_PARAM)
	    throw new CompilerError("emitDInsn:" + il);
	fillD(il.ltype(), il.lval);
	int offset = base_offset_read(il.rval);
	genCode(op1);
	genModBaseOff(op2, R_EBP,offset);
	saveD(il.dtype(), il.dval);
    }

    final void emitFNEG(ILnode il) {
	fillF(il.rtype(), il.rval);
	/* fchs */
	genCode(0xd9); genCode(0xe0);
	saveF(il.dtype(), il.dval);
    }

    final void emitDNEG(ILnode il) {
	fillD(il.rtype(), il.rval);
	/* fchs */
	genCode(0xd9); genCode(0xe0);
	saveD(il.dtype(), il.dval);
    }

    final void emitI2B(ILnode il) {
	int rreg, dreg;
	rreg = regalloc0(il.rtype(), il.rval);
	dreg = regalloc1(il.dtype(), il.dval);
	if (rreg == INVALID_REG) {
	    dreg = regalloc(REG_ANY);
	    /* movsbl offset(%ebp),%dreg */
	    genCode(0x0f); genMR(I_MOVSB, R_EBP, base_offset(il.rval), dreg);
	    save(dreg, il.dtype(), il.dval);
	} else {
	    rreg = regalloc(REG_LH, il.rtype(), il.rval);
	    if (dreg == INVALID_REG) {
		dreg = regalloc(REG_ANY);
	    }
	    /* movsbl %rreg,%dreg */
	    genCode(0x0f); genRR(0xbe, dreg, rreg);
	    save(dreg, il.dtype(), il.dval);
	}
    }

    final void emitI2S(ILnode il) {
	int rreg, dreg;
	rreg = regalloc0(il.rtype(), il.rval);
	dreg = regalloc1(il.dtype(), il.dval);
	if (rreg == INVALID_REG) {
	    dreg = regalloc(REG_ANY);
	    /* movswl offset(%ebp),%dreg */
	    genCode(0x0f); genMR(I_MOVSH, R_EBP, base_offset(il.rval), dreg);
	    save(dreg, il.dtype(), il.dval);
	} else {
	    rreg = regalloc(REG_LH, il.rtype(), il.rval);
	    if (dreg == INVALID_REG) {
		dreg = regalloc(REG_ANY);
	    }
	    if (dreg == R_EAX) {
		if (rreg != R_EAX)
		    genRR(I_MOV, rreg, R_EAX);
		/* cwtl */
		genCode(0x98);
	    } else {
		/* movswl %rreg,%dreg */
		genCode(0x0f); genRR(0xbf, dreg, rreg);
	    }
	    save(dreg, il.dtype(), il.dval);
	}
    }

    final void emitI2F(ILnode il, int op) {
	int tagR = il.tagR();
	int offset;

	if (tagR == TAG_CONST) {
	    offset = base_offset(il.dval);
	    genRM(I_MOVIM | 1, 0, R_EBP, offset);
	    genImm32(il.rval);
	} else if (tagR == TAG_PARAM) {
//  	    int rreg = regalloc(REG_ANY, il.rtype(), il.rval);
//  	    offset = base_offset(il.dval);
//  	    genRM(I_MOV, rreg, R_EBP, offset);
	    throw new CompilerError("R is param:" + il);
	} else {
	    offset = base_offset_read(il.rval);
	}

	/* fildl */
	genCode(0xdb); genModBaseOff(0x00, R_EBP, offset);

	if (op == ILnode.I2F)
	    saveF(il.dtype(), il.dval);
	else
	    saveD(il.dtype(), il.dval);
    }

    final void emitF2D(ILnode il) {
	fillF(il.rtype(), il.rval);
	saveD(il.dtype(), il.dval);
    }

    final void emitD2F(ILnode il) {
	fillD(il.rtype(), il.rval);
	saveF(il.dtype(), il.dval);
    }

    final void emitF2I(ILnode il, int op, int pc) {
	/* regalloc0(R_EAX); */
	regSave(R_EAX);
	if (op == ILnode.F2I) {
	    fillF(il.rtype(), il.rval);
	} else {
	    fillD(il.rtype(), il.rval);
	}
	genCALLnotINV(RT_addr[RT_f2i], pc);
	save(R_EAX, il.dtype(), il.dval);
    }

    final void emitF2Iinline(ILnode il, int op) {
	int tagD = il.tagD();
	if (tagD == TAG_CONST) {
	    throw new CompilerError("emitF2I:" + il);
	}
	
	/* regalloc0(R_EAX); */
	regSave(R_EAX);

	int tmpoff = -4; /* base_offset(nlocals+maxstack) */
	int tmpreg = R_EAX; /* regalloc(REG_LH); */
	int offset = base_offset(il.dval);
	int dreg = R_EAX; /* regalloc(REG_ANY, il.dtype(), il.dval); */

	if (op == ILnode.F2I) {
	    fillF(il.rtype(), il.rval);
	} else {
	    fillD(il.rtype(), il.rval);
	}
	/* ftst */
	genCode(0xd9); genCode(0xe4);
	/* fnstcw tmpoff(%ebp) */
	genCode(I_FLOAT_S);
	genModBaseOff(0x38, R_EBP, tmpoff);
	/* movl tmpoff(%ebp),%eax */
	genMR(I_MOV, R_EBP, tmpoff, tmpreg);
	/* movb 0xc,%ah */
	genCode(0xb4 | tmpreg);
	genCode(0x0c);
	/* movw %ax, tmpoff+2(%ebp) */
	genCode(0x66);
	genRM(I_MOV, tmpreg, R_EBP, tmpoff+2);
	/* fldcw  tmpoff+2(%ebp) */
	genCode(I_FLOAT_S);
	genModBaseOff(0x2c, R_EBP, tmpoff+2);
	/* fistpl offset(%ebp) */
	genCode(0xdb);
	genModBaseOff(0x18, R_EBP, offset);
	/* movl offset(%ebp),%dreg */
	genMR(I_MOV, R_EBP, offset, dreg);
	/* fldcw offset(%ebp) */
	genCode(I_FLOAT_S);
	genModBaseOff(0x2c, R_EBP, tmpoff);
	/* cmp $0x80000000,%dreg */
	genIR(I_CMP, 0x80000000, dreg);
	/* jne Next */
	genCode(I_JCC | I_JNE);
        genCode(17);
	/* fnstsw */
	genCode(0xdf); genCode(0xe0);
	/* sahf */
	genCode(0x9e);
	/* jp NaN */
	genCode(I_JCC | I_JP);
	genCode(10);
	/* movl $0x7fffffff,%dreg */
	genCode(I_MOVIR | dreg); genImm32(0x80000000);
	/* adcl $-1,%eax */
	genIR(I_ADC, -1, dreg);
	/* jmp Next */
	genCode(I_JMP_8);
	genCode(2);
	/* Nan: xor %dreg,%dreg */
	genRR(I_XOR, dreg, dreg);
	/* Next: */
	save(dreg, il.dtype(), il.dval);
    }

    final void emitCMP(ILnode il) {
	int lreg, rreg;

	lreg = regalloc0(il.ltype(), il.lval);
	rreg = regalloc0(il.rtype(), il.rval);

	if (il.tagR() == TAG_CONST) {
	    if (lreg != INVALID_REG) {
		genIR(I_CMP, il.rval, lreg);
	    } else {
		genIM(I_CMP, il.rval, R_EBP, base_offset(il.lval));
	    }
	} else if (rreg != INVALID_REG) {
	    if (il.tagL() == TAG_CONST) {
		lreg = regalloc(REG_ANY, il.ltype(), il.lval);
		genRR(I_CMP, rreg, lreg);
	    } else if (lreg == INVALID_REG) {
		genRM(I_CMP, rreg, R_EBP, base_offset(il.lval));
	    } else {
		genRR(I_CMP, rreg, lreg);
	    }
	} else {
	    lreg = regalloc(REG_ANY, il.ltype(), il.lval);
	    genMR(I_CMP, R_EBP, base_offset(il.rval), lreg);
	}
    }

    final void emitTEST(ILnode il) {
	int lreg, rreg;

	lreg = regalloc0(il.ltype(), il.lval);
	rreg = regalloc0(il.rtype(), il.rval);

	if (il.tagR() == TAG_CONST) {
	    if (lreg != INVALID_REG) {
		lreg = regalloc(REG_ANY, il.ltype(), il.lval);
		if (lreg == R_EAX) {
		    if (il.rval >= 0 && il.rval < 256) {
			/* testb $imm,%al */
			genCode(0xa8);
			genCode(il.rval);
		    } else {
			/* testl $imm,%eax */
			genCode(0xa9);
			genImm32(il.rval);
		    }
		} else {
		    genCode(0xf7);
		    genCode(MODRM_R | lreg);
		    genImm32(il.rval);
		}
	    } else {
		genCode(0xf7);
		genModBaseOff(0x00, R_EBP, base_offset(il.lval));
		genImm32(il.rval);
	    }
	} else if (rreg != INVALID_REG) {
	    lreg = regalloc(REG_ANY, il.ltype(), il.lval);
	    genRR(I_TEST, rreg, lreg);
	} else {
	    lreg = regalloc(REG_ANY, il.ltype(), il.lval);
	    genRM(I_TEST, lreg, R_EBP, base_offset(il.rval));
	}
    }

    final void emitFCMP(ILnode il) {
	fillF(il.ltype(), il.lval);
	fillF(il.rtype(), il.rval);
	/* fucompp */
	genCode(0xda);
	genCode(0xe9);
    }

    final void emitDCMP(ILnode il) {
	fillD(il.ltype(), il.lval);
	fillD(il.rtype(), il.rval);
	/* fucompp */
	genCode(0xda);
	genCode(0xe9);
    }

    final void emitMUL(ILnode il) {
	int lreg, rreg;

	lreg = regalloc0(il.ltype(), il.lval);
	rreg = regalloc0(il.rtype(), il.rval);
	regalloc0(R_EAX);
	regSave(R_EDX);

	if (il.tagR() == TAG_CONST) {
	    rreg = regalloc(REG_NOT_A, il.rtype(), il.rval);
	    fill(R_EAX, il.ltype(), il.lval);
	    genRR(0xf7, 0x05, rreg);
	} else if (il.tagL() == TAG_CONST) {
	    lreg = regalloc(REG_NOT_A, il.ltype(), il.lval);
	    fill(R_EAX, il.rtype(), il.rval);
	    genRR(0xf7, 0x05, lreg);
	} else if (rreg == R_EAX) {
	    if (lreg != INVALID_REG) {
		genCode(0x0f); genRR(0xaf, 0, lreg);
	    } else {
		genCode(0xf7); genModBaseOff(0x20, R_EBP, base_offset(il.lval));
	    }
	} else {
	    fill(R_EAX, il.ltype(), il.lval);
	    if (rreg != INVALID_REG) {
		genCode(0x0f); genRR(0xaf, 0, rreg);
	    } else {
		genCode(0xf7); genModBaseOff(0x20, R_EBP, base_offset(il.rval));
	    }
	}
	regInvalidate(R_EDX);
	save(R_EAX, il.dtype(), il.dval);
    }

    final void emitDIV(ILnode il, int dreg) {
	int lreg, rreg;

	lreg = regalloc0(il.ltype(), il.lval);
	rreg = regalloc0(il.rtype(), il.rval);
	regalloc0(R_EAX);
	regalloc0(R_EDX);

	if (il.tagR() == TAG_CONST) {
	    rreg = regalloc(REG_NOT_AD, il.rtype(), il.rval);
	    fill(R_EAX, il.ltype(), il.lval);
	    /* cdq */
	    genCode(0x99);
	    /* idiv */
	    genRR(0xf7, 0x07, rreg);
	} else if (rreg != INVALID_REG) {
	    rreg = regalloc(REG_NOT_AD, il.rtype(), il.rval);
	    fill(R_EAX, il.ltype(), il.lval);
	    /* cltd */
	    genCode(0x99);
	    /* idiv */
	    genCode(0xf7); genCode(0xf8 | rreg);
	} else {
	    fill(R_EAX, il.ltype(), il.lval);
	    /* cltd */
	    genCode(0x99);
	    /* idiv */
	    genCode(0xf7); genModBaseOff(0x38, R_EBP, base_offset(il.rval));
	}
	regInvalidate(R_EAX);
	regInvalidate(R_EDX);
	save(dreg, il.dtype(), il.dval);
    }

    final void emitSHIFT(ILnode il, int op) {
	int lreg, rreg, dreg;

	lreg = regalloc0(il.ltype(), il.lval);
	rreg = regalloc0(il.rtype(), il.rval);
	dreg = regalloc1(il.dtype(), il.dval);

	if (il.tagR() != TAG_CONST)
	    regalloc0(R_ECX);
	else if (il.rval > 255)
	    throw new CompilerError("shift value ???:" + il);

	if (dreg == INVALID_REG && matchLandD(il)) {
	    int offset = base_offset_write(il.dtype(), il.dval);
	    if (il.tagR() == TAG_CONST) {
		if (il.rval == 1) {
		    genCode(0xd1);
		    genModBaseOff(op, R_EBP, offset);
		} else {
		    genCode(0xc1);
		    genModBaseOff(op, R_EBP, offset);
		    genCode(il.rval);
		}
	    } else {
		fill(R_ECX, il.rtype(), il.rval);
		genCode(0xd3);
		genModBaseOff(op, R_EBP, offset);
	    }
	    return;
	}
	if (il.tagR() == TAG_CONST) {
	    dreg = regallocD(REG_ANY, il, lreg, dreg);
	    if (il.rval == 1) {
		genCode(0xd1);
		genCode(MODRM_R | op | dreg);
	    } else {
		genCode(0xc1);
		genCode(MODRM_R | op | dreg);
		genCode(il.rval);
	    }
	} else {
	    dreg = regallocD(REG_NOT_C, il, lreg, dreg);
	    fill(R_ECX, il.rtype(), il.rval);
	    genCode(0xd3);
	    genCode(MODRM_R | op | dreg);
	}
	regInvalidate(dreg);
	save(dreg, il.dtype(), il.dval);
    }

    final void emitBRANCH(ILnode il, int pc) {
	int op = 0;
	LinkageInfo newlink;

	switch(il.lval) {
	    /* integer */
	case CC_JSR:
	    /* FIX ME : violate JVM specification */
	    genCode(I_CALL);
	    newlink = newLinkage(pc, native_pc, il.rval, -1);
	    genCode(0);
	    genCode(0);
	    genCode(0);
	    genCode(0);
	    regInvalidate();
	    return;

	case CC_A:
	    genCode(I_JMP);
	    newlink = newLinkage(pc, native_pc, il.rval, -1);
	    newlink.flag |= LinkageInfo.JUMP; 
	    genCode(0);
	    genCode(0);
	    genCode(0);
	    genCode(0);
	    return;

	case CC_E:	op = I_JE; break;
	case CC_NE:	op = I_JNE; break;
	case CC_L:	op = I_JL; break;
	case CC_LE:	op = I_JLE; break;
	case CC_G:	op = I_JNLE; break;
	case CC_GE:	op = I_JNL; break;

	case CC_LU:	op = I_JB; break;
	case CC_LEU:	op = I_JBE; break;
	case CC_GU:	op = I_JNBE; break;
	case CC_GEU:	op = I_JNB; break;

	case CC_TBLOB:	op = I_JNBE; break;

	case CC_TBLSW:
	    newlink = newLinkage(pc, native_pc, il.rval, -1);
	    newlink.flag |= LinkageInfo.OFFSET_ABSOLUTE;
	    genCode(0);
	    genCode(0);
	    genCode(0);
	    genCode(0);
	    return;

	    /* floating condition */
	case CC_FE:
	    regInvalidate(R_EAX);
	    /* fnstsw */
	    genCode(0xdf); genCode(0xe0);
	    /* andb   $0x45,%ah*/
	    genCode(0x80); genCode(0xe4); genCode(0x45);
	    /* cmpb   $0x40,%ah*/
	    genCode(0x80); genCode(0xfc); genCode(0x40);
	    op = I_JE;
	    break;

	case CC_FNE:
	    regInvalidate(R_EAX);
	    /* fnstsw */
	    genCode(0xdf); genCode(0xe0);
	    /* andb   $0x44,%ah*/
	    genCode(0x80); genCode(0xe4); genCode(0x44);
	    /* xorb   $0x40,%ah*/
	    genCode(0x80); genCode(0xf4); genCode(0x40);
	    op = I_JNE;
	    break;

	case CC_FL:
	    regInvalidate(R_EAX);
	    /* fnstsw */
	    genCode(0xdf); genCode(0xe0);
	    /* andb   $0x45,%ah*/
	    genCode(0x80); genCode(0xe4); genCode(0x45);
	    /* decb   %ah*/
	    genCode(0xfe); genCode(0xcc);
	    /* cmpb   $0x40,%ah*/
	    genCode(0x80); genCode(0xfc); genCode(0x40);
	    op = I_JNB;
	    break;

	case CC_FLE:
	    regInvalidate(R_EAX);
	    /* fnstsw */
	    genCode(0xdf); genCode(0xe0);
	    /* andb   $0x45,%ah*/
	    genCode(0x80); genCode(0xe4); genCode(0x45);
	    /* cmpb   $0x1,%ah*/
	    genCode(0x80); genCode(0xfc); genCode(0x01);
	    op = I_JNE;
	    break;

	case CC_FG:
	    regInvalidate(R_EAX);
	    /* fnstsw */
	    genCode(0xdf); genCode(0xe0);
	    /* andb   $0x5,%ah*/
	    genCode(0x80); genCode(0xe4); genCode(0x05);
	    op = I_JNE;
	    break;

	case CC_FGE:
	    regInvalidate(R_EAX);
	    /* fnstsw */
	    genCode(0xdf); genCode(0xe0);
	    /* andb   $0x45,%ah*/
	    genCode(0x80); genCode(0xe4); genCode(0x45);
	    op = I_JNE;
	    break;

	case CC_NFL:
	    regInvalidate(R_EAX);
	    /* fnstsw */
	    genCode(0xdf); genCode(0xe0);
	    /* andb   $0x45,%ah*/
	    genCode(0x80); genCode(0xe4); genCode(0x45);
	    op = I_JE;
	    break;

	case CC_NFLE:
	    regInvalidate(R_EAX);
	    /* fnstsw */
	    genCode(0xdf); genCode(0xe0);
	    /* andb   $0x5,%ah*/
	    genCode(0x80); genCode(0xe4); genCode(0x05);
	    op = I_JE;
	    break;

	case CC_NFG:
	    regInvalidate(R_EAX);
	    /* fnstsw */
	    genCode(0xdf); genCode(0xe0);
	    /* andb   $0x45,%ah*/
	    genCode(0x80); genCode(0xe4); genCode(0x45);
	    /* cmpb   $0x1,%ah*/
	    genCode(0x80); genCode(0xfc); genCode(0x01);
	    op = I_JE;
	    break;

	case CC_NFGE:
	    regInvalidate(R_EAX);
	    /* fnstsw */
	    genCode(0xdf); genCode(0xe0);
	    /* andb   $0x45,%ah*/
	    genCode(0x80); genCode(0xe4); genCode(0x45);
	    /* decb   %ah*/
	    genCode(0xfe); genCode(0xcc);
	    /* cmpb   $0x40,%ah*/
	    genCode(0x80); genCode(0xfc); genCode(0x40);
	    op = I_JB;
	    break;

	default:
	    throw new CompilerError("branch cond:" + il.lval);

	}
	
	genCode(I_JCC1);
        genCode(0x80 | op);
	newlink = newLinkage(pc, native_pc, il.rval, -1);
	newlink.flag |= LinkageInfo.JCC;
	newlink.flag |= (op << LinkageInfo.JCC_OP_SHIFT);
	genCode(0);
	genCode(0);
	genCode(0);
	genCode(0);
	
    }

    final void emitCALL(ILnode il, int pc, int bc_stack_size) {
	int func = il.rval;
	int attr = RT_attr[func];

	regSave();
	if ((attr & RT_ATTR_VIRTBL) != 0) {
	    /* movl 0x0(%esp,1),%eax */
	    genCode(0x8b); genCode(0x44); genCode(0x24); genCode(0x00);
	    /* movl $0x4(%eax),%eax */
	    genCode(0x8b); genCode(0x40); genCode(0x04);
	}
	if ((attr & RT_ATTR_PUSH32) != 0) {
	    genPUSH32(il.lval);
	} else if ((attr & RT_ATTR_REGPARM) != 0) {
	    /* pass one argument in registers EAX */
	    genCode(I_MOVIR | R_EAX);
	    genImm32(il.lval);
	}
	genCALL(RT_addr[func], pc);
	if (func == RT_getstatic) {
	    switch(il.typeD()) {
	    case TYPE_DOUBLE:
	    case TYPE_FLOAT:
		genCode(I_NOP);
		break;
	    case TYPE_LONG:
		genCode(I_NOP);
		genCode(I_NOP);
		genCode(I_NOP);
		genCode(I_NOP);
		genCode(I_NOP);
		genCode(I_NOP);
		break;
	    default:
	    }
	}

	/* pop off the stack space */
	if ((attr & RT_ATTR_STDCALL) == 0) {
	    int pop = (attr & RT_ATTR_ARGCMASK);
	    if ((attr & RT_ATTR_VARARGC) != 0) {
		if (il.tagD() == TAG_CONST)
		    pop -= il.dval;
		else {
		    pop += bc_stack_size + nlocals - il.dval;
		}
	    }
	    if (pop != 0)
		genIR(I_ADD, pop*4, R_ESP);
	}
    }

    final void emitRETURN(ILnode il, int pc) {
	if ((access & ACC_SYNCHRONIZED) != 0) {
	    // FIX ME: only return value should be saved
	    regSave();

	    if ((access & ACC_STATIC) != 0) {
		genMR(I_MOV, R_EBP, 8, R_EAX);
		genPUSHM(R_EAX, 0);
	    } else {
		int reg = regalloc0(TAG_LOCAL, 0);
		if (reg == INVALID_REG) {
		    int offset = base_offset(0);
		    genPUSHM(R_EBP, offset);
		} else {
		    genPUSHR(reg);
		}
	    }
	    genCALL(RT_addr[RT_monitorExit], pc);
	    /* genIR(I_ADD, 4, R_ESP); */
	}
	if (il.tagD() != TAG_CONST) { // non-void function
	    switch(il.typeD()) {
	    case TYPE_LONG:
		fill(R_EAX, il.ltype(), il.lval);
		fill(R_EDX, il.rtype(), il.rval);
		break;
	    case TYPE_INT:
		fill(R_EAX, il.rtype(), il.rval);
		break;
	    case TYPE_FLOAT:
		if (il.tagR() == TAG_PARAM) {
		    if (il.rval == 0) // nothing to do
			break;
		    throw new CompilerError("???:" + il);
		}
		fillF(il.rtype(), il.rval);
		break;
	    case TYPE_DOUBLE:
		if (il.tagR() == TAG_PARAM) {
		    if (il.rval == 0) // nothing to do
			break;
		    throw new CompilerError("???:" + il);
		}
		fillD(il.rtype(), il.rval);
		break;
	    default:
		throw new CompilerError("unimplemented yet:" + il);
	    }
	}
	genCode(I_LEAVE);
	genCode(I_RET);

	regDiscard();
    }

    final void emitTBLSW(ILnode il, int pc) {
	int tbl_addr;

	int indx = regalloc(REG_ANY, il.ltype(), il.lval & MODRM_SCALE_MASK);
	// jmp *.TBL(,%edx,4)
	genCode(0xff);
	genCode(0x24);
	genCode((il.lval >>> MODRM_SCALE_BITS)|0x05|(indx<<3));
	tbl_addr = code_area + native_pc + 4;
	tbl_addr = (tbl_addr + 3) & (~3);
	
	LinkageInfo newlink = newLinkage(pc, native_pc, -1, tbl_addr);
	newlink.flag |= (LinkageInfo.OFFSET_ABSOLUTE | LinkageInfo.TARGET_RELOCATED);
	genImm32(tbl_addr);
	
	while (native_pc < tbl_addr - code_area)
	    genCode(I_NOP);
    }

    final void compressCode(int beg_npc, int end_npc, int to_npc) {
      int from, to;
      
      if (to_npc >= beg_npc) throw new CompilerError("compressCode: to_npc must be < beg_npc!");
      if (end_npc <= beg_npc) throw new CompilerError("compressCode: end_npc must be > beg_npc!");

      for (from = beg_npc, to = to_npc;
	   from <= end_npc;
	   from++, to++) {
	int b = getNativeCode(from);
	setNativeCode(to, b);
      }
    }

    final void emitInlinePrologue(ILnode il, int pc) {
      // Emit: mov $<il.rval>, 8(%ebp)
      genRM(I_MOVIM | 1, 0, R_EBP, 8);
      genImm32(il.rval);
    }

    final void emitInlineEpilogue(ILnode il, int pc) {
      // Emit: mov $<il.rval>, 8(%ebp)
      genRM(I_MOVIM | 1, 0, R_EBP, 8);
      genImm32(il.rval);
    }

    // Returns new end_nativepc
    final void fixLinkage() {
	LinkageInfo link;
	int compressed = 0;
	int end_npc = native_pc;

	// First pass: Find "short" jumps conservatively
	for (link = linkage; link != null; link = link.next) {
	  
	  //	  System.err.println("FIXLINK: is "+link.toString());

	  // XXX mdw: For now, don't rewrite JUMP as it causes compress
	  // of 3 bytes -- which throws off alignment of TBLSW!
	  if ((link.flag & LinkageInfo.JCC) == 0) continue;

	  if (disable_compression) continue;

	  int from_npc = link.from_npc;
	  int target_npc = bcinfo[link.target_pc].native_pc;
	  
	  // -4 as offset is from next instruction (and offset is 4 bytes)
	  int offset = (target_npc - from_npc - 4);

	  //	  System.err.println("FIXLINK: 1st pass offset: "+offset);
	  
	  if ((offset <= 127) && (offset >= -128)) { 
	    // One-byte offset
	    //	    System.err.println("FIXLINK: rewriting as SHORTJMP: "+link.toString());
	    link.flag |= LinkageInfo.SHORTJMP;

	    // Rewrite insn
	    int insn, compress;
	    
	    if ((link.flag & LinkageInfo.JCC) != 0) {
	      int op = (link.flag & LinkageInfo.JCC_OP_MASK) >> LinkageInfo.JCC_OP_SHIFT;
	      insn = I_JCC | op;
	      link.from_npc -= 1;
	      compress = SIZE_SHORTJCC_COMPRESS;
	    } else {
	      insn = I_JMP_8;
	      compress = SIZE_SHORTJMP_COMPRESS;
	    }
	    setNativeCode(link.from_npc-1, insn);
	    
	    // Compress the code
	    compressCode(from_npc+compress, end_npc, link.from_npc+1);
	    end_npc -= compress;

	    // Move all native code offsets between
	    // link.from_pc and end of bcinfo...
	    int i;
	    for (i = link.from_pc+1; i < bcinfo.length; i++) {
	      if (bcinfo[i] == null) continue;
	      bcinfo[i].native_pc -= compress;
	    }

	    // Move all native code offsets in the linkage
	    LinkageInfo link2;
	    for (link2 = linkage; link2 != null; link2 = link2.next) {
	      if (link2 == link) continue;
	      
	      if (link2.from_npc > from_npc) {
		// Jump starts within compressed code
		link2.from_npc -= compress;

		if (link2.target_npc == -1) continue;
		
		target_npc = link2.target_npc;
		if ((link2.flag & LinkageInfo.TARGET_RELOCATED) != 0) {
		  // Relocated: Get relative to code area
		  target_npc -= code_area;
		}

		if ((link2.flag & LinkageInfo.OFFSET_ABSOLUTE) == 0) {
		  // Relative
		  if (!((target_npc > from_npc) && (target_npc < native_pc))) {
		    // Target NOT in compressed code
		    link2.target_npc += compress;
		  }
		} else {
		  // Absolute
		  if ((target_npc > from_npc) && (target_npc < native_pc)) {
		    link2.target_npc -= compress;
		  }
		}
		  
	      } else {
		// Jump starts before compressed code
		if (link2.target_npc == -1) continue;

		target_npc = link2.target_npc;
		if ((link2.flag & LinkageInfo.TARGET_RELOCATED) != 0) {
		  // Relocated: Get relative to code area
		  target_npc -= code_area;
		}

		if ((target_npc > from_npc) && (target_npc < native_pc)) {
		  // Jump to within compressed code
		  link2.target_npc -= compress;
		}
	      }
	      
	    }
	    
	  }
	}

	// At this point, size of native code is fixed.
	// relocate native code in order to shrink native code space.
	{
	    int diff = NativeCodeReAlloc(end_npc);
	    if (diff != 0) {
		for (link = linkage; link != null; link = link.next) {
		    if ((link.flag & LinkageInfo.TARGET_RELOCATED) != 0) {
			if ((link.flag & LinkageInfo.OFFSET_ABSOLUTE) != 0) {
			    link.target_npc -= diff;
			} else {
			    link.target_npc += diff;
			}
		    }
		}
	    }
	}

	// Second pass: Resolve all linkage offsets
	for (link = linkage; link != null; link = link.next) {
	  //	  System.err.println("FIXLINK: 2nd pass: Resolving "+link.toString());
	  int from_npc = link.from_npc;
	  int offset;
	  
	  if (link.target_npc == -1) {
	    // Must resolve
	    int target_npc;
	    
	    target_npc = bcinfo[link.target_pc].native_pc;

	    if ((link.flag & LinkageInfo.OFFSET_ABSOLUTE) == 0) {
	      // -4 as offset is from next instruction (and offset is 4 bytes)
	      offset = (target_npc - from_npc - 4);
	    } else {
	      offset = target_npc + code_area;
	    }
	    
	    //	    System.err.println("FIXLINK: Resolving 0x"+Integer.toHexString(from_npc)+" -> 0x"+Integer.toHexString(target_npc)+", offset: "+offset);
	  
	    link.target_npc = offset;
	  } else {
	    offset = link.target_npc;
	  }

	  if ((link.flag & LinkageInfo.SHORTJMP) != 0) {
	    // Short jump: Plug one byte
	    offset += 3; // Offset size is now just one byte
	    if (!((offset <= 127) && (offset >= -128)))
	      throw new CompilerError("fixLinkage: Bad SHORTJMP offset in 3rd pass!");
	    setNativeCode(link.from_npc, offset);
	  } else {
	    // Plug 4 bytes
	    int plug = offset;
	    int pc = link.from_npc; 
	    setNativeCode(pc, plug & 0xff);
	    ++pc; plug >>= 8;
	    setNativeCode(pc, plug & 0xff);
	    ++pc; plug >>= 8;
	    setNativeCode(pc, plug & 0xff);
	    ++pc; plug >>= 8;
	    setNativeCode(pc, plug & 0xff);
	  }
	}
	  
	this.native_pc = end_npc;
    }

    //// X86 Stack Frame Layout:
    ////
    ////  %esp ->	  local(n)   ---+
    ////		  local(n+1)    |nlocals - args_size 
    ////		  ...           |
    ////		             ---+
    ////		  stack(0)   ---+
    ////		  stack(1)      |maxstack + 1
    ////	  -8	  ...           |
    ////	  -4	  stack(n)   ---+
    ////  %ebp -> 0x0	  prev %ebp
    ////	  0x4	  return %eip
    ////	  0x8	  current method
    ////	  0xc	  arg(0)     ---+
    ////	  0x10	  arg(1)        |args_size
    ////	          ...           |
    ////	  	  arg(n)     ---+
    ////		  ------
    ////

    final void genPrologue() {
	int frame_size = (nlocals + maxstack + 1) * 4;

	genPUSHR(R_EBP);
	genRR(I_MOV, R_ESP, R_EBP);	// movl %esp,%ebp
	genIR(I_SUB, frame_size, R_ESP);

	if ((access & ACC_SYNCHRONIZED) != 0) {
	    if ((access & ACC_STATIC) != 0) {
		genMR(I_MOV, R_EBP, 8, R_EAX);
		genPUSHM(R_EAX, 0);
	    } else {
		genPUSHM(R_EBP, base_offset(0));
	    }
	    genCALL(RT_addr[RT_monitorEnter], -1);
	    genIR(I_ADD, 4, R_ESP);
	}
    }

    private final void expandOneBytecode(BCinfo bc, ILnode il, int pc) {
	save_eflags = false;
	for(; il != null; il = il.next) {
	    if ((debug & DEBUG_NATIVE) != 0)
		System.err.println("0x" + Integer.toHexString(code_area + native_pc) + "\t" + il);

	    switch(il.op()) {
	    case ILnode.ELIMINATED:
	    case ILnode.NOP:
		continue;

	    case ILnode.ARRAYCHK:
		if (!omit_array_boundary_check)
		    emitARRAYCHK(il);
		break;

	    case ILnode.DIV0CHK:
		emitDIV0CHK(il);
		break;

	    case ILnode.MOVE:
		emitMOVE(il);
		break;

	    case ILnode.LD:
		emitLD(il, ILnode.LD);
		break;
  	    case ILnode.LDSH:
  		emitLD(il, ILnode.LDSH);
  		break;
  	    case ILnode.LDSB:
  		emitLD(il, ILnode.LDSB);
  		break;
  	    case ILnode.LDUH:
  		emitLD(il, ILnode.LDUH);
  		break;
	    case ILnode.FLD:
		emitFLD(il);
		break;
	    case ILnode.DLD:
		emitDLD(il);
		break;

	    case ILnode.ST:
		emitST(il, ILnode.ST);
		break;
	    case ILnode.STB:
		emitST(il, ILnode.STB);
		break;
	    case ILnode.STH:
		emitST(il, ILnode.STH);
		break;
	    case ILnode.FST:
		emitFST(il);
		break;
	    case ILnode.DST:
		emitDST(il);
		break;

	    case ILnode.ADD:
		if (il.tagL() == TAG_PARAM && il.tagD() == TAG_PARAM) {
		    /* special case. {get|put}{field|static} long */
		    if (il.tagR() != TAG_CONST || il.rval != 4 ||
			il.lval != 0 || il.dval != 0)
			throw new CompilerError("add:" + il);
		    genIR(I_ADD, 4, R_EAX);
		    break;
		}
		if (il.tagR() == TAG_CONST && emitINCDEC(il, il.rval))
		    break;
		emitINSN(il, I_ADD, true);
		break;

	    case ILnode.FADD:
		emitFInsn(il, 0xd8, 0x00);
		break;

	    case ILnode.DADD:
		emitDInsn(il, 0xdc, 0x00);
		break;

	    case ILnode.ADDL:
		save_eflags = true;
		emitINSN(il, I_ADD, true);
		break;

	    case ILnode.ADDH:
		emitINSN(il, I_ADC, true);
		break;

	    case ILnode.CMP:
		save_eflags = true;
		emitCMP(il);
		break;

	    case ILnode.TEST:
		save_eflags = true;
		emitTEST(il);
		break;

	    case ILnode.SUB:
		if (il.tagR() == TAG_CONST && emitINCDEC(il, -il.rval))
		    break;
		emitINSN(il, I_SUB, false);
		break;

	    case ILnode.FSUB:
		emitFInsn(il, 0xd8, 0x20);
		break;

	    case ILnode.DSUB:
		emitDInsn(il, 0xdc, 0x20);
		break;

	    case ILnode.SUBL:
		save_eflags = true;
		emitINSN(il, I_SUB, false);
		break;

	    case ILnode.SUBH:
		emitINSN(il, I_SBB, false);
		break;

	    case ILnode.MUL:
		emitMUL(il);
		break;

	    case ILnode.FMUL:
		emitFInsn(il, 0xd8, 0x08);
		break;

	    case ILnode.DMUL:
		emitDInsn(il, 0xdc, 0x08);
		break;

	    case ILnode.DIV:
		emitDIV(il, R_EAX);
		break;

	    case ILnode.FDIV:
		emitFInsn(il, 0xd8, 0x30);
		break;

	    case ILnode.DDIV:
		emitDInsn(il, 0xdc, 0x30);
		break;

	    case ILnode.IREM:
		emitDIV(il, R_EDX);
		break;

	    case ILnode.AND:
		emitINSN(il, I_AND, true);
		break;

	    case ILnode.OR:
		emitINSN(il, I_OR, true);
		break;

	    case ILnode.XOR:
		emitINSN(il, I_XOR, true);
		break;

	    case ILnode.SLL:
		if (eliminateIndexScale(il))
		    continue;
		else
		    emitSHIFT(il, I_SHL);
		break;

	    case ILnode.SRA:
		emitSHIFT(il, I_SAR);
		break;

	    case ILnode.SRL:
		emitSHIFT(il, I_SHR);
		break;

	    case ILnode.FNEG:
		emitFNEG(il);
		break;
	    case ILnode.DNEG:
		emitDNEG(il);
		break;
	    case ILnode.I2B:
		emitI2B(il);
		break;
	    case ILnode.I2S:
		emitI2S(il);
		break;
	    case ILnode.I2C:
  		il.setL(TAG_CONST, 0xffff);
  		emitINSN(il, I_AND, true);
		break;
	    case ILnode.I2F:
		emitI2F(il, ILnode.I2F);
		break;
	    case ILnode.I2D:
		emitI2F(il, ILnode.I2D);
		break;
	    case ILnode.F2D:
		emitF2D(il);
		break;
	    case ILnode.D2F:
		emitD2F(il);
		break;
	    case ILnode.F2I:
		// emitF2Iinline(il, ILnode.F2I);
		emitF2I(il, ILnode.F2I, pc);
		break;
	    case ILnode.D2I:
		// emitF2Iinline(il, ILnode.D2I);
		emitF2I(il, ILnode.D2I, pc);
		break;

	    case ILnode.FCMP:
		emitFCMP(il);
		break;

	    case ILnode.DCMP:
		emitDCMP(il);
		break;

	    case ILnode.BRANCH:
		regSave();
		emitBRANCH(il, pc);
		break;

	    case ILnode.TBLSW:
		regSave();
		emitTBLSW(il, pc);
		break;

	    case ILnode.RET:
		/* FIX ME : violate JVM specification */
		genCode(I_RET);
		break;

	    case ILnode.CALL:
		emitCALL(il, pc, bc.stack_size);
		break;

	    case ILnode.RETURN:
		emitRETURN(il, pc);
		break;

	    case ILnode.INLINE_PROLOGUE:
	      emitInlinePrologue(il, pc);
	      break;

	    case ILnode.INLINE_EPILOGUE:
	      emitInlineEpilogue(il, pc);
	      break;

	    default:
		throw new CompilerError("unimplemented yet:" + il);
	    }
	    freeRegs(il);
	}
    }

    final void genNativeCode() {
	NativeCodeAlloc(bytecode_length * 20);

	genPrologue();

//	if (thisHandle != null) {
//	    expandOneBytecode(null, thisHandle, 0);
//	}

	for(int pc = 0; pc < bcinfo.length; pc++) {
	    BCinfo bc = bcinfo[pc];
	    if (bc == null)
		continue;
	    bc.native_pc = native_pc;
	    if ((bc.flag & BCinfo.FLAG_BRANCH_TARGET) != 0) {
		regSave();
		bc.native_pc = native_pc;
		regInvalidate();
	    } else {
		bc.native_pc = native_pc;
	    }
	    if ((debug & DEBUG_NATIVE) != 0) {
		if ((bc.flag & BCinfo.FLAG_BRANCH_TARGET) != 0) {
		    System.err.println("### BB beg ###");
		}
		System.err.println("***" + pc + "\t" + opcName(pc));
	    }
	    //	    if ((bc.flag & BCinfo.FLAG_BEGIN_BASIC_BLOCK) != 0) {
	    if ((bc.flag & BCinfo.FLAG_BRANCH_TARGET) != 0) {
		regInvalidate();
	    }
	    expandOneBytecode(bc, bc.next, pc);
	}
	fixLinkage();
	ExceptionHandler.Set(this);
    }
}
