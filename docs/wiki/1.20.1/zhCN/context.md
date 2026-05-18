# 执行上下文

`PiGraphContext` 是一次图执行的 public 输入。number 进入表达式变量表，object 交给 Java action 或 predicate 使用。

`PiEngineContext` 仍作为兼容 alias 和当前 action executor 内部入口保留。新的应用层 binder 应使用 `PiGraphContext`。

## Java 常量 key

```java
public static final PiEngineNumberKey BASE_DAMAGE = PiEngineNumberKey.of("baseDamage");
public static final PiEngineContextKey<LivingEntity> TARGET =
        PiEngineContextKey.of("target", LivingEntity.class);
public static final PiEngineValueKey<Number> HUD_MANA_FILL =
        PiEngineValueKey.number("hud.mana_fill");
```

```java
PiGraphContext context = PiGraphContext.builder()
        .number(BASE_DAMAGE, 6)
        .object(TARGET, target)
        .build();
```

规则：

- context number 必须是表达式变量名，例如 `baseDamage`。
- context object 也使用普通变量名，例如 `target`。
- frame 输出可以用普通变量名，也可以用 dotted path，例如 `hud.mana_fill`。

## 读取执行结果

```java
PiEngineFrame frame = action.execute(context);

double damage = frame.numberOr(PiEngineNumberKey.of("damage"), 0);
boolean accepted = frame.flagOr(PiEngineFlagKey.of("accepted"), false);
double manaFill = frame.numberOr(HUD_MANA_FILL, 0);
```

`emit_number`、`emit_flag`、`emit_object` 的 `name` 写入 `PiEngineFrame`，因此支持 dotted path：

```json
{
  "type": "pidatagraph:emit_number",
  "name": "hud.mana_fill",
  "value": "mana / maxMana"
}
```

## Minecraft 绑定 helper

typed Java 输入优先使用 `PiGraphContextBindings.record(...)` 或自定义 `PiGraphContextBinder`。

```java
public record SpellCastInput(Object caster, LivingEntity target, double baseDamage) {
}

PiGraphContextBinder<SpellCastInput> binder =
        PiGraphContextBindings.record("spell_cast", SpellCastInput.class);

PiGraphContext context = binder.bind(new SpellCastInput(caster, target, 6));
```

旧的 `PiEngineContextBindings` 仍可用于 Minecraft-specific helper 值：

| 方法 | 写入内容 |
| --- | --- |
| `level` | `level`, `gameTime`, `dayTime`, `clientSide` |
| `random` | `random` object，并接入表达式 `rand()` |
| `entity` | 坐标、旋转、tick、onGround、delta |
| `living` | entity 内容，加 health、maxHealth、absorption、armor |
| `vector` | `Vec3` object，加 X/Y/Z/Length |
| `blockPos` | `BlockPos` object，加 X/Y/Z |
| `itemStack` | `ItemStack` object，加 count、damage、maxDamage、damageRatio、empty |
| `damageSource` | `damageSource` object |
| `hand` | `hand` object |

## Contract

Java action 和旧 binder 用 `PiEngineContextContract` 声明输入。新的 graph binder 用 `PiGraphContextSchema` 声明同一类输入形状。

```java
return PiEngineContextContract.builder()
        .number("baseDamage")
        .object("target", LivingEntity.class)
        .object("damageSource", DamageSource.class)
        .build();
```

```java
return PiGraphContextSchema.builder("spell_cast")
        .number("baseDamage")
        .object("target", LivingEntity.class)
        .object("damageSource", DamageSource.class)
        .build();
```

`PiEngineRunner` 和 `verify(...)` 会用 contract 提前发现缺失 number、缺失 object 和类型错误。
