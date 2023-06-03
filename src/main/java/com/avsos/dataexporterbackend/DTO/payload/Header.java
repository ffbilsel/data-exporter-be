package com.avsos.dataexporterbackend.DTO.payload;

import com.avsos.dataexporterbackend.DTO.types.DataType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Header {
    String name;
    DataType type;

    public Header(OutHeader header) {
        this.name = header.getName();
        this.type = header.getType();
    }
}

