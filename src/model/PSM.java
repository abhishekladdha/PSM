package model;

import java.util.ArrayList;
import java.util.HashMap;

import utility.ArrayAllocationAndInitialization;
import utility.InverseTransformSampler;
import nlp.Corpus;
import gamma.GammaUtils;


public class PSM extends TopicModel {
	// The hyperparameter for the document-topic distribution.
	// alpha is in the variable param in TopicModel.
	private double tAlpha = 0;
	// The hyperparameter for the topic-word distribution.
	// beta is in the variable param in TopicModel.
	double vBeta = 0;
	
	private double[] theta = null; // Document-Aspect distribution, size D * A.
	private double[] thetaSum = null; // Cumulative document-Aspect
										// distribution, size
										// D * A.
	private double[][] phi = null; // Aspect-Vocab distribution, size A * VS.
	private double[][] phiSum = null; // Cumulative topic-word distribution,
										// size T * VS.
	private double[][] phiBack = null; // Aspect-Vocab background word distribution, size A * VS.
	private double[][] phiBackSum = null; // Cumulative topic-BackWords distribution,
										// size A * V.
	
	// ndt[d][t]: the counts of document d having topic t.
	private int[] ndt = null;
	// ndsum[d]: the counts of document d having any topic.
	private int ndSum = 0;
	//private int[] ndSum = null;
	// ntw[t][w]: the counts of word w appearing under topic t.
	double[][] ntw = null;
	// ntsum[t]: the counts of any word appearing under topic t.
	double[] ntSum = null;
	// ntw[t][w]: the counts of background word w appearing under topic t.
	double[][] nbw = null;
	// ntsum[t]: the counts of any background word appearing under topic t.
	double[] nbSum = null;
	
	public int numstats = 0;
	boolean isGpu = true;
	
	public PSM(Corpus corpus2, ModelParameters param2) {
		super(corpus2, param2);
		
		tAlpha = param.T * param.alpha;
		vBeta = param.V * param.beta;
		isGpu = param.isGpu;
		/**Allocate memory for temporary variables and initialize their values.*/
		allocateMemoryForTempVariables();
		// Initialize the first status of Markov chain randomly.
		initializeFirstMarkovChainRandomly();
	}
	/** Randomly initialize first markov chain*/
	private void initializeFirstMarkovChainRandomly() {
		z = new int[param.D];
		r = new boolean[param.D][];
		for (int d = 0; d < param.D; ++d) {
			int topic = (int) Math.floor(randomGenerator.nextDouble()* param.T);
			z[d] = topic ;
			updateCountAspect(d, topic, +1);
			
			int N = corpus.docs[d].length;
			r[d] = new boolean[N];
			for (int n = 0; n < N; ++n) {
				if(corpus.docs[d][n] >= 0){
					int word = corpus.docs[d][n];
					boolean swit = (Math.floor(randomGenerator.nextDouble()* 2) > 0 ) ? true : false;
					r[d][n] = swit;
					updateCountVocab(d,n,word,topic,+1);
				}
			}
		}	
	}
	
	private void updateCountAspect(int d, int topic, int flag) {
		ndt[topic] += flag;
	}
	
	/**
	 * Update vocab count of words for each topic. Also, include counts from GPU 
	 */
	private void updateCountVocab(int d, int n, int word, int topic,int flag){
		if(r[d][n] == true){
			ntw[topic][word] += flag;
			ntSum[topic] += flag;
		}
		else{
			nbw[topic][word] += flag;
			nbSum[topic] += flag;
		}
		// Add counts to other words in GPU
		if(isGpu) {
			String s = corpus.vocab.getWordstrByWordid(word);
			/** Phrase to words */
			if(s.split(" ").length > 1){
				String[] tokens = s.split(" ");
				for(int i =0; i < tokens.length; i++){
					double count = flag*0.01;
					if(r[d][n] == true){
						ntw[topic][corpus.vocab.getWordidByWordstr(tokens[i])] += count;
						ntSum[topic] += count;
					}
				}
			}
			else{
				/** Promote all phrases in the phrase set of aspect */
				if(corpus.aspectIssue.containsKey(word)){
					double prom = corpus.aspectProm[corpus.aspectIndex.get(corpus.vocab.getWordstrByWordid(word))];
					HashMap<Integer,Integer> issue_indices_1 = corpus.aspectIssue.get(word);
					ArrayList<Integer> issue_indices = new ArrayList<Integer>(issue_indices_1.keySet());
					for(int k = 0; k < issue_indices.size(); k++){
					    double count = flag*0.05*prom;
						if(r[d][n] == true){
							ntw[topic][issue_indices.get(k)] += count;
							ntSum[topic] += count;
						}
					}
				}
			}
		}
	}
	
