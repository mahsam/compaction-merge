import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class KVPairStruct {
	public BigInteger keyLen;
	public String keyVal;
	public BigInteger valueLen;
	public String valueVal;
	
	public static void sortKVPS(ArrayList<KVPairStruct> kvpslist) {
		// sort the key-value pair list based on their keys
		Collections.sort(kvpslist, new Comparator<KVPairStruct>() {
    	    public int compare(KVPairStruct kvps1, KVPairStruct kvps2) {
    	        return kvps1.keyVal.compareTo(kvps2.keyVal);
    	    }
    	});
    }   
}
