//
// Copyright (C) 1996 1997 1998 1999 by FUJITSU LABORATRIES LTD.
//
// $Revision: 1.8 $
// $Date: 1999/09/24 04:19:29 $
// $Author: kouya $
//

package org.OpenJIT;

public interface Constants
{
    /* Signature Characters */
    public static final char   SIGC_VOID                  = 'V';
    public static final char   SIGC_BOOLEAN               = 'Z';
    public static final char   SIGC_BYTE                  = 'B';
    public static final char   SIGC_CHAR                  = 'C';
    public static final char   SIGC_SHORT                 = 'S';
    public static final char   SIGC_INT                   = 'I';
    public static final char   SIGC_LONG                  = 'J';
    public static final char   SIGC_FLOAT                 = 'F';
    public static final char   SIGC_DOUBLE                = 'D';
    public static final char   SIGC_ARRAY                 = '[';
    public static final char   SIGC_CLASS                 = 'L';
    public static final char   SIGC_METHOD                = '(';
    public static final char   SIGC_ENDCLASS              = ';';
    public static final char   SIGC_ENDMETHOD             = ')';
    public static final char   SIGC_PACKAGE               = '/';

    /* Constant table */
    public static final int CONSTANT_UTF8                = 1;
    public static final int CONSTANT_UNICODE             = 2;
    public static final int CONSTANT_INTEGER             = 3;
    public static final int CONSTANT_FLOAT               = 4;
    public static final int CONSTANT_LONG                = 5;
    public static final int CONSTANT_DOUBLE              = 6;
    public static final int CONSTANT_CLASS               = 7;
    public static final int CONSTANT_STRING              = 8;
    public static final int CONSTANT_FIELD               = 9;
    public static final int CONSTANT_METHOD              = 10;
    public static final int CONSTANT_INTERFACEMETHOD     = 11;
    public static final int CONSTANT_NAMEANDTYPE         = 12;

    /* Access Flags */
    public static final int ACC_PUBLIC                   = 0x00000001;
    public static final int ACC_PRIVATE                  = 0x00000002;
    public static final int ACC_PROTECTED                = 0x00000004;
    public static final int ACC_STATIC                   = 0x00000008;
    public static final int ACC_FINAL                    = 0x00000010;
    public static final int ACC_SYNCHRONIZED             = 0x00000020;
    public static final int ACC_VOLATILE                 = 0x00000040;
    public static final int ACC_TRANSIENT                = 0x00000080;
    public static final int ACC_NATIVE                   = 0x00000100;
    public static final int ACC_INTERFACE                = 0x00000200;
    public static final int ACC_ABSTRACT                 = 0x00000400;
    public static final int ACC_SUPER                    = 0x00000020;
    
