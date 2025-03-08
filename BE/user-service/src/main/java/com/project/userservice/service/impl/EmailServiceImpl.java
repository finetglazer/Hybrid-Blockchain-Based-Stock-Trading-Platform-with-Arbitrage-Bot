package com.project.userservice.service.impl;

import com.project.userservice.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendVerificationEmail(String toEmail, String token) {
        String subject = "Verify your account";
        String verificationLink = "http://localhost:8081/api/v1/auth/verify?token=" + token;

        String body = "Please click the following link to verify: \n" + verificationLink;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String token) {
        String subject = "Reset your password";
        String resetLink = "http://localhost:8081/users/api/v1/auth/reset-password?token=" + token;

        String body = "Please click the following link to reset your password: \n" + resetLink;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);

    }

    @Override
    public void sendConcurrentLoginNotification(String toEmail, String oldIp, String newIp) {
        String subject = "Concurrent Login Detected";
        String text = String.format("We detected a login from a new IP address (%s) while your account is active from another IP (%s). "
                + "If this wasn't you, please secure your account immediately.", newIp, oldIp);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

}
