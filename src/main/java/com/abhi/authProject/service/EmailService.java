package com.abhi.authProject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetUrl = "http://localhost:5173/reset-password?token=" + resetToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Reset Request - Placement Portal");
        message.setText("Hello,\n\n" +
                "You requested to reset your password for the Placement Portal.\n\n" +
                "Click the link below to reset your password:\n" +
                resetUrl + "\n\n" +
                "This link will expire in 1 hour.\n\n" +
                "If you didn't request this, please ignore this email.\n\n" +
                "Best regards,\n" +
                "Placement Portal Team");

        mailSender.send(message);
    }

    public void sendPasswordResetConfirmation(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Reset Successful - Placement Portal");
        message.setText("Hello,\n\n" +
                "Your password has been successfully reset.\n\n" +
                "If you didn't make this change, please contact support immediately.\n\n" +
                "Best regards,\n" +
                "Placement Portal Team");

        mailSender.send(message);
    }
}
