package com.kapor.techtalk.service;

import com.kapor.techtalk.model.RoleplaySession;
import com.kapor.techtalk.model.TechTalkScenario;

import java.util.List;

public record RoleplayContext(
        TechTalkScenario scenario,
        RoleplaySession session,
        String systemPrompt,
        String promptVersion,
        List<RoleplaySession.Message> conversation
) { }
