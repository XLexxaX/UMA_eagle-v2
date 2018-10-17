package org.aksw.limes.core.controller;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.aksw.limes.core.io.cache.Instance;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MemoryMapping;
import org.aksw.limes.core.ml.algorithm.Eagle;
import org.apache.log4j.Logger;

public class GoldStandardBatchReader {

	private HashMap<String, HashMap<String, Double>> gold = new HashMap<>();
	private HashMap<String, HashMap<String, Double>> train = new HashMap<>();
	public HashMap<String, Double> trainResult = new HashMap<>();
	public HashMap<String, Double> testResult = new HashMap<>();

	protected static Logger logger = Logger.getLogger(GoldStandardBatchReader.class);

	public GoldStandardBatchReader(String inputFile) {
		try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
			String line = "";
			HashMap<String, Double> tmp = null;
			while ((line = br.readLine()) != null) {
				String s = line.split(">")[0].split("<")[1];
				String t = line.split("<")[3].split(">")[0];
				tmp = gold.get(s);
				if (tmp == null)
					tmp = new HashMap<>();
				tmp.put(t, 1.0);
				gold.put(s, tmp);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info("#Positive samples in gold set: " + getSize(gold));

	}

	private int getSize(HashMap<String, HashMap<String, Double>> x) {
		int size = 0;
		Iterator<Entry<String, HashMap<String, Double>>> it = x.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, HashMap<String, Double>> pair = it.next();
			Iterator<Entry<String, Double>> it2 = pair.getValue().entrySet().iterator();
			while (it2.hasNext()) {
				it2.next();
				size++;
			}
		}
		return size;
	}

	public double isMatch(String s, String t) {

		/*
		 * try {
		 * Files.write(Paths.get("C:/Users/Alexander/Desktop/data_phones/results.txt"),
		 * (s+";"+t+"\n").getBytes(), StandardOpenOption.APPEND); } catch (IOException
		 * e) { e.printStackTrace(); //exception handling left as an exercise for the
		 * reader }
		 */

		HashMap<String, Double> tmp1 = train.get(s);
		if (tmp1 == null)
			tmp1 = new HashMap<String, Double>();

		if (gold.containsKey(s)) {
			if (gold.get(s).containsKey(t)) {
				tmp1.put(t, 1.0);
				train.put(s, tmp1);
				return 1.0;
			}
		} else if (gold.containsKey(t)) {
			if (gold.get(t).containsKey(s)) {
				tmp1.put(t, 1.0);
				train.put(s, tmp1);
				return 1.0;
			}
		}
		tmp1.put(t, -1.0);
		train.put(s, tmp1);
		return -1.0;

	}

	@SuppressWarnings("unlikely-arg-type")
	public void eval(AMapping result, int iteration, String mappingrule) {
		HashMap<String, Integer> trainResult = new HashMap<>();
		trainResult.put("tp", 0);
		trainResult.put("fp", 0);
		trainResult.put("fn", 0);
		HashMap<String, Integer> testResult = new HashMap<>();
		testResult.put("tp", 0);
		testResult.put("fp", 0);
		testResult.put("fn", 0);
		HashMap<String, HashMap<String, Double>> trainPrediction = new HashMap<>();
		HashMap<String, HashMap<String, Double>> trainGold = new HashMap<>();

		int numberOfPositives = 0;
		int numberOfNegatives = 0;

		Iterator<Entry<String, HashMap<String, Double>>> it = train.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, HashMap<String, Double>> pair = it.next();
			Iterator<Entry<String, Double>> it2 = pair.getValue().entrySet().iterator();
			while (it2.hasNext()) {
				Entry<String, Double> pair2 = it2.next();

				// by the way, maintain some information about the distribution of the training
				// set
				if (pair2.getValue() > 0.0) {
					numberOfPositives++;
				} else {
					numberOfNegatives++;
				}

				// The calculation of tp/fp/fn expects a data-structure, that only contains the
				// positive class, either for the prediction or gold-set. But the "train"
				// variable
				// contains positive as well as negative samples. So here we extract only the
				// positive samples
				// for the subsequent performance calculations.
				if (pair2.getValue() > 0.0) {
					if (!isContained(pair.getKey(), pair2.getKey(), trainGold)) {
						HashMap<String, Double> tmp = trainGold.get(pair.getKey());
						if (tmp == null)
							tmp = new HashMap<>();
						tmp.put(pair2.getKey(), 1.0);
						trainGold.put(pair.getKey(), tmp);
					}
				}

				// extract the predictions for the training-set
				if (isContained(pair.getKey(), pair2.getKey(), result.getMap())) {
					if (!isContained(pair.getKey(), pair2.getKey(), trainPrediction)) {
						HashMap<String, Double> tmp = trainPrediction.get(pair.getKey());
						if (tmp == null)
							tmp = new HashMap<>();
						tmp.put(pair2.getKey(), 1.0);
						trainPrediction.put(pair.getKey(), tmp);
					}
				}
			}
		}

