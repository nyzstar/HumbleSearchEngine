package com.hse.index;


import java.util.ArrayList;
import java.util.List;

public class PostingList {

	private int termId;
	/* A list of docIDs (i.e. postings) */
	private List<Integer> postings;

	public PostingList(int termId, List<Integer> list) {
		this.termId = termId;
		this.postings = list;
	}

	public PostingList(int termId) {
		this.termId = termId;
		this.postings = PostingList.createList();
	}

	public int getTermId() {
		return this.termId;
	}

	public List<Integer> getList() {
		return this.postings;
	}
	
	/**
	 * Initialize the underlining list structure.
	 * @return
	 */
	public static List<Integer> createList(){
		return new ArrayList<Integer>();
	}
	
	public String toString(){
		StringBuffer result = new StringBuffer();
		result.append(termId);
		for(Integer docId : postings){
			result.append("\t");
			result.append(docId);
		}
		return result.toString();
	}
}
