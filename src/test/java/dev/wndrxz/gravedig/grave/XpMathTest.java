package dev.wndrxz.gravedig.grave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XpMathTest {

    // reference points straight from the wiki xp table
    @Test
    void totalAtKnownLevels() {
        assertEquals(0, XpMath.totalAtLevel(0));
        assertEquals(7, XpMath.totalAtLevel(1));
        assertEquals(352, XpMath.totalAtLevel(16));
        assertEquals(394, XpMath.totalAtLevel(17));
        assertEquals(1507, XpMath.totalAtLevel(31));
        assertEquals(2920, XpMath.totalAtLevel(40));
    }

    @Test
    void toNextMatchesCurve() {
        assertEquals(7, XpMath.toNextLevel(0));
        assertEquals(37, XpMath.toNextLevel(15));
        assertEquals(42, XpMath.toNextLevel(16));
        assertEquals(112, XpMath.toNextLevel(30));
        assertEquals(121, XpMath.toNextLevel(31));
    }

    @Test
    void progressRoundsAndClamps() {
        assertEquals(XpMath.totalAtLevel(5) + Math.round(0.5f * XpMath.toNextLevel(5)),
                XpMath.totalPoints(5, 0.5f));
        assertEquals(XpMath.totalAtLevel(3), XpMath.totalPoints(3, 0f));
        assertEquals(XpMath.totalAtLevel(3), XpMath.totalPoints(3, -1f));
    }
}
