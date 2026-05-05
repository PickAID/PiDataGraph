# PiDataGraph Java 完整接入链路

这份文档不讲抽象概念，直接给一套完整样例。

样例目标：

```text
examplemod:fire_hit
当 Java 代码触发它时：
1. 检查 power > 0 且 cooldown <= 0
2. 对 target 造成 base + power * 2 点伤害
3. 返回 accepted = true
4. 返回 final_damage = base + power * 2
```

这里用的是 PiDataGraph 当前已经存在的 API：

```text
PiEngineAction
PiEngineActionType
PiEngineActionRegistry
PiEngineContext
PiEngineContextKey
PiEngineNumberKey
PiEngineContextContract
PiEngineRunner
PiDataBuildContext
PiDataDefinition
PiDataCatalog
PiDataProvider
PiDataPackRegistries
```

文中 `ExampleHitRunner`、`ExampleHitContext`、`DamageTargetAction` 是下游模组自己写的接入层，不是 PiDataGraph 里已经存在的类。它们的作用是把 PiDataGraph 接到真实游戏代码上。

## 文件结构

完整样例需要这些文件：

```text
src/main/java/com/example/examplemod/ExampleMod.java
src/main/java/com/example/examplemod/graph/ExampleGraph.java
src/main/java/com/example/examplemod/graph/HitInput.java
src/main/java/com/example/examplemod/graph/ExampleHitContext.java
src/main/java/com/example/examplemod/graph/DamageTargetAction.java
src/main/java/com/example/examplemod/graph/ExampleHitRunner.java
src/main/java/com/example/examplemod/graph/HitResult.java
src/main/java/com/example/examplemod/graph/ExampleDataRegistries.java
src/main/java/com/example/examplemod/graph/ExampleReloadEvents.java
src/main/java/com/example/examplemod/data/ExampleHitActionData.java
src/main/java/com/example/examplemod/data/ExampleDataGen.java
src/main/java/com/example/examplemod/game/ExampleSkillLogic.java
src/generated/resources/data/examplemod/examplemod/hit_action/fire_hit.json
```

如果使用 datagen，最后那个 JSON 由 `ExampleHitActionData` 生成，通常落在 `src/generated/resources`。如果不使用 datagen，就把同样的 JSON 手写到 `src/main/resources/data/examplemod/examplemod/hit_action/fire_hit.json`。

## 第 1 步：模组入口

文件：

```text
src/main/java/com/example/examplemod/ExampleMod.java
```

代码：

```java
package com.example.examplemod;

import net.minecraftforge.fml.common.Mod;

@Mod(ExampleMod.MODID)
public final class ExampleMod {
    public static final String MODID = "examplemod";

    public ExampleMod() {
    }
}
```

这一步只提供 `MODID`，后面的注册都会用它。

## 第 2 步：声明 PiDataGraph 内容和 action 注册表

文件：

```text
src/main/java/com/example/examplemod/graph/ExampleGraph.java
```

代码：

```java
package com.example.examplemod.graph;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.pickaid.pidatagraph.data.PiDataDefinition;
import org.pickaid.pidatagraph.engine.action.PiEngineAction;
import org.pickaid.pidatagraph.engine.action.PiEngineActionData;
import org.pickaid.pidatagraph.engine.action.PiEngineActionRegistry;
import org.pickaid.pidatagraph.engine.action.PiEngineActions;
import org.pickaid.pidatagraph.engine.predicate.PiEnginePredicates;

public final class ExampleGraph {
    public static final ResourceKey<Registry<PiEngineAction>> HIT_ACTIONS =
            ResourceKey.createRegistryKey(id("hit_action"));

    public static final PiEngineActionRegistry ACTIONS = PiEngineActionRegistry.builder()
            .install(PiEngineActions.core())
            .installPredicates(PiEnginePredicates.core())
            .add(DamageTargetAction.TYPE)
            .build();

    public static final PiDataDefinition<PiEngineAction> HIT_ACTION_DEFINITION =
            PiEngineActionData.definition(id("hit_action"), "examplemod/hit_action", ACTIONS);

    private ExampleGraph() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(ExampleMod.MODID, path);
    }
}
```

