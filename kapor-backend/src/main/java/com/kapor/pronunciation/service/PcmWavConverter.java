package com.kapor.pronunciation.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Converts the app's signed 16-bit, 16 kHz, mono PCM stream into portable WAV. */
final class PcmWavConverter {
    static final int SAMPLE_RATE = 16_000;
    static final int BYTES_PER_SECOND = SAMPLE_RATE * 2;

    private PcmWavConverter() { }

    static byte[] toWav(byte[] pcm) {
        if (pcm == null || pcm.length == 0 || pcm.length % 2 != 0) {
            throw new IllegalArgumentException("Bản ghi PCM không hợp lệ.");
        }
        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put(StandardCharsets.US_ASCII.encode("RIFF"));
        header.putInt(36 + pcm.length);
        header.put(StandardCharsets.US_ASCII.encode("WAVEfmt "));
        header.putInt(16).putShort((short) 1).putShort((short) 1);
        header.putInt(SAMPLE_RATE).putInt(BYTES_PER_SECOND).putShort((short) 2).putShort((short) 16);
        header.put(StandardCharsets.US_ASCII.encode("data")).putInt(pcm.length);
        byte[] wav = new byte[44 + pcm.length];
        System.arraycopy(header.array(), 0, wav, 0, 44);
        System.arraycopy(pcm, 0, wav, 44, pcm.length);
        return wav;
    }

    static long durationMs(byte[] pcm) {
        return Math.round(pcm.length * 1000d / BYTES_PER_SECOND);
    }
}
