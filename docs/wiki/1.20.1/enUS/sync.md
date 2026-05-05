# Sync

PiDataGraph sync consumes the lower-level core modules instead of duplicating them:

- `PiDataGraphState` uses PiSerializeKit `@PiSyncModel`.
- `PiDataGraphSync.full(...)` creates a PiNet `PiSyncEnvelope`.
- `PiDataGraphSync.receive(...)` registers a receiver on PiNet `PiSyncRuntime`.

```java
CompoundTag payload = new CompoundTag();
payload.putDouble("damage", 12.5);

PiDataGraphState state = new PiDataGraphState(id("fire_hit"), revision, payload);
PiSyncEnvelope envelope = PiDataGraphSync.full(PiSyncRoute.TRACKING, state);

netService.sendSyncEnvelope(player, envelope);
```

Use this for runtime graph state. Definitions and action JSON should still be loaded through datapacks/resource reload.
