package com.abhi.authProject.service;

import com.abhi.authProject.model.*;
import com.abhi.authProject.repo.InterviewRepository;
import com.abhi.authProject.repo.JobApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class InterviewService {

    private static final Logger logger = LoggerFactory.getLogger(InterviewService.class);

    private final InterviewRepository interviewRepository;
    private final JobApplicationRepository jobApplicationRepository;
    
    // REMOVED: The old JavaMailSender is gone.
    // private final JavaMailSender mailSender;

    // ADDED: The new SendGridEmailService.
    private final SendGridEmailService emailService;

    // The senderEmail property is no longer needed here because it's configured inside SendGridEmailService.
    
    @Autowired
    public InterviewService(InterviewRepository interviewRepository,
                            JobApplicationRepository jobApplicationRepository,
                            SendGridEmailService emailService) { // UPDATED constructor
        this.interviewRepository = interviewRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.emailService = emailService; // UPDATED constructor
    }

    @Transactional
    public Interview scheduleInterview(InterviewScheduleRequest request) {
        JobApplication jobApplication = jobApplicationRepository.findById(request.getJobApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("Job Application not found with ID: " + request.getJobApplicationId()));

        if (jobApplication.getStatus() != ApplicationStatus.ACCEPTED) {
            throw new IllegalStateException("Cannot schedule interview for an application not in ACCEPTED status.");
        }

        Interview interview = new Interview();
        interview.setJobApplication(jobApplication);
        interview.setHrName(request.getHrName());
        interview.setHrEmail(request.getHrEmail());
        interview.setScheduledDateTime(request.getScheduledDateTime());
        interview.setInterviewLink(request.getInterviewLink());
        interview.setInterviewLocation(request.getInterviewLocation());
        interview.setNotes(request.getNotes());
        interview.setStatus(InterviewStatus.SCHEDULED);

        Interview savedInterview = interviewRepository.save(interview);

        jobApplication.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);
        jobApplicationRepository.save(jobApplication);

        try {
            sendInterviewScheduledEmailToApplicant(savedInterview);
        } catch (IOException e) {
            logger.error("Interview {} was scheduled, but failed to send notification email.", savedInterview.getId(), e);
        }

        return savedInterview;
    }

    @Transactional
    public Interview bookInterviewSlot(Long interviewId, InterviewBookingRequest request) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found with ID: " + interviewId));

        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new IllegalStateException("Interview is not in SCHEDULED status, cannot book.");
        }

        interview.setStudentBookedDateTime(request.getStudentBookedDateTime());
        interview.setStatus(InterviewStatus.BOOKED);
        Interview updatedInterview = interviewRepository.save(interview);

        JobApplication jobApplication = updatedInterview.getJobApplication();
        jobApplication.setStatus(ApplicationStatus.INTERVIEW_BOOKED);
        jobApplicationRepository.save(jobApplication);

        try {
            sendInterviewBookedConfirmationToApplicant(updatedInterview);
            sendInterviewBookedNotificationToHR(updatedInterview);
        } catch (IOException e) {
            logger.error("Interview {} was booked, but failed to send one or more confirmation emails.", updatedInterview.getId(), e);
        }

        return updatedInterview;
    }

    private void sendInterviewScheduledEmailToApplicant(Interview interview) throws IOException {
        String subject = "Interview Scheduled for " + interview.getJobApplication().getJobTitle();
        String interviewDetails = "Company: " + interview.getJobApplication().getCompanyName() + "<br>" +
                                  "Position: " + interview.getJobApplication().getJobTitle() + "<br>" +
                                  "Scheduled Date/Time: " + interview.getScheduledDateTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")) + "<br>";
        if (interview.getInterviewLink() != null && !interview.getInterviewLink().isEmpty()) {
            interviewDetails += "Meeting Link: <a href='" + interview.getInterviewLink() + "'>" + interview.getInterviewLink() + "</a><br>";
        }
        if (interview.getInterviewLocation() != null && !interview.getInterviewLocation().isEmpty()) {
            interviewDetails += "Location: " + interview.getInterviewLocation() + "<br>";
        }
        interviewDetails += "HR Contact: " + interview.getHrName() + " (" + interview.getHrEmail() + ")";

        String emailBody = "Dear " + interview.getJobApplication().getApplicantName() + ",<br><br>" +
                           "Your interview for the <strong>" + interview.getJobApplication().getJobTitle() + "</strong> position at <strong>" + interview.getJobApplication().getCompanyName() + "</strong> has been scheduled.<br><br>" +
                           "Here are the details:<br>" + interviewDetails + "<br><br>" +
                           "Please log in to your placement portal to view and book your interview slot.<br><br>" +
                           "Best regards,<br>The Placement Team";
                           
        emailService.sendEmailWithAttachment(interview.getJobApplication().getApplicantEmail(), subject, emailBody, null); // No attachment
        logger.info("Interview scheduled email sent to: {}", interview.getJobApplication().getApplicantEmail());
    }

    private void sendInterviewBookedConfirmationToApplicant(Interview interview) throws IOException {
        String subject = "Interview Slot Confirmed: " + interview.getJobApplication().getJobTitle();
        String emailBody = "Dear " + interview.getJobApplication().getApplicantName() + ",<br><br>" +
                           "Your interview slot for the <strong>" + interview.getJobApplication().getJobTitle() + "</strong> position at <strong>" + interview.getJobApplication().getCompanyName() + "</strong> has been successfully booked.<br><br>" +
                           "<strong>Your Confirmed Interview Details:</strong><br>" +
                           "Date/Time: " + interview.getStudentBookedDateTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")) + "<br>";
        if (interview.getInterviewLink() != null && !interview.getInterviewLink().isEmpty()) {
            emailBody += "Meeting Link: <a href='" + interview.getInterviewLink() + "'>" + interview.getInterviewLink() + "</a><br>";
        }
        if (interview.getInterviewLocation() != null && !interview.getInterviewLocation().isEmpty()) {
            emailBody += "Location: " + interview.getInterviewLocation() + "<br>";
        }
        emailBody += "<br>Please ensure you are prepared for the interview.<br><br>" +
                     "Best regards,<br>The Placement Team";

        emailService.sendEmailWithAttachment(interview.getJobApplication().getApplicantEmail(), subject, emailBody, null); // No attachment
        logger.info("Interview booked confirmation email sent to: {}", interview.getJobApplication().getApplicantEmail());
    }

    private void sendInterviewBookedNotificationToHR(Interview interview) throws IOException {
        String subject = "Interview Booked by Student: " + interview.getJobApplication().getApplicantName();
        String emailBody = "Hello " + interview.getHrName() + ",<br><br>" +
                           "The student, <strong>" + interview.getJobApplication().getApplicantName() + "</strong> (Roll No: " + interview.getJobApplication().getApplicantRollNo() + "), has booked their interview slot.<br><br>" +
                           "<strong>Interview Details:</strong><br>" +
                           "Job Title: " + interview.getJobApplication().getJobTitle() + "<br>" +
                           "Company: " + interview.getJobApplication().getCompanyName() + "<br>" +
                           "Applicant Email: " + interview.getJobApplication().getApplicantEmail() + "<br>" +
                           "Confirmed Date/Time: " + interview.getStudentBookedDateTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")) + "<br>";
        if (interview.getInterviewLink() != null && !interview.getInterviewLink().isEmpty()) {
            emailBody += "Meeting Link: " + interview.getInterviewLink() + "<br>";
        }
        if (interview.getInterviewLocation() != null && !interview.getInterviewLocation().isEmpty()) {
            emailBody += "Location: " + interview.getInterviewLocation() + "<br>";
        }
        emailBody += "<br>Please prepare accordingly.<br><br>" +
                     "Regards,<br>Placement Portal System";

        emailService.sendEmailWithAttachment(interview.getHrEmail(), subject, emailBody, null); // No attachment
        logger.info("HR notification email sent for booked interview to: {}", interview.getHrEmail());
    }

    // --- NO CHANGES to the methods below ---
    public List<Interview> getInterviewsForApplicant(String applicantEmail) {
        return interviewRepository.findByJobApplication_ApplicantEmail(applicantEmail);
    }

    public Optional<Interview> getInterviewById(Long id) {
        return interviewRepository.findById(id);
    }
}