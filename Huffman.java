
import java.io.*;
import java.util.ArrayList;

import javax.swing.plaf.multi.MultiTableHeaderUI;

import net.datastructures.*;
import java.util.concurrent.*;

/**
 * Class Huffman that provides huffman compression encoding and decoding of files
 * @author Lucia Moura 2021
 *
 */

public class Huffman {

	/**
	 * 
	 * Inner class Huffman Node to Store a node of Huffman Tree
	 *
	 */
	private class HuffmanTreeNode { 
	    private int character;      // character being represented by this node (applicable to leaves)
	    private int count;          // frequency for the subtree rooted at node
	    private HuffmanTreeNode left;  // left/0  subtree (NULL if empty)
	    private HuffmanTreeNode right; // right/1 subtree subtree (NULL if empty)
	    public HuffmanTreeNode(int c, int ct, HuffmanTreeNode leftNode, HuffmanTreeNode rightNode) {
	    	character = c;
	    	count = ct;
	    	left = leftNode;
	    	right = rightNode;
	    }

	    public int getChar() { return character;}
	    public Integer getCount() { return count; }
	    public HuffmanTreeNode getLeft() { return left;}
	    public HuffmanTreeNode getRight() { return right;}
		public boolean isLeaf() { return left==null ; } // since huffman tree is full; if leaf=null so must be right
	}
	
	/**
	 * 
	 * Auxiliary class to write bits to an OutputStream
	 * Since files output one byte at a time, a buffer is used to group each output of 8-bits
	 * Method close should be invoked to flush half filed buckets by padding extra 0's
	 */
	private class OutBitStream {
		OutputStream out;
		int buffer;
		int buffCount;
		int byteCounter;
		public OutBitStream(OutputStream output) { // associates this to an OutputStream
			out = output;
			buffer=0;
			buffCount=0;
			byteCounter = 0;
		}
		public void writeBit(int i) throws IOException { // write one bit to Output Stream (using byte buffer)
		    buffer=buffer<<1;
		    buffer=buffer+i;
		    buffCount++;
		    if (buffCount==8) { 
		    	out.write(buffer); 
		    	//System.out.println("buffer="+byteCounter);
				byteCounter++;
		    	buffCount=0;
		    	buffer=0;
		    }
		}

		public String toString() {
			return ""+byteCounter;//+buffCount;
		}
		
		public void close() throws IOException { // close output file, flushing half filled byte
			if (buffCount>0) { //flush the remaining bits by padding 0's
				buffer=buffer<<(8-buffCount);
				out.write(buffer);
			}
			out.close();
		}
		
 	}
	
	/**
	 * 
	 * Auxiliary class to read bits from a file
	 * Since we must read one byte at a time, a buffer is used to group each input of 8-bits
	 * 
	 */
	private class InBitStream {
		InputStream in;
		int buffer;    // stores a byte read from input stream
		int buffCount; // number of bits already read from buffer
		public InBitStream(InputStream input) { // associates this to an input stream
			in = input;
			buffer=0; 
			buffCount=8;
		}
		public int readBit() throws IOException { // read one bit to Output Stream (using byte buffer)
			if (buffCount==8) { // current buffer has already been read must bring next byte
				buffCount=0;
				buffer=in.read(); // read next byte
				if (buffer==-1) return -1; // indicates stream ended
			}
			int aux=128>>buffCount; // shifts 1000000 buffcount times so aux has a 1 is in position of bit to read
			//System.out.println("aux="+aux+"buffer="+buffer);
			buffCount++;
			if ((aux&buffer)>0) return 1; // this checks whether bit buffcount of buffer is 1
			else return 0;
			
		}

	}
	
	/**
	 * Builds a frequency table indicating the frequency of each character/byte in the input stream
	 * @param input is a file where to get the frequency of each character/byte
	 * @return freqTable a frequency table must be an ArrayList<Integer? such that freqTable.get(i) = number of times character i appears in file 
	 *                   and such that freqTable.get(256) = 1 (adding special character representing"end-of-file")
	 * @throws IOException indicating errors reading input stream
	 */
	//change to private
	
