package com.mfsys.Automatic.Email.controller;

import com.mfsys.Automatic.Email.model.FetchOrderTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.mfsys.Automatic.Email.service.EmailService;

import java.util.List;

@RestController
@RequestMapping("/")
public class EmailController {
    private EmailService emailService;

    @Autowired
    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("sendEmail")
    public void sendEmail(){
        emailService.sendEmail();
    }

    @PostMapping("viewEmail")
    public void viewEmail(){
        emailService.viewEmail();
    }

    @PostMapping("getText")
    public List<FetchOrderTable> getText(){return emailService.printText();}
}
