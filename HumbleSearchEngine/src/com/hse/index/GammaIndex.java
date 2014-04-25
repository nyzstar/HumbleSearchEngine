package com.hse.index;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.BitSet;
import java.util.List;

public class GammaIndex implements BaseIndex {
	/**
	 * Integer mask with just the highest order bit set
	 */
	private static final int LSB_MASK = 0x01;
	private static final int[] BIT_MASK_SET = {0x0080, 0x0040, 0x0020, 0x0010, 0x0008, 0x0004, 0x0002, 0x0001};
	
	@Override
	public PostingList readPosting(FileChannel fc) {
		//read term Id;
		try{
			ByteBuffer byteBuffer = ByteBuffer.allocate(12);
			byteBuffer.clear();
			int endOfFile = fc.read(byteBuffer);
			if(endOfFile == -1) return null;
			byteBuffer.rewind();
			int termId = byteBuffer.getInt();
			int freq = byteBuffer.getInt();
			int firstDocId = byteBuffer.getInt();
			
			List<Integer> docIdList = PostingList.createList();
			docIdList.add(firstDocId);
			long position = fc.position();
			int buferSize = 5 * (freq - 1);
			try{
				byteBuffer = ByteBuffer.allocate(buferSize);
			}catch(IllegalArgumentException e){
				e.printStackTrace();
			}
			fc.read(byteBuffer);
			int[] numberEndIndex = new int[2];
			BitSet inputGammaCode = toBitSet(byteBuffer.array());
			int startIndex = 0, prevDocId = firstDocId;
			for (int i = 1; i < freq; i++){
				GammaDecodeInteger(inputGammaCode, startIndex, numberEndIndex);
				prevDocId = prevDocId + numberEndIndex[0];
				docIdList.add(prevDocId);
				startIndex = numberEndIndex[1];
			}
			fc.position(position + (startIndex + 7) / 8);
			return new PostingList(termId, docIdList);
		}catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void writePosting(FileChannel fc, PostingList posting) {
		Integer termId = posting.getTermId();

		BitSet bitSet= new BitSet();
		int nextIndex=0;
		int prevDocId = posting.getList().get(0), gap = 0;
		for (int i = 1; i < posting.getList().size(); i++){
			gap = posting.getList().get(i) - prevDocId;
			nextIndex = GammaEncodeInteger(gap, bitSet, nextIndex);
			prevDocId = posting.getList().get(i);
		}
		
		byte[] gammaGapPosting = toByteArray(bitSet, nextIndex);
		Integer bufferSize = 4 + 4 + 4 + gammaGapPosting.length;
		ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
		byteBuffer.clear();
		// Term Id.
		byteBuffer.putInt(termId);
		// No. of bytes for storing the posting list.
		byteBuffer.putInt(posting.getList().size());
		// put first doc Id;
		byteBuffer.putInt(posting.getList().get(0));
		
		byteBuffer.put(gammaGapPosting);
		
		byteBuffer.flip();
		while(byteBuffer.hasRemaining()){
			try {
				fc.write(byteBuffer);
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}
	
	/**
	 * Gamma encodes number.  The encoded bits are placed in BitSet outputGammaCode starting at
	 * (0-based) index position startIndex.  Returns the index position immediately following the
	 * encoded bits.  If you try to gamma encode 0, then the return value should be startIndex (i.e.,
	 * it does nothing).
	 * 
	 * @param number            Number to be gamma encoded
	 * @param outputGammaCode   Gamma encoded bits are placed in this BitSet starting at startIndex
	 * @param startIndex        Encoded bits start at this index position in outputGammaCode
	 * @return                  Index position in outputGammaCode immediately following the encoded bits
	 */
	public static int GammaEncodeInteger(int number, BitSet outputGammaCode, int startIndex) {
		int nextIndex = startIndex;
		if (number == 0) return startIndex; //do nothing.
		if (number == 1){
			outputGammaCode.clear(startIndex);
			return startIndex + 1;
		}
		boolean[] bitArray = new boolean[Integer.SIZE];
		int countBit = 0;
		boolean curBit = (number & LSB_MASK) == 1;
		number = number >> 1;
		
		while(curBit || number != 0){
			outputGammaCode.set(nextIndex);
			bitArray[countBit] = curBit;
			countBit++;
			nextIndex++;
			curBit = (number & LSB_MASK) == 1;
			number = number >> 1;
		}
		//make the last 1 to 0 as a continuation bit, since we need (actual length - 1) 1s
		outputGammaCode.clear(nextIndex-1);
		
		for (int i=countBit-2; i>=0; i--){
			if(bitArray[i]){
				outputGammaCode.set(nextIndex);
			}else{
				outputGammaCode.clear(nextIndex);
			}
			nextIndex++;
		}
		
		return nextIndex;
	}
	
	/**
	 * Decodes the Gamma encoded number in BitSet inputGammaCode starting at (0-based) index startIndex.
	 * The decoded number is returned in numberEndIndex[0] and the index position immediately following
	 * the encoded value in inputGammaCode is returned in numberEndIndex[1].
	 * 
	 * @param inputGammaCode  BitSet containing the gamma code
	 * @param startIndex      Gamma code starts at this index position
	 * @param numberEndIndex  Return values: index 0 holds the decoded number; index 1 holds the index
	 *                        position in inputGammaCode immediately following the gamma code.
	 */
	public static void GammaDecodeInteger(BitSet inputGammaCode, int startIndex, int[] numberEndIndex) {
		int nextIndex = startIndex;
		if (!inputGammaCode.get(nextIndex)){ // 1
			numberEndIndex[0] = 1;
			numberEndIndex[1] = nextIndex + 1;
			return;
		}
		
		//get the number of 1s to determine the length.
		while (inputGammaCode.get(nextIndex++)){ }
		
		int count = nextIndex - startIndex - 1;
		
		//read the number in bit and comvert it to integer.
		int value = 1; //leading bit.
		for (int i = 0; i < count; i++){
			value = (value << 1) + (inputGammaCode.get(nextIndex) ? 1 : 0);
			nextIndex++;
		}
		
		numberEndIndex[0] = value;
		numberEndIndex[1] = nextIndex;
	}
	
	public static byte[] toByteArray(BitSet bits, int length){
		if (length == 1 && !bits.get(0)) return new byte[1];
	    byte[] bytes = new byte[(length + 7) / 8];
	    for (int i = 0; i < length; i++) {
	    	if (bits.get(i)){
	    		bytes[i / 8] |= (0x01 << (7 - i % 8));
	    	}
	    }
	    return bytes;
	}
	
	public static BitSet toBitSet(byte[] byteArray){
		BitSet result = new BitSet();
		for (int i = 0; i < byteArray.length; i++){
			for (int j = 0; j < 8; j++){
				result.set(i * 8 + j, 
						(byteArray[i] & BIT_MASK_SET[j]) == BIT_MASK_SET[j]);
			}
		}
		return result;
	}
}
