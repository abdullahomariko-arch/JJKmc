package me.axebanz.jJK;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerProfile {
    public final UUID uuid;

    public int ce = 10;

    public String techniqueId = null;
    public boolean techniqueEnabled = false;

    public long nullifiedUntilMs = 0L;

    public final Map<String, Long> cooldowns = new HashMap<>();

    public long regenLockedUntilMs = 0L;

    public long isohReapplyUntilMs = 0L;
    public String isohReapplyTargetUuid = null;

    public String wheelMode = "MELEE";
    public final Map<String, Integer> wheelStacks = new HashMap<>();

    public int wheelTier = 0;
    public int wheelHitCounter = 0;

    public boolean copyBonded = false;
    public String copiedTechniqueId = null;
    public long copiedErasedAtMs = 0L;

    public final Map<String, Integer> copyCharges = new HashMap<>();

    public String rikaStorageBase64 = null;

    public String rikaEntityUuid = null;

    public boolean permaDead = false;
    public String permaDeadTechniqueId = null;
    public boolean seanceBindingVowActive = false;
    public String seanceReincarnatedUuid = null;
    public boolean isReincarnated = false;

    public String seanceSpawnWorld = null;
    public double seanceSpawnX = 0;
    public double seanceSpawnY = 0;
    public double seanceSpawnZ = 0;

    public boolean strawDollBindingVowActive = false;

    // ===== Ten Shadows Shikigami =====
    public String summonedEntityUuid = null;
    public String summonedShikigamiId = null;
    public String ritualShikigamiId = null;
    public final Map<String, Long> summonCooldowns = new HashMap<>();
    public final Map<String, ShikigamiState> shikigamiStates = new HashMap<>();

    /** Shadow Storage — serialised inventory base64 (like rikaStorageBase64) */
    public String shadowStorageBase64 = null;

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
    }
}