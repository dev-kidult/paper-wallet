package com.sweden.webwallet.Component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

@Component
@PropertySource("classpath:SMTP.properties")
public class MailService {

    @Autowired
    JavaMailSender javaMailSender;

    @Value("${mail.smtp.mail}")
    String form;

    public boolean sendMail(String to, String subject, String content) {
        MimeMessagePreparator preparator = new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws Exception {
                mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
                mimeMessage.setFrom(new InternetAddress(form));
                mimeMessage.setSubject(subject);
                mimeMessage.setText(content, "utf-8", "html");
            }
        };

        try {
            javaMailSender.send(preparator);
            return true;
        } catch (MailException me) {
            System.err.println(me.toString());
            me.printStackTrace();
            return false;
        }
    }
}
