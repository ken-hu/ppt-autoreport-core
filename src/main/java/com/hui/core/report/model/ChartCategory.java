package com.hui.core.report.model;

/**
 * <b><code>ChartCategory</code></b>
 * <p/>
 * Description:
 * <p/>
 * <b>Creation Time:</b> 2018/10/31 21:58.
 *
 * @author Hu Weihui
 */
public class ChartCategory {

    //类别名
    private String categoryName;

    //值
    private double val;

    public ChartCategory(String categoryName, double val) {

        this.categoryName = categoryName;
        this.val = val;
    }

    public ChartCategory() {
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public double getVal() {
        return val;
    }

    public void setVal(double val) {
        this.val = val;
    }


}
