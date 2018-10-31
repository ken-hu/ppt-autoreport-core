package com.hui.core.report.model;

import java.util.List;

public class TableRowData {

    //每一行数据
    private List<String> dataList;

    public TableRowData() {
    }

    public TableRowData(List<String> dataList) {
        this.dataList = dataList;
    }


    public List<String> getDataList() {
        return dataList;
    }

    public void setDataList(List<String> dataList) {
        this.dataList = dataList;
    }
}
