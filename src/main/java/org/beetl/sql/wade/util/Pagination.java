package org.beetl.sql.wade.util;

public class Pagination {
    
    public static final int MAX_PAGE_SIZE = 500;
    
    public static final int MAX_RECODE_SIZE = Integer.MAX_VALUE;
    
    private boolean batch;
    
    private boolean range;
    
    private boolean needCount = true;
    
    private boolean onlyCount = false;
    
    private int start;
    
    private int size;
    
    private int count;
    
    private int currPage;
    
    /**
     * construct function
     * @throws Exception
     */
    public Pagination() throws Exception {
    }
    
    /**
     * construct function
     * @param batch
     * @throws Exception
     */
    public Pagination(boolean batch) throws Exception {
        setBatch(batch);
    }
    
    /**
     * construct function
     * @param batch
     * @param size
     * @throws Exception
     */
    public Pagination(boolean batch, int size) throws Exception {
        setBatch(batch, size);
    }
    
    /**
     * get max page size
     * @return int
     * @throws Exception
     */
    public int getMaxPageSize() throws Exception {
        return MAX_PAGE_SIZE;
    }
    
    /**
     * set batch
     * @param batch
     * @throws Exception
     */
    public void setBatch(boolean batch) throws Exception {
        this.batch = batch;
        this.size = getMaxPageSize();
        if (batch)
            range = true;
    }
    
    /**
     * set batch
     * @param batch
     * @param size
     * @throws Exception
     */
    public void setBatch(boolean batch, int size) throws Exception {
        this.batch = batch;
        this.size = size;
        if (batch)
            range = true;
    }
    
    /**
     * set range
     * @param start
     * @param count
     */
    public void setRange(int start, int size) {
        range = true;
        this.start = start;
        this.size = size;
    }
    
    /**
     * is batch
     * @return boolean
     */
    public boolean isBatch() {
        return batch;
    }
    
    /**
     * is range
     * @return boolean
     */
    public boolean isRange() {
        return range;
    }
    
    /**
     * is need count
     * @return boolean
     */
    public boolean isNeedCount() {
        return needCount;
    }
    
    /**
     * set need count
     * @param needCount
     */
    public void setNeedCount(boolean needCount) {
        this.needCount = needCount;
    }
    
    /**
     * is count
     * @return int
     */
    public int getCount() {
        return count;
    }
    
    /**
     * set count
     * @param count
     */
    public void setCount(int count) {
        this.count = count;
    }
    
    /**
     * get start
     * @return int
     */
    public int getStart() {
        return start;
    }
    
    /**
     * get size
     * @return int
     */
    public int getSize() {
        return size;
    }
    
    /**
     * get curr page
     * @return int
     */
    public int getCurrPage() {
        return currPage;
    }
    
    /**
     * get curr page
     * @param currPage
     */
    public void setCurrPage(int currPage) {
        this.currPage = currPage;
    }
    
    /**
     * @return onlyCount
     */
    public boolean isOnlyCount() {
        return onlyCount;
    }
    
    /**
     * @param onlyCount Ҫ���õ� onlyCount
     */
    public void setOnlyCount(boolean onlyCount) {
        this.onlyCount = onlyCount;
    }
    
}