    /* Opcodes */
    final static int opc_nop = 0;
    final static int opc_aconst_null = 1;
    final static int opc_iconst_m1 = 2;
    final static int opc_iconst_0 = 3;
    final static int opc_iconst_1 = 4;
    final static int opc_iconst_2 = 5;
    final static int opc_iconst_3 = 6;
    final static int opc_iconst_4 = 7;
    final static int opc_iconst_5 = 8;
    final static int opc_lconst_0 = 9;
    final static int opc_lconst_1 = 10;
    final static int opc_fconst_0 = 11;
    final static int opc_fconst_1 = 12;
    final static int opc_fconst_2 = 13;
    final static int opc_dconst_0 = 14;
    final static int opc_dconst_1 = 15;
    final static int opc_bipush = 16;
    final static int opc_sipush = 17;
    final static int opc_ldc = 18;
    final static int opc_ldc_w = 19;
    final static int opc_ldc2_w = 20;
    final static int opc_iload = 21;
    final static int opc_lload = 22;
    final static int opc_fload = 23;
    final static int opc_dload = 24;
    final static int opc_aload = 25;
    final static int opc_iload_0 = 26;
    final static int opc_iload_1 = 27;
    final static int opc_iload_2 = 28;
    final static int opc_iload_3 = 29;
    final static int opc_lload_0 = 30;
    final static int opc_lload_1 = 31;
    final static int opc_lload_2 = 32;
    final static int opc_lload_3 = 33;
    final static int opc_fload_0 = 34;
    final static int opc_fload_1 = 35;
    final static int opc_fload_2 = 36;
    final static int opc_fload_3 = 37;
    final static int opc_dload_0 = 38;
    final static int opc_dload_1 = 39;
    final static int opc_dload_2 = 40;
    final static int opc_dload_3 = 41;
    final static int opc_aload_0 = 42;
    final static int opc_aload_1 = 43;
    final static int opc_aload_2 = 44;
    final static int opc_aload_3 = 45;
    final static int opc_iaload = 46;
    final static int opc_laload = 47;
    final static int opc_faload = 48;
    final static int opc_daload = 49;
    final static int opc_aaload = 50;
    final static int opc_baload = 51;
    final static int opc_caload = 52;
    final static int opc_saload = 53;
    final static int opc_istore = 54;
    final static int opc_lstore = 55;
    final static int opc_fstore = 56;
    final static int opc_dstore = 57;
    final static int opc_astore = 58;
    final static int opc_istore_0 = 59;
    final static int opc_istore_1 = 60;
    final static int opc_istore_2 = 61;
    final static int opc_istore_3 = 62;
    final static int opc_lstore_0 = 63;
    final static int opc_lstore_1 = 64;
    final static int opc_lstore_2 = 65;
    final static int opc_lstore_3 = 66;
    final static int opc_fstore_0 = 67;
    final static int opc_fstore_1 = 68;
    final static int opc_fstore_2 = 69;
    final static int opc_fstore_3 = 70;
    final static int opc_dstore_0 = 71;
    final static int opc_dstore_1 = 72;
    final static int opc_dstore_2 = 73;
    final static int opc_dstore_3 = 74;
    final static int opc_astore_0 = 75;
    final static int opc_astore_1 = 76;
    final static int opc_astore_2 = 77;
    final static int opc_astore_3 = 78;
    final static int opc_iastore = 79;
    final static int opc_lastore = 80;
    final static int opc_fastore = 81;
    final static int opc_dastore = 82;
    final static int opc_aastore = 83;
    final static int opc_bastore = 84;
    final static int opc_castore = 85;
    final static int opc_sastore = 86;
    final static int opc_pop = 87;
    final static int opc_pop2 = 88;
    final static int opc_dup = 89;
    final static int opc_dup_x1 = 90;
    final static int opc_dup_x2 = 91;
    final static int opc_dup2 = 92;
    final static int opc_dup2_x1 = 93;
    final static int opc_dup2_x2 = 94;
    final static int opc_swap = 95;
    final static int opc_iadd = 96;
    final static int opc_ladd = 97;
    final static int opc_fadd = 98;
    final static int opc_dadd = 99;
    final static int opc_isub = 100;
    final static int opc_lsub = 101;
    final static int opc_fsub = 102;
    final static int opc_dsub = 103;
    final static int opc_imul = 104;
    final static int opc_lmul = 105;
    final static int opc_fmul = 106;
    final static int opc_dmul = 107;
    final static int opc_idiv = 108;
    final static int opc_ldiv = 109;
    final static int opc_fdiv = 110;
    final static int opc_ddiv = 111;
    final static int opc_irem = 112;
    final static int opc_lrem = 113;
    final static int opc_frem = 114;
    final static int opc_drem = 115;
    final static int opc_ineg = 116;
    final static int opc_lneg = 117;
    final static int opc_fneg = 118;
    final static int opc_dneg = 119;
    final static int opc_ishl = 120;
    final static int opc_lshl = 121;
    final static int opc_ishr = 122;
    final static int opc_lshr = 123;
    final static int opc_iushr = 124;
    final static int opc_lushr = 125;
    final static int opc_iand = 126;
    final static int opc_land = 127;
    final static int opc_ior = 128;
    final static int opc_lor = 129;
    final static int opc_ixor = 130;
    final static int opc_lxor = 131;
    final static int opc_iinc = 132;
    final static int opc_i2l = 133;
    final static int opc_i2f = 134;
    final static int opc_i2d = 135;
    final static int opc_l2i = 136;
    final static int opc_l2f = 137;
    final static int opc_l2d = 138;
    final static int opc_f2i = 139;
    final static int opc_f2l = 140;
    final static int opc_f2d = 141;
    final static int opc_d2i = 142;
    final static int opc_d2l = 143;
    final static int opc_d2f = 144;
    final static int opc_i2b = 145;
    final static int opc_i2c = 146;
    final static int opc_i2s = 147;
    final static int opc_lcmp = 148;
    final static int opc_fcmpl = 149;
    final static int opc_fcmpg = 150;
    final static int opc_dcmpl = 151;
    final static int opc_dcmpg = 152;
    final static int opc_ifeq = 153;
    final static int opc_ifne = 154;
    final static int opc_iflt = 155;
    final static int opc_ifge = 156;
    final static int opc_ifgt = 157;
    final static int opc_ifle = 158;
    final static int opc_if_icmpeq = 159;
    final static int opc_if_icmpne = 160;
    final static int opc_if_icmplt = 161;
    final static int opc_if_icmpge = 162;
    final static int opc_if_icmpgt = 163;
    final static int opc_if_icmple = 164;
    final static int opc_if_acmpeq = 165;
    final static int opc_if_acmpne = 166;
    final static int opc_goto = 167;
    final static int opc_jsr = 168;
    final static int opc_ret = 169;
    final static int opc_tableswitch = 170;
    final static int opc_lookupswitch = 171;
    final static int opc_ireturn = 172;
    final static int opc_lreturn = 173;
    final static int opc_freturn = 174;
    final static int opc_dreturn = 175;
    final static int opc_areturn = 176;
    final static int opc_return = 177;
    final static int opc_getstatic = 178;
    final static int opc_putstatic = 179;
    final static int opc_getfield = 180;
    final static int opc_putfield = 181;
    final static int opc_invokevirtual = 182;
    final static int opc_invokespecial = 183;
    final static int opc_invokestatic = 184;
    final static int opc_invokeinterface = 185;
    final static int opc_xxxunusedxxx = 186;
    final static int opc_new = 187;
    final static int opc_newarray = 188;
    final static int opc_anewarray = 189;
    final static int opc_arraylength = 190;
    final static int opc_athrow = 191;
    final static int opc_checkcast = 192;
    final static int opc_instanceof = 193;
    final static int opc_monitorenter = 194;
    final static int opc_monitorexit = 195;
    final static int opc_wide = 196;
    final static int opc_multianewarray = 197;
    final static int opc_ifnull = 198;
    final static int opc_ifnonnull = 199;
    final static int opc_goto_w = 200;
    final static int opc_jsr_w = 201;
    final static int opc_breakpoint = 202;
    final static int opc_ldc_quick = 203;
    final static int opc_ldc_w_quick = 204;
    final static int opc_ldc2_w_quick = 205;
    final static int opc_getfield_quick = 206;
    final static int opc_putfield_quick = 207;
    final static int opc_getfield2_quick = 208;
    final static int opc_putfield2_quick = 209;
    final static int opc_getstatic_quick = 210;
    final static int opc_putstatic_quick = 211;
    final static int opc_getstatic2_quick = 212;
    final static int opc_putstatic2_quick = 213;
    final static int opc_invokevirtual_quick = 214;
    final static int opc_invokenonvirtual_quick = 215;
    final static int opc_invokesuper_quick = 216;
    final static int opc_invokestatic_quick = 217;
    final static int opc_invokeinterface_quick = 218;
    final static int opc_invokevirtualobject_quick = 219;
    final static int opc_invokeignored_quick = 220;
    final static int opc_new_quick = 221;
    final static int opc_anewarray_quick = 222;
    final static int opc_multianewarray_quick = 223;
    final static int opc_checkcast_quick = 224;
    final static int opc_instanceof_quick = 225;
    final static int opc_invokevirtual_quick_w = 226;
    final static int opc_getfield_quick_w = 227;
    final static int opc_putfield_quick_w = 228;
    final static int opc_nonnull_quick = 229;
    final static int opc_first_unused_index = 230;
    final static int opc_software = 254;
    final static int opc_hardware = 255;

