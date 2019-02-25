//
// Copyright (C) 1996 1997 1998 1999 by FUJITSU LABORATRIES LTD.
//
// $Revision: 1.19 $
// $Date: 1999/09/27 04:52:02 $
// $Author: kouya $
//

package org.OpenJIT;

import java.io.*;
import java.util.*;

class OpenJITProperties extends Properties {
    public OpenJITProperties() {
	try {
	    File file = new File("OpenJIT.properties");
	    FileInputStream in = new FileInputStream(file);
	    this.load(in);
	    in.close();
	    // this.list(System.err);
	} catch(Exception e) {
	}
    }

    /* overload */
    public String getProperty(String key) {
	String str;
	str = super.getProperty(key);
	if (str == null)
	    str = System.getProperty(key);
	return str;
    }

    public boolean getPropertyBoolean(String key, boolean default_value) {
	String str;
	str = super.getProperty(key);
	if (str == null)
	    str = System.getProperty(key);
	if (str == null)
	    return default_value;
	if (str.compareTo("true") == 0)
	    return true;
	if (str.compareTo("false") == 0)
	    return false;
	return default_value;
    }
}

class Select {
    Select next;
    String clazz;
    String method;
    String signature;

    public Select() {};

    public Select(String prop) {
	int index;
	String str;
	Select sel = this;

	while(true) {
	    index = prop.indexOf(':');
	    if (index >= 0) {
		str = prop.substring(0, index);
		prop = prop.substring(index+1);
	    } else {
		str = prop;
		prop = null;
	    }
	    if (str.length() <= 0)
		break;
	    index = str.indexOf('(');
	    if (index >= 0) {
		sel.signature = str.substring(index);
		str = str.substring(0, index);
	    }
	    index = str.indexOf('.');
	    if (index >= 0) {
		sel.method = str.substring(index + 1);
		sel.clazz = (str.substring(0, index)).replace('/', '.');
	    } else {
		sel.method = null;
		sel.clazz = str.replace('/','.');
	    }
	    if (prop != null && prop.length() > 0) {
		sel.next = new Select();
		sel = sel.next;
	    } else {
		break;
	    }
	}
    }

    boolean match(Compile mb) {
	String clazz = mb.className();
	String method = mb.methodName();
	String signature = mb.signatureName();

	for (Select sel = this; sel != null; sel = sel.next) {
	    if (sel.clazz != null) {
		if (!clazz.regionMatches(0, sel.clazz, 0, sel.clazz.length()))
		    continue;
	    }
	    if (sel.method != null) {
		if (!method.regionMatches(0, sel.method, 0, sel.method.length()))
		    continue;
	    }
	    if (sel.signature != null) {
		if (!signature.regionMatches(0, sel.signature, 0, sel.signature.length()))
		    continue;
	    }
	    return true;
	}
	return false;
    }
}

public abstract
class Compile implements Constants {

    /* Instance variables */
    /* method block information. set by native code */
    protected int methodblock;
    protected int constantpool;
    Class clazz;
    byte signature[];
    public byte bytecode[];
    public int bytecode_length; 
    public ExceptionHandler exceptionHandler[];
    public byte attribute[];
    protected int access;
    protected int nlocals;
    protected int maxstack;
    protected int args_size;
    /* bytecode information */
    protected BCinfo bcinfo[];
    int flags;
    /* native code handling */
    protected int code_area;
    private int code_size;
    protected int native_pc;

    /* Class variables */
    /* scope control */
    private static Select enable;
    private static Select disable;
    /* architecture dependent */
    static boolean little_endian_long;
    /* optimize level */
    static boolean optimize_stack = true;
    static boolean optimize_handle = true;
    static boolean optimize_inline = false;
    /* debugging */
    static int debug;
    static final int DEBUG_STOP_AT_START  = (1<<0);
    static final int DEBUG_STOP_AT_INVOKE = (1<<1);
    static final int DEBUG_VERBOSE	  = (1<<2);
    static final int DEBUG_MORE_VERBOSE	  = (1<<3);
    static final int DEBUG_RTL	  	  = (1<<4);
    static final int DEBUG_NATIVE  	  = (1<<5);
    static final int DEBUG_INLINE  	  = (1<<6);
    /* for performance evaluation */
    static boolean omit_array_boundary_check = false;
    static boolean omit_array_store_check = false;

