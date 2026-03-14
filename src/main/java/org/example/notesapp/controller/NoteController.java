package org.example.notesapp.controller;

import org.example.notesapp.model.Note;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/notes")
@CrossOrigin(origins = "*")
public class NoteController {

    private List<Note> notes = new ArrayList<>();
    private AtomicLong counter = new AtomicLong();

    @GetMapping
    public List<Note> getNotes() {
        return notes;
    }

    @PostMapping
    public Note addNote(@RequestBody Note note) {
        Note newNote = new Note(counter.incrementAndGet(), note.getText());
        notes.add(newNote);
        return newNote;
    }
}