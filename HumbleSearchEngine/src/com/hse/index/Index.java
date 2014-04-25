package com.hse.index;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.hse.utils.Pair;

public class Index {

	// Term id -> (position in index file, doc frequency) dictionary
	private static Map<Integer, Pair<Long, Integer>> postingDict 
		= new TreeMap<Integer, Pair<Long, Integer>>();
	// Doc name -> doc id dictionary
	private static Map<String, Integer> docDict
		= new TreeMap<String, Integer>();
	// Term -> term id dictionary
	private static Map<String, Integer> termDict
		= new TreeMap<String, Integer>();
	
	
	// Block queue
	private static LinkedList<File> blockQueue
		= new LinkedList<File>();

	// Total file counter
	private static int totalFileCount = 0;
	// Document counter
	private static int docIdCounter = 0;
	// Term counter
	private static int wordIdCounter = 0;
	// Index
	private static BaseIndex index = null;
	// debug flag
	private static boolean debug = false;


	public static PostingList mergePostingList(PostingList pl1, PostingList pl2){
		if(pl1.getTermId() != pl2.getTermId()) 
			throw new RuntimeException("The term ID of the two posting list don't match!");
		Integer termId = pl1.getTermId();
		List<Integer> docList1 = pl1.getList();
		List<Integer> docList2 = pl2.getList();
		List<Integer> combineList = PostingList.createList();
		
		Integer idx1 = 0, idx2 = 0;
		while (idx1 < docList1.size() && idx2 < docList2.size()){
			if (docList1.get(idx1).compareTo(docList2.get(idx2)) < 0){
				combineList.add(docList1.get(idx1++));
			}else if (docList1.get(idx1).compareTo(docList2.get(idx2)) > 0){
				combineList.add(docList2.get(idx2++));
			}else { //equal.
				combineList.add(docList1.get(idx1));
				idx1++; idx2++;
			}
		}
		
		// put the remaining elements of the list to combined list.
		while (idx1 < docList1.size()){
			combineList.add(docList1.get(idx1++));
		}
		
		while (idx2 < docList2.size()){
			combineList.add(docList2.get(idx2++));
		}
		
		return new PostingList(termId, combineList);
	}
	/* 
	 * Merging of two indices file can be performed linearly 
	 * and with file position pointers only traveling 
	 * in the forward direction, given that the content of two index files is sorted.
	 * */	
	/* I decided to record the position of the posting list of each term in the index file later, 
	 * since merging can be done with file pointers only traveling in the forward direction.
	 * We don't need the file position for the subsequent merging.
	 * */
	public static void mergeInvertedIndexFiles(FileChannel bfc1, 
			FileChannel bfc2, FileChannel mfc) throws IOException{
		
		PostingList pl1, pl2, pl3;
		pl1 = index.readPosting(bfc1);
		pl2 = index.readPosting(bfc2);
		//System.out.println(pl1.toString());
		while (pl1 != null && pl2 != null){
			if (pl1.getTermId() == pl2.getTermId()){
				pl3 = mergePostingList(pl1, pl2);
				index.writePosting(mfc, pl3);
				pl1 = index.readPosting(bfc1);
				pl2 = index.readPosting(bfc2);
				
			}else if (pl1.getTermId() < pl2.getTermId()){
				index.writePosting(mfc, pl1);
				pl1 = index.readPosting(bfc1);
			}else{
				index.writePosting(mfc, pl2);
				pl2 = index.readPosting(bfc2);
			}
		}
		//copy the tails of the remaining list to the combine list.
		while (pl1 != null){
			index.writePosting(mfc, pl1);
			pl1 = index.readPosting(bfc1);	
		}
		while (pl2 != null){
			index.writePosting(mfc, pl2);
			pl2 = index.readPosting(bfc2);	
		}		
	}
	
