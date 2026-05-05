# 同步桥接

`PiDataGraphSync` 用 PiSerializeKit 保存 payload，用 PiNet 发送 envelope。

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

接收：

```java
PiDataGraphSync.receive(runtime, new ResourceLocation("examplemod", "fire_hit"), state -> {
    CompoundTag payload = state.payload;
    long revision = state.revision;
});
```

可用方法：

| 方法 | 作用 |
| --- | --- |
| `full(route, state)` | 创建 full sync envelope |
| `decode(envelope)` | 从 envelope 读回 `PiDataGraphState` |
| `receive(runtime, graphId, consumer)` | 注册指定 graph id 的接收器 |
| `matcher(graphId)` | 创建 PiNet matcher |
| `target(graphId)` | 创建 PiNet target |

`PiDataGraphState` 字段为 `graphId`、`revision`、`payload`。`revision` 必须大于等于 0。
