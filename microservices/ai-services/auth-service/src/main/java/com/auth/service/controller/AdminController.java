package com.auth.service.controller;

import com.auth.service.model.Auth;
import com.auth.service.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final AuthService authService;

    public AdminController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public String showApiKeyForm(Model model) {
        model.addAttribute("apiKeys", authService.getAllApiKeys());
        model.addAttribute("maxApiKeys", authService.getMaxApiKeys());
        model.addAttribute("currentCount", authService.countApiKeys());
        return "api-keys";
    }

    @PostMapping("/create")
    public String createApiKey(@RequestParam("apiKey") String apiKey, Model model) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                model.addAttribute("error", "API Key cannot be empty.");
                return "api-keys";
            }
            Auth auth = new Auth();
            auth.setApiKey(apiKey.trim());
            authService.createApiKey(auth);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to create API key: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/delete")
    public String deleteApiKey(@RequestParam("apiKey") String apiKey, Model model) {
        try {
            authService.deleteApiKey(apiKey);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to delete API key: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/update-limit")
    public String updateMaxApiKeys(@RequestParam("maxApiKeys") int maxApiKeys, Model model) {
        try {
            if (maxApiKeys < authService.countApiKeys()) {
                model.addAttribute("error", "New limit cannot be less than current number of API keys.");
                return "api-keys";
            }
            authService.setMaxApiKeys(maxApiKeys);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to update API key limit: " + e.getMessage());
        }
        return "redirect:/admin";
    }
}