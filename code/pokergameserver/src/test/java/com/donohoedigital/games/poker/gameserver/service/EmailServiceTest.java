package com.donohoedigital.games.poker.gameserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    JavaMailSender mailSender;

    EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, "noreply@ddpoker.com", "https://poker.example.com");
    }

    @Test
    void sendVerificationEmail_sendsToCorrectAddress() {
        emailService.sendVerificationEmail("user@example.com", "alice", "tok123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertThat(msg.getTo()).containsExactly("user@example.com");
        assertThat(msg.getSubject()).isEqualTo("Verify your DD Poker account");
        assertThat(msg.getText()).contains("https://poker.example.com/verify-email?token=tok123");
        assertThat(msg.getText()).contains("7 days");
    }

    @Test
    void sendPasswordResetEmail_sendsToCorrectAddress() {
        emailService.sendPasswordResetEmail("user@example.com", "alice", "reset-tok");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertThat(msg.getTo()).containsExactly("user@example.com");
        assertThat(msg.getSubject()).isEqualTo("Reset your DD Poker password");
        assertThat(msg.getText()).contains("https://poker.example.com/reset-password?token=reset-tok");
        assertThat(msg.getText()).contains("1 hour");
    }

    @Test
    void sendEmailChangeConfirmation_sendsToNewAddress() {
        emailService.sendEmailChangeConfirmation("new@example.com", "alice", "change-tok");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertThat(msg.getTo()).containsExactly("new@example.com");
        assertThat(msg.getSubject()).isEqualTo("Confirm your new DD Poker email address");
        assertThat(msg.getText()).contains("https://poker.example.com/verify-email?token=change-tok");
    }

    @Test
    void sendVerificationEmail_nullMailSender_logsInsteadOfThrowing() {
        EmailService noMailService = new EmailService(null, "noreply@ddpoker.com", "https://poker.example.com");
        // Should not throw — falls back to logging
        noMailService.sendVerificationEmail("user@example.com", "alice", "tok456");
        verifyNoInteractions(mailSender);
    }
}
