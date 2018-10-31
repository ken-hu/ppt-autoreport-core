package com.hui.core.report.model;

import java.util.List;

public class TableData {

    //表格一行数据
    private List<TableRowData> tableRowDataList;

    public TableData() {

    }

    public TableData(List<TableRowData> tableRowDataList) {
        this.tableRowDataList = tableRowDataList;
    }

    public List<TableRowData> getTableRowDataList() {
        return tableRowDataList;
    }

    public void setTableRowDataList(List<TableRowData> tableRowDataList) {
        this.tableRowDataList = tableRowDataList;
    }
}
