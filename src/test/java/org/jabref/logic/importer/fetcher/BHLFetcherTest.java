package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import kong.unirest.json.JSONObject;
import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.parser.SyntaxParser;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jabref.logic.importer.fetcher.transformers.AbstractQueryTransformer.NO_EXPLICIT_FIELD;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BHLFetcherTest {
    private BHLFetcher fetcher;

    @BeforeEach
    void setup() {
        fetcher = new BHLFetcher();
    }

    @Test
    void shouldReturnName() {
        assertEquals("Biodiversity H. Library", fetcher.getName());
    }

    @Test
    void testGetURLForQuery() throws FetcherException, MalformedURLException, URISyntaxException, QueryNodeParseException {
        String testQuery = "cocos island costa rica birds";
        SyntaxParser parser = new StandardSyntaxParser();
        URL url = fetcher.getURLForQuery(parser.parse(testQuery, NO_EXPLICIT_FIELD));
        String expected = "https://www.biodiversitylibrary.org/api3?op=PublicationSearch&searchterm=cocos+island+costa+rica+birds";
        expected += "&searchtype=C&page=1&pageSize=10&apikey=" + BHLFetcher.API_KEY + "&format=json";

        assertEquals(expected, url.toString());
    }

    @Test
    void testBHLJSONtoBibtex() {
        String jsonString = """
                {
                    "Status": "ok",
                    "ErrorMessage": "",
                    "Result": [
                        {
                            "BHLType": "Part",
                            "FoundIn": "Both",
                            "Volume": "2",
                            "Authors": [
                                {
                                    "Name": "Gifford, Edward Winslow,"
                                }
                            ],
                            "PartUrl": "https://www.biodiversitylibrary.org/part/69838",
                            "PartID": "69838",
                            "Genre": "Article",
                            "Title": "Field notes on the land birds of the Galapagos Islands, and of Cocos Island,Costa Rica",
                            "ContainerTitle": "Proceedings of the California Academy of Sciences, 4th series.",
                            "Series": "4",
                            "Date": "1919",
                            "PageRange": "189--258"
                        }
                   ]
                }
                """;

        JSONObject jsonObject = new kong.unirest.json.JSONObject(jsonString);
        BibEntry bibEntry = BHLFetcher.parseBHLJSONToBibtex(jsonObject);
        assertEquals(Optional.of("1919"), bibEntry.getField(StandardField.DATE));
        assertEquals(Optional.of("Gifford, Edward Winslow,"), bibEntry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Field notes on the land birds of the Galapagos Islands, and of Cocos Island,Costa Rica"), bibEntry.getField(StandardField.TITLE));
        assertEquals(Optional.of("https://www.biodiversitylibrary.org/part/69838"), bibEntry.getField(StandardField.URL));
        assertEquals(Optional.of("4"), bibEntry.getField(StandardField.SERIES));
        assertEquals(Optional.of("2"), bibEntry.getField(StandardField.VOLUME));
    }
}
