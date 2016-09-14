package TestGenerator;

import DataTypes.Event2;
import daikon.PptName;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by NatchaS on 2/18/16.
 */
public class InvocationSequence {
    public static void outputTraceGraph(List<Event2> ppts, String outputPath){
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outputPath));
            for(Event2 e : ppts){
                out.write(e.getPptName().getMethodName());
                out.newLine();

            }
            out.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
