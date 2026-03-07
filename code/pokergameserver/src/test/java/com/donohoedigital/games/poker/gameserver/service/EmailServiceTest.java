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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void sendVerificationEmail_mailSenderException_doesNotThrow() {
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        // Should not propagate the exception
        assertThatCode(() -> emailService.sendVerificationEmail("user@example.com", "alice", "tok789"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendVerificationEmail_setsFromAddress() {
        emailService.sendVerificationEmail("user@example.com", "alice", "tok");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getFrom()).isEqualTo("noreply@ddpoker.com");
    }

    @Test
    void sendVerificationEmail_bodyContainsUsername() {
        emailService.sendVerificationEmail("user@example.com", "alice", "tok");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).contains("Hi alice");
    }

    @Test
    void sendPasswordResetEmail_bodyContainsExpiry() {
        emailService.sendPasswordResetEmail("user@example.com", "bob", "reset-tok");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).contains("Hi bob");
        assertThat(captor.getValue().getText()).contains("password reset");
    }

    @Test
    void sendEmailChangeConfirmation_bodyContainsConfirmMessage() {
        emailService.sendEmailChangeConfirmation("new@example.com", "carol", "change-tok");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).contains("Hi carol");
        assertThat(captor.getValue().getText()).contains("new DD Poker email");
    }

    @Test
    void sendPasswordResetEmail_nullMailSender_doesNotThrow() {
        EmailService noMailService = new EmailService(null, "noreply@ddpoker.com", "https://poker.example.com");
        assertThatCode(() -> noMailService.sendPasswordResetEmail("user@example.com", "alice", "tok"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendEmailChangeConfirmation_nullMailSender_doesNotThrow() {
        EmailService noMailService = new EmailService(null, "noreply@ddpoker.com", "https://poker.example.com");
        assertThatCode(() -> noMailService.sendEmailChangeConfirmation("user@example.com", "alice", "tok"))
                .doesNotThrowAnyException();
    }
}
