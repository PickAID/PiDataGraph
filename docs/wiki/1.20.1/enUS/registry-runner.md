# Datapack Registries and Runner

PiDataGraph can put actions in a Minecraft datapack registry. Runtime code then reads actions from `RegistryAccess` instead of keeping a separate reload cache.

## Register the Action Datapack Registry

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

`PiDataPackSync.SYNC_TO_CLIENT` supplies both server and network codecs. `PiDataPackSync.SERVER_ONLY` registers only the server codec.

## Bind Input

```java
public record HitInput(double base, double power, LivingEntity target, DamageSource source) {
}

public enum HitBinder implements PiGraphContextBinder<HitInput> {
    INSTANCE;

    @Override
    public PiGraphContextSchema schema() {
        return PiGraphContextSchema.builder("hit")
                .number("base")
                .number("power")
                .object("target", LivingEntity.class)
                .object("damageSource", DamageSource.class)
                .build();
    }

    @Override
    public PiGraphContext bind(HitInput input) {
        return PiGraphContext.builder()
                .number("base", input.base())
                .number("power", input.power())
                .object("target", input.target())
                .object("damageSource", input.source())
                .build();
    }
}
```

## Execute

```java
PiEngineRunner<HitInput> runner =
        PiEngineRunner.actionRegistry(SPELL_ACTIONS_KEY, HitBinder.INSTANCE);

PiEngineFrame frame = runner.run(
        server.registryAccess(),
        new ResourceLocation("examplemod", "fire_hit"),
        input);
```

You can also run from a `PiDataSet<PiEngineAction>`:

```java
PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);
PiEngineFrame frame = runner.run(dataSet, new ResourceLocation("examplemod", "fire_hit"), input);
```

`runner.run(...)` checks the binder contract and action contract before execution.

## Verify All Actions

```java
runner.verifyAll(dataSet);
runner.verifyAll(server.registryAccess());
```

Failures include the entry id and field path:

```text
examplemod:fire_hit/context.numbers.power: missing engine context number `power`
```
