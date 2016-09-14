package YicesHelpers;

/*
 * Generates Yices commands
 */
public class Generator {
	
	public String define(String var, String type){
		String yicesExpr = "(define "+var+"::"+type+")";
		return yicesExpr;
	}

	public String defineArray(String var, String type, String retType){
		String yicesExpr = "(define "+var+"::(-> "+type+" "+retType+")";
		return yicesExpr;
	}
	
	public String defineValue(String var, String type, int value){
		String yicesExpr = "(define "+var+"::(subtype (n::"+type+") (= n "+value+")))";
		return yicesExpr;
	}
	
	public String expr(String expr){
		if(expr.contains("array_")){
			expr = evalArray(expr);
		}
		String yicesExpr = "(assert "+expr+")";
		return yicesExpr;
	}
	
	public String reset()
	{
		return "(reset)";
	}

	public String evalArray(String expr){
		String formatedExpr = expr+"";
		String begin="";
		String end="";
		String name="";
		String value="";
		//array_folder[x] -> (array_folder x)
		if(expr.contains("array_")){
			int arrayIdx = expr.indexOf("array_");
			int br1 = expr.indexOf('[', arrayIdx);
			int br2 = expr.indexOf(']', arrayIdx);
			
			begin=expr.substring(0, arrayIdx);
			name=expr.substring(arrayIdx,br1);
			value=expr.substring(br1+1,br2);
			end=expr.substring(br2+1,expr.length());
			formatedExpr = begin+"("+name+" "+value+")"+end;
			//Replace with (name value)
		}
		
		return formatedExpr;
	}
}
