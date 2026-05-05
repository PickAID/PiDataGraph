# 数据包注册表和 Runner

PiDataGraph 可以把 action 放进 Minecraft datapack registry。这样下游代码从 `RegistryAccess` 取 action，而不是自己维护一份 reload cache。

## 注册 action 数据包注册表

```java
public static final ResourceKey<Registry<PiEngineAction>> SPELL_ACTIONS_KEY =
        ResourceKey.createRegistryKey(new ResourceLocation("examplemod", "spell_actions"));

@SubscribeEvent
public static void newDataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
    PiDataPackRegistries.action(
            event,
            SPELL_ACTIONS_KEY,
            SPELL_ACTIONS,
            PiDataPackSync.SYNC_TO_CLIENT);
}
```

`PiDataPackSync.SYNC_TO_CLIENT` 会给 registry 同时提供 network codec。`PiDataPackSync.SERVER_ONLY` 只注册服务端 codec。

## 绑定输入

```java
public record HitInput(double base, double power, LivingEntity target, DamageSource source) {
}

public enum HitBinder implements PiEngineContextBinder<HitInput> {
    INSTANCE;

    @Override
    public PiEngineContextContract contract() {
        return PiEngineContextContract.builder()
                .number("base")
                .number("power")
                .object("target", LivingEntity.class)
                .object("damageSource", DamageSource.class)
                .build();
    }

    @Override
    public PiEngineContext bind(HitInput input) {
        return PiEngineContext.builder()
                .number("base", input.base())
                .number("power", input.power())
                .object("target", input.target())
                .object("damageSource", input.source())
                .build();
    }
}
```

## 执行

```java
PiEngineRunner<HitInput> runner =
        PiEngineRunner.actionRegistry(SPELL_ACTIONS_KEY, HitBinder.INSTANCE);

PiEngineFrame frame = runner.run(
        server.registryAccess(),
        new ResourceLocation("examplemod", "fire_hit"),
        input);
```

也可以从 `PiDataSet<PiEngineAction>` 执行：

```java
PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);
PiEngineFrame frame = runner.run(dataSet, new ResourceLocation("examplemod", "fire_hit"), input);
```

`runner.run(...)` 会先校验 binder contract 和 action contract，然后再执行。

## 验证所有 action

```java
runner.verifyAll(dataSet);
runner.verifyAll(server.registryAccess());
```

验证失败会带上 entry id 和字段路径，例如：

```text
examplemod:fire_hit/context.numbers.power: missing engine context number `power`
```
