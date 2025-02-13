package org.tukma.interviewer.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tukma.interviewer.models.Interview;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
}
