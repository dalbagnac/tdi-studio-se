// ============================================================================
//
// Copyright (C) 2006-2007 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.ui.wizards.metadata.connection.files.excel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

import org.junit.Assert;
import org.junit.Test;

/**
 * DOC yexiaowei class global comment. Detailled comment
 */
public class ExcelReader {

    private String excelPath = null;

    private Workbook workbook = null;

    public ExcelReader() {

    }

    /**
     * Maybe take long time
     * <p>
     * DOC yexiaowei ExcelReader constructor comment.
     * 
     * @param excel
     * @throws BiffException
     * 
     * @throws IOException
     */
    public ExcelReader(String excel) throws BiffException, IOException {
        this.excelPath = excel;
        init();
    }

    private void init() throws BiffException, IOException {
        WorkbookSettings worksetting = new WorkbookSettings();
        worksetting.setEncoding("ISO-8859-15");
        workbook = Workbook.getWorkbook(new File(excelPath));
    }

    public String[] getSheetNames() {
        return workbook.getSheetNames();
    }

    public List<String[]> readSheet(String sheetName) {

        Sheet sheet = workbook.getSheet(sheetName);

        if (sheet == null) {
            return null;
        }

        int rows = sheet.getRows();

        List<String[]> res = new ArrayList<String[]>();
        for (int i = 0; i < rows; i++) {
            Cell[] cells = sheet.getRow(i);
            String[] contents = new String[cells.length];
            for (int j = 0, k = cells.length; j < k; j++) {
                contents[j] = cells[j].getContents();
            }
            res.add(contents);
        }

        return res.size() <= 0 ? null : res;

    }

    /**
     * Getter for excelPath.
     * 
     * @return the excelPath
     */
    public String getExcelPath() {
        return this.excelPath;
    }

    public static String[] getColumnsTitle(int rows) {
        String[] x = new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
                "S", "T", "U", "V", "W", "X", "Y", "Z" };
        if (rows <= 0) {
            return null;
        } else if (rows <= 26) {
            String[] res = new String[rows];
            System.arraycopy(x, 0, res, 0, rows);
            return res;
        } else if (rows < 26 * 26) {
            String[] res = new String[rows];
            System.arraycopy(x, 0, res, 0, 26);
            int offset = 26;
            FirstLoop: for (String first : x) {
                for (String second : x) {
                    String rowName = first + second;
                    res[offset] = rowName;
                    offset++;
                    if (offset == rows) {
                        break FirstLoop;
                    }
                }
            }
            return res;
        } else {
            return null;// Too much rows
        }
    }

    @Test
    public void testReadSheet() {
        try {
            ExcelReader reader = new ExcelReader("/home/yexiaowei/testdata/test.xls");
            List res = reader.readSheet("Sheet1");
            Assert.assertEquals(7, res.size());

        } catch (BiffException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRowNamesGenerators() {
        String[] res = ExcelReader.getColumnsTitle(26);
        Assert.assertEquals("Z", res[25]);

        res = ExcelReader.getColumnsTitle(27);
        Assert.assertEquals("AA", res[26]);

        res = ExcelReader.getColumnsTitle(100);
        for (String name : res) {
            System.out.println(name);
        }
    }
}
