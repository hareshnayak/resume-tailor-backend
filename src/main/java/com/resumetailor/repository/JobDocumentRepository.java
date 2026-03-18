package com.resumetailor.repository;

import com.resumetailor.model.JobDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobDocumentRepository extends MongoRepository<JobDocument, String> {

    List<JobDocument> findByJobUrl(String jobUrl);
}

