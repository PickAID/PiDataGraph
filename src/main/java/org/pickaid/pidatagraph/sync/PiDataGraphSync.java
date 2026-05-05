package org.pickaid.pidatagraph.sync;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import org.pickaid.pinet.api.sync.PiSyncConsumer;
import org.pickaid.pinet.api.sync.PiSyncMatcher;
import org.pickaid.pinet.api.sync.PiSyncRuntime;
import org.pickaid.pinet.api.sync.model.PiSyncEnvelope;
import org.pickaid.pinet.api.sync.model.PiSyncEnvelopeKind;
import org.pickaid.pinet.api.sync.model.PiSyncRoute;
import org.pickaid.pinet.api.sync.model.PiSyncTarget;
import org.pickaid.piserializekit.runtime.schema.registry.PiSchemas;

public final class PiDataGraphSync {
    public static final ResourceLocation SCHEMA_ID = new ResourceLocation("pidatagraph", "data_graph_state");
    public static final ResourceLocation TARGET_KIND = new ResourceLocation("pidatagraph", "graph");

    private PiDataGraphSync() {
    }

    public static PiSyncEnvelope full(PiSyncRoute route, PiDataGraphState state) {
        Objects.requireNonNull(state, "state");
        return new PiSyncEnvelope(
                PiSyncEnvelopeKind.FULL,
                Objects.requireNonNull(route, "route"),
                SCHEMA_ID,
                target(state.graphId),
                state.revision,
                PiSchemas.saveClientView(state)
        );
    }

    public static PiDataGraphState decode(PiSyncEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope");
        if (!envelope.schemaId().equals(SCHEMA_ID)) {
            throw new IllegalArgumentException("unexpected PiDataGraph sync schema: " + envelope.schemaId());
        }
        return PiSchemas.loadFull(PiDataGraphState.class, envelope.payload());
    }

    public static void receive(PiSyncRuntime runtime, ResourceLocation graphId, Consumer<PiDataGraphState> consumer) {
        Objects.requireNonNull(runtime, "runtime");
        Objects.requireNonNull(consumer, "consumer");
        runtime.register(matcher(graphId), consumer(consumer));
    }

    public static PiSyncMatcher matcher(ResourceLocation graphId) {
        PiSyncTarget target = target(graphId);
        return PiSyncMatcher.schema(SCHEMA_ID)
                .and(PiSyncMatcher.targetKind(TARGET_KIND))
                .and(envelope -> envelope.target().key().equals(target.key()));
    }

    public static PiSyncTarget target(ResourceLocation graphId) {
        return PiSyncTarget.of(TARGET_KIND, Objects.requireNonNull(graphId, "graphId").toString());
    }

    public static PiSyncConsumer consumer(Consumer<PiDataGraphState> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        return (envelope, context) -> consumer.accept(decode(envelope));
    }
}
