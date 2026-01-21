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
    // Positive lookahead (?=^##) splits before headers without consuming them
    static final java.util.regex.Pattern SECTION_HEADER_PATTERN = java.util.regex.Pattern
            .compile("(?m)(?=^## )");

    @ConfigProperty(name = "campaign.chunk.size", defaultValue = "500")
    int chunkSize;

    @ConfigProperty(name = "campaign.chunk.overlap", defaultValue = "50")
    int chunkOverlap;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore; // Neo4j

    @Inject
    EmbeddingModel embeddingModel; // Ollama nomic-embed-text

    @Inject
    SessionFactory sessionFactory;

    public void ingestFile(String filename, String content) {
        Log.infof("Processing file: %s (size: %d bytes)", filename, content.length());

        if (content.contains(TOOLS_DOC_SEPARATOR)) {
            String[] parts = content.split(TOOLS_DOC_SEPARATOR);
            Log.infof("Found %d structured sections in %s", parts.length, filename);
            int processedCount = 0;
            for (String part : parts) {
                String trimmed = part.trim();
                // Skip empty parts and parts that are just the separator
                if (!trimmed.isBlank() && !trimmed.equals("============")) {
                    processStructuredMarkdown(filename, trimmed);
                    processedCount++;
                }
            }
            Log.infof("Processed %d non-empty sections from %s", processedCount, filename);
        } else {
            processStructuredMarkdown(filename, content.trim());
        }

        Log.infof("Completed processing file: %s", filename);
    }

    private void processStructuredMarkdown(String filename, String content) {
        // Parse YAML frontmatter
        // Note: structured frontmatter includes the real filename
        // Keep ingest sourceFile for traceability + allow re-processing
        Map<String, Object> yamlMetadata = parseYamlFrontmatter(content);
        yamlMetadata.put("sourceFile", filename);
        yamlMetadata.put("canonical", "true");

        Metadata common = Metadata.from(yamlMetadata);

        String cleanContent = removeYamlFrontmatter(content)
                .replaceAll("\\^[a-z0-9]+$", ""); // replace block references

        String prefix = "";
        if (yamlMetadata.containsKey("adventureName")) {
            prefix += "Adventure: %s\n\n".formatted(yamlMetadata.get("adventureName"));
        }
        if (yamlMetadata.containsKey("chapterName")) {
            prefix += "Chapter %s: %s\n\n".formatted(yamlMetadata.get("chapterNumber"), yamlMetadata.get("chapterName"));
        }

        List<TextSegment> segments = new ArrayList<>();

        if (prefix.length() + cleanContent.length() > chunkSize) {
            String[] sections = SECTION_HEADER_PATTERN.split(cleanContent);
            int sectionIndex = 0;
            for (String section : sections) {
                if (section.isBlank()) {
                    continue;
                }
                String sectionTitle = extractFirstLine(section);
                String enrichedSection = prefix + section;

                // If section is still too large, chunk it again
                if (enrichedSection.length() > chunkSize * 2) {
                    Log.infof("Section %d '%s' is large (%d chars), splitting into chunks", sectionIndex, sectionTitle,
                            section.length());
                    var doc = Document.from(enrichedSection);
                    var splitter = DocumentSplitters.recursive(chunkSize, chunkOverlap);
                    List<TextSegment> subSegments = splitter.split(doc);

                    int chunkIndex = 0;
                    for (TextSegment subSegment : subSegments) {
                        subSegment.metadata()
                                .put("section", sectionTitle)
                                .put("sectionIndex", sectionIndex)
                                .put("chunkIndex", chunkIndex++)
                                .put("sourceFile", filename)
                                .put("canonical", "true");
                        subSegment.metadata().putAll(yamlMetadata);
                        segments.add(subSegment);
                    }
                } else {
                    TextSegment segment = TextSegment.from(
                            enrichedSection,
                            common);
                    segment.metadata()
                            .put("section", sectionTitle)
                            .put("sectionIndex", sectionIndex)
                            .put("chunkIndex", 0);
                    segments.add(segment);
                }
                sectionIndex++;
            }
        } else if (!cleanContent.isBlank()) {
            TextSegment segment = TextSegment.from(
                    prefix + cleanContent,
                    common);
            segment.metadata()
                    .put("sectionIndex", 0)
                    .put("chunkIndex", 0);
            segments.add(segment);
        }

        // Generate embeddings and store
        Log.infof("Generating embeddings for %d segments from %s", segments.size(), filename);

        if (segments.isEmpty()) {
            Log.warnf("No valid segments to embed for %s", filename);
            return;
        }

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);
        Log.infof("Stored %d embeddings for %s", embeddings.size(), filename);
    }

    private String extractFirstLine(String content) {
        int newlineIndex = content.indexOf('\n');
        if (newlineIndex > 0) {
            return content.substring(0, newlineIndex)
                    .replaceAll("^#* ", "")
                    .trim();
        }
        return content.trim();
    }

    private String removeYamlFrontmatter(String content) {
        return YAML_FRONTMATTER_PATTERN.matcher(content).replaceFirst("").trim();
    }

    private Map<String, Object> parseYamlFrontmatter(String content) {
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
            Map<String, Object> result = new HashMap<>();

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
    private void parseHierarchicalTags(List<?> loreTags, Map<String, Object> metadata) {
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

            if (parts.length == 0 || "compendium".equals(parts[0])) {
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
     * List all ingested files with their embedding counts.
     * Returns a list of file info maps.
     */
    public List<Map<String, Object>> listIngestedFiles() {
        var session = sessionFactory.openSession();
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            String cypher = """
                    MATCH (n:Document)
                    WHERE n.sourceFile IS NOT NULL
                    RETURN n.sourceFile as sourceFile,
                           count(*) as embeddingCount
                    ORDER BY n.sourceFile
                    """;

            Iterable<Map<String, Object>> results = session.query(cypher, Map.of());
            results.forEach(row -> {
                String sourceFile = (String) row.get("sourceFile");
                Long embeddingCount = (Long) row.get("embeddingCount");

                result.add(Map.of(
                        "sourceFile", sourceFile,
                        "embeddingCount", embeddingCount));
            });
        } catch (Exception e) {
            Log.errorf(e, "Error listing ingested files: %s", e.getMessage());
        }

        return result;
    }

    /**
     * Delete all embeddings for a specific file.
     * Returns the number of embeddings deleted.
     */
    public int deleteFile(String sourceFile) {
        var session = sessionFactory.openSession();
        var tx = session.beginTransaction();

        try {
            String cypher = """
                    MATCH (n:Document)
                    WHERE n.sourceFile = $sourceFile
                    WITH count(n) as deleteCount
                    MATCH (n:Document)
                    WHERE n.sourceFile = $sourceFile
                    DETACH DELETE n
                    RETURN deleteCount
                    """;

            Iterable<Map<String, Object>> results = session.query(cypher,
                    Map.of("sourceFile", sourceFile));

            int deleteCount = 0;
            for (Map<String, Object> row : results) {
                deleteCount = ((Long) row.get("deleteCount")).intValue();
            }

            tx.commit();
            Log.infof("Deleted %d embeddings for file: %s", deleteCount, sourceFile);
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
     * Delete all document embeddings.
     * Returns the number of embeddings deleted.
     */
    public int deleteAllDocuments() {
        var session = sessionFactory.openSession();
        var tx = session.beginTransaction();

        try {
            String cypher = """
                    MATCH (n:Document)
                    WITH count(n) as deleteCount
                    MATCH (n:Document)
                    DETACH DELETE n
                    RETURN deleteCount
                    """;

            Iterable<Map<String, Object>> results = session.query(cypher, Map.of());

            int deleteCount = 0;
            for (Map<String, Object> row : results) {
                deleteCount = ((Long) row.get("deleteCount")).intValue();
            }

            tx.commit();
            Log.infof("Deleted %d document embeddings", deleteCount);
            return deleteCount;
        } catch (Exception e) {
            tx.rollback();
            Log.errorf(e, "Error deleting documents: %s", e.getMessage());
            throw new RuntimeException("Failed to delete documents: " + e.getMessage(), e);
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
     * Ingest multiple document files.
     * Processes all files and returns a result with successes and failures.
     *
     * @param files List of file uploads to process
     * @return IngestResult containing processed files and any errors
     */
    public IngestResult ingestDocuments(List<org.jboss.resteasy.reactive.multipart.FileUpload> files) {
        List<String> processedFiles = new ArrayList<>();
        List<FileError> errors = new ArrayList<>();

        for (var file : files) {
            try {
                String content = java.nio.file.Files.readString(file.uploadedFile());
                ingestFile(file.fileName(), content);
                processedFiles.add(file.fileName());
            } catch (Exception e) {
                Log.errorf(e, "Error processing file: %s", file.fileName());
                errors.add(new FileError(file.fileName(), e.getMessage()));
            }
        }

        return new IngestResult(processedFiles, errors);
    }
}
