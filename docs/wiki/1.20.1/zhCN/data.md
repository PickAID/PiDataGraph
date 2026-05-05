# 公式和数据

PiDataGraph 的数据入口是 Minecraft datapack JSON。字段可以是普通 Codec 类型，也可以是 `PiDoubleExpression`、`PiIntExpression`、`PiBooleanExpression`。表达式在加载阶段编译，运行时只传入本次事件的 context。

## 定义一个数据对象

```java
public record SpellFormula(
        PiDoubleExpression damage,
        PiIntExpression cooldown,
        PiBooleanExpression enabled
) {
    public static final Codec<SpellFormula> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PiDoubleExpression.CODEC.fieldOf("damage").forGetter(SpellFormula::damage),
            PiIntExpression.CODEC.fieldOf("cooldown").forGetter(SpellFormula::cooldown),
            PiBooleanExpression.CODEC.fieldOf("enabled").forGetter(SpellFormula::enabled)
    ).apply(instance, SpellFormula::new));
}
```

对应 JSON：

```json
{
  "damage": "baseDamage + spellPower * 2 - targetArmor",
  "cooldown": "max(5, baseCooldown - haste)",
  "enabled": "mana >= cost & cooldownReady == 1"
}
```

表达式字段在 JSON 里就是字符串。这样数据文件容易写，Java 端仍然能用强类型对象。

## 声明数据类型

`PiDataDefinition` 负责三件事：数据 id、数据文件夹、Codec 和加载校验。

```java
public static final PiDataDefinition<SpellFormula> SPELL_FORMULA_DATA =
        PiDataDefinition.<SpellFormula>builder(
                new ResourceLocation("examplemod", "spell_formulas"),
                "spell_formulas",
                SpellFormula.CODEC)
        .verify("damage", (context, spec) ->
                spec.damage().compile(context.expressionLanguage(), context.expressionScope()))
        .verify("cooldown", (context, spec) ->
                spec.cooldown().compile(context.expressionLanguage(), context.expressionScope()))
        .verify("enabled", (context, spec) ->
                spec.enabled().compile(context.expressionLanguage(), context.expressionScope()))
        .build();
```

`folder = "spell_formulas"` 会读取：

```text
data/<namespace>/spell_formulas/<path>.json
```

## 编译成运行时库

如果数据对象需要预编译，使用 `PiEngineContentType` 把原始数据变成运行时对象。

```java
public record CompiledSpellFormula(
        ResourceLocation id,
        PiCompiledDoubleExpression damage,
        PiCompiledIntExpression cooldown,
        PiCompiledBooleanExpression enabled
) {
}
```

```java
public static final PiEngineContentType<SpellFormula, CompiledSpellFormula> SPELL_FORMULAS =
        PiEngineContentType.builder(SPELL_FORMULA_DATA, (entry, context) -> new CompiledSpellFormula(
                entry.id(),
                entry.value().damage().compile(context.expressionLanguage(), context.expressionScope()),
                entry.value().cooldown().compile(context.expressionLanguage(), context.expressionScope()),
                entry.value().enabled().compile(context.expressionLanguage(), context.expressionScope())))
        .scope(PiExpressionScope.of(
                "baseDamage",
                "spellPower",
                "targetArmor",
                "baseCooldown",
                "haste",
                "mana",
                "cost",
                "cooldownReady"))
        .build();
```

运行时只需要 `PiEngineContext`：

```java
CompiledSpellFormula fireball = formulas.require(new ResourceLocation("examplemod", "fireball"));

double damage = context.evaluate(fireball.damage());
int cooldown = context.evaluate(fireball.cooldown());
boolean enabled = context.evaluate(fireball.enabled());
```

## 服务器 reload 接入

服务器端读取 datapack 数据时，注册 `AddReloadListenerEvent`。它会在进世界和 `/reload` 时重新加载。

```java
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public final class ExampleDataReloads {
    private static volatile PiEngineLibrary<CompiledSpellFormula> formulas =
            PiEngineLibrary.<CompiledSpellFormula>builder().build();

    private ExampleDataReloads() {
    }

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        PiEngineBuildContext buildContext = PiEngineBuildContext.standard()
                .withScope(SPELL_FORMULAS.scope());

        event.addListener(new PiDataReloadListener<>(SPELL_FORMULA_DATA, buildContext.dataContext()) {
            @Override
            protected void applyData(
                    PiDataSet<SpellFormula> data,
                    ResourceManager resourceManager,
                    ProfilerFiller profiler
            ) {
                formulas = PiEngineLibrary.compile(data, buildContext, SPELL_FORMULAS);
            }
        });
    }

    public static CompiledSpellFormula requireFormula(ResourceLocation id) {
        return formulas.require(id);
    }
}
```

`AddReloadListenerEvent` 负责“从已经启用的数据包里读 JSON”。如果你只是读取玩家启用的数据包，用它就够了。`AddPackFindersEvent` 只在你要给模组塞一个内置可选数据包时使用。

## Datagen

用 `PiDataCatalog` 写数据，用 `PiDataProvider` 输出 JSON。Provider 可以带校验 context，这样数据生成阶段也会抓表达式错误。

```java
PiDataSet<SpellFormula> spells = PiDataCatalog.builder(SPELL_FORMULA_DATA, "examplemod")
        .add(builder -> builder.entry("fireball", new SpellFormula(
                PiDoubleExpression.of("baseDamage + spellPower * 2 - targetArmor"),
                PiIntExpression.of("max(5, baseCooldown - haste)"),
                PiBooleanExpression.of("mana >= cost & cooldownReady == 1"))))
        .buildSet();

gen.addProvider(server, new PiDataProvider(
        output,
        "Example Spell Formulas",
        PiEngineBuildContext.standard()
                .withScope(SPELL_FORMULAS.scope())
                .dataContext(),
        spells));
```

输出文件：

```text
data/examplemod/spell_formulas/fireball.json
```

## 表达式能力

| 类型 | 内容 |
| --- | --- |
| 数学 | `+`, `-`, `*`, `/`, `%`, `^` |
| 比较 | `<`, `<=`, `>`, `>=`, `==`, `!=` |
| 逻辑 | `&`, `|` |
| 函数 | `min`, `max`, `clamp`, `abs`, `floor`, `ceil`, `round`, `sqrt`, `pow`, `sin`, `cos`, `tan`, `rand` |

自定义函数：

```java
PiExpressionLanguage language = PiExpressionLanguage.standardBuilder()
        .function("scale_by_power", 2, (context, args) ->
                args[0] * context.variables().get("spellPower") + args[1])
        .build();
```

把自定义语言放进 build context 和 runtime context，数据加载与运行时就会使用同一套规则。

## 错误路径

校验错误会带数据 id 和字段路径：

```text
examplemod:fireball/damage: invalid expression `baseDamage + missing` for variables [baseDamage, spellPower, targetArmor]
```

这比运行时炸在某个技能调用里更容易查。数据越复杂，越应该把表达式和 context 契约写进加载阶段。
