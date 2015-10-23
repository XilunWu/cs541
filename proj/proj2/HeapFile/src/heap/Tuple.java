package heap;

import global.GlobalConst;

import java.lang.*;

/*
 * Check if this definition is correct
 */

public class Tuple {
	public byte data[];
	private int offset;
	private int length;

	public Tuple() {
		data = new byte[GlobalConst.PAGE_SIZE];
		offset = 0;
		length = GlobalConst.PAGE_SIZE;
	}
	
	public void copyData(byte [] t_data, short start, short length)
	{
		for (int i = 0; i < length; i++)
		{
			data[i] = t_data[i + start];
		}
	}

	public Tuple(byte t_data[], int t_offset, int t_length){
		data = t_data;
		offset = t_offset;
		length = t_length;
	}

	public int getLength()
	{
		return length;
	}
	
	public byte [] getTupleByteArray()
	{
		return data;
	}
}