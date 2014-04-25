package com.hse.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.hse.index.VBIndex;

public class VBIndexTest {

	@Test
	public void test() {
		
		assertEquals(true, VBIndex.isEndingByte((byte)0xf3));
		
		int testInt = 100000;
		byte[] outputVBCode = new byte[Integer.SIZE/7 + 1];
		int numBytes = VBIndex.VBEncodeInteger(testInt, outputVBCode);
		printByteArray(outputVBCode);
		
		int[] outputInteger = new int[2];
		VBIndex.VBDecodeInteger(outputVBCode, 0, outputInteger);
		assertEquals(testInt, outputInteger[0]);
		
	}
	
	private void printByteArray(byte[] outputVBCode){
		for (int i=0; i<outputVBCode.length; i++){
			System.out.println(Integer.toBinaryString(outputVBCode[i] & 0xFF));
		}
	}
	
}