		HashMap<String, Integer> tmp = null;
		tmp = calcEval(result.getMap(), gold);
		testResult.put("tp", tmp.get("+"));
		testResult.put("fp", tmp.get("-"));
		tmp = calcEval(gold, result.getMap());
		testResult.put("tp", tmp.get("+"));
		testResult.put("fn", tmp.get("-"));
		tmp = calcEval(trainPrediction, trainGold);
		trainResult.put("tp", tmp.get("+"));
		trainResult.put("fp", tmp.get("-"));
		tmp = calcEval(trainGold, trainPrediction);
		trainResult.put("tp", tmp.get("+"));
		trainResult.put("fn", tmp.get("-"));

		double trainP, trainR, trainF, testP, testR, testF;
		try {
			trainP = trainResult.get("tp") * 100.0 / (trainResult.get("tp") + trainResult.get("fp"));
			if (new Double(trainP).isNaN())
				throw new ArithmeticException();
		} catch (ArithmeticException e) {
			trainP = 0.0;
		}
		try {
			trainR = trainResult.get("tp") * 100.0 / (trainResult.get("tp") + trainResult.get("fn"));
			if (new Double(trainR).isNaN())
				throw new ArithmeticException();
		} catch (ArithmeticException e) {
			trainR = 0.0;
		}
		try {
			trainF = 2.0 * trainP * trainR / (trainP + trainR);
			if (new Double(trainF).isNaN())
				throw new ArithmeticException();
		} catch (ArithmeticException e) {
			trainF = 0.0;
		}
		try {
			testP = testResult.get("tp") * 100.0 / (testResult.get("tp") + testResult.get("fp"));
			if (new Double(testP).isNaN())
				throw new ArithmeticException();
		} catch (ArithmeticException e) {
			testP = 0.0;
		}
		try {
			testR = testResult.get("tp") * 100.0 / (testResult.get("tp") + testResult.get("fn"));
			if (new Double(testR).isNaN())
				throw new ArithmeticException();
		} catch (ArithmeticException e) {
			testR = 0.0;
		}
		try {
			testF = 2.0 * testP * testR / (testP + testR);
			if (new Double(testF).isNaN())
				throw new ArithmeticException();
		} catch (ArithmeticException e) {
			testF = 0.0;
		}

		String outString = iteration + ";" + trainP + ";" + trainR + ";" + trainF + ";" + testP + ";" + testR + ";"
				+ testF + ";" + mappingrule + ";" + numberOfPositives + ";" + numberOfNegatives + "\n";
		System.out.println(outString);

		this.trainResult.put("p", trainP);
		this.trainResult.put("r", trainR);
		this.trainResult.put("f", trainF);
		this.testResult.put("p", testP);
		this.testResult.put("r", testR);
		this.testResult.put("f", trainF);

		try {
			Files.write(Paths.get("C:/Users/Alexander/Desktop/data_phones/results.txt"), outString.getBytes(),
					StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
			// exception handling left as an exercise for the reader
		}

	}

	private HashMap<String, Integer> calcEval(HashMap<String, HashMap<String, Double>> a,
			HashMap<String, HashMap<String, Double>> b) {

		HashMap<String, Integer> result = new HashMap<>();
		result.put("+", 0);
		result.put("-", 0);

		Iterator<Entry<String, HashMap<String, Double>>> it = a.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, HashMap<String, Double>> pair = it.next();
			Iterator<Entry<String, Double>> it2 = pair.getValue().entrySet().iterator();
			while (it2.hasNext()) {
				Entry<String, Double> pair2 = it2.next();
				if (isContained(pair.getKey(), pair2.getKey(), b)) {
					result.put("+", result.get("+") + 1);
				} else {
					result.put("-", result.get("-") + 1);
				}
			}
		}
		return result;

	}

	public boolean isContained(String s, String t, HashMap<String, HashMap<String, Double>> map) {
		boolean found = false;
		if (map.containsKey(s)) {
	    	if (map.get(s).containsKey(t)) {
	    		found = true;
	    	}
	    } else if (map.containsKey(t)) {
	    	if (map.get(t).containsKey(s)) {
	    		found = true;
	    	}
	    }
		return found;
	}

	public HashMap<String, HashMap<String, Double>> getTrain() {
		// TODO Auto-generated method stub
		return train;
	}

}
