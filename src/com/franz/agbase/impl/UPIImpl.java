package com.franz.agbase.impl;

import java.nio.CharBuffer;

import com.franz.agbase.UPI;
import com.franz.agbase.transport.AGConnector;
import com.franz.agbase.transport.AGDirectLink;

/**
 * This class represents instances of Universal Part Identifiers.
 * UPIs are used to denote nodes and literals in AllegroGraph 2.0.
 * They are returned as values of queries or accessors, and may be used
 * as arguments to queries and accessors.  In general, UPI references 
 * are more efficient than string references.
 * 
 * <p>
 * UPIs are dicusssed in more detail in the AllegroGraph Introduction.
 * <p>
 * There are no public constructors or accessors.
 * 
 * @author mm
 *
 */
public class UPIImpl implements com.franz.agbase.UPI {
	
	private static final int  CODE_LOWEST = -14;
	private static final UPIImpl         WILD = new UPIImpl(AGConnector.AGU_WILD);
	private static final UPIImpl NULL_CONTEXT = new UPIImpl(AGConnector.AGU_NULL_CONTEXT);
	
	static final int WIDTH = AGDirectLink.UPI_WIDTH;
	
	public byte[] getUpi() {
		return upi;
	}
	public static UPI wildUPI () { return WILD; }
	public static UPIImpl nullUPI () { return NULL_CONTEXT; }
	static String wildString () { return null; }
	static String nullString ( boolean allAllowed ) {
		if ( allAllowed ) return "";
		return null;
	}
	static String allString () { return null; }
	
	public static UPI refNull ( UPI x ) {
		if ( x==null ) return NULL_CONTEXT;
		return x;
	}
	
	static boolean isWild ( UPI x ) { 
		if ( x==null ) return true;
		if ( x instanceof UPIImpl ) return ((UPIImpl) x).isWild();
		return false;
		}
	public boolean isWild () {
		if ( this==WILD ) return true;
		if ( upi!=null ) return false;
		return WILD.code==code;
	}
	public static boolean isNullContext ( UPI x ) {
		if ( x==null ) return false;
		if ( x==NULL_CONTEXT ) return true;
		if ( !(x instanceof UPIImpl )) return false;
		UPIImpl y = (UPIImpl) x;
		if ( y.upi!=null ) return false;
		return (NULL_CONTEXT.code)==y.code;
	}
	public boolean isNullContext () {
		if ( upi!=null ) return false;
		return (NULL_CONTEXT.code)==code;
	}
	
	public static boolean canReference ( UPI x ) {
		// If result is true, can cast x to UPIImpl.
		if ( isNullContext(x) ) return true;
		if ( !(x instanceof UPIImpl )) return false;
		UPIImpl y = (UPIImpl) x;
		if ( y.upi!=null ) return true;
		return false;
	}
	public boolean canReference ()  {
		if ( upi!=null ) return true;
		if ( isNullContext() ) return true;
		return false;
	}
	
	static boolean hasBody ( UPI x ) {
		if ( !(x instanceof UPIImpl )) return false;
		UPIImpl y = (UPIImpl) x;
		return null!=y.upi; }
	boolean hasBody() { return null!=upi; }
	
  byte[] upi = null;
  int setWidth = 0;
  
  long code = CODE_LOWEST;
  int width = WIDTH;
  
  /**
   * 
   * @param x  A code number is negative, a triple id is positive.
   */
  public UPIImpl ( long x ) {
	  super();
	  code = x;
	  width = 0;
  }
  public UPIImpl () {
	  super(); 
	  upi = new byte[WIDTH];
  }
  UPIImpl ( byte[] bb ) {
	  super();
	  upi = new byte[WIDTH];
	  for (int i = 0; i < bb.length; i++) {
		upi[i] = bb[i];
	}
  }
  
  public long getCode () { return code; }
  
  /**
   * 
   * @return true if it may be worth the trip to get the string for this UPI.
   */
  public boolean withLabel () {
	  return (null!=upi);
  }
  
  public void addByte ( int b ) {
	  if ( upi==null )
		  throw new IllegalStateException("Cannot update marker UPI.");
	  if ( setWidth==WIDTH )
		  throw new IllegalStateException("UPI bytes are all there.");
	  upi[setWidth++] = (byte)b;
  }
  
  public int getByte ( int i ) {
	  if ( i<WIDTH ) return  0xff & upi[i];
	  return -1;
  }
  
  
  public String hexByte ( int i ) {
	  String h = Integer.toHexString(getByte(i));
	  if ( "0".equals(h) ) h = "00";
	  if ( 2<h.length() ) h = h.substring(h.length()-2);
	  return h;
  }
  
  public String blankNodeIDString () {
	  return hexByte(0) + hexByte(1)  + hexByte(2)  + hexByte(3);
  }
  