	public ArrayList<Integer> buildFrequencyTable(InputStream input) throws IOException{
		ArrayList<Integer> freqTable = new ArrayList<>(257); // declare frequency table
		for (int i=0; i<257;i++) freqTable.add(i,0); // initialize frequency values with 0

		int i;
		while ((i = input.read()) != -1) {
			System.out.println("Putting: "+i); 
			System.out.println(Character.toString(i));
			freqTable.set(i,freqTable.get(i)+1);
		}
		freqTable.set(256,1);

		return freqTable;
	}

	/**
	 * Create Huffman tree using the given frequency table; the method requires a heap priority queue to run in O(nlogn) where n is the characters with nonzero frequency
	 * @param freqTable the frequency table for characters 0..255 plus 256 = "end-of-file" with same specs are return value of buildFrequencyTable
	 * @return root of the Huffman tree build by this method
	 */
	public HuffmanTreeNode buildEncodingTree(ArrayList<Integer> freqTable) {

		HeapPriorityQueue<Integer, HuffmanTreeNode> priorityQueue;
		priorityQueue = new HeapPriorityQueue<>();
		
		for (int i = 0; i<freqTable.size(); i++) { 
			if (freqTable.get(i) > 0) {
				HuffmanTreeNode huffMan = new HuffmanTreeNode((char)i, freqTable.get(i), null, null);
				priorityQueue.insert(huffMan.getCount(), new HuffmanTreeNode((char)i, freqTable.get(i), null, null));
			}
		}

		while (priorityQueue.size() > 1) {
			Entry<Integer, HuffmanTreeNode> e1 = priorityQueue.removeMin();
			Entry<Integer, HuffmanTreeNode> e2 = priorityQueue.removeMin();
			HuffmanTreeNode root = new HuffmanTreeNode('\0', e1.getKey() + e2.getKey(), e1.getValue(), e2.getValue());

			priorityQueue.insert(e1.getKey()+e2.getKey(), root);
		}
		
	   return priorityQueue.removeMin().getValue();
	}
	
	
	/**
	 * 
	 * @param encodingTreeRoot - input parameter storing the root of the HUffman tree
	 * @return an ArrayList<String> of length 257 where code.get(i) returns a String of 0-1 correspoding to each character in a Huffman tree
	 *                                                  code.get(i) returns null if i is not a leaf of the Huffman tree
	 */
	private ArrayList<String> buildEncodingTable(HuffmanTreeNode encodingTreeRoot) {
		ArrayList<String> code = new ArrayList<>(257);
		for (int i=0;i<257;i++) code.add(i, null);
		
		String result = "";

		traverseRecursive(encodingTreeRoot, result, code);
		return code;
	}

	private void traverseRecursive(HuffmanTreeNode node, String result, ArrayList<String> table) {
		if (!(node.isLeaf())) {
			traverseRecursive(node.getLeft(), result+"0", table);

			traverseRecursive(node.getRight(), result+"1", table);
		} else {
			table.set(node.getChar(), result);
		}
	}
	
	/**
	 * Encodes an input using encoding Table that stores the Huffman code for each character
	 * @param input - input parameter, a file to be encoded using Huffman encoding
	 * @param encodingTable - input parameter, a table containing the Huffman code for each character
	 * @param output - output paramter - file where the encoded bits will be written to.
	 * @throws IOException indicates I/O errors for input/output streams
	 */
	private void encodeData(InputStream input, ArrayList<String> encodingTable, OutputStream output) throws IOException {
        OutBitStream bitStream = new OutBitStream(output); // uses bitStream to output bit by bit

        StringBuilder sb = new StringBuilder();
        int i;
		int count = 0;
        while ((i = input.read()) != -1) {
			sb.append(encodingTable.get(i));
			count++;
        }

		sb.append(encodingTable.get(256));
		String[] allBits = sb.toString().split("");

			for (int j = 0; j<allBits.length; j++) {
				bitStream.writeBit(Integer.parseInt(allBits[j]));
			}
        
        bitStream.close(); 
		System.out.println("The count of bits is: "+count);
        System.out.println("Number of bytes in output: "+ (int) Math.ceil((sb.length())/8.0));
    }


	
	/**
	 * Decodes an encoded input using encoding tree, writing decoded file to output
	 * @param input  input parameter a stream where header has already been read from
	 * @param encodingTreeRoot input parameter contains the root of the Huffman tree
	 * @param output output parameter where the decoded bytes will be written to 
	 * @throws IOException indicates I/O errors for input/output streams
	 */
	private void decodeData(ObjectInputStream input, HuffmanTreeNode encodingTreeRoot, FileOutputStream output) throws IOException {
		
		InBitStream inputBitStream = new InBitStream(input); // associates a bit stream to read bits from file

		HuffmanTreeNode current = encodingTreeRoot;
		int count = 0;
		int countReadBits = 0;
		
		while (true) {

			if (current.isLeaf()) {

				if (current.getChar() == 256) {
					break;
				} else {
					output.write(current.getChar());
					count++;
					current = encodingTreeRoot;
				}

			} else {
				current = (inputBitStream.readBit() == 1) ? current.getRight() : current.getLeft();
				countReadBits++;
			}
		}

	 	System.out.println("Number of bytes in input: "+ (int)Math.ceil(countReadBits/8.0));
	 	System.out.println("Number of bytes in output: "+count);
    }
	
