package com.hui.core.report.exception;

/**
 * <b><code>ReportErrorEnum</code></b>
 * <p/>
 * Description:报告声场异常MSG
 * <p/>
 * <b>Creation Time:</b> 2018/10/31 22:41.
 *
 * @author Hu Weihui
 */
public enum ReportErrorEnum {
    /********************** 报告读取失败 **********************/
    READ_FAIL("READ_FAIL"),

    /********************** 数据读取失败 **********************/
    CHART_DATA_ERROR("图表数据读取失败"),
    CHART_SERIER_DATA_ERROR("图表系列数据读取失败"),

    /********************** 构造图表失败 **********************/
    LINE_CHART_SERIER_ERROR("折线图系列读取失败"),
    BAR_CHART_SERIER_ERROR("柱状图系列读取失败"),
    PIE_CHART_SERIER_ERROR("饼图系列读取失败"),
    BAR_LINE_CHART_ERROR("构造柱状+折线图失败，传入数据系列<1");

    private String msg;


    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    ReportErrorEnum(String msg) {
        this.msg = msg;
    }
}
