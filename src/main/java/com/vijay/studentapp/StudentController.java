package com.vijay.studentapp;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class StudentController {

    private final StudentRepository repository;

    public StudentController(StudentRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/students")
    public List<Student> getStudents() {
        return repository.findAll();
    }

    @GetMapping("/students/{id}")
    public Student getStudent(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found"
                ));
    }

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    public Student addStudent(@RequestBody Student student) {
        Student newStudent = new Student();
        newStudent.setName(student.getName());
        newStudent.setEmail(student.getEmail());
        newStudent.setCourse(student.getCourse());

        return repository.save(newStudent);
    }

    @PutMapping("/students/{id}")
    public Student updateStudent(
            @PathVariable Long id,
            @RequestBody Student student
    ) {
        Student existing = getStudent(id);

        existing.setName(student.getName());
        existing.setEmail(student.getEmail());
        existing.setCourse(student.getCourse());

        return repository.save(existing);
    }

    @DeleteMapping("/students/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudent(@PathVariable Long id) {
        Student existing = getStudent(id);
        repository.delete(existing);
    }
}