	/**
	 * Method that implements Huffman encoding on plain input into encoded output
	 * @param inputFileName - this is the file to be encoded (compressed)
	 * @param outputFileName - this is the Huffman encoded file corresponding to input
	 * @throws IOException indicates problems with input/output streams
	 */
	public void encode(String inputFileName, String outputFileName) throws IOException {
		System.out.println("\nEncoding "+inputFileName+ " " + outputFileName);
		
		// prepare input and output files streams
		FileInputStream input = new FileInputStream(inputFileName);
		FileInputStream copyInput = new FileInputStream(inputFileName); // create copy to read input twice
		FileOutputStream out = new FileOutputStream(outputFileName);
 		ObjectOutputStream codedOutput= new ObjectOutputStream(out); // use ObjectOutputStream to print objects to file

		ArrayList<Integer> freqTable= buildFrequencyTable(input); // build frequencies from input
		HuffmanTreeNode root= buildEncodingTree(freqTable); // build tree using frequencies
		ArrayList<String> codes= buildEncodingTable(root);  // buildcodes for each character in file
		codedOutput.writeObject(freqTable); //write header with frequency table
		encodeData(copyInput,codes,codedOutput); // write the Huffman encoding of each character in file

	}
	
    /**
     * Method that implements Huffman decoding on encoded input into a plain output
     * @param inputFileName  - this is an file encoded (compressed) via the encode algorithm of this class
     * @param outputFileName      - this is the output where we must write the decoded file  (should original encoded file)
     * @throws IOException - indicates problems with input/output streams
     * @throws ClassNotFoundException - handles case where the file does not contain correct object at header
     */
	public void decode (String inputFileName, String outputFileName) throws IOException, ClassNotFoundException {
		System.out.println("\nDecoding "+inputFileName+ " " + outputFileName);
		// prepare input and output file streams
		FileInputStream in = new FileInputStream(inputFileName);
 		ObjectInputStream codedInput= new ObjectInputStream(in);
 		FileOutputStream output = new FileOutputStream(outputFileName);
 		
		ArrayList<Integer> freqTable = (ArrayList<Integer>) codedInput.readObject(); //read header with frequency table
		//System.out.println("FrequencyTable is from"+inputFileName+freqTable);
		HuffmanTreeNode root= buildEncodingTree(freqTable);
		decodeData(codedInput, root, output);
	}

	public static void main(String[] args) throws IOException{

		try {
		Huffman myHuff = new Huffman();
		ArrayList<Integer> table = myHuff.buildFrequencyTable(new FileInputStream("ACTGhandout.txt"));
		System.out.println(table);
		// long startTimeEnode = System.nanoTime();
  //           myHuff.encode("large_2.txt", "large.huf");
  //           long endTimeEcode = System.nanoTime();
  //           System.out.println("Encoding took "+ TimeUnit.NANOSECONDS.toSeconds(endTimeEcode - startTimeEnode));
  //           System.out.println("-------------------------------------------------------------------------");



  //           long startTimeDecode = System.nanoTime();
  //           myHuff.decode("large.huf", "largeRecovered.txt");
  //           long endTimeDecode  = System.nanoTime();
           
  //           System.out.println("Decoding took "+TimeUnit.NANOSECONDS.toSeconds(endTimeDecode - startTimeDecode));
        } catch (IOException e) {

        }

	}

}
