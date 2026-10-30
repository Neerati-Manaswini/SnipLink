package com.manaswini.sniplink;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeGeneratorTest {

    @Test
    void producesRequestedLengthAndAlphabet() {
        String code = CodeGenerator.random(7);
        assertEquals(7, code.length());
        assertTrue(code.matches("[0-9A-Za-z]{7}"));
    }

    @Test
    void codesDiffer() {
        assertNotEquals(CodeGenerator.random(12), CodeGenerator.random(12));
    }
}
