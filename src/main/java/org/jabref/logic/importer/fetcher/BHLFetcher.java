package org.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BHLFetcher implements SearchBasedParserFetcher {
    static final String API_KEY = new BuildInfo().bhlAPIKey;
    private static final Logger LOGGER = LoggerFactory.getLogger(BHLFetcher.class);
    private final String SEARCH_URL = "https://www.biodiversitylibrary.org/api3?";
    private final Object preferences;

    public BHLFetcher(ImportFormatPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    public BHLFetcher() {
        preferences = null;
    }

    public static BibEntry parseBHLJSONToBibtex(JSONObject bhlJSONResult) {
        BibEntry bibEntry = new BibEntry();
        Map<Field, String> fieldToBHL = new HashMap<>();
        fieldToBHL.put(StandardField.DATE, "Date");
        fieldToBHL.put(StandardField.VOLUME, "Volume");
        fieldToBHL.put(StandardField.TITLE, "Title");
        fieldToBHL.put(StandardField.SERIES, "Series");
        fieldToBHL.put(StandardField.URL, "PartUrl");

        if (bhlJSONResult.has("Authors")) {
            JSONArray authors = bhlJSONResult.getJSONArray("Authors");
            List<String> authorsList = new ArrayList<>();
            for (int j = 0; j < authors.length(); j++) {
                if (authors.getJSONObject(j).has("Name")) {
                    authorsList.add(authors.getJSONObject(j).getString("Name"));
                } else {
                    LOGGER.info("Empty author name.");
                }
            }
            bibEntry.setField(StandardField.AUTHOR, String.join(" and ", authorsList));
        } else {
            LOGGER.info("No author found.");
        }
        for (var entry : fieldToBHL.entrySet()) {
            Field field = entry.getKey();
            String bhlName = entry.getValue();
            if (bhlJSONResult.has(bhlName)) {
                String text = bhlJSONResult.getString(bhlName);
                if (!text.isEmpty()) {
                    bibEntry.setField(field, text);
                }
            }
        }
        return bibEntry;
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            String response = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining(OS.NEWLINE));
            JSONObject jsonObject = new JSONObject(response);
            List<BibEntry> entries = new ArrayList<>();
            if (jsonObject.has("Results")) {
                JSONArray results = jsonObject.getJSONArray("Results");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject bhlEntry = results.getJSONObject(i);
                    BibEntry entry = parseBHLJSONToBibtex(bhlEntry);
                    entries.add(entry);
                }
            }

            return entries;
        };
    }

    @Override
    public URL getURLForQuery(QueryNode luceneQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(SEARCH_URL);
        uriBuilder.addParameter("op", "PublicationSearch");
        uriBuilder.addParameter("searchterm", new DefaultQueryTransformer().transformLuceneQuery(luceneQuery).orElse(""));
        uriBuilder.addParameter("searchtype", "C");
        uriBuilder.addParameter("page", "1");
        uriBuilder.addParameter("pageSize", "10");
        uriBuilder.addParameter("apikey", API_KEY);
        uriBuilder.addParameter("format", "json");

        return uriBuilder.build().toURL();
    }

    @Override
    public String getName() {
        return "Biodiversity H. Library";
    }
}
