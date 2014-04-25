package com.hse.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Query {

	// Term id -> position in index file
	private static Map<Integer, Long> posDict = new TreeMap<Integer, Long>();
	// Term id -> document frequency
	private static Map<Integer, Integer> freqDict = new TreeMap<Integer, Integer>();
	// Doc id -> doc name dictionary
	private static Map<Integer, String> docDict = new TreeMap<Integer, String>();
	// Term -> term id dictionary
	private static Map<String, Integer> termDict = new TreeMap<String, Integer>();
	// Index
	private static BaseIndex index = null;

	
	/* 
	 * Write a posting list with a given termID from the file 
	 * You should seek to the file position of this specific
	 * posting list and read it back.
	 * */
	private static PostingList readPosting(FileChannel fc, int termId)
			throws IOException {
		Long position = posDict.get(termId);
		fc.position(position);
		return index.readPosting(fc);
	}

	public static List<Integer> intersectTwoPostingLists(List<Integer> l1, List<Integer> l2){
		List<Integer> intersection = PostingList.createList();
		if (l1.isEmpty() || l2.isEmpty()) return intersection;
		int idx1 = 0, idx2 = 0;
		while (idx1 < l1.size() && idx2 < l2.size()){
			if(l1.get(idx1).equals(l2.get(idx2))){
				intersection.add(l1.get(idx1));
				idx1++; idx2++;
			}else if(l1.get(idx1).compareTo(l2.get(idx2)) < 0){
				idx1++;
			}else{
				idx2++;
			}
		}
		return intersection;
	}
	
	public static void main(String[] args) throws IOException {
		/* Parse command line */
		if (args.length != 2) {
			System.err.println("Usage: java Query [Basic|VB|Gamma] index_dir");
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

		/* Get index directory */
		String input = args[1];
		File inputdir = new File(input);
		if (!inputdir.exists() || !inputdir.isDirectory()) {
			System.err.println("Invalid index directory: " + input);
			return;
		}

		/* Index file */
		RandomAccessFile indexFile = new RandomAccessFile(new File(input,
				"corpus.index"), "r");

		String line = null;
		/* Term dictionary */
		BufferedReader termReader = new BufferedReader(new FileReader(new File(
				input, "term.dict")));
		while ((line = termReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			termDict.put(tokens[0], Integer.parseInt(tokens[1]));
		}
		termReader.close();

		/* Doc dictionary */
		BufferedReader docReader = new BufferedReader(new FileReader(new File(
				input, "doc.dict")));
		while ((line = docReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			docDict.put(Integer.parseInt(tokens[1]), tokens[0]);
		}
		docReader.close();

		/* Posting dictionary */
		BufferedReader postReader = new BufferedReader(new FileReader(new File(
				input, "posting.dict")));
		while ((line = postReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			posDict.put(Integer.parseInt(tokens[0]), Long.parseLong(tokens[1]));
			freqDict.put(Integer.parseInt(tokens[0]),
					Integer.parseInt(tokens[2]));
		}
		postReader.close();

		/* Processing queries */
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		/* For each query */
		while ((line = br.readLine()) != null) {
			String[] words = line.trim().split("\\s+");
			if (words.length == 0){
				System.out.println("no results found");
				continue;
			}
			
			Integer[] termIds = new Integer[words.length];
			Boolean alienTermFound = false;
			for (int i=0; i<words.length; i++){
				Integer termId = termDict.get(words[i]);
				// check for outside dictionary terms.
				if(termId == null) { alienTermFound = true; break; }
				termIds[i] = termId;
			}
			if (alienTermFound){
				System.out.println("no results found");
				continue;
			}
			
			// Sort the words in the increasing order of their posting list length.
			Arrays.sort(termIds, new Comparator<Integer>(){
				@Override
				public int compare(Integer termId1, Integer termId2) {
					Integer freq1 = freqDict.get(termId1), freq2 = freqDict.get(termId2);
					if (freq1 == null) return -1;
					if (freq2 == null) return 1;
					return freqDict.get(termId1) - freqDict.get(termId2);
				}
			});
			
			// Intersect all the posting list.
			FileChannel ifc = indexFile.getChannel();
			List<Integer> combinedList = readPosting(ifc, termIds[0]).getList();
			for (int i=1; i<termIds.length; i++){
				List<Integer> li = readPosting(ifc, termIds[i]).getList();
				combinedList = intersectTwoPostingLists(combinedList, li);
			}
			
			//sort the docs in in lexicographical order.
			Collections.sort(combinedList, new Comparator<Integer>(){
				@Override
				public int compare(Integer docId1, Integer docId2) {
					return docDict.get(docId1).compareTo(docDict.get(docId2));
				}
			});
			
			//Print the query result to the console.
			if (combinedList.isEmpty()){
				System.out.println("no results found");
			}else{
				for (Integer docId:combinedList){
					System.out.println(docDict.get(docId));
				}
			}
			
		}
		br.close();
		indexFile.close();
	}
}
