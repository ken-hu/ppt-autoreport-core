package com.hui.core.report.model;

import java.util.List;

public class ChartData {
    //所有系列
    private List<ChartSeries> chartSeriesList;


    public ChartData() {

    }

    public ChartData(List<ChartSeries> chartSeriesList) {
        this.chartSeriesList = chartSeriesList;
    }

    public List<ChartSeries> getChartSeriesList() {
        return chartSeriesList;
    }

    public void setChartSeriesList(List<ChartSeries> chartSeriesList) {
        this.chartSeriesList = chartSeriesList;
    }


}
