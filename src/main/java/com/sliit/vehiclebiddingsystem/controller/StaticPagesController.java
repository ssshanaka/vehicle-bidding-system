package com.sliit.vehiclebiddingsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaticPagesController {

    @GetMapping("/contact")
    public String contactPage(Model model) {
        model.addAttribute("pageTitle", "Contact Us - Lanka Auto Traders");
        return "contact";
    }

    @GetMapping("/privacy")
    public String privacyPage(Model model) {
        model.addAttribute("pageTitle", "Privacy Policy - Lanka Auto Traders");
        return "privacy";
    }

    @GetMapping("/terms")
    public String termsPage(Model model) {
        model.addAttribute("pageTitle", "Terms of Service - Lanka Auto Traders");
        return "terms";
    }
}
