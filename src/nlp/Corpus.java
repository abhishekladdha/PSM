package nlp;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import crf.RunCrfPlus;

/**
 * A corpus has following components:
 * 1. Documents where each document contains a list of word ids.
 * 2. Vocabulary: the mapping from word id to word.
 * 3. Candidate Phrases for each document/sentence and their probabilities
 * 
 * @author abhishek
 */

public class Corpus {
	
	/** Domain name */
	public String domain = null;
	
	/** Vocabulary of domain */
	public Vocabulary vocab = null;
	
	public int[][] docs = null;
	public String[][] docsStr = null;
	
	/** N-best sequence */
	public int nBest = 5;
	/** Word, probability of r = 0, r = 1 and label for n best sequence for each document **/
	public ArrayList<ArrayList<String [][]>> wordCrfProb = null;
	
	/**Store the issueProb of all the issue present in document.**/   
	public ArrayList<ArrayList<Double []>> issueProb = null;
	
	/**Assign index to each aspect and corresponding to each index
	   aspectIssue stores all the vocabulary index of issues containing the aspect word
	   and length of those opinion phrases. **/
	public HashMap<String,Integer> aspectIndex = null;
	public HashMap<Integer,HashMap<Integer, Integer> > aspectIssue;
	
	/** Weightage of aspects during promotion */
	public double[] aspectProm;

	/** Build inverted index that is used to compute document
	    frequency and co-document frequency. **/
	private Map<String, HashSet<Integer>> wordstrToSetOfDocsMap = null;

	public Corpus(String domain2) {
		domain = domain2;
		wordstrToSetOfDocsMap = new TreeMap<String, HashSet<Integer>>();
		aspectIssue = new HashMap<Integer,HashMap<Integer, Integer>>();
	}
	
	/**
	 * Generate corpus from the output of CRF file
	 * @param domain
	 * @param docsFilepath: 
	 * @param vocabFilepath:
	 * @param issuesFilePath:
	 */
	
	public void getCorpusFromFile(String domain,  String issuesFilePath, String inputStopwordFile){
		
		/**Add vocabulary to corpus and get documents from CRF ouput*/
		vocab = new Vocabulary(inputStopwordFile);
		ArrayList<ArrayList<String>> documents_list_whole = vocab.getVocabularyFromOutputCRF(issuesFilePath);
		vocab.getVocabFromCrfpp(issuesFilePath);
		
		/** Read word probabilities of pre-trained CRF output*/
		wordCrfProb = RunCrfPlus.parseCrfOutput(issuesFilePath,nBest);
		getAspectIndexForDomain(domain);
		
		int size = documents_list_whole.size();
		docs = new int[size][];
		docsStr = new String[size][];
		issueProb = new ArrayList<ArrayList<Double []>>();
		for (int d = 0; d < size; ++d) {
			ArrayList<String> tokens = documents_list_whole.get(d);
			int word_count = 0;
			StringBuilder currDoc = new StringBuilder();
			for(int i1 =0; i1 < tokens.size(); i1++){
				String s = tokens.get(i1);
				if(vocab.containsWordstr(s)) {
					word_count++;
					currDoc.append(vocab.getWordidByWordstr(s)+ " ");
				}
			}
			
			if(word_count == 0){
				docs[d]  = new int[0];
				docsStr[d] = new String[0];
				ArrayList<Double []> issue = new ArrayList<Double []>();
				issueProb.add(issue);
			}
			
			else{
				String docsLine = currDoc.toString();
				String[] splits = docsLine.trim().split(" ");
				
				// Get all candidate phrases.
				ArrayList<Integer> issue = getIssueOfCurrentReview(d,5);
				
				// Each document is list of indices of words and phrases . 
				int length = splits.length + 1 + issue.size();
				docs[d] = new int[length];
				docsStr[d] = new String[length];
				// Adding words indices to doc
				for (int n = 0; n < splits.length; ++n) {
					int wordid = Integer.parseInt(splits[n]);
					docs[d][n] = wordid;
					docsStr[d][n] = vocab.getWordstrByWordid(wordid);
					
					// Update the inverted index.
					String wordstr = vocab.getWordstrByWordid(wordid);
					if (!wordstrToSetOfDocsMap.containsKey(wordstr)) {
						wordstrToSetOfDocsMap.put(wordstr,
								new HashSet<Integer>());
					}
					HashSet<Integer> setOfDocs = wordstrToSetOfDocsMap
							.get(wordstr);
					setOfDocs.add(d);
				}
				docs[d][splits.length] = -1; // Separate the word and phrase by -1 
				// Adding phrases indices to doc 
				for(int n = splits.length+1; n < length ; n++){
					int wordid = issue.get(n-(splits.length+1));
					docs[d][n] = wordid;
					docsStr[d][n] = vocab.getWordstrByWordid(wordid);
					String wordstr = vocab.getWordstrByWordid(wordid);
					if (!wordstrToSetOfDocsMap.containsKey(wordstr)) {
						wordstrToSetOfDocsMap.put(wordstr,
								new HashSet<Integer>());
					}
					HashSet<Integer> setOfDocs = wordstrToSetOfDocsMap
							.get(wordstr);
					setOfDocs.add(d);
				}
			}
		}
		setAspectProm();
	}
	
