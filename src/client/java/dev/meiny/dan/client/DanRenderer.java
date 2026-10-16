package dev.meiny.dan.client;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import dev.meiny.dan.entity.DanEntity;
import dev.meiny.dan.entity.ModEntityTypes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class DanRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<DanEntity, R> {
    public DanRenderer(EntityRendererProvider.Context context) {
        super(context, ModEntityTypes.DAN);
        withScale(0.9375f);
    }

    @Override
    public RenderType getRenderType(R renderState, Identifier texture) {
        if (!renderState.isInvisible) {
            return RenderTypes.entityTranslucentCull(texture);
        }
        return super.getRenderType(renderState, texture);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> renderPassInfo, BoneSnapshots snapshots) {
        snapshots.get("head").ifPresent(bone -> {
            float pitch = renderPassInfo.getOrDefaultGeckolibData(DataTickets.ENTITY_PITCH, 0f);
            float yaw = renderPassInfo.getOrDefaultGeckolibData(DataTickets.ENTITY_YAW, 0f);

            bone.setRotX(bone.getRotX() - pitch * Mth.DEG_TO_RAD);
            bone.setRotY(bone.getRotY() - yaw * Mth.DEG_TO_RAD);
        });
    }
}