这里做了三件事：

```text
HIT_ACTIONS            Minecraft datapack registry 的 key
ACTIONS                PiDataGraph action type 注册表
HIT_ACTION_DEFINITION  datagen 和校验用的数据定义
```

`ACTIONS` 里安装了 PiDataGraph 自带的 core action：

```text
pidatagraph:sequence
pidatagraph:if
pidatagraph:repeat
pidatagraph:with_number
pidatagraph:with_context
pidatagraph:guard
pidatagraph:for_each_object
pidatagraph:emit_number
pidatagraph:emit_flag
pidatagraph:emit_object
```

然后加了一个业务 action：

```text
examplemod:damage_target
```

## 第 3 步：定义一次执行的 Java 输入

文件：

```text
src/main/java/com/example/examplemod/graph/HitInput.java
```

代码：

```java
package com.example.examplemod.graph;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public record HitInput(
        ServerPlayer actor,
        LivingEntity target,
        ItemStack weapon,
        DamageSource damageSource,
        double base,
        double power,
        double cooldown
) {
}
```

这就是 Java 事件里本来就有或能算出来的信息。

例如：

```text
actor         谁触发
target        目标
weapon        使用的物品
damageSource  伤害来源
base          基础伤害
power         本次强度
cooldown      当前冷却
```

## 第 4 步：把 Java 输入绑定成 PiEngineContext

文件：

```text
src/main/java/com/example/examplemod/graph/ExampleHitContext.java
```

代码：

```java
package com.example.examplemod.graph;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.PiEngineContextBinder;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.context.PiEngineNumberKey;

public enum ExampleHitContext implements PiEngineContextBinder<HitInput> {
    INSTANCE;

    public static final PiEngineContextKey<ServerPlayer> ACTOR =
            PiEngineContextKey.of("actor", ServerPlayer.class);
    public static final PiEngineContextKey<LivingEntity> TARGET =
            PiEngineContextKey.of("target", LivingEntity.class);
    public static final PiEngineContextKey<ItemStack> WEAPON =
            PiEngineContextKey.of("weapon", ItemStack.class);
    public static final PiEngineContextKey<DamageSource> DAMAGE_SOURCE =
            PiEngineContextKey.of("damageSource", DamageSource.class);
    public static final PiEngineNumberKey BASE = PiEngineNumberKey.of("base");
    public static final PiEngineNumberKey POWER = PiEngineNumberKey.of("power");
    public static final PiEngineNumberKey COOLDOWN = PiEngineNumberKey.of("cooldown");

    @Override
    public PiEngineContextContract contract() {
        return PiEngineContextContract.builder()
                .number(BASE)
                .number(POWER)
                .number(COOLDOWN)
                .object(ACTOR)
                .object(TARGET)
                .object(WEAPON)
                .object(DAMAGE_SOURCE)
                .build();
    }

    @Override
    public PiEngineContext bind(HitInput input) {
        return PiEngineContext.builder()
                .object(ACTOR, input.actor())
                .object(TARGET, input.target())
                .object(WEAPON, input.weapon())
                .object(DAMAGE_SOURCE, input.damageSource())
                .number(BASE, input.base())
                .number(POWER, input.power())
                .number(COOLDOWN, input.cooldown())
                .build();
    }
}
```

这一步是整条链路里最关键的一步。

数据文件里写的公式仍然使用变量名：

```text
base
power
cooldown
```

Java 侧不需要反复手写这些字符串，`PiEngineNumberKey` 会把变量名收成常量，`contract()`、`bind()`、读取 `PiEngineFrame` 都能复用同一个 key。

Java action 只能稳定读取这里提供的对象：

```text
actor
target
weapon
damageSource
```

