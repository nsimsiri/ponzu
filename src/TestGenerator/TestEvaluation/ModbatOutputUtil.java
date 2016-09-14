package TestGenerator.TestEvaluation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by NatchaS on 4/9/16.
 */
public class ModbatOutputUtil {
    private static final String OUTPUT_DIR_CMD = "--log-path=";

    public static void main(String[] args){

            String cwd = Paths.get("").toAbsolutePath().toString();
            System.out.println(cwd);
        try {
            File modbatLog = new File("./modbat-log.txt");
//            String modbatRunCmd = "scala -cp ../../bin/ponzu.jar:../../lib/codemodel-2.6.jar:../../lib/daikon.jar:../../lib/gson-2.5.jar:../../lib/java-getopt.jar:../../lib/openmodbat-3.0.jar:. /Users/NatchaS/Documents/workspace/Research/SoftwareEng/mts_shared/mtsinference/inference-comparison/lib/openmodbat-3.0.jar";
            String modbatRunCmd = String.format("scala -cp ../../bin/ponzu.jar:../../lib/codemodel-2.6.jar:../../lib/daikon.jar:../../lib/gson-2.5.jar:../../lib/java-getopt.jar:../../lib/openmodbat-3.0.jar:. %s/lib/openmodbat-3.0.jar -h", cwd);
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
}
