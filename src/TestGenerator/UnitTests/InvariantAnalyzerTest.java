package TestGenerator.UnitTests;

import DataTypes.AnalysisInstance;
import DataTypes.Scenario;
import DataTypes.Scenario2;
import MTSGenerator2.InvParser;
import MTSGenerator2.TraceParser;
import StackAr.DataStructures.StackAr;
import TestGenerator.InvariantAnalyzer;
import daikon.VarInfo;
import daikon.VarInfoName;
import daikon.inv.Implication;
import daikon.inv.Invariant;
import daikon.inv.binary.twoScalar.TwoScalar;
import daikon.inv.ternary.threeScalar.ThreeScalar;
import daikon.inv.unary.scalar.SingleScalar;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by NatchaS on 2/9/17.
 */
public class InvariantAnalyzerTest {
    public static int test_count = 0;
    public static final String FILE_SEP = System.getProperty("file.separator");
    public static final String USER_DIR = System.getProperty("user.dir");

    public static List<Invariant> load_invariants(String classname, String filepath, String packagename, String tracename){
        InvParser parser = new InvParser(filepath + tracename + ".inv.gz");
        AnalysisInstance analysisInstance = parser.parse(packagename + "." + classname);
        new TraceParser(filepath + tracename + ".dtrace.gz", classname, analysisInstance);
        Scenario2 scs = analysisInstance.scenario;
        List<Invariant> allInvs = new ArrayList<>();
        for (String traceID : scs.getSequenceIDs()){
            for(Scenario2.Invocation invocation : scs.getSequenceByID(traceID)){
                allInvs.addAll(invocation.event.getPostCond());
                allInvs.addAll(invocation.event.getPreCond());
            }
        }
        return allInvs;
    }

    public static void test_1_stackar(){
        test_count+=1;
        String absolutePath = String.join(FILE_SEP,
                new String[]{USER_DIR, "unit_test_resources", "InvariantAnalyzerTestResources", "StackAr"})
                + System.getProperty("file.separator");
        System.out.println("Loading: " + absolutePath);

        // create test SUT
        StackAr sut = new StackAr(10);
        for (int i = 0; i < 10; i++) {
            try {
                sut.push(i);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        Map<String, Object> preMap = new HashMap<>();
        preMap.put(VarInfoName.THIS.name(), sut);
        preMap.put("x", new Integer(2));
        preMap.put("capacity", 10);

        List<Invariant> allInvs = load_invariants("StackAr", absolutePath, "StackAr.DataStructures", "StackAr");
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(absolutePath + "test_1_stackar.txt");
            for (Invariant inv : allInvs){
                String cname = inv.getClass().getCanonicalName();

                pw.write(String.format("<%s> %s\n\n", cname, inv));
                if (cname.startsWith("daikon.inv.binary.twoScalar")){
                    TwoScalar ts = (TwoScalar) inv;
                    VarInfo v1 = ts.var1();
                    VarInfo v2 = ts.var2();
                    InvariantAnalyzer.VarInfoParser v1_an = new InvariantAnalyzer.VarInfoParser(inv, v1);
                    InvariantAnalyzer.VarInfoParser v2_an = new InvariantAnalyzer.VarInfoParser(inv, v2);
                    Object o1 = v1_an.parse(preMap, preMap);
                    Object o2 = v2_an.parse(preMap, preMap);
                    pw.write(String.format("[%s]: %s\n\t[%s]: %s\n\t[%s]: %s\n",
                            ts.getClass().getSimpleName(), ts,
                            o1.getClass().getSimpleName(), o1,
                            o2.getClass().getSimpleName(), o2));
                } else if (cname.startsWith("daikon.inv.ternary.threeScalar")){
                    ThreeScalar ts=  (ThreeScalar) inv;
                    VarInfo v1 = ts.var1();
                    VarInfo v2 = ts.var2();
                    VarInfo v3 = ts.var3();
                    InvariantAnalyzer.VarInfoParser v1_an = new InvariantAnalyzer.VarInfoParser(inv, v1);
                    InvariantAnalyzer.VarInfoParser v2_an = new InvariantAnalyzer.VarInfoParser(inv, v2);
                    InvariantAnalyzer.VarInfoParser v3_an = new InvariantAnalyzer.VarInfoParser(inv ,v3);
                    Object o1 = v1_an.parse(preMap, preMap);
                    Object o2 = v2_an.parse(preMap, preMap);
                    Object o3 = v3_an.parse(preMap, preMap);
                    pw.write(String.format("[%s]: %s\n\t[%s]: %s\n\t[%s]: %s\n\t[%s]: %s\n",
                            ts.getClass().getSimpleName(), ts,
                            o1.getClass().getSimpleName(), o1,
                            o2.getClass().getSimpleName(), o2,
                            o3.getClass().getSimpleName(), o3));

                } else if (cname.startsWith("daikon.inv.unary.scalar")){
                    SingleScalar ss = (SingleScalar) inv;
                    VarInfo v1 = ss.var();
                    InvariantAnalyzer.VarInfoParser v1_an = new InvariantAnalyzer.VarInfoParser(inv, v1);
                    Object o1 = v1_an.parse(preMap, preMap);
                    pw.write(String.format("[%s]: %s\n\t[%s]: %s\n",
                            ss.getClass().getSimpleName(), ss,
                            o1.getClass().getSimpleName(), o1));
                } else {
                    pw.write("Cannot interpret " + inv.getClass().getName());
                }
                pw.write("\n------------------------------\n");
            }
        } catch (FileNotFoundException e){
            e.printStackTrace();
        } finally {
            pw.write(String.format("%s/%s tests completed.\n", test_count,
                    Arrays.asList(InvariantAnalyzerTest.class.getMethods()).stream().filter(m -> m.getName().startsWith("test_")).collect(Collectors.toList()).size()
            ));
            pw.close();
        }
    }

    public static void test_2_stackar(){
        test_count+=1;

    }



    public static void main(String[] args){
        test_1_stackar();
//        StackAr st = new StackAr(10);
//        for (Constructor c: st.getClass().getConstructors()){
//            Parameter[] p = c.getParameters();
//            System.out.println(c.getName() + ": " + Arrays.toString(p));
//        }
//        for(Method meth : st.getClass().getMethods()){
//            Parameter[] p = meth.getParameters();
//            System.out.println(meth.getName() + ": " + Arrays.toString(p));
//        }
        try {
            System.out.format("%s/%s tests completed.\n", test_count,
                    Arrays.asList(InvariantAnalyzerTest.class.getMethods()).stream().filter(m -> m.getName().startsWith("test_")).collect(Collectors.toList()).size()
            );
        } catch (Exception e){
            e.printStackTrace();
        }

    }
}