`contract()` 用于 reload 或 datagen 阶段检查 JSON。比如 JSON 里写了 `missingPower`，但这里没有提供这个变量，校验应该失败。
不用再单独手写 `VALIDATION`，`PiEngineContextBinder.validationContext()` 会从 `contract()` 生成校验上下文。

执行时，`PiEngineRunner` 也会检查 `bind(input)` 生成的 context 是否真的满足 `contract()`。也就是说，如果这里声明了 `power`，但 `bind()` 忘了 `.number(POWER, input.power())`，错误会在 action 执行前直接报出来。

## 第 5 步：实现一个业务 action

文件：

```text
src/main/java/com/example/examplemod/graph/DamageTargetAction.java
```

代码：

```java
package com.example.examplemod.graph;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.PiEngineFrame;
import org.pickaid.pidatagraph.engine.action.PiEngineAction;
import org.pickaid.pidatagraph.engine.action.PiEngineActionType;
import org.pickaid.pidatagraph.engine.action.PiEngineActions;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;

public record DamageTargetAction(PiDoubleExpression amount) implements PiEngineAction {
    public static final PiEngineActionType<DamageTargetAction> TYPE = PiEngineActionType.of(
            ExampleGraph.id("damage_target"),
            actionCodec -> RecordCodecBuilder.create(instance -> instance.group(
                    PiDoubleExpression.CODEC.fieldOf("amount").forGetter(DamageTargetAction::amount)
            ).apply(instance, DamageTargetAction::new))
    );

    @Override
    public PiEngineActionType<?> type() {
        return TYPE;
    }

    @Override
    public PiEngineContextContract contextContract() {
        return PiEngineContextContract.builder()
                .object(ExampleHitContext.TARGET)
                .object(ExampleHitContext.DAMAGE_SOURCE)
                .build();
    }

    @Override
    public PiEngineFrame execute(PiEngineContext context) {
        LivingEntity target = context.object(ExampleHitContext.TARGET).orElseThrow();
        DamageSource source = context.object(ExampleHitContext.DAMAGE_SOURCE).orElseThrow();
        double resolvedAmount = context.evaluate(amount);

        target.hurt(source, (float) resolvedAmount);

        return PiEngineFrame.builder()
                .number("damage_done", resolvedAmount)
                .build();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        contextContract().verify(context, path + ".context");
        PiEngineActions.verify(path + ".amount", context, amount);
    }
}
```

这个 action 对应 JSON 里的：

```json
{
  "type": "examplemod:damage_target",
  "amount": "base + power * 2"
}
```

执行时：

```text
target       从 PiEngineContext 的 object 里取
damageSource 从 PiEngineContext 的 object 里取
amount       从 JSON 里的公式计算
```

然后真正调用：

```java
target.hurt(source, (float) resolvedAmount);
```

这就是 PiDataGraph 和真实游戏行为的连接点。

## 第 6 步：注册 Forge datapack registry

文件：

```text
src/main/java/com/example/examplemod/graph/ExampleDataRegistries.java
```

代码：

```java
package com.example.examplemod.graph;

import com.example.examplemod.ExampleMod;
import org.pickaid.pidatagraph.data.PiDataPackRegistries;
import org.pickaid.pidatagraph.data.PiDataPackSync;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DataPackRegistryEvent;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ExampleDataRegistries {
    private ExampleDataRegistries() {
    }

    @SubscribeEvent
    public static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        PiDataPackRegistries.action(
                event,
                ExampleGraph.HIT_ACTIONS,
                ExampleGraph.ACTIONS,
                PiDataPackSync.SYNC_TO_CLIENT);
    }
}
```

这一步告诉 Minecraft：

```text
请把 data/<namespace>/examplemod/hit_action/*.json
加载成 PiEngineAction
并放进 examplemod:hit_action 这个 registry
```

## 第 7 步：写 runner

文件：

```text
src/main/java/com/example/examplemod/graph/ExampleHitRunner.java
```

代码：

