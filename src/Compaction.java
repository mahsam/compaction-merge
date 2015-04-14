import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.ArrayList;


/*
 * Compaction class implements only the first extra credit part.
 * here's my approach to implement a search function:
 * reading the file to find an object name
 * is brute-force algorithm. Instead, an index file can be created, the index
 * key is object name and the value is the position of object in the merged
 * file. The index file is also sorted on the keys (object names). To find a
 * name for instance "henwen", we look it up in index file and get the
 * position. Then in the merged file, we seek directly to this position to read
 * "henwen" object. There's a disadvantage to this approach. Updating the
 * merged file causes an update in index file as well.
 */
public class Compaction {

	/*
	 * args contains all the file paths that have to be merged together
	 */
    public static void main(String[] args) {
    	File file = null;
    	FileInputStream fis = null;
    	FileStruct fs = null;
    	ArrayList<FileStruct> fslist = new ArrayList<FileStruct>();
    	
    	// we create a FileStruct object for each file which allows to keep
    	// track of (data structure) objects in them
    	// then we add them the FileStruct object to a list for further process

    	for (int i = 0; i < args.length; i++) {
    		if (!args[i].isEmpty() && args[i] != null) {
    			file = new File(args[i]);
    			
				try {
					fis = new FileInputStream(file);
					RandomAccessFile raf = new RandomAccessFile(args[i], "r");
					fs = new FileStruct(fis, raf);
    				fslist.add(fs);
				}
    		    catch (FileNotFoundException e) {
    		        System.out.println("File not found" + e);
    		    }
    		    finally {
    		        try {
    		            if (fis != null) {
    		                fis.close();
    		            }
    		        }
    		        catch (IOException ioe) {
    		            System.out.println("Error while closing stream: " + ioe);
    		        }
    		    }
    		}
    	}
    	
    	// call merge function for FileStruct list
    	Compaction.merge(fslist);
    }
    
    public static void merge(ArrayList<FileStruct> fslist) {
    	FileOutputStream fos = null;
    	try {
    		File file = new File("./test/output");
	    	fos = new FileOutputStream(file);

	    	FileStruct fs = null;
	    	int numConflicts = 0;
	    	while (fslist.size() != 0) {
	    		FileStruct.sortFS(fslist);
	    		numConflicts = resolveConflicts(fslist, fos);
	    		
	    		if (numConflicts == 0){
		    		fs = fslist.get(0);
		    		if (fs.filePointer != -1){
		    			fs.writeObject(fos);
		    			fs.getObjectSpecs();
		    		}
		    		else {
		    			fs.finalizeStreams();
		    			fslist.remove(0);
		    		}	
	    		}
	    		else {
	    			for (int i = 0; i < numConflicts; i++) {
	    	    		fs = fslist.get(i);
	    	    		fs.getObjectSpecs();
	    	    		if (fs.filePointer == -1) {
	    	    			fs.finalizeStreams();
			    			fslist.remove(i);
	    	    		}
	    	    	}
	    		}
	    	}
    	}
    	catch(Exception e){
    		
    	}
    	finally {
    		try {
    			if (fos != null)
    				fos.close();
    		}
    		catch(Exception e){
    		
    		}
    	}
    }
    
    public static int resolveConflicts(ArrayList<FileStruct> fslist, FileOutputStream fos) {
    	if (fslist.size() < 2)
    		return 0;
    	
    	ArrayList<KVPairStruct> allkvps = new ArrayList<KVPairStruct>();
    	// counter is the number of elements in the list that have the same Object Name
    	int counter = 1;
    	
    	FileStruct fs = fslist.get(0);
    	String objname = fs.objName;
    	
    	// since the list is sorted, conflicts can be detected in the first elements of the list
    	for (int i = 1; i < fslist.size(); i++) {
    		fs = fslist.get(i);
    		if (!fs.objName.equals(objname))
				break;

    		allkvps.addAll(fs.readKeyValuePairs());
    		counter++;
    	}
    	
    	// when the conflict occurs
    	if (counter > 1) {
    		// get the very first element key-value pair
    		allkvps.addAll(fslist.get(0).readKeyValuePairs());
    		
    		// sort all the keys
    		// NOTE: I assumed there's no conflicts for keys
	    	KVPairStruct.sortKVPS(allkvps);
	    	
	    	try {
	    		// write the merged object into the output file
		    	fos.write(FileStruct.convertBigInt(BigInteger.valueOf(objname.length())));
				fos.write(objname.getBytes());
				fos.write(FileStruct.convertBigInt(BigInteger.valueOf(allkvps.size())));
				//write all the key-value pairs in the file
				for(KVPairStruct kvps : allkvps) {
					fs.writeKeyValuePair(fos, kvps);
				}
		    }
			catch(Exception e){
				try {
					if (fos != null)
						fos.close();
				}
				catch(Exception ex){
				
				}
			}
	    	return counter;
    	}
    	else
    		return 0;
    }
}
