package evaluation;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import DataTypes.MTS;
import parser.FSPParser;

public class RunEvaluation {

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		String task = args[0].toLowerCase();
		String filepath = args[1];
		String classname = args[2];
		String groundtruthFile = args[3];
		String outfile = args[4];

		if (task.equals("random"))
		{
			RandomWalkComparison comparison = new RandomWalkComparison();
			evaluate(comparison, filepath, classname, groundtruthFile, outfile, 100000, 10);
		}

		else if (task.equals("full"))
		{
			FullWalkComparison comparison = new FullWalkComparison();
			evaluate(comparison, filepath, classname, groundtruthFile, outfile, -1, 10);
		}

		else if (task.equals("testrandomerror"))
		{
			testRandomError(filepath, classname, groundtruthFile, outfile, 10);
		}
	}
/*		String classname = "Signature";
		String filepath = "evaluation/columba/";
		String groundtruth_path = "evaluation/groundtruth/Signature.lts";
*/		
/*		String classname = "Signature";
		String filepath = "evaluation/signature-all/";
		String groundtruth_path = "evaluation/groundtruth/Signature.lts";
*/		
//		String classname = "Connector";
//		String filepath = "evaluation/dacapo-connector/";
//		String tracename = "Connector";
//		String packagename = "org.apache.catalina.connector";
		
//		String classname = "ToHTMLStream";
//		String filepath = "evaluation/tohtmlstream/";
//		String groundtruth_path = "evaluation/groundtruth/tohtmlstream/ToHTMLStream.lts";
		
/*		String classname = "ElemNumber$NumberFormatStringTokenizer";
		String filepath = "evaluation/dacapo-stringtokenizer/";
		String groundtruth_path = "evaluation/groundtruth/stringtokenizer/NumberFormatStringTokenizer.lts";
*/		
/*		String classname = "StringTokenizer";
		String filepath = "evaluation/stringtokenizer-all/";
		String groundtruth_path = "evaluation/groundtruth/stringtokenizer/StringTokenizerCorrect2.lts";
*/
/*		String classname = "SMTPProtocol";
		String filepath = "evaluation/smtpprotocol-all/";
		String groundtruth_path = "evaluation/groundtruth/smtpprotocol/SMTPProtocolComplete.lts";
*/
		// String classname = "ZipOutputStream";
		// String filepath = "evaluation/jarinstaller/";
		// String groundtruth_path =
		// "evaluation/groundtruth/zipoutputstream/ZipOutputStreamSafe.lts";
		
/*		String classname = "JarOutputStream";
		String filepath = "evaluation/jarinstaller/";
		String groundtruth_path = "evaluation/groundtruth/JarOutputStream.lts";
*/		
//		String classname = "Signature";
//		String filepath = "evaluation/columba/";
//		String groundtruth_path = "evaluation/groundtruth/signature/Signature.lts";
		
