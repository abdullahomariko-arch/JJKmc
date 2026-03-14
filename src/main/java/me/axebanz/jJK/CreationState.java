package me.axebanz.jJK;

import java.util.UUID;

public final class CreationState {

    private UUID playerUuid;
    private CreationCategory currentCategory = CreationCategory.BLOCKS;

    public CreationState(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public CreationCategory getCurrentCategory() { return currentCategory; }

    public void shuffleNext() {
        this.currentCategory = currentCategory.next();
    }

    public void setCategory(CreationCategory category) {
        this.currentCategory = category;
    }

    public void reset() {
        this.currentCategory = CreationCategory.BLOCKS;
    }
}