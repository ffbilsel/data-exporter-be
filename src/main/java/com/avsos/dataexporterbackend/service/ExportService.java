package com.avsos.dataexporterbackend.service;

import com.avsos.dataexporterbackend.DTO.*;
import com.avsos.dataexporterbackend.DTO.payload.DataRow;
import com.avsos.dataexporterbackend.DTO.payload.Header;
import com.avsos.dataexporterbackend.DTO.payload.OutHeader;
import com.avsos.dataexporterbackend.DTO.types.DataType;
import com.avsos.dataexporterbackend.DTO.types.InType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ExportService {

    private final XmlMapper xmlMapper = new XmlMapper();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public Resource process(MultipartFile file, HeaderDto headerDto) {
        headerDto.setInHeaderMap();
        OutDto out;
        try {
           if (headerDto.getInType() == InType.CSV) {
               out = processCSV(file, headerDto);
           }
           else {
               out = processExcel(file, headerDto);
           }
            switch (headerDto.getOutType()) {
                case CSV -> {
                    return writeToCsv(out);
                }
                case EXCEL -> {
                    return writeToExcel(out);
                }
                case JSON -> {
                    return writeToJson(out);
                }
                case XML -> {
                    return writeToXml(out);
                }
                case G_SHEETS -> {
                    return writeToGoogleSheets(out);
                }
            }
       }
       catch (Exception e) {
           throw new RuntimeException(e);
       }
        return null;
    }

    public Resource process(HeaderTextDto headerTextDto) {
        headerTextDto.setInHeaderMap();
        OutDto out = processGoogleSheets(headerTextDto);
        try {
            switch (headerTextDto.getOutType()) {
                case CSV -> {
                    return writeToCsv(out);
                }
                case EXCEL -> {
                    return writeToExcel(out);
                }
                case JSON -> {
                    return writeToJson(out);
                }
                case XML -> {
                    return writeToXml(out);
                }
                case G_SHEETS -> {
                    return writeToGoogleSheets(out);
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private OutDto processCSV(MultipartFile file, HeaderDto headerDto) throws Exception {
        OutDto out = new OutDto(Arrays.stream(headerDto.getOutHeader()).map(Header::new).toArray(Header[]::new));
        String[] lines = new String(file.getInputStream().readAllBytes()).split("\r\n|\n");
        Map<Integer, String> indexes = new HashMap<>();
        pairIndexes(indexes, lines[0].split(";"), headerDto.getHeader());
        for (int i = 1; i < lines.length; i++) {
            String[] cells = lines[i].split(";");
            Map<String, Object> values = new HashMap<>();

            for (Map.Entry<Integer, String> entry : indexes.entrySet()) {
                values.put(entry.getValue(), transform(cells[entry.getKey()],
                        headerDto.getHeader()[headerDto.getInHeaderMap().get(entry.getValue())].getType()));
            }
            DataRow dataRow = new DataRow();
            List<Object> temp = new ArrayList<>();
            for (OutHeader outHeader : headerDto.getOutHeader()) {
                String script = outHeader.getScript();
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    script = script.replaceAll(entry.getKey(), entry.getValue() instanceof Integer ? entry.getValue().toString() : "\"" + entry.getValue() + "\"");
                }
                temp.add(transform(runJs(script), outHeader.getType()));
            }
            dataRow.setCells(temp.toArray());
            out.addToRow(dataRow);
        }
        return out;
    }

    private OutDto processExcel(MultipartFile file, HeaderDto headerDto) throws IOException {
        OutDto out = new OutDto(Arrays.stream(headerDto.getOutHeader()).map(Header::new).toArray(Header[]::new));

        Map<Integer, String> indexes = new HashMap<>();
        XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        List<String> tempHeaders = new ArrayList<>();
        Iterator<Cell> startIter = sheet.getRow(0).cellIterator();
        while (startIter.hasNext()) {
            tempHeaders.add(startIter.next().getStringCellValue());
        }
        pairIndexes(indexes, tempHeaders.toArray(new String[0]), headerDto.getHeader());
        Iterator<Row> rowIterator = sheet.rowIterator();
        rowIterator.next();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Iterator<Cell> cellIterator = row.cellIterator();
            List<String> cells =new ArrayList<>();

            while (cellIterator.hasNext()) {
                Cell cell =cellIterator.next();
                cell.setCellType(CellType.STRING);
                cells.add(cell.getStringCellValue());
            }
            Map<String, Object> values = new HashMap<>();

            for (Map.Entry<Integer, String> entry : indexes.entrySet()) {
                values.put(entry.getValue(), transform(cells.get(entry.getKey()),
                        headerDto.getHeader()[headerDto.getInHeaderMap().get(entry.getValue())].getType()));
            }
            DataRow dataRow = new DataRow();
            List<Object> temp = new ArrayList<>();
            for (OutHeader outHeader : headerDto.getOutHeader()) {
                String script = outHeader.getScript();
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    script = script.replaceAll(entry.getKey(), entry.getValue() instanceof Integer ? entry.getValue().toString() : "\"" + entry.getValue() + "\"");
                }
                temp.add(transform(runJs(script), outHeader.getType()));
            }
            dataRow.setCells(temp.toArray());
            out.addToRow(dataRow);
        }
        return out;
    }

    private OutDto processGoogleSheets(HeaderTextDto headerTextDto) {
        OutDto out = new OutDto(Arrays.stream(headerTextDto.getOutHeader()).map(Header::new).toArray(Header[]::new));

        Map<Integer, String> indexes = new HashMap<>();
        pairIndexes(indexes, headerTextDto.getData()[0], headerTextDto.getHeader());
        String[][] lines = headerTextDto.getData();
        for (int i = 1; i < lines.length; i++) {
            String[] cells = lines[i];
            Map<String, Object> values = new HashMap<>();

            for (Map.Entry<Integer, String> entry : indexes.entrySet()) {
                values.put(entry.getValue(), transform(cells[entry.getKey()],
                        headerTextDto.getHeader()[headerTextDto.getInHeaderMap().get(entry.getValue())].getType()));
            }
            DataRow dataRow = new DataRow();
            List<Object> temp = new ArrayList<>();
            for (OutHeader outHeader : headerTextDto.getOutHeader()) {
                String script = outHeader.getScript();
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    script = script.replaceAll(entry.getKey(), entry.getValue() instanceof Integer ? entry.getValue().toString() : "\"" + entry.getValue() + "\"");
                }
                temp.add(transform(runJs(script), outHeader.getType()));
            }
            dataRow.setCells(temp.toArray());
            out.addToRow(dataRow);
        }
        return out;
    }

    private Resource writeToCsv(OutDto out) {
        StringBuilder output = new StringBuilder();
        for (Header header : out.getHeaders()) {
            output.append(header.getName() + ";");
        }
        output.deleteCharAt(output.length() - 1);
        output.append("\n");
        for (DataRow dataRow : out.getDataRows()) {
            for (Object cell : dataRow.getCells()) {
                output.append(cell + ";");
            }
            output.deleteCharAt(output.length() - 1);
            output.append("\n");
        }
        try{
            File file = new File("./result.csv");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fr = new FileWriter(file);
            BufferedWriter br = new BufferedWriter(fr);
            br.write(output.toString());
            br.close();
            fr.close();
            return new ByteArrayResource(Files.readAllBytes( Paths.get("./result.csv")));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Resource writeToExcel(OutDto out) {
        try {

            Workbook wb = new XSSFWorkbook();

            XSSFSheet sheet = (XSSFSheet) wb.createSheet();
            CreationHelper creationHelper = wb.getCreationHelper();
            // Create
            XSSFRow rowHeader = sheet.createRow(0);
            Header[] headers = out.getHeaders();
            for (int i = 0; i < headers.length; i++) {
                XSSFCell cell = rowHeader.createCell(i);
                RichTextString richString = creationHelper
                        .createRichTextString(headers[i].getName());
                Font font = wb.createFont();
                font.setBold(true);
                richString.applyFont(font);

                cell.setCellValue(richString);
                CellStyle style = wb.createCellStyle();
                style.setFont(font);

                cell.setCellStyle(style);
            }

            List<DataRow> dataRows = out.getDataRows();
            for (int i = 0; i < dataRows.size(); i++) {
                XSSFRow row = sheet.createRow(i + 1);
                Object[] cells = dataRows.get(i + 1).getCells();
                for (int j = 0; j < cells.length ; j++) {
                    XSSFCell cell = row.createCell(j);
                    cell.setCellValue(cells[j].toString());
                }
            }
            OutputStream outputStream = new FileOutputStream("./result.xlsx");
            wb.write(outputStream);
            outputStream.close();
            wb.close();
            return new ByteArrayResource(Files.readAllBytes( Paths.get("./result.xlsx")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Resource writeToXml(OutDto out) {

       try {
           String str = xmlMapper.writer().withDefaultPrettyPrinter().writeValueAsString(
                   new Sheets(out.getDataRows().toArray(new DataRow[0])));
           File file = new File("./result.xml");
           if (!file.exists()) {
               file.createNewFile();
           }
           FileWriter fr = new FileWriter(file);
           BufferedWriter br = new BufferedWriter(fr);
           br.write(str);
           br.close();
           fr.close();
           return new ByteArrayResource(Files.readAllBytes( Paths.get("./result.xml")));
       }
       catch (Exception e){
           throw new RuntimeException(e);
       }
    }

    private Resource writeToJson(OutDto out) {
        try {
            String str = jsonMapper.writer().withDefaultPrettyPrinter().writeValueAsString(
                    new Sheets(out.getDataRows().toArray(new DataRow[0])));
            File file = new File("./result.xml");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fr = new FileWriter(file);
            BufferedWriter br = new BufferedWriter(fr);
            br.write(str);
            br.close();
            fr.close();
            return new ByteArrayResource(Files.readAllBytes( Paths.get("./result.xml")));
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private Resource writeToGoogleSheets(OutDto out) {
        return null;
    }

    private void pairIndexes(Map<Integer, String> indexes, String[] tokens, Header[] headers) {
        for (Header header : headers) {
            for (int i = 0; i < tokens.length; i++) {
                if (header.getName().equals(tokens[i])) {
                    indexes.put(i, tokens[i]);
                    break;
                }
            }
        }
    }

    private Object transform(String cell, DataType type) {
        Object val = null;
        switch (type) {
            case INT -> val = Integer.parseInt(cell);
            case STR -> val = cell;
            case CHAR -> val = cell.charAt(0);
            case DATE -> val = LocalDate.parse(cell);
            case DATETIME -> val = LocalDateTime.parse(cell);
        }
        return val;
    }

    private String runJs(String code) {
        Context context = Context.create();
        Value value = context.eval("js", code);
        return value.asString();
    }
}
