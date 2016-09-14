package TestGenerator;

import daikon.inv.Invariant;
import org.apache.commons.bcel6.verifier.statics.DOUBLE_Upper;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by NatchaS on 11/23/15.
 *
 * Invariant Checking Model
 *
 * <p> Encapsulates invariant checking process by parsing out the Daikon invariant string.
 * The invariant string comes in the format of [left-var] [comparison symbol] [right-var],
 * i.e orig(size(this.theArray[..])) < size(this.theArray[..]) </p>
 *
 * @author Natcha Simsiri
 *
 */
public class TestInvariant {
    private Invariant inv;
    private String leftVarStr;
    private String rightVarStr;
    private String equalityStr;

    public static final List<String> comparatorSet = Arrays.asList(new String[]{
            "==",
            "!=",
            "<=",
            ">=",
            ">",
            "<"
    }); //order must be kept.

    public TestInvariant(){
        this.inv = null;
    }
    public TestInvariant(Invariant inv){
        this(inv.toString());
        this.inv = inv;
    }

    /* Constructor
    *
    * @param invStr (required) Daikon Invariant Object's String, format stated above (orig(size(this.theArray[..])) < size(this.theArray[..])
    * */
    public TestInvariant(String invStr){
        String[] invParts = null;
        String comparator = "";
        for(String _comparator : comparatorSet){
            if (invStr.contains(_comparator)){
                invParts = invStr.split(_comparator);
                for(int i = 0; i < invParts.length;i++){
                    invParts[i] = invParts[i].replaceAll("\\s+","");
                    comparator = _comparator;
                }
                break;
            }
        }
        if (invParts == null || comparator.isEmpty()) {
            throw new TestInstanceException(String.format("%s not valid invariant", invStr));
        }

        this.leftVarStr = invParts[0];
        this.equalityStr = comparator;
        this.rightVarStr = invParts[1];

    }

    @Override
    public String toString() {
        return String.format("%s %s %s", this.leftVarStr, this.equalityStr, this.rightVarStr);
    }

    /*
    * Method recursively evaluates an invariant expression. Returns a value (usually a number value, as Daikon invariant
    * Expression either evaluates to a number comparison/null comparison.
    *
    * @param var (required) variable expression to be evaluated against the invariant
    * @param preVarMap (required) Hashmap of the SUT's field to the value prior a method call
    * @param postVarMap (required) Hashmap of the SUT's field to the value after a method call
    * */
    public Double parseAndEvaluateVar(String var, Map<String, Object> preVarMap, Map<String, Object> postVarMap){
        Map<String, Object> curVarMap = postVarMap;
        if (isNumeric(var)) return Double.parseDouble(var);
        if (!isArithmeticExpr(var)){
            // simple case - parse singular expression
            String[] varNoParen = parseExprParen(var);
            boolean isFindingSize = false;
            for(int i =0 ; i < varNoParen.length; i++){
                // loop through parsed var, e.g orig(size(this.theArray[..])) -> ['orig','size', 'this.theArray[..]')
                if (isOrig(varNoParen[i])){
                    curVarMap = preVarMap;
                } else if (isSize(varNoParen[i])){
                    isFindingSize = true;
                } else {
                    // varNoParen[i] must be a field name, e.g 'this.theArray[..]'
                    String fieldString = varNoParen[i];
                    var = varNoParen[i]; // singular expression, i.e this.theArray[..]
                    if (isFindingSize){
                        //usually this clause follows a var that is an array.
                        if (isArrayExp(var)){
                            String[] arrSplits = parseArray(var); // retrieve full name of array, this.theArray[..] -> this.theArray
                            var = parseFieldFromExp(arrSplits[0]); // parses *.field -> field
                        }
                        if (curVarMap.containsKey(var)) {
                            return new Double(Array.getLength(curVarMap.get(var)));
                        }

                    } else {
                        // var is usually a field with numerical type, e.g topOfStack.
                        var = parseFieldFromExp(var);
                        if (curVarMap.containsKey(var)) {
                            Object fieldValue = curVarMap.get(var);
                            if (fieldValue instanceof Number){
                                return new Double(((Number)fieldValue).doubleValue());
                            }
                        }
                    }
                    throw new TestInstanceException("Cannot parse: " + var);
                }
            }
        } else {
            // handle later - is a complex expression ex. this.topOfStack - orig(this.topOfStack) + 1 == 0
            var = var.replaceAll("\\s+", ""); // gets rid of white space
            String[] exps = parseArithmeticExprWithDelim(var);
            Double sum = new Double(0);
            String op = "+";
            for(int i = 0 ; i < exps.length; i++){
                String exp = exps[i].replaceAll("\\s+", "");
                if (isArithOp(exp)){
                    op = exps[i];
                } else {
                    Double objToCompute = parseAndEvaluateVar(exp, preVarMap, postVarMap);
                    sum = computeArith(sum, objToCompute, op);
                }
            }
            return sum;
        }
        throw new TestInstanceException("no such variable: " + var);
    }

