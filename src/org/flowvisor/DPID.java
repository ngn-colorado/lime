package org.flowvisor;

import java.io.PrintStream;
import java.math.BigInteger;

public class DPID {
	private String dpidString;
	private String dpidHexString;
	private BigInteger dpidBigInt;
	
	public DPID(String DPID){
		try{
			dpidBigInt = DPIDStringToBigInteger(DPID);
			dpidHexString = DPIDBigIntegerToHex(dpidBigInt);
			dpidString = DPID;
		} catch(IllegalArgumentException e){
			dpidBigInt = null;
			dpidString = null;
			dpidHexString = null;
			e.printStackTrace();
			throw new IllegalArgumentException("DPID string is incorrectly formatted: "+e.getMessage());
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((dpidBigInt == null) ? 0 : dpidBigInt.hashCode());
		result = prime * result
				+ ((dpidHexString == null) ? 0 : dpidHexString.hashCode());
		result = prime * result
				+ ((dpidString == null) ? 0 : dpidString.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DPID other = (DPID) obj;
		if (dpidBigInt == null) {
			if (other.dpidBigInt != null)
				return false;
		} else if (!dpidBigInt.equals(other.dpidBigInt))
			return false;
		if (dpidHexString == null) {
			if (other.dpidHexString != null)
				return false;
		} else if (!dpidHexString.equals(other.dpidHexString))
			return false;
		if (dpidString == null) {
			if (other.dpidString != null)
				return false;
		} else if (!dpidString.equals(other.dpidString))
			return false;
		return true;
	}

	public DPID(Long DPID){
		dpidBigInt = new BigInteger(Long.toHexString(DPID), 16);
		dpidString = DPIDLongToDPIDString(dpidBigInt.longValue());
		dpidHexString = DPIDLongToHex(dpidBigInt.longValue());
	}
	
	public DPID(BigInteger DPID){
		if(DPID.bitCount()>64){
			throw new IllegalArgumentException("Input BigInteger has too many bits");
		}
		dpidBigInt = DPID;
		dpidString = DPIDLongToDPIDString(dpidBigInt.longValue());
		dpidHexString = DPIDLongToHex(dpidBigInt.longValue());   
	}
	
	public static BigInteger DPIDHexToBigInteger(String DPID){
		try{
			String DPID2 = DPID.replaceAll("0x", "");
			DPID2 = DPID2.replaceAll("0X", "");
			DPID2 = DPID2.replaceAll("#", "");
			return new BigInteger(DPID2, 16);
		} catch(NumberFormatException e){
//			e.printStackTrace();
			return null;
		}
	}
	
	public static BigInteger DPIDStringToBigInteger(String DPID){
		BigInteger dpidBigInteger = DPIDHexToBigInteger(DPID);
		if(dpidBigInteger != null){
			return dpidBigInteger;
		}
		String dpidString = null;
		try{
			dpidString = DPIDStringToHex(DPID, "");
		} catch(IllegalArgumentException e){
			dpidBigInteger = DPIDHexToBigInteger(DPID);
			return dpidBigInteger;
		}
//		System.out.println(dpidString.toUpperCase());
		dpidBigInteger = DPIDHexToBigInteger(dpidString);
		if(dpidBigInteger == null){
			return DPIDHexToBigInteger("-"+dpidString);
		} else{
			return dpidBigInteger;
		}
	}
	
	public static String DPIDStringToHex(String DPID, String identifier){
//		DPID = DPID.toUpperCase();
		
		String[] tokens = DPID.split(":");
		if(tokens.length != 8){
			throw new IllegalArgumentException("DPID must be 64 bits containing eight bytes. Incorrect string: "+DPID);
		}
		String dpidString = identifier;
		for(int i = 0; i < tokens.length; i++){
			if(tokens[i].length() != 2){
				throw new IllegalArgumentException("Byte "+tokens[i]+" is of incorrect length to be a hex byte. Incorrect string: "+DPID);
			}
			dpidString += tokens[i];
		}
		return dpidString;
	}
	
	public static String DPIDHexToString(String DPID, String identifier){
		String dpidString = "";
//		DPID = DPID.toUpperCase();
		DPID = DPID.replaceAll(identifier, "");
//		System.out.println(DPID);
		BigInteger bigDPID = new BigInteger(DPID, 16);
//		DPID = bigDPID.toString(16);
		
		String newDPID = Long.toHexString(bigDPID.longValue());
//		newDPID = String.format("%016x", bigDPID.longValue());
		newDPID = DPIDLongToHex(bigDPID.longValue());
//		System.out.println(newDPID + "\n");
		if(newDPID.length() != 16){
			throw new IllegalArgumentException("Input string does contain the correct amount of hex characters");
		}
		int colonCount = 0;
		for(int i=0; i<newDPID.length(); i++){
			dpidString += newDPID.charAt(i);
			if(i%2 == 1 && i != newDPID.length() - 1){
				colonCount++;
			}
		}
		if(colonCount != 7){
			throw new IllegalArgumentException("Hex string was incorrect. " + colonCount + " colon seperates written. Generated dpid string was "+newDPID);
		}
		return dpidString;
	}
	
	public static String DPIDBigIntegerToHex(BigInteger DPID, String identifier){
		return DPIDLongToHex(DPID.longValue(), identifier);
	}
	
	public static String DPIDBigIntegerToHex(BigInteger DPID){
		return DPIDBigIntegerToHex(DPID, "");
	}
	
	public static String DPIDBigIntegerToDPIDString(BigInteger DPID){
		return DPIDLongToDPIDString(DPID.longValue());
	}
	
	public static String DPIDLongToHex(Long DPID){
//		String dpid = Long.toHexString(DPID);
//		int neededZeros = 16 - dpid.length();
//		for(int i = 0; i<neededZeros; i++){
//			dpid = "0" + dpid;
//		}
		String dpid = String.format("%016x", DPID);
		return dpid;
	}
	
	public static String DPIDLongToHex(Long DPID, String hexIdentier){
		if(hexIdentier.length() > 0){
			return hexIdentier + DPIDLongToHex(DPID);
		} else{
			return DPIDLongToHex(DPID);
		}
	}
	
	public static String DPIDLongToDPIDString(Long DPID){
//		String dpid = DPIDLongToHex(DPID);
		
		String dpidHexString = DPIDLongToHex(DPID);
		
		return DPIDHexToString(dpidHexString, "");
		
	}
	
	public String getDpidString() {
		return dpidString;
	}

	public String getDpidHexString() {
		return dpidHexString;
	}

	public BigInteger getDpidBigInt() {
		return dpidBigInt;
	}
	
	public Long getDpidLong(){
		return dpidBigInt.longValue();
	}

	public static void main(String[] args){
//		String dpidString = "aa:bb:cc:ee:ff:11:22:33";
		String dpidString = "00:A4:23:05:00:00:00:01";
		System.out.println("Original DPID: "+dpidString);
//		String dpid2 = "7a:bb:cc:ee:ff:11:22:33";
//		System.out.println(Long.parseLong("-"+DPID.DPIDStringToHex(dpid2, "").toUpperCase(), 16));
		DPID dpid = new DPID(dpidString);
		System.out.println("Hex string: "+dpid.getDpidHexString());
		System.out.println("DPID string: "+dpid.getDpidString());
		System.out.println("DPID long value: "+dpid.getDpidLong());
		String dpidHex = DPID.DPIDStringToHex(dpidString, "0x");
		System.out.println("DPID hex w/ 0x: "+dpidHex);
		System.out.println("DPID hex w/ 0x converted to DPID string: "+DPID.DPIDHexToString(dpidHex, "0x"));
		DPID dpid2 = new DPID(dpidHex);
		System.out.println("Long value of dpid created from hex string: "+dpid2.getDpidLong());
		BigInteger big = new BigInteger(DPID.DPIDStringToHex(dpidString, ""), 16);
		System.out.println("Big intger value of: "+dpidHex+" is: "+big.longValue());
		System.out.println("Number of bits: "+big.bitLength());
		System.out.println("Binary string of Long value: "+Long.toBinaryString(big.longValue()));
		System.out.println("Binary string of BigInteger value: "+big.toString(2));
	}
	
}
