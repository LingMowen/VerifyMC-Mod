package com.verifymc.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.verifymc.VerifyMC;
import com.verifymc.config.VerifyMCConfig;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

/**
 * Questionnaire service for handling registration questionnaire
 * Supports single/multiple choice/text questions with scoring system
 * Includes LLM-based essay scoring for text questions
 */
public class QuestionnaireService {
    private final Gson gson;
    private List<Map<String, Object>> questions;
    private final File configFile;
    private EssayScoringService essayScoringService;

    public QuestionnaireService(File dataFolder) {
        this.gson = new Gson();
        this.configFile = new File(dataFolder, "questionnaire.json");
        loadQuestionnaireConfig();
        
        if (VerifyMCConfig.LLM_SCORING_ENABLED.get()) {
            this.essayScoringService = new EssayScoringService();
            debugLog("LLM scoring enabled");
        }
    }

    /**
     * Load questionnaire configuration from file
     */
    private void loadQuestionnaireConfig() {
        if (!configFile.exists()) {
            createDefaultQuestionnaireConfig();
        }

        try (FileReader reader = new FileReader(configFile)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = gson.fromJson(reader, Map.class);
            if (config != null && config.containsKey("questions")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> qList = (List<Map<String, Object>>) config.get("questions");
                this.questions = qList;
            } else {
                this.questions = new ArrayList<>();
            }
            debugLog("Questionnaire configuration loaded with " + questions.size() + " questions");
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Failed to load questionnaire config", e);
            this.questions = new ArrayList<>();
        }
    }

    /**
     * Create default questionnaire configuration file
     */
    private void createDefaultQuestionnaireConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", false);
        config.put("passScore", 60);
        config.put("questions", new ArrayList<>());

