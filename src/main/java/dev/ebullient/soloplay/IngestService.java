package dev.ebullient.soloplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.neo4j.ogm.session.SessionFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkus.logging.Log;

@ApplicationScoped
public class IngestService {
    static final String TOOLS_DOC_SEPARATOR = "============\n";

    // Regex pattern for YAML frontmatter
    // (?s) enables DOTALL mode (. matches newlines)
    // ^--- matches opening ---
    // (.*?) captures content (non-greedy)
    // \n--- matches closing ---
    static final java.util.regex.Pattern YAML_FRONTMATTER_PATTERN = java.util.regex.Pattern
            .compile("(?s)^---\\s*\\n(.*?)\\n---\\s*\\n");

    // Regex pattern for markdown section headers (## Header)
    // (?m) enables multiline mode (^ matches line starts)
    // ^## matches headers at line start
    static final java.util.regex.Pattern SECTION_HEADER_PATTERN = java.util.regex.Pattern
            .compile("(?m)^## ");

    @ConfigProperty(name = "campaign.chunk.size", defaultValue = "500")
    int chunkSize;

    @ConfigProperty(name = "campaign.chunk.overlap", defaultValue = "50")
    int chunkOverlap;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore; // Neo4j

    @Inject
    EmbeddingModel embeddingModel; // Ollama nomic-embed-text

    @Inject
    LoreAssistant settingAssistant; // For RAG queries regarding the setting

    @Inject
    SessionFactory sessionFactory;

    public void loadSetting(String settingName, String filename, String content) {
        Log.infof("Processing file: %s for setting: %s (size: %d bytes)", filename, settingName, content.length());

        if (content.contains(TOOLS_DOC_SEPARATOR)) {
            String[] parts = content.split(TOOLS_DOC_SEPARATOR);
            Log.infof("Found %d structured sections in %s", parts.length, filename);
            int processedCount = 0;
            for (String part : parts) {
                String trimmed = part.trim();
                // Skip empty parts and parts that are just the separator
                if (!trimmed.isBlank() && !trimmed.equals("============")) {
                    processStructuredMarkdown(settingName, filename, trimmed);
                    processedCount++;
                }
            }
            Log.infof("Processed %d non-empty sections from %s", processedCount, filename);
        } else {
            splitDocument(settingName, filename, content);
        }

        Log.infof("Completed processing file: %s", filename);
    }