    final static int TYPE_INT =    0x00;
    final static int TYPE_LONG =   0x01;
    final static int TYPE_FLOAT =  0x02;
    final static int TYPE_DOUBLE = 0x03;
    final static int TYPE_MASK =   0x03;
    final static int TYPE_MASKIorF = 0x02; // distinguish integer or floating

    final static int TAG_CONST = 0x00;
    final static int TAG_STACK = 0x04;
    final static int TAG_LOCAL = 0x08;
    final static int TAG_PARAM = 0x0c;
    final static int TAG_MASK  = 0x0c;

    final static int TAG_SI = TAG_STACK | TYPE_INT;
    final static int TAG_SL = TAG_STACK | TYPE_LONG;
    final static int TAG_SF = TAG_STACK | TYPE_FLOAT;
    final static int TAG_SD = TAG_STACK | TYPE_DOUBLE;

    final static int TAG_VI = TAG_LOCAL | TYPE_INT;
    final static int TAG_VF = TAG_LOCAL | TYPE_FLOAT;
    final static int TAG_VD = TAG_LOCAL | TYPE_DOUBLE;

    final static int TAG_PI = TAG_PARAM | TYPE_INT;
    final static int TAG_PL = TAG_PARAM | TYPE_LONG;
    final static int TAG_PF = TAG_PARAM | TYPE_FLOAT;
    final static int TAG_PD = TAG_PARAM | TYPE_DOUBLE;

