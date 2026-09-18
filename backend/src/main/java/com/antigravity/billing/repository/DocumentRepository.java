package com.antigravity.billing.repository;

import com.antigravity.billing.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    @Query("SELECT d FROM Document d WHERE " +
           "(:entityType IS NULL OR d.entityType = :entityType) AND " +
           "(:search IS NULL OR LOWER(d.fileName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(d.notes) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Document> findDocumentsWithFilter(
            @Param("entityType") String entityType,
            @Param("search") String search,
            Pageable pageable
    );
}
