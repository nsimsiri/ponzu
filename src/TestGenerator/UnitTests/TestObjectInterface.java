package TestGenerator.UnitTests;

/**
 * Created by NatchaS on 4/18/16.
 */
public class TestObjectInterface {
    Integer x;
    Comparable I;
    public TestObjectInterface(){
        this.x = 10;
        this.I = this.x;
    }

    public TestObjectInterface(Integer x){
        this.x=x;
        this.I = x;
    }

    public boolean testEq(Comparable K){

        return K.compareTo(this.I)==0;
    }
}
