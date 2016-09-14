package evaluation;

import java.util.LinkedList;
import java.util.List;

import DataTypes.MTS;
import DataTypes.MTS_state;
import DataTypes.MTS_transition;

public class FullWalkComparison implements AutomataComparison {

	private int numAccepted = 0;
	private int numTotal = 0;

	public Pair<Double, Double> compareAutomata(MTS inferredModel, MTS groundTruth, int numRuns, int maxLength)
	{
		// Calculate precision and recall
		double precision = calculateMetric(inferredModel, groundTruth, maxLength);
		double recall = calculateMetric(groundTruth, inferredModel, maxLength);

		// Print results
		System.out.println(inferredModel.getName());
		System.out.println("  Precision: " + precision);
		System.out.println("  Recall:    " + recall);

		return new Pair<Double, Double>(precision, recall);
	}

	/**
	 * Calculate a precision or recall metric by generating all possible traces
	 * a test model can produce up to a specified length and calculating the
	 * proportion that an evaluation model accepts
	 * 
	 * @param testMts
	 *            Model from which to generate traces
	 * @param evalMts
	 *            Model on which to test trace acceptance
	 * @param maxLength
	 *            The maximum length of generated traces
	 * @return
	 */
	private double calculateMetric(MTS testMts, MTS evalMts, int maxLength)
	{
		MTS_state initialState = testMts.getInitialState();

		// Depth-first walk the model to simulate each trace
		for (MTS_transition trans : testMts.getAllOutGoing(initialState.getName()))
		{
			LinkedList<MTS_transition> emptyTrace = new LinkedList<>();
			dfWalk(testMts, evalMts, emptyTrace, trans, maxLength);
		}

		// Print detailed results, reset accepted/total trace counts
		System.out.println("    " + numAccepted + " of " + numTotal);
		double result = (double) numAccepted / (double) numTotal;
		numAccepted = 0;
		numTotal = 0;
		return result;
	}

	/**
	 * In the test model, recursively walk all possible traces up to a specified
	 * length, evaluating its acceptance in the evaluation model at each step
	 * 
	 * @param testMts
	 *            Model from which to generate traces
	 * @param evalMts
	 *            Model on which to test trace acceptance
	 * @param oldTrace
	 *            Trace from the previous recursive step
	 * @param newTrans
	 *            Transition to add to the old trace
	 * @param maxLength
	 *            The maximum length of generated traces
	 */
	private void dfWalk(MTS testMts, MTS evalMts, LinkedList<MTS_transition> oldTrace, MTS_transition newTrans,
	        int maxLength)
	{
		// Only walk traces up to specified length
		if (oldTrace.size() >= maxLength)
		{
			return;
		}

		// Copy the trace and add the transition
		LinkedList<MTS_transition> newTrace = (LinkedList<MTS_transition>) oldTrace.clone();
		newTrace.add(newTrans);

		// Test new trace for acceptance in the eval model
		simulateTrace(evalMts, newTrace);

		// Optimization: stop walking early if max trance length was just hit
		if (newTrace.size() >= maxLength)
		{
			return;
		}

		// Walk all outgoing transitions
		MTS_state currentState = testMts.getState(newTrans.getEnd());
		for (MTS_transition trans : testMts.getAllOutGoing(currentState.getName()))
		{
			dfWalk(testMts, evalMts, newTrace, trans, maxLength);
		}
	}

	/**
	 * Test a trace for acceptance in the evaluation model
	 * 
	 * @param evalMts
	 *            Model on which to test trace acceptance
	 * @param trace
	 *            Trace to test
	 */
	private void simulateTrace(MTS evalMts, List<MTS_transition> trace)
	{
		List<MTS_state> currentStates = new LinkedList<>();
		currentStates.add(evalMts.getInitialState());
		boolean traceIsAccepted = true;

		// Walk each transition in the trace
		for (MTS_transition currentTransition : trace)
		{
			List<MTS_state> nextStates = new LinkedList<>();

			// Try continuing this trace from all current/potential states
			for (MTS_state currentState : currentStates)
			{
				// Find transitions from the current state that accept the next
				// event in the trace
				for (MTS_transition nextTrans : evalMts.getAllOutGoing(currentState.getName()))
				{
					if (nextTrans.getMTSAName().equals(currentTransition.getMTSAName()))
					{
						nextStates.add(evalMts.getState(nextTrans.getEnd()));
					}
				}
			}

			// Fail if the next event cannot be accepted
			if (nextStates.size() == 0)
			{
				traceIsAccepted = false;
				break;
			}
			currentStates = nextStates;
		}

		numTotal++;
		if (traceIsAccepted)
		{
			numAccepted++;
		}
	}
}