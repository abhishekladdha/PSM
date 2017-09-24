package nlp;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import crf.RunCrfPlus;
import utility.ExceptionUtility;
import utility.FileReaderAndWriter;

/**
 * Contains mapping from word id to word, and vice versa.
 * @author abhishek
 */

public class Vocabulary {
	/** Assign words to id and vice-versa*/
	public Map<Integer, String> wordidToWordstrMap = null;
	public Map<String, Integer> wordstrToWordidMap = null;
	HashSet<String> stopwords;
	
	public Vocabulary() {
		wordstrToWordidMap = new TreeMap<String, Integer>();
		wordidToWordstrMap = new TreeMap<Integer, String>();
	}
	
	public Vocabulary(String inputStopwordFile) {
		wordstrToWordidMap = new TreeMap<String, Integer>();
		wordidToWordstrMap = new TreeMap<Integer, String>();
		stopwords = new HashSet<String>();
		// Set up stop words
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputStopwordFile));
			String line;
			try {
				while((line = br.readLine())!=null){
					stopwords.add(line.trim());
				}
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Read the vocabulary from the file.
	 * @param filePath 
	 * @return
	 */
	public void getVocabularyFromFile(String filePath) {
		ArrayList<String> lines = FileReaderAndWriter
				.readFileAllLines(filePath);
		for (String line : lines) {
			String[] splits = line.trim().split(":");
			ExceptionUtility.assertAsException(splits.length == 2);
			int wordid = Integer.parseInt(splits[0]);
			String wordstr = splits[1].trim();
			//if(!wordstr.equals("bought"))
			addWordstrWithWordid(wordid, wordstr);
		}
	}
	/**
	 * Generate vocabulary from output of CRF removes stopwords and less frequent words  
	 * @param issuepath output file of CRF
	 * @return List of words of all the sentences
	 */
	public ArrayList<ArrayList<String>> getVocabularyFromOutputCRF(String issuepath) {
		
		HashSet<String> vocab = new HashSet<String>();
		TreeMap<String, Integer> counts = new TreeMap<String, Integer>();
		
		ArrayList<ArrayList<String>> documents_list_whole = new ArrayList<ArrayList<String>>();
	    ArrayList<String> curr_doc = new ArrayList<String>();
		
		String doc;
		
		/**
		 * Parse output of CRF and add sentences/document only once from
		 * n-best sequence output Also build the vocab of words with number of
		 * occurrence.
		 */
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(issuepath));
			boolean firstTime = false;
			while ((doc = br.readLine()) != null) {
				doc = doc.toLowerCase();
				String[] separate = doc.split("\t| |  ");
				if (separate.length == 1 && firstTime == true) {
					documents_list_whole.add(new ArrayList<String>(curr_doc));
					curr_doc.clear();
					firstTime = false;
				} else if (separate.length == 3) {
					if (separate[1].equals("4"))
						firstTime = true;
				} else if (separate.length > 1 && firstTime == true) {
					String s = separate[0];
					/** String should not be number and empty*/
					if (s.length() > 1 && !s.matches("[0-9%]+")) {
						curr_doc.add(s);
						if (!vocab.contains(s)) {
							counts.put(s, 1);
							vocab.add(s);
						} else {
							int count = counts.get(s);
							counts.put(s, count + 1);
						}
					}
				}
			}
			br.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		/** Remove stopwords and infrequent words*/
		for(String s : vocab){
			if(!stopwords.contains(s) && counts.get(s) > 3)
				addWordstrWithoutWordid(s);
		}
		return documents_list_whole;
	}
	/**
	 * Add candidate phrases into vocab with unigram words present in vocab.
	 * @param filePath
	 */
	public void getVocabFromCrfpp(String filePath){
		ArrayList<String> issues = RunCrfPlus.extractIssuePhrase(filePath);
		int n_words = 0;
		for(int i =0; i < issues.size(); i++){
			n_words = 0;
			String issue = issues.get(i);
			String[] tokens = issue.split("_");
			String vocab_issue = "";
			for(int j =0; j < tokens.length; j++){
				if(containsWordstr(tokens[j]) && tokens[j].length() > 1){
					vocab_issue += tokens[j] + " ";
					n_words++;
				}
			}
			if (!containsWordstr(vocab_issue) && n_words > 1) {
				addWordstrWithoutWordid(vocab_issue);
			}	
		}
	}
	/**
	 * Add two vocabulary
	 * @param vocab
	 */
	public void addVocabulary(Vocabulary vocab) {
		for (String wordstr : vocab.wordstrToWordidMap.keySet()) {
			if (!containsWordstr(wordstr)) {
				addWordstrWithoutWordid(wordstr);
			}
		}
	}

	/**
	 * Add a (wordid, wordstr) into the vocabulary. If the wordstr already
	 * exists in the vocabulary, then output errors.
	 */
	public void addWordstrWithWordid(int wordid, String wordstr) {
		ExceptionUtility.assertAsException(!containsWordid(wordid),
				"The word id already exists in the vocabulary!");
		ExceptionUtility.assertAsException(!containsWordstr(wordstr),
				"The word string already exists in the vocabulary!");
		wordidToWordstrMap.put(wordid, wordstr);
		wordstrToWordidMap.put(wordstr, wordid);
	}

	/**
	 * Add a wordstr into the vocabulary. We assign it a new wordid. If the
	 * wordstr already exists in the vocabulary, then output errors.
	 */
	public void addWordstrWithoutWordid(String wordstr) {
		ExceptionUtility.assertAsException(!containsWordstr(wordstr),
				"The word string already exists in the vocabulary!");
		int wordid = this.size();
		wordidToWordstrMap.put(wordid, wordstr);
		wordstrToWordidMap.put(wordstr, wordid);
	}

	public boolean containsWordstr(String wordstr) {
		return wordstrToWordidMap.containsKey(wordstr);
	}

	public boolean containsWordid(int wordid) {
		return wordidToWordstrMap.containsKey(wordid);
	}

	public String getWordstrByWordid(int wordid) {
		ExceptionUtility.assertAsException(containsWordid(wordid));
		return wordidToWordstrMap.get(wordid);
	}

	public int getWordidByWordstr(String wordstr) {
		ExceptionUtility.assertAsException(containsWordstr(wordstr));
		return wordstrToWordidMap.get(wordstr);
	}

	public int size() {
		return wordidToWordstrMap.size();
	}

	public void printToFile(String filepath) {
		StringBuilder sbOutput = new StringBuilder();
		for (Map.Entry<Integer, String> entry : wordidToWordstrMap.entrySet()) {
			int wordid = entry.getKey();
			String wordstr = entry.getValue();
			sbOutput.append(wordid + ":" + wordstr);
			sbOutput.append(System.getProperty("line.separator"));
		}
		FileReaderAndWriter.writeFile(filepath, sbOutput.toString());
	}
}
