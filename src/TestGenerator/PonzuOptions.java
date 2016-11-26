package TestGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by NatchaS on 3/2/16.
 */
public class PonzuOptions {
    public static boolean verbose = true;
    public static boolean runAll = true;
    public static boolean runChicory = false;
    public static boolean runMTSGenerator = false;
    public static boolean runModbat = false;

    public static final String SUT_COMMAND_FLAG="SUT_COMMAND";

    public static final String DEBUG_FLAG="--debug";
    public static final String DEBUG_CACHE_SPACE_FLAG="--debug-cache-space";
    public static final String DEBUG_CACHE_METHOD = "--debug-cache-method";
    public static final String DEBUG_METHOD = "--method";

    public static final String TEST_CLASS_FLAG="--test-class";
    public static final String MODBAT_CONFIG_FLAG= "--modbat-cfg";
    public static final String TEST_SUITES_FLAG="--tests";
    public static final String SER_TEST = "--sertest"; // test cache deserialization.

    public static final String MODBAT_N = "--modbat-n";
    public static final String MODBAT_S = "--modbat-s";
    public static final String MODBAT_COMPILE = "--modbat-c";


    public static final String JUNIT_RUNNER = "org.junit.runner.JUnitCore";

    private String testClassname;
    private HashMap<String, String> optionMap;

    public PonzuOptions(HashMap<String, String> optionMap){
        this.optionMap = optionMap;
        this.testClassname = optionMap.get(TEST_CLASS_FLAG);
    }

    public static PonzuOptions processArguments(String[] args){
        HashMap<String, String> argMap = new HashMap<>();
        for(int i = 0; i < args.length;i++){
            String cmd = args[i];
            if (cmd.substring(0,2).equals("--") && cmd.contains("=")){
                String[] pair_cmd = cmd.split("=");
                argMap.put(pair_cmd[0], pair_cmd[1]);
            } else if (cmd.substring(0,2).equals("--")){
                // single flag argument - no input with flag. i.e '--sertest'
                argMap.put(cmd, "");
                if (cmd.equals("--run-chicory")) {
                    runAll = false;
                    runChicory = true;
                    if (verbose) System.out.println("running only Chicory..");
                } else if (cmd.equals("--run-mtsgen")){
                    runAll=false;
                    runMTSGenerator=true;
                    if (verbose) System.out.println("running only MTSGenerator..");
                } else if (cmd.equals("--run-modbat")){
                    runAll=false;
                    runModbat=true;
                    if (verbose) System.out.println("running only Modbat..");
                }
            } else {
                // no option flag ('--') means it is the SUT execution command
                String testSuiteCommand = cmd;
                int numOfTestSuiteArg = (args.length - 1) - i;
                while(numOfTestSuiteArg > 0){
                    i++;
                    numOfTestSuiteArg--;
                    testSuiteCommand += String.format(" %s", args[i]);
                }
                argMap.put(SUT_COMMAND_FLAG, testSuiteCommand);
            }
        }

        if (!argMap.containsKey(TEST_CLASS_FLAG)){
            System.out.println("Failed to load options: please specify System Under Test's full class.");
            System.exit(1);
        }

        return new PonzuOptions(argMap);
    }

    public String getSimpleTestClassname(){
        String[] fullClasspaths = this.testClassname.split("\\.");
        return fullClasspaths[fullClasspaths.length-1];
    }

    public String getTestPackagename(){
        StringBuilder sb = new StringBuilder("");
        String[] testPackageClassname = this.testClassname.split("\\.");
        for(int i = 0; i < testPackageClassname.length-1; i++){
            sb.append(testPackageClassname[i]);
            if (i < testPackageClassname.length-2); sb.append(".");
        }
        return sb.toString();
    }

    public String getModbatNumberOfTests(){
        return this.optionMap.getOrDefault(MODBAT_N, "100");
    }

    public String getModbatSeed(){return this.optionMap.get(MODBAT_S);}
    public boolean hasModbatSeed(){ return this.optionMap.containsKey(MODBAT_S);}

    public String getFullTestClassname(){
        return this.testClassname;
    }

