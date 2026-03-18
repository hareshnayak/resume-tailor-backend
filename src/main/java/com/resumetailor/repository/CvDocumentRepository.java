package com.resumetailor.repository;

import com.resumetailor.model.CvDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CvDocumentRepository extends MongoRepository<CvDocument, String> {

    Optional<CvDocument> findByUserId(String userId);

    void deleteByUserId(String userId);
}
