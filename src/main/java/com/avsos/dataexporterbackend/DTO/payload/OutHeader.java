package com.avsos.dataexporterbackend.DTO.payload;

import com.avsos.dataexporterbackend.DTO.types.DataType;
import lombok.Data;

@Data
public class OutHeader {
    String name;
    DataType type;
    String script;


}
