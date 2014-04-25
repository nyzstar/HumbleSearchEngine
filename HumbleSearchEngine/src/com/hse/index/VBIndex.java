package com.hse.index;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public class VBIndex implements BaseIndex {
	public static final int INVALID_VBCODE = -1;
	public static final int IntegerSize = Integer.SIZE/7 + 1;
	
	@Override
	public PostingList readPosting(FileChannel fc) {
		//read term Id;
		try{
			long position = fc.position();
			ByteBuffer byteBuffer = ByteBuffer.allocate(IntegerSize * 2);
			byteBuffer.clear();
			int endOfFile = fc.read(byteBuffer);
			if(endOfFile == -1) return null;
			byteBuffer.rewind();
			byte[] byteArray = byteBuffer.array();
			int[] numberEndIndex = new int[2];
			// Read termId;
			VBDecodeInteger(byteArray, 0, numberEndIndex);
			if(numberEndIndex[0] == INVALID_VBCODE){
				return null; //cannot decode term Id.
			}
			Integer termId = numberEndIndex[0];
			
			// Read frequency.
			VBDecodeInteger(byteArray, numberEndIndex[1], numberEndIndex);
			if(numberEndIndex[0] == INVALID_VBCODE){
				return null; //cannot decode term Id.
			}
			Integer freq = numberEndIndex[0];
			int startIndexPostingList = numberEndIndex[1];
			
			//allocate buffer to read entire posting list.
			byteBuffer = ByteBuffer.allocate(IntegerSize * (1 + 1 + freq));
			byteBuffer.clear();
			fc.position(position);
			fc.read(byteBuffer);

			byteArray = byteBuffer.array();
			//Read doc ids.
			List<Integer> docIdList = PostingList.createList();
			int prevDocId = 0;
			for (int i=0; i<freq; i++){
				VBDecodeInteger(byteArray, startIndexPostingList, numberEndIndex);
				if (numberEndIndex[0] == INVALID_VBCODE) 
					return null;
				Integer docId = numberEndIndex[0] + prevDocId;
				prevDocId = docId;
				startIndexPostingList = numberEndIndex[1];
				docIdList.add(docId);
			}
			
			//make sure the file channel is at the right position for the next readPosting call.
			fc.position(position + startIndexPostingList);
			return new PostingList(termId, docIdList);
		}catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void writePosting(FileChannel fc, PostingList posting) {
		Integer termId = posting.getTermId();
		Integer bufferSize = IntegerSize * (1 + 1 + posting.getList().size());
		ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
		byteBuffer.clear();
		
		byte[] outputVBCode = new byte[IntegerSize];
		int numBytes = 0;
		// Term Id.
		numBytes = VBEncodeInteger(termId, outputVBCode);
		byteBuffer.put(outputVBCode, 0, numBytes);
		
		//Frequency.
		numBytes = VBEncodeInteger(posting.getList().size(), outputVBCode);
		byteBuffer.put(outputVBCode, 0, numBytes);
		
		//Doc Ids. Using VB encoding.
		boolean first = true;
		int prevDocId = 0, gap = 0;
		for (Integer docId:posting.getList()){
			if (first){
				numBytes = VBEncodeInteger(docId, outputVBCode);
				byteBuffer.put(outputVBCode, 0, numBytes);
				prevDocId = docId;
				first = false;
			}else{
				gap = docId - prevDocId;
				numBytes = VBEncodeInteger(gap, outputVBCode);
				byteBuffer.put(outputVBCode, 0, numBytes);
				prevDocId = docId;
			}
		}
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
	 * Encodes gap using a VB code.  The encoded bytes are placed in outputVBCode.  Returns the number
	 * bytes placed in outputVBCode.
	 * 
	 * @param gap            gap to be encoded.  Assumed to be greater than or equal to 0.
	 * @param outputVBCode   VB encoded bytes are placed here.  This byte array is assumed to be large
	 * 						 enough to hold the VB code for gap (e.g., Integer.SIZE/7 + 1).
	 * @return				 Number of bytes placed in outputVBCode.
	 */
	public static int VBEncodeInteger(int gap, byte[] outputVBCode) {
		int numBytes = 0;
		int bitMask = 0x7F;
		int addContinuationBit = 0x80;
		// Fill in your code here
		int result = gap & bitMask;
		outputVBCode[numBytes] = (byte) result;
		gap = gap >> 7;
		while (gap != 0){
			numBytes++;
			result = gap & bitMask;
			outputVBCode[numBytes] = (byte) result;
			gap = gap >> 7;
		}
		//add continuation bit.
		outputVBCode[numBytes] = (byte) (outputVBCode[numBytes] | addContinuationBit);
		return numBytes + 1;
	}
	
	
	/**
	 * Decodes the first integer encoded in inputVBCode starting at index startIndex.  The decoded
	 * number is placed in the first element of the numberEndIndex array and the index position
	 * immediately after the encoded value is placed in the second element of numberEndIndex.
	 * 
	 * @param inputVBCode     Byte array containing the VB encoded number starting at index startIndex.
	 * @param startIndex      Index in inputVBCode where the VB encoded number starts
	 * @param numberEndIndex  Outputs are placed in this array.  The first element is set to the
	 * 						  decoded number (or INVALID_VBCODE if there's a problem) and the second
	 * 						  element is set to the index of inputVBCode immediately after the end of
	 * 						  the VB encoded number.
	 */
	public static void VBDecodeInteger(byte[] inputVBCode, int startIndex, int[] numberEndIndex) {
		int bitMask = 0x7F;
		int sum = 0, counter = 0;
		int thisByte = inputVBCode[startIndex];
		
		while(!isEndingByte((byte)thisByte)){
			sum = (thisByte << (7 * counter)) + sum;
			counter++; startIndex++;
			try{
				thisByte = inputVBCode[startIndex];
			}catch(ArrayIndexOutOfBoundsException e){
				numberEndIndex[0] = INVALID_VBCODE;
				return;
			}
		}
		numberEndIndex[0] = ((thisByte & bitMask) << (7 * counter)) + sum;
		numberEndIndex[1] = startIndex + 1;
	}
	
	public static boolean isEndingByte(byte input){
		return (input & 0x80) == 0x80;
	}
	
}
