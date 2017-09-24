package crf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;

import utility.FileReaderAndWriter;
import utility.ThreadInputStream;

/**
 * Class to train, test the CRF model using crf++, extract the probabilities and
 * phrases from output of crf++.
 * @author abhishek
 */

public class RunCrfPlus {
	/** Combine all the aspect sentences to single file
	 * @param Path of 
	 * @throws Exception
	 * Combine all the head aspect of one domain to one file.
	 */
	public static void combinedAspects(String inputPath, String outputPath) throws Exception{
		
		File path_in = new File(inputPath);
	    File[] files_in = path_in.listFiles();
	    String doc;
	    StringBuilder sbLine = new StringBuilder();
	    for(int u = 0; u < files_in.length; u++){
	    	BufferedReader br = new BufferedReader(new FileReader(files_in[u]));
			//System.out.println("file names "+files_in[u].getPath());
			
			while((doc = br.readLine()) != null){
				sbLine.append(doc + "\n");
			}
			br.close();
	    }
	    FileReaderAndWriter.writeFile(outputPath,sbLine.toString());
	}
	/**
	 * Combine all the head aspect of all domain to one file.
	 * @param inputPath
	 * @param outputPath
	 * @throws Exception
	 */
	public static void combinedDomains(String inputPath, String outputPath) throws Exception{
		File file_path = new File(inputPath);
	    File[] files = file_path.listFiles();
	    //String full = "";
	    StringBuilder sbLine = new StringBuilder();
	    for (int k = 0; k < files.length; k++){
	    	//System.out.println(files[k]);
	    	if (files[k].isDirectory()){
		    //if (files[k].isDirectory() && ! files[k].getName().equals("keyboard") ){ 
				File path_in = new File(files[k].getPath());
			    File[] files_in = path_in.listFiles();
			    //System.out.println(files_in.length);
			    String doc;
			    for(int u = 0; u < files_in.length; u++){
			    	BufferedReader br = new BufferedReader(new FileReader(files_in[u]));
					//System.out.println("file names "+files_in[u].getPath());
					
					while((doc = br.readLine()) != null){
						sbLine.append(doc + "\n");
						//full += doc +"\n" ; 
					}
					//full = full.substring(0, full.length()-1);
					br.close();
			    }
		    }
	    }
	    FileReaderAndWriter.writeFile(outputPath , sbLine.toString());
	}
	
	/**
	 * Train CRF model using trainFilePath as training file with template as feature format
	 * , testFilePath as test file and redirect output to file OutputPath
	 * @param trainFilePath
	 * @param testFilePath
	 * @param OutputPath
	 * @throws Exception
	 */
	
	public static void trainCRF(String trainFilePath)throws Exception{
		Runtime r = Runtime.getRuntime();
		Process proc = r.exec("crf_learn ./template "+ trainFilePath + " runCrf.model");
		ThreadInputStream errorGobbler = new ThreadInputStream(proc.getErrorStream());
		ThreadInputStream outputGobbler = new ThreadInputStream(proc.getInputStream());
		errorGobbler.run(true, "a");
		outputGobbler.run(true,"a");
		proc.waitFor();
		proc.destroy();
	}
	
