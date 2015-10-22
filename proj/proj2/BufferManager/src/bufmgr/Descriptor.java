package bufmgr;

import global.PageId;

public class Descriptor {
	public int pin_count = 0;
	public PageId number = new PageId();
	public boolean dirtybit = false;
}