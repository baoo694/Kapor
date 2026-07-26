package com.kapor.pronunciation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AzurePronunciationAssessorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AzurePronunciationAssessor assessor = new AzurePronunciationAssessor(null, objectMapper);

    @Test
    void usesASpringCompatibleWavContentTypeForAzureRequests() {
        assertThat(AzurePronunciationAssessor.AZURE_WAV_CONTENT_TYPE)
                .isEqualTo(MediaType.parseMediaType("audio/wav; codecs=\"audio/pcm\"; samplerate=16000"));
        assertThat(AzurePronunciationAssessor.AZURE_WAV_CONTENT_TYPE.getParameter("codecs"))
                .isEqualTo("\"audio/pcm\"");
        assertThatCode(() -> MediaType.parseMediaType(AzurePronunciationAssessor.AZURE_WAV_CONTENT_TYPE.toString()))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> MediaType.parseMediaType("audio/wav; codecs=audio/pcm; samplerate=16000"))
                .isInstanceOf(InvalidMediaTypeException.class);
    }

    @Test
    void sendsWavBodyWithValidContentTypeBeforeCallingAzure() {
        AtomicReference<MockClientHttpRequest> capturedRequest = new AtomicReference<>();
        ClientHttpConnector connector = (method, uri, requestCallback) -> {
            MockClientHttpRequest request = new MockClientHttpRequest(method, uri);
            return requestCallback.apply(request).then(Mono.fromSupplier(() -> {
                capturedRequest.set(request);
                MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                response.setBody("""
                        {"RecognitionStatus":"Success","NBest":[{
                          "PronunciationAssessment":{"AccuracyScore":90,"FluencyScore":88,"CompletenessScore":100,"PronScore":91},
                          "Words":[]
                        }]}
                        """);
                return response;
            }));
        };
        AzurePronunciationAssessor httpAssessor = new AzurePronunciationAssessor(
                WebClient.builder().clientConnector(connector), objectMapper);
        ReflectionTestUtils.setField(httpAssessor, "speechKey", "test-speech-key");
        ReflectionTestUtils.setField(httpAssessor, "region", "japanwest");
        ReflectionTestUtils.setField(httpAssessor, "locale", "ko-KR");

        var result = httpAssessor.assess("서버 배포가 완료되었습니다", new byte[] {1, 2, 3, 4});

        assertThat(result.scores().getPronunciation()).isEqualTo(91);
        assertThat(capturedRequest.get()).isNotNull();
        assertThat(capturedRequest.get().getMethod()).isEqualTo(HttpMethod.POST);
        assertThat(capturedRequest.get().getHeaders().getContentType())
                .isEqualTo(AzurePronunciationAssessor.AZURE_WAV_CONTENT_TYPE);
        assertThat(capturedRequest.get().getHeaders().getContentType().getParameter("codecs"))
                .isEqualTo("\"audio/pcm\"");
        assertThat(capturedRequest.get().getURI().toString()).contains("japanwest.stt.speech.microsoft.com");
    }

    @Test
    void parsesAzureScoresAndKeepsKoreanPhonemesUnnamedWhenAzureDoesNotNameThem() throws Exception {
        var result = assessor.parse(objectMapper.readTree("""
                {"RecognitionStatus":"Success","NBest":[{
                  "PronunciationAssessment":{"AccuracyScore":81.4,"FluencyScore":76,"CompletenessScore":100,"PronScore":80.5},
                  "Words":[{"Word":"배포가","Offset":1200000,"Duration":4200000,
                    "PronunciationAssessment":{"AccuracyScore":64,"ErrorType":"Mispronunciation"},
                    "Phonemes":[
                      {"PronunciationAssessment":{"AccuracyScore":61}},
                      {"PronunciationAssessment":{"AccuracyScore":70}}
                    ]
                  }]
                }]}
                """));

        assertThat(result.scores().getPronunciation()).isEqualTo(81);
        assertThat(result.scores().getOverall()).isEqualTo(81);
        assertThat(result.words()).singleElement().satisfies(word -> {
            assertThat(word.getText()).isEqualTo("배포가");
            assertThat(word.getErrorType()).isEqualTo("Mispronunciation");
            assertThat(word.getOffsetMs()).isEqualTo(120L);
            assertThat(word.getPhonemes()).extracting(phoneme -> phoneme.getScore()).containsExactly(61, 70);
            assertThat(word.getPhonemes()).allSatisfy(phoneme -> assertThat(phoneme.getPhoneme()).isNull());
        });
    }

    @Test
    void parsesFlatAzurePronunciationSchemaReturnedForKoreanRestRequests() throws Exception {
        var result = assessor.parse(objectMapper.readTree("""
                {"RecognitionStatus":"Success","NBest":[{
                  "AccuracyScore":78.6,"FluencyScore":74.2,"CompletenessScore":100,"PronScore":79.1,
                  "Words":[{"Word":"배포가","Offset":1200000,"Duration":4200000,
                    "AccuracyScore":64,"ErrorType":"Mispronunciation",
                    "Phonemes":[{"Phoneme":"b","AccuracyScore":61},{"Phoneme":"ae","AccuracyScore":70}]
                  }]
                }]}
                """));

        assertThat(result.scores().getAccuracy()).isEqualTo(79);
        assertThat(result.scores().getFluency()).isEqualTo(74);
        assertThat(result.scores().getCompleteness()).isEqualTo(100);
        assertThat(result.scores().getPronunciation()).isEqualTo(79);
        assertThat(result.words()).singleElement().satisfies(word -> {
            assertThat(word.getErrorType()).isEqualTo("Mispronunciation");
            assertThat(word.getPhonemes()).extracting(phoneme -> phoneme.getScore()).containsExactly(61, 70);
        });
    }
}
