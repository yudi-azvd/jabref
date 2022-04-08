package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MySilverPlatterImporterTest {
    SilverPlatterImporter sut;

    private BufferedReader strToBufferedReader(String str) {
        Reader stringReader = new StringReader(str);
        return new BufferedReader(stringReader);
    }

    @BeforeEach
    public void beforeEach() {
        sut = new SilverPlatterImporter();
    }

    @Test
    public void lineHasStartPattern() throws IOException {
        String str = "Record..stuff..INSPEC\n";

        assertFalse(sut.isRecognizedFormat(strToBufferedReader(str)));
    }

    @Test
    public void lineDoesNotHaveStartPattern() throws IOException {
        String str = "UT INSPEC:777777\n";

        assertFalse(sut.isRecognizedFormat(strToBufferedReader(str)));
    }

    @Test
    public void lineHasLessThan5CharactersAndDoesNotEqualStartingString() throws IOException {
        String str = "...\n";

        assertFalse(sut.isRecognizedFormat(strToBufferedReader(str)));
    }

    @Test
    public void lineHasMoreThan4CharactersAndDoesNotEqualStartingString() throws IOException {
        String str = "FA:  \n";

        assertFalse(sut.isRecognizedFormat(strToBufferedReader(str)));
    }

    @Test
    public void lineHasMoreThan4CharactersAndEqualStartingString() throws IOException {
        String str = "TI:  \n";

        assertTrue(sut.isRecognizedFormat(strToBufferedReader(str)));
    }
}
