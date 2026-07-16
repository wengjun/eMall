package com.emall.loadtest;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.zip.DataFormatException;
import org.HdrHistogram.Histogram;

final class HistogramCodec {
    private HistogramCodec() {
    }

    static String encode(Histogram histogram) {
        ByteBuffer buffer = ByteBuffer.allocate(histogram.getNeededByteBufferCapacity());
        int encodedLength = histogram.encodeIntoCompressedByteBuffer(buffer);
        byte[] encoded = new byte[encodedLength];
        buffer.flip();
        buffer.get(encoded);
        return Base64.getEncoder().encodeToString(encoded);
    }

    static Histogram decode(String encoded) {
        try {
            return Histogram.decodeFromCompressedByteBuffer(ByteBuffer.wrap(Base64.getDecoder().decode(encoded)), 0L);
        } catch (DataFormatException ex) {
            throw new IllegalArgumentException("invalid HdrHistogram payload", ex);
        }
    }
}