        try {
            // Ensure parent directory exists
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (!created) {
                    VerifyMC.LOGGER.warn("Failed to create parent directory: {}", parentDir.getAbsolutePath());
                }
            }

            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(config, writer);
                debugLog("Created default questionnaire config at: " + configFile.getAbsolutePath());
            }
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Failed to create questionnaire config at: " + configFile.getAbsolutePath(), e);
        }
    }

    /**
     * Reload questionnaire configuration from disk
     */
    public void reload() {
        loadQuestionnaireConfig();
    }

    public boolean isEnabled() {
        return VerifyMCConfig.QUESTIONNAIRE_ENABLED.get();
    }

    public int getPassScore() {
        return VerifyMCConfig.QUESTIONNAIRE_PASS_SCORE.get();
    }

    /**
     * Get questionnaire configuration as JSON
     */
    public JsonObject getQuestionnaire(String language) {
        JsonObject result = new JsonObject();
        result.addProperty("enabled", isEnabled());
        result.addProperty("passScore", getPassScore());

        if (!isEnabled() || questions == null || questions.isEmpty()) {
            result.add("questions", new JsonArray());
            return result;
        }

        JsonArray questionsArray = new JsonArray();

        for (Map<String, Object> qMap : questions) {
            JsonObject question = new JsonObject();

            Object id = qMap.get("id");
            question.addProperty("id", id != null ? ((Number) id).intValue() : 0);

            // Get question text based on language
            String questionText = "zh".equals(language) ?
                (String) qMap.get("question_zh") :
                (String) qMap.get("question_en");
            if (questionText == null) {
                questionText = (String) qMap.getOrDefault("question", "");
            }
            question.addProperty("question", questionText);

            String type = (String) qMap.getOrDefault("type", "single_choice");
            question.addProperty("type", type);

            Object required = qMap.get("required");
            question.addProperty("required", required != null && (Boolean) required);

            // Input metadata
            @SuppressWarnings("unchecked")
            Map<String, Object> inputMap = (Map<String, Object>) qMap.get("input");
            JsonObject inputMeta = new JsonObject();
            if (inputMap != null) {
                for (Map.Entry<String, Object> entry : inputMap.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value instanceof Number) {
                        inputMeta.addProperty(key, (Number) value);
                    } else if (value instanceof Boolean) {
                        inputMeta.addProperty(key, (Boolean) value);
                    } else if (value != null) {
                        inputMeta.addProperty(key, value.toString());
                    }
                }
            }
            question.add("input", inputMeta);

            // Options
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> optionsList = (List<Map<String, Object>>) qMap.get("options");
            JsonArray optionsArray = new JsonArray();

            if (optionsList != null) {
                int optionIndex = 0;
                for (Map<String, Object> optMap : optionsList) {
                    JsonObject option = new JsonObject();
                    option.addProperty("id", optionIndex++);

                    String optText = "zh".equals(language)
                        ? (String) optMap.get("text_zh")
                        : (String) optMap.get("text_en");
                    if (optText == null) {
                        optText = (String) optMap.getOrDefault("text", "");
                    }
                    option.addProperty("text", optText);
                    optionsArray.add(option);
                }
            }
            question.add("options", optionsArray);
            questionsArray.add(question);
        }

        result.add("questions", questionsArray);
        return result;
    }

    /**
     * Score questionnaire answers
     */
    public QuestionnaireResult scoreAnswers(Map<Integer, QuestionAnswer> answers) {
        if (!isEnabled() || questions == null || questions.isEmpty()) {
            return new QuestionnaireResult(true, 100, getPassScore(), Collections.emptyList());
        }

        int totalScore = 0;
        List<QuestionScoreDetail> details = new ArrayList<>();

        for (Map<String, Object> qMap : questions) {
            Object idObj = qMap.get("id");
            int questionId = idObj != null ? ((Number) idObj).intValue() : 0;
            String questionType = (String) qMap.getOrDefault("type", "single_choice");
            QuestionAnswer answer = answers.get(questionId);

            if (answer == null) {
                details.add(new QuestionScoreDetail(questionId, questionType, 0, getMaxScore(qMap), "No answer submitted"));
                continue;
            }

            if ("text".equalsIgnoreCase(questionType)) {
                // Use LLM scoring if enabled, otherwise give full score
                int maxScore = getMaxScore(qMap);
                
                if (VerifyMCConfig.LLM_SCORING_ENABLED.get() && essayScoringService != null 
                        && answer.getTextAnswer() != null && !answer.getTextAnswer().isEmpty()) {
                    // Get question text and scoring rule
                    String questionText = getQuestionText(qMap, "zh"); // Default to Chinese
                    String scoringRule = (String) qMap.getOrDefault("scoring_rule", "");
                    
                    EssayScoringService.EssayScoringRequest request = new EssayScoringService.EssayScoringRequest(
                            questionId, questionText, answer.getTextAnswer(), scoringRule, maxScore);
                    
                    try {
                        EssayScoringService.EssayScoringResult result = essayScoringService.score(request);
                        int score = result.getScore();
                        String reason = result.getReason();
                        
                        if (result.isManualReview()) {
                            // Manual review required - give 0 score but mark for review
                            details.add(new QuestionScoreDetail(questionId, questionType, 0, maxScore, 
                                    "Manual review required: " + reason));
                            debugLog("Question " + questionId + " requires manual review: " + reason);
                        } else {
                            details.add(new QuestionScoreDetail(questionId, questionType, score, maxScore, reason));
                            totalScore += score;
                            debugLog("Question " + questionId + " scored by LLM: " + score + "/" + maxScore + " - " + reason);
                        }
                    } catch (Exception e) {
                        VerifyMC.LOGGER.error("LLM scoring failed for question " + questionId, e);
                        // Fallback to manual review
                        details.add(new QuestionScoreDetail(questionId, questionType, 0, maxScore, 
                                "LLM scoring failed, manual review required"));
                    }
                } else {
                    // No LLM scoring or empty answer - give full score (backward compatible)
                    details.add(new QuestionScoreDetail(questionId, questionType, maxScore, maxScore, 
                            "Text answer accepted (no LLM scoring)"));
                    totalScore += maxScore;
                }
            } else {
                QuestionScoreDetail detail = scoreChoiceQuestion(qMap, answer, questionId);
                totalScore += detail.getScore();
                details.add(detail);
            }
        }

        int passScore = getPassScore();
        boolean passed = totalScore >= passScore;
        debugLog("Questionnaire evaluation: score=" + totalScore + ", passScore=" + passScore + ", passed=" + passed);
        return new QuestionnaireResult(passed, totalScore, passScore, details);
    }

    private QuestionScoreDetail scoreChoiceQuestion(Map<String, Object> questionMap, QuestionAnswer answer, int questionId) {
        int questionScore = 0;
        int maxScore = getMaxScore(questionMap);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> optionsList = (List<Map<String, Object>>) questionMap.get("options");
        if (optionsList != null) {
            for (int optionId : answer.getSelectedOptionIds()) {
                if (optionId >= 0 && optionId < optionsList.size()) {
                    Object scoreObj = optionsList.get(optionId).get("score");
                    if (scoreObj instanceof Number) {
                        questionScore += ((Number) scoreObj).intValue();
                    }
                }
            }
        }

        questionScore = Math.max(0, Math.min(maxScore, questionScore));
        return new QuestionScoreDetail(questionId, answer.getType(), questionScore, maxScore, "Locally scored");
    }

    private int getMaxScore(Map<String, Object> questionMap) {
        String type = (String) questionMap.getOrDefault("type", "single_choice");
        Object configured = questionMap.get("max_score");

        if ("text".equalsIgnoreCase(type)) {
            if (configured instanceof Number) {
                return Math.max(1, ((Number) configured).intValue());
            }
            return 20;
        }

        if (configured instanceof Number) {
            return Math.max(1, ((Number) configured).intValue());
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> optionsList = (List<Map<String, Object>>) questionMap.get("options");
        if (optionsList != null && !optionsList.isEmpty()) {
            int total = 0;
            for (Map<String, Object> option : optionsList) {
                Object scoreObj = option.get("score");
                if (scoreObj instanceof Number) {
                    total += ((Number) scoreObj).intValue();
                }
            }
            return Math.max(1, total);
        }

        return 1;
    }

    private String getQuestionText(Map<String, Object> questionMap, String language) {
        String questionText = "zh".equals(language)
                ? (String) questionMap.get("question_zh")
                : (String) questionMap.get("question_en");
        if (questionText == null) {
            questionText = (String) questionMap.getOrDefault("question", "");
        }
        return questionText;
    }

    private void debugLog(String msg) {
        if (VerifyMCConfig.DEBUG.get()) {
            VerifyMC.LOGGER.info("[DEBUG] QuestionnaireService: {}", msg);
        }
    }

    // --- Data Classes ---

    public static class QuestionAnswer {
        private final String type;
        private final List<Integer> selectedOptionIds;
        private final String textAnswer;

        public QuestionAnswer(String type, List<Integer> selectedOptionIds, String textAnswer) {
            this.type = type != null ? type : "";
            this.selectedOptionIds = selectedOptionIds != null ? new ArrayList<>(selectedOptionIds) : new ArrayList<>();
            this.textAnswer = textAnswer != null ? textAnswer : "";
        }

        public String getType() { return type; }
        public List<Integer> getSelectedOptionIds() { return Collections.unmodifiableList(selectedOptionIds); }
        public String getTextAnswer() { return textAnswer; }
    }

    public static class QuestionScoreDetail {
        private final int questionId;
        private final String type;
        private final int score;
        private final int maxScore;
        private final String reason;

        public QuestionScoreDetail(int questionId, String type, int score, int maxScore, String reason) {
            this.questionId = questionId;
            this.type = type;
            this.score = score;
            this.maxScore = maxScore;
            this.reason = reason;
        }

        public int getQuestionId() { return questionId; }
        public int getScore() { return score; }
        public int getMaxScore() { return maxScore; }
        public String getReason() { return reason; }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("questionId", questionId);
            json.addProperty("type", type);
            json.addProperty("score", score);
            json.addProperty("maxScore", maxScore);
            json.addProperty("reason", reason);
            return json;
        }
    }

    public static class QuestionnaireResult {
        private final boolean passed;
        private final int score;
        private final int passScore;
        private final List<QuestionScoreDetail> details;

        public QuestionnaireResult(boolean passed, int score, int passScore, List<QuestionScoreDetail> details) {
            this.passed = passed;
            this.score = score;
            this.passScore = passScore;
            this.details = details != null ? new ArrayList<>(details) : new ArrayList<>();
        }

        public boolean isPassed() { return passed; }
        public int getScore() { return score; }
        public int getPassScore() { return passScore; }
        public List<QuestionScoreDetail> getDetails() { return Collections.unmodifiableList(details); }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("passed", passed);
            json.addProperty("score", score);
            json.addProperty("passScore", passScore);

            JsonArray detailArray = new JsonArray();
            for (QuestionScoreDetail detail : details) {
                detailArray.add(detail.toJson());
            }
            json.add("details", detailArray);
            return json;
        }
    }
}
