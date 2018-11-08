package org.aksw.limes.core.ml.algorithm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import org.aksw.limes.core.controller.GoldStandardBatchReader;
import org.aksw.limes.core.evaluation.qualititativeMeasures.FMeasure;
import org.aksw.limes.core.evaluation.qualititativeMeasures.IQualitativeMeasure;
import org.aksw.limes.core.evaluation.qualititativeMeasures.PseudoFMeasure;
import org.aksw.limes.core.exceptions.NotYetImplementedException;
import org.aksw.limes.core.exceptions.UnsupportedMLImplementationException;
import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.cache.HybridCache;
import org.aksw.limes.core.io.cache.Instance;
import org.aksw.limes.core.io.cache.MemoryCache;
import org.aksw.limes.core.io.config.Configuration;
import org.aksw.limes.core.io.ls.LinkSpecification;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.aksw.limes.core.ml.algorithm.eagle.core.ALDecider;
import org.aksw.limes.core.ml.algorithm.eagle.core.ExpressionFitnessFunction;
import org.aksw.limes.core.ml.algorithm.eagle.core.ExpressionProblem;
import org.aksw.limes.core.ml.algorithm.eagle.core.IGPFitnessFunction;
import org.aksw.limes.core.ml.algorithm.eagle.core.LinkSpecGeneticLearnerConfig;
import org.aksw.limes.core.ml.algorithm.eagle.core.PseudoFMeasureFitnessFunction;
import org.aksw.limes.core.ml.algorithm.eagle.util.PropertyMapping;
import org.aksw.limes.core.ml.algorithm.eagle.util.TerminationCriteria;
import org.apache.log4j.Logger;
import org.jgap.InvalidConfigurationException;
import org.jgap.gp.GPProblem;
import org.jgap.gp.IGPProgram;
import org.jgap.gp.impl.GPGenotype;
import org.jgap.gp.impl.GPPopulation;
import org.jgap.gp.impl.GPProgram;
import org.jgap.gp.impl.ProgramChromosome;

/**
 * @author Tommaso Soru (tsoru@informatik.uni-leipzig.de)
 * @author Klaus Lyko (lyko@informatik.uni-leipzig.de)
 *
 */
public class Eagle extends ACoreMLAlgorithm {

	// ======================= COMMON VARIABLES ======================
	private IGPProgram allBest = null;
	private IGPFitnessFunction fitness;

	public IGPFitnessFunction getFitness() {
		return fitness;
	}

	private GPGenotype gp;

	// ================ SUPERVISED-LEARNING VARIABLES ================
	private int turn = 0;
	private List<IGPProgram> bestSolutions = new LinkedList<IGPProgram>();
	private ALDecider alDecider = new ALDecider();

	// =============== UNSUPERVISED-LEARNING VARIABLES ===============
	private List<LinkSpecification> specifications;

	// ======================= PARAMETER NAMES =======================

	protected static final String ALGORITHM_NAME = "Eagle";

	private final boolean logging = false;
	private ACache tsC;
	private ACache ttC;

	public static final String ELITISM = "elitism";
	public static final String GENERATIONS = "generations";
	public static final String PRESERVE_FITTEST = "preserve_fittest";
	public static final String MAX_DURATION = "max_duration";
	public static final String INQUIRY_SIZE = "inquiry_size";
	public static final String MAX_ITERATIONS = "max_iterations";
	public static final String MAX_QUALITY = "max_quality";
	public static final String TERMINATION_CRITERIA = "termination_criteria";
	public static final String TERMINATION_CRITERIA_VALUE = "termination_criteria_value";
	public static final String BETA = "beta";
	public static final String POPULATION = "population";
	public static final String MUTATION_RATE = "mutation_rate";
	public static final String REPRODUCTION_RATE = "reproduction_rate";
	public static final String CROSSOVER_RATE = "crossover_rate";
	public static final String PSEUDO_FMEASURE = "pseudo_fmeasure";
	public static final String SIMPLE_FITNESS = "simple_fitness";

	public static final String MEASURE = "measure";
	public static final String PROPERTY_MAPPING = "property_mapping";
	private static final String ORIGINAL = "original";

	// ========================================================================

	protected static Logger logger = Logger.getLogger(Eagle.class);

	/**
	 * Eagle constructor.
	 */
	protected Eagle() {
		super();
		setDefaultParameters();
	}

	@Override
	protected String getName() {
		return ALGORITHM_NAME;
	}

	@Override
	protected void init(List<LearningParameter> lp, ACache source, ACache target) {
		super.init(lp, source, target);
		this.turn = 0;
		this.bestSolutions = new LinkedList<IGPProgram>();
	}

