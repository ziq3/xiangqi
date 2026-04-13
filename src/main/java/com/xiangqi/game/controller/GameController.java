package com.xiangqi.game.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GameController {

    @GetMapping("/")
    public String home() {
        return "index"; // Chỉ làm nhiệm vụ trả về file giao diện
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}