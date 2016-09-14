package evaluation;

import DataTypes.MTS;

public interface AutomataComparison {
	/**
	 * Compare two automata by calculating precision and recall. For precision,
	 * generate traces on the inferred model and calculate the proportion that
	 * the ground truth accepts. For recall, do the same but swap the models.
	 * 
	 * @param inferredModel
	 *            A model inferred using some specification mining technique
	 * @param groundTruth
	 *            The known-correct, ground truth model
	 * @param numRuns
	 *            How many traces to generate (if the chosen evaluation strategy
	 *            supports it)
	 * @param maxLength
	 *            The maximum length of generated traces
	 * @return A precision and recall pair
	 */
	public Pair<Double, Double> compareAutomata(MTS inferredModel, MTS groundTruth, int numRuns, int maxLength);
}