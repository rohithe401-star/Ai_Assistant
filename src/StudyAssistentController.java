package com.research.ai.assistant;


import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(value = "*")
@AllArgsConstructor
public class StudyAssistantController {

    private final StudyAssistantService studyAssistantService;


//    public ResearchController(ResearchService researchService) {
//        this.researchService = researchService;
//    }

    @PostMapping("/assist")
    public ResponseEntity<String> processContent(@RequestBody StudyRequest request) {
        try{
            String result = studyAssistantService.processContent(request);
            return ResponseEntity.ok(result);

        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());

        }

    }
