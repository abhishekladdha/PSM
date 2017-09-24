package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import nlp.Corpus;
import utility.ExceptionUtility;
import utility.ItemWithValue;

/**
 * The superclass of all topic models. It contains all the basic settings that
 * are used by all topic models. It also contains the basic methods that are
 * used by all topic models.
 * @author abhishek
 */

public abstract class TopicModel {
	
	public ModelParameters param = null;

	public Corpus corpus = null; // The corpus of a domain.
	
	// Topic assignments for each sentence in Gibbs sampler.
	// We put z in the superclass as every topic model is supposed to have it.
	protected int[] z = null;
	protected boolean r[][]= null;
	// Random number generator.
	protected Random randomGenerator = null;

	protected TopicModel(Corpus corpus2, ModelParameters param2) {
		corpus = corpus2;
		param = param2;
		randomGenerator = new Random(param.randomSeed);
	}

	// Run topic model.
	public abstract void run();

	// Get topic word distribution.
	public abstract double[][] getTopicWordDistribution();

	public static TopicModel selectModel(Corpus corpus2, ModelParameters param2) {
		String modelName = param2.modelName;
		
		if(modelName.equals("PSM")){
			return new PSM(corpus2, param2);
		}
		else {
			ExceptionUtility
					.throwAndCatchException("The model name is not recognizable!");
		}
		return null;
	}

	/**
	 * Return the list of top words and their original probabilities.
	 */
	
	public ArrayList<ArrayList<ItemWithValue>> getTopWordStrsWithProbabilitiesUnderTopics(
			int twords) {
		double[][] topicWordDist = getTopicWordDistribution();
		assert (topicWordDist != null && topicWordDist[0] != null) : "Topic word distribution is null!";

		ArrayList<ArrayList<ItemWithValue>> topWordStrsUnderTopics = new ArrayList<ArrayList<ItemWithValue>>();
		int T = topicWordDist.length;
		int V = topicWordDist[0].length;
		// If twords is negative, then get all words.
		if (twords > V || twords < 0) {
			twords = V;
		}

		for (int t = 0; t < T; t++) {
			ArrayList<ItemWithValue> wordsProbsList = new ArrayList<ItemWithValue>();
			for (int w = 0; w < V; w++) {
				ItemWithValue wwp = new ItemWithValue(w, topicWordDist[t][w]);
				wordsProbsList.add(wwp);
			}
			Collections.sort(wordsProbsList);

			ArrayList<ItemWithValue> topwordsProbsList = new ArrayList<ItemWithValue>();
			int k = 0;
			for (int i = 0; i < twords; i++) {
				int wordid = (Integer) wordsProbsList.get(k).getIterm();
				k++;
				String wordstr = corpus.vocab.getWordstrByWordid(wordid);
				//topWords.add(wordstr);
				boolean prevPresent = false;
				int insert_index = -1;
				for(int j = i-1; j >= 0; j--){
					String prev_word = (String) topwordsProbsList.get(j).getIterm();
					if(prev_word.split(" ").length >= wordstr.split(" ").length){
						if(prev_word.contains(wordstr)){
							prevPresent = true;
							break;
						}
					}
					else{
						if(wordstr.contains(prev_word)){
							topwordsProbsList.remove(j);
							insert_index = j;
							i--;
						}
					}
				}
				if(prevPresent == true){
					i--;	
				}
				else{
					double prob = wordsProbsList.get(i).getValue();
					ItemWithValue iwp = new ItemWithValue(wordstr, prob);
					if(insert_index >= 0)
						topwordsProbsList.add(insert_index,iwp);
					else
						topwordsProbsList.add(iwp);
				}
			}
			topWordStrsUnderTopics.add(topwordsProbsList);
		}
		return topWordStrsUnderTopics;
	}

}
