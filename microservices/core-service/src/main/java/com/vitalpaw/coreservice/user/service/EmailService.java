package com.vitalpaw.coreservice.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendConfirmationEmail(String to, String token) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("VitalPaw - Confirma tu cuenta");
        helper.setText(String.format("""
            <h2>Bienvenido a VitalPaw</h2>
            <p>Para confirmar tu cuenta, usa el siguiente código:</p>
            <h3>%s</h3>
            <p>Este código expira en 24 horas.</p>
            <p>Si no solicitaste esta acción, ignora este correo.</p>
            """, token), true);

        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String to, String token) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("VitalPaw - Restablecer contraseña");
        helper.setText(String.format("""
            <h2>Restablecimiento de contraseña</h2>
            <p>Para restablecer tu contraseña, usa el siguiente código:</p>
            <h3>%s</h3>
            <p>Este código expira en 1 hora.</p>
            <p>Si no solicitaste esta acción, ignora este correo.</p>
            """, token), true);

        mailSender.send(message);
    }
}