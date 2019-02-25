//
// Copyright (C) 1996 1997 1998 1999 by FUJITSU LABORATRIES LTD.
//
// $Revision: 1.32 $
// $Date: 1999/10/25 03:24:35 $
// $Author: kouya $
//

package org.OpenJIT;

abstract class SparcCode extends OptimizeRTL {

    // software trap from "/usr/include/sys/trap.h"
    final static int ST_DIV0 = 0x02;
    final static int ST_RANGE_CHECK = 0x05;

    // SPARC V8 instruction code
    final static int I_CALL    = (1<<30);

    final static int I_ADD     = ((2<<30)|( 0<<19));	/* op3=000000 */
    final static int I_AND     = ((2<<30)|( 1<<19));	/* op3=000001 */
    final static int I_OR      = ((2<<30)|( 2<<19));	/* op3=000010 */
    final static int I_XOR     = ((2<<30)|( 3<<19));	/* op3=000011 */
    final static int I_SUB     = ((2<<30)|( 4<<19));	/* op3=000100 */
    final static int I_ANDN    = ((2<<30)|( 5<<19));	/* op3=000101 */
    final static int I_ORN     = ((2<<30)|( 6<<19));	/* op3=000110 */
    final static int I_XNOR    = ((2<<30)|( 7<<19));	/* op3=000111 */
    final static int I_ADDX    = ((2<<30)|( 8<<19));	/* op3=001000 */
    final static int I_UMUL    = ((2<<30)|(10<<19));	/* op3=001010 */
    final static int I_SMUL    = ((2<<30)|(11<<19));	/* op3=001011 */
    final static int I_SUBX    = ((2<<30)|(12<<19));	/* op3=001100 */
    final static int I_UDIV    = ((2<<30)|(14<<19));	/* op3=001110 */
    final static int I_SDIV    = ((2<<30)|(15<<19));	/* op3=001111 */
    final static int I_ADDCC   = ((2<<30)|(16<<19));	/* op3=010000 */
    final static int I_ANDCC   = ((2<<30)|(17<<19));	/* op3=010001 */
    final static int I_ORCC    = ((2<<30)|(18<<19));	/* op3=010010 */
    final static int I_XORCC   = ((2<<30)|(19<<19));	/* op3=010011 */
    final static int I_SUBCC   = ((2<<30)|(20<<19));	/* op3=010100 */
    final static int I_ANDNCC  = ((2<<30)|(21<<19));	/* op3=010101 */
    final static int I_ORNCC   = ((2<<30)|(22<<19));	/* op3=010110 */
    final static int I_XNORCC  = ((2<<30)|(23<<19));	/* op3=010111 */
    final static int I_ADDXCC  = ((2<<30)|(24<<19));	/* op3=011000 */
    final static int I_UMULCC  = ((2<<30)|(26<<19));	/* op3=011010 */
    final static int I_SMULCC  = ((2<<30)|(27<<19));	/* op3=011011 */
    final static int I_SUBXCC  = ((2<<30)|(28<<19));	/* op3=011100 */
    final static int I_UDIVCC  = ((2<<30)|(30<<19));	/* op3=011110 */
    final static int I_SDIVCC  = ((2<<30)|(31<<19));	/* op3=011111 */
    final static int I_TADDCC  = ((2<<30)|(32<<19));	/* op3=100000 */
    final static int I_TSUBCC  = ((2<<30)|(33<<19));	/* op3=100001 */
    final static int I_TADDCCTV= ((2<<30)|(34<<19));	/* op3=100010 */
    final static int I_TSUBCCTV= ((2<<30)|(35<<19));	/* op3=100011 */
    final static int I_MULSCC  = ((2<<30)|(36<<19));	/* op3=100100 */
    final static int I_SLL     = ((2<<30)|(37<<19));	/* op3=100101 */
    final static int I_SRL     = ((2<<30)|(38<<19));	/* op3=100110 */
    final static int I_SRA     = ((2<<30)|(39<<19));	/* op3=100111 */
    final static int I_RDY     = ((2<<30)|(40<<19));	/* op3=101000 */
    final static int I_RDPSR   = ((2<<30)|(41<<19));	/* op3=101001 */
    final static int I_RDWIM   = ((2<<30)|(42<<19));	/* op3=101010 */
    final static int I_RDTBR   = ((2<<30)|(43<<19));	/* op3=101011 */
    final static int I_WRY     = ((2<<30)|(48<<19));	/* op3=110000 */
    final static int I_WRPSR   = ((2<<30)|(49<<19));	/* op3=110001 */
    final static int I_WRWIM   = ((2<<30)|(50<<19));	/* op3=110010 */
    final static int I_WRTBR   = ((2<<30)|(51<<19));	/* op3=110011 */
    final static int I_CPOP1   = ((2<<30)|(54<<19));	/* op3=110110 */
    final static int I_CPOP2   = ((2<<30)|(55<<19));	/* op3=110111 */
    final static int I_JMPL    = ((2<<30)|(56<<19));	/* op3=111000 */
    final static int I_RETT    = ((2<<30)|(57<<19));	/* op3=111001 */
    final static int I_FLUSH   = ((2<<30)|(59<<19));	/* op3=111011 */
    final static int I_SAVE    = ((2<<30)|(60<<19));	/* op3=111100 */
    final static int I_RESTORE = ((2<<30)|(61<<19));	/* op3=111101 */

    final static int I_LD      = ((3<<30)|( 0<<19));	/* op3=000000 */
    final static int I_LDUB    = ((3<<30)|( 1<<19));	/* op3=000001 */
    final static int I_LDUH    = ((3<<30)|( 2<<19));	/* op3=000010 */
    final static int I_LDD     = ((3<<30)|( 3<<19));	/* op3=000011 */
    final static int I_ST      = ((3<<30)|( 4<<19));	/* op3=000100 */
    final static int I_STB     = ((3<<30)|( 5<<19));	/* op3=000101 */
    final static int I_STH     = ((3<<30)|( 6<<19));	/* op3=000110 */
    final static int I_STD     = ((3<<30)|( 7<<19));	/* op3=000111 */
    final static int I_LDSB    = ((3<<30)|( 9<<19));	/* op3=001001 */
    final static int I_LDSH    = ((3<<30)|(10<<19));	/* op3=001010 */
    final static int I_LDSTUB  = ((3<<30)|(13<<19));	/* op3=001101 */
    final static int I_SWAP    = ((3<<30)|(15<<19));	/* op3=001111 */
    final static int I_LDA     = ((3<<30)|(16<<19));	/* op3=010000 */
    final static int I_LDUBA   = ((3<<30)|(17<<19));	/* op3=010001 */
    final static int I_LDUHA   = ((3<<30)|(18<<19));	/* op3=010010 */
    final static int I_LDDA    = ((3<<30)|(19<<19));	/* op3=010011 */
    final static int I_STA     = ((3<<30)|(20<<19));	/* op3=010100 */
    final static int I_STBA    = ((3<<30)|(21<<19));	/* op3=010101 */
    final static int I_STHA    = ((3<<30)|(22<<19));	/* op3=010110 */
    final static int I_STDA    = ((3<<30)|(23<<19));	/* op3=010111 */
    final static int I_LDSBA   = ((3<<30)|(25<<19));	/* op3=011001 */
    final static int I_LDSHA   = ((3<<30)|(26<<19));	/* op3=011010 */
    final static int I_LDSTUBA = ((3<<30)|(29<<19));	/* op3=011101 */
    final static int I_SWAPA   = ((3<<30)|(31<<19));	/* op3=011111 */
    final static int I_LDF     = ((3<<30)|(32<<19));	/* op3=100000 */
    final static int I_LDFSR   = ((3<<30)|(33<<19));	/* op3=100001 */
    final static int I_LDDF    = ((3<<30)|(35<<19));	/* op3=100011 */
    final static int I_STF     = ((3<<30)|(36<<19));	/* op3=100100 */
    final static int I_STFSR   = ((3<<30)|(37<<19));	/* op3=100101 */
    final static int I_STDFQ   = ((3<<30)|(38<<19));	/* op3=100110 */
    final static int I_STDF    = ((3<<30)|(39<<19));	/* op3=100111 */
    final static int I_LDC     = ((3<<30)|(48<<19));	/* op3=110000 */
    final static int I_LDCSR   = ((3<<30)|(49<<19));	/* op3=110001 */
    final static int I_LDDC    = ((3<<30)|(51<<19));	/* op3=110011 */
    final static int I_STC     = ((3<<30)|(52<<19));	/* op3=110100 */
    final static int I_STCSR   = ((3<<30)|(53<<19));	/* op3=110101 */
    final static int I_STDCQ   = ((3<<30)|(54<<19));	/* op3=110110 */
    final static int I_STDC    = ((3<<30)|(55<<19));	/* op3=110111 */

    final static int I_ANNUL   = (1 << 29);

    final static int I_BN      =  ((2<<22)|(0<<25));	/* cond=0000 */
    final static int I_BE      =  ((2<<22)|(1<<25));	/* cond=0001 */
    final static int I_BLE     =  ((2<<22)|(2<<25));	/* cond=0010 */
    final static int I_BL      =  ((2<<22)|(3<<25));	/* cond=0011 */
    final static int I_BLEU    =  ((2<<22)|(4<<25));	/* cond=0100 */
    final static int I_BCS     =  ((2<<22)|(5<<25));	/* cond=0101 */
    final static int I_BNEG    =  ((2<<22)|(6<<25));	/* cond=0110 */
    final static int I_BVS     =  ((2<<22)|(7<<25));	/* cond=0111 */
    final static int I_BA      =  ((2<<22)|(8<<25));	/* cond=1000 */
    final static int I_BNE     =  ((2<<22)|(9<<25));	/* cond=1001 */
    final static int I_BG      = ((2<<22)|(10<<25));	/* cond=1010 */
    final static int I_BGE     = ((2<<22)|(11<<25));	/* cond=1011 */
    final static int I_BGU     = ((2<<22)|(12<<25));	/* cond=1100 */
    final static int I_BCC     = ((2<<22)|(13<<25));	/* cond=1101 */
    final static int I_BPOS    = ((2<<22)|(14<<25));	/* cond=1110 */
    final static int I_BVC     = ((2<<22)|(15<<25));	/* cond=1111 */

