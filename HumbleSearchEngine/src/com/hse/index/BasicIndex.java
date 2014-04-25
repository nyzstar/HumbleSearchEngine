package com.hse.index;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public class BasicIndex implements BaseIndex {

	@Override
	public PostingList readPosting(FileChannel fc) {
		//read term Id;
		try{
			ByteBuffer byteBuffer = ByteBuffer.allocate(8);
			byteBuffer.clear();
			int endOfFile = fc.read(byteBuffer);
			if(endOfFile == -1) return null;
			byteBuffer.flip();
			Integer termId = byteBuffer.getInt();
			Integer freq = byteBuffer.getInt();
	//		System.out.println("Term Id: " + termId + " Freq: " + freq);
			byteBuffer.clear();
			
			List<Integer> docIdList = PostingList.createList();
			byteBuffer = ByteBuffer.allocate(4 * freq);
			fc.read(byteBuffer);
			byteBuffer.rewind();
			//Read doc ids.
			for (int i=0; i<freq; i++){
				Integer docId = byteBuffer.getInt();
				docIdList.add(docId);
			}
			return new PostingList(termId, docIdList);
		}catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void writePosting(FileChannel fc, PostingList posting) {
		Integer termId = posting.getTermId();
		Integer bufferSize = 4 * (1 + 1 + posting.getList().size());
		ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
		byteBuffer.clear();
		// Term Id.
		byteBuffer.putInt(termId);
		//Frequency.
		byteBuffer.putInt(posting.getList().size());
		//Doc Ids.
		for (Integer docId:posting.getList()){
			byteBuffer.putInt(docId);
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
}