```java
package com.example.examplemod.graph;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.pickaid.pidatagraph.engine.PiEngineFrame;
import org.pickaid.pidatagraph.engine.PiEngineRunner;
import org.pickaid.pidatagraph.engine.context.PiEngineFlagKey;
import org.pickaid.pidatagraph.engine.context.PiEngineNumberKey;

public final class ExampleHitRunner {
    private static final PiEngineFlagKey ACCEPTED = PiEngineFlagKey.of("accepted");
    private static final PiEngineNumberKey FINAL_DAMAGE = PiEngineNumberKey.of("final_damage");
    private static final PiEngineRunner<HitInput> RUNNER =
            PiEngineRunner.actionRegistry(ExampleGraph.HIT_ACTIONS, ExampleHitContext.INSTANCE);

    private ExampleHitRunner() {
    }

    public static HitResult run(ServerLevel level, ResourceLocation actionId, HitInput input) {
        return RUNNER.run(level.registryAccess(), actionId, input, ExampleHitRunner::result);
    }

    public static PiEngineRunner<HitInput> runner() {
        return RUNNER;
    }

    private static HitResult result(PiEngineFrame frame) {
        return new HitResult(
                frame.flagOr(ACCEPTED, false),
                frame.numberOr(FINAL_DAMAGE, 0.0D));
    }
}
```

业务代码以后只调用：

```java
HitResult result = ExampleHitRunner.run(level, new ResourceLocation("examplemod", "fire_hit"), input);
```

不用自己重复写：

```text
查 registry
创建 PiEngineContext
执行 PiEngineAction
解析 PiEngineFrame
处理缺失 action
reload 校验 registry 内容
```

文件：

```text
src/main/java/com/example/examplemod/graph/HitResult.java
```

代码：

```java
package com.example.examplemod.graph;

public record HitResult(boolean accepted, double finalDamage) {
}
```

## 第 8 步：每次 datapack reload 后校验 registry 内容

文件：

```text
src/main/java/com/example/examplemod/graph/ExampleReloadEvents.java
```

代码：

```java
package com.example.examplemod.graph;

import com.example.examplemod.ExampleMod;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public final class ExampleReloadEvents {
    private ExampleReloadEvents() {
    }

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ExampleHitRunner.runner().reloadVerifier(event.getRegistryAccess()));
    }
}
```

如果 JSON 写成这样：

```json
{
  "type": "examplemod:damage_target",
  "amount": "base + missingPower * 2"
}
```

服务器启动或 `/reload` 后应该失败，因为 `ExampleHitContext.contract()` 只允许：

```text
base
power
cooldown
```

## 第 9 步：写 datagen

文件：

```text
src/main/java/com/example/examplemod/data/ExampleHitActionData.java
```

代码：

```java
package com.example.examplemod.data;

import com.example.examplemod.graph.DamageTargetAction;
import java.util.List;
import org.pickaid.pidatagraph.data.PiDataGenEntry;
import org.pickaid.pidatagraph.data.PiDataSet;
import org.pickaid.pidatagraph.engine.action.PiEmitFlagAction;
import org.pickaid.pidatagraph.engine.action.PiEmitNumberAction;
import org.pickaid.pidatagraph.engine.action.PiEngineAction;
import org.pickaid.pidatagraph.engine.action.PiGuardAction;
import org.pickaid.pidatagraph.engine.action.PiSequenceAction;
import org.pickaid.pidatagraph.engine.predicate.PiExpressionPredicate;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;

public final class ExampleHitActionData implements PiDataGenEntry<PiEngineAction> {
    @Override
    public void register(PiDataSet.Builder<PiEngineAction> builder) {
        builder.entry("fire_hit", new PiGuardAction(
                PiExpressionPredicate.of("power > 0 & cooldown <= 0"),
                new PiSequenceAction(List.of(
                        new DamageTargetAction(PiDoubleExpression.of("base + power * 2")),
                        new PiEmitFlagAction("accepted", PiExpressionPredicate.of("1")),
                        new PiEmitNumberAction("final_damage", PiDoubleExpression.of("base + power * 2"))
                ))
        ));
    }
}
```

文件：

