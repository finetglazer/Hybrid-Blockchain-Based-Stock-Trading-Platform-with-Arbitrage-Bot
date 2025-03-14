package com.project.userservice.service.impl;

import com.project.userservice.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;

@Service
public class EmailServiceImpl implements EmailService {

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${resetBaseUrl}")
    private String resetBaseUrl;

    @Override
    public void sendVerificationEmail(String toEmail, String token) {
        String subject = "Verify your account";
        String verificationLink = resetBaseUrl + "/users/api/v1/auth/verify?token=" + token;

        // HTML email content
        String htmlBody = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Email Verification</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            color: #333333;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .container {
                            background-color: #f9f9f9;
                            border-radius: 5px;
                            padding: 30px;
                            border: 1px solid #dddddd;
                        }
                        .header {
                            text-align: center;
                            padding-bottom: 20px;
                            border-bottom: 1px solid #eeeeee;
                            margin-bottom: 20px;
                        }
                        .logo {
                            font-size: 24px;
                            font-weight: bold;
                            color: #4285f4;
                        }
                        h1 {
                            color: #333333;
                            font-size: 22px;
                        }
                        p {
                            margin-bottom: 15px;
                        }
                        .button {
                            display: inline-block;
                            background-color: #4285f4;
                            color: white !important;
                            text-decoration: none;
                            padding: 12px 30px;
                            border-radius: 4px;
                            font-weight: bold;
                            margin: 20px 0;
                            text-align: center;
                        }
                        .footer {
                            margin-top: 30px;
                            text-align: center;
                            font-size: 12px;
                            color: #888888;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">YourCompany</div>
                        </div>
                        
                        <h1>Email Verification</h1>
                        <p>Hello,</p>
                        <p>Thank you for registering with us. Please verify your email address to complete your registration and activate your account.</p>
                        
                        <div style="text-align: center;">
                            <a href="%s" class="button">Verify Email Address</a>
                        </div>
                        
                        <p>If the button above doesn't work, you can also verify by copying and pasting this link into your browser:</p>
                        <p style="word-break: break-all;"><a href="%s">%s</a></p>
                        
                        <p>If you didn't create an account, please ignore this email.</p>
                        
                        <div class="footer">
                            <p>&copy; 2025 YourCompany. All rights reserved.</p>
                            <p>123 Main Street, City, Country</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(verificationLink, verificationLink, verificationLink);

        try {
            // Create a MIME message
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true indicates HTML content

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
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
