    package com.itjob.unit.service;

    import com.itjob.exception.AppException;
    import com.itjob.exception.ErrorCode;
    import com.itjob.service.impl.EmailServiceImpl;
    import jakarta.mail.Message;
    import jakarta.mail.MessagingException;
    import jakarta.mail.Multipart;
    import jakarta.mail.internet.InternetAddress;
    import jakarta.mail.internet.MimeMessage;
    import org.junit.jupiter.api.DisplayName;
    import org.junit.jupiter.api.Test;
    import org.junit.jupiter.api.extension.ExtendWith;
    import org.mockito.ArgumentCaptor;
    import org.mockito.InjectMocks;
    import org.mockito.Mock;
    import org.mockito.junit.jupiter.MockitoExtension;
    import org.springframework.mail.javamail.JavaMailSender;

    import static org.assertj.core.api.Assertions.assertThat;
    import static org.assertj.core.api.Assertions.assertThatThrownBy;
    import static org.mockito.ArgumentMatchers.anyString;
    import static org.mockito.Mockito.doThrow;
    import static org.mockito.Mockito.verify;
    import static org.mockito.Mockito.when;

    @ExtendWith(MockitoExtension.class)
    @DisplayName("Unit - EmailServiceImpl")
    class EmailServiceImplTest {

        private static final String TO = "candidate@example.com";

        @Mock
        private JavaMailSender mailSender;

        @Mock
        private MimeMessage mimeMessage;

        @InjectMocks
        private EmailServiceImpl emailService;

        @Test
        @DisplayName("sendVerifyEmail -> sends email with verification subject, recipient and OTP in body")
        void sendVerifyEmailSendsMessageContainingOtp() throws Exception {
            // Arrange
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act
            emailService.sendVerifyEmail(TO, "123456");

            // Assert
            verify(mailSender).send(mimeMessage);
            assertSubject("Verify your email - IT Job");
            assertRecipient();
            assertThat(bodyContent()).contains("123456");
        }

        @Test
        @DisplayName("sendForgotPasswordOtp -> sends email with reset subject, recipient and OTP in body")
        void sendForgotPasswordOtpSendsMessageContainingOtp() throws Exception {
            // Arrange
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act
            emailService.sendForgotPasswordOtp(TO, "654321");

            // Assert
            verify(mailSender).send(mimeMessage);
            assertSubject("Reset your password - IT Job");
            assertRecipient();
            assertThat(bodyContent()).contains("654321");
        }

        @Test
        @DisplayName("sendChangePasswordOtp -> sends email with change-confirm subject, recipient and OTP in body")
        void sendChangePasswordOtpSendsMessageContainingOtp() throws Exception {
            // Arrange
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act
            emailService.sendChangePasswordOtp(TO, "999999");

            // Assert
            verify(mailSender).send(mimeMessage);
            assertSubject("Confirm password change - IT Job");
            assertRecipient();
            assertThat(bodyContent()).contains("999999");
        }

        @Test
        @DisplayName("sendVerifyEmail -> MessagingException during message assembly throws EMAIL_SENDING_FAILED")
        void sendVerifyEmailThrowsAppExceptionWhenMessagingFails() throws Exception {
            // Arrange
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new MessagingException("boom"))
                    .when(mimeMessage).setSubject(anyString(), anyString());

            // Act & Assert
            assertThatThrownBy(() -> emailService.sendVerifyEmail(TO, "123456"))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_SENDING_FAILED);
        }

        private void assertSubject(String subject) throws MessagingException {
            verify(mimeMessage).setSubject(subject, "UTF-8");
        }

        private void assertRecipient() throws MessagingException {
            verify(mimeMessage).setRecipient(Message.RecipientType.TO, new InternetAddress(TO));
        }

        private String bodyContent() throws Exception {
            // MimeMessageHelper(multipart=true) stores the body inside a nested MimeMultipart
            ArgumentCaptor<Multipart> captor = ArgumentCaptor.forClass(Multipart.class);
            verify(mimeMessage).setContent(captor.capture());
            Object content = captor.getValue().getBodyPart(0).getContent();
            while (content instanceof Multipart nested) {
                content = nested.getBodyPart(0).getContent();
            }
            return content.toString();
        }
    }
