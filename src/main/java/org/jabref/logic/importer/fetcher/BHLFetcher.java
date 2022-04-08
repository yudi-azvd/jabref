package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.util.BuildInfo;
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
    private final String SEARCH_URL = "https://www.biodiversitylibrary.org/api3?";
    private final Object preferences;
    private static final Logger LOGGER = LoggerFactory.getLogger(BHLFetcher.class);

    public BHLFetcher(ImportFormatPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    public BHLFetcher() {
        preferences = null;
    }

    public static BibEntry parseBHLJSONToBibtex(JSONObject jsonObject) {
        BibEntry bibEntry = new BibEntry();
        Map<Field, String> fieldToBHL = new HashMap<>();
        fieldToBHL.put(StandardField.DATE, "Date");
        fieldToBHL.put(StandardField.VOLUME, "Volume");
        fieldToBHL.put(StandardField.TITLE, "Title");
        fieldToBHL.put(StandardField.SERIES, "Series");
        fieldToBHL.put(StandardField.URL, "PartUrl");

        if (jsonObject.has("Result")) {
            JSONArray results = jsonObject.getJSONArray("Result");
            for (int i = 0; i < results.length(); i++) {
                if (results.getJSONObject(i).has("Authors")) {
                    JSONArray authors = results.getJSONObject(i).getJSONArray("Authors");
                    List<String> authorsList = new ArrayList<>();
                    for (int j = 0; j < authors.length(); j++) {
                        if (authors.getJSONObject(j).has("Name")) {
                            authorsList.add(authors.getJSONObject(i).getString("Name"));
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
                    if (results.getJSONObject(i).has(bhlName)) {
                        String text = results.getJSONObject(i).getString(bhlName);
                        if (!text.isEmpty()) {
                            bibEntry.setField(field, text);
                        }
                    }
                }
            }
        }
        return bibEntry;
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            return null;
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
