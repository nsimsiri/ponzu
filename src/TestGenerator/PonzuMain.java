package TestGenerator;

import MTSGenerator2.TraceParser;
import TestGenerator.ArgumentCache.ArgumentCacheStream;
import TestGenerator.ArgumentCache.ArgumentObjectInfo;
import TestGenerator.ArgumentCache.IArgumentCache;
import TestGenerator.ArgumentCache.MethodSignaturesPair;
import TestGenerator.ArgumentCache.RandomizedArgumentMap;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

//import modbat.mbt.Main;

/**
 * Created by NatchaS on 2/26/16.
 * TODO (1) Chain Dtrace files ie. [StackAr1.dtrace.gz StackAr2.dtrace.gz ..] + use GeneratorApp class instead of
 * TODO (2): get rid of hardcoded classpath ../../lib/* by exporting ponzu to runnable jar (currently failing or something)
 * TODO (3): Use consistent naming scheme for modbat scala file.
 * TODO (4): ** OPTIMIZE OBJECT CACHING MEMORY - see AgumentObjectInfo
 */
public class PonzuMain {
    private static boolean verbose = true;
    public static final String CUR_PATH = Paths.get("").toAbsolutePath().toString();;
    public static void main(String[] args) throws Exception{
        /*
        * --test-class=DataStructures.StackAr
        * --tests=test_suite_command.txt
        * in test_suite_command.txt:
        * DataStructures.StackArTester
        * */

        if (args.length>0){
            PonzuOptions options = PonzuOptions.processArguments(args);
            // QUICK DEBUGS
            if (options.isDebug()){
                checkArgCache(options.getDebugFile(), null);
                System.exit(0);
            } else if (options.isDebugCacheSpace()){
                checkArgSpaceUsage(options.getDebugCacheSpaceFile());
                System.exit(0);
            } else if (options.isDebugCacheMethod()){
                Map<String, String> debugFileMethodMap = options.getDebugCacheMethod();
                checkCacheForMethod(debugFileMethodMap.get(PonzuOptions.DEBUG_CACHE_METHOD), null,
                                    debugFileMethodMap.get(PonzuOptions.DEBUG_METHOD), null);
                System.exit(0);
            }

            final String testClassnameFull = options.getFullTestClassname(); // DataStructures.StackAr

            final String testPackagename = PonzuOptions.parsePackagename(testClassnameFull); //DataStructures
            final String testClassname = PonzuOptions.parseClassname(testClassnameFull); //StackAr
            List<String> testSuiteNames = new ArrayList<String>();
            if (PonzuOptions.runAll || PonzuOptions.runChicory){
                final List<String> testSuiteCommands = options.loadTestCommands();
                ExecutorService threadPool = Executors.newFixedThreadPool(testSuiteCommands.size());

                // (1) run chicory to get dtrace + invariant files => <SUT_name>.dtrace.tgz and <SUT_name>.inv.tgz.
                if (!testSuiteCommands.isEmpty()) {
                    Collection<Callable<Void>> chicoryTasks = new ArrayList<Callable<Void>>();
                    for (final String testSuiteClassnameFull : testSuiteCommands){ // DataStructures.StackArTester : [...]
                        final String testSuitePackagename = PonzuOptions.parsePackagename(testSuiteClassnameFull);
                        testSuiteNames.add(testSuitePackagename);
                        chicoryTasks.add(new Callable<Void>(){
                            public Void call(){
                                runChicoryOnNewProcess(testPackagename, testClassname, testSuiteClassnameFull); // (DataStuctures, StackAr, DataStructures.StackArTester)
                                return null;
                            }
                        });
                    }
                    threadPool.invokeAll(chicoryTasks);
                    threadPool.shutdown();
                    if (verbose){
                        System.out.println("[PONZU] all Chicory processes finished.");
                    }
                    if (options.isTestingCacheDeserialization()){
                        checkArgCache("./" + ArgumentCacheStream.getDefaultNaming(testClassnameFull), testClassnameFull);
                    }
                }
            }

            // (2) run MTS Generator to generate models and input for modbat.
            if (PonzuOptions.runAll || PonzuOptions.runMTSGenerator) {
                runMTSGenerator(testPackagename, testClassname, options.loadTestCommands(), options);
            }

            //(3) run MODBAT
            if (PonzuOptions.runAll || PonzuOptions.runModbat){
                runModbat(testPackagename, testClassname, options);
            }

        } else {
            System.out.println("[PONZU 0.2] commandline help coming soon..");
        }

    }

