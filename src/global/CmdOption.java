package global;

import org.kohsuke.args4j.Option;

/**
 * Command line options.
 */

public class CmdOption {

	// ------------------------------------------------------------------------
	// Input and Output
	// ------------------------------------------------------------------------

	public String inputCorporeaDirectory = "./Data/Output/Corpus/domains/";
	
	@Option(name = "-i", usage = "Specify the input directory of each domain in feature format for each aspect ")
	public String inputReviewDirectory = "./Data/Input/domains/";
	
	@Option(name = "-o", usage = "Specify the output root directory of the program")
	public String outputRootDirectory = "./Data/Output/";
	
	@Option(name = "-sword", usage = "Specify the input directory of each domain "
			+ "where each domain contains documents and vocabulary")
	public String inputStopwords = "./Data/Input/stopwords.txt";

	@Option(name = "-sdocs", usage = "Specify the suffix of input docs file")
	public String suffixInputCorporeaDocs = ".docs";

	@Option(name = "-svocab", usage = "Specify the suffix of input vocab file")
	public String suffixInputCorporeaVocab = ".vocab";
	
	@Option(name = "-sphrase", usage = "Specify the suffix of input vocab file")
	public String suffixInputCorporeaPhrases = ".phrase";
	
	// ------------------------------------------------------------------------
	// General Settings for Topic Model
	// ------------------------------------------------------------------------

	@Option(name = "-ntopics", usage = "Specify the number of topics")
	public int nTopics = 10;

	@Option(name = "-burnin", usage = "Specify the number of iterations for burn-in period")
	public int nBurnin = 200;

	@Option(name = "-niters", usage = "Specify the number of Gibbs sampling iterations")
	public int nIterations = 2000;

	@Option(name = "-slag", usage = "Specify the length of interval to sample for "
			+ "calculating posterior distribution")
	public int sampleLag = 50; // -1 means only sample the last one.

	@Option(name = "-mname", usage = "Specify the name of the topic model")
	public String modelName = "PSM";

	/******************* Hyperparameters *********************/
	@Option(name = "-alpha", usage = "Specify the hyperparamter alpha")
	public double alpha = 5;

	@Option(name = "-beta", usage = "Specify the hyperparamter beta")
	public double beta = 0.1;

	@Option(name = "-rseed", usage = "Specify the seed for random number generator")
	public int randomSeed = 837191;

	/******************* Output *********************/
	@Option(name = "-twords", usage = "Specify the number of top words for each topic")
	public int twords = 50; // Print out top words ranked by probabilities per
							// each topic. -1: print out all words under topic.

	public CmdOption getSoftCopy() {
		CmdOption cmdOption2 = new CmdOption();
		cmdOption2.inputCorporeaDirectory = this.inputCorporeaDirectory;
		cmdOption2.inputReviewDirectory = this.inputReviewDirectory;
		cmdOption2.inputStopwords = this.inputStopwords;
		cmdOption2.suffixInputCorporeaPhrases = this.suffixInputCorporeaPhrases;
		cmdOption2.outputRootDirectory = this.outputRootDirectory;
		cmdOption2.suffixInputCorporeaDocs = this.suffixInputCorporeaDocs;
		cmdOption2.suffixInputCorporeaVocab = this.suffixInputCorporeaVocab;
		cmdOption2.nTopics = this.nTopics;
		cmdOption2.nBurnin = this.nBurnin;
		cmdOption2.nIterations = this.nIterations;
		cmdOption2.sampleLag = this.sampleLag;
		cmdOption2.modelName = this.modelName;
		cmdOption2.alpha = this.alpha;
		cmdOption2.beta = this.beta;
		cmdOption2.randomSeed = this.randomSeed;
		cmdOption2.twords = this.twords;
		return cmdOption2;
	}
}