    protected MLResults learnOriginal(AMapping trainingData) {
		
		try {
			setUp(trainingData);
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			return null;
		}
    	
    	turn++;
        fitness.addToReference(extractPositiveMatches(trainingData));
        fitness.fillCachesIncrementally(trainingData);

        Integer nGen = (Integer) Integer.parseInt(getParameter(GENERATIONS).toString());
        
        for (int gen = 1; gen <= nGen; gen++) {
            gp.evolve();
            bestSolutions.add(determineFittestOriginal(gp, gen));
        }

        MLResults result = createSupervisedResult();
        return result;
        
    }
    
    private IGPProgram determineFittestOriginal(GPGenotype gp, int gen) {

        GPPopulation pop = gp.getGPPopulation();
        pop.sortByFitness();

        IGPProgram bests[] = {gp.getFittestProgramComputed(), pop.determineFittestProgram(),
                // gp.getAllTimeBest(),
                pop.getGPProgram(0),};
        IGPProgram bestHere = null;
        double fittest = Double.MAX_VALUE;

        for (IGPProgram p : bests) {
            if (p != null) {
                double fitM = fitness.calculateRawFitness(p);
                if (fitM < fittest) {
                    fittest = fitM;
                    bestHere = p;
                }
            }
        }
        /* consider population if necessary */
        if (bestHere == null) {
            logger.debug("Determining best program failed, consider the whole population");
            for (IGPProgram p : pop.getGPPrograms()) {
                if (p != null) {
                    double fitM = fitness.calculateRawFitness(p);
                    if (fitM < fittest) {
                        fittest = fitM;
                        bestHere = p;
                    }
                }
            }
        }
        
        
        // remember the best
        if ((Boolean) Boolean.parseBoolean(getParameter(PRESERVE_FITTEST).toString())) {
            if (allBest == null || fitness.calculateRawFitness(allBest) > fittest) {
                allBest = bestHere;
                logger.info("Generation " + gen + " new fittest (" + fittest + ") individual: " + getLinkSpecification(bestHere));
            }
        }

        return bestHere;
    }
	