    private void processStructuredMarkdown(String settingName, String filename, String content) {
        // Parse YAML frontmatter
        Map<String, String> yamlMetadata = parseYamlFrontmatter(content);
        String cleanContent = removeYamlFrontmatter(content);
        Metadata common = Metadata.from(yamlMetadata)
                .put("settingName", settingName)
                // Note: structured frontmatter includes a filename
                // indicate the source file for traceability
                .put("sourceFile", filename)
                .put("canonical", "true"); // source material

        List<TextSegment> segments = new ArrayList<>();

        if (cleanContent.length() > chunkSize) {
            String[] sections = SECTION_HEADER_PATTERN.split(cleanContent);
            for (int i = 0; i < sections.length; i++) {
                String section = sections[i];
                String sectionTitle = extractFirstLine(section);

                // If section is still too large, chunk it again
                if (section.length() > chunkSize * 2) {
                    Log.infof("Section %d '%s' is large (%d chars), splitting into chunks", i, sectionTitle,
                            section.length());
                    var doc = Document.from(section);
                    var splitter = DocumentSplitters.recursive(chunkSize, chunkOverlap);
                    List<TextSegment> subSegments = splitter.split(doc);

                    for (int j = 0; j < subSegments.size(); j++) {
                        TextSegment subSegment = subSegments.get(j);
                        subSegment.metadata()
                                .put("section", sectionTitle)
                                .put("sectionIndex", i)
                                .put("chunkIndex", j)
                                .merge(common);
                        segments.add(subSegment);
                    }
                } else {
                    TextSegment segment = TextSegment.from(
                            section,
                            Metadata.from("section", sectionTitle)
                                    .put("sectionIndex", i)
                                    .merge(common));
                    segments.add(segment);
                }
            }
        } else {
            TextSegment segment = TextSegment.from(
                    cleanContent,
                    common);
            segments.add(segment);
        }

        // Generate embeddings and store
        Log.infof("Generating embeddings for %d segments from %s", segments.size(), filename);

        // Validate segments before sending to embedding model
        for (int i = 0; i < segments.size(); i++) {
            TextSegment seg = segments.get(i);
            if (seg.text() == null || seg.text().isBlank()) {
                Log.warnf("Skipping empty segment %d in %s", i, filename);
                segments.remove(i);
                i--; // Adjust index after removal
            } else if (seg.text().length() > 8000) {
                Log.warnf("Segment %d in %s is very large (%d chars), may fail", i, filename, seg.text().length());
            }
        }

        if (segments.isEmpty()) {
            Log.warnf("No valid segments to embed for %s", filename);
            return;
        }

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);
        Log.infof("Stored %d embeddings for %s", embeddings.size(), filename);
    }

    // Split document into smaller segments, generate embeddings, and store them
    void splitDocument(String settingName, String filename, String content) {
        var doc = Document.from(content);
        var splitter = DocumentSplitters.recursive(
                chunkSize, // max chunk size in characters
                chunkOverlap // overlap between chunks
        );

        List<TextSegment> segments = splitter.split(doc);
        Log.infof("Split %s into %d chunks", filename, segments.size());

        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            segment.metadata()
                    .put("settingName", settingName)
                    .put("sourceFile", filename)
                    .put("canonical", "true") // source material
                    .put("chunkIndex", i);
        }

        // Validate segments before sending to embedding model
        for (int i = 0; i < segments.size(); i++) {
            TextSegment seg = segments.get(i);
            if (seg.text() == null || seg.text().isBlank()) {
                Log.warnf("Skipping empty segment %d in %s", i, filename);
                segments.remove(i);
                i--; // Adjust index after removal
            } else if (seg.text().length() > 8000) {
                Log.warnf("Segment %d in %s is very large (%d chars), may fail", i, filename, seg.text().length());
            }
        }

        if (segments.isEmpty()) {
            Log.warnf("No valid segments to embed for %s", filename);
            return;
        }

        // Generate embeddings
        Log.infof("Generating embeddings for %d chunks from %s", segments.size(), filename);
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        // Store in Neo4j
        embeddingStore.addAll(embeddings, segments);
        Log.infof("Stored %d embeddings for %s", embeddings.size(), filename);
    }

    private String extractFirstLine(String content) {
        int newlineIndex = content.indexOf('\n');
        if (newlineIndex > 0) {
            return content.substring(0, newlineIndex).trim();
        }
        return content.trim();
    }

    private String removeYamlFrontmatter(String content) {
        return YAML_FRONTMATTER_PATTERN.matcher(content).replaceFirst("").trim();
    }

    private Map<String, String> parseYamlFrontmatter(String content) {
        try {
            // Extract YAML content between --- delimiters using regex
            var matcher = YAML_FRONTMATTER_PATTERN.matcher(content);

            if (!matcher.find()) {
                return new HashMap<>(); // No frontmatter - this is fine
            }

            String yamlContent = matcher.group(1).trim();

            // Parse YAML using Jackson
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            @SuppressWarnings("unchecked")
            Map<String, Object> rawMap = yamlMapper.readValue(yamlContent, Map.class);

            // If aliases exists but name doesn't, use first alias as name
            if (!rawMap.containsKey("name") && rawMap.containsKey("aliases")) {
                Object aliases = rawMap.get("aliases");
                if (aliases instanceof List<?> list && !list.isEmpty()) {
                    rawMap.put("name", list.get(0));
                }
            }

            // Convert to Map<String, String> for metadata
            Map<String, String> result = new HashMap<>();

            Object loreTags = rawMap.get("loreTags");
            if (loreTags instanceof List<?> loreTagList) {
                parseHierarchicalTags(loreTagList, result);
            }

            for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // Skip null values
                if (value == null) {
                    Log.debugf("Skipping null value for YAML key: %s", key);
                    continue;
                }

                // Handle lists as comma-delimited strings
                if (value instanceof List<?> list) {
                    result.put(key,
                            list.stream()
                                    .map(Object::toString)
                                    .collect(Collectors.joining(",")));
                } else {
                    result.put(key, value.toString());
                }
            }
            return result;
        } catch (Exception e) {
            // Document has frontmatter but it's malformed - fail the upload
            Log.errorf(e, "Failed to parse YAML frontmatter: %s", e.getMessage());
            throw new DocumentProcessingException(
                    "Invalid YAML frontmatter: " + e.getMessage(), e);
        }
    }

    /**
     * Parse hierarchical loreTags into structured metadata.
     * Example: "lore/monster/cr/8" → metadata.put("monster.cr", "8")
     *
     * @param loreTags List of hierarchical tags from YAML
     * @param metadata Metadata map to populate
     */
    private void parseHierarchicalTags(List<?> loreTags, Map<String, String> metadata) {
        if (loreTags == null || loreTags.isEmpty()) {
            return;
        }

        boolean contentTypeSet = false;

        for (Object tag : loreTags) {
            String tagStr = tag.toString();

            // Only process tags with "lore/" prefix
            if (!tagStr.startsWith("lore/")) {
                continue;
            }

            // Remove "lore/" prefix
            String path = tagStr.substring(5); // "monster/cr/8"

            // Split into parts: ["monster", "cr", "8"]
            String[] parts = path.split("/");

            if (parts.length == 0) {
                continue;
            }

            // Set contentType from first lore/ tag encountered
            if (!contentTypeSet) {
                metadata.put("contentType", parts[0]);
                contentTypeSet = true;
            }

            final String key;
            final String value;
            // Parse nested properties
            if (parts.length == 2) {
                key = parts[0];
                value = parts[1];
            } else if (parts.length >= 3) {
                // Complex case: "lore/monster/cr/8" → metadata["monster.cr"] = "8"
                // Build dotted key from all parts except the last
                StringBuilder keyBuilder = new StringBuilder(parts[0]);
                for (int i = 1; i < parts.length - 1; i++) {
                    keyBuilder.append(".").append(parts[i]);
                }
                key = keyBuilder.toString();
                value = parts[parts.length - 1].replaceAll("\\s+", " ").trim();
            } else {
                // Simple case: "lore/statblock" → contentType already set, no other value to
                // save
                continue;
            }

            if (value.isEmpty()) {
                continue; // Skip tags with empty values
            }

            // Convert to list if multiple of the same key, e.g.
            // - lore/monster/environment/grassland
            // - lore/monster/environment/hill
            // - lore/monster/environment/mountain
            metadata.merge(key, value, (v1, v2) -> v1 + ", " + v2);
        }
    }

    /**
     * List all ingested files with their settings and embedding counts.
     * Returns a map of setting name -> list of file info maps.
     */
    public Map<String, List<Map<String, Object>>> listIngestedFiles() {
        var session = sessionFactory.openSession();
        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        try {
            // Query for files grouped by setting with counts
            String cypher = """
                    MATCH (n:Document)
                    WHERE n.settingName IS NOT NULL AND n.sourceFile IS NOT NULL
                    RETURN n.settingName as settingName,
                           n.sourceFile as sourceFile,
                           count(*) as embeddingCount
                    ORDER BY n.settingName, n.sourceFile
                    """;

            Iterable<Map<String, Object>> results = session.query(cypher, Map.of());
            results.forEach(row -> {
                String settingName = (String) row.get("settingName");
                String sourceFile = (String) row.get("sourceFile");
                Long embeddingCount = (Long) row.get("embeddingCount");

                result.computeIfAbsent(settingName, k -> new ArrayList<>())
                        .add(Map.of(
                                "sourceFile", sourceFile,
                                "embeddingCount", embeddingCount));
            });
        } catch (Exception e) {
            Log.errorf(e, "Error listing ingested files: %s", e.getMessage());
        }

        return result;
    }

    /**
     * List all available adventures from ingested documents.
     * Adventures are identified by having "adventures" in the sourceFile path.
     * Returns a map of setting name -> list of adventure names (from YAML sources
     * field).
     */
    public Map<String, List<String>> listAdventures() {
        var session = sessionFactory.openSession();
        Map<String, List<String>> result = new HashMap<>();

        try {
            // Query for documents with "adventures" in path and extract sources
            String cypher = """
                    MATCH (n:Document)
                    WHERE n.sourceFile IS NOT NULL
                      AND toLower(n.sourceFile) CONTAINS 'adventures'
                      AND n.sources IS NOT NULL
                    RETURN DISTINCT n.settingName as settingName,
                           n.sources as adventureName
                    ORDER BY n.settingName, n.sources
                    """;

            Iterable<Map<String, Object>> results = session.query(cypher, Map.of());
            results.forEach(row -> {
                String settingName = (String) row.get("settingName");
                String adventureName = (String) row.get("adventureName");

                if (settingName != null && adventureName != null) {
                    result.computeIfAbsent(settingName, k -> new ArrayList<>())
                            .add(adventureName);
                }
            });
        } catch (Exception e) {
            Log.errorf(e, "Error listing adventures: %s", e.getMessage());
        }

        return result;
    }

    /**
     * Validate that an adventure exists in the ingested documents for a given
     * setting.
     * Returns true if the adventure is found, false otherwise.
     */
    public boolean validateAdventureExists(String settingName, String adventureName) {
        if (settingName == null || adventureName == null) {
            return false;
        }

        Map<String, List<String>> adventures = listAdventures();
        List<String> settingAdventures = adventures.get(settingName);

        return settingAdventures != null && settingAdventures.contains(adventureName);
    }

    /**
     * Delete all embeddings for a specific file in a setting.
     * Returns the number of embeddings deleted.
     */
    public int deleteFile(String settingName, String sourceFile) {
        var session = sessionFactory.openSession();
        var tx = session.beginTransaction();

        try {
            String cypher = """
                    MATCH (n:Document)
                    WHERE n.settingName = $settingName AND n.sourceFile = $sourceFile
                    WITH n, count(*) as deleteCount
                    DETACH DELETE n
                    RETURN deleteCount
                    """;

            Iterable<Map<String, Object>> results = session.query(cypher,
                    Map.of("settingName", settingName, "sourceFile", sourceFile));

            int deleteCount = 0;
            for (Map<String, Object> row : results) {
                deleteCount = ((Long) row.get("deleteCount")).intValue();
            }

            tx.commit();
            Log.infof("Deleted %d embeddings for file: %s in setting: %s", deleteCount, sourceFile, settingName);
            return deleteCount;
        } catch (Exception e) {
            tx.rollback();
            Log.errorf(e, "Error deleting file: %s", e.getMessage());
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
        } finally {
            tx.close();
        }
    }

    /**
     * Delete all embeddings for a specific setting.
     * Returns the number of embeddings deleted.
     */
    public int deleteSetting(String settingName) {
        var session = sessionFactory.openSession();
        var tx = session.beginTransaction();

        try {
            String cypher = """
                    MATCH (n:Document)
                    WHERE n.settingName = $settingName
                    WITH count(n) as deleteCount
                    MATCH (n:Document)
                    WHERE n.settingName = $settingName
                    DETACH DELETE n
                    RETURN deleteCount
                    """;

            Iterable<Map<String, Object>> results = session.query(cypher, Map.of("settingName", settingName));

            int deleteCount = 0;
            for (Map<String, Object> row : results) {
                deleteCount = ((Long) row.get("deleteCount")).intValue();
            }

            tx.commit();
            Log.infof("Deleted %d embeddings for setting: %s", deleteCount, settingName);
            return deleteCount;
        } catch (Exception e) {
            tx.rollback();
            Log.errorf(e, "Error deleting setting: %s", e.getMessage());
            throw new RuntimeException("Failed to delete setting: " + e.getMessage(), e);
        } finally {
            tx.close();
        }
    }

    /**
     * Result of batch document ingestion.
     * Contains lists of successfully processed files and any errors encountered.
     */
    public record IngestResult(
            List<String> processedFiles,
            List<FileError> errors,
            int successCount) {

        public IngestResult(List<String> processedFiles, List<FileError> errors) {
            this(processedFiles, errors, processedFiles.size());
        }

        public boolean hasErrors() {
            return errors != null && !errors.isEmpty();
        }

        public boolean allFailed() {
            return successCount == 0 && hasErrors();
        }
    }

    public record FileError(String fileName, String errorMessage) {
    }

    /**
     * Ingest multiple document files for a setting.
     * Processes all files and returns a result with successes and failures.
     *
     * @param settingName The setting name to associate documents with
     * @param files       List of file uploads to process
     * @return IngestResult containing processed files and any errors
     */
    public IngestResult ingestDocuments(String settingName,
            List<org.jboss.resteasy.reactive.multipart.FileUpload> files) {
        List<String> processedFiles = new ArrayList<>();
        List<FileError> errors = new ArrayList<>();

        for (var file : files) {
            try {
                String content = java.nio.file.Files.readString(file.uploadedFile());
                loadSetting(settingName, file.fileName(), content);
                processedFiles.add(file.fileName());
            } catch (Exception e) {
                Log.errorf(e, "Error processing file: %s", file.fileName());
                errors.add(new FileError(file.fileName(), e.getMessage()));
            }
        }

        return new IngestResult(processedFiles, errors);
    }
}
