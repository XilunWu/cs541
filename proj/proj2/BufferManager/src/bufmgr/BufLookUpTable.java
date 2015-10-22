package bufmgr;

import java.util.ArrayList;

import global.PageId;
import global.GlobalConst;

import chainexception.ChainException;

public class BufLookUpTable {
	public class Bucket {
		public class BucketElem {
			public int pageid;
			public int frameid;
			private BucketElem(){};
			public BucketElem(int pageid, int frameid){
				this.pageid = pageid;
				this.frameid = frameid;
			}
		}
		private ArrayList<BucketElem> list = new ArrayList<BucketElem>();
		public Bucket (){}
		/*	return -1 as not found
		return frame# if found
		*/
		public int lookup(int pageid)
			throws HashEntryNotFoundException {
			for (BucketElem e : list) {
				if (e.pageid == pageid)
					return e.frameid;
			}
			throw new HashEntryNotFoundException(null, "HASHTABLE: HASH_ENTRY_NOT_FOUND.");
		}
		public void add(int pageid, int frameid){
			list.add(new BucketElem(pageid, frameid));
		}
		public void remove(int pageid, int frameid){
			for (BucketElem e : list) {
				if (e.pageid == pageid && e.frameid == frameid) {
					list.remove(e);
					break;
				}
			}
		}
	}

	public static final int HTSIZE = 23;
	private Bucket [] dir = new Bucket[HTSIZE];

	public BufLookUpTable() {
		for (int i = 0; i < HTSIZE; ++i)
				dir[i] = new Bucket();
	}

	private int hfunction(int x) {return (5 * x + 9) % HTSIZE;}
	/*	return -1 as not found
		return frame# if found
	*/
	public int lookup(int pageid) 
	 throws HashEntryNotFoundException {
		return dir[hfunction(pageid)].lookup(pageid);
	}
	public void add(int pageid, int frameid){
		dir[hfunction(pageid)].add(pageid, frameid);
	}
	public void remove(int pageid, int frameid) {
		dir[hfunction(pageid)].remove(pageid, frameid);
	}
}