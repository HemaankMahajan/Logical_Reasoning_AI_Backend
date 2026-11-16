package com.AI.writer.app.Util;

import lombok.Data;
import java.util.List;

@Data
public class AIResponseDTO {
    private String id;
    private List<Choice> choices;
    private Usage usage;

    @Data
    public static class Choice {
        private Message message;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
        private String reasoning;
    }

    @Data
    public static class Usage {
        private int prompt_tokens;
        private int completion_tokens;
        private int total_tokens;
        private CompletionTokensDetails completion_tokens_details;

        @Data
        public static class CompletionTokensDetails {
            private int reasoning_tokens;
        }
    }
}