```text
src/main/java/com/example/examplemod/data/ExampleDataGen.java
```

代码：

```java
package com.example.examplemod.data;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.graph.ExampleGraph;
import com.example.examplemod.graph.ExampleHitContext;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.pickaid.pidatagraph.data.PiDataCatalog;
import org.pickaid.pidatagraph.data.PiDataProvider;
import org.pickaid.pidatagraph.data.PiDataSet;
import org.pickaid.pidatagraph.engine.action.PiEngineAction;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ExampleDataGen {
    private ExampleDataGen() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        PiDataSet<PiEngineAction> hitActions = PiDataCatalog.builder(
                        ExampleGraph.HIT_ACTION_DEFINITION,
                        ExampleMod.MODID)
                .add(new ExampleHitActionData())
                .buildSet();

        event.getGenerator().addProvider(
                event.includeServer(),
                new PiDataProvider(
                        event.getGenerator().getPackOutput(),
                        "ExampleMod hit actions",
                        ExampleHitContext.INSTANCE.validationContext(),
                        hitActions));
    }
}
```

这段 datagen 会生成下面这个 JSON。

## 第 10 步：最终 JSON 数据

文件：

```text
src/generated/resources/data/examplemod/examplemod/hit_action/fire_hit.json
```

完整内容：

```json
{
  "type": "pidatagraph:guard",
  "predicate": {
    "type": "pidatagraph:expression",
    "value": "power > 0 & cooldown <= 0"
  },
  "child": {
    "type": "pidatagraph:sequence",
    "children": [
      {
        "type": "examplemod:damage_target",
        "amount": "base + power * 2"
      },
      {
        "type": "pidatagraph:emit_flag",
        "name": "accepted",
        "predicate": {
          "type": "pidatagraph:expression",
          "value": "1"
        }
      },
      {
        "type": "pidatagraph:emit_number",
        "name": "final_damage",
        "value": "base + power * 2"
      }
    ]
  }
}
```

这是整个示例唯一必须存在的 PiDataGraph JSON。

它的读取路径来自 registry key：

```text
examplemod:hit_action
```

所以文件夹是：

```text
data/examplemod/examplemod/hit_action/
```

它的 entry id 是：

```text
examplemod:fire_hit
```

所以文件名是：

```text
fire_hit.json
```

## 第 11 步：业务代码调用

文件：

```text
src/main/java/com/example/examplemod/game/ExampleSkillLogic.java
```

代码：

```java
package com.example.examplemod.game;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.graph.ExampleHitRunner;
import com.example.examplemod.graph.HitInput;
import com.example.examplemod.graph.HitResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class ExampleSkillLogic {
    private static final ResourceLocation FIRE_HIT =
            new ResourceLocation(ExampleMod.MODID, "fire_hit");

    private ExampleSkillLogic() {
    }

    public static HitResult fireHit(ServerPlayer player, LivingEntity target) {
        ServerLevel level = (ServerLevel) player.level();

        HitInput input = new HitInput(
                player,
                target,
                player.getMainHandItem(),
                player.damageSources().playerAttack(player),
                4.0D,
                3.0D,
                0.0D);

        return ExampleHitRunner.run(level, FIRE_HIT, input);
    }
}
```

这次输入是：

```text
base = 4
power = 3
cooldown = 0
```

JSON 里的 guard：

```text
power > 0 & cooldown <= 0
```

计算结果：

```text
3 > 0 & 0 <= 0
= true
```

JSON 里的伤害公式：

```text
base + power * 2
```

计算结果：

```text
4 + 3 * 2
= 10
```

所以执行结果是：

```text
target.hurt(player.damageSources().playerAttack(player), 10.0F)
```

`ExampleHitRunner` 会把 frame 映射成：

```text
accepted = true
finalDamage = 10
```

业务代码如果需要读取结果：

```java
HitResult result = ExampleSkillLogic.fireHit(player, target);

boolean accepted = result.accepted();
double damage = result.finalDamage();
```

## 第 12 步：整条链路实际发生了什么

按执行顺序看：

