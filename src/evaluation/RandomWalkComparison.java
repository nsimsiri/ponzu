package evaluation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import DataTypes.MTS;
import DataTypes.MTS_state;
import DataTypes.MTS_transition;

public class RandomWalkComparison implements AutomataComparison {

	static private Random randGen = new Random();

	public Pair<Double, Double> compareAutomata(MTS toCompare, MTS groundTruth, int numRuns, int maxLength)
	{
		double precision = new Float(0);
		double recall = new Float(0);

		for (int i = 0; i < numRuns; i++)
		{
			ArrayList<MTS_transition> simulateTrace = createTrace(toCompare, maxLength);
			double simulateCoeff = simulateTrace(groundTruth, simulateTrace);
			precision += simulateCoeff;

			simulateTrace = createTrace(groundTruth, maxLength);
			simulateCoeff = simulateTrace(toCompare, simulateTrace);
			recall += simulateCoeff;
		}

		System.out.println(toCompare.getName());
		System.out.println("  Precision: " + (precision / numRuns));
		System.out.println("  Recall:    " + (recall / numRuns));

		return new Pair<Double, Double>(precision / numRuns, recall / numRuns);
	}

	private ArrayList<MTS_transition> createTrace(MTS input, int maxLength)
	{
		ArrayList<MTS_transition> simulateTrace = new ArrayList<MTS_transition>();
		boolean lastWasSelfTransition = false;
		int selfTransitionCount = 0;
		int selfTransitionRepeat = 0;
		MTS_state currentState = input.getInitialState();

		for (int j = 0; j < maxLength; j++)
		{
			ArrayList<MTS_transition> currentTransitions = input.getAllOutGoing(currentState.getName());

			if (currentTransitions.size() == 0)
				break;

			// Count how many self-transitions in the current state
			if (!lastWasSelfTransition)
			{
				for (MTS_transition tran : currentTransitions)
				{
					if (tran.getStart() == tran.getEnd())
					{
						selfTransitionCount++;
					}
				}
			}

			if (selfTransitionCount == currentTransitions.size() && selfTransitionRepeat == selfTransitionCount)
				break;

			// Pick a random transition
			if (selfTransitionRepeat < maxLength / input.getStateSize())
			{
				MTS_transition nextTransition = currentTransitions.get(randGen.nextInt(currentTransitions.size()));
				simulateTrace.add(nextTransition);
				currentState = input.getState(nextTransition.getEnd());

				if (nextTransition.getStart() == nextTransition.getEnd())
				{
					selfTransitionRepeat++;
					lastWasSelfTransition = true;
				} else
				{
					selfTransitionCount = 0;
					selfTransitionRepeat = 0;
					lastWasSelfTransition = false;
				}
			}
			// When there are too many self-transitions that are repeatedly
			// traversed, try to leave the state.
			else
			{
				if (currentTransitions.size() - selfTransitionCount == 0)
					break;

				MTS_transition nextTransition = null;
				int nextIndex = randGen.nextInt(2 * currentTransitions.size() - selfTransitionCount);

				if (nextIndex < currentTransitions.size())
				{
					nextTransition = currentTransitions.get(nextIndex);
				} else
				{
					int countOut = 0;
					int index = nextIndex - currentTransitions.size();
					for (MTS_transition testTran : currentTransitions)
					{
						if (testTran.getStart() != testTran.getEnd())
						{
							if (index == countOut)
							{
								nextTransition = testTran;
								break;
							} else
							{
								countOut++;
							}
						}
					}
				}

				if (nextTransition.getStart() == nextTransition.getEnd())
				{
					selfTransitionRepeat++;
					lastWasSelfTransition = true;
				} else
				{
					selfTransitionCount = 0;
					selfTransitionRepeat = 0;
					lastWasSelfTransition = false;
				}

				simulateTrace.add(nextTransition);
				currentState = input.getState(nextTransition.getEnd());
			}
		}

		return simulateTrace;
	}

	private double simulateTrace(MTS simulateOn, ArrayList<MTS_transition> trace)
	{
		double coeff = 0;
		int count = 0;

		Queue<MTS_state> currentStates = new LinkedList<MTS_state>();
		currentStates.add(simulateOn.getInitialState());

		for (MTS_transition currentTransition : trace)
		{
			Queue<MTS_state> nextStates = new LinkedList<MTS_state>();
			Set<String> nextStatesNames = new LinkedHashSet<String>();

			for (MTS_state currentState : currentStates)
			{
				ArrayList<MTS_transition> analyzeTransitions = simulateOn.getAllOutGoing(currentState.getName());

				for (MTS_transition analyzing : analyzeTransitions)
				{
					if (analyzing.getMTSAName().equals(currentTransition.getMTSAName())
					        && !nextStatesNames.contains("S" + analyzing.getEnd()))
					{
						nextStates.add(simulateOn.getState(analyzing.getEnd()));
						nextStatesNames.add("S" + analyzing.getEnd());
					}
				}
			}

			if (nextStates.size() == 0)
			{
				// break;
				if (currentTransition.getStart() != currentTransition.getEnd())
					break;
			} else
			{
				count++;
				currentStates = nextStates;
			}
		}

		coeff = (double) count / (double) trace.size();
		return coeff;
	}
}