	@Override
	protected MLResults learn(AMapping trainingData, GoldStandardBatchReader gsbr) {
		
		if (logging) {
			for (int j = 0; j < gp.getGPPopulation().getPopSize(); j++) {
				try {
					LinkSpecification ls = getLinkSpecification(gp.getGPPopulation().getGPProgram(j));
					Files.write(Paths.get("C:/Users/Alexander/Desktop/data_phones/results.txt"),
							(ls.getFullExpression().toString() + "-" + ls.getThreshold() + "-" + ls.getQuality()
									+ "\n").getBytes(),
							StandardOpenOption.APPEND);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				Files.write(Paths.get("C:/Users/Alexander/Desktop/data_phones/results.txt"),
						("\n1111111111111\n".toString() + "\n").getBytes(), StandardOpenOption.APPEND);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if ((Boolean) (Boolean.parseBoolean(getParameter(ORIGINAL).toString())))
			return this.learnOriginal(trainingData);

		MLResults result = null;
		int elitism = (Integer) Integer.parseInt(getParameter(ELITISM).toString());
		if (elitism > 0) {

			

			if (fitness.getClass().getName()
					.equals("org.aksw.limes.core.ml.algorithm.eagle.core.ExpressionFitnessFunction") && (Boolean) Boolean.parseBoolean(getParameter(SIMPLE_FITNESS).toString())) {
				ExpressionFitnessFunction eff = ((ExpressionFitnessFunction) fitness);
				eff.setUseFullCaches(false);
				eff.trimKnowledgeBases(trainingData);
				tsC = eff.getTrimmedSourceCache();
				ttC = eff.getTrimmedTargetCache();
			}

			PreparedGeneration prepgen = preserve_fittest(gsbr);

			if (logging) {
				for (int j = 0; j < gp.getGPPopulation().getPopSize(); j++) {
					try {
						LinkSpecification ls = getLinkSpecification(gp.getGPPopulation().getGPProgram(j));
						Files.write(Paths.get("C:/Users/Alexander/Desktop/data_phones/results.txt"),
								(ls.getFullExpression().toString() + "-" + ls.getThreshold() + "-" + ls.getQuality()
										+ "\n").getBytes(),
								StandardOpenOption.APPEND);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				try {
					Files.write(Paths.get("C:/Users/Alexander/Desktop/data_phones/results.txt"),
							("\n222222222222222\n".toString() + "\n").getBytes(), StandardOpenOption.APPEND);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			try {
				setUp(trainingData);
			} catch (InvalidConfigurationException e) {
				e.printStackTrace();
				logger.error(e.getMessage());
				return null;
			}

			turn++;
			fitness.addToReference(extractPositiveMatches(trainingData));
			fitness.fillCachesIncrementally(trainingData);

			Integer nGen = (Integer) Integer.parseInt(getParameter(GENERATIONS).toString());

			for (int gen = 1; gen <= nGen; gen++) {
				gp.evolve();

			}

			for (int i = 0; i < gp.getGPPopulation().getPopSize(); i++) {
				if (i < elitism) {
					gp.getGPPopulation().setGPProgram(i,
							(GPProgram) prepgen.getPreserved_generation().getGPProgram(i).clone());
					// gp.getGPPopulation().setGPProgram(i,
					// gp.getGPPopulation().getGPProgram(i));//prepgen.getChanged_generation().getGPPopulation().getGPProgram(i-elitism));
				} // else {
					// }

			}
			//gp.getGPPopulation().sortByFitness();
			quicksort(gp.getGPPopulation(), gsbr);

			GPProgram fittestnow = (GPProgram) gp.getGPPopulation().getGPProgram(0);// (GPProgram)
																					// prepgen.getPreserved_generation().getGPProgram(0);

			try {
				if ((Boolean) Boolean.parseBoolean(getParameter(SIMPLE_FITNESS).toString())) {
					if (tsC != null && ttC != null)
						Files.write(Paths.get("C:/Users/Alexander/Desktop/data_phones/results.txt"), ("##########:"
						+ gsbr.evalF(fitness.getMapping(tsC, ttC, getLinkSpecification(fittestnow))) + "\n").getBytes(),
						StandardOpenOption.APPEND);// fitness.calculateRawMeasure(fittestnow)
				} else {
					Files.write(Paths.get("C:/Users/Alexander/Desktop/data_phones/results.txt"), ("##########:"
					+ fitness.calculateRawMeasure(fittestnow) + "\n").getBytes(),
					StandardOpenOption.APPEND);// fitness.calculateRawMeasure(fittestnow)
				}                                                                                                                                                                                                                                                                                                                                 
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			allBest = fittestnow;
			bestSolutions.add(fittestnow);

			result = createSupervisedTrainResult(tsC, ttC);

		} else {

			try {
				setUp(trainingData);
			} catch (InvalidConfigurationException e) {
				e.printStackTrace();
				logger.error(e.getMessage());
				return null;
			}

			turn++;
			fitness.addToReference(extractPositiveMatches(trainingData));
			fitness.fillCachesIncrementally(trainingData);

			Integer nGen = (Integer) Integer.parseInt(getParameter(GENERATIONS).toString());

			for (int gen = 1; gen <= nGen; gen++) {
				gp.evolve();
				bestSolutions.add(determineFittest(gp, gen, null));
			}

			result = createSupervisedResult();
		}

		// Log the rules of the current population for test/debug-purposes.
		if (logging) {
			for (int j = 0; j < gp.getGPPopulation().getPopSize(); j++) {
				try {
					LinkSpecification ls = getLinkSpecification(gp.getGPPopulation().getGPProgram(j));
					Files.write(Paths.get("C:/Users/Alexander/Desktop/data_phones/results.txt"),
							(ls.getFullExpression().toString() + "-" + ls.getThreshold() + "-" + ls.getQuality() + "\n")
									.getBytes(),
							StandardOpenOption.APPEND);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				Files.write(Paths.get("C:/Users/Alexander/Desktop/data_phones/results.txt"),
						("\n------------\n".toString() + "\n").getBytes(), StandardOpenOption.APPEND);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return result;

	}

	// Todo: Convert to Quicksort
	public GPPopulation bubblesort(GPPopulation pop, GoldStandardBatchReader gsbr) {
		double lastFitness = 0.0;

		for (int i = 0; i < pop.getPopSize(); i++) {
			for (int j = 1; j < pop.getPopSize(); j++) {
				if (pop.getGPProgram(j) != null && pop.getGPProgram(j - 1) != null) {
					double fitM = gsbr.evalF(fitness.getMapping(tsC, ttC, getLinkSpecification(pop.getGPProgram(j))));// fitness.calculateRawMeasure(pop.getGPProgram(j));
					if (fitM < lastFitness) {
						GPProgram ref = (GPProgram) pop.getGPProgram(j).clone();
						pop.setGPProgram(j, pop.getGPProgram(j - 1));
						pop.setGPProgram(j - 1, ref);
					}
					lastFitness = fitM;
				}
			}
		}
		return pop;
	}
	// Experimental

	public GPPopulation quicksort(GPPopulation pop, GoldStandardBatchReader gsbr) {
		if (fitness.getClass().getName() == "ExpressionFitnessFunction")
			((ExpressionFitnessFunction) fitness).setUseFullCaches(false);
		ArrayList<Integer> indices = new ArrayList<>();
		for (int i = 0; i < pop.getPopSize(); i++) {
			indices.add(i);
		}
		indices = sort(pop, indices, gsbr);
		ArrayList<GPProgram> tmp = new ArrayList<>();
		for (int i = 0; i < pop.getPopSize(); i++) {
			tmp.add((GPProgram) pop.getGPProgram(i).clone());
		}
		for (int i = 0; i < indices.size(); i++) {
			pop.setGPProgram(i, tmp.get(indices.get(i)));
		}
		return pop;
	}

	public ArrayList<Integer> sort(GPPopulation pop, ArrayList<Integer> indices, GoldStandardBatchReader gsbr) {

		if (indices.size() < 2) {
			return indices;
		}

		int randomInt = (int) (Math.random() * ((indices.size())));
		double reffitM = 0;
		if ((Boolean) Boolean.parseBoolean(getParameter(SIMPLE_FITNESS).toString())) {
			if (tsC == null || ttC == null) {
				reffitM = fitness.calculateRawMeasure(pop.getGPProgram(indices.get(randomInt)));
			} else {
				reffitM = gsbr.evalF(
						fitness.getMapping(tsC, ttC, getLinkSpecification(pop.getGPProgram(indices.get(randomInt)))));
			}
		} else {
			reffitM = fitness.calculateRawMeasure(pop.getGPProgram(indices.get(randomInt)));
		}
		ArrayList<Integer> left = new ArrayList<>();
		ArrayList<Integer> right = new ArrayList<>();
		ArrayList<Integer> sortedIndices = new ArrayList<>();

		for (int i = 0; i < indices.size() && i < pop.getPopSize(); i++) {
			if (i != randomInt) {
				double currentfitM = 0;
				if ((Boolean) Boolean.parseBoolean(getParameter(SIMPLE_FITNESS).toString())) {
					if (tsC == null || ttC == null) {
						currentfitM = fitness.calculateRawMeasure(pop.getGPProgram(indices.get(i)));
					} else {
						currentfitM = gsbr.evalF(
								fitness.getMapping(tsC, ttC, getLinkSpecification(pop.getGPProgram(indices.get(i)))));
					}
				} else {
					currentfitM = fitness.calculateRawMeasure(pop.getGPProgram(indices.get(i)));
				}
				if (currentfitM > reffitM) {
					left.add(indices.get(i));
				} else {
					right.add(indices.get(i));
				}
			}
		}
		left = sort(pop, left, gsbr);
		right = sort(pop, right, gsbr);
		sortedIndices.addAll(left);
		sortedIndices.add(indices.get(randomInt));
		sortedIndices.addAll(right);

		return sortedIndices;

	}

	protected class PreparedGeneration {
		public GPPopulation getChanged_generation() {
			return changed_generation;
		}

		public void setChanged_generation(GPPopulation changed_generation) {
			this.changed_generation = changed_generation;
		}

		public GPPopulation getPreserved_generation() {
			return preserved_generation;
		}

		public void setPreserved_generation(GPPopulation preserved_generation) {
			this.preserved_generation = preserved_generation;
		}

		protected GPPopulation changed_generation;
		protected GPPopulation preserved_generation;

		protected PreparedGeneration(GPPopulation changed_generation, GPPopulation preserved_generation) {
			this.changed_generation = changed_generation;
			this.preserved_generation = preserved_generation;
		}
	}

	public PreparedGeneration preserve_fittest(GoldStandardBatchReader gsbr) {

		int elitism = (Integer) Integer.parseInt(getParameter(ELITISM).toString());

		if (elitism > 0) {
			gp.calcFitness();

			GPPopulation pop = gp.getGPPopulation();
			// pop.sortByFitness();

			pop = quicksort(pop, gsbr);// bubblesort(pop);

			GPPopulation fittest = null;
			GPPopulation remaining = null;
			try {
				fittest = new GPPopulation(gp.getGPConfiguration(), elitism);
				remaining = new GPPopulation(gp.getGPConfiguration(), gp.getGPPopulation().getPopSize() - elitism);
			} catch (InvalidConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			int ctr = 0;
			while (ctr < elitism && ctr < pop.getPopSize()) {
				fittest.setGPProgram(ctr, (GPProgram) pop.getGPProgram(ctr).clone());
				ctr++;
			}
			while (ctr < pop.getPopSize()) {
				remaining.setGPProgram(ctr - elitism, (GPProgram) pop.getGPProgram(ctr).clone());
				ctr++;
			}

			@SuppressWarnings("unused")
			PropertyMapping pm = (PropertyMapping) getParameter(PROPERTY_MAPPING);
			LinkSpecGeneticLearnerConfig jgapConfig;
			try {
				/*
				 * GPGenotype tmpgp = setUp(remaining.getPopSize()); for (int i = 0; i <
				 * (remaining.getPopSize()); i++) { tmpgp.getGPPopulation().setGPProgram(i,
				 * (GPProgram) remaining.getGPProgram(i).clone()); }
				 */
				return new PreparedGeneration(null, fittest);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	private GPGenotype setUp(int popsize) throws InvalidConfigurationException {
		PropertyMapping pm = (PropertyMapping) getParameter(PROPERTY_MAPPING);
		if (!pm.wasSet()) {
			pm.setDefault(configuration.getSourceInfo(), configuration.getTargetInfo());
		}
		LinkSpecGeneticLearnerConfig jgapConfig2 = new LinkSpecGeneticLearnerConfig(configuration.getSourceInfo(),
				configuration.getTargetInfo(), pm);

		jgapConfig2.sC = sourceCache;
		jgapConfig2.tC = targetCache;

		jgapConfig2.setPopulationSize(popsize);
		jgapConfig2.setCrossoverProb((Float) Float.parseFloat(getParameter(CROSSOVER_RATE).toString()));
		jgapConfig2.setMutationProb((Float) Float.parseFloat(getParameter(MUTATION_RATE).toString()));
		jgapConfig2
				.setPreservFittestIndividual((Boolean) Boolean.parseBoolean(getParameter(PRESERVE_FITTEST).toString()));
		jgapConfig2.setReproductionProb((Float) Float.parseFloat(getParameter(REPRODUCTION_RATE).toString()));
		jgapConfig2.setPropertyMapping(pm);

		PseudoFMeasure pfm = (PseudoFMeasure) getParameter(PSEUDO_FMEASURE);
		// fitness = PseudoFMeasureFitnessFunction.getInstance(jgapConfig2, pfm,
		// sourceCache, targetCache);
		org.jgap.Configuration.reset();
		// jgapConfig2.setFitnessFunction(fitness);

		GPProblem gpP;

		gpP = new ExpressionProblem(jgapConfig2);
		return gpP.create();
	}

	@Override
	protected MLResults learn(PseudoFMeasure pfm) {

		learningParameters.add(new LearningParameter(PSEUDO_FMEASURE, pfm, PseudoFMeasure.class, Double.NaN, Double.NaN,
				Double.NaN, PSEUDO_FMEASURE));

		try {
			setUp(null);
		} catch (InvalidConfigurationException e) {
			logger.error(e.getMessage());
			return null;
		}

		Integer nGen = (Integer) Integer.parseInt(getParameter(GENERATIONS).toString());

		specifications = new LinkedList<LinkSpecification>();
		logger.info("Start learning");
		for (int gen = 1; gen <= nGen; gen++) {
			gp.evolve();
			IGPProgram currentBest = determineFittestUnsup(gp, gen);
			LinkSpecification currentBestMetric = getLinkSpecification(currentBest);
			// TODO: save the best LS of each generation
			specifications.add(currentBestMetric);
		}

		// Log the rules of the current population for test/debug-purposes.
		if (logging) {
			for (int j = 0; j < gp.getGPPopulation().getPopSize(); j++) {
				try {
					LinkSpecification ls = getLinkSpecification(gp.getGPPopulation().getGPProgram(j));
					Files.write(Paths.get("C:/Users/Alexander/Desktop/data_phones/results.txt"),
							(ls.getFullExpression().toString() + "-" + ls.getQuality() + "\n").getBytes(),
							StandardOpenOption.APPEND);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				Files.write(Paths.get("C:/Users/Alexander/Desktop/data_phones/results.txt"),
						("\n------------\n".toString() + "\n").getBytes(), StandardOpenOption.APPEND);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		allBest = determineFittestUnsup(gp, nGen);
		return createUnsupervisedResult();

	}

	@Override
	protected AMapping predict(ACache source, ACache target, MLResults mlModel) {
		// if (allBest != null) {
		return fitness.getMapping(source, target, mlModel.getLinkSpecification());
		// } else {
		// logger.error("No link specification calculated so far.");
		// assert (allBest != null);
		// }
		// return MappingFactory.createDefaultMapping();
	}

	@Override
	protected boolean supports(MLImplementationType mlType) {
		return mlType == MLImplementationType.SUPERVISED_BATCH || mlType == MLImplementationType.UNSUPERVISED
				|| mlType == MLImplementationType.SUPERVISED_ACTIVE;
	}

	// *************** active learning implementation
	// *****************************************
	@Override
	protected AMapping getNextExamples(int size, GoldStandardBatchReader gsbr)
			throws UnsupportedMLImplementationException {
		// throw new UnsupportedMLImplementationException(this.getName());
		return calculateOracleQuestions(size, gsbr);

	}

	@Override
	protected MLResults activeLearn(AMapping oracleMapping, GoldStandardBatchReader gsbr)
			throws UnsupportedMLImplementationException {
		logger.info("EAGLE active learning started with " + oracleMapping.size() + " examples");
		return learn(oracleMapping, gsbr);
	}

	@Override
	protected MLResults activeLearn() throws UnsupportedMLImplementationException {
		logger.info(
				"Supposed to run an active EAGLE, but provided no oracle data. Running default unsupervised approach instead.");
		return learn(new PseudoFMeasure());
	}

	@Override
	public void setDefaultParameters() {
		learningParameters = new ArrayList<>();
		learningParameters
				.add(new LearningParameter(GENERATIONS, 20, Integer.class, 1, Integer.MAX_VALUE, 1, GENERATIONS));
		learningParameters.add(new LearningParameter(PRESERVE_FITTEST, true, Boolean.class, Double.NaN, Double.NaN,
				Double.NaN, PRESERVE_FITTEST));
		learningParameters.add(new LearningParameter(MAX_DURATION, 60, Long.class, 0, Long.MAX_VALUE, 1, MAX_DURATION));
		learningParameters
				.add(new LearningParameter(INQUIRY_SIZE, 10, Integer.class, 1, Integer.MAX_VALUE, 1, INQUIRY_SIZE));
		learningParameters.add(
				new LearningParameter(MAX_ITERATIONS, 500, Integer.class, 1, Integer.MAX_VALUE, 1, MAX_ITERATIONS));
		learningParameters.add(new LearningParameter(MAX_QUALITY, 0.5, Double.class, 0d, 1d, Double.NaN, MAX_QUALITY));
		learningParameters.add(new LearningParameter(TERMINATION_CRITERIA, TerminationCriteria.iteration,
				TerminationCriteria.class, Double.NaN, Double.NaN, Double.NaN, TERMINATION_CRITERIA));
		learningParameters.add(new LearningParameter(TERMINATION_CRITERIA_VALUE, 0.0, Double.class, 0d,
				Double.MAX_VALUE, Double.NaN, TERMINATION_CRITERIA_VALUE));
		learningParameters.add(new LearningParameter(BETA, 1.0, Double.class, 0d, 1d, Double.NaN, BETA));
		learningParameters
				.add(new LearningParameter(POPULATION, 20, Integer.class, 1, Integer.MAX_VALUE, 1, POPULATION));
		learningParameters
				.add(new LearningParameter(MUTATION_RATE, 0.4f, Float.class, 0f, 1f, Double.NaN, MUTATION_RATE));
		learningParameters.add(
				new LearningParameter(REPRODUCTION_RATE, 0.4f, Float.class, 0f, 1f, Double.NaN, REPRODUCTION_RATE));
		learningParameters
				.add(new LearningParameter(CROSSOVER_RATE, 0.3f, Float.class, 0f, 1f, Double.NaN, CROSSOVER_RATE));
		learningParameters.add(new LearningParameter(MEASURE, new FMeasure(), IQualitativeMeasure.class, Double.NaN,
				Double.NaN, Double.NaN, MEASURE));
		learningParameters.add(new LearningParameter(PSEUDO_FMEASURE, new PseudoFMeasure(), IQualitativeMeasure.class,
				Double.NaN, Double.NaN, Double.NaN, MEASURE));
		learningParameters.add(new LearningParameter(PROPERTY_MAPPING, new PropertyMapping(), PropertyMapping.class,
				Double.NaN, Double.NaN, Double.NaN, PROPERTY_MAPPING));
		learningParameters.add(new LearningParameter(ELITISM, 0, Integer.class, 1, Integer.MAX_VALUE, 1, ELITISM));
		learningParameters.add(new LearningParameter(SIMPLE_FITNESS, true, Boolean.class, Double.NaN, Double.NaN,
				Double.NaN, SIMPLE_FITNESS));
		learningParameters.add(new LearningParameter(ORIGINAL, false, Boolean.class, Double.NaN, Double.NaN,
				Double.NaN, ORIGINAL));
	}

	// ====================== SPECIFIC METHODS =======================

	/**
	 * Configures EAGLE.
	 *
	 * @param trainingData
	 *            training data
	 * @throws InvalidConfigurationException
	 */
	private void setUp(AMapping trainingData) throws InvalidConfigurationException {
		PropertyMapping pm = (PropertyMapping) getParameter(PROPERTY_MAPPING);
		if (!pm.wasSet()) {
			pm.setDefault(configuration.getSourceInfo(), configuration.getTargetInfo());
		}
		LinkSpecGeneticLearnerConfig jgapConfig = new LinkSpecGeneticLearnerConfig(configuration.getSourceInfo(),
				configuration.getTargetInfo(), pm);

		jgapConfig.sC = sourceCache;
		jgapConfig.tC = targetCache;

		jgapConfig.setPopulationSize((Integer) Integer.parseInt(getParameter(POPULATION).toString()));
		jgapConfig.setCrossoverProb((Float) Float.parseFloat(getParameter(CROSSOVER_RATE).toString()));
		jgapConfig.setMutationProb((Float) Float.parseFloat(getParameter(MUTATION_RATE).toString()));
		jgapConfig
				.setPreservFittestIndividual((Boolean) Boolean.parseBoolean(getParameter(PRESERVE_FITTEST).toString()));
		jgapConfig.setReproductionProb((Float) Float.parseFloat(getParameter(REPRODUCTION_RATE).toString()));
		jgapConfig.setPropertyMapping(pm);

		if (trainingData != null) { // supervised

			FMeasure fm = (FMeasure) getParameter(MEASURE);
			fitness = ExpressionFitnessFunction.getInstance(jgapConfig, fm, trainingData);
			org.jgap.Configuration.reset();
			jgapConfig.setFitnessFunction(fitness);

		} else { // unsupervised

			PseudoFMeasure pfm = (PseudoFMeasure) getParameter(PSEUDO_FMEASURE);
			fitness = PseudoFMeasureFitnessFunction.getInstance(jgapConfig, pfm, sourceCache, targetCache);
			org.jgap.Configuration.reset();
			jgapConfig.setFitnessFunction(fitness);

		}

		GPProblem gpP;

		gpP = new ExpressionProblem(jgapConfig);
		if (gp == null)
			gp = gpP.create();
	}

	/**
	 * Returns only positive matches, that are those with a confidence higher then
	 * 0.
	 *
	 * @param trainingData
	 *            training data
	 * @return
	 */
	private AMapping extractPositiveMatches(AMapping trainingData) {
		AMapping positives = MappingFactory.createDefaultMapping();
		for (String sUri : trainingData.getMap().keySet())
			for (String tUri : trainingData.getMap().get(sUri).keySet()) {
				double confidence = trainingData.getConfidence(sUri, tUri);
				if (confidence > 0)
					positives.add(sUri, tUri, confidence);
			}
		return positives;
	}

	/**
	 * Method to compute best individuals by hand.
	 *
	 * @param gp
	 *            GP genotype
	 * @param gen
	 *            number of generations
	 * @return
	 */
	private IGPProgram determineFittest(GPGenotype gp, int gen, GPProgram fittestnow) {

		GPPopulation pop = gp.getGPPopulation();
		pop.sortByFitness();

		IGPProgram bests[] = { gp.getFittestProgramComputed(), pop.determineFittestProgram(),
				// gp.getAllTimeBest(),
				pop.getGPProgram(0), fittestnow };
		IGPProgram bestHere = null;
		double fittest = Double.MAX_VALUE;

		if (Integer.parseInt(getParameter(ELITISM).toString()) == 0) {
			for (IGPProgram p : bests) {
				if (p != null) {
					double fitM = fitness.calculateRawFitness(p);
					if (fitM < fittest) {
						fittest = fitM;
						bestHere = p;
					}
				}
			}
		}
		/* consider population if necessary */
		if (bestHere == null) {
			logger.debug("Determining best program failed, consider the whole population");
			for (IGPProgram p : pop.getGPPrograms()) {
				if (p != null) {
					double fitM = fitness.calculateRawFitness(p);
					if (fitM < fittest) {
						fittest = fitM;
						bestHere = p;
					}
				}
			}
		}
		// remember the best
		if (Integer.parseInt(getParameter(ELITISM).toString()) == 0) {
			if ((Boolean) Boolean.parseBoolean(getParameter(PRESERVE_FITTEST).toString())) {
				if (allBest == null || fitness.calculateRawFitness(allBest) > fittest) {
					allBest = bestHere;
					logger.info("Generation " + gen + " new fittest (" + fittest + ") individual: "
							+ getLinkSpecification(bestHere));
				}
			}
		} else {
			allBest = bestHere;
		}

		return bestHere;
	}

	/**
	 * Computes for a given jgap Program its corresponding link specification.
	 *
	 * @param p
	 *            the GP program
	 * @return the link specification
	 */
	private LinkSpecification getLinkSpecification(IGPProgram p) {
		Object[] args = {};
		ProgramChromosome pc = p.getChromosome(0);
		return (LinkSpecification) pc.getNode(0).execute_object(pc, 0, args);
	}

	/**
	 * @return wrap with results
	 */
	private MLResults createSupervisedTrainResult(ACache tsC, ACache ttC) {
		if (tsC == null || ttC == null)
			return createSupervisedResult();
		MLResults result = new MLResults();
		result.setMapping(fitness.getMapping(tsC, ttC, getLinkSpecification(allBest)));
		result.setLinkSpecification(getLinkSpecification(allBest));
		result.setQuality(allBest.getFitnessValue());
		result.addDetail("specifiactions", bestSolutions);
		return result;
	}

	/**
	 * @return wrap with results
	 */
	private MLResults createSupervisedResult() {
		MLResults result = new MLResults();
		result.setMapping(fitness.getMapping(sourceCache, targetCache, getLinkSpecification(allBest)));
		result.setLinkSpecification(getLinkSpecification(allBest));
		result.setQuality(allBest.getFitnessValue());
		result.addDetail("specifiactions", bestSolutions);
		return result;
	}

	/**
	 * Constructs the MLResult for this run.
	 *
	 * @return wrap with results
	 */
	private MLResults createUnsupervisedResult() {
		MLResults result = new MLResults();
		result.setMapping(fitness.getMapping(sourceCache, targetCache, getLinkSpecification(allBest)));
		result.setLinkSpecification(getLinkSpecification(allBest));
		result.setQuality(allBest.getFitnessValue());
		result.addDetail("specifiactions", specifications);
		return result;
	}

	/**
	 * @param size
	 *            number of questions
	 * @return the mapping
	 */
	private AMapping calculateOracleQuestions(int size, GoldStandardBatchReader gsbr) {
		// first get all Mappings for the current population
		logger.info("Getting mappings for output");
		GPPopulation pop = this.gp.getGPPopulation();
		pop.sortByFitness();
		HashSet<LinkSpecification> metrics = new HashSet<LinkSpecification>();
		List<AMapping> candidateMaps = new LinkedList<AMapping>();
		// and add the all time best

		metrics.add(getLinkSpecification(allBest));

		for (IGPProgram p : pop.getGPPrograms()) {
			LinkSpecification m = getLinkSpecification(p);
			if (m != null && !metrics.contains(m)) {
				// logger.info("Adding metric "+m);
				metrics.add(m);
			}
		}
		// fallback solution if we have too less candidates
		if (metrics.size() <= 1) {
			// TODO implement
			throw new NotYetImplementedException("Fallback solution if we have too less candidates.");
		}

		// get mappings for all distinct metrics

		/*
		 * HashMap<String, HashMap<String, Double >> src_without_seen =
		 * gsbr.removeSeen(sourceCache); ACache c_src_without_seen = new MemoryCache();
		 * c_src_without_seen.setInstanceMap(src_without_seen); HashMap<String,
		 * HashMap<String, Double >> trg_without_seen = gsbr.removeSeen(targetCache);
		 * ACache c_src_without_seen = new MemoryCache();
		 */

		logger.info("Getting " + metrics.size() + " full mappings to determine controversy matches...");
		for (LinkSpecification m : metrics) {
			candidateMaps.add(fitness.getMapping(sourceCache, targetCache, m));
		}
		// get most controversy matches
		logger.info("Getting " + size + " controversy match candidates from " + candidateMaps.size() + " maps...");
		;
		List<ALDecider.Triple> controversyMatches = alDecider.getControversyCandidates(candidateMaps, size, gsbr);
		// List<ALDecider.Triple> controversyMatches =
		// alDecider.getControversyCandidates(candidateMaps, size, null);
		// construct answer
		AMapping answer = MappingFactory.createDefaultMapping();
		for (ALDecider.Triple t : controversyMatches) {
			answer.add(t.getSourceUri(), t.getTargetUri(), t.getSimilarity());
		}
		return answer;
	}

	/**
	 * Method to compute best individuals by hand.
	 *
	 * @param gp
	 *            GP genotype
	 * @param gen
	 *            number of generations
	 * @return the GP program
	 */
	private IGPProgram determineFittestUnsup(GPGenotype gp, int gen) {

		GPPopulation pop = gp.getGPPopulation();
		pop.sortByFitness();

		IGPProgram bests[] = { gp.getFittestProgramComputed(), pop.determineFittestProgram(),
				// gp.getAllTimeBest(),
				pop.getGPProgram(0), };
		IGPProgram bestHere = null;
		double fittest = Double.MAX_VALUE;

		for (IGPProgram p : bests) {
			if (p != null) {
				double fitM = fitness.calculateRawFitness(p);
				// System.out.println(fitM);
				if (fitM < fittest) {
					fittest = fitM;
					bestHere = p;
				}
			}
		}
		/* consider population if neccessary */
		if (bestHere == null) {
			logger.debug("Determining best program failed, consider the whole population.");
			for (IGPProgram p : pop.getGPPrograms()) {
				if (p != null) {
					double fitM = fitness.calculateRawFitness(p);
					if (fitM < fittest) {
						fittest = fitM;
						bestHere = p;
					}
				}
			}
		}

		if ((Boolean) Boolean.parseBoolean(getParameter(PRESERVE_FITTEST).toString())) {
			if (allBest != null) {
				// System.out.println("" + fitness.calculateRawFitness(allBest));
			}
			if (allBest == null || fitness.calculateRawFitness(allBest) > fittest) {
				allBest = bestHere;
				logger.info("Generation " + gen + " new fittest (" + fittest + ") individual: "
						+ getLinkSpecification(bestHere));
			}
		}

		return bestHere;
	}

	/**
	 * @return current turn
	 */
	public int getTurn() {
		return turn;
	}

	@Override
	protected MLResults learn(AMapping trainingData) throws UnsupportedMLImplementationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected MLResults activeLearn(AMapping oracleMapping) throws UnsupportedMLImplementationException {
		// TODO Auto-generated method stub
		return null;
	}

}
