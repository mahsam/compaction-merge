import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/*
 * in File Struct, we keep the file information and the header of object
 * (Object Name Length, Object Name, Number of KV Pairs) which we
 * currently processing. The key-value pairs won't be read until they the
 * object has to be written in output file.
 * 
 * Although there are many reads and writes in this approach, it'll solve the
 * issue with large files/objects which don't fit in the buffer pool.
 * 
 * filePointer keeps the position of the byte we just read from the file.
 * 
 * for optimization: we could use the combination of bufferSize and filePointer
 * in order to avoid a lot of reads and writes 
 */
public class FileStruct {
	private int byteSize = 8;
	private FileInputStream fis;
	private RandomAccessFile raf;
	private FileChannel fc;
//	private int bufferSize = 1000;
	
	public int filePointer;
	public BigInteger objNameLen;
	public String objName;
	public BigInteger numKVPairs;
	
	public FileStruct(FileInputStream fis, RandomAccessFile raf) {
		this.fis = fis;
		this.raf = raf;
		this.fc = raf.getChannel();
		this.filePointer = 0;
		getObjectSpecs();
	}
	
	public void getObjectSpecs() {
		// get the object length, object name and number of key-value pairs
		ByteBuffer buf = randomReadBytes(filePointer, byteSize);
		objNameLen = new BigInteger(buf.array());
		buf = randomReadBytes(filePointer, objNameLen.intValue());
		objName = new String(buf.array());
		buf = randomReadBytes(filePointer, byteSize);
		numKVPairs = new BigInteger(buf.array());
	}
	
	public KVPairStruct getKVPair() {
		ByteBuffer buf;
		KVPairStruct kvps = null;
		// read a key-value pair and saves it in KVPairStruct
		try {
			if (filePointer != -1) {
				kvps = new KVPairStruct();
				
				// read a key-value pair for current object
				buf = randomReadBytes(filePointer, byteSize);
				kvps.keyLen = new BigInteger(buf.array());
				buf = randomReadBytes(filePointer, kvps.keyLen.intValue());
				kvps.keyVal = new String(buf.array());
				
				buf = randomReadBytes(filePointer, byteSize);
				kvps.valueLen = new BigInteger(buf.array());
				buf = randomReadBytes(filePointer, kvps.valueLen.intValue());
				kvps.valueVal = new String(buf.array());
			}
			
			if (kvps == null)
				throw new Exception("Error reading key-value pair");
		}
		catch (Exception e) {
			System.out.println("Error reading file" + e);
		}
		return kvps;
	}
	
	public ByteBuffer randomReadBytes(long pos, int len) {
		ByteBuffer buf = ByteBuffer.allocate(len);
		// random access to file based on filePointer
		try {
			if (filePointer != -1) {
				int bytesRead = fc.read(buf);
				if (bytesRead == -1)
					filePointer = -1;
				else if (bytesRead != len)
					throw new Exception("Error reading file");
				else
					filePointer += len;
			}
		}
		catch(Exception e){
			System.out.println("Error reading file" + e);
		}
		return buf;
	}
	
	public void writeObject(FileOutputStream fos) {
		// write the object and its key-value pairs to the output file
		try {
			fos.write(convertBigInt(objNameLen));
			fos.write(objName.getBytes());
			fos.write(convertBigInt(numKVPairs));
			
			int intNumKVP = numKVPairs.intValue();
			while (intNumKVP > 0){
				// write a key-value pair to the output file
				writeKeyValuePair(fos, getKVPair());
				intNumKVP--;
			}
		}
		catch (Exception e){
			System.out.println("Error writing file" + e);
			try {
				if (fos != null)
					fos.close();
			}
			catch(Exception ex){
			
			}
		}
	}
	public void writeKeyValuePair(FileOutputStream fos, KVPairStruct kvps) {
		// write a key-value pair to the output file
		try {
			fos.write(FileStruct.convertBigInt(kvps.keyLen));
			fos.write(kvps.keyVal.getBytes());
			fos.write(FileStruct.convertBigInt(kvps.valueLen));
			fos.write(kvps.valueVal.getBytes());
		}
		catch (Exception e){
			System.out.println("Error writing file" + e);
			try {
				if (fos != null)
					fos.close();
			}
			catch(Exception ex){
			
			}
		}
	}
	
	public ArrayList<KVPairStruct> readKeyValuePairs() {
		// get all the key-value pairs for current object
		// this list is used to resolve conflicts
		ArrayList<KVPairStruct> kvplist = new ArrayList<KVPairStruct>();
	
		try {
			int intNumKVP = numKVPairs.intValue();
			
			while (intNumKVP > 0){
				kvplist.add(getKVPair());
				intNumKVP--;
			}
			
			if (kvplist.size() != numKVPairs.intValue())
				throw new Exception("mismatch in number of key-value pairs");
		}
		catch (Exception e){
			System.out.println("Error reading file" + e);
		}
		return kvplist;
	}
	
	
	public static byte[] convertBigInt(BigInteger bi) {
		// convert the big integer to bytes in order to write in the file
		long b = bi.longValue();
		byte[] bytes = ByteBuffer.allocate(8).putLong(b).array();
		return bytes;
	}
	
	public void finalizeStreams() {
		// make sure all the streams are closed
		try{
			fis.close();
			raf.close();
			fc.close();
		}
		catch (Exception e){
			System.out.println("Error closing file" + e);
		}
		
	}
	
	public static void sortFS(ArrayList<FileStruct> fslist) {
		// sort a list of FileStructs
    	Collections.sort(fslist, new Comparator<FileStruct>() {
    	    public int compare(FileStruct fs1, FileStruct fs2) {
    	        return fs1.objName.compareTo(fs2.objName);
    	    }
    	});
    }
}