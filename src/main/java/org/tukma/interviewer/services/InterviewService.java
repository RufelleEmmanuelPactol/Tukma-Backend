package org.tukma.interviewer.services;

import org.springframework.stereotype.Service;
import org.tukma.auth.models.UserEntity;
import org.tukma.interviewer.models.Interview;
import org.tukma.interviewer.repositories.InterviewRepository;
import org.tukma.jobs.models.Job;

@Service
public class InterviewService {
    private InterviewRepository interviewRepository;

    public InterviewService(InterviewRepository interviewRepository) {
        this.interviewRepository = interviewRepository;
    }

    public Interview createInterview(UserEntity user, Job job,
                                     String analysisResults, String resumeText,
                                     String ticket){
        Interview interview = new Interview();
        interview.setUser(user);
        interview.setJob(job);
        interview.setAnalysisResults(analysisResults);
        interview.setResumeText(resumeText);
        interview.setTicket(ticket);
        interviewRepository.save(interview);
        return interview;
    }
}