    /* inlining parameters */
    protected int inline_maxinsns; // Maximum instructions to inline
    private static final int DEFAULT_MAXINSNS = 20; 

    /* runtime constants */
    static int RT_fconst_0;
    static int RT_fconst_1;
    static int RT_fconst_2;
    static int RT_dconst_0;
    static int RT_dconst_1;

    /* runtime routines */
    final static int RT_anewarray = 0;
    final static int RT_anewarray_quick = RT_anewarray + 1;
    final static int RT_athrow = RT_anewarray_quick + 1;
    final static int RT_check_aastore = RT_athrow + 1;
    final static int RT_checkcast = RT_check_aastore + 1;
    final static int RT_checkcast_quick = RT_checkcast + 1;
    final static int RT_d2i = RT_checkcast_quick + 1;
    final static int RT_d2l = RT_d2i + 1;
    final static int RT_dcmpg = RT_d2l + 1;
    final static int RT_dcmpl = RT_dcmpg + 1;
    final static int RT_drem = RT_dcmpl + 1;
    final static int RT_f2i = RT_drem + 1;
    final static int RT_f2l = RT_f2i + 1;
    final static int RT_fcmpg = RT_f2l + 1;
    final static int RT_fcmpl = RT_fcmpg + 1;
    final static int RT_frem = RT_fcmpl + 1;
    final static int RT_getstatic = RT_frem + 1;
    final static int RT_instanceof = RT_getstatic + 1;
    final static int RT_instanceof_quick = RT_instanceof + 1;
    final static int RT_invokeinterface = RT_instanceof_quick + 1;
    final static int RT_invokeinterface_quick = RT_invokeinterface + 1;
    final static int RT_invokespecial = RT_invokeinterface_quick + 1;
    final static int RT_invokespecial_resolve = RT_invokespecial + 1;
    final static int RT_invokesuper_quick = RT_invokespecial_resolve + 1;
    final static int RT_invokestatic = RT_invokesuper_quick + 1;
    final static int RT_invokestatic_resolve = RT_invokestatic + 1;
    final static int RT_invokevirtual_resolve = RT_invokestatic_resolve + 1;
    final static int RT_l2d = RT_invokevirtual_resolve + 1;
    final static int RT_l2f = RT_l2d + 1;
    final static int RT_lcmp = RT_l2f + 1;
    final static int RT_ldiv = RT_lcmp + 1;
    final static int RT_lmul = RT_ldiv + 1;
    final static int RT_lrem = RT_lmul + 1;
    final static int RT_lshl = RT_lrem + 1;
    final static int RT_lshr = RT_lshl + 1;
    final static int RT_lushr = RT_lshr + 1;
    final static int RT_monitorEnter = RT_lushr + 1;
    final static int RT_monitorExit = RT_monitorEnter + 1;
    final static int RT_multianewarray = RT_monitorExit + 1;
    final static int RT_multianewarray_quick = RT_multianewarray + 1;
    final static int RT_newarray = RT_multianewarray_quick + 1;
    final static int RT_new = RT_newarray + 1;
    final static int RT_new_quick = RT_new + 1;
    final static int RT_resolveField = RT_new_quick + 1;
    final static int RT_resolveStaticField = RT_resolveField + 1;
    final static int RT_resolveString = RT_resolveStaticField + 1;
    final static int RT_END = RT_resolveString + 1;

    static int RT_addr[] = new int[RT_END]; // entry point of runtime function
    static int RT_attr[] = new int[RT_END]; // attribute of runtime function

    abstract void parseBytecode();
    abstract void convertRTL();
    abstract void optimizeRTL();
    abstract void genNativeCode();
    abstract void regAlloc();

