package com.example.AsmGD1.controller.FaceID;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/acvstore")
public class FacePageController {

    @GetMapping("/register-face")
    public String showRegisterFacePage() {
        return "WebQuanLy/register-face"; // KHÔNG có .html
    }
}