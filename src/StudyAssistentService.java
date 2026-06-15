package com.research.ai.assistant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

@Service
public class StudyAssistantService {

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.api.key}")
    private String groqApiKey;

    private WebClient webClient;
    private final ObjectMapper objectMapper;

    public StudyAssistantService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String processContent(StudyRequest request) {
        String prompt = buildPrompt(request);

        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        String response = webClient.post()
                .uri(groqApiUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + groqApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .map(errorBody -> {
                                    System.out.println("GROQ ERROR: " + errorBody);
                                    return new RuntimeException("Groq API error: " + errorBody);
                                })
                )
                .bodyToMono(String.class)
                .block();

        return extractTextFromResponse(response);
    }

    private String extractTextFromResponse(String response) {
        try {
            GroqResponse groqResponse = objectMapper.readValue(response, GroqResponse.class);
            if (groqResponse.getChoices() != null && !groqResponse.getChoices().isEmpty()) {
                return groqResponse.getChoices().get(0).getMessage().getContent();
            }
            return "No content found";
        } catch (Exception e) {
            return "Error parsing response: " + e.getMessage();
        }
    }
    private String buildPrompt(StudyRequest request) {
        String content = request.getContent() != null ? request.getContent().trim() : "";
        String goal = request.getResearchGoal() != null ? request.getResearchGoal().trim() : "General";
        String language = request.getProgrammingLanguage() != null ? request.getProgrammingLanguage().trim() : "Java";

        switch (request.getOperation()) {

            // ═══════════════════════════════
            // CORE STUDY (1-20)
            // ═══════════════════════════════

            case "summarize":
                return "You are a student study assistant. Summarize the following content clearly.\n\n" +
                        "Content: " + content + "\n\n" +
                        "Give output in this format:\n" +
                        "IN SHORT: Write 2 simple sentences.\n" +
                        "KEY POINTS: List 5 bullet points.\n" +
                        "IMPORTANT TERMS: List 3 to 5 key terms.\n" +
                        "Keep it simple for a student to understand.";

            case "keypoints":
                return "You are a study assistant. Extract exactly 10 key points from this content for exam preparation.\n\n" +
                        "Content: " + content + "\n\n" +
                        "Give output as a numbered list 1 to 10.\n" +
                        "Each point must be short, clear, and exam-ready.\n" +
                        "Do not add extra text.";

            case "notes":
                return "You are a study assistant. Create structured study notes for the topic: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "Format the notes with clear sections and subsections.\n" +
                        "Each section must have a heading, key points, and what to remember.\n" +
                        "Make it perfect for revision.";

            case "define":
                return "You are a study assistant. Find and define all important terms in the content below.\n\n" +
                        "Content: " + content + "\n\n" +
                        "For each term give:\n" +
                        "TERM: name\n" +
                        "DEFINITION: one simple line\n" +
                        "EXAMPLE: one easy real example\n\n" +
                        "Define at least 5 terms.";

            case "examples":
                return "You are a study assistant. Give real world examples for: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "Give at least 3 examples.\n" +
                        "For each example explain: what it is, where it is used, and why it relates to the topic.\n" +
                        "Use simple language a student can understand.";

            case "compare":
                return "You are a study assistant. Compare the topics mentioned in: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "Create a comparison with these points: Definition, Key difference, When to use, Advantage, Disadvantage.\n" +
                        "Show both sides clearly.\n" +
                        "End with a one-line summary of which is better and when.";

            case "formulas":
                return "You are a study assistant. List all formulas related to: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "For each formula give:\n" +
                        "FORMULA: the equation\n" +
                        "VARIABLES: what each letter means\n" +
                        "WHEN TO USE: the situation\n" +
                        "EXAMPLE: one solved example\n\n" +
                        "List all formulas found in the content.";

            case "diagram":
                return "You are a study assistant. Explain the following content using a text-based diagram or flowchart.\n\n" +
                        "Content: " + content + "\n\n" +
                        "Draw a simple ASCII diagram using arrows and boxes.\n" +
                        "Label each part clearly.\n" +
                        "After the diagram, explain each part in 1 line.\n" +
                        "Make it easy to visualize for a student.";

            case "timeline":
                return "You are a study assistant. Create a chronological timeline for: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "List events in order from oldest to newest.\n" +
                        "For each event give: date or period, what happened, why it was important.\n" +
                        "Highlight the most important milestone.";

            case "pros_cons":
                return "You are a study assistant. List pros and cons for: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "Give at least 4 pros and 4 cons.\n" +
                        "Each point must have a short explanation.\n" +
                        "End with a one-line verdict.";

            case "why_how":
                return "You are a study assistant. Explain WHY and HOW for: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "WHY section: explain the reason and purpose in simple words.\n" +
                        "HOW section: give step by step process with clear steps.\n" +
                        "Use simple language suitable for a student.";

            case "difficult":
                return "You are a study assistant. Simplify this difficult concept for a student.\n\n" +
                        "Topic: " + goal + "\n" +
                        "Content: " + content + "\n\n" +
                        "Explain it like you are teaching a 10 year old child.\n" +
                        "Use very simple words, a relatable analogy, and one easy example.\n" +
                        "Then give the key thing to remember in one line.";

            case "realworld":
                return "You are a study assistant. Give real world applications for: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "Give at least 4 real world uses.\n" +
                        "For each: name the industry, describe how it is used, and why it matters.\n" +
                        "Make it interesting and relatable for a student.";

            case "related":
                return "You are a study assistant. Suggest related topics to study after: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "List 5 related topics.\n" +
                        "For each topic explain why it is related and what the student will learn.\n" +
                        "Give a recommended study order from easiest to hardest.";

            case "flashcards":
                return "You are a study assistant. Create 10 flashcards from this content for: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "For each flashcard give:\n" +
                        "QUESTION: a clear exam-style question\n" +
                        "ANSWER: a short direct answer\n\n" +
                        "Make questions cover the most important concepts.";

            case "mnemonics":
                return "You are a study assistant. Create memory tricks and mnemonics to remember: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "Create at least 2 mnemonics.\n" +
                        "For each mnemonic: give the word or phrase, explain what each letter or part stands for, and how it helps remember the concept.\n" +
                        "Make them fun and easy to recall.";

            case "study_plan":
                return "You are a study assistant. Create a 7 day study plan for: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "For each day give: topic to study, time needed in minutes, and specific task to complete.\n" +
                        "Include revision days and practice days.\n" +
                        "Make it realistic for a student with other subjects too.";

            case "check_understanding":
                return "You are a study assistant. Test the student's understanding of: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "List 3 must-know concepts from this topic.\n" +
                        "Ask 3 questions to check understanding.\n" +
                        "Give the correct answers after each question.\n" +
                        "Tell the student what they must revise if they got it wrong.";

            case "quiz":
                return "You are a study assistant. Create a quiz with 5 multiple choice questions from: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "For each question give:\n" +
                        "QUESTION: clear question\n" +
                        "A B C D options\n" +
                        "CORRECT ANSWER: the letter\n" +
                        "EXPLANATION: why that answer is correct in one line\n\n" +
                        "Make questions exam-level difficulty.";

            case "essay_write":
                return "You are a study assistant. Write a complete essay on: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "Structure the essay as:\n" +
                        "INTRODUCTION: 2 sentences introducing the topic\n" +
                        "BODY PARAGRAPH 1: first main point with explanation\n" +
                        "BODY PARAGRAPH 2: second main point with explanation\n" +
                        "BODY PARAGRAPH 3: third main point with explanation\n" +
                        "CONCLUSION: summarize in 2 sentences\n\n" +
                        "Use formal academic language.";

            // ═══════════════════════════════
            // EXAM PREP (21-35)
            // ═══════════════════════════════

            case "exam_questions":
                return "You are an exam preparation assistant. Generate likely exam questions for: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "Give 3 MCQ questions with 4 options and correct answer.\n" +
                        "Give 3 short answer questions with 2 line answers.\n" +
                        "Give 2 long answer questions with key points to cover.\n" +
                        "Focus on questions that are most likely to appear in exam.";

            case "mistakes":
                return "You are a study assistant. List common mistakes students make in: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "Give at least 5 common mistakes.\n" +
                        "For each mistake: describe the wrong way, explain why it is wrong, give the correct approach.\n" +
                        "Focus on mistakes that cost marks in exam.";

            case "tricks":
                return "You are a study assistant. Give shortcuts and tricks to solve problems faster in: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "Give at least 5 tricks.\n" +
                        "For each trick: explain what it is, when to use it, and show an example.\n" +
                        "Focus on tricks that save time in exam.";

            case "past_questions":
                return "You are an exam assistant. Generate past exam style questions for: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "Create 5 questions in the style of university or board exams.\n" +
                        "Include mix of MCQ, short answer, and long answer questions.\n" +
                        "Give model answers for each question.\n" +
                        "Mark the important keywords in each answer.";

            case "answer_template":
                return "You are an exam assistant. Give a perfect answer writing template for: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "Show the ideal structure for writing exam answers.\n" +
                        "Give a sample answer using the template.\n" +
                        "List the keywords and phrases that score full marks.\n" +
                        "Give tips on what examiners look for.";

            case "score_predictor":
                return "You are an exam assistant. Analyze this content and tell which topics to focus on for maximum marks: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "Categorize topics as HIGH priority, MEDIUM priority, LOW priority.\n" +
                        "For each category list the topics and estimated marks.\n" +
                        "Give a strategy to score above 80 percent.";

            case "highlights":
                return "You are a study assistant. Extract the most important highlights from this content for: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "Give 5 must-know points that will definitely come in exam.\n" +
                        "Give 3 bonus points for extra marks.\n" +
                        "Mark each point with its importance level: HIGH, MEDIUM, or LOW.";

            case "quick_summary":
                return "You are a study assistant. Give a 1 minute revision summary of this content.\n\n" +
                        "Content: " + content + "\n\n" +
                        "Give exactly 3 points only.\n" +
                        "Each point must be one short sentence.\n" +
                        "Focus only on what is most important for exam.\n" +
                        "No extra explanation needed.";

            case "onepager":
                return "You are a study assistant. Create a one page cheat sheet for: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "Include: key definitions, important formulas, key points, common exam questions.\n" +
                        "Keep everything very short and clear.\n" +
                        "Format it so a student can revise the entire topic in 5 minutes.";

            case "revision":
                return "You are a study assistant. Create a complete revision guide for: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "Cover: main concepts, key formulas, important definitions, example problems.\n" +
                        "Organize from basic to advanced.\n" +
                        "Add a quick self-test with 3 questions at the end.";

            case "important":
                return "You are a study assistant. List the 5 most important topics to study in: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "Rank them from most important to least important for exam.\n" +
                        "For each topic explain why it is important and what type of question it appears in.\n" +
                        "Give a study tip for each topic.";

            case "weightage":
                return "You are an exam assistant. Analyze the mark weightage for: " + goal + "\n\n" +
                        "Content: " + content + "\n\n" +
                        "Estimate the percentage of marks each topic carries.\n" +
                        "List topics from highest to lowest weightage.\n" +
                        "Give advice on how much time to spend on each topic.\n" +
                        "Focus on maximizing score with minimum effort.";

            case "tips_exam":
                return "You are an exam coach. Give practical exam writing tips for: " + goal + "\n\n" +
                        "Give at least 8 specific tips.\n" +
                        "Include tips for: reading questions, managing time, writing answers, handling MCQ, avoiding common errors.\n" +
                        "Make each tip actionable and specific to the subject.";

            case "time_mgmt":
                return "You are an exam coach. Create a time management plan for: " + goal + " exam.\n\n" +
                        "Assume the exam is 3 hours long.\n" +
                        "Tell how many minutes to spend on each section.\n" +
                        "Give tips for: starting strong, handling difficult questions, using remaining time for review.\n" +
                        "Include a time allocation table.";

            case "stress":
                return "You are a student counselor. Give practical stress relief tips for a student preparing for: " + goal + " exam.\n\n" +
                        "Give 5 techniques to reduce exam anxiety.\n" +
                        "Include: breathing exercises, study breaks, sleep tips