    final static int I_CBN     =  ((7<<22)|(0<<25));	/* cond=0000 */
    final static int I_CB123   =  ((7<<22)|(1<<25));	/* cond=0001 */
    final static int I_CB12    =  ((7<<22)|(2<<25));	/* cond=0010 */
    final static int I_CB13    =  ((7<<22)|(3<<25));	/* cond=0011 */
    final static int I_CB1     =  ((7<<22)|(4<<25));	/* cond=0100 */
    final static int I_CB23    =  ((7<<22)|(5<<25));	/* cond=0101 */
    final static int I_CB2     =  ((7<<22)|(6<<25));	/* cond=0110 */
    final static int I_CB3     =  ((7<<22)|(7<<25));	/* cond=0111 */
    final static int I_CBA     =  ((7<<22)|(8<<25));	/* cond=1000 */
    final static int I_CB0     =  ((7<<22)|(9<<25));	/* cond=1001 */
    final static int I_CB03    = ((7<<22)|(10<<25));	/* cond=1010 */
    final static int I_CB02    = ((7<<22)|(11<<25));	/* cond=1011 */
    final static int I_CB023   = ((7<<22)|(12<<25));	/* cond=1100 */
    final static int I_CB01    = ((7<<22)|(13<<25));	/* cond=1101 */
    final static int I_CB013   = ((7<<22)|(14<<25));	/* cond=1110 */
    final static int I_CB012   = ((7<<22)|(15<<25));	/* cond=1111 */

    final static int I_FBN     =  ((6<<22)|(0<<25));	/* cond=0000 */
    final static int I_FBNE    =  ((6<<22)|(1<<25));	/* cond=0001 */
    final static int I_FBLG    =  ((6<<22)|(2<<25));	/* cond=0010 */
    final static int I_FBUL    =  ((6<<22)|(3<<25));	/* cond=0011 */
    final static int I_FBL     =  ((6<<22)|(4<<25));	/* cond=0100 */
    final static int I_FBUG    =  ((6<<22)|(5<<25));	/* cond=0101 */
    final static int I_FBG     =  ((6<<22)|(6<<25));	/* cond=0110 */
    final static int I_FBU     =  ((6<<22)|(7<<25));	/* cond=0111 */
    final static int I_FBA     =  ((6<<22)|(8<<25));	/* cond=1000 */
    final static int I_FBE     =  ((6<<22)|(9<<25));	/* cond=1001 */
    final static int I_FBUE    = ((6<<22)|(10<<25));	/* cond=1010 */
    final static int I_FBGE    = ((6<<22)|(11<<25));	/* cond=1011 */
    final static int I_FBUGE   = ((6<<22)|(12<<25));	/* cond=1100 */
    final static int I_FBLE    = ((6<<22)|(13<<25));	/* cond=1101 */
    final static int I_FBULE   = ((6<<22)|(14<<25));	/* cond=1110 */
    final static int I_FBO     = ((6<<22)|(15<<25));	/* cond=1111 */

    final static int I_TN      =  ((2<<30)|(0x3a<<19)|(0<<25));	/* cond=0000 */
    final static int I_TE      =  ((2<<30)|(0x3a<<19)|(1<<25));	/* cond=0001 */
    final static int I_TLE     =  ((2<<30)|(0x3a<<19)|(2<<25));	/* cond=0010 */
    final static int I_TL      =  ((2<<30)|(0x3a<<19)|(3<<25));	/* cond=0011 */
    final static int I_TLEU    =  ((2<<30)|(0x3a<<19)|(4<<25));	/* cond=0100 */
    final static int I_TCS     =  ((2<<30)|(0x3a<<19)|(5<<25));	/* cond=0101 */
    final static int I_TNEG    =  ((2<<30)|(0x3a<<19)|(6<<25));	/* cond=0110 */
    final static int I_TVS     =  ((2<<30)|(0x3a<<19)|(7<<25));	/* cond=0111 */
    final static int I_TA      =  ((2<<30)|(0x3a<<19)|(8<<25));	/* cond=1000 */
    final static int I_TNE     =  ((2<<30)|(0x3a<<19)|(9<<25));	/* cond=1001 */
    final static int I_TG      = ((2<<30)|(0x3a<<19)|(10<<25));	/* cond=1010 */
    final static int I_TGE     = ((2<<30)|(0x3a<<19)|(11<<25));	/* cond=1011 */
    final static int I_TGU     = ((2<<30)|(0x3a<<19)|(12<<25));	/* cond=1100 */
    final static int I_TCC     = ((2<<30)|(0x3a<<19)|(13<<25));	/* cond=1101 */
    final static int I_TPOS    = ((2<<30)|(0x3a<<19)|(14<<25));	/* cond=1110 */
    final static int I_TVC     = ((2<<30)|(0x3a<<19)|(15<<25));	/* cond=1111 */

    final static int I_FMOVS   =  ((2<<30)|(0x34<<19)|(1<<5));	/* opf=000000001 */
    final static int I_FNEGS   =  ((2<<30)|(0x34<<19)|(5<<5));	/* opf=000000101 */
    final static int I_FABSS   =  ((2<<30)|(0x34<<19)|(9<<5));	/* opf=000001001 */
    final static int I_FSQRTS  = ((2<<30)|(0x34<<19)|(41<<5));	/* opf=000101001 */
    final static int I_FSQRTD  = ((2<<30)|(0x34<<19)|(42<<5));	/* opf=000101010 */
    final static int I_FSQRTQ  = ((2<<30)|(0x34<<19)|(43<<5));	/* opf=000101011 */
    final static int I_FADDS   = ((2<<30)|(0x34<<19)|(65<<5));	/* opf=001000001 */
    final static int I_FADDD   = ((2<<30)|(0x34<<19)|(66<<5));	/* opf=001000010 */
    final static int I_FADDQ   = ((2<<30)|(0x34<<19)|(67<<5));	/* opf=001000011 */
    final static int I_FSUBS   = ((2<<30)|(0x34<<19)|(69<<5));	/* opf=001000101 */
    final static int I_FSUBD   = ((2<<30)|(0x34<<19)|(70<<5));	/* opf=001000110 */
    final static int I_FSUBQ   = ((2<<30)|(0x34<<19)|(71<<5));	/* opf=001000111 */
    final static int I_FMULS   = ((2<<30)|(0x34<<19)|(73<<5));	/* opf=001001001 */
    final static int I_FMULD   = ((2<<30)|(0x34<<19)|(74<<5));	/* opf=001001010 */
    final static int I_FMULQ   = ((2<<30)|(0x34<<19)|(75<<5));	/* opf=001001011 */
    final static int I_FDIVS   = ((2<<30)|(0x34<<19)|(77<<5));	/* opf=001001101 */
    final static int I_FDIVD   = ((2<<30)|(0x34<<19)|(78<<5));	/* opf=001001110 */
    final static int I_FDIVQ   = ((2<<30)|(0x34<<19)|(79<<5));	/* opf=001001111 */
    final static int I_FSMULD  =((2<<30)|(0x34<<19)|(105<<5));	/* opf=001101001 */
    final static int I_FDMULQ  =((2<<30)|(0x34<<19)|(110<<5));	/* opf=001101110 */
    final static int I_FITOS   =((2<<30)|(0x34<<19)|(196<<5));	/* opf=011000100 */
    final static int I_FDTOS   =((2<<30)|(0x34<<19)|(198<<5));	/* opf=011000110 */
    final static int I_FQTOS   =((2<<30)|(0x34<<19)|(199<<5));	/* opf=011000111 */
    final static int I_FITOD   =((2<<30)|(0x34<<19)|(200<<5));	/* opf=011001000 */
    final static int I_FSTOD   =((2<<30)|(0x34<<19)|(201<<5));	/* opf=011001001 */
    final static int I_FQTOD   =((2<<30)|(0x34<<19)|(203<<5));	/* opf=011001011 */
    final static int I_FITOQ   =((2<<30)|(0x34<<19)|(204<<5));	/* opf=011001100 */
    final static int I_FSTOQ   =((2<<30)|(0x34<<19)|(205<<5));	/* opf=011001101 */
    final static int I_FDTOQ   =((2<<30)|(0x34<<19)|(206<<5));	/* opf=011001110 */
    final static int I_FSTOI   =((2<<30)|(0x34<<19)|(209<<5));	/* opf=011010001 */
    final static int I_FDTOI   =((2<<30)|(0x34<<19)|(210<<5));	/* opf=011010010 */
    final static int I_FQTOI   =((2<<30)|(0x34<<19)|(211<<5));	/* opf=011010011 */

    final static int I_FCMPS   = ((2<<30)|(0x35<<19)|(81<<5));	/* opf=001010001 */
    final static int I_FCMPD   = ((2<<30)|(0x35<<19)|(82<<5));	/* opf=001010010 */
    final static int I_FCMPQ   = ((2<<30)|(0x35<<19)|(83<<5));	/* opf=001010011 */
    final static int I_FCMPES  = ((2<<30)|(0x35<<19)|(85<<5));	/* opf=001010101 */
    final static int I_FCMPED  = ((2<<30)|(0x35<<19)|(86<<5));	/* opf=001010110 */
    final static int I_FCMPEQ  = ((2<<30)|(0x35<<19)|(87<<5));	/* opf=001010111 */

    final static int CUR_METHOD_OFFSET = 64;

    final static boolean SMALL_INT(int X) {
	return ((-0x1000 <= X) && (X < 0x1000));
    }

    final static int HI_INT(int X) {
	return (X & 0xfffff000);
    }

    final static int LO_INT(int X) {
	return ((X & 0x00000fff)|(1<<13));
    }

    final static int Imm(int imm) {
	return ((imm & 0x1fff)|(1<<13));
    }

    final int CALL(int disp) {
	disp -= (code_area + native_pc * 4);
	int ret = (I_CALL|(disp>>>2));
	return ret;
    }

    final static int SETHI(int rd, int imm) {
	return ((4<<22)|(rd << 25)|((imm >> 10) & 0x003fffff));
    }

    final static int SETLO(int rd, int imm) {
	return (I_OR|(rd << 25)|(rd << 14)|LO_INT(imm));
    }

    final static int OP(int op, int rd, int rs1, int rs2) {
	return (op|(rd << 25)|(rs1 << 14)|rs2);
    }

    final static int BR(int op, int disp) {
	return ((op)|(disp & 0x003fffff));
    }

    final static int NOP = (4<<22);

    final static int SET(int rd, int imm) {
	return (I_OR|(rd << 25)|Imm(imm));
    }
    final static int MOVE(int rd,int rs) {
	return (I_OR|(rd << 25)|(rs << 14));
    }

    /* Control Transfer Instruction */
    final static boolean IsCTI(int inst) {
	return (((inst >> 30) == 0x1)
		||(((inst >> 30) == 0x0) && (((inst >> 22) & 0x7) != 0x4))
		||((inst & 0xc1f80000) == 0x81c00000));
    }

