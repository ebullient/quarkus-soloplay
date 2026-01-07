package dev.ebullient.soloplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.neo4j.ogm.session.SessionFactory;

import io.quarkus.logging.Log;

@ApplicationScoped
public class LoreRepository {
    @Inject
    SessionFactory sessionFactory;

    /**
     * Retrieve document content by filename (from YAML frontmatter).
     * Used for resolving cross-references in campaign documents.
     *
     * @param filename The filename (e.g., "backgrounds/acolyte-xphb.md")
     * @return Concatenated text content, or null if not found
     */
    public String getDocumentByFilename(String filename) {
        var session = sessionFactory.openSession();

        try {
            String cypher = """
                    MATCH (n:Document)
                    WHERE n.filename = $filename
                    RETURN n.text as text
                    ORDER BY n.sectionIndex, n.chunkIndex
                    """;

            Iterable<Map<String, Object>> results = session.query(cypher, Map.of("filename", filename));

            StringBuilder content = new StringBuilder();
            for (Map<String, Object> row : results) {
                String text = (String) row.get("text");
                if (text != null) {
                    if (content.length() > 0) {
                        content.append("\n\n");
                    }
                    content.append(text);
                }
            }

            if (content.length() == 0) {
                return null;
            }
            return content.toString();
        } catch (Exception e) {
            Log.errorf(e, "Error retrieving document by filename: %s", e.getMessage());
            return null;
        }
    }

    /**
     * List all available adventures from ingested documents.
     * Adventures are identified by having "adventures" in the sourceFile path.
     * Returns a list of adventure names (from YAML sources field).
     */
    public List<String> listAdventures() {
        var session = sessionFactory.openSession();
        List<String> result = new ArrayList<>();

        try {
            // Query for documents with "adventures" in path and extract sources
            String cypher = """
                    MATCH (n:Document)
                    WHERE n.sourceFile IS NOT NULL
                      AND toLower(n.sourceFile) CONTAINS 'adventures'
                      AND n.sources IS NOT NULL
                    RETURN DISTINCT n.sources as adventureName
                    ORDER BY n.sources
                    """;

            Iterable<Map<String, Object>> results = session.query(cypher, Map.of());
            results.forEach(row -> {
                String adventureName = (String) row.get("adventureName");
                if (adventureName != null) {
                    result.add(adventureName);
                }
            });
        } catch (Exception e) {
            Log.errorf(e, "Error listing adventures: %s", e.getMessage());
        }

        return result;
    }

    /**
     * Validate that an adventure exists in the ingested documents.
     * Returns true if the adventure is found, false otherwise.
     */
    public boolean validateAdventureExists(String adventureName) {
        if (adventureName == null) {
            return false;
        }

        List<String> adventures = listAdventures();
        return adventures.contains(adventureName);
    }
}
