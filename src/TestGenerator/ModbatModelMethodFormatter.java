package TestGenerator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Created by NatchaS on 10/22/16.
 * TODO: comeback to solve the case of private methods being in SEKT model.
 */
public class ModbatModelMethodFormatter {
    // Use Reflection on Object to help execute method.
    private Class instanceType;
    private String instanceName;
    public ModbatModelMethodFormatter(String instanceName, Class instanceType){
        this.instanceName = instanceName;
        this.instanceType = instanceType;
    }


    public void formatMethod(String methodName, String methodSignatureArrayName, List<String> methodSignatures, List<String> argVariables, List<String> argCreationStatements){
        Method reflectedMethod = null;
        boolean isReflectedMethodPrivate = false;
        boolean hasMethodSignatureName = methodSignatureArrayName != null;
        try {
            Class[] classList = new Class[methodSignatures.size()];
            for(int i = 0; i < methodSignatures.size(); i++) {
                String typeName = methodSignatures.get(i);
                classList[i] = ModbatModelUtilities.primitiveToClass.getOrDefault(typeName, Class.forName(typeName));
            }
            reflectedMethod = instanceType.getDeclaredMethod(methodName, classList);
            isReflectedMethodPrivate = Modifier.isPrivate(reflectedMethod.getModifiers());
        } catch (NoSuchMethodException e){
            e.printStackTrace();
        } catch (ClassNotFoundException e){
            e.printStackTrace();
        }


        String methodLine = "";
        if (isReflectedMethodPrivate){
            /*
            StringBuffer signatureCreationBuffer = new StringBuffer("Array[String](");
            if (!hasMethodSignatureName){
                for(int i = 0; i < methodSignatures.size(); i++){
                    signatureCreationBuffer.append("\""+ methodSignatures.get(i) + "\"");
                    if(i!=methodSignatures.size()-1) {
                        signatureCreationBuffer.append(", ");
                    }
                    signatureCreationBuffer.append(")");
                }
            }

            methodLine = String.format("ModbatModelUtilities.executeMethod(%s, \"%s\", %s, %s)",
                    this.instanceName,
                    methodName,
                    (hasMethodSignatureName) ? methodSignatureArrayName : signatureCreationBuffer.toString(),
                    ModbatArgumentFormatter.argListToString(argVariables));
            */
            return;
        } else {
            methodLine = String.format("%s.%s(%s)",
                    this.instanceName,
                    methodName,
                    ModbatArgumentFormatter.argListToString(argVariables));
        }
        argCreationStatements.add(methodLine);
    }
}
