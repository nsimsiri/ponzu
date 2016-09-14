package evaluation;

import java.io.IOException;

import DataTypes.MTS;
import parser.FSPParser;

public class RunEvaluationContractor {

	public static void main(String[] args) 
	{
		String[] classnames = {
//				"ElemNumber$NumberFormatStringTokenizer", 
//				"Signature", 
//				"StackAr",
//				"SMTPProtocol", 
//				"Socket", 
//				"StringTokenizerClean", 
				"ToHTMLStream", 
//				"ZipOutputStream"
				};
		String[] classnamesNoFilter = {
//				"ElemNumber$NumberFormatStringTokenizer", 
//				"Signature", 
//				"StackAr",
//				"SMTPProtocol", 
//				"Socket", 
//				"StringTokenizer", 
				"ToHTMLStream", 
//				"ZipOutputStream"
		};
		String filepath = "evaluation/contractor/";
		String filepathNoFilter = "evaluation/contractor/no-invariant-filtering/";
		String[] groundtruth_safe_paths = {
//				"stringtokenizer/NumberFormatStringTokenizerCorrect.lts",
//				"signature/Signature.lts",
//				"stackar/StackAr.lts",
//				"smtpprotocol/SMTPProtocolComplete.lts",
//				"socket/SocketSafe.lts",
//				"stringtokenizer/StringTokenizerCorrect2.lts",
				"tohtmlstream/ToHTMLStream.lts",
//				"zipoutputstream/ZipOutputStreamSafe.lts"
		};
		String[] groundtruth_good_paths = {
				"stringtokenizer/NumberFormatStringTokenizer.lts",
				"socket/SocketGood.lts",
				"stringtokenizer/StringTokenizer2.lts",
				"zipoutputstream/ZipOutputStreamSpec.lts"
		};

		MTS contractorMTS = null;
		MTS groundTruth = null;
		RandomWalkComparison comparison = new RandomWalkComparison();
		
		for (int i = 0; i < classnames.length; i++)
		{
			try {
				contractorMTS = FSPParser.parse(filepath + classnames[i] + ".lts");
				groundTruth = FSPParser.parse("evaluation/groundtruth/" + groundtruth_safe_paths[i]);
			} catch (IOException e) {
				System.out.println(e.getMessage());
				System.exit(1);
			}
			
			System.out.println(classnames[i] + " with filtering");
			comparison.compareAutomata(contractorMTS, groundTruth, 100000, 10);
			
			try {
				contractorMTS = FSPParser.parse(filepathNoFilter + classnamesNoFilter[i] + ".lts");
			} catch (IOException e) {
				System.out.println(e.getMessage());
				System.exit(1);
			}
			
			contractorMTS.removeOtherEvents(groundTruth.getEvents());
			System.out.println(classnames[i] + " without filtering");
			comparison.compareAutomata(contractorMTS, groundTruth, 100000, 10);
			
		}
		
/*		try {
			contractorMTS = FSPParser.parse("evaluation/contractor/StackAr.lts");
			groundTruth = FSPParser.parse("evaluation/groundtruth/stackar/StackAr.lts");
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		
		AutomataComparison.compareAutomata(contractorMTS, groundTruth, 100000, 10);
*/	}

}
