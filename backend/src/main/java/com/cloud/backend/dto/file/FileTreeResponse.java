package com.cloud.backend.dto.file;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FileTreeResponse {

    private Long id;
    private String name;
    private Boolean isDirectory;
    private List<FileTreeResponse> children = new ArrayList<>();

    public static FileTreeResponse of(Long id, String name, boolean isDirectory) {
        FileTreeResponse node = new FileTreeResponse();
        node.setId(id);
        node.setName(name);
        node.setIsDirectory(isDirectory);
        return node;
    }
}
