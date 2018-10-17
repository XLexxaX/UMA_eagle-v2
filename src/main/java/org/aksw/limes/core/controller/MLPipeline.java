package org.aksw.limes.core.controller;

import static org.junit.Assert.fail;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.aksw.limes.core.evaluation.evaluator.EvaluatorFactory;
import org.aksw.limes.core.evaluation.evaluator.EvaluatorType;
import org.aksw.limes.core.evaluation.qualititativeMeasures.PseudoFMeasure;
import org.aksw.limes.core.exceptions.UnsupportedMLImplementationException;
import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.config.Configuration;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.aksw.limes.core.io.mapping.reader.RDFMappingReader;
import org.aksw.limes.core.ml.algorithm.ACoreMLAlgorithm;
import org.aksw.limes.core.ml.algorithm.ActiveMLAlgorithm;
import org.aksw.limes.core.ml.algorithm.Eagle;
import org.aksw.limes.core.ml.algorithm.LearningParameter;
import org.aksw.limes.core.ml.algorithm.MLAlgorithmFactory;
import org.aksw.limes.core.ml.algorithm.MLImplementationType;
import org.aksw.limes.core.ml.algorithm.MLResults;
import org.aksw.limes.core.ml.algorithm.SupervisedMLAlgorithm;
import org.aksw.limes.core.ml.algorithm.UnsupervisedMLAlgorithm;
import org.aksw.limes.core.ml.algorithm.eagle.core.ALDecider;
import org.aksw.limes.core.ml.algorithm.eagle.util.PropertyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execution pipeline for generating mappings using ML.
 * Provides overloaded convenience methods.
 *
 * @author Kevin Dreßler
 */
public class MLPipeline {

    public static final Logger logger = LoggerFactory.getLogger(MLPipeline.class);
    
    public static AMapping execute(
            ACache source,
            ACache target,
            Configuration configuration,
            String mlAlgorithmName,
            MLImplementationType mlImplementationType,
            List<LearningParameter> learningParameters,
            String trainingDataFile,
            EvaluatorType pfmType,
            int maxIt, int maxRounds, GoldStandardBatchReader gsbr
    ) throws UnsupportedMLImplementationException {
        Class<? extends ACoreMLAlgorithm> clazz = MLAlgorithmFactory.getAlgorithmType(mlAlgorithmName);
        MLResults mlm;
        AMapping trainingDataMap = MappingFactory.createDefaultMapping();
        mlImplementationType=MLImplementationType.SUPERVISED_ACTIVE;
        configuration.setMlImplementationType(MLImplementationType.SUPERVISED_ACTIVE);
        if (
                mlImplementationType == MLImplementationType.SUPERVISED_BATCH){
            // TODO make it check for different readers
            RDFMappingReader mappingReader = new RDFMappingReader(trainingDataFile);
            trainingDataMap = mappingReader.read();
        }
        mlImplementationType = MLImplementationType.SUPERVISED_ACTIVE;
        AMapping result;
		switch (mlImplementationType) {
            case SUPERVISED_BATCH:
                SupervisedMLAlgorithm mls = new SupervisedMLAlgorithm(clazz);
                mls.init(learningParameters, source, target);
                mls.getMl().setConfiguration(configuration);
                mlm = mls.learn(trainingDataMap);
                logger.info("Learned: " + mlm.getLinkSpecification().getFullExpression() + " with threshold: " + mlm.getLinkSpecification().getThreshold());
                return mls.predict(source, target, mlm);
            case SUPERVISED_ACTIVE:
                
            	ActiveMLAlgorithm mla = new ActiveMLAlgorithm(clazz);
                mla.init(learningParameters, source, target);
                mla.getMl().setConfiguration(configuration);
                mlm = mla.activeLearn();
                
                double rating;
                String reply, evaluationMsg;
                int i = 0;
                AMapping examples = null;
                //AMapping examples = mla.getNextExamples(0);
    			while (i < maxRounds) {
    				 i++;
    				 
    				 
    				 AMapping nextExamples;
    				 if (examples == null) {
    				 	examples = mla.getNextExamples(5, gsbr);
    				 	nextExamples = examples;
    				 	examples.setPredicate("http://www.w3.org/2002/07/owl#sameAs");
    				 } else { 
    					 nextExamples = mla.getNextExamples(5, gsbr);
    					 examples.getMap().putAll(nextExamples.getMap());
        				 examples.setSize(examples.getSize()+5);
    				 }
    				 if (examples.getSize()> 140) {
    					 System.out.println("");
    				 }
    				 
                     int j = 0;
                     for (String s : nextExamples.getMap().keySet()) {
                         for (String t : nextExamples.getMap().get(s).keySet()) {
                             boolean rated = false;
                             j++;
                             do {
                                 evaluationMsg = "Exemplar #" + i + "." + j + ": (" + s + ", " + t + ")";
                                 try {
                                     logger.info(evaluationMsg);
                                     rating = gsbr.isMatch(s, t);
                                     if (rating >= -1.0d && rating <= 1.0d) {
                                         nextExamples.getMap().get(s).put(t, rating);
                                         rated = true;
                                     } else {
                                         logger.error("Input number out of range [-1,+1], please try again...");
                                     }
                                 } catch (NoSuchElementException e) {
                                     logger.error("Input did not match floating point number, please try again...");
                                 }
                             } while (!rated);
                         }
                     }
                     mlm = mla.asActive().activeLearn(examples);
                     //result = mla.predict(source, target, mlm);
                     
                     gsbr.eval(mlm.getMapping(), i, mlm.getLinkSpecification().getFullExpression() + "-" + mlm.getLinkSpecification().getThreshold());
                 }
                 logger.info("Learned: " + mlm.getLinkSpecification().getFullExpression() + " with threshold: " + mlm.getLinkSpecification().getThreshold());
                 //result = mla.predict(source, target, mlm);
                 gsbr.eval(mlm.getMapping(), maxRounds, mlm.getLinkSpecification().getFullExpression());
                 return mlm.getMapping();
            	
            	
            case UNSUPERVISED:
                UnsupervisedMLAlgorithm mlu = new UnsupervisedMLAlgorithm(clazz);
                mlu.init(learningParameters, source, target);
                mlu.getMl().setConfiguration(configuration);
                PseudoFMeasure pfm = null;
                if(pfmType != null){
                    pfm = (PseudoFMeasure) EvaluatorFactory.create(pfmType);
                }
                mlm = mlu.learn(pfm);
                logger.info("u - Learned: " + mlm.getLinkSpecification().getFullExpression() + " with threshold: " + mlm.getLinkSpecification().getThreshold());
                return mlu.predict(source, target, mlm);
            default:
                throw new UnsupportedMLImplementationException(clazz.getName());
        }
    }
}
