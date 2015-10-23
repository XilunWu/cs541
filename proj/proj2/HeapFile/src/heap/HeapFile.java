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

public class HeapFile implements GlobalConst{
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
	 	//Actually, as the treemap is maintained in mem,
	 	//we must hold one HeapFile object for every call of HeapFile(name)
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
	 	if(record.length > MAX_TUPSIZE) {
			throw new SpaceNotAvailableException("Space not available");
		}
	 	RID rid = null;
		Map.Entry<Short, List<PageId>> entry = freespace.ceilingEntry(new Short((short)(record.length + 4)));
		if (entry == null) { 	//no page is large enough to hold record. 
								//allocate new data page and append it to tail,
								//also update treemap
			//get the header page
			HFPage currpage = new HFPage();
			Minibase.BufferManager.pinPage(firstpageid, currpage, false);
			currpage.setCurPage(firstpageid);
			//create a new page		
			HFPage datapage = new HFPage();
			PageId datapageid = Minibase.BufferManager.newPage(datapage, 1);
			//find the last page
			PageId currpageid = new PageId(firstpageid.pid);
			PageId nextpageid;
			while (currpage.getNextPage().pid != -1) {
				nextpageid = currpage.getNextPage();
				Minibase.BufferManager.unpinPage(currpageid, true);
				currpageid = nextpageid;
				Minibase.BufferManager.pinPage(currpageid, currpage, false);
				currpage.setCurPage(currpageid);
			}
			datapage.setCurPage(datapageid);
			currpage.setNextPage(datapageid);
			Minibase.BufferManager.unpinPage(currpageid, true);
			//insert the record
			rid = datapage.insertRecord(record);
			//update treemap and unpin data page
			Short freesize = new Short(datapage.getFreeSpace());
			Minibase.BufferManager.unpinPage(datapageid, true);
			List<PageId> pagelist = freespace.get(freesize);
			if (pagelist == null) {
					pagelist = new ArrayList<PageId>();
					pagelist.add(datapageid);
					freespace.put(freesize, pagelist);
			} else {
				pagelist.add(datapageid);
			}

			/*
			for (Short sp : freespace.keySet()) {
				for (PageId p : freespace.get(sp)) {
					System.out.print("(" + sp + ", " + p + ") ");	
				}
			}
			System.out.println("");
			*/
			
		} else {	//found a suitable page
			HFPage datapage = new HFPage();
			List<PageId> pagelist = entry.getValue();
			Short freesize = entry.getKey();
			//always choose the 1st one
			PageId pageid = pagelist.get(0);
			Minibase.BufferManager.pinPage(pageid, datapage, false);
			datapage.setCurPage(pageid);
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

			/*
			for (Short sp : freespace.keySet()) {
				for (PageId p : freespace.get(sp)) {
					System.out.print("(" + sp + ", " + p + ") ");	
				}
			}
			System.out.println("");
			*/
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

		datapage.setCurPage(pageid);
		int length = datapage.getSlotLength(rid.slotno);
		Tuple tuple = new Tuple(datapage.selectRecord(rid), 0, length);
		Minibase.BufferManager.unpinPage(pageid, true);
		return tuple;
	}

	public boolean deleteRecord(RID rid)
	 throws ChainException, 
	 IOException {
	 	/*
	 	for (Short sp : freespace.keySet()) {
				for (PageId p : freespace.get(sp)) {
					System.out.print("(" + sp + ", " + p + ") ");	
				}
			}
		System.out.println("");
		*/
	 	PageId pageid = new PageId(rid.pageno.pid);
		HFPage datapage = new HFPage();
		datapage.setCurPage(pageid);
		Minibase.BufferManager.pinPage(pageid, datapage, false);
		//check if this record exists
		if (datapage.selectRecord(rid) == null) {
			Minibase.BufferManager.unpinPage(pageid, true);
			throw new InvalidUpdateException();
		}
		//print treemap
		/*
		System.out.println(freespace.isEmpty());
		for (Short sp : freespace.keySet()) {
			for (PageId p : freespace.get(sp)) {
				System.out.print("(" + sp + ", " + p + ") ");	
			}
		}
		System.out.println("");
		*/
		/*
		//remove from treemap
		Short freesize = new Short(datapage.getFreeSpace());
		List<PageId> pagelist = freespace.get(freesize);
		//if not full
		if (pagelist != null) {
			//find the elem sharing the same pageid.pid
			for (PageId p : pagelist)
				if (p.pid == pageid.pid)
					pagelist.remove(p);
			if (pagelist.isEmpty())
				freespace.remove(freesize);
		}
		*/
		//delete the record from hfpage
		datapage.deleteRecord(rid);
		/*
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
		*/
		Minibase.BufferManager.unpinPage(pageid, true);
		--cnt;
		return true;
	 }

	public boolean updateRecord(RID rid, Tuple newRecord) 
	 throws InvalidUpdateException, 
	 IOException {
	 	PageId pageid = rid.pageno;
	 	HFPage datapage = new HFPage();
	 	datapage.setCurPage(pageid);
		Minibase.BufferManager.pinPage(pageid, datapage, false);

		if(datapage.getSlotLength(rid.slotno) != newRecord.getLength()) {
			Minibase.BufferManager.unpinPage(pageid, true);
			throw new InvalidUpdateException();
		}
		datapage.updateRecord(rid, newRecord);
		Minibase.BufferManager.unpinPage(pageid, true);
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