	/**
	 * Run a certain number of Gibbs Sampling sweeps.
	 */
	
	private void runGibbsSampling() {
		int print_iteration = 100;
		for (int i = 0; i < param.nIterations; ++i) {
			if(i%print_iteration == 0){
				System.out.println("Running Iteration Number " + i);	
			}
			for (int d = 0; d < param.D; ++d) {
				sampleTopicAssignment(d);
			}
			for (int d = 0; d < param.D; ++d) {
				int N = corpus.docs[d].length;
				boolean issueStarted = false;
				int issue = 0;
				for (int n = 0; n < N; ++n) {
					if(corpus.docs[d][n] == -1)
						issueStarted = true;
					else if(issueStarted == false)
						sampleSwitchVariable(d,n,0,issue);
					else{
						sampleSwitchVariable(d,n,1,issue);
						issue++;
					}
				}
			}
			if (i >= param.nBurnin && param.sampleLag > 0
					&& i % param.sampleLag == 0) {
				updatePosteriorDistribution();
			}
		}
	}
	
	/**
	 * Sample a topic assigned to document d.
	 */
	
	private void sampleTopicAssignment(int d) {
		
		int old_topic = z[d];
		updateCountAspect(d, old_topic, -1);
		int N = corpus.docs[d].length;
		for(int i = 0; i < N; i++){
			if(corpus.docs[d][i] >= 0){
				updateCountVocab(d,i,corpus.docs[d][i],old_topic,-1);
			}
		}
		double[] p = new double[param.T];
		for (int t = 0; t < param.T; t++) {
			if(t == old_topic){
				p[t] = Math.exp(calculateTopicProbability(d,t));
			}
			else
				p[t] = Math.exp(calculateTopicProbability(d,t)); 
		}
		
		int new_topic = InverseTransformSampler.sample(p,
				randomGenerator.nextDouble());
		
		z[d] = new_topic;
		if(new_topic < 0){
			System.out.println();
		}
		updateCountAspect(d, new_topic, +1);
		
		for(int i = 0; i < N; i++){
			if(corpus.docs[d][i] >= 0)
				updateCountVocab(d,i,corpus.docs[d][i],new_topic,+1);
		}
	}
	
	/**
	 * Calculate conditioned probability of assiging doc d to topic t
	 */
	private double calculateTopicProbability(int d,int t){
		double p = Math.log((ndt[t] + param.alpha) / (ndSum + tAlpha));
		int N = corpus.docs[d].length;
		int nBack = 0;
		int nAspect = 0;
		for(int i = 0; i < N; i++){
			if(corpus.docs[d][i]>= 0){
				if(r[d][i] == true){
					p += Math.log((double)ntw[t][corpus.docs[d][i]]+ param.beta);
					nAspect++;
				}
				else{
					p += Math.log((double)nbw[t][corpus.docs[d][i]]+ param.beta);
					nBack++;
				}
			}
		}
		//System.out.println("document Finished");
		p += GammaUtils.lgamma((double) ntSum[t]+vBeta)
				- GammaUtils.lgamma((double) ntSum[t] +nAspect+ vBeta)
				+ GammaUtils.lgamma((double) nbSum[t]+ vBeta)
				- GammaUtils.lgamma((double) nbSum[t]+nBack+ vBeta);
		if(Double.isNaN(p) || Double.isInfinite(p) || p == 0.0){
			System.out.println("Number is not defined");
		}
		return p;
	}
	
	/**Switch variable probability for words and phrases */
	private void sampleSwitchVariable(int d,int n,int isIssue,int issueNo) {
		
		int topic = z[d];
		int word = corpus.docs[d][n];
		updateCountVocab(d, n, word,topic, -1);

		double[] p = new double[2];
		p[0] = (ntw[topic][word]+param.beta)/(ntSum[topic]+vBeta)
				*getCrfProbability(d,n,0,isIssue,issueNo);
		p[1] = (nbw[topic][word]+param.beta)/(nbSum[topic]+vBeta)
				*getCrfProbability(d,n,1,isIssue,issueNo);
		
		boolean new_switch = (InverseTransformSampler.sample(p,
				randomGenerator.nextDouble()) > 0) ? true:false;
		r[d][n] = new_switch;
		updateCountVocab(d, n, word,topic, +1);
	}
	