	/** Set aspect promotion for each aspect based on the frequency of aspect phrases */
	public void setAspectProm(){
		int minAspectIssue =10000;
		for(HashMap<Integer,Integer> j : aspectIssue.values()){
			if(minAspectIssue > j.size()){
				minAspectIssue = j.size();
			}
		}
		for(Map.Entry<Integer,HashMap<Integer,Integer>> aspect : aspectIssue.entrySet()){
			int i = aspectIndex.get(vocab.getWordstrByWordid(aspect.getKey()));
			aspectProm[i] = Math.max((double) minAspectIssue/aspect.getValue().size(),0.7);
		}
	}
	
	/**
	 * Assign the aspect it's index
	 * @param domain
	 */
	public void getAspectIndexForDomain(String domain){
		aspectIndex = new HashMap<String ,Integer>();
		if(domain.equals("router")){
			aspectIndex.put("connection", 0);
			aspectIndex.put("wireless", 1);
			aspectIndex.put("signal", 2);
			aspectIndex.put("firmware", 3);
		}
		else if(domain.equals("gps")){
			aspectIndex.put("direction", 0);
			aspectIndex.put("screen", 1);
			aspectIndex.put("software", 2);
			aspectIndex.put("voice", 3);
		}
		else if(domain.equals("keyboard")){
			aspectIndex.put("keys", 0);
			aspectIndex.put("pad", 1);
			aspectIndex.put("range", 2);
			aspectIndex.put("spacebar", 3);
		}
		else if(domain.equals("mouse")){
			aspectIndex.put("battery", 0);
			aspectIndex.put("button", 1);
			aspectIndex.put("pointer", 2);
			aspectIndex.put("wheel", 3);
		}
		aspectProm = new double[aspectIndex.size()];
	}
	
	/** 
	 * Read output probabilities of CRF and extract the candidate phrase of each with it's probability
	 * @param d document id
	 * @param m M-best output of crf
	 * @return List of phrase index for current doc
	 */
	public ArrayList<Integer> getIssueOfCurrentReview(int d, int m){
		ArrayList<String [][]> condProb = wordCrfProb.get(d);
		
		HashSet<Integer> issue = new HashSet<Integer>();
		HashMap<Integer,Double []> prob = new HashMap<Integer,Double []>();
		for(int j = 0; j < 4; j++){
			
			/** Computes the probability of r=1 for the issue by taking maximum over all
			its words, r= 0 by taking minimum of over all its words.**/
			
			String curr_issue = "";
			Double[] indProb = new Double[2]; 
			indProb[1] = 0.0;
			indProb[0] = 0.0;
			int nWords = 0;
			for (int i =0; i < condProb.size(); i++){
				String word = condProb.get(i)[j][0].toString().toLowerCase();
				//System.out.println("curr " + condProb.get(i)[j][1] + " " + word);
				if(Integer.parseInt(condProb.get(i)[j][1]) == 1 && vocab.containsWordstr(word) ){
					curr_issue += word+" ";
					nWords++;
					if(Double.parseDouble(condProb.get(i)[j][3]) > indProb[1]){
						indProb[1] = Double.parseDouble(condProb.get(i)[j][3]);
					}
					if(Double.parseDouble(condProb.get(i)[j][2]) < indProb[0]){
						indProb[0] = Double.parseDouble(condProb.get(i)[j][2]);
					}
				}
			}
			/** If issue is present in vocabulary add it in the docs and add it to issue to corresponding aspect. */
			if(vocab.containsWordstr(curr_issue) && nWords > 1){
				int issueId = vocab.getWordidByWordstr(curr_issue);
				issue.add(issueId);
				prob.put(issueId, indProb);
				String[] tokens = curr_issue.split(" ");
				for(int i = tokens.length -1; i >= 0; i--){
					if(aspectIndex.containsKey(tokens[i])){
						int issueWordId = vocab.getWordidByWordstr(tokens[i]);
						if(aspectIssue.containsKey(issueWordId)){
							aspectIssue.get(issueWordId).put(issueId, tokens.length);
						}
						else{
							HashMap<Integer, Integer> yu = new HashMap<Integer, Integer>();
							yu.put(issueId,tokens.length);
							aspectIssue.put(issueWordId, yu);
						}
						break;
					}
				}
			}
		}
		/** Convert the hash set of issue to arraylist and add probability to list of issue probability.*/
		ArrayList<Integer> list = new ArrayList<Integer>(issue);
		ArrayList<Double []> probability = new ArrayList<Double []>();
		for(int i =0; i < list.size(); i++){
			Double[] d2 = prob.get(list.get(i));
			probability.add(d2);
		}
		issueProb.add(probability);
		return list;
	}
	
	/**
	 * Get the number of documents in the corpus.
	 */
	
	public int getNoofDocuments() {
		return docs == null ? 0 : docs.length;
	}

	/**
	 * Get the number of documents that contain this word.
	 */
	public int getDocumentFrequency(String wordstr) {
		if (!wordstrToSetOfDocsMap.containsKey(wordstr)) {
			return 0;
		}
		return wordstrToSetOfDocsMap.get(wordstr).size();
	}

	/**
	 * Get the co-document frequency which is the number of documents that both
	 * words appear.
	 */
	public int getCoDocumentFrequency(String wordstr1, String wordstr2) {
		if (!wordstrToSetOfDocsMap.containsKey(wordstr1)
				|| !wordstrToSetOfDocsMap.containsKey(wordstr2)) {
			return 0;
		}
		HashSet<Integer> setOfDocs1 = wordstrToSetOfDocsMap.get(wordstr1);
		HashSet<Integer> setOfDocs2 = wordstrToSetOfDocsMap.get(wordstr2);
		HashSet<Integer> intersection = new HashSet<Integer>(setOfDocs1);
		intersection.retainAll(setOfDocs2);
		return intersection.size();
	}
}