    /* Class initializer */
    static {
	OpenJITProperties props = new OpenJITProperties();
	String str;

	/* control which methods should be compiled */
	str = props.getProperty("compile.enable");
	if (str != null)
	    enable = new Select(str);
	str = props.getProperty("compile.disable");
	if (str != null)
	    disable = new Select(str);

	/* debug options */
	str = props.getProperty("compile.verbose");
	if (str != null) {
	    debug |= DEBUG_VERBOSE;
	    if (str.lastIndexOf("more") != -1)
		debug |= DEBUG_MORE_VERBOSE;
	}
	str = props.getProperty("compile.stop");
	if (str != null) {
	    if (str.lastIndexOf("start") != -1)
		debug |= DEBUG_STOP_AT_START;
	    if (str.lastIndexOf("invoke") != -1)
		debug |= DEBUG_STOP_AT_INVOKE;
	}
	str = props.getProperty("compile.debug");
	if (str != null) {
	    if (str.lastIndexOf("native") != -1)
		debug |= DEBUG_NATIVE;
	    if (str.lastIndexOf("RTL") != -1)
		debug |= DEBUG_RTL;
	    if (str.lastIndexOf("inline") != -1)
		debug |= DEBUG_INLINE;
	}

	/* optimize options */
	optimize_stack = 
	    props.getPropertyBoolean("compile.optimize.stack", 
				     optimize_stack);
	optimize_handle = 
	    props.getPropertyBoolean("compile.optimize.handle", 
				     optimize_handle);
	optimize_inline = 
	    props.getPropertyBoolean("compile.optimize.inline", 
				     optimize_inline);

	/* for perfomance evaluation. */
	/* make faster but violate JVM spec */
	omit_array_boundary_check = 
	    props.getPropertyBoolean("compile.omit.arrayBoundayCheck", 
				     omit_array_boundary_check);
	omit_array_store_check =
	    props.getPropertyBoolean("compile.omit.arrayStoreCheck",
				     omit_array_store_check);
    }

    /* Used for inlining: Initialize parser state to the method pointed
     * to by 'index' in the caller's constant pool.
     */
    public final native void initParser(int caller_cp, int index);

    /* Used for inlining: Resolve the MethodRef at index 'index' in
     * the ConstantPool of this method.
     */
    public final native void resolveClass(int index);

    /* Used for inlining: Create a new compile object to parse the
     * method specified by the given index.
     */
    public Compile createParser(int index) throws IllegalAccessException, InstantiationException {
      Compile parser = (Compile)this.getClass().newInstance();
      parser.initParser(this.constantpool, index);
      return parser;
    }

    /* Used for inlining: Only parse and stop there */
    public boolean parseOnly() {
      try {
	bcinfo = new BCinfo [bytecode_length];
	parseBytecode();
	return true;
      } catch (Exception e) {
	e.printStackTrace(System.err);
	System.err.println("\tin " + this);
      } catch (Error e) {
	e.printStackTrace(System.err);
	System.err.println("\tin " + this);
      }
      return false;
    }

    public boolean compile() {

//      System.out.println("--- compile() called for: "+className()+"."+methodName());
      
	if (enable != null && !enable.match(this))
	    return false;

	if (disable != null && disable.match(this)) 
	    return false;

	try {
	    bcinfo = new BCinfo [bytecode_length];

	    parseBytecode();
	    convertRTL();
	    if (optimize_stack)
		optimizeRTL();
	    regAlloc();
	    genNativeCode();
	    return true;

	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    System.err.println("\tin " + this);
	} catch (Error e) {
	    e.printStackTrace(System.err);
	    System.err.println("\tin " + this);
	}
	return false;
    }

    public final int pc2uchar(int pc) {
	return ((int)bytecode[pc] & 0xff);
    }

    public final int pc2signedchar(int pc) {
	return (int)bytecode[pc];
    }

    public final int pc2signedshort(int pc) {
	short s = (short)bytecode[pc];
	s <<= 8;
	s |= ((int)bytecode[pc+1] & 0xff);
	return (int)s;
    }

    public final int pc2signedlong(int pc) {
	int s = (int)bytecode[pc];
	s <<= 8;
	s |= ((int)bytecode[pc+1] & 0xff);
	s <<= 8;
	s |= ((int)bytecode[pc+2] & 0xff);
	s <<= 8;
	s |= ((int)bytecode[pc+3] & 0xff);
	return s;
    }