    // for peephole optimization
    final static int TAG_REF   = 0x10;
    final static int TAG_FREE  = 0x20;
    final static int TAG_SAVE  = 0x40;

    // condition code 
    final static int CC_A = 0;
    final static int CC_E = 1;
    final static int CC_NE = 2;
    final static int CC_L = 3;
    final static int CC_LE = 4;
    final static int CC_G = 5;
    final static int CC_GE = 6;
    /* floating */
    final static int CC_FA = 7;
    final static int CC_FE = 8;
    final static int CC_FNE = 9;
    final static int CC_FL = 10;
    final static int CC_FLE = 11;
    final static int CC_FG = 12;
    final static int CC_FGE = 13;
    /* Not NaN case */
    final static int CC_NFL = 14;
    final static int CC_NFLE = 15;
    final static int CC_NFG = 16;
    final static int CC_NFGE = 17;
    /* special */
    final static int CC_LU = 18;
    final static int CC_LEU = 19;
    final static int CC_GU = 20;
    final static int CC_GEU = 21;
    /* others */
    final static int CC_TBLOB = 22; /* table out of boundary */
    final static int CC_TBLSW = 23;
    final static int CC_JSR = 24;

    final static int CC_FLOAT = CC_FA;
    final static int CC_NotNaN = CC_NFL;

    final static int ARG0 = 0;
    final static int ARG1 = 1;
    final static int ARG2 = 2;
    final static int ARG3 = 3;

    final static int RETI = 0;
    final static int RETL = 1;
    final static int RETF = 0;

    /* taking out arraylength from handle. these depend on JVM */
    final static int METHOD_OFFSET = 4;
    final static int METHOD_FLAG_BITS = 5;

    /* FLAGS after parsing bytecode */
    final static int FLAG_HAS_INVOKE    = 1 << 0;
    final static int FLAG_HAS_EXCEPTION = 1 << 1;
    final static int FLAG_HAS_JSR	= 1 << 2;
    final static int FLAG_HAS_THROW	= 1 << 3;
    final static int FLAG_HAS_CALL      = 1 << 4;
    final static int FLAG_OPTIMIZE_LEAF = 1 << 5;
}