    final void emitLD(int tag, int dst, int base, int offset) {
	switch(tag & TYPE_MASK) {
	case TYPE_INT:
	    genCode(OP(I_LD, dst, base, Imm(offset)));
	    break;
	case TYPE_LONG:
	    genCode(OP(I_LD, dst, base, Imm(offset)));
	    genCode(OP(I_LD, dst+1, base, Imm(offset+4)));
	    break;
	case TYPE_FLOAT:
	    genCode(OP(I_LDF, dst, base, Imm(offset)));
	    break;
	case TYPE_DOUBLE:
	    if ((offset & 7) == 0) {
		genCode(OP(I_LDDF, dst, base, Imm(offset)));
	    } else {
		genCode(OP(I_LDF, dst, base, Imm(offset)));
		genCode(OP(I_LDF, dst+1, base, Imm(offset+4)));
	    }
	    break;
	}
    }

    final void emitST(int tag, int src, int base, int offset) {
	switch(tag & TYPE_MASK) {
	case TYPE_INT:
	    genCode(OP(I_ST, src, base, Imm(offset)));
	    break;
	case TYPE_LONG:
	    genCode(OP(I_ST, src, base, Imm(offset)));
	    genCode(OP(I_ST, src+1, base, Imm(offset+4)));
	    break;
	case TYPE_FLOAT:
	    genCode(OP(I_STF, src, base, Imm(offset)));
	    break;
	case TYPE_DOUBLE:
	    if ((offset & 7) == 0) {
		genCode(OP(I_STDF, src, base, Imm(offset)));
	    } else {
		genCode(OP(I_STF, src, base, Imm(offset)));
		genCode(OP(I_STF, src+1, base, Imm(offset+4)));
	    }
	    break;
	}
    }
}

class SparcVirReg implements Constants {
    int regNumI;		// integer register number
    int regNumF;		// floating point register number
    int lastTag;
    int baseReg;		// base register to spill
    int offset;			// memory offset from base register

    public SparcVirReg(int baseReg, int offset) {
	this.regNumI = SparcReg.REG_INVALID;
	this.regNumF = SparcReg.REG_INVALID;
	this.baseReg = baseReg;
	this.offset  = offset;
    }

    final SparcVirReg assign(int tag, int phyNum) {
	lastTag = tag;
	if ((tag & TYPE_MASK) == TYPE_INT)
	    regNumI = phyNum;
	else
	    regNumF = phyNum;
	return this;
    }

    final void spill(int tag, SparcCode compile) {
//  	    if ((lastTag & TYPE_MASKIorF) != (tag & TYPE_MASKIorF))
//  		throw new CompilerError("spill mismatch: last:" + 
//  					(((lastTag & TYPE_MASK) == TYPE_INT) ? "%i" : "%f") +
//  					" i:" + regNumI +
//  					" f:" + regNumF);
	if ((lastTag & TYPE_MASK) == TYPE_INT) {
	    if ((tag & TYPE_MASK) == TYPE_INT) {
		if (Sparc.assert && regNumI == SparcReg.REG_INVALID)
		    throw new CompilerError("spill invalid %i regster");
		compile.emitST(lastTag, regNumI, baseReg, offset);
		if ((Sparc.debug & Compile.DEBUG_NATIVE) != 0) {
		    System.err.println("\t\t\t\t\tspill: %i" + regNumI);
		}
	    }
	} else {
	    if ((tag & TYPE_MASK) != TYPE_INT) {
		if (Sparc.assert && regNumF == SparcReg.REG_INVALID)
		    throw new CompilerError("spill invalid %f regster");
		compile.emitST(lastTag, regNumF, baseReg, offset);
		if ((Sparc.debug & Compile.DEBUG_NATIVE) != 0) {
		    System.err.println("\t\t\t\t\tspill: %f" + regNumF);
		}
	    }
	}
    }

    final void nouse(SparcReg compile) {
	if (regNumI != SparcReg.REG_INVALID) {
	    if ((Sparc.debug & Compile.DEBUG_NATIVE) != 0)
		System.err.println("\t\t\t\t\tnouse %i" + regNumI);
	    compile.iTmp.freeTmpReg(regNumI);
	}
	if (regNumF != SparcReg.REG_INVALID) {
	    if ((Sparc.debug & Compile.DEBUG_NATIVE) != 0)
		System.err.println("\t\t\t\t\tnouse %f" + regNumF);
	    compile.fTmp.freeTmpReg(regNumF);
	}
    }
}

final class SparcPhyRegs implements Constants {
    SparcVirReg vregs[];
    int init;
    int free;
    int used;
    int dirt;
    int keep;
    int tag;

    public SparcPhyRegs(int init, int tag) {
	this.free = init;
	this.init = init;
	vregs = new SparcVirReg[32];
	this.tag = tag;
    }

    final void fixReg(int regno) {
	int mask = ~(1 << regno);
	this.free &= mask;
	this.init &= mask;
    }

    final int getFixReg() {
	int reg = SparcReg.REG_O0;
	int win_reg = init & 0xffffff00;

	do {
	    int mask = 1 << reg;
	    if ((win_reg & mask) != 0) {
		mask = ~mask;
		init &= mask;
		free &= mask;
		return reg;
	    }
	    reg++;
	} while(reg < 32);
	return -1;
    }

    final void save(SparcCode compile) {
	int reg = 0;
	for(int vec = dirt & init; vec != 0; vec >>>= 1) {
	    if ((vec & 1) != 0) {
		if (Sparc.assert && vregs[reg] == null)
		    throw new CompilerError("Save???:" + reg);
		vregs[reg].spill(tag, compile);
	    }
	    ++reg;
	}
	dirt = 0;
    }

    final void invalidateI() {
	free = init;
	used = 0;
	dirt = 0;
	keep = 0;

	int reg = 0;
	for(int vec = free; vec != 0; vec >>>=1) {
	    if ((vec & 1) != 0) {
		if (vregs[reg] != null) {
		    vregs[reg].regNumI = SparcReg.REG_INVALID;
		}
		vregs[reg] = null;
	    }
	    ++reg;
	}
    }

    final void invalidateF() {
	free = init;
	used = 0;
	dirt = 0;
	keep = 0;

	int reg = 0;
	for(int vec = free; vec != 0; vec >>>=1) {
	    if ((vec & 1) != 0) {
		if (vregs[reg] != null) {
		    vregs[reg].regNumF = SparcReg.REG_INVALID;
		}
		vregs[reg] = null;
	    }
	    ++reg;
	}
    }

    final void disableAlloc(int regNum) {
	keep |= (1 << regNum);
    }

    final void enableAlloc() {
	keep = 0;
    }

    final void needSave(int regNum) {
	int mask = (1 << regNum);
	keep |= mask;
	dirt |= mask;
	if (Sparc.assert && (init & mask) != 0 && vregs[regNum] == null) {
	    throw new CompilerError("register allocation: " + regNum + " " + vregs[regNum]);
	}
    }

    final private int searchVector(int vec) {
	int reg = 0;
	for(; vec != 0; vec >>>= 1) {
	    if ((vec & 1) != 0) {
		int mask = 1 << reg;
		used |= mask;
		keep |= mask;
		free &= ~mask;
		return reg;
	    }
	    ++reg;
	}
	return -1;
    }

    final int getTmpReg(int tag, SparcCode compile) {
	int reg = 0;
	int vec = used & (~keep);

	if (((reg = searchVector(free)) >= 0) ||
	    ((reg = searchVector(vec & ~dirt)) >= 0) ||
	    ((reg = searchVector(vec)) >= 0)) {
	    SparcVirReg old = vregs[reg];
	    int mask = (1<<reg);
	    if ((dirt & mask) != 0) {
		if (Sparc.assert && old == null) {
		    throw new CompilerError("register allocation: %" + reg);
		}
		old.spill(tag, compile);
		dirt &= ~mask;
	    }
	    if (old != null) {
		if ((tag & TYPE_MASKIorF) == TYPE_INT)
		    old.regNumI = SparcReg.REG_INVALID;
		else
		    old.regNumF = SparcReg.REG_INVALID;
	    }
	    return reg;
	} else {
	    throw new CompilerError("No free temporary register");
	}
    }

    final void freeTmpReg(int phyNum) {
	int mask = ~(1 << phyNum);
	keep &= mask;
	dirt &= mask;
    }
}

abstract class SparcReg extends SparcCode {
    final static int REG_G0 = 0;
    final static int REG_G1 = 1;
    final static int REG_G2 = 2;
    final static int REG_G3 = 3;

    final static int REG_O0 = 8;
    final static int REG_O1 = 9;
    final static int REG_SP = 14;
    final static int REG_O7 = 15;
    final static int REG_L0 = 16;
    final static int REG_L7 = 23;
    final static int REG_I0 = 24;
    final static int REG_I1 = 25;
    final static int REG_FP = 30;
    final static int REG_I7 = 31;

    final static int REG_F0 = 0;
    final static int REG_F1 = 1;

    final static int REG_INVALID = -1;

    final static int PARAM_REG_SIZE = 6;
    final static int PARAM_REG_OFFSET = 68;

    final static boolean assert = true;

    SparcVirReg virRegs[];

    SparcPhyRegs iTmp;
    SparcPhyRegs fTmp;

    final void loadVirReg(int dst, int tag, int virNum) {
	SparcVirReg vr = virRegs[virNum];
	emitLD(tag, dst, vr.baseReg, vr.offset);
    }

    final void storeVirReg(int src, int tag, int virNum) {
	SparcVirReg vr = virRegs[virNum];
	emitST(tag, src, vr.baseReg, vr.offset);
    }

    final void regAlloc() {
	/* stack must be greater than maxstack for a temporary register */
	int stack = maxstack + 1;
	int nregs = nlocals + stack;
	int offset = (stack < PARAM_REG_SIZE) ? 92 : PARAM_REG_OFFSET + stack * 4;
	int i;
	boolean optimize_leaf = false;

	if (((flags & (FLAG_HAS_CALL | FLAG_HAS_EXCEPTION | FLAG_HAS_JSR | FLAG_HAS_THROW)) == 0) &&
	    (access & ACC_SYNCHRONIZED) == 0 &&
	    nregs < 6) {
	    optimize_leaf = true;
	    flags |= FLAG_OPTIMIZE_LEAF;
	    // available regsiters:  %o0-%o5, %g1-%g4
	    iTmp = new SparcPhyRegs(0x00003f1e, TYPE_INT);
	} else {
	    // available regsiters:  %i0-%i5, %l0-%l7, %g1-%g4
	    iTmp = new SparcPhyRegs(0x3fff001e, TYPE_INT);
	}
	// unavailable register: %f0
	fTmp = new SparcPhyRegs(0x55555554, TYPE_FLOAT);

	if (thisHandle != null) {
	    virRegs = new SparcVirReg[nregs + 1];
	} else {
	    virRegs = new SparcVirReg[nregs];
	}
	for (i = nlocals; i < nregs; i++) {
	    virRegs[i] = new SparcVirReg(REG_SP, offset);
	    offset += 4;
	}
	if (optimize_leaf) {
	    for (i = 0; i < args_size; i++) {
		virRegs[i] = new SparcVirReg(REG_SP, i * 4 + 68);
	    }
	} else {
	    for (i = 0; i < args_size; i++) {
		virRegs[i] = new SparcVirReg(REG_FP, i * 4 + 68);
	    }
	}
	for (; i < nlocals; i++) {
	    virRegs[i] = new SparcVirReg(REG_SP, offset);
	    offset += 4;
	}

	// allocate argument registers
	int reg_args = args_size < 6 ? args_size : 6;
	for (i = 0; i < reg_args; i++) {
	    if (optimize_leaf) {
		iTmp.fixReg(REG_O0 + i);
		virRegs[i].regNumI = (REG_O0 + i);
	    } else {
		iTmp.fixReg(REG_I0 + i);
		virRegs[i].regNumI = (REG_I0 + i);
	    }
	}

	// allocate handle (this)
	if (thisHandle != null) {
	    SparcVirReg vr = new SparcVirReg(0, 0);
	    virRegs[nregs] = vr;
	    vr.regNumI = iTmp.getFixReg();
	}

	// allocate other registers
	for (; i < nregs; i++) {
	    int regno = iTmp.getFixReg();
	    if (regno < 0)
		break;
	    virRegs[i].regNumI = regno;
	}
    }