    public static void runChicoryOnNewProcess(String packageName, String className, String sutCommand){
        System.out.println("\t\t - run chicory new process method");
        String ppt_select_pattern = getSelectPattern(packageName, className);
        String ppt_omit_pattern = getOmitPattern(packageName, className);
        RuntimeMXBean jvm = ManagementFactory.getRuntimeMXBean();
        String separator = System.getProperty("file.separator");
        String classpath = jvm.getClassPath();
        String javapath = System.getProperty("java.home") + separator + "bin" + separator + "java";

        // Parse test suite command, must treat all case [junit runner, test, arg1, arg2
        String testSuiteName;
        if (sutCommand.contains(PonzuOptions.JUNIT_RUNNER)){
            String[] junitSplit = sutCommand.split(" "); // ["org.junit.runner.JUnitCore", "TestSuite"]
            if (junitSplit.length!=2 || !junitSplit[0].equals("org.junit.runner.JUnitCore")){
                throw new IllegalArgumentException("[PONZU Exception] bad junit test execution format.");
            }
            testSuiteName = junitSplit[1];
        } else {
            // NORMAL CASE
            testSuiteName = sutCommand;
        }
        String testClassname = PonzuOptions.parseClassname(testSuiteName);
        String chicoryCmdString = String.format("daikon.Chicory --daikon --dtrace-file=%s.dtrace.gz %s %s %s", testClassname, ppt_select_pattern, ppt_omit_pattern, sutCommand);

        // TODO 2: get rid of hardcoded classpath ../../lib/* by exporting ponzu to runnable jar (currently failing or something)
//        ArrayList<String> javaCmd = new ArrayList<String>(Arrays.asList(new String[]{javapath, "-cp", String.format("%s:../../lib/*",classpath)}));
        ArrayList<String> javaCmd = new ArrayList<String>(Arrays.asList(new String[]{javapath, "-Xms2500M", "-cp", String.format("%s", classpath)}));

        String[] chicoryCmd = chicoryCmdString.split(" ");
        javaCmd.addAll(Arrays.asList(chicoryCmd));
        if (verbose) {
            System.out.println(System.getProperty("user.dir"));
            System.out.println(javaCmd);
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(javaCmd);
            pb.redirectErrorStream(true);
            File log = new File(String.format("%s_ChicoryOutput.txt", className));
            pb.redirectOutput(log);
            pb.redirectError(log);
            Process process = pb.start();
            process.waitFor();

        } catch (IOException e){
            e.printStackTrace();
        } catch (InterruptedException e){
            e.printStackTrace();
        }

        System.out.println("[PONZU] \t\t - finished");

    }

    public static void runMTSGenerator(String packageName, String className, List<String> testSuites, PonzuOptions options){
        // must be able to join invariant files somehow.
//        String invname   = String.format("%s.inv.gz", className);
        String invname   = String.format("%s.inv.gz", PonzuOptions.parseClassname(testSuites.get(0)));

        // must rename serialized file from classname -> test suite classname. i.e DataStructures_StackArObjectMap.ser -> DataStructures_StackArTesterObjectMap.ser. + union
        // refer to TODO 1 - this means unioning serialized files.
        String sername   = ArgumentCacheStream.getDefaultNaming(String.format("%s.%s", packageName, className));

        boolean foundinv   = new File(String.format("./%s", invname)).exists();
        boolean foundSer   = new File(String.format("./%s", sername)).exists();

        for(String testSuite : testSuites){
            String testSuiteClassname = PonzuOptions.parseClassname(testSuite);

            String tracename = String.format("%s.dtrace.gz", testSuiteClassname);
            boolean foundTrace = new File(String.format("./%s", tracename)).exists();
            if (!foundTrace && !foundinv && !foundSer) {
                if (!foundTrace) System.out.format("[Ponzu] %s not found\n", tracename);
                if (!foundinv) System.out.format("[Ponzu] %s not found\n", invname);
                if (!foundSer) System.out.format("[Ponzu] %s not found\n", sername);
                System.exit(1);
            }
        }

        String[] mtsgenArg = new String[3 + testSuites.size()]; // classname, filepath, packagename, tracename.
        mtsgenArg[0] = className;
        mtsgenArg[1] = "./";
        mtsgenArg[2] = packageName;
        for(int i =  0 ; i < testSuites.size(); i++){
            mtsgenArg[i+3] = PonzuOptions.parseClassname(testSuites.get(i));
        }
        // Will replace MTSGenerator2.TraceParser with the real one when ready
        System.out.println("[PONZU] Running MTRGen with " + Arrays.asList(mtsgenArg).toString());
        TraceParser.main(mtsgenArg);
    }

