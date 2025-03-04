package org.tukma.jobs.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tukma.jobs.models.Job;
import org.tukma.jobs.models.Keyword;

import java.util.List;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    public List<Keyword> findByKeywordOwner_Id(Long id);
    
    public List<Keyword> findByKeywordOwner(Job job);

}
