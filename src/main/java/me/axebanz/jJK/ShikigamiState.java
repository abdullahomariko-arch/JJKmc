package me.axebanz.jJK;

public enum ShikigamiState {
    /** Not yet unlocked — requires ritual to tame */
    LOCKED,
    /** Unlocked and available for summoning */
    UNLOCKED,
    /** Currently summoned in the world */
    ACTIVE,
    /** Permanently destroyed — may trigger fusion */
    DESTROYED,
    /** Created via fusion of destroyed shikigami */
    FUSED_UNLOCKED
}