    /*
     * physical register number.
     *
     * @param	tag	type of leaf.
     * @param	val	virtual register number.
     * @exception 
     * @return	physical register number if allocated. otherwise REG_INVALID(-1).
     */
    final int regNum(int tag, int val) {
	if ((tag & TAG_MASK) == TAG_CONST) {
	    return (val == 0) ? REG_G0 : REG_INVALID;
	}
	if ((tag & TAG_MASK) == TAG_PARAM) {
	    if ((tag & TYPE_MASK) == TYPE_INT)
		return val + REG_O0;
	    return val;
	}
	SparcVirReg vr = virRegs[val];
	return (tag & TYPE_MASK) == TYPE_INT ? vr.regNumI : vr.regNumF;
    }

    final void saveRegs() {
	if ((debug & DEBUG_NATIVE) != 0)
	    System.err.println("\t\t\t\t\tsave %i: 0x" + Integer.toHexString(iTmp.dirt));
	iTmp.save(this);
	if ((debug & DEBUG_NATIVE) != 0)
	    System.err.println("\t\t\t\t\tsave %f: 0x" + Integer.toHexString(fTmp.dirt));
	fTmp.save(this);
    }

    final void invalidateRegs() {
	iTmp.invalidateI();
	fTmp.invalidateF();
    }

    final int allocTmpReg() {
	int regno = iTmp.getTmpReg(TYPE_INT, this);
	iTmp.vregs[regno] = null;
	return regno;
    }

    final void freeTmpReg(int phyNum) {
	iTmp.freeTmpReg(phyNum);
    }

    final int allocTmpRegF() {
	int regno = fTmp.getTmpReg(TYPE_FLOAT, this);
	fTmp.vregs[regno] = null;
	return regno;
    }

    final void freeTmpRegF(int phyNum) {
	fTmp.freeTmpReg(phyNum);
    }

    final int allocSrcReg(int tag, int virNum) {
	if ((tag & TAG_MASK) == TAG_PARAM) {
	    if (assert && virNum != RETF && virNum != ARG0 && virNum != ARG1) {
		throw new CompilerError("physical register:" + virNum);
	    }
	    if ((tag & TYPE_MASK) == TYPE_INT) {
		return virNum + REG_O0;
	    }
	    return virNum;
	}
	if ((tag & TAG_MASK) == TAG_CONST) {
	    if (assert && virNum != 0)
		throw new CompilerError("internal error");
	    return REG_G0;
	}

	SparcVirReg vr = virRegs[virNum];
	SparcPhyRegs pr;
	int regno;

	if ((tag & TYPE_MASKIorF) == TYPE_INT) {
	    pr = iTmp;
	    regno = vr.regNumI;
	} else {
	    pr = fTmp;
	    regno = vr.regNumF;
	}
	if (regno == REG_INVALID) {
	    regno = pr.getTmpReg(tag, this);
	    pr.vregs[regno] = vr;
	    vr.assign(tag, regno);
	    /* fill */
	    emitLD(tag, regno, vr.baseReg, vr.offset);
	    if ((debug & DEBUG_NATIVE) != 0) {
		System.err.println("\t\t\t\t\tfill: " + ILnode.tag2String(tag, virNum) + 
				   (((tag & TYPE_MASKIorF) == TYPE_INT) ? " = %i" : " = %f")
				   + regno);
	    }
	}
	pr.disableAlloc(regno);
	return regno;
    }

    final int allocDstReg(ILnode il) {
	int tag = il.dtype();
	int virNum = il.dval;
	int regno;

	if ((tag & TAG_MASK) == TAG_PARAM) {
	    if ((tag & TYPE_MASK) == TYPE_INT) {
		if (virNum >= ARG0 && virNum < ARG0 + PARAM_REG_SIZE) {
		    return virNum + REG_O0;
		} else {
		    regno = iTmp.getTmpReg(tag, this);
		    iTmp.vregs[regno] = null;
		}
	    } else {
		regno = fTmp.getTmpReg(tag, this);
		fTmp.vregs[regno] = null;
	    }
	    if (regno == 0)
		throw new CompilerError("regno == 0");
	    /* save register number to il.dval */
	    /* this value is used in regAllocEnd() */
	    il.dval |= (regno << 24);
	    return regno;
	}
	if ((tag & TAG_MASK) == TAG_CONST) {
	    if (assert && virNum != 0)
		throw new CompilerError("internal error");
	    return REG_G0;
	}

	SparcVirReg vr = virRegs[virNum];
	SparcPhyRegs pr;

	if ((tag & TYPE_MASKIorF) == TYPE_INT) {
	    pr = iTmp;
	    regno = vr.regNumI;
	} else {
	    pr = fTmp;
	    regno = vr.regNumF;
	}
	if (regno == REG_INVALID) {
	    regno = pr.getTmpReg(tag, this);
	    pr.vregs[regno] = vr;
	    vr.assign(tag, regno);
	    if ((debug & DEBUG_NATIVE) != 0) {
		System.err.println("\t\t\t\t\tassign: " + ILnode.tag2String(tag, virNum) + 
				   (((tag & TYPE_MASKIorF) == TYPE_INT) ? " = %i" : " = %f")
				   + regno);
	    }
	} else {
	    vr.lastTag = tag;
	}
	if ((tag & TYPE_MASK) == TYPE_DOUBLE) {
	    vr = virRegs[virNum + 1];
	    if (vr.regNumF != REG_INVALID)
		pr.freeTmpReg(vr.regNumF);
	    vr.assign(tag, REG_INVALID);
	}
	pr.needSave(regno);
	return regno;
    }

    final void freeRegs(ILnode il) {
	if (il.isSTORE() && (il.dtype() & TAG_FREE) != 0)
	    virRegs[il.dval].nouse(this);
	if ((il.ltype() & TAG_FREE) != 0)
	    virRegs[il.lval & 0x1ffff].nouse(this);
	if ((il.rtype() & TAG_FREE) != 0)
	    virRegs[il.rval & 0x1ffff].nouse(this);
    }

    final void regAllocEnd(ILnode il) {
	if (il.tagD() == TAG_PARAM) { /* save param registers */
	    int tmpreg = (il.dval >>> 24);
	    if (tmpreg != 0) {
		int num = il.dval & 0xffffff;
		int offset = (num - ARG0) * 4 + PARAM_REG_OFFSET;
		int type = il.typeD();

		switch(type) {
		case TYPE_INT:
		    emitST(type, tmpreg, REG_SP, offset);
		    break;
		case TYPE_FLOAT:
		    emitST(type, tmpreg, REG_SP, offset);
		    if (num < ARG0 + PARAM_REG_SIZE)
			emitLD(TAG_PI, num + REG_O0, REG_SP, offset);
		    break;
		case TYPE_DOUBLE:
		    emitST(type, tmpreg, REG_SP, offset);
		    if (num < ARG0 + PARAM_REG_SIZE) {
			emitLD(TAG_PI, num + REG_O0, REG_SP, offset);
		    }
		    if (num + 1 < ARG0 + PARAM_REG_SIZE) {
			emitLD(TAG_PI, num + REG_O0 + 1, REG_SP, offset + 4);
		    }
		    break;
		}
	    }
	}
	iTmp.enableAlloc();
	fTmp.enableAlloc();
    }
}

public final class Sparc extends SparcReg {
    // runtime attribute 
    final static int RT_ATTR_VIRTBL = (1<<0);
    final static int RT_ATTR_PATCH_SET_O0 = (1<<1);
    final static int RT_ATTR_PATCH_SET_O0_CALL = (1<<2);
    final static int RT_ATTR_PATCH_SET_G3_CALL = (1<<3);

    final static int MONITOR_ENTER_PC = 4;
    final static int STACK_REDZONE = (0x1000 - 8);

    private final void setParamI(int argc) {
	int regnum = regNum(TAG_VI, argc);
	int base;
	int reg_p0;

	if ((flags & FLAG_OPTIMIZE_LEAF) == 0) {
	    base = REG_FP;
	    reg_p0 = REG_I0;
	} else {
	    base = REG_SP;
	    reg_p0 = REG_O0;
	}
	if (regnum >= 0) {
	    if (argc < PARAM_REG_SIZE) {
		if (reg_p0 + argc != regnum)
		    genCode(MOVE(regnum, reg_p0 + argc));
	    } else {
		genCode(OP(I_LD, regnum, base, Imm(argc * 4 + 68)));
	    }
	} else {
	    if (argc < PARAM_REG_SIZE)
		genCode(OP(I_ST, reg_p0 + argc, base, Imm(argc * 4 + 68)));
	}
    }

    private final void setParamF(int argc) {
	if (argc < PARAM_REG_SIZE) {
	    if ((flags & FLAG_OPTIMIZE_LEAF) == 0) {
		genCode(OP(I_ST, REG_I0 + argc, REG_FP, Imm(argc * 4 + 68)));
	    } else {
		genCode(OP(I_ST, REG_O0 + argc, REG_SP, Imm(argc * 4 + 68)));
	    }
	}
    }

    private Sparc() {}

