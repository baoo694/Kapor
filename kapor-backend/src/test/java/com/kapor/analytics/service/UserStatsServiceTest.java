package com.kapor.analytics.service;

import com.kapor.analytics.model.LearningActivityEvent;
import com.kapor.user.model.User;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserStatsServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private UserStatsService userStatsService;

    @Test
    void atomicallyAddsAllProfileCountersFromAnActivityEvent() {
        LearningActivityEvent event = LearningActivityEvent.builder()
                .minutesStudied(7)
                .cardsReviewed(3)
                .roleplaySessions(1)
                .videosWatched(2)
                .build();

        userStatsService.addActivity("user-1", event);

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), updateCaptor.capture(), eq(User.class));
        Document increments = (Document) updateCaptor.getValue().getUpdateObject().get("$inc");
        assertThat(increments)
                .containsEntry("stats.totalStudyMinutes", 7)
                .containsEntry("stats.totalCardsReviewed", 3)
                .containsEntry("stats.totalRoleplaySessions", 1)
                .containsEntry("stats.totalVideosWatched", 2);
    }

    @Test
    void skipsMongoUpdateWhenTheEventHasNoProfileCounters() {
        userStatsService.addActivity("user-1", LearningActivityEvent.builder().build());

        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), eq(User.class));
    }
}
