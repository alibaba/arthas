package org.example.jfranalyzerbackend.dto;

import org.example.jfranalyzerbackend.enums.FileType;

import java.time.LocalDateTime;

public record FileView(long id,
                       String uniqueName,
                       String originalName,
                       long size,
                       FileType type,
                       LocalDateTime createdTime) {
}
