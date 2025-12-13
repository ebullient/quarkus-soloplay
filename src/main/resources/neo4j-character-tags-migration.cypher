// Migration script to convert Character entities from type/status enums to tags
// Run this script ONCE after deploying the tag-based Character changes
// This script is idempotent - safe to run multiple times

// Step 1: Initialize tags array for all characters that don't have one
MATCH (c:Character)
WHERE c.tags IS NULL
SET c.tags = [];

// Step 2: Convert CharacterType to tags
// PC -> player-controlled + protagonist
MATCH (c:Character)
WHERE c.type = 'PC' AND c.tags IS NOT NULL
SET c.tags = c.tags + ['player-controlled', 'protagonist'];

// SIDEKICK -> companion
MATCH (c:Character)
WHERE c.type = 'SIDEKICK' AND c.tags IS NOT NULL
SET c.tags = c.tags + ['companion'];

// NPC -> npc (if not already converted to PC or SIDEKICK)
MATCH (c:Character)
WHERE c.type = 'NPC' AND c.tags IS NOT NULL AND size(c.tags) = 0
SET c.tags = c.tags + ['npc'];

// Step 3: Convert CharacterStatus to tags
// DEAD -> dead
MATCH (c:Character)
WHERE c.status = 'DEAD' AND c.tags IS NOT NULL
SET c.tags = c.tags + ['dead'];

// RETIRED -> retired
MATCH (c:Character)
WHERE c.status = 'RETIRED' AND c.tags IS NOT NULL
SET c.tags = c.tags + ['retired'];

// MISSING -> missing
MATCH (c:Character)
WHERE c.status = 'MISSING' AND c.tags IS NOT NULL
SET c.tags = c.tags + ['missing'];

// UNKNOWN -> unknown
MATCH (c:Character)
WHERE c.status = 'UNKNOWN' AND c.tags IS NOT NULL
SET c.tags = c.tags + ['unknown'];

// ACTIVE status doesn't need a tag (it's the default, absence of status tags means active)

// Step 4: Remove old type and status properties
MATCH (c:Character)
WHERE c.type IS NOT NULL
REMOVE c.type;

MATCH (c:Character)
WHERE c.status IS NOT NULL
REMOVE c.status;

// Step 5: Verify migration - return count of characters with tags
MATCH (c:Character)
RETURN count(c) as totalCharacters,
       count(c.tags) as charactersWithTags,
       collect(DISTINCT c.tags) as uniqueTagCombinations;
