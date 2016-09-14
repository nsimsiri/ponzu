package TestGenerator;

import java.util.ArrayList;
import java.lang.reflect.Field;

/**
 * Created by NatchaS on 11/18/15.
 *
 * ITestInstance interface
 *
 * Interface allowing the User to create callback functions for the TestGenerator.
 * The main function that runs the test cases is the invokeMethod where a method string is passed and the user must
 * make their appropriate call for the given method name, and randomized set of arguemnts. User must also instantiate the
 * SUT instance.
 *
 * @author Natcha Simsiri
 */

public interface ITestInstance {
    /** User callback for invoking their SUT's instance method - basically does string match and call the appropriate function.
     *  This will be deprecated and replaced with TestGenerator's invokeMethodFromPpt function which uses Java Reflection for SUT method invocation.
     *  @methodName (required) Name of the method to be invoked
     *  @args (requied) TestGenerator's randomized arguments if the method requires any arguments.
     * */
    public Object invokeMethod(String methodName, ArrayList<Object> args);

    /**
     * Returns the 'Class' of the SUT. Will be deprecated and replaced with Class.forName(SUT class string)
     */
    public Class<?> getRuntimeClass();

    /**
     * Returns the SUT instance the user must instantiate in their ITestInstance implementation.
     */
    public Object getTestInstance();
}
