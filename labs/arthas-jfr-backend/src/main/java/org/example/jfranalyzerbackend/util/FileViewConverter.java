package org.example.jfranalyzerbackend.util;

import org.example.jfranalyzerbackend.dto.FileView;
import org.example.jfranalyzerbackend.entity.shared.file.FileEntity;

public class FileViewConverter {
    public static FileView convert(FileEntity entity) {
        return new FileView(
                        entity.getId(),
                        entity.getUniqueName(),
                        entity.getOriginalName(),
                        entity.getSize(),
                        entity.getType(),
                        entity.getCreatedTime()
                );
    }
}
