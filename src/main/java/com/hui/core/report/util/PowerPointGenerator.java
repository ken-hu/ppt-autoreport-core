package com.hui.core.report.util;

import com.hui.core.report.exception.ReportErrorEnum;
import com.hui.core.report.exception.ReportExcetion;
import com.hui.core.report.model.*;
import org.apache.poi.POIXMLDocumentPart;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <b><code>PowerPointGenerator</code></b>
 * <p/>
 * Description:
 * <p/>
 * <b>Creation Time:</b> 2018/10/31 22:41.
 *
 * @author Hu Weihui
 */
public class PowerPointGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(PowerPointGenerator.class);

    private PowerPointGenerator() {

    }

    /**
     * PPT构造方法
     *
     * @param templateFilePath
     * @param destFilePath
     * @param slideDataMap     每一页的数据（页数，对应页数的数据）
     */
    public static void generatorPowerPoint(String templateFilePath, String destFilePath, Map<Integer, SlideData> slideDataMap) {
        XMLSlideShow ppt = readPowerPoint(templateFilePath);
        if (ppt == null) {
            throw new ReportExcetion(ReportErrorEnum.READ_FAIL.getMsg());
        }

        List<XSLFSlide> slideList = ppt.getSlides();
        //遍历每一页PPT构造
        for (XSLFSlide slide : slideList) {
            int slidePage = slide.getSlideNumber();
            SlideData slideData = slideDataMap.get(slidePage);
            if (slideData != null) {
                generatorSlide(slide, slideData);
            } else {
                LOGGER.info("PPT 第 {} 页没有需要替换的数据", slidePage);
            }
        }
        savePowerPoint(ppt, destFilePath);
    }

    /**
     * 保存ppt到指定路径
     *
     * @param ppt
     * @param outputFilePath
     */
    private static void savePowerPoint(XMLSlideShow ppt, String outputFilePath) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(outputFilePath);
            ppt.write(fileOutputStream);
        } catch (IOException e) {
            LOGGER.info("save powerpoint to destPath fail ", e);
        } finally {
            try {
                fileOutputStream.close();
                ppt.close();
            } catch (IOException e) {
                LOGGER.info("stream close fail  ", e);
            }
        }

    }


    /**
     * 读取模板库PPT
     *
     * @param inputFilePath
     * @return
     */
    private static XMLSlideShow readPowerPoint(String inputFilePath) {
        FileInputStream fileInputStream = null;
        XMLSlideShow ppt = null;
        try {
            fileInputStream = new FileInputStream(inputFilePath);
            ppt = new XMLSlideShow(fileInputStream);
        } catch (IOException e) {
            LOGGER.info(" readPowerPoint fail,reason: {}", e);
        }
        return ppt;
    }


    /**
     * 替换每一页数据
     *
     * @param slide
     * @param slideData
     * @throws IOException
     */
    private static void generatorSlide(XSLFSlide slide, SlideData slideData) {
        List<POIXMLDocumentPart> partList = slide.getRelations();
        //当一页有多个图表的时候
        int chartNum = 0;
        for (POIXMLDocumentPart part : partList) {
            if (part instanceof XSLFChart) {
                ChartData chartData = slideData.getChartDataList().get(chartNum);
                if (chartData == null) {
                    throw new ReportExcetion(ReportErrorEnum.CHART_DATA_ERROR.getMsg());
                }
                List<ChartSeries> seriesDataList = chartData.getChartSeriesList();
                if (seriesDataList == null) {
                    throw new ReportExcetion(ReportErrorEnum.CHART_SERIER_DATA_ERROR.getMsg());
                }
                //构造PPT
                generatorChart((XSLFChart) part, seriesDataList);
                chartNum++;
            }
        }


        List<XSLFShape> shapeList = slide.getShapes();

        for (XSLFShape shape : shapeList) {
            Map<String, String> textMap = slideData.getTextMap();
            List<TableData> tableDataList = slideData.getTableDataList();
            //当一页有多个表格的时候
            int tableNum = 0;
            //判断文本框
            if (shape instanceof XSLFTextShape) {
                generatorTextBox((XSLFTextShape) shape, textMap);
            }
            //判断表格
            if (shape instanceof XSLFTable) {
                List<TableRowData> tableRowDataList = tableDataList.get(tableNum).getTableRowDataList();
                generatorTable((XSLFTable) shape, tableRowDataList);

            }
            tableNum++;
        }

    }

    /**
     * 构造图表
     *
     * @param chart
     * @param seriesDataList
     */
    private static void generatorChart(XSLFChart chart, List<ChartSeries> seriesDataList) {
        //替换图表的数据
        CTPlotArea plotArea = chart.getCTChart().getPlotArea();
        if (plotArea.getBarChartList().size() != 0 && plotArea.getLineChartList().size() != 0) {
            //折线+柱状
            generatorBarAndLineGraph(chart, seriesDataList);

        } else if (plotArea.getBarChartList().size() != 0) {
            //柱状
            generatorBarGraph(chart, seriesDataList);

        } else if (plotArea.getLineChartList().size() != 0) {
            //折线
            generatorLineGraph(chart, seriesDataList);

        } else if (plotArea.getPieChartList().size() != 0) {
            //饼图
            generatorPieGraph(chart, seriesDataList);

        }
    }

    /**
     * 构造文本
     *
     * @param textShape
     * @param textMap
     */
    private static void generatorTextBox(XSLFTextShape textShape, Map<String, String> textMap) {
        List<XSLFTextParagraph> textParagraphList = textShape.getTextParagraphs();
        for (XSLFTextParagraph textParagraph : textParagraphList) {
            //正则表达式匹配${}标识符的text
            String text = textParagraph.getText();
            String regex = "\\$\\{.*?\\}";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(text);
            List<String> keys = new ArrayList<>();
            while (matcher.find()) {
                keys.add(matcher.group());
            }
            for (String key : keys) {
                String textKey = key.substring(2, key.length() - 1);
                text = text.replace(key, textMap.get(textKey) == null ? " " : textMap.get(textKey));
            }

            List<XSLFTextRun> textRuns = textParagraph.getTextRuns();
            for (XSLFTextRun textRun : textRuns) {
                textRun.setText("");
            }
            if (textRuns.size() > 0) {
                textRuns.get(0).setText(text);
            }
        }
    }

    /**
     * 构造表格
     *
     * @param table
     * @param tableDataList
     */
    private static void generatorTable(XSLFTable table, List<TableRowData> tableDataList) {
        List<XSLFTableRow> rows = table.getRows();
        int rowSize = rows.size() - 1;
        for (int i = 0; i < tableDataList.size(); i++) {
            //为了保证样式的不变判断数据量比模板的row多的时候，在原来表格基础上新增row
            if (i < rowSize) {
                List<XSLFTableCell> cells = rows.get(i + 1).getCells();
                for (int j = 0; j < tableDataList.get(i).getDataList().size(); j++) {
                    String s = tableDataList.get(i).getDataList().get(j);
                    cells.get(j).setText(s);
                }
            } else {
                table.addRow();
                XSLFTableRow row = rows.get(i + 1);
                for (int j = 0; j < tableDataList.get(i).getDataList().size(); j++) {
                    String s = tableDataList.get(i).getDataList().get(j);
                    row.addCell().setText(s);
                }
            }
        }
        //清空我的配置文件（根据自己解析数据的业务清空配置文件）
        table.getCell(0, 0).setText("");
    }


    /**
     * 构建折线图.
     *
     * @param chart          the chart
     * @param seriesDataList the series data list
     * @throws IOException the io exception
     * @since nile -cmszbs-szcst 0.1.0
     */
    private static void generatorLineGraph(XSLFChart chart, List<ChartSeries> seriesDataList) {
        //创建Excel 绑定到图表
        Workbook workBook = createGraphWorkBook(seriesDataList);
        Sheet sheet = workBook.getSheetAt(0);

        //获取折线图系列
        //获取图形区域
        CTPlotArea plotArea = chart.getCTChart().getPlotArea();
        CTLineChart lineChart = plotArea.getLineChartList().get(0);

        //todo 模板系列比填充数据的系列少，新增系列成功，但名字没显示出来，如果模板本身存在足够的系列则不会出现该问题（可多不可少）
        //模板系列比填充数据的系列少，要提前新增系列
        if (seriesDataList.size() > lineChart.getSerList().size()) {
            int addTimes = seriesDataList.size() - lineChart.getSerList().size();
            for (int i = 0; i < addTimes; i++) {
                CTLineSer ctLineSer = lineChart.addNewSer();

                CTSerTx serTx = ctLineSer.addNewTx();

                CTAxDataSource axDataSource = ctLineSer.addNewCat();

                CTNumDataSource numDataSource = ctLineSer.addNewVal();

                addSeries(serTx, axDataSource, numDataSource);

            }
        }

        //模板系列比填充数据的系列多，要去除多余的系列
        if (lineChart.getSerList().size() > seriesDataList.size()) {
            int removeTimes = lineChart.getSerList().size() - seriesDataList.size();
            for (int i = 0; i < removeTimes; i++) {
                lineChart.removeSer(lineChart.getSerList().size() - 1);
            }
        }

        List<CTLineSer> serList = lineChart.getSerList();
        if (serList == null) {
            throw new ReportExcetion(ReportErrorEnum.LINE_CHART_SERIER_ERROR.getMsg());
        }
        for (int i = 0; i < seriesDataList.size(); i++) {
            ChartSeries seriesData = seriesDataList.get(i);

            CTLineSer lineSer = serList.get(i);
            //更新系列名称
            CTSerTx tx = lineSer.getTx();

            //获取类别和值的数据源操作对象
            CTAxDataSource category = lineSer.getCat();
            CTNumDataSource val = lineSer.getVal();

            //第一列是类别 从第二列开始刷新数据
            int colNum = i + 1;
            refreshSeriesData(sheet, tx, category, val, seriesData, colNum);
        }

        // 更新嵌入的workbook
        writeToGraphExcel(workBook, chart);
    }

    /**
     * 构建柱状图.
     *
     * @param chart          the chart
     * @param seriesDataList the series data list
     * @throws IOException the io exception
     * @since nile -cmszbs-szcst 0.1.0
     */
    private static void generatorBarGraph(XSLFChart chart, List<ChartSeries> seriesDataList) {
        //创建Excel 绑定到图表
        Workbook workBook = createGraphWorkBook(seriesDataList);
        Sheet sheet = workBook.getSheetAt(0);
        CTPlotArea plotArea = chart.getCTChart().getPlotArea();
        CTBarChart barChart = plotArea.getBarChartList().get(0);

        if (seriesDataList.size() > barChart.getSerList().size()) {
            int addTimes = seriesDataList.size() - barChart.getSerList().size();
            for (int i = 0; i < addTimes; i++) {
                CTBarSer ctBarSer = barChart.addNewSer();

                CTSerTx serTx = ctBarSer.addNewTx();

                CTAxDataSource axDataSource = ctBarSer.addNewCat();

                CTNumDataSource numDataSource = ctBarSer.addNewVal();

                addSeries(serTx, axDataSource, numDataSource);
            }
        }

        if (barChart.getSerList().size() > seriesDataList.size()) {
            int removeTimes = barChart.getSerList().size() - seriesDataList.size();
            for (int i = 0; i < removeTimes; i++) {
                barChart.removeSer(barChart.getSerList().size() - 1);
            }
        }

        List<CTBarSer> serList = barChart.getSerList();
        if (serList == null) {
            throw new ReportExcetion(ReportErrorEnum.BAR_CHART_SERIER_ERROR.getMsg());
        }

        for (int i = 0; i < seriesDataList.size(); i++) {
            ChartSeries seriesData = seriesDataList.get(i);

            CTBarSer barSer = serList.get(i);
            //更新系列名称
            CTSerTx tx = barSer.getTx();

            //获取类别和值的数据源操作对象
            CTAxDataSource category = barSer.getCat();
            CTNumDataSource val = barSer.getVal();

            int colNum = i + 1;
            refreshSeriesData(sheet, tx, category, val, seriesData, colNum);
        }
        // 更新嵌入的workbook
        writeToGraphExcel(workBook, chart);

    }


    /**
     * 构建饼图
     *
     * @param chart          the chart
     * @param seriesDataList the series data list
     * @throws IOException the io exception
     * @since nile -cmszbs-szcst 0.1.0
     */
    private static void generatorPieGraph(XSLFChart chart, List<ChartSeries> seriesDataList) {
        //创建Excel 绑定到图表
        Workbook workBook = createGraphWorkBook(seriesDataList);
        Sheet sheet = workBook.getSheetAt(0);

        CTPlotArea plotArea = chart.getCTChart().getPlotArea();
        CTPieChart pieChart = plotArea.getPieChartList().get(0);

        if (seriesDataList.size() > pieChart.getSerList().size()) {
            int addTimes = seriesDataList.size() - pieChart.getSerList().size();
            for (int i = 0; i < addTimes; i++) {
                CTPieSer ctPieSer = pieChart.addNewSer();

                CTSerTx serTx = ctPieSer.addNewTx();

                CTAxDataSource axDataSource = ctPieSer.addNewCat();

                CTNumDataSource numDataSource = ctPieSer.addNewVal();

                addSeries(serTx, axDataSource, numDataSource);
            }
        }

        if (pieChart.getSerList().size() > seriesDataList.size()) {
            int removeTimes = pieChart.getSerList().size() - seriesDataList.size();
            for (int i = 0; i < removeTimes; i++) {
                pieChart.removeSer(pieChart.getSerList().size() - 1);
            }
        }

        List<CTPieSer> serList = pieChart.getSerList();

        if (serList == null) {
            throw new ReportExcetion(ReportErrorEnum.PIE_CHART_SERIER_ERROR.getMsg());
        }

        for (int i = 0; i < seriesDataList.size(); i++) {
            ChartSeries seriesData = seriesDataList.get(i);

            CTPieSer pieSer = serList.get(i);
            //更新系列名称
            CTSerTx tx = pieSer.getTx();

            //获取类别和值的数据源操作对象
            CTAxDataSource category = pieSer.getCat();
            CTNumDataSource val = pieSer.getVal();

            //第一列是类别 从第二列开始刷新数据
            int colNum = i + 1;
            refreshSeriesData(sheet, tx, category, val, seriesData, colNum);
        }

        // 更新嵌入的workbook
        writeToGraphExcel(workBook, chart);

    }

    /**
     * 构建bar+line图.
     *
     * @param chart          the chart
     * @param seriesDataList the series data list
     * @since nile -cmszbs-szcst 0.1.0
     */
    private static void generatorBarAndLineGraph(XSLFChart chart, List<ChartSeries> seriesDataList) {
        //创建Excel 绑定到图表
        Workbook workBook = createGraphWorkBook(seriesDataList);
        Sheet sheet = workBook.getSheetAt(0);

        CTPlotArea plotArea = chart.getCTChart().getPlotArea();

        // 获取折线图表
        CTLineChart lineChart = plotArea.getLineChartArray(0);
        // 获取柱状图表
        CTBarChart barChart = plotArea.getBarChartArray(0);
        // 获取图表的系列
        CTBarSer barSer = barChart.getSerArray(0);
        // 获取折线的系列
        CTLineSer lineSer = lineChart.getSerArray(0);

        CTSerTx barTx = barSer.getTx();
        CTSerTx lineTx = lineSer.getTx();


        // Category Axis Data
        // 获取每个系列的类别，即x轴的值
        CTAxDataSource barCat = barSer.getCat();
        CTAxDataSource lineCat = lineSer.getCat();
        // 获取图表的值
        CTNumDataSource barVal = barSer.getVal();
        CTNumDataSource lineVal = lineSer.getVal();

        if (seriesDataList.size() < 1) {
            throw new ReportExcetion(ReportErrorEnum.BAR_LINE_CHART_ERROR.getMsg());
        }
        // 刷新柱状图数据
        refreshSeriesData(sheet, barTx, barCat, barVal, seriesDataList.get(0), 1);
        // 刷新折线图的数据
        refreshSeriesData(sheet, lineTx, lineCat, lineVal, seriesDataList.get(1),
                2);

        // 更新嵌入的workbook
        writeToGraphExcel(workBook, chart);
    }

    /**
     * 创建并初始化工作簿.
     *
     * @param seriesDataList the series data list
     * @return the workbook
     * @since nile -cmszbs-szcst 0.1.0
     */
    private static Workbook createGraphWorkBook(List<ChartSeries> seriesDataList) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        for (int i = 0; i <= seriesDataList.get(0).getChartCategoryList().size(); i++) {
            sheet.createRow(i);
            for (int j = 0; j <= seriesDataList.size(); j++) {
                sheet.getRow(i).createCell(j);
            }
        }
        LOGGER.info("创建并初始化表格成功");
        return workbook;
    }

    /**
     * 工作簿内容写到对应图形的EXCEL.
     *
     * @param workbook the workbook
     * @param chart    the chart
     * @throws IOException the io exception
     * @since nile -cmszbs-szcst 0.1.0
     */
    private static void writeToGraphExcel(Workbook workbook, XSLFChart chart) {
        POIXMLDocumentPart xlsPart = chart.getRelations().get(0);
        OutputStream xlsOut = xlsPart.getPackagePart().getOutputStream();
        try {
            workbook.write(xlsOut);
        } catch (IOException e) {
            LOGGER.info(" the stream for writeToGraphExcel fail fail", e);
        } finally {
            try {
                xlsOut.close();
                workbook.close();
            } catch (IOException e) {
                LOGGER.info("close the stream for writeToGraphExcel fail ", e);
            }
        }
        LOGGER.info("表格嵌入图表并更新成功");
    }


    /**
     * 刷新图表和表格的数据.
     *
     * @param sheet      the sheet
     * @param category   the category
     * @param val        the val
     * @param seriesData the series data
     * @param colNum     the col num
     * @since nile -cmszbs-szcst 0.1.0
     */
    private static void refreshSeriesData(Sheet sheet, CTSerTx tx, CTAxDataSource category, CTNumDataSource val,
                                          ChartSeries seriesData, int colNum) {
        //更新系列名称
        tx.getStrRef().getStrCache().getPtArray(0).setV(seriesData.getSeriesName());

        //绑定系列名为excel对应的列名
        String titleRef = new CellReference(sheet.getSheetName(), 0, colNum, true, true).formatAsString();
        tx.getStrRef().setF(titleRef);

        //类别数据
        CTStrData ctStrData = category.getStrRef().getStrCache();
        //系列值
        CTNumData ctNumData = val.getNumRef().getNumCache();

        ctStrData.setPtArray((CTStrVal[]) null);
        ctNumData.setPtArray((CTNumVal[]) null);

        //更新图表
        refreshCategoryData(ctStrData, ctNumData, seriesData);

        //更新Excel表格每一列的数据
        refreshExcelData(colNum, sheet, seriesData);

        //图数据绑定EXCEL表格数据
        ctNumData.getPtCount().setVal(seriesData.getChartCategoryList().size() - 1);
        ctStrData.getPtCount().setVal(seriesData.getChartCategoryList().size() - 1);

        String numDataRange = new CellRangeAddress(1, seriesData.getChartCategoryList().size(), colNum, colNum)
                .formatAsString(sheet.getSheetName(), true);
        val.getNumRef().setF(numDataRange);
        String axisDataRange = new CellRangeAddress(1, seriesData.getChartCategoryList().size(), 0, 0)
                .formatAsString(sheet.getSheetName(), true);
        category.getStrRef().setF(axisDataRange);
    }

    /**
     * 刷新表格数据.
     *
     * @param colNum     the col num
     * @param sheet      the sheet
     * @param seriesData the series data
     * @since teachermanager 0.1.0
     */
    private static void refreshExcelData(int colNum, Sheet sheet, ChartSeries seriesData) {
        List<ChartCategory> categoryDataList = seriesData.getChartCategoryList();
        //更新Excel表格每一列的数据
        sheet.getRow(0).getCell(colNum).setCellValue(seriesData.getSeriesName());
        for (int i = 0; i < sheet.getLastRowNum(); i++) {
            ChartCategory categoryData = categoryDataList.get(i);
            sheet.getRow(i + 1).getCell(colNum).setCellValue(categoryData.getVal());
        }
        LOGGER.info("更新第 {} 列数据成功", colNum);
        if (colNum == 1) {
            // 初始化excel的类别那一列
            for (int i = 0; i < seriesData.getChartCategoryList().size(); i++) {
                String serName = seriesData.getChartCategoryList().get(i).getCategoryName();
                sheet.getRow(i + 1).getCell(0).setCellValue(serName);
            }
            LOGGER.info("初始化excel 类别成功");
        }

    }

    /**
     * 刷新对应系列的列数据（类别数据）.
     *
     * @param ctStrData  the ct str data
     * @param ctNumData  the ct num data
     * @param seriesData the series data
     * @since teachermanager 0.1.0
     */
    private static void refreshCategoryData(CTStrData ctStrData, CTNumData ctNumData, ChartSeries seriesData) {
        List<ChartCategory> categoryDataList = seriesData.getChartCategoryList();
        for (int i = 0; i < categoryDataList.size(); i++) {
            ChartCategory categoryData = categoryDataList.get(i);
            //该系列的类别名字
            CTStrVal ctStrVal = ctStrData.addNewPt();
            ctStrVal.setIdx(i);
            ctStrVal.setV(categoryData.getCategoryName());

            //该系列对应类别的值
            CTNumVal ctNumVal = ctNumData.addNewPt();
            ctNumVal.setIdx(i);
            ctNumVal.setV(String.valueOf(categoryData.getVal()));
        }
        LOGGER.info("更新图形的类别数据成功");
    }

    /**
     * Add series.
     *
     * @param tx       the tx
     * @param category the category
     * @param val      the val
     * @since teachermanager 0.1.0
     */
    private static void addSeries(CTSerTx tx, CTAxDataSource category, CTNumDataSource val) {
        tx.addNewStrRef().addNewStrCache().addNewPt();

        CTStrData ctStrData = category.addNewStrRef().addNewStrCache();
        ctStrData.addNewPt();
        ctStrData.addNewPtCount();

        CTNumData ctNumData = val.addNewNumRef().addNewNumCache();
        ctNumData.addNewPt();
        ctNumData.addNewPtCount();
    }

}
