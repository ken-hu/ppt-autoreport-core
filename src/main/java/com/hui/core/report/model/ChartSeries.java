package com.hui.core.report.model;

import java.util.List;

public class ChartSeries {

    //系列名字
    private String seriesName;

    //该系列图表类别+值
    private List<ChartCategory> chartCategoryList;

    public ChartSeries() {

    }

    public ChartSeries(String seriesName, List<ChartCategory> chartCategoryList) {
        this.seriesName = seriesName;
        this.chartCategoryList = chartCategoryList;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public void setSeriesName(String seriesName) {
        this.seriesName = seriesName;
    }

    public List<ChartCategory> getChartCategoryList() {
        return chartCategoryList;
    }

    public void setChartCategoryList(List<ChartCategory> chartCategoryList) {
        this.chartCategoryList = chartCategoryList;
    }

}
