package launch;

import java.io.File;
import java.util.ArrayList;

import global.CmdOption;
import model.ModelParameters;
import model.ModelPrinter;
import model.TopicModel;
import nlp.Corpus;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import crf.RunCrfPlus;

/**
 * The main entry of the program.
 */

public class MainEntry {
	
	static CmdOption cmdOption = new CmdOption();
	static CmdLineParser parser = new CmdLineParser(cmdOption);
	
	public static void main(String[] args) {
		try {
			
			long startTime = System.currentTimeMillis();
			System.out.println("Program Starts.");
			
			// Parse the arguments.
			parser.parseArgument(args);
			  
			// Check if the input directory is valid.
			if (new File(cmdOption.inputReviewDirectory).listFiles() == null) {
				System.err.println("Input directory is not correct, program exits!");
				return;
			}
			
			// Run the proposed method on the multiple domains.
			run();
			 
			System.out.println("Program Ends.");
			long endTime = System.currentTimeMillis();
			showRunningTime(endTime - startTime);
			
		} catch (CmdLineException cle) {
			System.out.println("Command line error: " + cle.getMessage());
			showCommandLineHelp(parser);
			return;
		} catch (Exception e) {
			System.out.println("Error in program: " + e.getMessage());
			e.printStackTrace();
			return;
		}
	}
	/** generate corpora for PSM for all domain from output of CRF */
	private static ArrayList<Corpus> getCorpora(String inputCorporeaDirectory,
			String suffixInputCorporeaDocs, String suffixInputCorporeaVocab, 
			String suffixInputCorporeaPhrases, String inputStopwordsFile) {
		ArrayList<Corpus> corpora = new ArrayList<Corpus>();
		File[] domainFiles = new File(inputCorporeaDirectory).listFiles();
		for (File domainFile : domainFiles) {
			if (domainFile.isDirectory()) {
				// Only consider folders.
				String domain = domainFile.getName();
				String issueFilePath = domainFile.getAbsolutePath() + File.separator
						+ domain + suffixInputCorporeaPhrases;
				
				Corpus corpus = new Corpus(domain);
				corpus.getCorpusFromFile(domain,issueFilePath,inputStopwordsFile);
				corpora.add(corpus);
			}
		}
		return corpora;
	}
	
	/**
	 * Run crf to generate candidate phrases and build dummy corpus from output.
	 */
	private static void getPhrases(String inputReviewDirectory, String inputCorporeaDirectory,
			String suffixInputCorporeaPhrases ) throws Exception {
		
	    File[] domainFiles = new File(inputReviewDirectory).listFiles();
	    for (int k = 0; k < domainFiles.length; k++){
	    	System.out.println(domainFiles[k]);
		    if (domainFiles[k].isDirectory()){ 
		    	String domain = domainFiles[k].getName();
		    	RunCrfPlus.crfTask(domain,
		    			inputCorporeaDirectory,suffixInputCorporeaPhrases,inputReviewDirectory);
		    	//BuildCorpusFromFeature formater = new BuildCorpusFromFeature(cmdOption);
				//formater.getDocsAndVocab(domain);
		    }
	    }
	}
	/**
	 * 
	 * For each model, generate the candidate phrases using crf, create its corpora and run the model.
	 */
	public static void run() {
		// Run CRF model and extract Phrases of each domain.
		try {
			getPhrases(cmdOption.inputReviewDirectory,
					cmdOption.inputCorporeaDirectory,
					cmdOption.suffixInputCorporeaPhrases);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Phrase detection of all domain done");
		
		// Read the corpus of each domain.
		ArrayList<Corpus> corpora = getCorpora(
				cmdOption.inputCorporeaDirectory,
				cmdOption.suffixInputCorporeaDocs,
				cmdOption.suffixInputCorporeaVocab,
				cmdOption.suffixInputCorporeaPhrases,
				cmdOption.inputStopwords);
		
		System.out.println("Getting Corpora of all domain done");
		
		long startTime = System.currentTimeMillis();

		String currentIterationRootDirectory = cmdOption.outputRootDirectory + File.separator;
		String currentIterationModelName = cmdOption.modelName;

		// Run the topic model.
		System.out.println("-----------------------------------");
		System.out.println("Running " + cmdOption.modelName +" on each domain.");
		System.out.println("-----------------------------------");
		
		runTopicModel(corpora, cmdOption.nTopics, currentIterationModelName,
					currentIterationRootDirectory);
		double timeLength = (System.currentTimeMillis() - startTime) / 1000.0;

		System.out.println("###################################");
		System.out.println("Learning Iteration  Ends! "
				+ timeLength + "seconds");
		System.out.println("###################################");
		System.out.println("");
	}

	/**
	 * LearningIteration : Run PSM on each domain.
	 */
	private static void runTopicModel(ArrayList<Corpus> corpora, int nTopics,
			String currentIterationModelName,
			String currentIterationRootDirectory) {
		
		for (Corpus corpus : corpora) {
			String currentIterationModelDirectory = currentIterationRootDirectory +
					currentIterationModelName
					+ File.separator
					+ corpus.domain
					+ File.separator;

			ModelParameters param = new ModelParameters(corpus, nTopics, cmdOption);

			param.modelName = currentIterationModelName;
			param.outputModelDirectory = currentIterationModelDirectory;

			System.out.println("\"" + param.domain + "\" <" + param.modelName
					+ "> Starts...");
			
			TopicModel model = TopicModel.selectModel(corpus, param);
			model.run();

			ModelPrinter modelPrinter = new ModelPrinter(model);
			modelPrinter.printModel(param.outputModelDirectory);
			
			System.out.println("\"" + param.domain + "\" <" + param.modelName
					+ "> Ends!");

		}
	}


	private static void showCommandLineHelp(CmdLineParser parser) {
		System.out.println("java [options ...] [arguments...]");
		parser.printUsage(System.out);
	}

	private static void showRunningTime(long time) {
		System.out.println("Elapsed time: "
				+ String.format("%.3f", (time) / 1000.0) + " seconds");
		System.out.println("Elapsed time: "
				+ String.format("%.3f", (time) / 1000.0 / 60.0) + " minutes");
		System.out.println("Elapsed time: "
				+ String.format("%.3f", (time) / 1000.0 / 3600.0) + " hours");
	}	
}