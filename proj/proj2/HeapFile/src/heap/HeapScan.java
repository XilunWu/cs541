package heap;

import global.Convert;
import global.GlobalConst;
import global.Minibase;
import global.RID;
import global.*;
import heap.HeapFile;
import heap.HeapScan;
import heap.Tuple;

import java.io.IOException;

import chainexception.ChainException;


public class HeapScan
{
	public PageId firstpage;
	
	private HeapFile hfile;
	private RID ridptr;
	
	private HFPage first_hf;
	private HFPage curr_hf;
	private HFPage next_hf;
	private PageId first_pid;
	private PageId curr_pid;
	private PageId next_pid;
	
	
	protected HeapScan(HeapFile hf){
	
		hfile = hf;
		hs_init(hf.headerpageid());
	}

	public void hs_init(PageId pid){
// System.out.println("hs_init:pid:"+pid.pid);
		first_pid = pid;
		curr_hf = new HFPage();
		first_hf = new HFPage();
		Minibase.BufferManager.pinPage(pid, first_hf, false);
		next_pid = first_hf.getNextPage();
		curr_pid = next_pid;
// System.out.println("curr_pid:"+curr_pid.pid);
//		Minibase.BufferManager.unpinPage(pid, false);
		Minibase.BufferManager.pinPage(curr_pid, curr_hf, false);
		ridptr = curr_hf.firstRecord();
		Minibase.BufferManager.unpinPage(curr_pid, true);
// System.out.println("rid_pid:"+ridptr.pageno.pid);

	}
	
	//added
	public HeapScan(){
	}
	
	protected void finalize() throws Throwable {
		try {
			this.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		
	}
	
	public void close() throws ChainException{
		ridptr = null;
		first_hf = null;
		curr_hf = null;
		next_hf = null;
		first_pid = null;
		curr_pid = null;
		next_pid = null;
		firstpage = null;
	
	}
	
	public boolean hasNext(){
		boolean flag;
		HFPage hfpagetmp = new HFPage();
		if(ridptr == null){
			Minibase.BufferManager.unpinPage(curr_pid, false);
			flag = false;
		}
		if(curr_hf.hasNext(ridptr)){ 
			flag = true;
			ridptr = curr_hf.nextRecord(ridptr);
			if(ridptr == null){
				PageId hftmp = curr_hf.getNextPage();
				if(hftmp.pid != -1) {
					Minibase.BufferManager.unpinPage(curr_pid, false);
					Minibase.BufferManager.pinPage(hftmp, curr_hf, false);
					curr_pid = hftmp;

					ridptr = curr_hf.firstRecord();
				}
			}
		}
		else {
			flag = false;
			Minibase.BufferManager.unpinPage(curr_pid, false);
		}


		return flag;
	}
	
	int i=0;
	public Tuple getNext(RID rid){
		Tuple tuple;
		byte bytearray[];
		HFPage hfpagetmp = new HFPage();
		Minibase.BufferManager.pinPage(curr_pid, curr_hf, false);
		if(ridptr == null){
// System.out.println("%%%%%%%%%%%%%%%");
			Minibase.BufferManager.unpinPage(curr_pid, false);
			Minibase.BufferManager.unpinPage(first_pid, true);
			return null;
		}

		rid.slotno = ridptr.slotno;
		rid.pageno = ridptr.pageno;
// System.out.println("0 -- getNext:rid.pageno.pid:"+rid.pageno.pid);
		short length = curr_hf.getSlotLength(ridptr.slotno);
// System.out.println((i++)+"	getNext:curr_pid:"+curr_pid+"	getNext:length:"+length+"	slotno:"+ridptr.slotno);
		tuple = new Tuple();
		tuple.copyData(curr_hf.selectRecord(ridptr), (short)0, length);
		ridptr = curr_hf.nextRecord(ridptr);
		if(ridptr == null){

			PageId hftmp = curr_hf.getNextPage();
// System.out.println("hftmp:"+hftmp.pid);
			if(hftmp.pid != -1){
				Minibase.BufferManager.unpinPage(curr_pid, false);
	
				Minibase.BufferManager.pinPage(hftmp, curr_hf, false);
				curr_pid = hftmp;

				ridptr = curr_hf.firstRecord();
			}
		}
		
		Minibase.BufferManager.unpinPage(curr_pid, true);
		return tuple;
		
	}
	
}
	
	