    /*
     * STACK FRAME LAYOUT:
     *
     *	%sp		+=========================================================
     *			| 16 words in which to save register window	
     *			|
     *	%sp + 64 	+---------------------------------------------------------
     *			| (hidden param) currentMethod
     *			|
     *	%sp + 68	+---------------------------------------------------------
     *			| 6 words into which callee may store register arguments
     *			|
     *	%sp + 92	+---------------------------------------------------------
     *			| outgoing parameters past the sixth if any
     *			|
     *	%sp + ??	+--(68 + max(maxstack,6) * 4)-----------------------------
     *			| java stack
     *			|
     *	%sp + ??	+--(68 + max(maxstack,6) * 4 + maxstack)------------------
     *			| local var (- args)
     *			|
     *			+--(68 + max(maxstack,6) * 4 + maxstack + nlocals)--------
     *			| monitor (1 word) if synchronized method
     *	%fp(old %sp)	+=========================================================
     *			| 
     *			|
     *	%fp + 64 	+---------------------------------------------------------
     *			| 
     *			|
     *	%fp + 68	+---------------------------------------------------------
     *			| args
     *			|
     *	%fp + 92	+---------------------------------------------------------
     *			| 
     *			|
     *	%fp + ??	+---------------------------------------------------------
     *			| 
     *			|
     *			+---------------------------------------------------------
     *			| 
     *			|
     *			+=========================================================
     */

    private final void genPrologue() {
	int frame_size = 92;

	if ((access & ACC_SYNCHRONIZED) != 0)
	    frame_size += 4;	/* for monitor exit on exception */

	if (maxstack < 6)
	    frame_size += (6 + maxstack) * 4;
	else
	    frame_size += maxstack * 4 * 2;
	frame_size += (nlocals - args_size) * 4;
	frame_size = (frame_size + 7) & ~7;
	/* if leaf method, overflow check is omitted. */
	if ((flags & FLAG_HAS_INVOKE) != 0) {
	    /* stack over flow check */
	    genCode(OP(I_LD, REG_G0, REG_SP, Imm(- STACK_REDZONE)));
	}
	/* make stack frame */
	if ((flags & FLAG_OPTIMIZE_LEAF) == 0) {
	    genCode(OP(I_SAVE, REG_SP, REG_SP, Imm(-frame_size)));
	    /* set current methodblock */
	    if ((flags & (FLAG_HAS_CALL | FLAG_HAS_EXCEPTION)) != 0) {
		genCode(OP(I_ST, REG_G3, REG_SP, Imm(CUR_METHOD_OFFSET)));
	    }
	}

	if ((access & ACC_SYNCHRONIZED) != 0) {
	    if ((access & ACC_STATIC) != 0) {
		// cbHandle(fieldclass(&mb->fb))
		genCode(OP(I_LD, REG_O0, REG_G3, Imm(0)));
	    } else {
		genCode(MOVE(REG_O0, REG_I0));
	    }
	    // must be (native_pc == MONITOR_ENTER_PC)
	    genCode(CALL(RT_addr[RT_monitorEnter]));
	    genCode(OP(I_ST, REG_O0, REG_FP, Imm(-4)));
	}

	/* initialize parameter variable */
	int argc = 0;

	if ((access & ACC_STATIC) == 0) {
	    setParamI(argc);
	    argc++;
	}

    loop:
	for(int i = 1; i < signature.length; i++) {
	    switch(signature[i]) {
	    case SIGC_ENDMETHOD:
		break loop;
	    case SIGC_LONG:
		setParamI(argc);
		argc++;
		setParamI(argc);
		argc++;
		break;
	    case SIGC_BOOLEAN:
	    case SIGC_SHORT:
	    case SIGC_BYTE:
	    case SIGC_CHAR:
	    case SIGC_INT:
		setParamI(argc);
		argc++;
		break;
	    case SIGC_CLASS:
		while (signature[i] != SIGC_ENDCLASS) i++;
		setParamI(argc);
		argc++;
		break;
	    case SIGC_ARRAY:
		while (signature[i] == SIGC_ARRAY) i++;
		if (signature[i] == SIGC_CLASS) {
		    while (signature[i] != SIGC_ENDCLASS) i++;
		}
		setParamI(argc);
		argc++;
		break;
	    case SIGC_DOUBLE:
		setParamF(argc);
		argc++;
		setParamF(argc);
		argc++;
		break;
	    case SIGC_FLOAT:
		setParamF(argc);
		argc++;
		break;
	    }
	}
	/* end of method prologue */
    }

    private final int REGorSIMM13(int type, int val) {
	if ((type & TAG_MASK) == TAG_CONST) {
	    if (val == 0)
		return REG_G0;
	    if (SMALL_INT(val))
		return Imm(val);
	    int tmp = allocTmpReg();
	    genCode(SETHI(tmp, val));
	    if ((val & 0x0fff) != 0)
		genCode(SETLO(tmp, val));
	    return tmp;
	} else {
	    return allocSrcReg(type, val);
	}
    }

    private final void fixFowardBranch(BCinfo bc) {
	for(int br = bc.native_pc; br >= 0; ) {
	    int disp = native_pc - br;
	    int inst = getNativeCode(br);
	    int nbr = br + ((inst << 10) >> 10);

	    if ((inst >> 30) == 1) { /* call */
		setNativeCode(br, I_CALL | (disp & 0x3fffffff));
	    } else if ((inst >> 22) == 0) { /* table branch */
		setNativeCode(br, code_area + br * 4 + (disp << 2));
	    } else { /* branch */
		setNativeCode(br, BR(inst & 0xffc00000, disp));
	    }
	    br = nbr;
	}
    }

    private final void emitIDIVInsn(ILnode il) {
	int rd, rs1, rs2;

	if (il.tagL() == TAG_CONST) {
	    if (il.lval < 0)
		genCode(OP(I_WRY, 0, 0, Imm(-1)));
	    else
		genCode(OP(I_WRY, 0, 0, Imm(0)));
	    if (il.lval == 0) {
		rs1 = REG_G0;
	    } else {
		rs1 = allocTmpReg();
		emitSetConst(rs1, il.lval);
	    }
	} else {
	    int rtmp = allocTmpReg();
	    rs1 = allocSrcReg(il.ltype(), il.lval);
	    genCode(OP(I_SRA, rtmp, rs1, Imm(31)));
	    genCode(OP(I_WRY, 0, 0, rtmp));
	    freeTmpReg(rtmp);
	}
	genCode(NOP);
	genCode(NOP);
	genCode(NOP);

	rs2 = REGorSIMM13(il.rtype(), il.rval);
	rd = allocDstReg(il);
	// bug:
	// -2147483648/-1 shoud be -2147483648
	genCode(OP(I_SDIVCC, rd, rs1, rs2));    
	genCode(BR(I_BVS | I_ANNUL, 2));
	genCode(SETHI(rd, 0x80000000));
    }

    private final void emitIREMInsn(ILnode il) {
	int rd, rs1, rs2;

	if (il.tagR() == TAG_CONST & (il.rval > 0)) {
	    /* translate operation SDIV -> SRA */
	    int rsft;
	    int v = il.rval;

	    if (v == 1) {		/* remainder by 1 */
		rd = allocDstReg(il);
		genCode(MOVE(rd, REG_G0));
		return;
	    }

	    for (rsft = 0; v != 0; rsft++) {
		if ((v & 1) != 0) {
		    if (v == 1) {
			int rtmp;
			rs1 = allocSrcReg(il.ltype(), il.lval);
			rd = allocDstReg(il);
			if (rsft == 1) {
			    rtmp = allocTmpReg();
			    genCode(OP(I_SRL, rtmp, rs1, Imm(31)));
			    genCode(OP(I_ADD, rtmp, rs1, rtmp));
			} else {
			    rtmp = allocTmpReg();
			    genCode(OP(I_SRA, rtmp, rs1, Imm(rsft-1)));
			    genCode(OP(I_SRL, rtmp, rtmp, Imm(32-rsft)));
			    genCode(OP(I_ADD, rtmp, rs1, rtmp));
			}
			genCode(OP(I_SRA, rtmp, rtmp, Imm(rsft)));
			genCode(OP(I_SLL, rtmp, rtmp, Imm(rsft)));
			genCode(OP(I_SUB, rd, rs1, rtmp));
			freeTmpReg(rtmp);
			return;
		    } else
			break;
		}
		v >>= 1;
	    }
	}

	if (il.tagL() == TAG_CONST) {
	    if (il.lval == 0) {
		rs1 = REG_G0;
	    } else {
		rs1 = allocTmpReg();
		emitSetConst(rs1, il.lval);
	    }
	} else {
	    rs1 = allocSrcReg(il.ltype(), il.lval);
	}
	rs2 = REGorSIMM13(il.rtype(), il.rval);
	int tmp = allocTmpReg();
	/* Y reg must be set */
	if (il.tagL() == TAG_CONST) {
	    if (il.lval < 0)
		genCode(OP(I_WRY, 0, 0, Imm(-1)));
	    else
		genCode(OP(I_WRY, 0, 0, Imm(0)));
	} else {
	    genCode(OP(I_SRA, tmp, rs1, Imm(31)));
	    genCode(OP(I_WRY, 0, 0, tmp));
	}
	genCode(NOP);
	genCode(NOP);
	genCode(NOP);
	// genCode(OP(I_SDIV, tmp, rs1, rs2));
	genCode(OP(I_SDIVCC, tmp, rs1, rs2));    
	genCode(BR(I_BVS | I_ANNUL, 2));
	genCode(SETHI(tmp, 0x80000000));

	genCode(OP(I_SMUL, tmp, tmp, rs2));
	rd = allocDstReg(il);
	genCode(OP(I_SUB,  rd, rs1, tmp));
    }

    private final void emitMemInsn(ILnode il, int op) {
	int rd, rs1, rs2;

	rd = REG_INVALID;
	rs1 = REGorSIMM13(il.ltype(), il.lval);
	if (il.isSTORE()) {
	    if (il.tagD() == TAG_CONST) {
		if (il.dval == 0) {
		    rd = REG_G0;
		} else {
		    rd = allocTmpReg();
		    emitSetConst(rd, il.dval);
		}
	    } else {
		rd = allocSrcReg(il.dtype(), il.dval);
	    }
    	}
	if (il.tagR() == TAG_CONST) {
	    if (rd == REG_INVALID)
		rd = allocDstReg(il);
	    if (SMALL_INT(il.rval)) {
		genCode(OP(op, rd, rs1, Imm(il.rval)));
	    } else if (il.tagL() == TAG_CONST) {
		int addr = il.lval + il.rval;

		if (SMALL_INT(addr)) {
		    genCode(OP(op, rd, 0, addr));
		} else {
		    int rt = allocTmpReg();
		    genCode(SETHI(rt, HI_INT(addr)));
		    genCode(OP(op, rd, rt, LO_INT(addr)));
		}
	    } else {
		int rt = allocTmpReg();
		emitSetConst(rt, il.rval);
		genCode(OP(op, rd, rs1, rt));
	    }
	} else {
	    rs2 = allocSrcReg(il.rtype(), il.rval);
	    if (rd == REG_INVALID)
		rd = allocDstReg(il);
	    genCode(OP(op, rd, rs1, rs2));
	}
    }

