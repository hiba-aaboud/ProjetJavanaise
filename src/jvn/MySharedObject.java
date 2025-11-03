package jvn;

public class MySharedObject implements MySharedInterface, java.io.Serializable {

    private static final long serialVersionUID = 1L;

	private String value;
	
    public MySharedObject(String v) { this.value = v; }
    
    @Override
    public String getValue() { return value; }
    
    @Override
    public void setValue(String v) { this.value = v; }
    
    @Override 
    public String toString() {
    	return "Mon Objet Partagé(" + value + ")"; 
    	}
    
 
   
}
