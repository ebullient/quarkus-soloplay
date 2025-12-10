package dev.ebullient.soloplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

@ApplicationScoped
public class CampaignService {
    static final String TOOLS_DOC_SEPARATOR = "============\n";

    // Regex pattern for YAML frontmatter
    // (?s) enables DOTALL mode (. matches newlines)
    // ^--- matches opening ---
    // (.*?) captures content (non-greedy)
    // \n--- matches closing ---
    static final java.util.regex.Pattern YAML_FRONTMATTER_PATTERN =
            java.util.regex.Pattern.compile("(?s)^---\\s*\\n(.*?)\\n---\\s*\\n");

    @ConfigProperty(name = "campaign.chunk.size", defaultValue = "500")
    int chunkSize;

    @ConfigProperty(name = "campaign.chunk.overlap", defaultValue = "50")
    int chunkOverlap;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore; // Neo4j

    @Inject
    EmbeddingModel embeddingModel; // Ollama nomic-embed-text

    @Inject
    SettingAssistant settingAssistant; // For RAG queries regarding the setting

    public void loadSetting(String settingName, String filename, String content) {
        if (content.startsWith(TOOLS_DOC_SEPARATOR)) {
            String[] parts = content.split(TOOLS_DOC_SEPARATOR);
            for (String part : parts) {
                if (!part.isBlank()) {
                    processStructuredMarkdown(settingName, filename, part.trim());
                }
            }
        } else {
            splitDocument(settingName, filename, content);
        }
    }

    private void processStructuredMarkdown(String settingName, String filename, String content) {
        // Parse YAML frontmatter
        Map<String, String> yamlMetadata = parseYamlFrontmatter(content);
        String cleanContent = removeYamlFrontmatter(content);
        Metadata common = Metadata.from(yamlMetadata)
                .put("settingName", settingName)
                .put("sourceFile", filename)
                .put("canonical", "true"); // source material

        List<TextSegment> segments = new ArrayList<>();

        if (cleanContent.length() > chunkSize) {
            String[] sections = content.split("(?m)^## ");
            for (int i = 0; i < sections.length; i++) {
                String section = sections[i];
                String sectionTitle = extractFirstLine(section);

                TextSegment segment = TextSegment.from(
                        section,
                        Metadata.from("section", sectionTitle)
                                .put("sectionIndex", i)
                                .merge(common));
                segments.add(segment);
            }
        } else {
            TextSegment segment = TextSegment.from(
                    cleanContent,
                    common);
            segments.add(segment);
        }

        // Generate embeddings and store
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);
    }

    // Split document into smaller segments, generate embeddings, and store them
    void splitDocument(String settingName, String filename, String content) {
        var doc = Document.from(content);
        var splitter = DocumentSplitters.recursive(
                chunkSize, // max chunk size in characters
                chunkOverlap // overlap between chunks
        );

        List<TextSegment> segments = splitter.split(doc);
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            segment.metadata()
                    .put("settingName", settingName)
                    .put("filename", filename)
                    .put("canonical", "true") // source material
                    .put("chunkIndex", i);
        }

        // Generate embeddings
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        // Store in Neo4j
        embeddingStore.addAll(embeddings, segments);
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
                return new HashMap<>(); // No frontmatter
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
            for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                if (entry.getValue() != null) {
                    result.put(entry.getKey(), entry.getValue().toString());
                }
            }
            return result;
        } catch (Exception e) {
            // Log and return empty map on error
            System.err.println("Error parsing YAML frontmatter: " + e.getMessage());
            return new HashMap<>();
        }
    }
}
