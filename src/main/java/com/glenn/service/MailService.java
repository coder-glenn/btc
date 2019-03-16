package com.glenn.service;

import com.glenn.domain.MailProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.Map;

@Service
public class MailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private MailProperties mailProperties;

    public void sendSimpleMail(String subject, String text) {

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(mailProperties.getFrom());
        mailMessage.setTo(mailProperties.getTo());
        mailMessage.setCc(mailProperties.getCc());
        mailMessage.setSubject(subject);
        mailMessage.setText(text);

        javaMailSender.send(mailMessage);
    }


    public void sendHtmlMail(String subject, String text, Map<String, String> attachmentMap) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        //是否发送的邮件是富文本（附件，图片，html等）
        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);

        messageHelper.setFrom(mailProperties.getFrom());
        messageHelper.setTo(mailProperties.getTo());
        messageHelper.setCc(mailProperties.getCc());
        messageHelper.setSubject(subject);
        messageHelper.setText(text, true);//重点，默认为false，显示原始html代码，无效果

        if(attachmentMap != null){
            attachmentMap.entrySet().stream().forEach(entrySet -> {
                try {
                    File file = new File(entrySet.getValue());
                    if(file.exists()){
                        messageHelper.addAttachment(entrySet.getKey(), new FileSystemResource(file));
                    }
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            });
        }

        javaMailSender.send(mimeMessage);
    }
}
