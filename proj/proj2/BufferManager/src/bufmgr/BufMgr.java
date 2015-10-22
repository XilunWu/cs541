package bufmgr;

import global.Convert;
import global.GlobalConst;
import global.Minibase;
import global.Page;
import global.PageId;

import chainexception.ChainException;

import java.util.ArrayList;
import java.util.Set;
import java.util.Iterator;
import java.lang.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class BufMgr implements GlobalConst {
	private Page [] bufpool;
	private Descriptor [] bufdescr;
	private String replacementPolicy;
	private int lookAheadSize;
	private int numbufs;
	private BufLookUpTable lookUpTable;
	private ArrayList<ArrayList<Long>> pintime;
	private long inittime = (long)System.currentTimeMillis() / 1000;
	/**
	* Create the BufMgr object.
	* Allocate pages (frames) for the buffer pool in main memory and
	* make the buffer manage aware that the replacement policy is
	* specified by replacerArg (e.g., LH, Clock, LRU, MRU, LRFU, etc.).
	*
	* @param numbufs number of buffers in the buffer pool
	* @param lookAheadSize number of pages to be looked ahead, you can ignore that parameter
	* @param replacementPolicy Name of the replacement policy, that parameter will be set to "LRFU"
	*/
	public BufMgr(int numbufs, int lookAheadSize, String replacementPolicy) {
		bufpool = new Page[numbufs];
		for (int i = 0; i < numbufs; ++i) 
			bufpool[i] = new Page();
		bufdescr = new Descriptor[numbufs]; 
		for (int i = 0; i < numbufs; ++i) 
			bufdescr[i] = new Descriptor();
		this.numbufs = numbufs;
		this.lookAheadSize = lookAheadSize;
		this.replacementPolicy = replacementPolicy;
		lookUpTable = new BufLookUpTable();
		pintime = new ArrayList<ArrayList<Long>>();
		for (int i = 0; i < numbufs; ++i) 
			pintime.add(new ArrayList<Long>());
	}

	/**
	* helper function for pin/unpin
	* find an empty frame or a replacement candidate
	* return frameId (-1 if no candidate)
	*/
	private int replace() {
		/* a very large number which is impossible for CRF	*/
		long time = (long)System.currentTimeMillis() / 1000 - inittime + 1;
		double min = 1000000;
		int frameid = -1;
		//System.out.println("time is " + time);
		for (int i = 0; i < numbufs; ++i) {
//			System.out.println("i: " + i + " , pin_count = " + bufdescr[i].pin_count);
			if (bufdescr[i].pin_count == 0) {
				if (pintime.get(i).isEmpty()) return i;
				else {
					double sum = 0;
					for (int j = 0; j < pintime.get(i).size(); ++j)
						sum += 1.0 / (time - pintime.get(i).get(j).longValue() + 1);
					if (sum < min) {
						min = sum;
						frameid = i;
					}
				}
			}
		}
		//System.out.println("candidate: " + frameid);

		return frameid;
	}

	/**
	* Pin a page.
	* First check if this page is already in the buffer pool.
	* If it is, increment the pin_count and return a pointer to this
	* page.
	* If the pin_count was 0 before the call, the page was a
	* replacement candidate, but is no longer a candidate.
	* If the page is not in the pool, choose a frame (from the
	* set of replacement candidates) to hold this page, read the
	* page (using the appropriate method from {\em diskmgr} package) and pin it.
	* Also, must write out the old page in chosen frame if it is dirty
	* before reading new page.__ (You can assume that emptyPage==false for
	* this assignment.)
	*
	* @param pageno page number in the Minibase.
	* @param page the pointer point to the page.
	* @param emptyPage true (empty page); false (non-empty page)
	*/
	public void pinPage(PageId pageno, Page page, boolean emptyPage) 
		throws BufferPoolExceededException,
		 IOException {
		/*	
		 *	check if this page is in buffer pool.
		 *	use lookUpTable
		 */
		int frameid = -1;
		try {
			frameid = lookUpTable.lookup(pageno.pid);
		} catch (HashEntryNotFoundException e) {
			
		} finally {
			/*	page not found	*/
			long time = (long)System.currentTimeMillis() / 1000;
			if (frameid == -1) {
				/*	
				 * choose a frame (from the
				 * set of replacement candidates) to hold this page, read the
				 * page (using read_page() method from DiskMgr package) and pin it.
				 * Also, must write out the old page in chosen frame if it is dirty
				 * before reading new page.__ (You can assume that emptyPage==false for
				 * this assignment.)
				 */
				frameid = replace();
				if (frameid == -1) throw new BufferPoolExceededException(null, "BUFMGR: BUFPOOL_EXCEEDED.");
				try {
					/*	write out frame candidate if needed	*/
					if (bufdescr[frameid].dirtybit) 
						Minibase.DiskManager.write_page(bufdescr[frameid].number, bufpool[frameid]);
					Minibase.DiskManager.read_page(pageno, bufpool[frameid]);
				} catch (Exception e) {
					throw new IOException("BUFMGR: IO ERROR WHEN CALL READ/WRITE PAGE.");
				}
				/*	remove candidate from BufLookUpTable */
				lookUpTable.remove(bufdescr[frameid].number.pid, frameid);
				/*	modify Descriptor and hashtable*/
				bufdescr[frameid].pin_count = 1;	
				bufdescr[frameid].number.pid = pageno.pid;
				bufdescr[frameid].dirtybit = false;
				/*	don't forget to clear the pintime set of this frame */
				pintime.get(frameid).clear();
				/*	add new item into buflookuptable */
				lookUpTable.add(pageno.pid, frameid);
				/*	set page pointer to frame in bufpool	*/
				page.setPage(bufpool[frameid]);
			} else {	/*	page is in buffer pool	*/
				(bufdescr[frameid].pin_count)++;	
				page.setPage(bufpool[frameid]);
			}
			/*	add (time - inittime + 1) into pintime, since time starts from 1  */
			pintime.get(frameid).add(new Long(time - inittime + 1));
		}
		
	}

	/**
	* Unpin a page specified by a pageId.
	* This method should be called with dirty==true if the client has
	* modified the page.
	* If so, this call should set the dirty bit
	* for this frame.
	* Further, if pin_count>0, this method should
	* decrement it.
	*If pin_count=0 before this call, throw an exception
	* to report error.
	*(For testing purposes, we ask you to throw
	* an exception named PageUnpinnedException in case of error.)
	*
	* @param pageno page number in the Minibase.
	* @param dirty the dirty bit of the frame
	*/
	public void unpinPage(PageId pageno, boolean dirty) 
		throws PageUnpinnedException {
		int frameid;
		try {
			frameid = lookUpTable.lookup(pageno.pid);
		} catch (Exception e) {
			throw new PageUnpinnedException(e, "BUFMGR: PAGE_NOT_PINNED.");
		}
		if (dirty) bufdescr[frameid].dirtybit = true;
		if (bufdescr[frameid].pin_count == 0) throw new PageUnpinnedException(null, "BUFMGR: PAGE_NOT_PINNED.");
		(bufdescr[frameid].pin_count)--;
	}

	/**
	* Allocate new pages.
	* Call DB object to allocate a run of new pages and
	* find a frame in the buffer pool for the first page
	* and pin it. (This call allows a client of the Buffer Manager
	* to allocate pages on disk.) If buffer is full, i.e., you
	* can't find a frame for the first page, ask DB to deallocate
	* all these pages, and return null.
	*
	* @param firstpage the address of the first page.
	* @param howmany total number of allocated new pages.
	*
	* @return the first page id of the new pages.__ null, if error.
	*/
	public PageId newPage(Page firstpage, int howmany) 
	 throws ChainException, IOException {
	 	PageId firstpageid;
		if (null == firstpage) throw new ChainException(null, "Null pointer received: firstpage.");
		try {
			firstpageid = Minibase.DiskManager.allocate_page(howmany);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (IllegalStateException e) {
			throw e;
		} 
		try {
			pinPage(firstpageid, firstpage, false);
		} catch (BufferPoolExceededException e) {
			//de-allocate all pages
			try {
				Minibase.DiskManager.deallocate_page(firstpageid, howmany);
			} catch (Exception ex) {
				throw ex;
			} finally {
				return null;
			}
		} catch (IOException e) {
			throw e;
		}
		return firstpageid;
	};

	/**
	* This method should be called to delete a page that is on disk.
	* This routine must call the method in diskmgr package to
	* deallocate the page.
	*
	* @param globalPageId the page number in the data base.
	*/
	public void freePage(PageId globalPageId)   
	 throws PagePinnedException,
	  ChainException {
	 	int frameid = -1;
	 	try {
	 		frameid = lookUpTable.lookup(globalPageId.pid);
	 	} catch (HashEntryNotFoundException e) {
	 		//page is not in buffer, free it
		 	Minibase.DiskManager.deallocate_page(globalPageId);
	 	} 

	 	//System.out.println("frameid is " + frameid + ", pageid is " + globalPageId.pid);
	 	if (frameid != -1 && bufdescr[frameid].pin_count != 0) {
	 	/*	
	 		System.out.println(bufdescr[frameid].pin_count);
	 		System.out.println(bufdescr[frameid].number.pid);
	 		System.out.println(bufdescr[frameid].dirtybit);
		*/	
	 		throw new PagePinnedException(null, "BUFMGR: PAGE_IS_PINNED. ");
	 	}
	 	Minibase.DiskManager.deallocate_page(globalPageId);
	 	
	}

	/**
	* Used to flush a particular page of the buffer pool to disk.
	* This method calls the write_page method of the diskmgr package.
	*
	* @param pageid the page number in the database.
	*/
	public void flushPage(PageId pageid) 
	 throws PageUnpinnedException, IOException {
		int frameid;
		try {
			frameid = lookUpTable.lookup(pageid.pid);
		} catch (Exception e) {
			throw new PageUnpinnedException(e, "BUFMGR: PAGE_NOT_PINNED.");
		}
		try {
			/*	
			 *	note: Shall we check if dirtybit is true before flushing?
			 *	Or just flush it whenever this function is called?
			 */
			Minibase.DiskManager.write_page(pageid, bufpool[frameid]);
		} catch (Exception e) {
			throw new IOException("BUFMGR: IO ERROR WHEN CALL READ/WRITE PAGE.");
		}
	}

	/**
	* Used to flush all dirty pages in the buffer pool to disk
	*
	*/
	public void flushAllPages() 
 	 throws PageUnpinnedException, IOException {
		for (Descriptor descr : bufdescr) 
			if (descr.dirtybit) flushPage(descr.number);
	}

	/**
	* Returns the total number of buffer frames.
	*/
	public int getNumBuffers() {
		return numbufs;
	}

	/**
	* Returns the total number of unpinned buffer frames.
	*/
	public int getNumUnpinned() {
		int count = 0;
		for (Descriptor descr : bufdescr) 
			if (descr.pin_count == 0) ++count;
		return count;
	}

};