package org.sooil.autologextraction.service;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FileService {

    private static final String EXCEL_FILE_PATH = "/Applications/workspace/work_docs/adolescent_001_20240725.xls";
    private static final String LOG_FILE_PATH = "/Applications/workspace/work_docs/plgs_simulation 4.log";
    private static final String NEW_EXCEL_FILE_PATH = "/Applications/workspace/work_docs/adolescent_0725_updated_result.xls";

    private static final Pattern STATUS_PATTERN = Pattern.compile(
            "\\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\]-performCheck.*status - (START|KEEP|STOP) - (\\{(?:[^{}]|\\{[^{}]*\\})*\\})"
    );

    /*private static final Pattern RESULT_PATTERN = Pattern.compile(
            "\\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\]-performCheck.*result - (\\{(?:[^{}]|\\{[^{}]*\\})*\\}) - (START|KEEP|STOP)"
    );*/
    private static final Pattern RESULT_PATTERN = Pattern.compile(
            "\\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\]-performCheck.*(status|result) - (\\{(?:[^{}]|\\{[^{}]*\\})*\\})(?: - (START|KEEP|STOP))?"
    );

    public Map<String, String> extractLogEntries() throws IOException {
        Map<String, String> logEntries = new HashMap<>();
        try (FileInputStream fis = new FileInputStream(new File(LOG_FILE_PATH))) {
            StringBuilder logContent = new StringBuilder();
            int ch;
            while ((ch = fis.read()) != -1) {
                logContent.append((char) ch);
            }
            String[] lines = logContent.toString().split("\n");

            for (String line : lines) {
                Matcher statusMatcher = STATUS_PATTERN.matcher(line);
                Matcher resultMatcher = RESULT_PATTERN.matcher(line);
                if (statusMatcher.find()) {
                    String timestamp = statusMatcher.group(1);
                    String status = statusMatcher.group(2);
                    String json = statusMatcher.group(3);
                    String timeKey = timestamp.substring(11, 16);
                    logEntries.put(timeKey, "status - " + status + " - " + json);
                } else if (resultMatcher.find()) {
                    String timestamp = resultMatcher.group(1);
                    String json = resultMatcher.group(2);
                    String status = resultMatcher.group(3);
                    String timeKey = timestamp.substring(11, 16);
                    logEntries.put(timeKey, "result - " + json + " - " + status);
                }
            }
        }
        return logEntries;
    }

    public void updateExcelFile(Map<String, String> logEntries) throws IOException {
        try (FileInputStream fis = new FileInputStream(new File(EXCEL_FILE_PATH));
             Workbook workbook = new HSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell timeCell = row.getCell(1);
                if (timeCell != null && timeCell.getCellType() == CellType.STRING) {
                    String cellTime = timeCell.getStringCellValue();

                    if (cellTime.length() >= 19 && cellTime.charAt(10) == 'T') {
                        String timeKey = cellTime.substring(11, 16);

                        if (logEntries.containsKey(timeKey)) {
                            String status = logEntries.get(timeKey);
                            Cell statusCell = row.createCell(9, CellType.STRING);
                            statusCell.setCellValue(status);
                        } else {
                            Cell statusCell = row.createCell(9, CellType.STRING);
                            statusCell.setCellValue("");
                        }
                    } else {
                        System.err.println("Unexpected time format: " + cellTime);
                    }
                }
            }

            // Save the workbook to a new file
            try (FileOutputStream fos = new FileOutputStream(new File(NEW_EXCEL_FILE_PATH))) {
                workbook.write(fos);
            }
        }
    }
}