```text
1. Forge 触发 DataPackRegistryEvent.NewRegistry
2. ExampleDataRegistries 注册 examplemod:hit_action
3. Minecraft 加载 data/examplemod/examplemod/hit_action/fire_hit.json
4. PiEngineActionRegistry 用 type 字段解码 JSON
5. examplemod:fire_hit 进入 registry
6. AddReloadListenerEvent 安装校验 listener
7. 每次 datapack reload 后，ExampleHitRunner.verifyAll 检查所有 hit action
8. 业务代码调用 ExampleSkillLogic.fireHit
9. ExampleSkillLogic 创建 HitInput
10. ExampleHitRunner 从 registry 取 examplemod:fire_hit
11. ExampleHitContext.bind 把 HitInput 变成 PiEngineContext
12. PiEngineRunner 检查 binder contract，确认 context 真的提供 base、power、cooldown、target 等输入
13. PiEngineRunner 检查 action contract，确认 action 需要的输入也都存在
14. pidatagraph:guard 执行 predicate
15. predicate 读取 power 和 cooldown
16. guard 通过后执行 pidatagraph:sequence
17. sequence 执行 examplemod:damage_target
18. damage_target 读取 target 和 damageSource
19. damage_target 计算 base + power * 2
20. damage_target 调用 target.hurt
21. sequence 执行 pidatagraph:emit_flag
22. sequence 执行 pidatagraph:emit_number
23. sequence 合并所有 PiEngineFrame
24. ExampleHitRunner 把 PiEngineFrame 映射成 HitResult
25. ExampleHitRunner.run 返回 HitResult
```

这就是 PiDataGraph 的真实职责：

```text
它不替你写伤害逻辑。
它让数据决定何时调用伤害逻辑，以及用什么公式调用。
```

## 这套样例里每个类的职责

```text
ExampleGraph
注册 action type，声明 datapack registry，提供 datagen 定义。

HitInput
一次执行的 Java 输入。

ExampleHitContext
把 HitInput 绑定成 PiEngineContext，并提供校验用的变量和对象列表。

DamageTargetAction
一个真实业务 action。它从 context 取 target 和 damageSource，计算 amount，然后伤害实体。

ExampleDataRegistries
把 examplemod:hit_action 注册成 Forge datapack registry。

ExampleHitRunner
业务入口。它负责查 registry、绑定 context、执行 action，并把 frame 映射成 HitResult。

HitResult
业务层读取的执行结果。

ExampleReloadEvents
每次 datapack reload 后校验所有加载出来的 action。

ExampleHitActionData
用 Java 生成 fire_hit.json。

ExampleDataGen
把 PiDataProvider 接进 Forge datagen。

ExampleSkillLogic
真实业务调用点。
```

## 如果要扩展第二个 action

比如要加：

```text
examplemod:set_cooldown
```

只需要做四件事：

```text
1. 写 SetCooldownAction implements PiEngineAction
2. 在 ExampleGraph.ACTIONS 里 add(SetCooldownAction.TYPE)
3. 在 JSON 里加 {"type": "examplemod:set_cooldown", ...}
4. 在 verify 里检查它需要的 context 和表达式
```

不用改：

```text
PiEngineActionRegistry
PiEngineContext
PiEngineFrame
PiDataProvider
Forge datapack registry 注册方式
```

这就是 registry-first action 系统的扩展点。

## 这套写法为什么不需要手动查 registry

样例里的重复部分已经被收进 PiDataGraph：

```text
PiEngineContextBinder
PiEngineRunner
PiDataPackRegistries
PiDataProvider
```

下游项目只保留和自己业务有关的部分：

```text
HitInput
ExampleHitContext
DamageTargetAction
ExampleHitActionData
ExampleSkillLogic
```

也就是说，Java 代码只负责说明“这次执行有哪些输入”和“真正改变游戏世界的叶子 action 怎么做”。查 datapack registry、绑定 context、执行 action、reload 后校验这些固定流程由 PiDataGraph 提供。
