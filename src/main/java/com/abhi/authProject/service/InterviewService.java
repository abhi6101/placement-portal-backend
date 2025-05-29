package com.abhi.authProject.service;

import com.abhi.authProject.model.InterviewBookingRequest;
import com.abhi.authProject.model.InterviewScheduleRequest;
import com.abhi.authProject.model.ApplicationStatus;
import com.abhi.authProject.model.Interview;
import com.abhi.authProject.model.InterviewStatus;
import com.abhi.authProject.model.JobApplication;
import com.abhi.authProject.repo.InterviewRepository;
import com.abhi.authProject.repo.JobApplicationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public InterviewService(InterviewRepository interviewRepository,
                            JobApplicationRepository jobApplicationRepository,
                            JavaMailSender mailSender) {
        this.interviewRepository = interviewRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public Interview scheduleInterview(InterviewScheduleRequest request) throws MessagingException {
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

        // Update JobApplication status to INTERVIEW_SCHEDULED
        jobApplication.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);
        jobApplicationRepository.save(jobApplication);

        // Send email to student about scheduled interview
        sendInterviewScheduledEmailToApplicant(savedInterview);

        return savedInterview;
    }

    @Transactional
    public Interview bookInterviewSlot(Long interviewId, InterviewBookingRequest request) throws MessagingException {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found with ID: " + interviewId));

        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new IllegalStateException("Interview is not in SCHEDULED status, cannot book.");
        }

        interview.setStudentBookedDateTime(request.getStudentBookedDateTime());
        interview.setStatus(InterviewStatus.BOOKED);
        Interview updatedInterview = interviewRepository.save(interview);

        // Update JobApplication status to INTERVIEW_BOOKED
        JobApplication jobApplication = updatedInterview.getJobApplication();
        jobApplication.setStatus(ApplicationStatus.INTERVIEW_BOOKED);
        jobApplicationRepository.save(jobApplication);

        // Send confirmation to student and notification to HR
        sendInterviewBookedConfirmationToApplicant(updatedInterview);
        sendInterviewBookedNotificationToHR(updatedInterview);

        return updatedInterview;
    }

    public List<Interview> getInterviewsForApplicant(String applicantEmail) {
        return interviewRepository.findByJobApplication_ApplicantEmail(applicantEmail);
    }

    public Optional<Interview> getInterviewById(Long id) {
        return interviewRepository.findById(id);
    }

    // Helper method to send email to applicant about scheduled interview
    private void sendInterviewScheduledEmailToApplicant(Interview interview) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(senderEmail);
        helper.setTo(interview.getJobApplication().getApplicantEmail());
        helper.setSubject("Interview Scheduled for " + interview.getJobApplication().getJobTitle());

        String interviewDetails = "Company: " + interview.getJobApplication().getCompanyName() + "<br>" +
                                  "Position: " + interview.getJobApplication().getJobTitle() + "<br>" +
                                  "Scheduled Date/Time: " + interview.getScheduledDateTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")) + "<br>";
        if (interview.getInterviewLink() != null && !interview.getInterviewLink().isEmpty()) {
            interviewDetails += "Meeting Link: <a href='" + interview.getInterviewLink() + "'>" + interview.getInterviewLink() + "</a><br>";
        }
        if (interview.getInterviewLocation() != null && !interview.getInterviewLocation().isEmpty()) {
            interviewDetails += "Location: " + interview.getInterviewLocation() + "<br>";
        }
        interviewDetails += "HR Contact: " + interview.getHrName() + " (" + interview.getHrEmail() + ")<br><br>" +
                            "Please confirm your slot by visiting your portal or clicking a link (you'll need to build this link on frontend).";

        String emailBody = "Dear " + interview.getJobApplication().getApplicantName() + ",<br><br>" +
                           "Your interview for the <strong>" + interview.getJobApplication().getJobTitle() + "</strong> position at <strong>" + interview.getJobApplication().getCompanyName() + "</strong> has been scheduled.<br><br>" +
                           "Here are the details:<br>" + interviewDetails + "<br>" +
                           "Please log in to your placement portal to view and book your interview slot.<br><br>" +
                           "Best regards,<br>The Placement Team";
        helper.setText(emailBody, true);
        mailSender.send(message);
        System.out.println("Interview scheduled email sent to: " + interview.getJobApplication().getApplicantEmail());
    }

    // Helper method to send confirmation to applicant about booked slot
    private void sendInterviewBookedConfirmationToApplicant(Interview interview) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(senderEmail);
        helper.setTo(interview.getJobApplication().getApplicantEmail());
        helper.setSubject("Interview Slot Confirmed: " + interview.getJobApplication().getJobTitle());

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
        helper.setText(emailBody, true);
        mailSender.send(message);
        System.out.println("Interview booked confirmation email sent to: " + interview.getJobApplication().getApplicantEmail());
    }

    // Helper method to send notification to HR about booked slot
    private void sendInterviewBookedNotificationToHR(Interview interview) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(senderEmail);
        helper.setTo(interview.getHrEmail()); // Send to the specific HR who scheduled it
        helper.setSubject("Interview Booked by Student: " + interview.getJobApplication().getApplicantName());

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
        helper.setText(emailBody, true);
        mailSender.send(message);
        System.out.println("HR notification email sent for booked interview to: " + interview.getHrEmail());
    }
}