    public boolean isDebug(){
        return this.optionMap.containsKey(DEBUG_FLAG);
    }
    public boolean isDebugCacheSpace() { return this.optionMap.containsKey(DEBUG_CACHE_SPACE_FLAG);}
    public boolean isDebugCacheMethod() { return this.optionMap.containsKey(DEBUG_CACHE_METHOD) && this.optionMap.containsKey(DEBUG_METHOD);}


    public String getDebugFile(){
        return this.optionMap.get(DEBUG_FLAG);
    }
    public String getDebugCacheSpaceFile() { return this.optionMap.get(DEBUG_CACHE_SPACE_FLAG);}
    public Map<String, String> getDebugCacheMethod() {
        Map<String, String> debugMethodMap = new HashMap<>();
        debugMethodMap.put(DEBUG_CACHE_METHOD, this.optionMap.get(DEBUG_CACHE_METHOD));
        debugMethodMap.put(DEBUG_METHOD, this.optionMap.get(DEBUG_METHOD));
        return debugMethodMap;
    }

    public boolean isTestingCacheDeserialization(){
        return this.optionMap.containsKey(SER_TEST);
    }

    public final List<String> loadTestCommands() {
        if (this.optionMap.containsKey(TEST_SUITES_FLAG)) {
            String filename = this.optionMap.get(TEST_SUITES_FLAG);
            if (!hasFile(filename)) {
                System.out.println("Error: unable to load tests from config file " + filename);
                System.exit(1);
            }
            try {
                final List<String> testSuiteLines = Files.readAllLines(new File(filename).toPath());
                return testSuiteLines;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // no test suite command file found,
        return Arrays.asList(new String[]{this.optionMap.get(SUT_COMMAND_FLAG)});
    }

    public boolean isCompilingScalaModel(){
        if (optionMap.containsKey(MODBAT_COMPILE)){
            Boolean b = new Boolean(optionMap.get(MODBAT_COMPILE));
            return b;
        }
        return true;
    }

    public final String loadModbatConfig(String modbatModelName, String binDirname, String errDirname){
        if (this.optionMap.containsKey(MODBAT_CONFIG_FLAG)){
            String modbatConfigFilename = this.optionMap.get(MODBAT_CONFIG_FLAG);
            try {
                List<String> configLines = Files.readAllLines(new File(modbatConfigFilename).toPath());
                for (String line : configLines){
                    if (line.contains("--classpath")){
//                        return Arrays.asList(line.split(" "));
                        return line;
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if (verbose) System.out.println("[PONZU] WARNING: could not load modbat config file");
            }
        }
        // default modbat commandline arguments
        String modbatOptStr = String.format("--classpath=./%s %s --log-path=%s --loop-limit=50 -n=%s",
                binDirname,
                modbatModelName,
                errDirname,
                getModbatNumberOfTests());
        if (hasModbatSeed()){
            modbatOptStr += String.format(" -s=%s", getModbatSeed());
        }
        return modbatOptStr;
    }

    public final String loadModbatFile(){
        return null;
    }

    public static boolean hasFile(String filename){
        return new File(filename).exists();
    }

    public static boolean hasFile(String filename, String directory){
        String separator = System.getProperty("file.seperator");
        return new File(directory + separator + filename).exists();
    }

    private static List<String> _parseFullClassname(String name){
        StringBuilder sb = new StringBuilder("");
        String[] testPackageClassname = name.split("\\.");
        String classname = testPackageClassname[testPackageClassname.length-1];
        for(int i = 0; i < testPackageClassname.length-1; i++){
            sb.append(testPackageClassname[i]);
            if (i < testPackageClassname.length-2) sb.append(".");
        }
        return Arrays.asList(new String[]{sb.toString(), classname});
    }

    public static String parseFullClassnameFromCommand(String cmd){
        return cmd.split("\\.")[0];
    }

    public static String parsePackagename(String fullClassname){
        return _parseFullClassname(fullClassname).get(0);
    }

    public static String parseClassname(String fullClassname){
        return _parseFullClassname(fullClassname).get(1);
    }

    public static void main(String[] args){

        System.out.println(parsePackagename("java.util.ArrayList"));
        System.out.println(parseClassname("java.util.ArrayList"));
    }
}
