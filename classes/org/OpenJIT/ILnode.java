//
// Copyright (C) 1996 1997 1998 1999 by FUJITSU LABORATRIES LTD.
//
// $Revision: 1.14 $
// $Date: 1999/09/24 04:19:30 $
// $Author: kouya $
//

package org.OpenJIT;

public final class ILnode extends LinkedList implements Constants
{
    final static byte ELIMINATED = 1;
    final static byte NOP = ELIMINATED + 1;
    final static byte ARRAYCHK = NOP + 1;
    final static byte DIV0CHK = ARRAYCHK + 1;
    final static byte BRANCH = DIV0CHK + 1;
    final static byte CALL = BRANCH + 1;
    final static byte RETURN = CALL + 1;
    final static byte TBLSW = RETURN + 1;
    final static byte RET = TBLSW + 1;
    final static byte MOVE = RET + 1;

    final static byte LD = MOVE + 1;
    final static byte LDSH = LD + 1;
    final static byte LDSB = LDSH + 1;
    final static byte LDUH = LDSB + 1;
    final static byte FLD = LDUH + 1;
    final static byte DLD = FLD + 1;

    final static byte ST = DLD + 1;
    final static byte STB = ST + 1;
    final static byte STH = STB + 1;
    final static byte FST = STH + 1;
    final static byte DST = FST + 1;

    final static byte ADD = DST + 1;
    final static byte ADDL = ADD + 1;
    final static byte ADDH = ADDL + 1;
    final static byte SUB = ADDH + 1;
    final static byte SUBL = SUB + 1;
    final static byte SUBH = SUBL + 1;
    final static byte MUL = SUBH + 1;
    final static byte DIV = MUL + 1;
    final static byte IREM = DIV + 1;

    final static byte AND = IREM + 1;
    final static byte OR = AND + 1;
    final static byte XOR = OR + 1;

    final static byte SLL = XOR + 1;
    final static byte SRA = SLL + 1;
    final static byte SRL = SRA + 1;

    final static byte CMP = SRL + 1;
    final static byte TEST = CMP + 1;

    final static byte FADD = TEST + 1;
    final static byte FSUB = FADD + 1;
    final static byte FMUL = FSUB + 1;
    final static byte FDIV = FMUL + 1;
    final static byte FNEG = FDIV + 1;
    final static byte FCMP = FNEG + 1;

    final static byte DADD = FCMP + 1;
    final static byte DSUB = DADD + 1;
    final static byte DMUL = DSUB + 1;
    final static byte DDIV = DMUL + 1;
    final static byte DNEG = DDIV + 1;
    final static byte DCMP = DNEG + 1;

    final static byte I2B = DCMP + 1;
    final static byte I2C = I2B + 1;
    final static byte I2S = I2C + 1;
    final static byte I2F = I2S + 1;
    final static byte I2D = I2F + 1;
    final static byte F2I = I2D + 1;
    final static byte D2I = F2I + 1;
    final static byte F2D = D2I + 1;
    final static byte D2F = F2D + 1;

    final static byte INLINE_PROLOGUE = D2F + 1;
    final static byte INLINE_EPILOGUE = INLINE_PROLOGUE + 1;

    final static int OFFSET_OP = 24;
    final static int OFFSET_D = 16;
    final static int OFFSET_L = 8;
    final static int OFFSET_R = 0;

    private int op_and_tag;
    int dval, lval, rval;

    final int op() {
	return op_and_tag >>> OFFSET_OP;
    }

    final int dtype() {
	return (op_and_tag >> OFFSET_D) & 0xff;
    }

    final int ltype() {
	return (op_and_tag >> OFFSET_L) & 0xff;
    }

    final int rtype() {
	return (op_and_tag >> OFFSET_R) & 0xff;
    }

    final int tagD() {
	return (op_and_tag >> OFFSET_D) & TAG_MASK;
    }

    final int tagL() {
	return (op_and_tag >> OFFSET_L) & TAG_MASK;
    }

    final int tagR() {
	return (op_and_tag >> OFFSET_R) & TAG_MASK;
    }

    final int typeD() {
	return (op_and_tag >> OFFSET_D) & TYPE_MASK;
    }

    final int typeL() {
	return (op_and_tag >> OFFSET_L) & TYPE_MASK;
    }

    final int typeR() {
	return (op_and_tag >> OFFSET_R) & TYPE_MASK;
    }

