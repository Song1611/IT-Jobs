package com.itjob.service.impl;

import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    private static final String FROM = "IT Job <doansong1611@gmail.com>";

    @Override
    public void sendVerifyEmail(String email, String otp) {
        String subject = "Verify your email - IT Job";
        String html = buildOtpHtml(otp, "Verification Code", "Verify your email address");
        send(email, subject, html);
    }

    @Override
    public void sendForgotPasswordOtp(String email, String otp) {
        String subject = "Reset your password - IT Job";
        String html = buildOtpHtml(otp, "Password Reset Code", "Reset your password");
        send(email, subject, html);
    }

    @Override
    public void sendChangePasswordOtp(String email, String otp) {
        String subject = "Confirm password change - IT Job";
        String html = buildOtpHtml(otp, "Password Change Code", "Confirm your new password");
        send(email, subject, html);
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(FROM);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.debug("Email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
            throw new AppException(ErrorCode.EMAIL_SENDING_FAILED);
        }
    }

    private String buildOtpHtml(String otp, String title, String subtitle) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,sans-serif">
                    <table style="width:100%%;max-width:480px;margin:40px auto;background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08)">
                        <tr>
                            <td style="background-color:#2563eb;padding:24px;text-align:center">
                                <h1 style="color:#ffffff;margin:0;font-size:22px">IT Job</h1>
                            </td>
                        </tr>
                        <tr>
                            <td style="padding:32px 24px;text-align:center">
                                <p style="color:#6b7280;font-size:14px;margin:0 0 4px">%s</p>
                                <h2 style="color:#111827;font-size:28px;margin:16px 0">%s</h2>
                                <div style="background-color:#f3f4f6;border-radius:8px;padding:16px;margin:16px 0;font-size:32px;letter-spacing:8px;font-weight:bold;color:#2563eb">%s</div>
                                <p style="color:#9ca3af;font-size:12px;margin:16px 0 0">This code expires in 5 minutes.</p>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(subtitle, title, otp);
    }
}
