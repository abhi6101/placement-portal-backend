package com.abhi.authProject.service;

import com.abhi.authProject.model.ApplicationStatus;
import com.abhi.authProject.model.JobApplication;
import com.abhi.authProject.model.JobApplicationRequest1;
import com.abhi.authProject.model.JobDetails;
import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.JobApplicationRepository;
import com.abhi.authProject.repo.JobDetailsRepo;
import com.abhi.authProject.repo.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JobApplicationServiceTest {

    @Mock
    private SendGridEmailService sendGridEmailService;

    @Mock
    private EmailService emailService;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private UserRepo userRepo;

    @Mock
    private JobDetailsRepo jobDetailsRepo;

    @InjectMocks
    private JobApplicationService jobApplicationService;

    @BeforeEach
    void setUp() {
        // Set values for @Value fields
        ReflectionTestUtils.setField(jobApplicationService, "recipientEmail", "admin@test.com");
        ReflectionTestUtils.setField(jobApplicationService, "resumeStorageDirectory", "tmp/resumes");
    }

    @Test
    void processJobApplication_ShouldSendAdminEmail() throws IOException {
        // Mock Resume
        MultipartFile resume = mock(MultipartFile.class);
        when(resume.getOriginalFilename()).thenReturn("resume.pdf");
        when(resume.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[0]));

        // Arrange
        JobApplicationRequest1 request = new JobApplicationRequest1(
                "1", "Dev", "Google", "Alice", "alice@test.com", "123", "R1", "Cov", resume);

        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        jobApplicationService.processJobApplication(request);

        // Assert
        // Verify Applicant Confirmation
        verify(sendGridEmailService).sendEmailWithAttachment(eq("alice@test.com"), anyString(), anyString(),
                anyString());
        // Verify Admin Notification
        verify(sendGridEmailService).sendEmailWithAttachment(eq("admin@test.com"), anyString(), anyString(),
                anyString());
    }

    @Test
    void updateApplicationStatus_ShouldUseRealEmail_AndIncludeInterviewDetails() throws IOException {
        // Arrange
        Long appId = 1L;
        JobApplication application = new JobApplication();
        application.setId(appId);
        application.setApplicantEmail("@alice"); // Username stored
        application.setApplicantName("Alice");
        application.setJobTitle("Dev");
        application.setCompanyName("Google");
        application.setJobId("101");
        application.setAppliedAt(LocalDateTime.now());

        when(jobApplicationRepository.findById(appId)).thenReturn(Optional.of(application));
        when(jobApplicationRepository.save(any(JobApplication.class))).thenReturn(application);

        // Mock User resolution
        Users user = new Users();
        user.setEmail("alice@realemail.com");
        when(userRepo.findByUsername("@alice")).thenReturn(Optional.of(user));

        // Mock Job Details
        JobDetails jobDetails = new JobDetails();
        jobDetails.setInterview_details("{\"round\":\"Technical\"}");
        when(jobDetailsRepo.findById(101)).thenReturn(Optional.of(jobDetails));

        // Act
        jobApplicationService.updateApplicationStatus(appId, ApplicationStatus.SHORTLISTED);

        // Assert
        // Verify email sent to REAL email ("alice@realemail.com") not "@alice"
        // And Verify sendAcceptanceEmail is called (because SHORTLISTED)
        verify(emailService).sendAcceptanceEmail(
                eq("alice@realemail.com"),
                eq("Alice"),
                eq("Dev"),
                eq("Google"),
                eq("{\"round\":\"Technical\"}"));
    }
}
