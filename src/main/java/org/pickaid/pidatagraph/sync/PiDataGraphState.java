package org.pickaid.pidatagraph.sync;

import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.pickaid.piserializekit.api.schema.PiField;
import org.pickaid.piserializekit.api.schema.PiSyncModel;
import org.pickaid.piserializekit.api.schema.PiSyncScope;

@PiSyncModel(id = "pidatagraph:data_graph_state", version = 1)
public final class PiDataGraphState {
    @PiField(id = "graph_id", sync = PiSyncScope.GLOBAL, persist = true)
    public ResourceLocation graphId = new ResourceLocation("minecraft", "empty");

    @PiField(id = "revision", sync = PiSyncScope.GLOBAL, persist = true)
    public long revision;

    @PiField(id = "payload", sync = PiSyncScope.GLOBAL, persist = true)
    public CompoundTag payload = new CompoundTag();

    public PiDataGraphState() {
    }

    public PiDataGraphState(ResourceLocation graphId, long revision, CompoundTag payload) {
        this.graphId = Objects.requireNonNull(graphId, "graphId");
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must be >= 0");
        }
        this.revision = revision;
        this.payload = Objects.requireNonNull(payload, "payload").copy();
    }
}
