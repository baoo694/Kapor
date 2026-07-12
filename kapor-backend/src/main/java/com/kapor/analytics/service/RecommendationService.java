package com.kapor.analytics.service;

import com.kapor.analytics.dto.DashboardResponse;
import com.kapor.user.model.User;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    // For now, return a static recommendation until we integrate LangChain4j for dynamic AI recommendations
    public DashboardResponse.RecommendationCard generateRecommendation(User user) {
        return DashboardResponse.RecommendationCard.builder()
                .type("review_due")
                .title("Ôn tập từ vựng Backend")
                .subtitle("Bạn có 15 thẻ MemByte cần ôn tập hôm nay")
                .targetScreen("/(tabs)/membyte")
                .icon("🧠")
                .build();
    }
}
