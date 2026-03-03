package com.donohoedigital.games.poker.gameserver.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger LOG = LogManager.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String baseUrl;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender,
            @Value("${app.email.from:noreply@ddpoker.com}") String fromAddress,
            @Value("${app.base-url:localhost}") String baseUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.baseUrl = baseUrl;
    }

    public void sendVerificationEmail(String toEmail, String username, String token) {
        String link = baseUrl + "/verify-email?token=" + token;
        String body = "Hi " + username + ",\n\n" + "Please verify your DD Poker account by clicking the link below:\n\n"
                + link + "\n\n" + "This link expires in 7 days.\n\n"
                + "If you did not create this account, you can ignore this email.\n\n" + "— The DD Poker Team";
        send(toEmail, "Verify your DD Poker account", body);
    }

    public void sendPasswordResetEmail(String toEmail, String username, String token) {
        String link = baseUrl + "/reset-password?token=" + token;
        String body = "Hi " + username + ",\n\n" + "A password reset was requested for your DD Poker account.\n\n"
                + link + "\n\n" + "This link expires in 1 hour. If you did not request a reset, ignore this email.\n\n"
                + "— The DD Poker Team";
        send(toEmail, "Reset your DD Poker password", body);
    }

    public void sendEmailChangeConfirmation(String toEmail, String username, String token) {
        String link = baseUrl + "/verify-email?token=" + token;
        String body = "Hi " + username + ",\n\n"
                + "Please confirm your new DD Poker email address by clicking the link below:\n\n" + link + "\n\n"
                + "This link expires in 7 days. If you did not request this change, ignore this email.\n\n"
                + "— The DD Poker Team";
        send(toEmail, "Confirm your new DD Poker email address", body);
    }

    private void send(String to, String subject, String body) {
        if (mailSender == null) {
            LOG.info("SMTP not configured — email to {} subject '{}' suppressed. Verification link: {}", to, subject,
                    body.lines().filter(l -> l.startsWith("http")).findFirst().orElse("(none)"));
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            LOG.error("Failed to send email to {} subject '{}': {}", to, subject, e.getMessage());
        }
    }
}
