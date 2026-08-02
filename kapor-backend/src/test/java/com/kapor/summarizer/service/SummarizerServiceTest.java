package com.kapor.summarizer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapor.membyte.repository.MembyteDeckRepository;
import com.kapor.membyte.repository.MembyteFlashcardRepository;
import com.kapor.summarizer.dto.SummarizerCardDto;
import com.kapor.summarizer.dto.SummarizerSaveDeckRequest;
import com.kapor.membyte.model.MembyteDeck;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SummarizerServiceTest {

    @Test
    void discardsDuplicateAndNonKoreanCardsFromGemini() throws Exception {
        SummarizerService service = service();
        ObjectMapper mapper = new ObjectMapper();
        var generatedCards = mapper.createObjectNode().putArray("cards");
        generatedCards.addObject().put("korean", "배포").put("pronunciation", "baepo").put("vietnamese", "triển khai").put("english", "deployment").put("definitionEn", "release software").put("exampleKo", "배포합니다.").putNull("grammarNote").put("context", "배포 문서");
        generatedCards.addObject().put("korean", "배포").put("pronunciation", "baepo").put("vietnamese", "duplicate").put("english", "deployment").put("definitionEn", "duplicate").put("exampleKo", "배포합니다.").putNull("grammarNote").put("context", "배포 문서");
        generatedCards.addObject().put("korean", "서버").put("pronunciation", "seobeo").put("vietnamese", "máy chủ").put("english", "server").put("definitionEn", "computer service").put("exampleKo", "서버를 확인합니다.").putNull("grammarNote").put("context", "서버 문서");
        generatedCards.addObject().put("korean", "코드").put("pronunciation", "kodeu").put("vietnamese", "mã nguồn").put("english", "code").put("definitionEn", "program text").put("exampleKo", "코드를 작성합니다.").putNull("grammarNote").put("context", "코드 문서");
        var response = mapper.createObjectNode();
        response.putArray("candidates").addObject().putObject("content")
                .putArray("parts").addObject()
                .put("text", mapper.writeValueAsString(mapper.createObjectNode().set("cards", generatedCards)));

        @SuppressWarnings("unchecked")
        List<SummarizerCardDto> cards = (List<SummarizerCardDto>) ReflectionTestUtils.invokeMethod(
                service, "parseCards", response, 8);

        assertThat(cards).extracting(SummarizerCardDto::getKorean)
                .containsExactly("배포", "서버", "코드");
    }

    @Test
    void savesSelectedCardsToANewPrivateMemByteDeck() {
        MembyteDeckRepository decks = mock(MembyteDeckRepository.class);
        MembyteFlashcardRepository cards = mock(MembyteFlashcardRepository.class);
        MembyteDeck savedDeck = MembyteDeck.builder().id("deck-1").build();
        when(decks.save(any(MembyteDeck.class))).thenReturn(savedDeck);
        SummarizerService service = new SummarizerService(WebClient.builder(), new ObjectMapper(), decks, cards);
        ReflectionTestUtils.setField(service, "maxSourceCharacters", 12000);
        SummarizerSaveDeckRequest request = new SummarizerSaveDeckRequest();
        request.setSourceTitle("Korean deployment guide");
        request.setCards(List.of(card("배포", "triển khai"), card("서버", "máy chủ")));

        var result = service.saveDeck("user-1", request);

        ArgumentCaptor<MembyteDeck> deckCaptor = ArgumentCaptor.forClass(MembyteDeck.class);
        verify(decks).save(deckCaptor.capture());
        verify(cards).saveAll(any());
        assertThat(result.getDeckId()).isEqualTo("deck-1");
        assertThat(result.getSavedCards()).isEqualTo(2);
        assertThat(deckCaptor.getValue().getLessonId()).startsWith("summarizer:");
    }

    private SummarizerService service() {
        return new SummarizerService(WebClient.builder(), new ObjectMapper(), mock(MembyteDeckRepository.class), mock(MembyteFlashcardRepository.class));
    }

    private SummarizerCardDto card(String korean, String vietnamese) {
        return SummarizerCardDto.builder().korean(korean).vietnamese(vietnamese).build();
    }
}
