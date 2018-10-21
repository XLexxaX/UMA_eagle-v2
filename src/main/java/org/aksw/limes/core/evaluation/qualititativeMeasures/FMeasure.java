package org.aksw.limes.core.evaluation.qualititativeMeasures;

import org.aksw.limes.core.datastrutures.GoldStandard;
import org.aksw.limes.core.io.mapping.AMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * F-Measure is the weighted average of the precision and recall
 *
 * @author Tommaso Soru (tsoru@informatik.uni-leipzig.de)
 * @version 1.0
 * @since 1.0
 */
public class FMeasure extends APRF implements IQualitativeMeasure {
    static Logger logger = LoggerFactory.getLogger(FMeasure.class);

    /** 
     * The method calculates the F-Measure of the machine learning predictions compared to a gold standard
     * @param predictions The predictions provided by a machine learning algorithm
     * @param goldStandard It contains the gold standard (reference mapping) combined with the source and target URIs
     * @return double - This returns the calculated F-Measure
     */
    @Override
    public double calculate(AMapping predictions, GoldStandard goldStandard) {

        double p = precision(predictions, goldStandard);
        double r = recall(predictions, goldStandard);

        if (p + r > 0d)
            return 2 * p * r / (p + r);
        else
            return 0d;

    }

    /** 
     * The method calculates the recall of the machine learning predictions compared to a gold standard
     * @param predictions The predictions provided by a machine learning algorithm
     * @param goldStandard It contains the gold standard (reference mapping) combined with the source and target URIs
     * @return double - This returns the calculated recall
     */
    public double recall(AMapping predictions, GoldStandard goldStandard) {
        return new Recall().calculate(predictions, goldStandard);
    }
    public double recallSimple(AMapping predictions, GoldStandard goldStandard) {
        return new Recall().calculateSimple(predictions, goldStandard);
    }

    /** 
     * The method calculates the precision of the machine learning predictions compared to a gold standard
     * @param predictions The predictions provided by a machine learning algorithm
     * @param goldStandard It contains the gold standard (reference mapping) combined with the source and target URIs
     * @return double - This returns the calculated precision
     */
    public double precision(AMapping predictions, GoldStandard goldStandard) {
        return new Precision().calculate(predictions, goldStandard);
    }

	public double precisionSimple(AMapping predictions, GoldStandard goldStandard) {
		return new Precision().calculateSimple(predictions, goldStandard);
	}

	@Override
	public double calculateSimple(AMapping predictions, GoldStandard goldStandard) {
		 double p = precisionSimple(predictions, goldStandard);
	     double r = recallSimple(predictions, goldStandard);

	        if (p + r > 0d)
	            return 2 * p * r / (p + r);
	        else
	            return 0d;
	}

}