    private final void emitMemDInsn(ILnode il, int op, int dop) {
	int rd, rs1, rs2;

	if (il.tagR() == TAG_CONST) {
	    if (SMALL_INT(il.rval + 4)) {
		rs1 = REGorSIMM13(il.ltype(), il.lval);
		if (il.isSTORE())
		    rd = allocSrcReg(il.dtype(), il.dval);
		else
		    rd = allocDstReg(il);
		genCode(OP(op, rd, rs1, Imm(il.rval)));
		genCode(OP(op, rd+1, rs1, Imm(il.rval + 4)));
	    } else if (il.tagL() == TAG_CONST) {
		int addr = il.lval + il.rval;

		if (il.isSTORE())
		    rd = allocSrcReg(il.dtype(), il.dval);
		else
		    rd = allocDstReg(il);
		if ((addr & 0x7) == 0) {
		    /* align 8byte boundary */
		    /* use lddf or stdf */
		    if (SMALL_INT(addr)) {
			genCode(OP(dop, rd, 0, addr));
		    } else {
			rs1 = allocTmpReg();
			genCode(SETHI(rs1, HI_INT(addr)));
			genCode(OP(dop, rd, rs1, LO_INT(addr)));
		    }
		} else {
		    if (SMALL_INT(addr + 4)) {
			genCode(OP(op, rd, 0, addr));
			genCode(OP(op, rd+1, 0, Imm(addr + 4)));
		    } else {
			rs1 = allocTmpReg();
			genCode(SETHI(rs1, HI_INT(addr)));
			genCode(OP(op, rd, rs1, LO_INT(addr)));
			genCode(OP(op, rd+1, rs1, Imm(LO_INT(addr)+4)));
		    }
		}
	    } else {
		rs1 = allocSrcReg(il.ltype(), il.lval);
		rs2 = allocTmpReg();
		if (il.isSTORE())
		    rd = allocSrcReg(il.dtype(), il.dval);
		else
		    rd = allocDstReg(il);
		emitSetConst(rs1, il.rval);
		genCode(OP(op, rd, rs1, rs2));
		genCode(OP(I_ADD, rs2, rs2, Imm(4)));
		genCode(OP(op, rd+1, rs1, rs2));
	    }
	} else if (il.tagR() == TAG_PARAM) { /* getfield, putfield */
	    rs1 = allocSrcReg(il.ltype(), il.lval);
	    rs2 = allocSrcReg(il.rtype(), il.rval);
	    int tmp = allocTmpReg();
	    if (il.isSTORE())
		rd = allocSrcReg(il.dtype(), il.dval);
	    else
		rd = allocDstReg(il);
	    genCode(OP(op, rd, rs1, rs2));
	    genCode(OP(I_ADD, tmp, rs2, Imm(4)));
	    genCode(OP(op, rd+1, rs1, tmp));
	} else { /* daload, dastore */
	    /* access to double[],  */
	    /* assume aligned 8byte boundary */
	    rs1 = allocSrcReg(il.ltype(), il.lval);
	    rs2 = allocSrcReg(il.rtype(), il.rval);
	    if (il.isSTORE())
		rd = allocSrcReg(il.dtype(), il.dval);
	    else
		rd = allocDstReg(il);
	    genCode(OP(dop, rd, rs1, rs2));
	}
    }

    private final void emitSetConst(int reg, int c) {
	if (SMALL_INT(c)) {
	    genCode(OP(I_OR, reg, 0, Imm(c)));
	} else {
	    genCode(SETHI(reg, c));
	    if ((c & 0x0fff) != 0) {
		genCode(SETLO(reg, c));
	    }
	}
    }

    private final void emitMoveInsn(ILnode il) {
	int rd, rs;
	int dtype = il.dtype() & (TAG_MASK | TYPE_MASK);
	int rtype = il.rtype() & (TAG_MASK | TYPE_MASK);

	if (il.tagD() == TAG_PARAM) {
	    // set parameter
	    if (il.typeD() == TYPE_INT) {
		if (il.dval < ARG0 + PARAM_REG_SIZE) {
		    rd = regNum(dtype, il.dval);
		    if (il.tagR() == TAG_CONST) {
			emitSetConst(rd, il.rval);
		    } else {
			rs = regNum(rtype, il.rval);
			if (rs < 0) {
			    loadVirReg(rd, rtype, il.rval);
			} else {
			    genCode(MOVE(rd, rs));
			}
		    }
		} else {
		    if (il.tagR() == TAG_CONST) {
			if (il.rval == 0) {
			    rd = REG_G0;
			} else {
			    rd = allocTmpReg();
			    emitSetConst(rd, il.rval);
			}
			genCode(OP(I_ST, rd, REG_SP, Imm((il.dval - ARG0) * 4 + PARAM_REG_OFFSET)));
			freeTmpReg(rd);
		    } else {
			rs = regNum(rtype, il.rval);
			if (rs < 0) {
			    rd = allocTmpReg();
			    loadVirReg(rd, rtype, il.rval);
			    genCode(OP(I_ST, rd, REG_SP, Imm((il.dval - ARG0) * 4 + PARAM_REG_OFFSET)));
			    freeTmpReg(rd);
			} else {
			    genCode(OP(I_ST, rs, REG_SP, Imm((il.dval - ARG0) * 4 + PARAM_REG_OFFSET)));
			}
		    }
		}
	    } else if (il.typeD() == TYPE_FLOAT) {
		int offset = (il.dval - ARG0) * 4 + PARAM_REG_OFFSET;

		if (regNum(rtype, il.rval) == REG_INVALID &&
		    il.dval < ARG0 + PARAM_REG_SIZE) {
		    rd = regNum(TAG_PI, il.dval);
		    loadVirReg(rd, TAG_PI, il.rval);
		} else {
		    rs = allocSrcReg(rtype, il.rval);
		    genCode(OP(I_STF, rs, REG_SP, Imm(offset)));
		    if (il.dval < ARG0 + PARAM_REG_SIZE) {
			rd = regNum(TAG_PI, il.dval);
			genCode(OP(I_LD, rd, REG_SP, Imm(offset)));
		    }
		}
	    } else if (il.typeD() == TYPE_DOUBLE) {
		int offset = (il.dval - ARG0) * 4 + PARAM_REG_OFFSET;

		if (regNum(rtype, il.rval) == REG_INVALID &&
		    il.dval + 1 < ARG0 + PARAM_REG_SIZE) {
		    rd = regNum(TAG_PI, il.dval);
		    loadVirReg(rd, TAG_PARAM | TYPE_LONG, il.rval);
		} else {
		    rs = allocSrcReg(rtype, il.rval);
		    if ((offset & 0x7) == 0) {
			genCode(OP(I_STDF, rs, REG_SP, Imm(offset)));
		    } else {
			genCode(OP(I_STF, rs, REG_SP, Imm(offset)));
			genCode(OP(I_STF, rs+1, REG_SP, Imm(offset + 4)));
		    }
		    if (il.dval < ARG0 + PARAM_REG_SIZE) {
			rd = regNum(TAG_PI, il.dval);
			genCode(OP(I_LD, rd, REG_SP, Imm(offset)));
		    }
		    if (il.dval+1 < ARG0 + PARAM_REG_SIZE) {
			rd = regNum(TAG_PI, il.dval+1);
			genCode(OP(I_LD, rd, REG_SP, Imm(offset + 4)));
		    }
		}
	    }
	    return;
	}

	if (il.tagR() == TAG_CONST) {
	    if (il.rval == 0) {
		if (regNum(dtype, il.dval) < 0) {
		    storeVirReg(REG_G0, dtype, il.dval);
		} else {
		    rd = allocDstReg(il);
		    genCode(MOVE(rd, REG_G0));
		}
	    } else {
		rd = allocDstReg(il);
		emitSetConst(rd, il.rval);
	    }
	} else {
	    if (dtype == rtype && il.dval == il.rval)
		return;
	    rs = regNum(rtype, il.rval);
	    if (rs >= 0) {
		rd = regNum(dtype, il.dval);
		if (rd < 0) {
		    storeVirReg(rs, dtype, il.dval);
		} else {
		    rd = allocDstReg(il);
		    if (rd != rs) {
			if (il.typeD() == TYPE_INT) {
			    genCode(MOVE(rd, rs));
			} else {
			    genCode(OP(I_FMOVS, rd, 0, rs));
			    if (il.typeD() == TYPE_DOUBLE)
				genCode(OP(I_FMOVS, (rd + 1), 0, (rs + 1)));
			}
		    }
		}
	    } else {
		rd = allocDstReg(il);
		loadVirReg(rd, rtype, il.rval);
	    }
	}
    }

    private final void emitSetReg(int rd, int rtype, int rval) {
	int rs;

	if ((rtype & TAG_MASK) == TAG_CONST) {
	    emitSetConst(rd, rval);
	} else {
	    if ((rs = regNum(rtype, rval)) < 0) {
		loadVirReg(rd, rtype, rval);
	    } else {
		genCode(MOVE(rd, rs));
	    }
	}
    }

    private final void emitInsn(ILnode il, int op, boolean is_swap) {
	int rs1, rs2, rd;
	if (il.tagL() == TAG_CONST) {
	    if (il.lval == 0) {
		rs1 = REG_G0;
		rs2 = REGorSIMM13(il.rtype(), il.rval);
	    } else {
		if (il.tagR() == TAG_CONST)
		    throw new CompilerError("const op const:" + il);
		if (is_swap) {
		    rs1 = allocSrcReg(il.rtype(), il.rval);
		    rs2 = REGorSIMM13(il.ltype(), il.lval);
		} else {
		    rs1 = allocTmpReg();
		    emitSetConst(rs1, il.lval);
		    rs2 = allocSrcReg(il.rtype(), il.rval);
		}
	    }
	} else {
	    rs1 = allocSrcReg(il.ltype(), il.lval);
	    rs2 = REGorSIMM13(il.rtype(), il.rval);
	}
	rd = allocDstReg(il);
	genCode(OP(op, rd, rs1, rs2));
    }

    /* I2B, I2C, I2S */
    private final void emitI2(ILnode il, int op, int shift) {
	int rs1, imm, rd;
	rs1 = allocSrcReg(il.rtype(), il.rval);
	imm = Imm(shift);
	rd = allocDstReg(il);
	genCode(OP(I_SLL, rd, rs1, imm));
	genCode(OP(op, rd, rd, imm));
    }

    private final void emitFNEGInsn(ILnode il) {
	int op = I_FNEGS;
	int rs2, rd;
	rs2 = allocSrcReg(il.rtype(), il.rval);
	rd = allocDstReg(il);
	genCode(OP(op, rd, 0, rs2));
	if (il.typeD() == TYPE_DOUBLE && rd != rs2) {
	    genCode(OP(I_FMOVS, rd+1, 0, rs2+1));
	}
    }