/*		String classname = "Socket";
		String filepath = "evaluation/socket-all/";
		String groundtruth_path = "evaluation/groundtruth/socket/SocketSafe.lts";
*/
/*		String classname = "StackAr";
		String filepath = "evaluation/stackar-long/";
		String packagename = "DataStructures.StackAr";
		String tracename = "StackAr";
		String groundtruth_path = "evaluation/groundtruth/stackar/StackAr.lts";
		
*//*		String classname = "SftpConnection";
		String filepath = "evaluation/sftpconnection/";
		String groundtruth_path = "evaluation/groundtruth/sftpconnection/SftpConnection.lts";
*/		

	/**
	 * Run a random walk precision/recall evaluation, outputting results to a
	 * CSV file
	 * 
	 * @param comparison
	 *            Type of comparison engine with which to evaluate the models
	 * @param filepath
	 *            Path to the directory containing the model .lts files
	 * @param classname
	 *            The name of the class whose traces were used to create the
	 *            models
	 * @param groundtruthFile
	 *            The full path to the groundtruth .lts file
	 * @param outfile
	 *            The full path to a file for CSV output. Output will be
	 *            appended to current contents
	 * @param numRuns
	 *            The number of random walks to perform when calculating
	 *            precision and recall
	 * @param maxLength
	 *            The maximum length of randomly-generated traces
	 */
	private static void evaluate(AutomataComparison comparison, String filepath, String classname,
	        String groundtruthFile, String outfile, int numRuns, int maxLength)
	{

		try {
			// Load ground truth and inferred models
			MTS groundTruth = FSPParser.parse(groundtruthFile);
			List<Pair<String, MTS>> nameModelPairs = nameModelPairs(filepath, classname);

			// Open output file in append mode, open output line with class name
			FileWriter csvOut = new FileWriter(outfile, true);
			csvOut.write(classname);

			// Load each model, calculate precision/recall, and output to csv
			for (Pair<String, MTS> nameAndModel : nameModelPairs)
			{
				MTS mts = nameAndModel.b;
				Pair<Double, Double> precisionRecall = comparison.compareAutomata(mts, groundTruth, numRuns, maxLength);
				
				csvOut.write("," + precisionRecall.a + "," + precisionRecall.b);
			}

			csvOut.write("\n");
			csvOut.close();
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Run the random walk precision/recall evaluation multiple times, find the
	 * standard deviation for each model's P/R using various numbers of random
	 * walks, and write results to a CSV file
	 * 
	 * @param filepath
	 *            Path to the directory containing the model .lts files
	 * @param classname
	 *            The name of the class whose traces were used to create the
	 *            models
	 * @param groundtruthFile
	 *            The full path to the groundtruth .lts file
	 * @param outfile
	 *            The full path to a file for CSV output. Output will be
	 *            appended to current contents
	 * @param maxLength
	 *            The maximum length of randomly-generated traces
	 */
	private static void testRandomError(String filepath, String classname, String groundtruthFile, String outfile,
	        int maxLength)
	{
		RandomWalkComparison comparison = new RandomWalkComparison();

		//
		int numRuns = 100000;
		int numTests = 10;

		try
		{
			// Load ground truth and inferred models
			MTS groundTruth = FSPParser.parse(groundtruthFile);
			List<Pair<String, MTS>> nameModelPairs = nameModelPairs(filepath, classname);

			// Open output file in append mode
			FileWriter csvOut = new FileWriter(outfile, true);

			// Write header if file is empty
			FileReader csvIn = new FileReader(outfile);
			if (csvIn.read() == -1)
			{
				writeTestHeader(csvOut, nameModelPairs, numRuns);
			}
			csvIn.close();

			csvOut.write(classname);

			// Matrix, a row for each inferred model of 'numTests' different P/R pairs
			List<List<Pair<Double, Double>>> modelPRsByTestNum = new LinkedList<>();
			for (int i = 0; i < nameModelPairs.size(); i++)
			{
				modelPRsByTestNum.add(new LinkedList<Pair<Double, Double>>());
			}

			//
			for (int i = 0; i < numTests; i++)
			{
				List<Pair<Double, Double>> pRForEachModel = new LinkedList<>();

				// Load each model, calculate precision/recall, and output to csv
				for (Pair<String, MTS> nameAndModel : nameModelPairs)
				{
					MTS mts = nameAndModel.b;
					Pair<Double, Double> newPR = comparison.compareAutomata(mts, groundTruth, numRuns, maxLength);
					pRForEachModel.add(newPR);
				}

				//
				for (int j = 0; j < pRForEachModel.size(); j++)
				{
					modelPRsByTestNum.get(j).add(pRForEachModel.get(j));
				}
			}

			// Get and write out the standard deviations
			for (List<Pair<Double, Double>> oneModelPRs : modelPRsByTestNum)
			{
				Pair<Double, Double> stdDevs = getStdDevs(oneModelPRs);
				csvOut.write("," + stdDevs.a + "," + stdDevs.b);
				csvOut.flush();
			}

			csvOut.write("\n");
			csvOut.close();
		} catch (IOException e)
		{
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Helper method to write CSV headers for the random walk error test
	 * 
	 * @throws IOException
	 */
	private static void writeTestHeader(FileWriter csvOut, List<Pair<String, MTS>> nameModelPairs, int numRuns)
	        throws IOException
	{
		// Write first header line
		csvOut.write("Library");
		for (Pair<String, MTS> nameAndModel : nameModelPairs)
		{
			csvOut.write("," + nameAndModel.a + ",");
		}
		csvOut.write("\n");

		// Write second header line
		for (int i = 0; i < nameModelPairs.size(); i++)
		{
			csvOut.write(",P,R");
		}
		csvOut.write("\n");
	}

	/**
	 * Calculate and return the standard deviation for a list of pairs of
	 * doubles, presumed to be a list of precisions and recalls
	 */
	private static Pair<Double, Double> getStdDevs(List<Pair<Double, Double>> precisionRecallList)
	{
		int size = precisionRecallList.size();

		// Calculate precision/recall means
		double pMean = 0.0;
		double rMean = 0.0;
		for (Pair<Double, Double> precisionRecall : precisionRecallList)
		{
			pMean += precisionRecall.a;
			rMean += precisionRecall.b;
		}
		pMean /= size;
		rMean /= size;

		// Calculate sum of square differences from means (numerators)
		double pNumerator = 0.0;
		double rNumerator = 0.0;
		for (Pair<Double, Double> precisionRecall : precisionRecallList)
		{
			pNumerator += Math.pow(precisionRecall.a - pMean, 2.0);
			rNumerator += Math.pow(precisionRecall.b - rMean, 2.0);
		}

		// Calculate standard deviations
		double pStdDev = Math.sqrt(pNumerator / size);
		double rStdDev = Math.sqrt(rNumerator / size);

		return new Pair<Double, Double>(pStdDev, rStdDev);
	}

	/**
	 * Return a list of model names and their unique filename endings
	 * 
	 * @throws IOException
	 */
	private static LinkedList<Pair<String, MTS>> nameModelPairs(String filepath, String classname) throws IOException
	{
		LinkedList<Pair<String, MTS>> namesAndModels = new LinkedList<>();

		LinkedList<Pair<String, String>> namesAndFilenames = new LinkedList<>();
		namesAndFilenames.add(new Pair<>("traditional1Tail", "Traditional_1_tail.lts"));
		namesAndFilenames.add(new Pair<>("traditional2Tail", "Traditional_2_tail.lts"));
		namesAndFilenames.add(new Pair<>("sekt1Tail", "Enhanced_1_tail.lts"));
		namesAndFilenames.add(new Pair<>("sekt2Tail", "Enhanced_2_tail.lts"));
		namesAndFilenames.add(new Pair<>("optTemi", "Refinement.lts"));
		namesAndFilenames.add(new Pair<>("pesTemi", "Refinement.lts"));
		// modelNamesAndFiles.add(new Pair<>("pesTemi(old)", "RefinementPess.lts"));
		namesAndFilenames.add(new Pair<>("contractor", "Con.lts"));
		namesAndFilenames.add(new Pair<>("synoptic", "Synoptic.lts"));
		namesAndFilenames.add(new Pair<>("invarimint", "Invarimint.InvMintSynoptic.lts"));

		// Load the models; remove some edges for pessimistic TEMI
		for (Pair<String, String> nameAndFile : namesAndFilenames)
		{
			// Name of the model and its .lts filename
			String modelName = nameAndFile.a;
			String filename = nameAndFile.b;

			MTS mts = FSPParser.parse(filepath + "/" + classname + filename);
			if (modelName.equals("pesTemi"))
			{
				mts.removeNonRequired();
			}

			namesAndModels.add(new Pair<String, MTS>(modelName, mts));
		}

		return namesAndModels;
	}
}
