package com.hui.core.report.model;

import java.util.List;
import java.util.Map;

public class SlideData {

    //页码
    private Integer slidePage;

    //表格数据 支持多个表格 按照创建顺序渲染
    private List<TableData> tableDataList;
    //图表数据 支持多个图表 按照创建顺序渲染
    private List<ChartData> chartDataList;
    //文本数据 格式${}
    private Map<String, String> textMap;

    public SlideData() {

    }

    public SlideData(Integer slidePage, List<TableData> tableDataList, List<ChartData> chartDataList, Map<String, String> textMap) {
        this.slidePage = slidePage;
        this.tableDataList = tableDataList;
        this.chartDataList = chartDataList;
        this.textMap = textMap;
    }

    public Integer getSlidePage() {
        return slidePage;
    }

    public void setSlidePage(Integer slidePage) {
        this.slidePage = slidePage;
    }

    public List<TableData> getTableDataList() {
        return tableDataList;
    }

    public void setTableDataList(List<TableData> tableDataList) {
        this.tableDataList = tableDataList;
    }

    public List<ChartData> getChartDataList() {
        return chartDataList;
    }

    public void setChartDataList(List<ChartData> chartDataList) {
        this.chartDataList = chartDataList;
    }

    public Map<String, String> getTextMap() {
        return textMap;
    }

    public void setTextMap(Map<String, String> textMap) {
        this.textMap = textMap;
    }
}
