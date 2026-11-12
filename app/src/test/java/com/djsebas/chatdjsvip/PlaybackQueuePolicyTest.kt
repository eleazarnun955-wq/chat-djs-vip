package com.djsebas.chatdjsvip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackQueuePolicyTest {
    @Test fun followsTheCurrentExplicitQueueOrder() {
        assertEquals(
            "track-c",
            nextTrackAfterCompletion("track-a", listOf("track-a", "track-c", "track-b"), listOf("track-a", "track-b", "track-c"), true)
        )
    }

    @Test fun ignoresTracksRemovedFromLibrary() {
        assertEquals(
            "track-c",
            nextTrackAfterCompletion("track-a", listOf("track-a", "removed", "track-c"), listOf("track-a", "track-c"), true)
        )
    }

    @Test fun fallsBackToFullLibraryWhenQueueHasNoValidTracks() {
        assertEquals(
            "track-b",
            nextTrackAfterCompletion("track-a", listOf("removed"), listOf("track-a", "track-b"), true)
        )
    }

    @Test fun returnsNullWhenAutomaticAdvanceIsDisabled() {
        assertNull(nextTrackAfterCompletion("track-a", listOf("track-a", "track-b"), listOf("track-a", "track-b"), false))
    }

    @Test fun returnsNullAtEndOfQueue() {
        assertNull(nextTrackAfterCompletion("track-b", listOf("track-a", "track-b"), listOf("track-a", "track-b"), true))
    }
}
