package com.project.userservice.service.impl;

import com.project.userservice.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${resetBaseUrl}")
    private String resetBaseUrl;

    private String frontendUrl = "http://127.0.0.1:5173";

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


    @Async
    @Override
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reset Your Password");

            // Create reset password URL that points to frontend (not backend)
            String resetUrl = frontendUrl + "/reset-password?token=" + token;

            String htmlContent =
                    "<!DOCTYPE html>" +
                            "<html lang=\"en\">" +
                            "<head>" +
                            "    <meta charset=\"UTF-8\">" +
                            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                            "    <title>Password Reset</title>" +
                            "    <style>" +
                            "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }" +
                            "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                            "        .header { background-color: #3498db; color: #ffffff; padding: 20px; text-align: center; }" +
                            "        .content { padding: 20px; background-color: #f9f9f9; }" +
                            "        .button { display: inline-block; background-color: #3498db; color: #ffffff; text-decoration: none; " +
                            "                  padding: 10px 20px; border-radius: 4px; margin-top: 15px; }" +
                            "        .warning { color: #e74c3c; font-weight: bold; }" +
                            "        .footer { padding: 15px; text-align: center; font-size: 12px; color: #777; }" +
                            "    </style>" +
                            "</head>" +
                            "<body>" +
                            "    <div class=\"container\">" +
                            "        <div class=\"header\">" +
                            "            <h2>Password Reset Request</h2>" +
                            "        </div>" +
                            "        <div class=\"content\">" +
                            "            <p>Hello,</p>" +
                            "            <p>We received a request to reset your password. Click the button below to create a new password:</p>" +
                            "            <p style=\"text-align: center;\">" +
                            "                <a href=\"" + resetUrl + "\" class=\"button\">Reset Password</a>" +
                            "            </p>" +
                            "            <p>If the button doesn't work, you can also copy and paste the following link into your browser:</p>" +
                            "            <p>" + resetUrl + "</p>" +
                            "            <p class=\"warning\">This link will expire in 1 hour for security reasons.</p>" +
                            "            <p>If you didn't request a password reset, please ignore this email or contact support if you have concerns.</p>" +
                            "            <p>Best regards,<br>Your Application Team</p>" +
                            "        </div>" +
                            "        <div class=\"footer\">" +
                            "            <p>This is an automated email. Please do not reply to this message.</p>" +
                            "        </div>" +
                            "    </div>" +
                            "</body>" +
                            "</html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);

            logger.info("Password reset email sent to: {}", toEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendConcurrentLoginNotification(String toEmail, String oldIp, String newIp) {
        String subject = "SECURITY ALERT: Concurrent Login Detected";

        try {
            // Create a MIME message for HTML content
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Set email properties
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);

            // HTML content with better formatting
            String htmlContent = String.format(
                    "<!DOCTYPE html>" +
                            "<html>" +
                            "<head>" +
                            "<meta charset=\"UTF-8\">" +
                            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                            "<title>Security Alert</title>" +
                            "<style type=\"text/css\">" +
                            "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333333; max-width: 600px; margin: 0 auto; }" +
                            ".container { border: 1px solid #ffc107; border-radius: 5px; padding: 20px; background-color: #fff; }" +
                            ".header { background-color: #ffc107; color: #000; padding: 15px; border-radius: 5px 5px 0 0; margin: -20px -20px 20px -20px; text-align: center; }" +
                            ".warning-icon { font-size: 48px; margin-bottom: 20px; color: #ffc107; text-align: center; }" +
                            ".alert-message { margin-bottom: 20px; padding: 15px; background-color: #fff3cd; border: 1px solid #ffeeba; border-radius: 4px; color: #856404; }" +
                            ".footer { margin-top: 30px; font-size: 12px; color: #777; text-align: center; }" +
                            "ol { margin-left: 20px; padding-left: 20px; }" +
                            "ol li { margin-bottom: 10px; }" +
                            "a { color: #0066cc; }" +
                            "</style>" +
                            "</head>" +
                            "<body>" +
                            "<div class=\"container\">" +
                            "<div class=\"header\">" +
                            "<h2>⚠️ Security Alert</h2>" +
                            "</div>" +
                            "<h3>Concurrent Login Detected</h3>" +
                            "<div class=\"alert-message\">" +
                            "<p>We detected a login attempt from a <strong>new IP address (%s)</strong> while your account is currently active from another IP address <strong>(%s)</strong>.</p>" +
                            "</div>" +
                            "<p>If this wasn't you, your account security may be at risk.</p>" +
                            "<h4>What You Should Do:</h4>" +
                            "<ol>" +
                            "<li>Check if someone else is using your account with your permission.</li>" +
                            "<li>Change your password immediately if you don't recognize this activity.</li>" +
                            "<li>Enable two-factor authentication for additional security.</li>" +
                            "<li>Review recent account activity for any suspicious behavior.</li>" +
                            "</ol>" +
                            "<p>If you need further assistance, please contact our support team:</p>" +
                            "<p>" +
                            "<a href=\"mailto:vinhhiepbn2003.work@gmail.com\">vinhhiepbn2003.work@gmail.com</a><br>" +
                            "<a href=\"mailto:hamy0401@gmail.com\">hamy0401@gmail.com</a><br>" +
                            "<a href=\"mailto:tranhung10122003@gmail.com\">tranhung10122003@gmail.com</a>" +
                            "</p>" +
                            "<div class=\"footer\">" +
                            "<p>&copy; 2025 E05 students. All rights reserved.</p>" +
                            "</div>" +
                            "</div>" +
                            "</body>" +
                            "</html>",
                    newIp, oldIp);

            // Set HTML content
            helper.setText(htmlContent, true);

            // Send the email
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            // Log the error
            System.err.println("Failed to send security notification email: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
