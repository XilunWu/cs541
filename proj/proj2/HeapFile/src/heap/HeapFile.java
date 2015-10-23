package heap;

import global.Convert;
import global.GlobalConst;
import global.Minibase;
import global.RID;
import global.PageId;
import global.Page;

import java.io.*;
import java.util.*;
import java.lang.*;

import chainexception.ChainException;

public class HeapFile {
	private PageId firstpageid;
	private String filename;
	private TreeMap<Short, List<PageId>> freespace = new TreeMap<Short, List<PageId>>();
	private int cnt = 0;

	public PageId headerpageid(){
		return firstpageid;
	}

	public HeapFile() {}
	/*
	 * If the given name already denotes a file, this opens it; 
	 * otherwise, this creates a new empty file.
	 */
	public HeapFile(String name) 
	 throws ChainException, 
	 IOException {
	 	//don't forget to check name!!!
	 	//use DiskManager.add_file_entry()
	 	if(name != null){				
	 		filename = name;
			firstpageid = Minibase.DiskManager.get_file_entry(name);
			if(firstpageid == null){		//file does not exist
				HFPage firstpage = new HFPage();
				/* create the first header page */
				firstpageid = Minibase.BufferManager.newPage(firstpage, 1);
				Minibase.DiskManager.add_file_entry(name, firstpageid);
				/*	set page's info	*/
				firstpage.setCurPage(firstpageid);
				/*	create the first data page */
				HFPage datapage = new HFPage();
				PageId datapageid = Minibase.BufferManager.newPage(datapage, 1);
				datapage.setPrevPage(firstpageid);
				datapage.setCurPage(datapageid);
				firstpage.setNextPage(datapageid);
				//set index and insert entry
				List<PageId> pagelist = new ArrayList<PageId>();
				pagelist.add(datapageid);
				freespace.put(new Short(datapage.getFreeSpace()), pagelist);
				Minibase.BufferManager.unpinPage(datapageid, true);
				Minibase.BufferManager.unpinPage(firstpageid, true);
				//Lazy: don't write info into header page. Because it is of no use here
			}
		}
	}

	public RID insertRecord(byte[] record) 
	 throws ChainException, 
	 IOException {
	 	RID rid;
		Map.Entry<Short, List<PageId>> entry = freespace.ceilingEntry(new Short((short)record.length));
		if (entry == null) { 	//no page is large enough to hold record. 
								//allocate new data page and append it to tail,
								//also update treemap
			//get the header page
			HFPage firstpage = new HFPage();
			Minibase.BufferManager.pinPage(firstpageid, firstpage, false);
			//create a new page		
			HFPage datapage = new HFPage();
			PageId datapageid = Minibase.BufferManager.newPage(datapage, 1);
			datapage.setPrevPage(firstpageid);
			datapage.setCurPage(datapageid);
			datapage.setNextPage(firstpage.getNextPage());
			firstpage.setNextPage(datapageid);
			Minibase.BufferManager.unpinPage(firstpageid, true);
			if (datapage.getNextPage().pid >= 0) {
				HFPage yetanotherpage = new HFPage();
				Minibase.BufferManager.pinPage(datapage.getNextPage(), yetanotherpage, false);
				yetanotherpage.setPrevPage(datapageid);
				Minibase.BufferManager.unpinPage(datapage.getNextPage(), true);
			}
			//insert the record
			rid = datapage.insertRecord(record);
			//update treemap and unpin data page
			Short freesize = new Short(datapage.getFreeSpace());
			Minibase.BufferManager.unpinPage(datapageid, true);
			List<PageId> pagelist = freespace.get(freesize);
			if (pagelist == null) {
					pagelist = new ArrayList<PageId>();
					pagelist.add(datapageid);
					freespace.put(new Short(datapage.getFreeSpace()), pagelist);
			} else {
				pagelist.add(datapageid);
			}
			Minibase.BufferManager.unpinPage(datapageid, true);
		} else {	//found a suitable page
			HFPage datapage = new HFPage();
			List<PageId> pagelist = entry.getValue();
			Short freesize = entry.getKey();
			//always choose the 1st one
			PageId pageid = pagelist.get(0);
			Minibase.BufferManager.pinPage(pageid, datapage, false);
			rid = datapage.insertRecord(record);
			pagelist.remove(pageid);
			if (pagelist.isEmpty())
				freespace.remove(freesize);
			//update treemap
			if (datapage.getFreeSpace() != 0){
				pagelist = freespace.get(new Short(datapage.getFreeSpace()));
				if (pagelist == null) {
					pagelist = new ArrayList<PageId>();
					pagelist.add(pageid);
					freespace.put(new Short(datapage.getFreeSpace()), pagelist);
				} else {
					pagelist.add(pageid);
				}
			}
			Minibase.BufferManager.unpinPage(pageid, true);
		}
		++cnt;
		return rid;
	}

	public Tuple getRecord(RID rid)
	 throws ChainException, 
	 IOException {
		PageId pageid = rid.pageno;
		HFPage datapage = new HFPage();
		Minibase.BufferManager.pinPage(pageid, datapage, false);
		Tuple tuple = new Tuple();
		short length = datapage.getSlotLength(rid.slotno);
		byte [] record = datapage.selectRecord(rid);
		if (record == null) {
			Minibase.BufferManager.unpinPage(pageid, false);
			throw new ChainException(null, "HEAPFILE: NO_SUCH_RECORD.");
		}
		tuple.copyData(record, (short)0, length);
		Minibase.BufferManager.unpinPage(pageid, false);
		return tuple;
	}

	public boolean deleteRecord(RID rid)
	 throws ChainException, 
	 IOException {
	 	PageId pageid = rid.pageno;
		HFPage datapage = new HFPage();
		Minibase.BufferManager.pinPage(pageid, datapage, false);
		//check if this record exists
		if (datapage.selectRecord(rid) == null) {
			Minibase.BufferManager.unpinPage(pageid, true);
			throw new InvalidUpdateException(null, "HEAPFILE: DELETE_ERROR_NO_RECORD.");
		}
		//remove from treemap
		Short freesize = new Short(datapage.getFreeSpace());
		List<PageId> pagelist = freespace.get(freesize);
		pagelist.remove(pageid);
		if (pagelist.isEmpty())
			freespace.remove(freesize);
		//delete the record from hfpage
		datapage.deleteRecord(rid);
		//update treemap and unpin data page
		freesize = new Short(datapage.getFreeSpace());
		Minibase.BufferManager.unpinPage(pageid, true);
		pagelist = freespace.get(freesize);
		if (pagelist == null) {
			pagelist = new ArrayList<PageId>();
			pagelist.add(pageid);
			freespace.put(new Short(datapage.getFreeSpace()), pagelist);
		} else {
			pagelist.add(pageid);
		}
		--cnt;
		return true;
	 }

	public boolean updateRecord(RID rid, Tuple newRecord) 
	 throws ChainException, 
	 IOException {
	 	//This can be modified to:
	 	//firstly see if any space to hold newRecord
	 	//if yes, update
	 	//if no, delete & insert
	 	deleteRecord(rid);
	 	rid.copyRID(insertRecord(newRecord.getTupleByteArray()));
	 	return true;
	 }

	public int getRecCnt() { //get number of records in the file
		return cnt;
	}

	public HeapScan openScan() {
		HeapScan hscan = new HeapScan();
		hscan.hs_init(firstpageid);
		return hscan;
	}
}

