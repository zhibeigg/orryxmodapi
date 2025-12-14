# OrryxModAPI

OrryxMod 服务端API，提供与OrryxMod客户端通信的接口。

## 环境要求

- Minecraft 1.12.2
- TabooLib 6.2.4+
- Java 8+

## 安装

### Gradle (Kotlin DSL)

```kotlin
repositories {
    // 添加你的仓库
}

dependencies {
    compileOnly("org.minelegend:orryxmodapi:1.0-SNAPSHOT")
}
```

## API 使用

### 获取实例

```java
// Java
OrryxModAPI api = OrryxModAPI.getInstance();

// Kotlin
val api = OrryxModAPI
```

### 版本检查

```java
if (OrryxModAPI.isSupportedVersion()) {
    // 当前版本支持
}
```

## 功能列表

### 1. 瞄准系统

#### 普通瞄准请求

```java
// 使用回调方式
OrryxModAPI.requestAiming(
    player,           // 目标玩家
    "fireball",       // 技能ID
    "fire.png",       // 指示图图片
    2.0,              // 指示图大小
    10.0,             // 瞄准半径（方块）
    aimInfo -> {
        // 成功回调
        Location loc = aimInfo.getLocation();
        String skillId = aimInfo.getSkillId();
    },
    error -> {
        // 失败回调
        if (error instanceof OrryxModAPI.PlayerCancelledException) {
            // 玩家取消
        }
    }
);

// 使用 CompletableFuture
OrryxModAPI.requestAimingAsync(player, "fireball", "fire.png", 2.0, 10.0)
    .thenAccept(aimInfo -> {
        // 处理结果
    })
    .exceptionally(ex -> {
        // 处理异常
        return null;
    });
```

#### 蓄力瞄准请求

```java
// 使用回调方式
OrryxModAPI.requestPressAiming(
    player,           // 目标玩家
    "charge_skill",   // 技能ID
    "charge.png",     // 指示图图片
    1.0,              // 初始大小
    5.0,              // 最大大小
    15.0,             // 瞄准半径
    100L,             // 最大蓄力Tick
    aimInfo -> { /* 成功 */ },
    error -> { /* 失败 */ }
);

// 使用 CompletableFuture
OrryxModAPI.requestPressAimingAsync(player, "charge_skill", "charge.png", 1.0, 5.0, 15.0, 100L);
```

### 2. 视觉效果

#### 鬼影效果

移动中产生跟随身体的魂影。

```java
OrryxModAPI.applyGhostEffect(
    viewer,     // 可视玩家
    target,     // 效果目标玩家
    3000L,      // 持续时间（毫秒）
    5,          // 密度
    100         // 间隔
);
```

#### 闪影效果

原地留下一道虚影。

```java
OrryxModAPI.applyFlickerEffect(
    viewer,     // 可视玩家
    target,     // 效果目标玩家
    2000L,      // 持续时间（毫秒）
    0.8f,       // 透明度（0.0-1.0）
    500L,       // 淡化时间（-1为不淡化）
    1.0f        // 缩放
);
```

#### 实体投影效果

在指定地点投影一个实体虚影。

```java
// 添加投影
OrryxModAPI.applyEntityShowEffect(
    viewer,         // 可视玩家
    entityUUID,     // 实体UUID
    "group1",       // 组标识
    location,       // 位置
    5000L,          // 持续时间（毫秒）
    0f, 0f, 0f,     // X/Y/Z轴旋转
    1.0f            // 缩放
);

// 移除投影
OrryxModAPI.removeEntityShowEffect(viewer, entityUUID, "group1");
```

### 3. 地震波效果

#### 圆形地震波

```java
// 使用坐标
OrryxModAPI.sendCircleShockwave(player, x, y, z, radius);

// 使用 Location
OrryxModAPI.sendCircleShockwave(player, location, 5.0);
```

#### 方形地震波

```java
// 使用坐标
OrryxModAPI.sendSquareShockwave(player, x, y, z, width, length, yaw);

// 使用 Location
OrryxModAPI.sendSquareShockwave(player, location, 3.0, 5.0, 90.0);
```

#### 扇形地震波

```java
// 使用坐标
OrryxModAPI.sendSectorShockwave(player, x, y, z, radius, yaw, angle);

// 使用 Location
OrryxModAPI.sendSectorShockwave(player, location, 5.0, 0.0, 60.0);
```

### 4. 导航系统

```java
// 开始导航
OrryxModAPI.startPlayerNavigation(
    player,     // 玩家
    100,        // X坐标
    64,         // Y坐标
    200,        // Z坐标
    5           // 目标范围半径
);

// 停止导航
OrryxModAPI.stopPlayerNavigation(player);
```

## 数据类

### AimInfo

瞄准结果信息。

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `getPlayer()` | `Player` | 获取玩家 |
| `getLocation()` | `Location` | 获取瞄准位置 |
| `getSkillId()` | `String` | 获取技能ID |
| `getTimestamp()` | `long` | 获取时间戳 |

## 异常类

| 异常 | 说明 |
|------|------|
| `UnsupportedVersionException` | 当前版本不支持（非1.12.2） |
| `PlayerCancelledException` | 玩家取消操作 |

## 协议类型

```java
public enum PacketType {
    AIM_REQUEST(1),
    AIM_CONFIRM(2),
    GHOST(3),
    AIM_RESPONSE(4),
    FLICKER(5),
    PRESS_AIM_REQUEST(6),
    MOUSE_REQUEST(7),
    ENTITY_SHOW(8),
    ENTITY_SHOW_REMOVE(9),
    PLAYER_NAVIGATION(10),
    PLAYER_NAVIGATION_STOP(11),
    SQUARE_SHOCKWAVE(12),
    CIRCLE_SHOCKWAVE(13),
    SECTOR_SHOCKWAVE(14)
}
```

## 通信频道

- 频道名称: `orryxmod:main`

## 许可证

MIT License
