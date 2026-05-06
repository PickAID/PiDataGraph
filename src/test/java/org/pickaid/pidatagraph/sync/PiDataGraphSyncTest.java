package org.pickaid.pidatagraph.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;
import org.pickaid.pinet.api.runtime.PiNetworkContext;
import org.pickaid.pinet.api.sync.model.PiSyncEnvelope;
import org.pickaid.pinet.api.sync.model.PiSyncEnvelopeKind;
import org.pickaid.pinet.api.sync.model.PiSyncRoute;
import org.pickaid.pinet.api.sync.profile.PiSyncRuntimeProfiles;
import org.pickaid.piserializekit.runtime.schema.registry.PiSchemas;

class PiDataGraphSyncTest {
    @Test
    void graphStateUsesPiSerializeKitSchemaBindings() {
        CompoundTag payload = new CompoundTag();
        payload.putString("spell", "fireball");
        PiDataGraphState state = new PiDataGraphState(id("spell/fireball"), 7L, payload);

        CompoundTag encoded = PiSchemas.saveClientView(state);
        PiDataGraphState decoded = PiSchemas.loadFull(PiDataGraphState.class, encoded);

        assertEquals(id("spell/fireball"), decoded.graphId);
        assertEquals(7L, decoded.revision);
        assertEquals("fireball", decoded.payload.getString("spell"));
    }

    @Test
    void graphStateCanTravelThroughPiNetSyncRuntime() {
        CompoundTag payload = new CompoundTag();
        payload.putDouble("damage", 12.5);
        PiDataGraphState state = new PiDataGraphState(id("spell/fireball"), 3L, payload);
        PiSyncEnvelope envelope = PiDataGraphSync.full(PiSyncRoute.TRACKING, state);
        List<PiDataGraphState> received = new ArrayList<>();
        var runtime = PiSyncRuntimeProfiles.strictScoped().runtime();

        PiDataGraphSync.receive(runtime, id("spell/fireball"), received::add);
        var decision = runtime.accept(envelope, new TestNetworkContext());

        assertTrue(decision.code().accepted());
        assertEquals(PiSyncEnvelopeKind.FULL, envelope.kind());
        assertEquals(PiDataGraphSync.SCHEMA_ID, envelope.schemaId());
        assertEquals(PiDataGraphSync.TARGET_KIND, envelope.target().kind());
        assertEquals("example:spell/fireball", envelope.target().key());
        assertEquals(1, received.size());
        assertEquals(12.5, received.get(0).payload.getDouble("damage"));
    }

    @Test
    void syncBridgeCanUseGeneratedResourceKeys() throws Exception {
        ResourceKey<?> fireball = resourceKey("spell/fireball");
        CompoundTag payload = new CompoundTag();
        payload.putDouble("damage", 12.5);
        PiDataGraphState state = new PiDataGraphState(fireball, 3L, payload);
        PiSyncEnvelope envelope = PiDataGraphSync.full(PiSyncRoute.TRACKING, state);
        List<PiDataGraphState> received = new ArrayList<>();
        var runtime = PiSyncRuntimeProfiles.strictScoped().runtime();

        PiDataGraphSync.receive(runtime, fireball, received::add);
        var decision = runtime.accept(envelope, new TestNetworkContext());

        assertTrue(decision.code().accepted());
        assertEquals(id("spell/fireball"), state.graphId);
        assertEquals("example:spell/fireball", PiDataGraphSync.target(fireball).key());
        assertEquals(1, received.size());
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("example", path);
    }

    private static ResourceKey<?> resourceKey(String path) throws Exception {
        Constructor<ResourceKey> constructor = ResourceKey.class.getDeclaredConstructor(ResourceLocation.class, ResourceLocation.class);
        constructor.setAccessible(true);
        return constructor.newInstance(id("registry"), id(path));
    }

    private static final class TestNetworkContext implements PiNetworkContext {
        @Override
        public boolean clientbound() {
            return true;
        }

        @Override
        public Optional<ServerPlayer> player() {
            return Optional.empty();
        }
    }
}