  public long blankNodeID () {
	  return getByte(0) + 256*getByte(1) +
	      256*256*getByte(2) + 256*256*256*getByte(3);
  }
  
  public static int compare ( UPI xx, UPI yy ) {
	  if ( !(xx instanceof UPIImpl) ) return 0;
	  if ( !(yy instanceof UPIImpl) ) return 0;
	  UPIImpl x = (UPIImpl) xx;
	  UPIImpl y = (UPIImpl) yy;
	  if ( x.upi==null && y.upi==null )
	  {
		  if ( x.code==y.code ) return 0;
		  if ( x.code<y.code ) return -1;
		  return 1;
	  }
	  if ( x.upi==null ) return -1;
	  if ( y.upi==null ) return 1;
	  byte[] xb = x.upi;  int xlen = xb.length;
	  byte[] yb = y.upi;  int ylen = yb.length;
	  for (int i = 0; i < xlen; i++) {
		if ( !(i<ylen) ) return 1;
		if ( xb[i]<yb[i] ) return -1;
		if ( xb[i]>yb[i] ) return 1;
	}
	  return 0;
  }
  
  CharSequence asHex () {
	  CharBuffer b = CharBuffer.allocate(2*WIDTH);
	  for (int i = 0; i < WIDTH; i++) {
		  int hi = (0xf0 & upi[i])>>4;
		  int lo = 0x0f & upi[i];
		  b.put("0123456789ABCDEF".charAt(hi));
		  b.put("0123456789ABCDEF".charAt(lo));
	  }
	  b.rewind();
	  return  b;
  }
  
  public CharSequence asChars () {
	  CharBuffer b = CharBuffer.allocate(WIDTH);
	  for (int i = 0; i < WIDTH; i++) {
		b.put(upiByteAsChar(i));
	}
	  b.rewind();
	  return  b;
  }
  
  char upiByteAsChar ( int i ) {
	  	int v = 1 + ( 0xff & upi[i] );
	  	return (char)v;
	  	}
  
  public CharSequence asChars ( CharSequence prefix ) {
	  CharBuffer b = CharBuffer.allocate(WIDTH + prefix.length());
	  for (int i = 0; i < prefix.length(); i++) {
		b.put(prefix.charAt(i));
	}
	  for (int i = 0; i < WIDTH; i++) {
		b.put(upiByteAsChar(i));
	}
	  b.rewind();
	  //AllegroGraph.prdb("asChars", b.toString());
	  return  b;
  }
  
  /* (non-Javadoc)
 * @see com.franz.ag.UPI#equals(java.lang.Object)
 */
public boolean equals ( Object x ) {
	  if ( this==x ) return true;
	  if ( !( x instanceof UPIImpl ) ) return false;
	  UPIImpl y = (UPIImpl)x;
	  if ( upi==null && y.upi==null ) return getCode()==y.getCode();
	  if ( upi==null || y.upi==null ) return false;
	  for (int i = 0; i < upi.length; i++) {
		if ( upi[i]!=y.upi[i] ) return false;
	}
	  return true;
  }
  
  /* (non-Javadoc)
 * @see com.franz.ag.UPI#hashCode()
 */
public int hashCode () {
	  int h = 0;
	  if ( upi==null ) return (int) getCode();
	  for (int i = 0; i < upi.length; i++) {
		h = h + ((upi[i])<<(8*(i%4)));
	}
	 return h;	             
  }
  
  /* (non-Javadoc)
 * @see com.franz.ag.UPI#toString()
 */
public String toString () {
	  if ( upi!=null ) return "<UPI " + asHex() + ">";
	  if ( code==AGConnector.AGU_NULL_CONTEXT ) return "";
	  return "<UPI code=" + code + ">";
  }
  
  /**
   * 
   * @return the 4-byte marker in some UPIs
   */
  public CharSequence getStoreBytes () {
	  if ( null==upi ) return "";
	  CharBuffer b = CharBuffer.allocate(8);
	  for (int i = 4; i < 8; i++) {
		  int hi = (0xf0 & upi[i])>>4;
		  int lo = 0x0f & upi[i];
		  b.put("0123456789ABCDEF".charAt(hi));
		  b.put("0123456789ABCDEF".charAt(lo));
	  }
	  b.rewind();
	  return  b;
  }
  
//  static public void main ( String[] a ) {
//	  int v;
//	  UPI x = new UPI();
//	  x.addByte(127);
//	  v = x.getByte(0);
//	  System.out.println(" " + v);
//	  x.addByte(128);
//	  v = x.getByte(1);
//	  System.out.println(" " + v);
//	  x.addByte(0);
//	  v = x.getByte(2);
//	  System.out.println(" " + v);
//	  x.addByte(-1);
//	  v = x.getByte(3);
//	  System.out.println(" " + v);
//  }
  
}
