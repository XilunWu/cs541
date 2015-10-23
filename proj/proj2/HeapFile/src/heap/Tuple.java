package heap;

import global.Convert;
import global.GlobalConst;
import global.Minibase;
import global.RID;
import heap.HeapFile;
import heap.HeapScan;
import heap.Tuple;

import java.io.IOException;

import chainexception.ChainException;


public class Tuple {

	public static int max_size = 1024;
	public byte data[];
	private int offset;
	private int length;
	
	public Tuple(){
		data = new byte[max_size];
		offset = 0;
		length = max_size;
	}
	
	public Tuple(byte t_data[], int t_offset, int t_length){
		data = t_data;
		offset = t_offset;
		length = t_length;
	}
	
	public int getLength(){
		return length;
	}
	
	public byte[] getTupleByteArray(){
		return data;
	}
}