package net.erutobusiness.erutosmobs.advancement;

import com.google.gson.JsonObject;
import net.erutobusiness.erutosmobs.ErutosMobs;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

/**
 * 進捗の引き金 `erutosmobs:shearwater`。条件は {"event": 名前} の 1 つだけ。名前は {@link #SEEN} ほか。
 * 羽根を持つ・嵐渡りの効果を得る・怒りを買うは、バニラの引き金（inventory_changed・effects_changed）で足りるので、ここには無い。
 */
public class ShearwaterTrigger extends SimpleCriterionTrigger<ShearwaterTrigger.Instance> {
    static final ResourceLocation ID = new ResourceLocation(ErutosMobs.MOD_ID, "shearwater");
    public static final ShearwaterTrigger INSTANCE = new ShearwaterTrigger();

    /** 見た（64 ブロック以内で、目の通る所） */
    public static final String SEEN = "seen";
    /** 帯電しているのを見た */
    public static final String SEEN_CHARGED = "seen_charged";
    /** 翼端で波を切るのを 24 ブロック以内で見た */
    public static final String SHEAR_NEAR = "shear_near";
    /** 投げた魚を食べた */
    public static final String FED = "fed";
    /** 船の横に 30 秒並んで飛んだ */
    public static final String FOLLOWED = "followed";
    /** 並んで飛び終えた鳥から羽根をもらった */
    public static final String GIFT = "gift";

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        return new Instance(player, GsonHelper.getAsString(json, "event"));
    }

    public void trigger(ServerPlayer player, String event) {
        this.trigger(player, instance -> instance.event.equals(event));
    }

    public static class Instance extends AbstractCriterionTriggerInstance {
        private final String event;

        Instance(ContextAwarePredicate player, String event) {
            super(ID, player);
            this.event = event;
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            json.addProperty("event", this.event);
            return json;
        }
    }
}