    static private final String name[] = {
	"???",
	"xxx",
	"nop",
	"arraychk",
	"div0chk",
	"branch",
	"call",
	"return",
	"tblsw",
	"ret",
	"move",
	"ld",
	"ldsh",
	"ldsb",
	"lduh",
	"fld",
	"dld",
	"st",
	"stb",
	"sth",
	"fst",
	"dst",
	"add",
	"addl",
	"addh",
	"sub",
	"subl",
	"subh",
	"mul",
	"div",
	"irem",
	"and",
	"or",
	"xor",
	"sll",
	"sra",
	"srl",
	"cmp",
	"test",
	"fadd",
	"fsub",
	"fmul",
	"fdiv",
	"fneg",
	"fcmp",
	"dadd",
	"dsub",
	"dmul",
	"ddiv",
	"dneg",
	"dcmp",
	"i2b",
	"i2c",
	"i2s",
	"i2f",
	"i2d",
	"f2i",
	"d2i",
	"f2d",
	"d2f",
	"<inline prologue>",
	"<inline epilogue>"
    };

    public ILnode() {
    }

    public ILnode(LinkedList ll) {
	ll.next = this;
    }

    public final ILnode insn(int op, int dtype, int dval, int ltype, int lval, int rtype, int rval) {
	this.op_and_tag = (op << OFFSET_OP) | (dtype << OFFSET_D) | (ltype << OFFSET_L) | rtype;
	this.dval = dval;
	this.lval = lval;
	this.rval = rval;
	return this;
    }

    public final ILnode insn2(int op, int dtype, int dval, int rtype, int rval) {
	this.op_and_tag = (op << OFFSET_OP) | (dtype << OFFSET_D) | rtype;
	this.dval = dval;
	this.rval = rval;
	return this;
    }

    public final ILnode move(int dtype, int dval, int rtype, int rval) {
	this.op_and_tag = (MOVE << OFFSET_OP) | (dtype << OFFSET_D) | rtype;
	this.dval = dval;
	this.rval = rval;
	return this;
    }

    public final ILnode moveStack(int dval, int rval) {
	this.op_and_tag = (MOVE << OFFSET_OP) | (TAG_STACK << OFFSET_D) | TAG_REF | TAG_STACK;
	this.dval = dval;
	this.lval = -1;
	this.rval = rval;
	return this;
    }

    public final ILnode call(int dtype, int dval, int func) {
	this.op_and_tag = (CALL << OFFSET_OP) | ((dtype & 0xff) << OFFSET_D);
	this.dval = dval;
	this.rval = func;
	return this;
    }

    /* for functions need to resolve */
    public final ILnode call(int dtype, int dval, int func, int index) {
	this.op_and_tag = (CALL << OFFSET_OP) | ((dtype & 0xff) << OFFSET_D);
	this.dval = dval;
	this.lval = index;
	this.rval = func;
	return this;
    }

    public final ILnode branch(int cond, int target_pc) {
	this.op_and_tag = (BRANCH << OFFSET_OP);
	// this.dval = 0;
	this.lval = cond;
	this.rval = target_pc;
	return this;
    }

    public final ILnode jsr(int target_pc) {
	this.op_and_tag = (BRANCH << OFFSET_OP) | (TAG_SI << OFFSET_D);
	// this.dval = 0;
	this.lval = CC_JSR;
	this.rval = target_pc;
	return this;
    }

    public final ILnode nop() {
	this.op_and_tag = (NOP << OFFSET_OP);
	return this;
    }

    public final ILnode inline_prologue(int methodblock) {
      this.op_and_tag = (INLINE_PROLOGUE << OFFSET_OP);
      this.rval = methodblock;
      return this;
    }
    
    public final ILnode inline_epilogue(int methodblock) {
      this.op_and_tag = (INLINE_EPILOGUE << OFFSET_OP);
      this.rval = methodblock;
      return this;
    }

    final void remove() {
	op_and_tag = (ELIMINATED << OFFSET_OP) | (op_and_tag & 0xffffff);
    }

    final void setD(int type, int val) {
	op_and_tag = (op_and_tag & 0xff00ffff) | ((type & 0xff) << OFFSET_D);
	dval = val;
    }

    final void setL(int type, int val) {
	op_and_tag = (op_and_tag & 0xffff00ff) | ((type & 0xff) << OFFSET_L);
	lval = val;
    }

    final void setR(int type, int val) {
	op_and_tag = (op_and_tag & 0xffffff00) | ((type & 0xff) << OFFSET_R);
	rval = val;
    }

    final void setConst(int val) {
	op_and_tag = (MOVE << OFFSET_OP) | (op_and_tag & 0x00ff0000);
	lval = 0;
	rval = val;
    }

