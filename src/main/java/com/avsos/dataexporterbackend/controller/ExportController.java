package com.avsos.dataexporterbackend.controller;

import com.avsos.dataexporterbackend.DTO.HeaderDto;
import com.avsos.dataexporterbackend.DTO.HeaderTextDto;
import com.avsos.dataexporterbackend.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin
@RestController
public class ExportController {

    ExportService exportService;

    @Autowired
    public void setExportService(ExportService exportService) {
        this.exportService = exportService;
    }

    @RequestMapping(value = "/export", method = RequestMethod.POST, consumes = "multipart/form-data", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody Resource output(@RequestPart MultipartFile file, @RequestPart HeaderDto inData) {
        return exportService.process(file, inData);
    }

    @PostMapping(value = "/export/sheets", consumes = "application/json")
    public @ResponseBody Resource output(@RequestBody HeaderTextDto inData) {
        return exportService.process(inData);
    }

}