	/**
	 * Train the CRF on the train sentences and output the n-best sequence labeling for test sentences 
	 * @param trainFilePath
	 * @param testFilePath
	 * @param OutputPath
	 * @throws Exception
	 */
	public static void runCrf(String trainFilePath,String testFilePath,String OutputPath) 
			throws Exception{
		try {
			Runtime r = Runtime.getRuntime();
			Process proc = r.exec("crf_learn ./template "+ trainFilePath + " runCrf.model");
			ThreadInputStream errorGobbler = new ThreadInputStream(proc.getErrorStream());
			ThreadInputStream outputGobbler = new ThreadInputStream(proc.getInputStream());
			errorGobbler.run(true, "a");
			outputGobbler.run(true,"a");
			proc.waitFor();
			proc.destroy();
			
			System.out.println("training completed");
			proc = r.exec("crf_test -v2 -n 5 -m ./runCrf.model "+ testFilePath);
			//errorGobbler = new ThreadInputStream(proc.getErrorStream());
			outputGobbler = new ThreadInputStream(proc.getInputStream());
			//errorGobbler.start();
			outputGobbler.run(OutputPath);
			proc.waitFor();
			proc.destroy();
			System.out.println("testing completed");
		}
		catch(InterruptedException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the n-best sequence for input file sentences
	 * @param testFilePath  input file path
	 * @param OutputPath output file path
	 * @throws Exception  File not found
	 */
	
	public static void testCrf(String testFilePath,String OutputPath) 
			throws Exception{
		Runtime r = Runtime.getRuntime();
		Process proc = r.exec("crf_test -v2 -n 5 -m ./runCrf.model "+ testFilePath);
		//errorGobbler = new ThreadInputStream(proc.getErrorStream());
		ThreadInputStream outputGobbler = new ThreadInputStream(proc.getInputStream());
		//errorGobbler.start();
		outputGobbler.run(OutputPath);
		proc.waitFor();
		proc.destroy();
		//System.out.println("Phrase detection using CRF++ completed");
	}
	
	/**
	 * Extract all the issues from output file filename. 
	 * @param filePath output file of CRF++
	 * @return ArrayList of all the issue
	 */
	
	public static ArrayList<String> extractIssuePhrase(String filePath){
		// To remove the duplicates in the issue phrases create a hash set.
		HashSet<String> issues = new HashSet<String>();
		ArrayList<String> lines = FileReaderAndWriter.readFileAllLines(filePath);
		//System.out.println(lines.size());
		String issue = "";
		int prevClass = 0; 
		//int nBest = 1;
		for(int i = 0; i < lines.size();i++){
			String[] tokens = lines.get(i).split("\t| |  ");
			if(tokens.length > 3 ){
				int t = Integer.parseInt(tokens[tokens.length-3].split("/")[0]);
				if(prevClass ==1 && t == 1){
					issue += "_" +tokens[0].replaceAll("\\s","");					
				}
				else if(prevClass ==1 && t == 0){
					issues.add(issue);
					issue = "";
				}
				else{
					issue = tokens[0];
				}
				prevClass = t;
			}
		}
		ArrayList<String> list = new ArrayList<String>(issues);
		return list;
	}
	
	// Todo:Generalize it for n.
	/**
	 * Extract all the n-possible sentences with probability of each token for each sentence
	 * by parsing the crf output file.   
	 * @param filePath
	 * @return 
	 */
	
	public static ArrayList<ArrayList<String [][]>>  parseCrfOutput(String filePath, int nBest){
		
		ArrayList<String> lines = FileReaderAndWriter.readFileAllLines(filePath);
		ArrayList<Integer> sizeReview = new ArrayList<Integer>();
		int currReviewSize = 0;
		boolean isCount = true;
		/** Get size of each sentence for initialization of array storing word probabilities */
		for(int i = 0; i < lines.size();i++){
			String[] tokens = lines.get(i).split("\t| |  ");
			if(tokens.length == 3){
				if(Integer.parseInt(tokens[1]) == 0)
					isCount = true;
				else if(Integer.parseInt(tokens[1]) > 0)
					isCount = false;
				if(Integer.parseInt(tokens[1]) == (nBest -1)) {
					sizeReview.add(currReviewSize);
					currReviewSize = 0;
				}		
			}
			else if(tokens.length > 3 && isCount == true){
				currReviewSize++;
			}
		}
		//Dummy Addition of review of size 1
		sizeReview.add(1);
		
		ArrayList<ArrayList<String [][]>> condProb = new ArrayList<ArrayList<String [][]>>();
		
		int currNBest = -1;
		ArrayList<String [][]> tempCond = new ArrayList<String [][]>();
		// Intialize the array for first sentence
		for(int i =0; i < sizeReview.get(0); i++){
			String [][] temp = new String [nBest][4];
			tempCond.add(temp);
		}
		
		int review_no = 2;
		int curWordIndex = -1;
		
		for(int i = 0; i < lines.size();i++){
			String[] tokens = lines.get(i).split("\t| |  ");
			//System.out.println(tokens.length);
			if(tokens.length == 1 && currNBest == (nBest-1)){
				condProb.add(new ArrayList<String[][]>(tempCond));
				tempCond.clear();
				//System.out.println(tempCond.size());
				//System.out.println(sizeReview.get(review_no-1));
				for(int j =0; j < sizeReview.get(review_no-1); j++){
					String [][] temp = new String [nBest][4];
					tempCond.add(temp);
				}
				review_no++;	
			}
			else if(tokens.length == 3){
				currNBest = Integer.parseInt(tokens[1]);
				curWordIndex = 0;
			}
			else if(tokens.length > 3){
				tempCond.get(curWordIndex)[currNBest][0] = tokens[0];
				tempCond.get(curWordIndex)[currNBest][1] = tokens[tokens.length-3].split("/")[0];
				tempCond.get(curWordIndex)[currNBest][2] = tokens[tokens.length-2].split("/")[1];
				tempCond.get(curWordIndex)[currNBest][3] = tokens[tokens.length-1].split("/")[1];
				curWordIndex++;
			}
		}
		return condProb;
	}
	
	/**
	 * Run the CRF++ on the review sentences and output the n-best in file 
	 * @param domain
	 * @param reviewPath
	 * @param inputCorporeaDirectory
	 * @param suffixInputCorporeaPhrases
	 * @param inputReviewDirectory
	 * @throws Exception
	 */
	public static void crfTask(String domain,String inputCorporeaDirectory,
			String suffixInputCorporeaPhrases, String inputReviewDirectory) throws Exception{
		
		String outputPath = inputReviewDirectory + domain +"/"+ domain +".txt";
    	//combinedAspects(reviewPath,outputPath);
		String phrasePath = inputCorporeaDirectory + domain + File.separator + 
								domain + suffixInputCorporeaPhrases;
		testCrf( outputPath , phrasePath);
		
	}
}