    final void swapLR() {
	int v = op_and_tag;

	op_and_tag = (v & 0xffff0000) | ((v & 0xff00) >> 8) | ((v & 0xff) << 8);
	v = rval;
	rval = lval;
	lval = v;
    }

    final void referD() {
	op_and_tag |= (int)(TAG_REF << OFFSET_D);
    }

    final boolean isNotReferD() {
	return (op_and_tag & (int)(TAG_REF << OFFSET_D)) == 0;
    }

    final void free(int flag) {
	op_and_tag |= flag & ((TAG_FREE << OFFSET_D)|(TAG_FREE << OFFSET_L)|(TAG_FREE << OFFSET_R));
    }

    final boolean isSTORE() {
	int op = (int)(op_and_tag >> OFFSET_OP);
	return ((op >= ST) && (op <= DST));
    }

    static final String tag2String(int type, int val) {
	StringBuffer buf = null;
	if ((type & TAG_REF) != 0)
	    buf = new StringBuffer("&");
	else
	    buf = new StringBuffer("");
	if ((type & TAG_FREE) != 0)
	    buf.append("@");
	switch(type & TAG_MASK) {
	case TAG_CONST:
	    return buf.append(val).toString();
	case TAG_STACK:
	    buf.append("%s");
	    val &= 0x1ffff;
	    break;
	case TAG_LOCAL:
	    buf.append("%v");
	    val &= 0x1ffff;
	    break;
	case TAG_PARAM:
	    buf.append("%r");
	    val &= 0x1ffff;
	    break;
	}
	switch(type & TYPE_MASK) {
	case TYPE_INT:
	    buf.append("i");
	    break;
	case TYPE_FLOAT:
	    buf.append("f");
	    break;
	case TYPE_DOUBLE:
	    buf.append("d");
	    break;
	default:
	}
	buf.append(val);
	return buf.toString();
    }

    public String toString() {
	StringBuffer buf = null;
	int op = op();

	if (name == null)
	    return "";
	if (name[op] == null)
	    return "";
	buf = new StringBuffer(name[op]);
	buf.append("\t");
	if (op >= ST && op <= DST) {
	    buf.append(tag2String(dtype(), dval));
	    buf.append(",[").append(tag2String(ltype(), lval));
	    buf.append("+").append(tag2String(rtype(), rval)).append("]");
	} else if (op >= LD && op <= DLD) {
	    buf.append("[").append(tag2String(ltype(), lval));
	    buf.append("+").append(tag2String(rtype(), rval)).append("],");
	    buf.append(tag2String(dtype(), dval));
	} else if (op == BRANCH) {
	    switch(lval) {
	    case CC_A: buf.append("A "); break;
	    case CC_E: buf.append("E "); break;
	    case CC_NE: buf.append("NE "); break;
	    case CC_L: buf.append("L "); break;
	    case CC_LE: buf.append("LE "); break;
	    case CC_G: buf.append("G "); break;
	    case CC_GE: buf.append("GE "); break;
		/* floating */
	    case CC_FA: buf.append("FA "); break;
	    case CC_FE: buf.append("FE "); break;
	    case CC_FNE: buf.append("FNE "); break;
	    case CC_FL: buf.append("FL "); break;
	    case CC_FLE: buf.append("FLE "); break;
	    case CC_FG: buf.append("FG "); break;
	    case CC_FGE: buf.append("FGE "); break;
		/* Not NaN case */
	    case CC_NFL: buf.append("NFL "); break;
	    case CC_NFLE: buf.append("NFLE "); break;
	    case CC_NFG: buf.append("NFG "); break;
	    case CC_NFGE: buf.append("NFGE "); break;
		/* special */
	    case CC_LU: buf.append("LU "); break;
	    case CC_LEU: buf.append("LEU "); break;
	    case CC_GU: buf.append("GU "); break;
	    case CC_GEU: buf.append("GEU "); break;
		/* others */
	    case CC_TBLOB: buf.append("TBLOB "); break;
	    case CC_TBLSW: buf.append("TBLSW "); break;
	    case CC_JSR: buf.append("JSR "); break;
	    }
	    buf.append(tag2String(rtype(), rval));
	} else {
	    if (op != MOVE && op != ELIMINATED) {
		buf.append(tag2String(ltype(), lval)).append(" ");
	    }
	    buf.append(tag2String(rtype(), rval));
	    if ((dtype() & TAG_MASK) != TAG_CONST) {
		buf.append(" -> ").append(tag2String(dtype(), dval));
	    }
	}
	return buf.toString();
    }
}