	public static void main(String[] args) throws IOException {
		/* Parse command line */
		if (args.length != 3) {
			System.err
					.println("Usage: java Index [Basic|VB|Gamma] data_dir output_dir");
			return;
		}

		/* Get index */
		String className = "cs276.assignments." + args[0] + "Index";
		try {
			Class<?> indexClass = Class.forName(className);
			index = (BaseIndex) indexClass.newInstance();
		} catch (Exception e) {
			System.err
					.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
			throw new RuntimeException(e);
		}

		/* Get root directory */
		String root = args[1];
		File rootdir = new File(root);
		if (!rootdir.exists() || !rootdir.isDirectory()) {
			System.err.println("Invalid data directory: " + root);
			return;
		}

		/* Get output directory */
		String output = args[2];
		File outdir = new File(output);
		if (outdir.exists() && !outdir.isDirectory()) {
			System.err.println("Invalid output directory: " + output);
			return;
		}

		if (!outdir.exists()) {
			if (!outdir.mkdirs()) {
				System.err.println("Create output directory failure");
				return;
			}
		}

		/* BSBI indexing algorithm */
		File[] dirlist = rootdir.listFiles();
		
		/* For each block */
		for (File block : dirlist) {
			File blockFile = new File(output, block.getName());
			blockQueue.add(blockFile);
			File blockDir = new File(root, block.getName());
			File[] filelist = blockDir.listFiles();
//			if	(filelist == null) continue;
			// Term Id -> Doc id Collections
			ArrayList<Pair<Integer, Integer>> termIdDocIdPairsCollection = new ArrayList<Pair<Integer, Integer>>();
			/* For each file */
			for (File file : filelist) {
				++totalFileCount;
				String fileName = block.getName() + "/" + file.getName();
				docDict.put(fileName, docIdCounter);
				
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line;
				while ((line = reader.readLine()) != null) {
					String[] tokens = line.trim().split("\\s+");
					for (String token : tokens) {
						Integer termId;
						if ((termId = termDict.get(token)) != null){
						}else{
							termId = termDict.size();
							termDict.put(token, termId);
						}
						termIdDocIdPairsCollection.add(new Pair<Integer, Integer>((int)termId, docIdCounter));
					}
				}
				docIdCounter++;
				reader.close();
			}

			/* Sort and output */
			if (!blockFile.createNewFile()) {
				System.err.println("Create new block failure.");
				return;
			}
			
			RandomAccessFile bfc = new RandomAccessFile(blockFile, "rw");
			FileChannel blockFileChannel = null;
			blockFileChannel = bfc.getChannel();
			
			Collections.sort(termIdDocIdPairsCollection, new Comparator<Pair<Integer, Integer>>(){
				@Override
				public int compare(Pair<Integer, Integer> tdPair1,
						Pair<Integer, Integer> tdPair2) {
				    int firstResult = tdPair1.getFirst().compareTo(tdPair2.getFirst());
				    if (firstResult == 0) { //First comparison returned that both elements are equal
				        return tdPair1.getSecond().compareTo(tdPair2.getSecond());
				    } else {
				        return firstResult;
				    }
				}
			});
			
			
			int prevTermId = -1;
			//collect and write to file.
			List<Integer> tempList = null;
			for (int i=0; i<termIdDocIdPairsCollection.size(); i++){
				//create posting list and write posting list to file.
				Pair<Integer, Integer> currentPair = termIdDocIdPairsCollection.get(i);
				if(currentPair.getFirst() != prevTermId){
					if(prevTermId != -1){
						PostingList postingList = new PostingList(prevTermId, tempList);
						index.writePosting(blockFileChannel, postingList);
					}
					tempList = PostingList.createList();
					prevTermId = currentPair.getFirst();
				}
				if(tempList.isEmpty() || 
						!tempList.get(tempList.size()-1).equals(currentPair.getSecond())){
					tempList.add(currentPair.getSecond());
				}
			}
			PostingList postingList = new PostingList(prevTermId, tempList);
			index.writePosting(blockFileChannel, postingList);
			bfc.close();
		}

		/* Required: output total number of files. */
		System.out.println(totalFileCount);

		/* Merge blocks */
		while (true) {
			if (blockQueue.size() <= 1)
				break;

			File b1 = blockQueue.removeFirst();
			File b2 = blockQueue.removeFirst();
			
			File combfile = new File(output, b1.getName() + "+" + b2.getName());
			if (!combfile.createNewFile()) {
				System.err.println("Create new block failure.");
				return;
			}
			
			RandomAccessFile bf1 = new RandomAccessFile(b1, "r");
			RandomAccessFile bf2 = new RandomAccessFile(b2, "r");
			RandomAccessFile mf = new RandomAccessFile(combfile, "rw");
			
			FileChannel bfc1 = bf1.getChannel();
			FileChannel bfc2 = bf2.getChannel();
			FileChannel mfc = mf.getChannel();
			
			mergeInvertedIndexFiles(bfc1, bfc2, mfc);
			
			bf1.close();
			bf2.close();
			mf.close();
			b1.delete();
			b2.delete();
			

			blockQueue.add(combfile);
		}
		
		// construct postingDict to record the position of posting list for each term.
		// TODO: one possible optimization is to record the position during the final merge.
		File indexFile = blockQueue.removeFirst();
		RandomAccessFile raf = new RandomAccessFile(indexFile, "r");
		FileChannel fc = raf.getChannel();
		Long position = fc.position();
		PostingList pl = index.readPosting(fc);
		while(pl != null){
			postingDict.put(pl.getTermId(), new Pair<Long, Integer>(position, pl.getList().size()));
			position = fc.position();
			pl = index.readPosting(fc);
		}
		raf.close();
		
		/* Dump constructed index back into file system */
		indexFile.renameTo(new File(output, "corpus.index"));

		BufferedWriter termWriter = new BufferedWriter(new FileWriter(new File(
				output, "term.dict")));
		for (String term : termDict.keySet()) {
			termWriter.write(term + "\t" + termDict.get(term) + "\n");
		}
		termWriter.close();

		BufferedWriter docWriter = new BufferedWriter(new FileWriter(new File(
				output, "doc.dict")));
		for (String doc : docDict.keySet()) {
			docWriter.write(doc + "\t" + docDict.get(doc) + "\n");
		}
		docWriter.close();

		BufferedWriter postWriter = new BufferedWriter(new FileWriter(new File(
				output, "posting.dict")));
		for (Integer termId : postingDict.keySet()) {
			postWriter.write(termId + "\t" + postingDict.get(termId).getFirst()
					+ "\t" + postingDict.get(termId).getSecond() + "\n");
		}
		postWriter.close();
	}

}
