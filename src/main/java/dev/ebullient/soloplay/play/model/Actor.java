package dev.ebullient.soloplay.play.model;

import java.util.List;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity("Actor")
public class Actor {
    @Id
    private String id; // human readable slug

    String name;
    private String summary; // Short, stable identifier (e.g., "Aged wizard", "Young warrior")
    private String description; // Full narrative that can evolve over time
    private String characterClass; // e.g., "Fighter", "Wizard"
    private Integer level;

    private List<String> tags;
    private List<String> aliases; // Alternative names (e.g., "Krux" for "Commodore Krux")
}
