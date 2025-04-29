package service;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * PDF Report Generator for Academic Hub
 *
 * @author eduardoescobar
 */
public class PDFReportGenerator {

    /**
     * Generates a PDF report with statistics by major
     * @param majorCounts Map of majors and their counts
     * @param departmentCounts Map of departments and their counts
     * @param totalRecords Total number of records
     * @return The generated PDF file
     */
    public static File generateMajorReport(Map<String, Integer> majorCounts,
                                           Map<String, Integer> departmentCounts,
                                           int totalRecords) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String fileName = "reports/major_report_" + dateFormat.format(new Date()) + ".pdf";

            File reportsDir = new File("reports");
            if (!reportsDir.exists()) {
                reportsDir.mkdir();
            }

            File reportFile = new File(fileName);

            try (FileOutputStream outputStream = new FileOutputStream(reportFile)) {
                String content = "ACADEMIC HUB REPORT\n" +
                        "Generated: " + new Date() + "\n\n" +
                        "MAJOR STATISTICS\n";

                for (Map.Entry<String, Integer> entry : majorCounts.entrySet()) {
                    double percentage = 100.0 * entry.getValue() / totalRecords;
                    content += entry.getKey() + ": " + entry.getValue() +
                            " (" + String.format("%.1f", percentage) + "%)\n";
                }

                content += "\nDEPARTMENT STATISTICS\n";
                for (Map.Entry<String, Integer> entry : departmentCounts.entrySet()) {
                    double percentage = 100.0 * entry.getValue() / totalRecords;
                    content += entry.getKey() + ": " + entry.getValue() +
                            " (" + String.format("%.1f", percentage) + "%)\n";
                }

                outputStream.write(content.getBytes());
            }

            MyLogger.makeLog("Generated PDF report: " + fileName);
            return reportFile;

        } catch (Exception e) {
            MyLogger.makeLog("Error generating PDF report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Opens the generated PDF report with the default PDF viewer
     * @param reportFile The PDF file to open
     */
    public static void openReport(File reportFile) {
        try {
            if (reportFile != null && reportFile.exists()) {
                // This would launch the default PDF viewer on most platforms
                java.awt.Desktop.getDesktop().open(reportFile);
                MyLogger.makeLog("Opened PDF report: " + reportFile.getAbsolutePath());
            }
        } catch (Exception e) {
            MyLogger.makeLog("Error opening PDF report: " + e.getMessage());
            e.printStackTrace();
        }
    }
}