package com.avsos.dataexporterbackend.DTO;

import com.avsos.dataexporterbackend.DTO.payload.Header;
import com.avsos.dataexporterbackend.DTO.payload.DataRow;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OutDto {

    Header[] headers;
    List<DataRow> dataRows = new ArrayList<>();

    public OutDto(Header[] headers) {
        this.headers = headers;
    }

    public void addToRow(DataRow row) {
        dataRows.add(row);
    }
}
