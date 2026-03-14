package org.example.notesapp.controller;

import org.example.notesapp.model.Note;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home(){
        return "Backend is running";
    }

}