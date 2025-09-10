package com.example.AsmGD1.controller.FaceID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/acvstore")
public class FaceAuthPageController {

    @GetMapping("/verify-face")
    public String showFaceAuthPage(Model model, Authentication authentication) {
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
        } else {
            model.addAttribute("username", "anonymous");
        }
        return "WebQuanLy/verify-face";
    }

}