    public final int pc2ushort(int pc) {
	int s = (int)bytecode[pc] & 0xff;
	s <<= 8;
	s |= ((int)bytecode[pc+1] & 0xff);
	return s;
    }

    public final native int ConstantPoolValue(int index);

    public final int ConstantPoolAddress(int index) {
	return (constantpool + (index << 2));
    }

    private final native int ConstantPoolTypeTable(int index);

    private static final int CONSTANT_POOL_ENTRY_TYPEMASK = 0x7f;
    private static final int CONSTANT_POOL_ENTRY_RESOLVED = 0x80;

    /**
     * @param	index	the index of constantpool.
     * @exception OpenJIT.CompilerError if the index is out of boundary.
     * @return	a type of constantpool.
     */
    public final int ConstantPoolType(int index) {
	return (ConstantPoolTypeTable(index) & CONSTANT_POOL_ENTRY_TYPEMASK);
    }

    /**
     * @param	index	the index of constantpool.
     * @exception OpenJIT.CompilerError if the index is out of boundary.
     * @return	<code>true</code> if this index is resolved already.
     */
    public final boolean ConstantPoolTypeResolved(int index) {
	return ((ConstantPoolTypeTable(index) & CONSTANT_POOL_ENTRY_RESOLVED) != 0);
    }

    /**
     * @param	index	the index of constantpool.
     * @exception OpenJIT.CompilerError if the index is out of boundary.
     * @return	a Class name of index
     */
    public final String ConstantPoolClass(int index) {
	return new String(ConstantPoolClass0(index));
    }

    private native byte[] ConstantPoolClass0(int index);

    /**
     * @param	index	the index of constantpool.
     * @exception OpenJIT.CompilerError if the index is out of boundary.
     * @return	a method name or or field name.
     */
    public final String ConstantPoolName(int index) {
	return new String(ConstantPoolName0(index));
    }

    private native byte[] ConstantPoolName0(int index);

    /**
     * @param	index	the index of constantpool.
     * @exception OpenJIT.CompilerError if the index is out of boundary.
     * @return the access bits for this entry of the constantpool (if field
     *  or method descriptor).
     */
    public final native int ConstantPoolAccess(int index);

    /**
     * @param	index	the index of constantpool.
     * @exception OpenJIT.CompilerError if the index is out of boundary.
     * @return	a method signature
     */
    public final native byte[] ConstantPoolMethodDescriptor(int index);

    /**
     * @param	index	the index of constantpool.
     * @exception OpenJIT.CompilerError if the index is out of boundary.
     * @see	OpenJIT.ConstantPoolMethodDescriptor
     * @return	a field type.
     */
    public final native byte ConstantPoolFieldDescriptor(int index);

    /**
     * @param	index	the index of constantpool.
     * @exception OpenJIT.CompilerError if the index is out of boundary.
     * @return	a offset of field.
     */
    public final native int ConstantPoolFieldOffset(int index);

    /**
     * @param	index	the index of constantpool.
     * @exception OpenJIT.CompilerError if the index is out of boundary.
     * @return	an absolute location of the static value.
     */
    public final native int ConstantPoolFieldAddress(int index);

    public final native void NativeCodeAlloc(int size);

    public final native int NativeCodeReAlloc(int size);

    public final void genCode(int code) {
	setNativeCode(native_pc, code);
	++native_pc;
    }

    public final native void setNativeCode(int pc, int code);

    public final native int getNativeCode(int pc);

    public final String methodName() {
	return new String(MethodName0());
    }

    private native byte[] MethodName0();

    public final String className() {
	return clazz.getName();
    }

    public final String signatureName() {
	return new String(signature);
    }

    public final String toString() {
	return className() + "." + methodName() + signatureName();
    }

    // for debugging
    public final String opcName(int pc) {
      try {
	int opcode = ((int)bytecode[pc]) & 0xff;
	if (opcode >= Debug.opcNames.length) {
	    return Integer.toString(opcode);
	} else {
	    return Debug.opcNames[opcode];
	}
      } catch (ArrayIndexOutOfBoundsException e) {
	// If code has been inlined the pc may be out of bounds for
	// the old bytecode array
	return new String("");
      }
    }
}
