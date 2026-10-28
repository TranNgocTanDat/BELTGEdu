package com.example.beprojectweb.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailService {
    JavaMailSender emailSender;

    public void sendVerificationEmail(String to, String subject, String text) throws MessagingException {
        sendHtmlEmail(to, subject, text);
    }

    public void sendHtmlEmail(String to, String subject, String html) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, java.nio.charset.StandardCharsets.UTF_8.name());

        helper.setTo(to);
        helper.setSubject(subject == null ? "Thông báo công việc" : subject.replace('\r', ' ').replace('\n', ' ').trim());
        helper.setText(html, true);

        emailSender.send(message);
    }
}
