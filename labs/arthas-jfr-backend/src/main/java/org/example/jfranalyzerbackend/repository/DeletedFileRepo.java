package org.example.jfranalyzerbackend.repository;

import org.example.jfranalyzerbackend.entity.shared.file.DeletedFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeletedFileRepo extends JpaRepository<DeletedFileEntity, Long> {
} 