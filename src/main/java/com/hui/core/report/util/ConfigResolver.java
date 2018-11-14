
package com.hui.core.report.util;

import com.hui.core.report.model.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.POIXMLDocumentPart;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b><code>ConfigResolver</code></b>
 * <p/>
 * Description:解读配置文件DEMO
 * <p/>
 * <b>Creation Time:</b> 2018/10/23 22:42.
 *
 * @author Hu Weihui
 */
public class ConfigResolver {

    //就是执行SQL的一个工具类。这里不提供。因为不是我写的。可以用JdbcTemplate代替。不过要修改下ConfigResolver的代码。
    //往后修改成JdbcTemplate再提交。测试运行的DEMO代码不需要这个工具类
    private SqlAutoMapper sqlAutoMapper;

    private static final String XML_OBJ_PATH = "declare namespace p='http://schemas.openxmlformats.org/presentationml/2006/main' .//*/p:cNvPr";

    private static final String CHART_SQL = "chart.sql";

    private static final String CHART_COL_ROW_VALUE = "chart.col.row.value";

    private static final String TABLE_SQL = "table.sql";


    /**
     * 分析PPT配置文件.
     *
     * @param templateFilePath the template file path
     * @param paramsMap        the params map
     * @return the map
     * @throws IOException the io exception
     * @since hui_project 1.0.0
     */
    public Map<Integer, SlideData> analysisPowerPointConfig(String templateFilePath, Map<String, Object> paramsMap) throws IOException {
        Map<Integer, SlideData> slideDataMap = new HashMap<>();

        FileInputStream fileInputStream = new FileInputStream(templateFilePath);
        XMLSlideShow ppt = new XMLSlideShow(fileInputStream);

        List<XSLFSlide> slideList = ppt.getSlides();
        for (XSLFSlide slide : slideList) {
            SlideData slideData = loadSlideConfig(slide,paramsMap);
            slideDataMap.put(slide.getSlideNumber(), slideData);
        }
        return slideDataMap;
    }


    /**
     * 读取每页配置文件.
     *
     * @param slide     the slide
     * @param paramsMap the params map
     * @return the slide data
     * @throws IOException the io exception
     * @since hui_project 1.0.0
     */
    private SlideData loadSlideConfig(XSLFSlide slide, Map<String, Object> paramsMap) throws IOException {
        SlideData slideData = new SlideData();
        List<ChartData> chartDataList = new ArrayList<>();
        Map<String, String> textMap = new HashMap<>();
        List<TableData> tableDataList = new ArrayList<>();

        List<POIXMLDocumentPart> partList = slide.getRelations();
        for (POIXMLDocumentPart part : partList) {
            if (part instanceof XSLFChart) {
                List<ChartSeries> seriesDataList = getDataMap((XSLFChart) part, paramsMap);
                ChartData chartData = new ChartData();
                chartData.setChartSeriesList(seriesDataList);
                chartDataList.add(chartData);
            }
        }

        List<XSLFShape> shapeList = slide.getShapes();

        for (XSLFShape shape : shapeList) {

            //判断文本框
            if (shape instanceof XSLFTextShape) {
                Map<String, String> tempMap = getDataMap((XSLFTextShape) shape,paramsMap);
                textMap.putAll(tempMap);
            }
            //判断表格
            if (shape instanceof XSLFTable) {
                List<TableRowData> tempList = getDataMap((XSLFTable) shape);
                TableData tableData = new TableData();
                tableData.setTableRowDataList(tempList);
                tableDataList.add(tableData);
            }
        }
        slideData.setTextMap(textMap);
        slideData.setTableDataList(tableDataList);
        slideData.setChartDataList(chartDataList);
        slideData.setSlidePage(slide.getSlideNumber());

        return slideData;
    }



    /**
     * 获取表格需要渲染的值
     *
     * @param table
     * @return
     */
    public  List<TableRowData> getDataMap(XSLFTable table) {
        Map<String, String> dataMap = new HashMap<>();
        XSLFTableCell cell = table.getCell(0, 0);
        String sql="";
        if (cell != null) {
            String text = cell.getText();
            Map<String, String> configMap = getConfig(text);
            for (String key : configMap.keySet()) {
                if (TABLE_SQL.equals(key)){
                    sql = configMap.get(key);
                }
            }
            List<Map<String, Object>> sqlDataList = sqlAutoMapper.selectList(sql);
            return analysisTableData(sqlDataList);
        }

        return new ArrayList<>();
    }

    /**
     * 分析表格数据.
     *
     * @param sqlDataList the sql data list
     * @return the list
     * @since hui_project 1.0.0
     */
    private List<TableRowData> analysisTableData(List<Map<String, Object>> sqlDataList){
        List<TableRowData> tableRowDataList = new ArrayList<>();
        for (Map<String, Object> stringObjectMap : sqlDataList) {
            TableRowData tableData = new TableRowData();
            List<String> dataList = new ArrayList<>();
            for (String key:stringObjectMap.keySet()){
                String data = (String) stringObjectMap.get(key);
                dataList.add(data);
            }
            tableData.setDataList(dataList);
            tableRowDataList.add(tableData);
        }
        return tableRowDataList;
    }