    /* evaluates the invariant based on the comparison symbol - symbols are hard coded, maybe a better way is to allow Users to provide
    * an interface impl or a lambda expression - but I think this is sufficient
    *
    * @param preVarMap (required) Hashmap of the SUT's field to the value prior a method call
    * @param postVarMap (required) Hashmap of the SUT's field to the value after a method call
    * */
    public void makeAssertion(Map<String, Object> preVarMap, Map<String, Object> postVarMap){
        Double leftVarValue = parseAndEvaluateVar(this.leftVarStr, preVarMap, postVarMap);
        Double rightVarValue = parseAndEvaluateVar(this.rightVarStr, preVarMap, postVarMap);
        if (equalityStr.equals("==")) assert(leftVarValue.compareTo(rightVarValue)==0);
        else if (equalityStr.equals("<=")) assert(leftVarValue.compareTo(rightVarValue) == -1|| leftVarValue.compareTo(rightVarValue) == 1);
        else if (equalityStr.equals("<")) assert(leftVarValue.compareTo(rightVarValue) == -1);
        else if (equalityStr.equals(">=")) assert(leftVarValue.compareTo(rightVarValue) == 1 || leftVarValue.compareTo(rightVarValue) == 0);
        else if (equalityStr.equals(">")) assert(leftVarValue.compareTo(rightVarValue) == 1);
        else if (equalityStr.equals("!=")) assert(leftVarValue.compareTo(rightVarValue) == 1 || leftVarValue.compareTo(rightVarValue) == -1);
        else {
            System.out.println("Invariant not comparable");
        }
//        System.out.println("successfully asserted " + this.toString());
    }

    /* Does arithmetic operation based on the operation string input, hardcoded for + and - for now since Daikon doesn't do much more
     * @param n1 (required) first number value
     * @param n2 (required) second number value
     * @param opr (required) operation string
    * */
    private Double computeArith(Double n1, Double n2, String opr){
        if (opr.equals("+")){
            return n1.doubleValue() + n2.doubleValue();
        } else if (opr.equals("-")){
            return n1.doubleValue() - n2.doubleValue();
        }
        return null;
    }

    /* Series of utility function that checks the kind of invariant expression.
    * @param exp (required), invariant expression
    * */
    private boolean isArithOp(String exp){ return exp.equals("+") || exp.equals("-");}
    private boolean isOrig(String exp){ return exp.contains("orig");}
    private boolean isSize(String exp){ return exp.contains("size");}
    private boolean isArrayExp(String exp) { return exp.contains("[") && exp.contains("]");}
    private boolean hasArithmeticOps(String exp) { return exp.contains("-") || exp.contains("+");}
    private boolean isNumeric(String exp){
        try {
            Double d = Double.parseDouble(exp);
        } catch (NumberFormatException e){
            return false;
        }
        return true;
    }

    private boolean isArithmeticExpr(String exp){
        if (hasArithmeticOps(exp) && !isNumeric(exp)){
            String[] split = exp.split("[-+]");
            if (split.length>1) return true;
        }
        return false;
    }

    private String[] parseExprParen(String exp) {return exp.split("[\\(\\)]");}
    private String parseFieldFromExp(String exp){
        String[] p =  exp.split("\\.");
        return p[p.length-1];
    }
    private String[] parseArithmeticExprWithDelim(String exp){
        return exp.split("(?<=-)|(?=-)|(?<=\\+)|(?=\\+)");
    }

    private String[] parseArray(String exp){
        return exp.split("[\\[\\]]");
    }

    /* Unit test..sort of, kind of thing */
    public static void main(String[] args){
        TestInvariant test = new TestInvariant();
        int[] a1 = new int[]{1,2,3};
        int a2 = 3;
        int a3 = 10;
        int a4 = 5;
        HashMap<String, Object> preMap = new HashMap<String, Object>();
        preMap.put("theArray", a1);
        preMap.put("topOfStack", a2);
        preMap.put("capacity", a3);
        preMap.put("DEFAULT_CAPACITY", a4);
        HashMap<String, Object> postMap = (HashMap<String, Object>) preMap.clone();
        postMap.put("theArray", new int[]{1,2,3,4});
        postMap.put("topOfStack", a2+1);


        Object num = test.parseAndEvaluateVar("-1", preMap, preMap);
        assert(num instanceof Number);
        assert(((Number) num).intValue() == -1);
        Object topOfStack = test.parseAndEvaluateVar("this.topOfStack", preMap, postMap);
        assert(topOfStack instanceof Number);
        assert(((Number) topOfStack).intValue() == (Integer)postMap.get("topOfStack"));

        topOfStack = test.parseAndEvaluateVar("orig(this.topOfStack)", preMap, preMap);
        assert(topOfStack instanceof Number);
        assert(((Number) topOfStack).intValue() == (Integer)preMap.get("topOfStack"));

        Object sizeOfArr = test.parseAndEvaluateVar("size(this.theArray[..])", preMap, postMap);
        assert((Number) sizeOfArr).intValue() == ((Array.getLength(postMap.get("theArray"))));

        sizeOfArr = test.parseAndEvaluateVar("orig(size(this.theArray[..]))", preMap, postMap);
        assert((Number) sizeOfArr).intValue() == ((Array.getLength(preMap.get("theArray"))));

        Number testAdd = (Number)test.parseAndEvaluateVar("1 - 2 +3", preMap, postMap);
        assert(testAdd.intValue()==2);
//        sizeOfArr = test.parseVar("size(this.theArray[..]-1)", preMap, postMap);

        Number testExpr = (Number)test.parseAndEvaluateVar("size(this.theArray[..])-1", preMap, postMap);
        assert(testExpr.intValue() == Array.getLength(postMap.get("theArray"))-1);

        TestInvariant test1 = new TestInvariant("size(this.theArray[..])-1 <= size(this.theArray[..])");
        test1.makeAssertion(preMap, postMap);

        TestInvariant test2 = new TestInvariant("size(this.theArray[..]) != orig(size(this.theArray[..]))");
        test2.makeAssertion(preMap, postMap);

        TestInvariant test3 = new TestInvariant("this.topOfStack - orig(this.topOfStack) - 1 == 0");
        test3.makeAssertion(preMap, postMap);

        System.out.println("all invariants asserted");


    }
}
