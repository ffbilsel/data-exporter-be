package com.avsos.dataexporterbackend.DTO;

import com.avsos.dataexporterbackend.DTO.payload.DataRow;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Sheets {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Sheet")
    public List<Sheets.Row> row = new ArrayList<>();

    public Sheets(DataRow[] rows) {
        Arrays.stream(rows).forEach(item -> row.add(new Row(item)));
    }

    public Sheets() {
    }

    public static class Row {

        @JacksonXmlElementWrapper(localName = "Row")
        @JacksonXmlProperty(localName = "Col")
        public Object[] col;

        public Row(DataRow item) {
           col = item.getCells();
        }

    }

}
