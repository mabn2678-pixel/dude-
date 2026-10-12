package com.vidget.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageSpaceCheckerTest {

    @Test
    fun calculateRequiredTempBytes_single_stream_applies_10_percent_buffer() {
        val estimatedBytes = 100_000_000L // 100 MB
        val required = StorageSpaceChecker.calculateRequiredTempBytes(
            estimatedSizeBytes = estimatedBytes,
            hasSeparateStreams = false
        )

        // Expected = 100 MB * 1.10 = 110 MB
        assertEquals(110_000_000L, required)
    }

    @Test
    fun calculateRequiredTempBytes_separate_streams_adds_merge_overhead() {
        val estimatedBytes = 100_000_000L // 100 MB combined (video + audio)
        val required = StorageSpaceChecker.calculateRequiredTempBytes(
            estimatedSizeBytes = estimatedBytes,
            hasSeparateStreams = true
        )

        // Expected = (100 MB * 1.10) + 100 MB = 210 MB (2.10x)
        assertEquals(210_000_000L, required)
    }

    @Test
    fun calculateRequiredTempBytes_zero_or_negative_returns_zero() {
        assertEquals(0L, StorageSpaceChecker.calculateRequiredTempBytes(0L, false))
        assertEquals(0L, StorageSpaceChecker.calculateRequiredTempBytes(-500L, true))
    }

    @Test
    fun formatBytes_formats_units_correctly() {
        assertEquals("0 B", StorageSpaceChecker.formatBytes(0L))
        assertEquals("500 B", StorageSpaceChecker.formatBytes(500L))
        assertEquals("1.0 KB", StorageSpaceChecker.formatBytes(1024L))
        assertEquals("50.0 MB", StorageSpaceChecker.formatBytes(50L * 1024 * 1024))
        assertEquals("1.50 GB", StorageSpaceChecker.formatBytes((1.50 * 1024 * 1024 * 1024).toLong()))
    }
}
