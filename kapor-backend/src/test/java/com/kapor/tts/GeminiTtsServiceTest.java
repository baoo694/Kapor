package com.kapor.tts;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GeminiTtsServiceTest {

    @Test
    void wrapsGeminiPcmAudioAsAValidMonoWavFile() {
        byte[] pcm = {1, 2, 3, 4};

        byte[] wav = GeminiTtsService.wavFromPcm(pcm, 24000);
        ByteBuffer header = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);

        byte[] riff = new byte[4];
        header.get(riff);
        assertArrayEquals("RIFF".getBytes(StandardCharsets.US_ASCII), riff);
        assertEquals(40, header.getInt());

        byte[] wave = new byte[4];
        header.get(wave);
        assertArrayEquals("WAVE".getBytes(StandardCharsets.US_ASCII), wave);
        header.position(22);
        assertEquals(1, header.getShort());
        assertEquals(24000, header.getInt());
        header.position(40);
        assertEquals(pcm.length, header.getInt());
        assertArrayEquals(pcm, java.util.Arrays.copyOfRange(wav, 44, wav.length));
    }
}
