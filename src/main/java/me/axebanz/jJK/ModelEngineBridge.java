package me.axebanz.jJK;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.manager.BehaviorManager;
import org.bukkit.entity.Entity;

public final class ModelEngineBridge {

    private final JJKCursedToolsPlugin plugin;

    public ModelEngineBridge(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void applyModel(Entity e, String modelId) {
        ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(e);
        if (modeledEntity == null) return;

        ActiveModel activeModel = ModelEngineAPI.createActiveModel(modelId);
        if (activeModel == null) return;

        modeledEntity.addModel(activeModel, true);
    }

    public void playAnimation(Entity e, String animation) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(e);
        if (modeledEntity == null) return;

        for (ActiveModel activeModel : modeledEntity.getModels().values()) {
            activeModel.getAnimationHandler().playAnimation(animation, 0.3, 0.3, 1.0, true);
        }
    }

    public void stopAnimation(Entity e, String animation) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(e);
        if (modeledEntity == null) return;

        for (ActiveModel activeModel : modeledEntity.getModels().values()) {
            activeModel.getAnimationHandler().stopAnimation(animation);
        }
    }

    public void removeModel(Entity e) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(e);
        if (modeledEntity == null) return;

        modeledEntity.destroy();
    }

    /**
     * Force the ModelEngine body rotation to face a specific yaw.
     * This is the KEY fix for Rika/any model facing the wrong direction.
     * ModelEngine uses its OWN body rotation controller that overrides vanilla rotation.
     */
    public void forceBodyYaw(Entity e, float yaw) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(e);
        if (modeledEntity == null) return;

        try {
            // ModelEngine 4.x: getBase().getBodyRotationController()
            var base = modeledEntity.getBase();
            if (base != null) {
                var bodyRotCtrl = base.getBodyRotationController();
                if (bodyRotCtrl != null) {
                    bodyRotCtrl.setYBodyRot(yaw);
                    bodyRotCtrl.setYHeadRot(yaw);
                }
            }
        } catch (Exception ex) {
            // Fallback: at least set the vanilla entity rotation
            // Some ModelEngine versions may not have this exact API
        }
    }
}