    private final void emitI2FD(ILnode il, int op) {
	int rs, rd;
	int type = (il.rtype() & ~TYPE_MASK) | TYPE_FLOAT;

	if (il.tagR() == TAG_CONST) {
	    if (il.rval == 0) {
		rs = REG_G0;
	    } else {
		rs = allocTmpReg();
		emitSetConst(rs, il.rval);
	    }
	    storeVirReg(rs, TAG_CONST, il.dval);
	    rd = allocDstReg(il);
	    loadVirReg(rd, type, il.dval);
	    genCode(OP(op, rd, 0, rd));
	} else {
	    rs = regNum(il.rtype(), il.rval);
	    if (rs != REG_INVALID) {
		storeVirReg(rs, il.rtype(), il.rval);
	    }
	    rs = regNum(type, il.rval);
	    if (rs != REG_INVALID) {
		loadVirReg(rs, type, il.rval);
	    } else {
		rs = allocSrcReg(type, il.rval);
	    }
	    rd = allocDstReg(il);
	    genCode(OP(op, rd, 0, rs));
	}
    }

    private final void emitFD2I(ILnode il, int op) {
	int rs, rd;

	rs = allocSrcReg(il.rtype(), il.rval);
	rd = allocTmpRegF();
	genCode(OP(op, rd, 0, rs));
	storeVirReg(rd, (il.dtype() & ~TYPE_MASK) | TYPE_FLOAT, il.dval);
	freeTmpRegF(rd);
	rd = allocDstReg(il);
	loadVirReg(rd, il.dtype(), il.dval);
    }

    private final void emitRETURN(ILnode il) {
	int rs;

	if (il.tagD() == TAG_CONST) {
	    if ((flags & FLAG_OPTIMIZE_LEAF) != 0) {
		genCode(OP(I_JMPL, 0, REG_O7, Imm(8)));
		genCode(NOP);
		return;
	    }
	} else { // not void function
	    switch(il.typeD()) {
	    case TYPE_LONG:
		if ((flags & FLAG_OPTIMIZE_LEAF) != 0) {
		    if (regNum(il.ltype(), il.lval) == REG_O1) {
			int rtype = il.rtype();
			int rval = il.rval;
	      
			if (regNum(rtype, rval) == REG_O0) {
			    // swap %i0 and %i1
			    genCode(OP(I_XOR, REG_O0, REG_O0, REG_O1));
			    genCode(OP(I_XOR, REG_O1, REG_O0, REG_O1));
			    genCode(OP(I_XOR, REG_O0, REG_O0, REG_O1));
			} else {
			    genCode(MOVE(REG_O0, REG_O1));
			    emitSetReg(REG_O1, rtype, rval);
			}
		    } else {
			emitSetReg(REG_O1, il.rtype(), il.rval);
			emitSetReg(REG_O0, il.ltype(), il.lval);
		    }
		    break;
		}
		if (regNum(il.ltype(), il.lval) == REG_I1) {
		    int rtype = il.rtype();
		    int rval = il.rval;
	      
		    if (regNum(rtype, rval) == REG_I0) {
			// swap %i0 and %i1
			genCode(OP(I_XOR, REG_I0, REG_I0, REG_I1));
			genCode(OP(I_XOR, REG_I1, REG_I0, REG_I1));
			genCode(OP(I_XOR, REG_I0, REG_I0, REG_I1));
		    } else {
			genCode(MOVE(REG_I0, REG_I1));
			emitSetReg(REG_I1, rtype, rval);
		    }
		} else {
		    emitSetReg(REG_I1, il.rtype(), il.rval);
		    emitSetReg(REG_I0, il.ltype(), il.lval);
		}
		break;
	    case TYPE_INT:
		if ((flags & FLAG_OPTIMIZE_LEAF) != 0) {
		    emitSetReg(REG_O0, il.rtype(), il.rval);
		    break;
		}
		if ((access & ACC_SYNCHRONIZED) == 0) {
		    if (il.tagR() == TAG_CONST && SMALL_INT(il.rval)) {
			genCode(OP(I_JMPL, 0, REG_I7, Imm(8)));
			genCode(OP(I_RESTORE, REG_O0, 0, Imm(il.rval)));
			return;
		    }
		    rs = regNum(il.rtype(), il.rval);
		    if (rs != REG_INVALID) {
			genCode(OP(I_JMPL, 0, REG_I7, Imm(8)));
			genCode(OP(I_RESTORE, REG_O0, rs, Imm(0)));
			return;
		    }
		}
		emitSetReg(REG_I0, il.rtype(), il.rval);
		break;
	    case TYPE_FLOAT:
		if (il.tagR() == TAG_CONST) {
		    throw new CompilerError("return const float");
		}
		rs = regNum(il.rtype(), il.rval);
		if (rs < 0) {
		    loadVirReg(REG_F0, il.rtype(), il.rval);
		} else if (rs != REG_F0) {
		    genCode(OP(I_FMOVS, REG_F0, 0, rs));
		}
		break;
	    case TYPE_DOUBLE:
		if (il.tagR() == TAG_CONST) {
		    throw new CompilerError("return const float");
		}
		rs = regNum(il.rtype(), il.rval);
		if (rs < 0) {
		    loadVirReg(REG_F0, il.rtype(), il.rval);
		} else if (rs != REG_F0) {
		    genCode(OP(I_FMOVS, REG_F0, 0, rs));
		    genCode(OP(I_FMOVS, REG_F1, 0, rs+1));
		}
		break;
	    }
	}
	if ((access & ACC_SYNCHRONIZED) != 0) {
	    /* call monitorExit(frame->monitor) */
	    il.dval = native_pc;
	    genCode(CALL(RT_addr[RT_monitorExit]));
	    genCode(OP(I_LD, REG_O0, REG_FP, Imm(-4)));
	}
	if ((flags & FLAG_OPTIMIZE_LEAF) == 0) {
	    genCode(OP(I_JMPL, 0, REG_I7, Imm(8)));
	    genCode(OP(I_RESTORE, 0, 0, Imm(0)));
	} else {
	    int previous_inst = getNativeCode(native_pc-1);
	    --native_pc;
	    genCode(OP(I_JMPL, 0, REG_O7, Imm(8)));
	    genCode(previous_inst);
	}
    }