	/** Get CRF probability of word and phrase switchVar r = 0 for background and r=1 for aspect topic
	 * isIssue = 0 for word, isIssue =1 for phrase */
	private double getCrfProbability(int d,int n,int switchVar,int isIssue,int issueNo){
		double p = 0;
		if(isIssue == 0){
			ArrayList<String[][]> prob = corpus.wordCrfProb.get(d);
			String word = corpus.vocab.getWordstrByWordid(corpus.docs[d][n]);
			int index = -1;
			for(int i =0; i < prob.size(); i++){
				if(prob.get(i)[0][0].toLowerCase().equals(word)){
					index = i;
				}
			}
			if(index == -1){
				if(word.length() == 2 )
					index = 0;
				else
					System.out.println(word);
			}
			p = Double.parseDouble(prob.get(index)[0][switchVar+2]);
		}
		else{
			p = corpus.issueProb.get(d).get(issueNo)[switchVar];
		}
		return p;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		// 1. Run a certain number of Gibbs Sampling sweeps.
		runGibbsSampling();
		// 2. Compute the posterior distributions.
		computePosteriorDistribution();
	}

	@Override
	public double[][] getTopicWordDistribution() {
		// TODO Auto-generated method stub
		return phi;
	}
	/**
	 * After burn in phase, update the posterior distributions every sample lag.
	 */
	private void updatePosteriorDistribution() {
		for (int t = 0; t < param.T; ++t) {
			thetaSum[t] += (ndt[t] + param.alpha)/ (ndSum + tAlpha);
		}
		for (int t = 0; t < param.T; ++t) {
			for (int w = 0; w < param.V; ++w) {
				phiSum[t][w] += (ntw[t][w] + param.beta) / (ntSum[t] + vBeta);
				phiBackSum[t][w] += (nbw[t][w] + param.beta) / (nbSum[t] + vBeta);
			}
		}
		++numstats;
	}

	/**
	 * Compute the posterior distributions.
	 */
	private void computePosteriorDistribution() {
		computeDocumentTopicDistribution();
		computeTopicWordDistribution();
	}
	
	/**
	 * Document-topic distribution: theta[][].
	 */
	private void computeDocumentTopicDistribution() {
		if (param.sampleLag > 0) {
			for (int t = 0; t < param.T; ++t) {
				theta[t] = thetaSum[t] / numstats;
			}
		} else {
			for (int t = 0; t < param.T; ++t) {
				theta[t] = (ndt[t] + param.alpha)/ (ndSum + tAlpha);
			}
		}
	}
	/**
	 * Topic-word distribution: phi[][].
	 */
	private void computeTopicWordDistribution() {
		if (param.sampleLag > 0) {
			for (int t = 0; t < param.T; ++t) {
				for (int w = 0; w < param.V; ++w) {
					phi[t][w] = phiSum[t][w] / numstats;
					phiBack[t][w] = phiBackSum[t][w] / numstats;
				}
			}
		} 
		else {
			for (int t = 0; t < param.T; ++t) {
				for (int w = 0; w < param.V; ++w) {
					phi[t][w] = (ntw[t][w] + param.beta) / (ntSum[t] + vBeta);
					phiBack[t][w] = (nbw[t][w] + param.beta) / (nbSum[t] + vBeta);
				}
			}
		}
	}
	
	private void allocateMemoryForTempVariables() {
		// TODO Auto-generated method stub
		/******************* Posterior distributions *********************/
		theta = ArrayAllocationAndInitialization.allocateAndInitialize(theta, param.T);
		phi = ArrayAllocationAndInitialization.allocateAndInitialize(phi,
				param.T, param.V);
		phiBack = ArrayAllocationAndInitialization.allocateAndInitialize(phiBack,
				param.T, param.V);
		
		if (param.sampleLag > 0) {
			thetaSum = ArrayAllocationAndInitialization.allocateAndInitialize(thetaSum,
					param.T);
			phiSum = ArrayAllocationAndInitialization.allocateAndInitialize(
					phiSum, param.T, param.V);
			phiBackSum = ArrayAllocationAndInitialization.allocateAndInitialize(
					phiBackSum, param.T, param.V);
		}

		/******************* Temp variables while sampling *********************/
		ndt = ArrayAllocationAndInitialization.allocateAndInitialize(ndt, param.T);
		ndSum = param.D;
		
		ntw = ArrayAllocationAndInitialization.allocateAndInitialize(ntw,
				param.T, param.V);
		ntSum = ArrayAllocationAndInitialization.allocateAndInitialize(ntSum,
				param.T);
		nbw = ArrayAllocationAndInitialization.allocateAndInitialize(nbw,
				param.T, param.V);
		nbSum = ArrayAllocationAndInitialization.allocateAndInitialize(nbSum,
				param.T);
	}
}
