package com.sliit.vehiclebiddingsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/help")
public class HelpController {

    @GetMapping
    public String helpCenter(Model model) {
        // Add any necessary data to the model
        model.addAttribute("pageTitle", "Help Center - Lanka Auto Traders");
        return "help";
    }
}

