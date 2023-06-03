package com.avsos.dataexporterbackend.DTO;

import com.avsos.dataexporterbackend.DTO.payload.Header;
import com.avsos.dataexporterbackend.DTO.payload.OutHeader;
import com.avsos.dataexporterbackend.DTO.types.InType;
import com.avsos.dataexporterbackend.DTO.types.OutType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class HeaderDto {

    InType inType;
    OutType outType;
    Header[] header;
    OutHeader[] outHeader;

    @JsonIgnore
    Map<String, Integer> inHeaderMap = new HashMap<>();

    @JsonIgnore
    public void setInHeaderMap() {
        for (int i = 0; i < header.length; i++) {
            inHeaderMap.put(header[i].getName(), i);
        }
    }

}
