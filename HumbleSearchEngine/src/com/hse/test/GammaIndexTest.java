package com.hse.test;

import static org.junit.Assert.assertEquals;

import java.util.BitSet;

import org.junit.Test;

import com.hse.index.GammaIndex;

public class GammaIndexTest {
	
	@Test
	public void TestGammaEncodeInteger() {
		BitSet outputGammaCode = new BitSet();
		int startIndex = GammaIndex.GammaEncodeInteger(0, outputGammaCode, 0);
		assertEquals(outputGammaCode, CreateBitSet(""));
		assertEquals(0, startIndex);
		
		outputGammaCode = new BitSet();
		startIndex = GammaIndex.GammaEncodeInteger(1, outputGammaCode, 0);
		assertEquals(outputGammaCode, CreateBitSet("0"));
		assertEquals(1, startIndex);

		outputGammaCode = new BitSet();
		startIndex = GammaIndex.GammaEncodeInteger(2, outputGammaCode, 0);
		assertEquals(outputGammaCode, CreateBitSet("100"));
		assertEquals(3, startIndex);

		outputGammaCode = new BitSet();
		startIndex = GammaIndex.GammaEncodeInteger(3, outputGammaCode, 0);
		assertEquals(outputGammaCode, CreateBitSet("101"));
		assertEquals(3, startIndex);
		
		outputGammaCode = new BitSet();
		startIndex = GammaIndex.GammaEncodeInteger(4, outputGammaCode, 0);
		assertEquals(outputGammaCode, CreateBitSet("11000"));
		assertEquals(5, startIndex);

		outputGammaCode = new BitSet();
		startIndex = GammaIndex.GammaEncodeInteger(9, outputGammaCode, 0);
		assertEquals(outputGammaCode, CreateBitSet("1110001"));
		assertEquals(7, startIndex);
		
		outputGammaCode = new BitSet();
		startIndex = GammaIndex.GammaEncodeInteger(13, outputGammaCode, 0);
		assertEquals(outputGammaCode, CreateBitSet("1110101"));
		assertEquals(7, startIndex);
		
		outputGammaCode = new BitSet();
		startIndex = GammaIndex.GammaEncodeInteger(24, outputGammaCode, 0);
		assertEquals(outputGammaCode, CreateBitSet("111101000"));
		assertEquals(9, startIndex);

		outputGammaCode = new BitSet();
		startIndex = GammaIndex.GammaEncodeInteger(511, outputGammaCode, 0);
		assertEquals(outputGammaCode, CreateBitSet("11111111011111111"));
		assertEquals(17, startIndex);
		
		
		outputGammaCode = new BitSet();
		startIndex = GammaIndex.GammaEncodeInteger(1025, outputGammaCode, 0);
		assertEquals(outputGammaCode, CreateBitSet("111111111100000000001"));
		assertEquals(21, startIndex);
		
		outputGammaCode = new BitSet();
		startIndex = GammaIndex.GammaEncodeInteger(1891, outputGammaCode, 0);
		assertEquals(outputGammaCode, CreateBitSet("111111111101101100011"));
		assertEquals(21, startIndex);
	}

	@Test
	public void TestGammaDecodeInteger() {
		int numberNextIndex[] = new int[2];
		BitSet inputGammaCode = CreateBitSet("11001");
		GammaIndex.GammaDecodeInteger(inputGammaCode, 0, numberNextIndex);
		assertEquals(5, numberNextIndex[0]);
		assertEquals(5, numberNextIndex[1]);
		
		inputGammaCode = CreateBitSet("0");
		GammaIndex.GammaDecodeInteger(inputGammaCode, 0, numberNextIndex);
		assertEquals(1, numberNextIndex[0]);
		assertEquals(1, numberNextIndex[1]);

		inputGammaCode = CreateBitSet("1111111111111110111111100000000");
		GammaIndex.GammaDecodeInteger(inputGammaCode, 0, numberNextIndex);
		assertEquals(0xFF00, numberNextIndex[0]);
		assertEquals(31, numberNextIndex[1]);
		
		inputGammaCode = CreateBitSet("101");
		GammaIndex.GammaDecodeInteger(inputGammaCode, 0, numberNextIndex);
		assertEquals(3, numberNextIndex[0]);
		assertEquals(3, numberNextIndex[1]);
		
		inputGammaCode = CreateBitSet("111101000");
		GammaIndex.GammaDecodeInteger(inputGammaCode, 0, numberNextIndex);
		assertEquals(24, numberNextIndex[0]);
		assertEquals(9, numberNextIndex[1]);
		
		
	}
	@Test
	public void bitToByteTest(){
		
		
		byte[] bytes = GammaIndex.toByteArray(CreateBitSet("0101"), 8);
		assertEquals((byte)0x50, bytes[0]);
		
		bytes = GammaIndex.toByteArray(CreateBitSet("0"), 1);
		assertEquals((byte)0x00, bytes[0]);
		
	}

	@Test
	public void byteToBitSetTest(){
		
		BitSet bitSet = GammaIndex.toBitSet(new byte[] { (byte) 0xF4, (byte) 0 });
		assertEquals(CreateBitSet("111101000"), bitSet);
		
		bitSet = GammaIndex.toBitSet(new byte[] { (byte) 0 });
		assertEquals(CreateBitSet("0"), bitSet);
		
		bitSet = GammaIndex.toBitSet(new byte[] { (byte) 0x40 });
		assertEquals(CreateBitSet("01"), bitSet);
	}
	
	
	public static BitSet CreateBitSet(String bits) {
		BitSet outputBitSet = new BitSet();
		int bitIndex = 0;
		for (int i = 0; i < bits.length(); ++i) {
			if (bits.charAt(i) == '1') {
				outputBitSet.set(bitIndex++, true);
			} else if (bits.charAt(i) == '0') {
				outputBitSet.set(bitIndex++, false);
			}
		}
		return outputBitSet;
	}

}
