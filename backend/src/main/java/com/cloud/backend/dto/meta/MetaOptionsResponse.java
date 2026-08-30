package com.cloud.backend.dto.meta;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MetaOptionsResponse {

    private Map<String, List<OptionItem>> groups;

    public MetaOptionsResponse(Map<String, List<OptionItem>> groups) {
        this.groups = groups;
    }
}
