package com.kapor.techtalk.service;

import com.kapor.techtalk.dto.RoleplayHintDto;
import com.kapor.techtalk.model.RoleplaySession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RoleplayAiProvider {
    Flux<String> streamReply(RoleplayContext context);
    Mono<RoleplaySession.Evaluation> evaluateTurn(RoleplayContext context, String userMessage);
    Mono<RoleplaySession.FinalEvaluation> evaluateSession(RoleplayContext context);
    Mono<RoleplayHintDto> hint(RoleplayContext context);
    String modelName();
}
