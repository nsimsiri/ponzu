package DataTypes;
import daikon.PptName;
//import daikon.inv.Invariants;

import java.util.LinkedHashSet;


/**
 * Created by NatchaS on 10/26/15.
 */

/*
* Inherits MTS_transition for invariant annotations on the edges of the MTS.
* Invariants are stored in the Even2 datatype.
* */
public class MTS_inv_transition extends MTS_transition {
    private Event2 eventObj;
    public MTS_inv_transition(PptName pptName, int start, int end, String type, Event2 eventObj){
        super(pptName,start,end,type);
        if(!eventObj.getName().equals(this.getEvent())){
            System.out.println("Mismatching events. Something is wrong with parsing.");
            System.exit(1);
        }
        this.eventObj = eventObj;
    }

    public MTS_inv_transition(String event, int start, int end, String type, Event2 eventObj){
        super(event,start,end,type);
        if(!eventObj.getName().equals(this.getEvent())){
            System.out.println("Mismatching events. Something is wrong with parsing.");
            System.exit(1);
        }
        this.eventObj = eventObj;
    }
    public static MTS_inv_transition MTS_transitionToInvAnnotated(MTS_transition trans, Event2 eventObj){
        if (trans.getName()==null){
            return new MTS_inv_transition(trans.getEvent(), trans.getStart(), trans.getEnd(), trans.getType(), eventObj);
        } else {
            return new MTS_inv_transition(trans.getName(), trans.getStart(), trans.getEnd(), trans.getType(), eventObj);
        }
    }

    public Event2 getEventObject(){
        return this.eventObj;
    }

    public void setEventObject(Event2 eventObj){
        if (!eventObj.getName().equals(this.getEvent())){
            System.out.println("Mismatching events.");
            return;
        }
        this.eventObj=eventObj;
    }
}
