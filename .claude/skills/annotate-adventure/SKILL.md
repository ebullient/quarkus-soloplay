---
name: annotate-adventure
description: |
  Annotate TTRPG adventure modules with GM_NOTE HTML comments for LLM game masters.
  Use when: (1) Preparing adventure text for AI-assisted play, (2) Adding tracking
  annotations to published adventures, (3) Converting adventure modules for use with
  LLM GMs. Adds structured comments that help AI track plot events, NPCs, discoveries,
  player choices, and combat encounters without modifying the original text.
---

# Annotate Adventure

Add `<!-- GM_NOTE: ... -->` HTML comments to adventure text to help LLM GMs track story events.

## Core Rules

1. **Comments guide the LLM, not players** - Mark what to record, what's significant, branching paths
2. **Only annotate trackable moments** - Story advancement, key NPCs, player choices, revelations, consequences
3. **Never modify original text** - Only add HTML comments

## Comment Types

### Chapter Boundaries

**Start** (after heading):
```markdown
<!-- GM_NOTE: CHAPTER START - chapter-N-slug
     Main objectives and themes. Key objectives: 2-4 goals.
     Prerequisites: What must have happened.
     Record with tags: ["plot:chapter-N-start", "location:relevant-location"] -->
```

**End**:
```markdown
<!-- GM_NOTE: CHAPTER END - chapter-N-complete
     What was accomplished. Preview of next chapter.
     Record with tags: ["plot:chapter-N-complete"] -->
```

### Plot Events (required for story progression)
```markdown
<!-- GM_NOTE: PLOT EVENT - event-slug (required)
     What happens and why it matters.
     Record with tags: ["plot:event-slug", "relevant-tag"] -->
```

### Critical Discoveries (information reveals)
```markdown
<!-- GM_NOTE: CRITICAL DISCOVERY - discovery-slug (required)
     What's revealed and why it's important.
     Record with tags: ["plot:discovery-slug", "discovery"] -->
```

### Player Choices (branching decisions)
```markdown
<!-- GM_NOTE: PLOT FLAG - choice-slug
     The choice and its implications.
     Record with tags: ["plot:choice-slug", "decision"] -->
```

### Alternate Paths
```markdown
<!-- GM_NOTE: ALTERNATE PATH - path-description
     How this differs from main route. Track carefully. -->
```

### Combat

**Boss/Major**:
```markdown
<!-- GM_NOTE: MAJOR COMBAT - encounter-slug (required if boss)
     Encounter significance.
     Record with tags: ["plot:encounter-slug", "combat", "creature:type"] -->
```

**Regular**:
```markdown
<!-- GM_NOTE: ENCOUNTER - encounter-slug
     Brief description.
     Record with tags: ["plot:encounter-slug", "combat"] -->
```

### NPC Introduction
```markdown
<!-- GM_NOTE: PLOT EVENT - met-npc-name (required if major)
     Who they are and their significance.
     Record with tags: ["plot:met-npc-name", "npc:npc-name"] -->
```

### Foreshadowing/Hidden Info
```markdown
<!-- GM_NOTE: PLOT INFO - info-slug
     What's hinted and when revealed.
     Record with tags: ["plot:info-slug", "npc:relevant-npc"] -->
```

## Tag Reference

| Category | Format | Examples |
|----------|--------|----------|
| Plot | `plot:event-name` | `plot:chapter-1-start`, `plot:defeated-villain` |
| NPC | `npc:name` | `npc:captain-vex`, `npc:the-oracle` |
| Location | `location:place` | `location:dark-tower`, `location:port-city` |
| Faction | `faction:group` | `faction:thieves-guild`, `faction:empire` |
| Creature | `creature:type` | `creature:dragon`, `creature:undead` |
| Special | bare tag | `discovery`, `decision`, `combat` |
| Crew | `crew:name` | `crew:first-mate-jin` |
| Vehicle | `vehicle:name` | `vehicle:storm-rider` |
| Treasure | `treasure:item` | `treasure:flame-sword` |
| Quest | `quest:name` | `quest:rescue-princess` |

**Always use 2-4 tags per event. Tags are lowercase with hyphens.**

## Placement

**Do place comments:**
- Before major encounters (before read-aloud text)
- Before important revelations
- After complex sections (mark accomplishments)
- At decision points

**Don't place comments:**
- Inside read-aloud text
- In stat blocks
- For minor flavor
- For every room (only significant ones)

## Checklist Per Chapter

- [ ] Chapter START at beginning
- [ ] Chapter END at end
- [ ] Major NPCs have `met-npc-name`
- [ ] Critical discoveries marked
- [ ] Player choices have PLOT FLAG
- [ ] Alternate paths marked
- [ ] Major combat annotated
- [ ] Tags lowercase with hyphens
- [ ] 2-4 tags per event
- [ ] Comments explain WHY not just WHAT
- [ ] Original text unchanged

## Decision Guide

| Situation | Action |
|-----------|--------|
| Could affect future chapters | Annotate |
| Players will ask "did we do this?" | Annotate |
| LLM needs to track for reference | Annotate |
| Just flavor or mechanics | Skip |
