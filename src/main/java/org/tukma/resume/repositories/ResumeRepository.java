package org.tukma.resume.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tukma.resume.models.Resume;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    Optional<Resume> findByResumeHash(String resumeHash);
    List<Resume> findByJob_Id(Long jobId);
    List<Resume> findByOwner_Id(Long ownerId);
    Optional<Resume> findByJob_IdAndOwner_Id(Long jobId, Long ownerId);
    List<Resume> findAllByJob_IdAndOwner_Id(Long jobId, Long ownerId);
}