    //TODO need to figure out ouputted modbat scala format.
    /*
    *  Compile modbat model in scala and run to generate tests.
     *  - Will have to create err and bin filenames. ie. <classname>_err, <classname>_bin
    * */
    public static void runModbat(String packageName, String className, PonzuOptions options){
        final String modelName_TEMP = className + "Enhanced_1_tail";
        final String scalaModelFilename_TEMP = String.format("%s.scala", modelName_TEMP);//String.format("%s_inva_based_2.scala", className);
        final String PATH_TO_MODBAT_TEMP = String.format("%s/openmodbat-3.0.jar", CUR_PATH); //MUST HAVE MODBAT IN WORKING DIRECTORY.
//        final String PATH_TO_MODBAT_TEMP = String.format("../../lib/openmodbat-3.0.jar");

        final String errFilename = String.format("%s_err", className);
        final String binFilename = String.format("%s_bin", className);
        final String classpath = ManagementFactory.getRuntimeMXBean().getClassPath();

        File errFile = new File(String.format("./%s", errFilename));
        File binFile = new File(String.format("./%s", binFilename));

        if (!PonzuOptions.hasFile("./"+scalaModelFilename_TEMP)){
            System.out.println(scalaModelFilename_TEMP + " not found.");
            System.exit(1);
        }

        if (!ModbatModelGenerator.makeInvariantAssertion){
            System.out.println("[PONZU] NOTE* INVARIANT ASSERTION IS OFF - TURN ON IN ModbatModelGenerator.java");
        }

        Runtime runTime = Runtime.getRuntime();
        try {

            File modbatLog = new File("modbat-log.txt");
            if (options.isCompilingScalaModel()){
                if (errFile.exists()) {
                    if (verbose) System.out.println("deleting " + errFile);
                    FileUtils.deleteDirectory(errFile);
                }
                if (binFile.exists()) {
                    if (verbose) System.out.println("deleting " + binFile);
                    FileUtils.deleteDirectory(binFile);
                }
                if (!errFile.mkdir() || !binFile.mkdir()){
                    System.out.println("[PONZU] unable to create bin/err directories for Modbat");
                    System.exit(1);
                }

                Process p0 = runTime.exec("scala -help");
                p0.waitFor();
                if (p0.exitValue()!=0){
                    System.out.println("[PONZU ERROR] Scala 2.11.x not found, please install. ");
                    System.exit(1);
                }

                String compileScala = String.format("scalac -d %s -cp %s %s", binFilename, classpath, scalaModelFilename_TEMP);
                System.out.println("[PONZU: compiling] " + compileScala + "\n");

                ProcessBuilder pb_compile = new ProcessBuilder(compileScala.split(" "));
                pb_compile.redirectErrorStream(true);
                pb_compile.redirectError(modbatLog);
                pb_compile.redirectOutput(modbatLog);
                Process p1 = pb_compile.start();
                p1.waitFor();
                if (p1.exitValue()!=0){
                    System.out.println("[PONZU ERROR] unable to compile modbat model");
                    System.exit(1);
                }
            }

            String modbatOption = options.loadModbatConfig(modelName_TEMP, binFilename, errFilename);
            String modbatRunCmd = String.format("scala -J-Xmx2G -cp %s %s %s", classpath, PATH_TO_MODBAT_TEMP, modbatOption);
            System.out.println("[PONZU running modbat] " + modbatRunCmd);

            ProcessBuilder pb = new ProcessBuilder(modbatRunCmd.split(" "));
            pb.redirectErrorStream(true);
            pb.redirectError(modbatLog);
            pb.redirectOutput(modbatLog);
            Process p2 = pb.start();
            p2.waitFor();
            if (p2.exitValue()!=0){
                System.out.println("[PONZU ERROR] Failed running modbat");
                System.exit(1);
            }


        } catch (IOException e){
            e.printStackTrace();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    private static String getSelectPattern(String packageName, String className) {
        StringBuilder sb = new StringBuilder("");
        String[] packages = packageName.split("\\.");
        for(int i =0; i < packages.length; i++){
            sb.append(packages[i]);
            if(i<packages.length-1) sb.append("\\.");
        }
        return String.format("--ppt-select-pattern=^%s\\.%s$", sb.toString(), className);
    }

    private static String getOmitPattern(String packageName, String className){
        String op = "--ppt-omit-pattern=";
        String omitString = String.format("%s.%s.main", packageName, className);
        if (packageName.length()==0){
            omitString=String.format("%s.main", className);
        }
        return String.format("--ppt-omit-pattern=%s", omitString);
    }

    private static void checkArgSpaceUsage(String serFile){
        int MAX_COUNT = 10000; //limit number of objects per method

        Function<Void, Void> memLook = (Void v) -> {
            Runtime rt = Runtime.getRuntime();
            System.out.printf("\n----MEMORY USAGE----\nFree=%s bytes\nMax=%s bytes\nTotal=%s bytes\n\n", rt.freeMemory(), rt.maxMemory(), rt.totalMemory());
            return null;
        };
        System.err.println("[PONZU] Testing cache deserialization: " + serFile);
        ArgumentCacheStream stream = new ArgumentCacheStream(serFile);
        IArgumentCache cacheMap = (IArgumentCache)stream.readObject();
        RandomizedArgumentMap realCacheMap = (RandomizedArgumentMap)cacheMap;
        Map<MethodSignaturesPair, List<List<ArgumentObjectInfo>>> cacheImpl = realCacheMap.getCacheMap();

        Iterator<Map.Entry<MethodSignaturesPair, List<List<ArgumentObjectInfo>>> > it = cacheImpl.entrySet().iterator();
        System.out.println("fnished loading");
        while(it.hasNext()){
            Map.Entry<MethodSignaturesPair, List<List<ArgumentObjectInfo>>> e = it.next();
            System.out.println(e.getKey());

            Iterator<List<ArgumentObjectInfo>> it2 = e.getValue().iterator();
            while(it2.hasNext()){
                List<ArgumentObjectInfo> params = it2.next();
                if (MAX_COUNT > 0){
                    StringBuilder sb = new StringBuilder("(");
                    for(ArgumentObjectInfo aoi : params){
                        Object aoi_obj = aoi.getObject_();
                        String aoi_obj_classname = "null_classname";
                        if (aoi_obj != null) aoi_obj_classname = aoi_obj.getClass().getSimpleName();
                        sb.append(String.format("[%s %s], ", aoi_obj, aoi_obj_classname));
                    }
                    sb.append(")");
                    System.out.println("\t" + sb.toString());
                    MAX_COUNT--;
                } else {
//                    it2.remove();
                }
            }
        }
        memLook.apply(null);
//        RandomizedArgumentMap m = new RandomizedArgumentMap(cacheImpl);
//        ArgumentCacheStream acs = new ArgumentCacheStream(serFile);
//        acs.writeObject(m);
    }

    private static void checkCacheForMethod(String serFile, String name, String methodName, String[] sigs){
        if (name == null) name = ArgumentCacheStream.splitDefaultNaming(serFile);
        System.err.printf("Check method: %s for %s\n", methodName, serFile);
        ArgumentCacheStream stream = new ArgumentCacheStream(serFile);
        IArgumentCache cacheMap = (IArgumentCache)stream.readObject();
        RandomizedArgumentMap realCacheMap = (RandomizedArgumentMap)cacheMap;
        Map<MethodSignaturesPair, List<List<ArgumentObjectInfo>>> cacheImpl = realCacheMap.getCacheMap();

        HashSet<MethodSignaturesPair> methodWithInputName = new HashSet();
        for(MethodSignaturesPair msp : cacheImpl.keySet()){
            if (msp.getMethodName().equals(methodName)){
                methodWithInputName.add(msp);
            }
        }
        for(MethodSignaturesPair key : methodWithInputName){
            List<List<ArgumentObjectInfo>> value = cacheImpl.get(key);
            System.out.println(key);
            for(List<ArgumentObjectInfo> params : value){
                StringBuilder sb = new StringBuilder("(");
                for(ArgumentObjectInfo aoi : params){
                    Object aoi_obj = aoi.getObject_();
                    String aoi_obj_classname = "null_classname";
                    if (aoi_obj != null) aoi_obj_classname = aoi_obj.getClass().getSimpleName();
                    sb.append(String.format("[%s %s], ", aoi_obj, aoi_obj_classname));
                }
                sb.append(")");
                System.out.println("\t" + sb.toString());
            }
        }
        System.out.printf("\n=========== OBSERVING OBJECTS ============\n");

        for(MethodSignaturesPair key : methodWithInputName){
            List<List<ArgumentObjectInfo>> value = cacheImpl.get(key);
            System.out.println("\n\nKEY: " + key);
            int i = 1;
            for(List<ArgumentObjectInfo> params : value){
                StringBuilder sb = new StringBuilder("PARAM SET " + i++ + ":\n");
                for(ArgumentObjectInfo aoi : params){
                    Object aoi_obj = aoi.getObject_();
                    String aoi_obj_classname = "null_classname";
                    if (aoi_obj != null) {
                        aoi_obj_classname = aoi_obj.getClass().getSimpleName();
                    }
                    sb.append(String.format("\n-------\n%s\n%s\n---------\n], ",aoi_obj_classname, aoi.getSerializedObject()));
                }
                sb.append("\n");
                System.out.println(sb.toString());
            }
        }
    }

    private static void checkArgCache(String serFile, String name){
        if (name == null) name = ArgumentCacheStream.splitDefaultNaming(serFile);
        System.err.println("[PONZU] Testing cache deserialization: " + serFile);
        ArgumentCacheStream stream = new ArgumentCacheStream(serFile);
        IArgumentCache cacheMap = (IArgumentCache)stream.readObject();
        RandomizedArgumentMap realCacheMap = (RandomizedArgumentMap)cacheMap;
        Map<MethodSignaturesPair, List<List<ArgumentObjectInfo>>> cacheImpl = realCacheMap.getCacheMap();
        for(Map.Entry<MethodSignaturesPair, List<List<ArgumentObjectInfo>>> e : cacheImpl.entrySet()){
            System.out.println(e.getKey());
            for(List<ArgumentObjectInfo> params : e.getValue()){
                StringBuilder sb = new StringBuilder("(");
                for(ArgumentObjectInfo aoi : params){
                    Object aoi_obj = aoi.getObject_();
                    String aoi_obj_classname = "null_classname";
                    if (aoi_obj != null) aoi_obj_classname = aoi_obj.getClass().getSimpleName();
                    sb.append(String.format("[%s %s], ", aoi_obj, aoi_obj_classname));
                }
                sb.append(")");
                System.out.println("\t" + sb.toString());
            }
        }
        System.err.printf("[PONZU]: COMPLETED DESERIALIZATION TEST: %s\n", cacheImpl.size());
        try {
            FileOutputStream fos = new FileOutputStream(new File(String.format("cache-stat-%s.csv", name)));
            PrintWriter out = new PrintWriter(fos);
            List<String> methodName = new ArrayList<>();
            List<Integer> count = new ArrayList<>();
            MethodSignaturesPair maxMSP = null;
            MethodSignaturesPair minMSP = null;
            for (Map.Entry<MethodSignaturesPair, List<List<ArgumentObjectInfo>>> e : cacheImpl.entrySet()){
                methodName.add(e.getKey().getMethodName());
                count.add(e.getValue().size());
                if (maxMSP == null) maxMSP = e.getKey();
                else {
                    if (cacheImpl.get(maxMSP).size() < e.getValue().size()) maxMSP = e.getKey();
                }
                if (minMSP == null) minMSP = e.getKey();
                else {
                    if (cacheImpl.get(minMSP).size() > e.getValue().size()) minMSP = e.getKey();
                }
            }
            out.write("ID, MethodName, count\n");

            double av = 0;
            Map<Integer, Integer> modeMap = new HashMap<>();
            for(int i = 0; i < count.size(); i++){
                out.write(String.format("M%s, %s, %s\n", i+1, methodName.get(i), count.get(i)));
                av += count.get(i);
                if (modeMap.containsKey(count.get(i))){
                    modeMap.put(count.get(i), modeMap.get(count.get(i)) + 1);
                } else {
                    modeMap.put(count.get(i), 1);
                }
            }
            out.write("\n");
            out.write("SIZE,, "+ cacheImpl.size() + "\n");
            out.write("MEAN,,"+ av/cacheImpl.size() + "\n");
            out.write("MODE,, "+ Collections.max(modeMap.values()) + "\n");
            out.write(String.format("MAX, %s, %s\n", maxMSP.getMethodName(), cacheImpl.get(maxMSP).size()));
            out.write(String.format("MIN, %s, %s\n", minMSP.getMethodName(), cacheImpl.get(minMSP).size()));
            out.flush();
            out.close();
        } catch (IOException e){
            e.printStackTrace();
        }

    }
}
