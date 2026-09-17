package com.bteam.platform.adapter.mail;

import com.bteam.platform.core.auth.port.MailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmtpMailSender implements MailSender {

    private final JavaMailSender mailSender;

    @Override
    public void sendResetPasswordEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("Dat lai mat khau");
        message.setText("""
            Xin chao,

            Ban da yeu cau dat lai mat khau.

            Mã đổi lại mật khẩu là: 
            %s

            mã này se het han sau 30 phut.
            """.formatted(token));

        mailSender.send(message);
    }
}
