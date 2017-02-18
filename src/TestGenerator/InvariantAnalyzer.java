package TestGenerator;

import MTSGenerator2.Converter;
import daikon.VarInfo;
import daikon.VarInfoName;
import daikon.inv.Invariant;
import org.apache.commons.lang3.math.NumberUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by NatchaS on 11/23/15.
 *
 * Invariant Checking Model
 *
 * <p> Encapsulates invariant checking process by parsing out the Daikon invariant string.
 * The invariant string comes in the format of [left-var] [comparison symbol] [right-var],
 * i.e orig(size(this.theArray[..])) < size(this.theArray[..]). Information related to
 * parsing daikon invariant string can be found here: https://plse.cs.washington.edu/daikon/download/doc/daikon.html#Interpreting-output
 * </p>
 *
 * @author Natcha Simsiri
 *
 */
public class InvariantAnalyzer {
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

    public InvariantAnalyzer(){
        this.inv = null;
    }

    public InvariantAnalyzer(Invariant inv){
        if (!Converter.isProcessableInvariantType(inv)){
            throw new IllegalArgumentException("Cannot parse invariant of type: " + inv.getClass());
        }

        String invStr = inv.toString();
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
        this.inv = inv;
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

    /**
     * VarInfoName parser. VarInfoName is the "name" of the VarInfo - the terms that composes an Invariant object.
     * This method recursively maps each operation on a "name" to an action on the real object during test generation.
     */

    public static class VarInfoParser {
        public Invariant inv;
        public VarInfo varInfo;
        public VarInfoName varName;

        public VarInfoParser(Invariant inv, VarInfo var){
            this.inv = inv;
            this.varInfo = var;
            this.varName = varInfo.get_VarInfoName();
        }

        public static String parse_print(VarInfoName varName){
            if (varName instanceof VarInfoName.Simple){
                VarInfoName.Simple vn = (VarInfoName.Simple)varName;
                return String.format("(0 %s)", vn.name);
            } else if (varName instanceof VarInfoName.Add){
                VarInfoName.Add vn = (VarInfoName.Add)varName;
                return String.format("(1 %s + %s)", parse_print(vn.term), vn.amount);
            } else if (varName instanceof VarInfoName.SizeOf){
                VarInfoName.SizeOf vn = (VarInfoName.SizeOf)varName;
                return String.format("(2 |%s|)", parse_print(vn.sequence));
            } else if (varName instanceof VarInfoName.Poststate){
                VarInfoName.Poststate vn = (VarInfoName.Poststate)varName;
                return String.format("(3 post[%s])", parse_print(vn.term));
            } else if (varName instanceof VarInfoName.Prestate){
                VarInfoName.Prestate vn = (VarInfoName.Prestate)varName;
                return String.format("(4 pre[%s])", parse_print(vn.term));
            } else if (varName instanceof VarInfoName.Field){
                VarInfoName.Field vn = (VarInfoName.Field)varName;
                return String.format("(5 %s->%s)", parse_print(vn.term), vn.field);
            } else if (varName instanceof VarInfoName.Elements) {
                VarInfoName.Elements vn = (VarInfoName.Elements)varName;
                return String.format("(6 Array(%s))", parse_print(vn.term));
            } else if (varName instanceof VarInfoName.Subscript){
                VarInfoName.Subscript vn = (VarInfoName.Subscript)varName;
                return String.format("(7 %s[%s]", vn.sequence, vn.index);
            } else if (varName instanceof VarInfoName.Slice){
                VarInfoName.Slice vn = (VarInfoName.Slice)varName;
                return String.format("(8 %s[%s:%s]", vn.sequence, vn.i, vn.j);
            } else {
                return  String.format("(%s %s)", varName.getClass().getSimpleName(), varName);
            }
        }

        public Object parse(Map<String, Object> origMap, Map<String, Object> postMap) throws IllegalStateException {
            Map<String, Object> objectMap = (this.varInfo.isPrestate()) ? origMap : postMap;
            return this._parse(this.varName, objectMap);
        }


        /**
         * Parses values from the invariants given the object instance.
         * */
        private Object _parse(VarInfoName varName, Map<String, Object> objectMap) throws IllegalStateException {
            if (varName instanceof VarInfoName.Simple){
                // parses simple name
                VarInfoName.Simple baseName = (VarInfoName.Simple)varName;
                if (this.varInfo.isThis() && baseName.name.equals(VarInfoName.THIS.name())){
                    // case "this" -> returns instance
                    return objectMap.get(VarInfoName.THIS.name());
                } else if (baseName.isLiteralConstant()){
                    // case literal "a", "-1"
                    if (NumberUtils.isNumber(baseName.name)) return Integer.parseInt(baseName.name);
                    return baseName.name;
                } else if (objectMap.containsKey(baseName.name)){
                    // case name for something
                    return objectMap.get(baseName.name);
                }

            } else if (varName instanceof VarInfoName.Add){
                // expects a number to be returned for an add.
                VarInfoName.Add vn = (VarInfoName.Add)varName;
                Object returnVal = _parse(vn.term, objectMap);
                if (returnVal instanceof Number){
                    Integer value = ((Number)returnVal).intValue();
                    return value + vn.amount;
                }

            } else if (varName instanceof VarInfoName.SizeOf){
                // expects a sequence to be returned
                VarInfoName.SizeOf vn = (VarInfoName.SizeOf)varName;
                Object sequenceObj = _parse(vn.sequence, objectMap);
                if (sequenceObj.getClass().isArray()){
                    return ((Object[])sequenceObj).length;
                } else if (sequenceObj instanceof Collection){
                    return ((Collection)sequenceObj).size();
                }

            } else if (varName instanceof VarInfoName.Poststate){
                VarInfoName.Poststate vn = (VarInfoName.Poststate)varName;
                return _parse(vn.term, objectMap);

            } else if (varName instanceof VarInfoName.Prestate){
                VarInfoName.Prestate vn = (VarInfoName.Prestate)varName;
                return _parse(vn.term, objectMap);

            } else if (varName instanceof VarInfoName.Field){
                VarInfoName.Field vn = (VarInfoName.Field)varName;
                // is static
                if (this.varInfo.isStaticConstant()){
                    try {
                        Class<?> clazz = Class.forName(vn.term.name());
                        return _get_static_field(clazz, vn.field);
                    } catch(ClassNotFoundException e){
                        System.err.println(e.getMessage());
                        throw new IllegalStateException(String.format("Expects a static field \"%s\" with map %s", varName, objectMap));
                    }
                } else if (objectMap.containsKey(vn.term)){
                    return _get_object_field(objectMap.get(vn.term), vn.field);
                }
                return _get_object_field(_parse(vn.term, objectMap), vn.field);

            } else if (varName instanceof VarInfoName.Elements) {
                // returns the array/collection "sequence" from "sequence[]" string

                VarInfoName.Elements vn = (VarInfoName.Elements)varName;
                return _parse(vn.term, objectMap);

            } else if (varName instanceof VarInfoName.Subscript){
                // returns an element of sequence[index]

                VarInfoName.Subscript vn = (VarInfoName.Subscript)varName;
                Object sequenceObj = _parse(vn.sequence, objectMap);
                Object indexObj = _parse(vn.index, objectMap);
                Integer idx = _parse_index(indexObj);

                if (idx!=null){
                    if (sequenceObj.getClass().isArray()){
                        return ((Object[]) sequenceObj)[idx];
                    } else if (sequenceObj instanceof List<?>){
                        return ((List<?>) sequenceObj).get(idx);
                    }
                }

            } else if (varName instanceof VarInfoName.Slice){
                // returns a subarray sequence[i:j]

                VarInfoName.Slice vn = (VarInfoName.Slice)varName;
                Integer idx_i = _parse_index(_parse(vn.i, objectMap));
                Integer idx_j = _parse_index(_parse(vn.j, objectMap));
                Object arrayObj = _parse(vn.sequence, objectMap);
                if (idx_i != null && idx_j != null){
                    if (arrayObj.getClass().isArray()){
                        return Arrays.copyOfRange(((Object[]) arrayObj), idx_i, idx_j);
                    } else if (arrayObj instanceof List<?>){
                        return ((List<?>) arrayObj).subList(idx_i, idx_j);
                    }
                }
            }

            throw new IllegalStateException(String.format("cannot parse VarInfoName \"%s\" of type %s with Map %s\n",
                    this.varName.toString(), this.varName.getClass().getSimpleName(),  objectMap));
        }

        private Integer _parse_index(Object indexObj){
            Integer idx = null;
            if (indexObj instanceof Number) idx = ((Number) indexObj).intValue();
            else if (indexObj instanceof String) idx = Integer.parseInt((String) indexObj);
            return idx;
        }

        private Object _get_static_field(Class<?> c, String fieldName){
            try {
                Field staticField = c.getDeclaredField(fieldName);
                staticField.setAccessible(true);
                return staticField.get(null);
            } catch (NoSuchFieldException e){
                e.printStackTrace();
            } catch (IllegalAccessException e){
                e.printStackTrace();
            }
            throw new IllegalArgumentException(String.format("Cannot find field \"%s\" in object type \"%s\".", fieldName, c));
        }
        private Object _get_object_field(Object o, String fieldName){
            try {
                Field field = o.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(o);
            } catch (NoSuchFieldException e){
                e.printStackTrace();
            } catch (IllegalAccessException e){
                e.printStackTrace();
            }
            throw new IllegalArgumentException(String.format("Cannot find field \"%s\" in object type \"%s\".", fieldName, o.getClass()));
        }
    }



    /* Unit test..sort of, kind of thing */
    public static void main(String[] args){
        VarInfoName x = VarInfoName.parse("A - 1");
        System.out.println(x);
        Collection<VarInfoName> xx = x.inOrderTraversal();
        for(VarInfoName k : xx){
            System.out.format("%s: %s", k.getClass().getSimpleName(), k);
        }
        /*
        InvariantAnalyzer test = new InvariantAnalyzer();
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

        InvariantAnalyzer test1 = new InvariantAnalyzer("size(this.theArray[..])-1 <= size(this.theArray[..])");
        test1.makeAssertion(preMap, postMap);

        InvariantAnalyzer test2 = new InvariantAnalyzer("size(this.theArray[..]) != orig(size(this.theArray[..]))");
        test2.makeAssertion(preMap, postMap);

        InvariantAnalyzer test3 = new InvariantAnalyzer("this.topOfStack - orig(this.topOfStack) - 1 == 0");
        test3.makeAssertion(preMap, postMap);

        System.out.println("all invariants asserted");
        */
    }
}