    private final void expandOneBytecode(BCinfo bc, ILnode il, int pc) {
	int rd, rs1, rs2;
	int bc_native_pc = native_pc;

	for(; il != null; il = il.next) {
	    if ((debug & DEBUG_NATIVE) != 0)
		System.err.println("0x" + Integer.toHexString(code_area + native_pc * 4) + "\t" + il);

	    switch(il.op()) {
	    case ILnode.ELIMINATED:
		continue;
	    case ILnode.NOP:
		genCode(NOP);
		continue;
	    case ILnode.BRANCH:
		int disp;
		int target_pc = il.rval;
		int cond = il.lval;
		int op;

		switch(cond) {
		    /* integer */
		case CC_A:	op = I_BA|I_ANNUL; break;
		case CC_E:	op = I_BE; break;
		case CC_NE:	op = I_BNE; break;
		case CC_L:	op = I_BL; break;
		case CC_LE:	op = I_BLE; break;
		case CC_G:	op = I_BG; break;
		case CC_GE:	op = I_BGE; break;
		    /* floating */
		case CC_FA:	op = I_FBA|I_ANNUL; break;
		case CC_FE:	op = I_FBE; break;
		case CC_FNE:	op = I_FBNE; break;
		case CC_FL:	op = I_FBUL; break;
		case CC_FLE:	op = I_FBULE; break;
		case CC_FG:	op = I_FBUG; break;
		case CC_FGE:	op = I_FBUGE; break;
		    /* Not NaN case */
		case CC_NFL:	op = I_FBL; break;
		case CC_NFLE:	op = I_FBLE; break;
		case CC_NFG:	op = I_FBG; break;
		case CC_NFGE:	op = I_FBGE; break;
		    /* special */
		case CC_LU:	op = I_BCS; break;
		case CC_LEU:	op = I_BLEU; break;
		case CC_GU:	op = I_BGU; break;
		case CC_GEU:	op = I_BCC; break;
		    /* others */
		case CC_TBLOB:	op = I_BGU; break;
		case CC_TBLSW:	op = 0; break;	    /* must be 0 for fixing forward branch */
		case CC_JSR:	op = 0; break;
		default:	op =0;
		};

		saveRegs();
		disp = bcinfo[target_pc].native_pc - native_pc;
		if (cond == CC_JSR) {
		    if (target_pc >= pc) {
			bcinfo[target_pc].native_pc = native_pc;
		    }
		    genCode(I_CALL|(disp & 0x3fffffff));
		    invalidateRegs();
		    if ((rs2 = regNum(il.dtype(), il.dval)) <= 0) {
			storeVirReg(REG_O7, il.dtype(), il.dval);
		    } else {
			genCode(MOVE(rs2, REG_O7));
		    }
		    break;
		}
		/* FPop2 and FBfcc instructions must not be continuous */
		if (((op >> 22) & 0x7) == 0x6) {
		    /* floating conditinonal branch */
		    if ((getNativeCode(pc-1) >> 19) == 0x1035) {
			/* previous instruction is FPop2 */
			genCode(NOP);
			disp -= 1;
		    }
		}
		if (target_pc >= pc) {
		    // forward branch. must be fixed after resolution of target address
		    bcinfo[target_pc].native_pc = native_pc;
		    if (cond == CC_TBLSW) {	/* tableswitch */
			il.dval = native_pc;
			genCode(BR(0, disp));
			break;
		    }
		} else {
		    if (cond == CC_TBLSW) {	/* tableswitch */
			il.dval = native_pc;
			genCode(code_area + bcinfo[target_pc].native_pc * 4);
			break;
		    }
		}

		genCode(BR(op, disp));

		// fill in delay slot
		if (cond == CC_TBLOB)
		    break;
		if (cond == CC_A)
		    break;

		// *** following code is meaningless. should be fixed
		// 	  if (il.next != null &&
		// 	      il.next.op == ILnode.SUBCC &&
		// 	      il.next.tagD() == TAG_CONST)
		// 	    break;

		// can't fill in delay slot
		genCode(NOP);

		break;

	    case ILnode.CALL:
		int func = il.rval;
		int addr = RT_addr[il.rval];
		saveRegs();
		switch(il.rval) {
		case RT_getstatic:
		case RT_resolveField:
		case RT_resolveStaticField:
		case RT_resolveString:
		    // self modify 2 instructions & set REG_O0
		    il.dval = native_pc;
		    genCode(CALL(addr));
		    genCode(SETHI(REG_O0, il.lval << 10));
		    if (il.rval == RT_getstatic && il.typeD() == TYPE_LONG)
			genCode(NOP);
		    break;
		case RT_anewarray:
		case RT_checkcast:
		case RT_instanceof:
		case RT_multianewarray:
		case RT_new:
		    // self modify 3 instructions & set REG_O0
		    il.dval = native_pc;
		    genCode(CALL(addr));
		    genCode(SETHI(REG_O0, il.lval << 10));
		    genCode(NOP);
		    break;
		case RT_invokeinterface:
		case RT_invokeinterface_quick:
		case RT_invokespecial:
		case RT_invokesuper_quick:
		    genCode(OP(I_LD, REG_G1, REG_O0, Imm(4)));
		case RT_invokestatic:
		    // self modify 3 instructions & set REG_G3
		    genCode(SETHI(REG_G3, HI_INT(il.lval)));
		    il.dval = native_pc;
		    genCode(CALL(addr));
		    genCode(SETLO(REG_G3, il.lval));
		    break;
		case RT_invokestatic_resolve:
		    // self modify 3 instructions & set REG_G3
		    il.dval = native_pc;
		    genCode(CALL(addr));
		    genCode(SETHI(REG_G3, il.lval << 10));
		    genCode(NOP);
		    break;
		case RT_invokespecial_resolve:
		case RT_invokevirtual_resolve:
		    genCode(OP(I_LD, REG_G1, REG_O0, Imm(4)));
		    // self modify 3 instructions & set REG_G3
		    il.dval = native_pc;
		    genCode(CALL(addr));
		    genCode(SETHI(REG_G3, il.lval << 10));
		    genCode(NOP);
		    break;
		default:
		    int previous_inst;
		    if (bc_native_pc < native_pc && !IsCTI(previous_inst = getNativeCode(native_pc-1))) {
			// fill delay slot
			--native_pc;
			il.dval = native_pc;
			genCode(CALL(addr));
			genCode(previous_inst);
		    } else {
			il.dval = native_pc;
			genCode(CALL(addr));
			genCode(NOP);
		    }
		}
		invalidateRegs();
		break;

	    case ILnode.ARRAYCHK:
		if (!omit_array_boundary_check) {
		    int robj, rsize, rindex;
		    robj = allocSrcReg(il.ltype(), il.lval);
		    rsize = allocTmpReg();
		    genCode(OP(I_LD, rsize, robj, Imm(METHOD_OFFSET)));
		    genCode(OP(I_SRL, rsize, rsize, Imm(METHOD_FLAG_BITS)));
		    rindex = REGorSIMM13(il.rtype(), il.rval);
		    genCode(OP(I_SUBCC, REG_G0, rsize, rindex));
		    genCode(I_TLEU | Imm(ST_RANGE_CHECK));
		}
		break;

	    case ILnode.DIV0CHK:
		if (il.dval != 0) {
		    genCode(I_TA | Imm(ST_DIV0));
		} else {
		    emitInsn(il, I_ORCC, true);
		    genCode(I_TE | Imm(ST_DIV0));
		}
		break;

	    case ILnode.RETURN:
		emitRETURN(il);
		break;

	    case ILnode.TBLSW:
		if (il.tagL() == TAG_CONST) {
		    rs1 = Imm(il.lval);
		} else {
		    rs1 = allocSrcReg(il.ltype(), il.lval);
		}
		il.dval = native_pc;
		int tmp = allocTmpReg();
		freeRegs(il);
		saveRegs();
		int tbl_addr = code_area + (native_pc + 5) * 4;
		genCode(SETHI(tmp, HI_INT(tbl_addr)));
		genCode(OP(I_OR, tmp, tmp, LO_INT(tbl_addr)));
		genCode(OP(I_LD, tmp, tmp, rs1));
		genCode(OP(I_JMPL, 0, tmp, 0));
		genCode(NOP);
		break;

	    case ILnode.IREM:
		emitIREMInsn(il);
		break;

	    case ILnode.MOVE:
		emitMoveInsn(il);
		break;

	    case ILnode.LD:
		emitMemInsn(il, I_LD);
		break;
	    case ILnode.FLD:
		emitMemInsn(il, I_LDF);
		break;
	    case ILnode.DLD:
		emitMemDInsn(il, I_LDF, I_LDDF);
		break;

	    case ILnode.ST:
		emitMemInsn(il, I_ST);
		break;
	    case ILnode.FST:
		emitMemInsn(il, I_STF);
		break;
	    case ILnode.DST:
		emitMemDInsn(il, I_STF, I_STDF);
		break;

	    case ILnode.STB:
		emitMemInsn(il, I_STB);
		break;
	    case ILnode.STH:
		emitMemInsn(il, I_STH);
		break;
	    case ILnode.LDSH:
		emitMemInsn(il, I_LDSH);
		break;
	    case ILnode.LDSB:
		emitMemInsn(il, I_LDSB);
		break;
	    case ILnode.LDUH:
		emitMemInsn(il, I_LDUH);
		break;

	    case ILnode.ADD:
		emitInsn(il, I_ADD, true);
		break;
	    case ILnode.FADD:
		emitInsn(il, I_FADDS, true);
		break;
	    case ILnode.DADD:
		emitInsn(il, I_FADDD, true);
		break;

	    case ILnode.SUB:
		emitInsn(il, I_SUB, false);
		break;
	    case ILnode.FSUB:
		emitInsn(il, I_FSUBS, false);
		break;
	    case ILnode.DSUB:
		emitInsn(il, I_FSUBD, false); 
		break;

	    case ILnode.MUL:
		emitInsn(il, I_SMUL, true);
		break;
	    case ILnode.FMUL:
		emitInsn(il, I_FMULS, true);
		break;
	    case ILnode.DMUL:
		emitInsn(il, I_FMULD, true);
		break;

	    case ILnode.DIV:
		emitIDIVInsn(il);
		break;
	    case ILnode.FDIV:
		emitInsn(il, I_FDIVS, false);
		break;
	    case ILnode.DDIV:
		emitInsn(il, I_FDIVD, false);
		break;

	    case ILnode.RET:
		emitInsn(il, I_JMPL, false);
		break;
	    case ILnode.ADDL:
		emitInsn(il, I_ADDCC, true);
		break;
	    case ILnode.ADDH:
		emitInsn(il, I_ADDX, true);
		break;
	    case ILnode.AND:
		emitInsn(il, I_AND, true);
		break;
	    case ILnode.OR:
		emitInsn(il, I_OR, true);
		break;
	    case ILnode.SLL:
		emitInsn(il, I_SLL, false);
		break;
	    case ILnode.SRA:
		emitInsn(il, I_SRA, false);
		break;
	    case ILnode.SRL:
		emitInsn(il, I_SRL, false);
		break;
	    case ILnode.TEST:
		emitInsn(il, I_ANDCC, true);
		break;
	    case ILnode.CMP:
	    case ILnode.SUBL:
		emitInsn(il, I_SUBCC, false);
		break;
	    case ILnode.SUBH:
		emitInsn(il, I_SUBX, false);
		break;
	    case ILnode.XOR:
		emitInsn(il, I_XOR, true);
		break;
	    case ILnode.I2B:
		emitI2(il, I_SRA, 24);
		break;
	    case ILnode.I2C:
		emitI2(il, I_SRL, 16);
		break;
	    case ILnode.I2S:
		emitI2(il, I_SRA, 16);
		break;
	    case ILnode.I2F:
		emitI2FD(il, I_FITOS);
		break;
	    case ILnode.I2D:
		emitI2FD(il, I_FITOD);
		break;
	    case ILnode.F2I:
		emitFD2I(il, I_FSTOI);
		break;
	    case ILnode.D2I:
		emitFD2I(il, I_FDTOI);
		break;
	    case ILnode.F2D:
		emitInsn(il, I_FSTOD, false);
		break;
	    case ILnode.D2F:
		emitInsn(il, I_FDTOS, false);
		break;
	    case ILnode.FCMP:
		emitInsn(il, I_FCMPES, false);
		break;
	    case ILnode.DCMP:
		emitInsn(il, I_FCMPED, false);
		break;
	    case ILnode.FNEG:
	    case ILnode.DNEG:
		// special case. SPARC doesn't have double negative operation.
		emitFNEGInsn(il);
		break;
	    default:
		throw new CompilerError("Internal Error. Unknown ILnode op:" + il.op());
	    }
	    
	    freeRegs(il);
	    regAllocEnd(il);
	}
    }

    private final void expandInstructions() {
	for(int pc = 0; pc < bcinfo.length; pc++) {
	    BCinfo bc = bcinfo[pc];
	    if (bc == null)
		continue;
	    if ((bc.flag & BCinfo.FLAG_BRANCH_TARGET) != 0) {
		saveRegs();
		invalidateRegs();
	    }
	    if ((debug & DEBUG_NATIVE) != 0) {
		if ((bc.flag & BCinfo.FLAG_BRANCH_TARGET) != 0) {
		    System.err.println("### BB beg ###");
		}
		System.err.println("***" + pc + "\t" + opcName(pc));
	    }
	    fixFowardBranch(bc);
	    bc.native_pc = native_pc;
	    expandOneBytecode(bc, bc.next, pc);
	}
    }

    private final void relocateNativeCode(int size) {
	int diff = NativeCodeReAlloc(size);
	int inst;
	int addr;

	if (diff != 0) {
	    if ((access & ACC_SYNCHRONIZED) != 0) {
		inst = getNativeCode(MONITOR_ENTER_PC);
		addr = (inst << 2) + diff;
		setNativeCode(MONITOR_ENTER_PC, CALL(addr));
	    }
	    for(int pc = 0; pc < bytecode_length; pc++) {
		BCinfo bc = bcinfo[pc];
		if (bc == null)
		    continue;
		for (ILnode il = bc.next; il != null; il = il.next) {
		    switch(il.op()) {
		    case ILnode.RETURN:
			if ((access & ACC_SYNCHRONIZED) == 0)
			    break;
			// monitorExit
		    case ILnode.CALL:
			inst = getNativeCode(il.dval);
			addr = (inst << 2) + diff;
			setNativeCode(il.dval, (I_CALL|(addr>>>2)));
			break;
		    case ILnode.TBLSW:
			inst = getNativeCode(il.dval);	// sethi
			addr = inst << 10;
			int inst2 = getNativeCode(il.dval+1); // or
			addr = (addr | (inst2 & 0x0fff)) + diff;
			setNativeCode(il.dval, ((inst >>> 22) << 22) | (addr >>> 10));
			setNativeCode(il.dval+1, ((inst >>> 12) << 12) | (addr & 0x0fff));
			break;
		    case ILnode.BRANCH:
			if (il.lval != CC_TBLSW)
			    break;
			inst = getNativeCode(il.dval);
			setNativeCode(il.dval, inst + diff);
			break;
		    }
		}
	    }
	}
    }

    final void genNativeCode() {
	NativeCodeAlloc(bytecode_length * 100);
	genPrologue();
	if (thisHandle != null) {
	    expandOneBytecode(null, thisHandle, 0);
	}
	expandInstructions();
	relocateNativeCode(native_pc * 4);
	ExceptionHandler.Set(this);
    }
}
