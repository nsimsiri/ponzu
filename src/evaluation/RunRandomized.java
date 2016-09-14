package evaluation;

import java.io.IOException;
import java.util.Set;

import DataTypes.MTS;
import parser.FSPParser;

public class RunRandomized {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] classnames = {
//				"ElemNumber$NumberFormatStringTokenizer", 
//				"Signature", 
//				"StackAr",
//				"SMTPProtocol", 
				"Socket", 
//				"StringTokenizer", 
//				"ToHTMLStream", 
//				"ZipOutputStream"
				};
		String[] classnamesNoFilter = {
//				"ElemNumber$NumberFormatStringTokenizer", 
//				"Signature", 
//				"StackAr",
//				"SMTPProtocol", 
				"Socket", 
//				"StringTokenizer", 
//				"ToHTMLStream", 
//				"ZipOutputStream"
		};
		String filepath = "evaluation/contractor/";
		String[] filepathRandomized = {
//				"evaluation/randomized/numberstringtokenizer/",
//				"evaluation/randomized/signature/",
//				"evaluation/randomized/stackar/",
//				"evaluation/randomized/smtpprotocol/",
				"evaluation/randomized/socket/",
//				"evaluation/randomized/stringtokenizer/",
//				"evaluation/randomized/tohtmlstream/",
//				"evaluation/randomized/zipoutputstream/"
		};
		String[] groundtruth_safe_paths = {
//				"stringtokenizer/NumberFormatStringTokenizerCorrect.lts",
//				"signature/Signature.lts",
//				"stackar/StackAr.lts",
//				"smtpprotocol/SMTPProtocol.lts",
				"socket/SocketSafe.lts",
//				"stringtokenizer/StringTokenizerCorrect2.lts",
//				"tohtmlstream/ToHTMLStream.lts",
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
			Set<String> groundTruthEvents = groundTruth.getEvents();
			
			contractorMTS.removeOtherEvents(groundTruthEvents);
			System.out.println(classnames[i] + " with full invariants");
			comparison.compareAutomata(contractorMTS, groundTruth, 100000, 10);
			
			for(int run = 0; run < 2; run++) {
				int score = (run == 0 ? 20 : 10);
				System.out.println(score + "");
				for(int model = 0; model < 10; model++) {
					try {
						contractorMTS = FSPParser.parse(filepathRandomized[i] + classnamesNoFilter[i] + 
								"-" + run + "-" + model + ".lts");
					} catch (IOException e) {
						System.out.println(e.getMessage());
						continue;
					}
					
					contractorMTS.removeOtherEvents(groundTruthEvents);
					System.out.println(classnames[i] + " with incomplete invariants - Contractor");
					comparison.compareAutomata(contractorMTS, groundTruth, 100000, 10);
					
					try {
						contractorMTS = FSPParser.parse(filepathRandomized[i] + classnamesNoFilter[i] + 
								"_inv_based-" + run + "-" + model + ".lts");
					} catch (IOException e) {
						System.out.println(e.getMessage());
						continue;
					}
					
					contractorMTS.removeOtherEvents(groundTruthEvents);
					System.out.println(classnames[i] + " with incomplete invariants - TEMI invariants-only");
					comparison.compareAutomata(contractorMTS, groundTruth, 100000, 10);

					try {
						contractorMTS = FSPParser.parse(filepathRandomized[i] + classnamesNoFilter[i] + 
								"Refinement-" + run + "-" + model + ".lts");
					} catch (IOException e) {
						System.out.println(e.getMessage());
						continue;
					}
					
					contractorMTS.removeOtherEvents(groundTruthEvents);
					System.out.println(classnames[i] + " with incomplete invariants - TEMI");
					comparison.compareAutomata(contractorMTS, groundTruth, 100000, 10);

					try {
						contractorMTS = FSPParser.parse(filepathRandomized[i] + classnamesNoFilter[i] + 
								"Refinement-req-" + run + "-" + model + ".lts");
					} catch (IOException e) {
						System.out.println(e.getMessage());
						continue;
					}
					
//					Set<String> onlyforSMTP = contractorMTS.getEvents();
//					groundTruth.removeOtherEvents(onlyforSMTP);
					
					contractorMTS.removeOtherEvents(groundTruthEvents);
					System.out.println(classnames[i] + " with incomplete invariants - required TEMI");
					comparison.compareAutomata(contractorMTS, groundTruth, 100000, 10);
				}
			}
			
		}
		
	}

}