    /**
     * 获取文本框里面的替换值
     *
     * @param text
     * @return
     */
    public  Map<String, String> getDataMap(XSLFTextShape text, Map<String,Object> paramsMap) {
        Map<String, String> dataMap = new HashMap<>();
        XmlObject[] xmlObjectArray = text.getXmlObject().selectPath(XML_OBJ_PATH);
        if (xmlObjectArray.length > 0) {
            CTNonVisualDrawingProps props = (CTNonVisualDrawingProps) (CTNonVisualDrawingProps.class.isInstance(xmlObjectArray[0]) ? xmlObjectArray[0] : null);
            if (props != null) {
                Map<String, String> configMap = getConfig(props.getDescr());
                for (String key : configMap.keySet()) {
                    String dataKey = key.substring(0,key.indexOf("."));
                    String sql = configMap.get(key);
                    String dataValue = sqlAutoMapper.selectOne(sql, paramsMap,String.class);
                    dataMap.put(dataKey, dataValue);
                }
            }
        }
        return dataMap;
    }


    /**
     * 获取表格数据.
     *
     * @param chart     the chart
     * @param paramsMap the params map
     * @return the data map
     * @throws IOException the io exception
     * @since hui_project 1.0.0
     */
    public List<ChartSeries> getDataMap(XSLFChart chart, Map<String, Object> paramsMap) throws IOException {
        POIXMLDocumentPart excelPart = chart.getRelations().get(0);
        InputStream excelInputStream = excelPart.getPackagePart().getInputStream();
        Workbook workbook = new XSSFWorkbook(excelInputStream);
        Sheet sheet = workbook.getSheetAt(0);
        String config = sheet.getRow(25).getCell(25).getStringCellValue();
        excelInputStream.close();
        workbook.close();
        Map<String, String> configMap = getConfig(config);
        String sql = "";
        Map<String, String> colRowValueMap = new HashMap<>();
        for (String key : configMap.keySet()) {
            if (CHART_SQL.equals(key)) {
                sql = configMap.get(key);
            } else if (CHART_COL_ROW_VALUE.equals(key)) {
                String colRowValue = configMap.get(key);
                String colRowValueArray[] = colRowValue.split(":");
                for (String s : colRowValueArray) {
                    String mapKey = s.substring(0, s.indexOf(","));
                    String mapValue = s.substring(s.indexOf(",") + 1, s.length());
                    colRowValueMap.put(mapKey, mapValue);
                }
            }

        }
        return analysisGraphData(sql, colRowValueMap, paramsMap);
    }


    /**
     * sql获取表数据.
     *
     * @param sql            the sql
     * @param colRowValueMap the col row value map
     * @param paramsMap      the params map
     * @return the list
     * @since hui_project 1.0.0
     */
    private  List<ChartSeries> analysisGraphData(String sql, Map<String,String> colRowValueMap, Map<String,Object>
            paramsMap) {
        String colName = colRowValueMap.get("col");
        String rowName = colRowValueMap.get("row");
        String valueName = colRowValueMap.get("value");

        List<Map<String, Object>> dataList = sqlAutoMapper.selectList(sql,paramsMap);
        List<ChartSeries> seriesDataList = new ArrayList<>();

        Map<String, ChartSeries> tmpMap = new HashMap<>();
        for (Map<String, Object> dataMap : dataList) {

            String _colName = (String)dataMap.get(colName);
            String _rowName = (String)dataMap.get(rowName);
            Double _value = Double.parseDouble((String) dataMap.get(valueName));

            if (tmpMap.get(_colName)!=null){
                ChartSeries seriesData = tmpMap.get(_colName);
                seriesData.getChartCategoryList().add(new ChartCategory(_rowName, _value));
            }else {
                List<ChartCategory> categoryDataList = new ArrayList<>();
                categoryDataList.add(new ChartCategory(_rowName, _value));
                ChartSeries seriesData = new ChartSeries();
                seriesData.setSeriesName(_colName);
                seriesData.setChartCategoryList(categoryDataList);
                tmpMap.put(_colName, seriesData);
            }
        }
        for (String key:tmpMap.keySet()){
            seriesDataList.add(tmpMap.get(key));
        }

        return seriesDataList;
    }

    /**
     * 配置转成Map
     *
     * @param config the config
     * @return the config
     * @since hui_project 1.0.0
     */
    private Map getConfig(String config) {
        config = config.trim();
        Map<String, String> configMap = new HashMap<>();
        if (StringUtils.isNotBlank(config) && config.length() >= 8) {
            int i = config.indexOf("<config>");
            int i1 = config.indexOf("</config>");
            config = config.substring(config.indexOf("<config>") + 8, config.indexOf("</config>"));
            String[] keyValues = config.split(";");
            for (String keyValue : keyValues) {
                keyValue = keyValue.trim();
                if (StringUtils.isNotBlank(keyValue)) {
                    int index = keyValue.indexOf("=");
                    String key = keyValue.substring(0, index);
                    String value = keyValue.substring(index + 1);
                    configMap.put(key, value);
                }
            }
        }
        return configMap;
    }


    /**
     * 去除配置.
     *
     * @param textShape the text shape
     * @since hui_project 1.0.0
     */
    public void removePPTConfig(XSLFTextShape textShape) {
        XmlObject[] xmlObjectArray = textShape.getXmlObject().selectPath(XML_OBJ_PATH);
        if (xmlObjectArray.length > 0) {
            CTNonVisualDrawingProps props = (CTNonVisualDrawingProps) (CTNonVisualDrawingProps.class.isInstance(xmlObjectArray[0]) ? xmlObjectArray[0] : null);
            if (props != null) {
                props.setDescr("");
            }
        }
    }



}
