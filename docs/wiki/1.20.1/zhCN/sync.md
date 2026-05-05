# 同步

PiDataGraph 的同步层不重复造网络和序列化轮子：

- `PiDataGraphState` 用 PiSerializeKit 的 `@PiSyncModel` 描述 payload。
- `PiDataGraphSync.full(...)` 把 state 编成 PiNet 的 `PiSyncEnvelope`。
- `PiDataGraphSync.receive(...)` 在 PiNet 的 `PiSyncRuntime` 上注册接收逻辑。

```java
CompoundTag payload = new CompoundTag();
payload.putDouble("damage", 12.5);

PiDataGraphState state = new PiDataGraphState(id("fire_hit"), revision, payload);
PiSyncEnvelope envelope = PiDataGraphSync.full(PiSyncRoute.TRACKING, state);

netService.sendSyncEnvelope(player, envelope);
```

接收：

```java
PiDataGraphSync.receive(netService.syncRuntime(), id("fire_hit"), state -> {
    double damage = state.payload.getDouble("damage");
    // update local cache or rebuild preview runtime
});
```

这层适合同步“已经被上层定义好的图状态”。公式、action registry、数据文件本身仍然应该走 datapack / resource reload，而不是塞进实时网络包。
