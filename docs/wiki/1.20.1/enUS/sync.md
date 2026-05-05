# Sync Bridge

`PiDataGraphSync` saves payloads through PiSerializeKit and sends envelopes through PiNet.

```java
CompoundTag payload = new CompoundTag();
payload.putDouble("damage", 12.5);

PiDataGraphState state = new PiDataGraphState(
        new ResourceLocation("examplemod", "fire_hit"),
        revision,
        payload);

PiSyncEnvelope envelope = PiDataGraphSync.full(PiSyncRoute.TRACKING, state);
netService.sendSyncEnvelope(player, envelope);
```

Receive:

```java
PiDataGraphSync.receive(runtime, new ResourceLocation("examplemod", "fire_hit"), state -> {
    CompoundTag payload = state.payload;
    long revision = state.revision;
});
```

Available methods:

| Method | Purpose |
| --- | --- |
| `full(route, state)` | Creates a full sync envelope |
| `decode(envelope)` | Reads `PiDataGraphState` from an envelope |
| `receive(runtime, graphId, consumer)` | Registers a receiver for one graph id |
| `matcher(graphId)` | Creates a PiNet matcher |
| `target(graphId)` | Creates a PiNet target |

`PiDataGraphState` contains `graphId`, `revision`, and `payload`. `revision` must be at least 0.
