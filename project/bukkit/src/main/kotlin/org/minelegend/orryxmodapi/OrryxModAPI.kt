package io.github.zhibei.org.minelegend.orryxmodapi

import com.google.common.io.ByteArrayDataInput
import com.google.common.io.ByteArrayDataOutput
import com.google.common.io.ByteStreams
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.messaging.PluginMessageListener
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.Plugin
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.warning
import taboolib.module.nms.MinecraftVersion
import taboolib.platform.BukkitPlugin
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

/**
 * OrryxMod API - Java友好版本
 * 提供与OrryxMod客户端通信的API接口
 */
@PlatformSide(Platform.BUKKIT)
object OrryxModAPI : Plugin() {

    private const val CHANNEL_NAME = "orryxmod:main"
    internal val pendingRequests = ConcurrentHashMap<UUID, PendingRequest>()

    private var isLegacyVersion: Boolean = false
    private var versionChecked: Boolean = false

    private fun checkVersion(): Boolean {
        if (!versionChecked) {
            isLegacyVersion = MinecraftVersion.versionId == 11202
            versionChecked = true
        }
        return isLegacyVersion
    }

    // 协议类型枚举 - Java友好
    enum class PacketType(val header: Int) {
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

    // 内部请求数据类
    internal class PendingRequest(
        val maxDistance: Double,
        val future: CompletableFuture<AimInfo>
    )

    @Awake(LifeCycle.ENABLE)
    private fun registerChannels() {
        val messenger = Bukkit.getMessenger()
        val plugin = BukkitPlugin.getInstance()
        messenger.registerIncomingPluginChannel(plugin, CHANNEL_NAME, MessageReceiver())
        messenger.registerOutgoingPluginChannel(plugin, CHANNEL_NAME)
    }

    private fun clean() {
        val iterator = pendingRequests.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.value.future.cancel(false)
            iterator.remove()
        }
    }

    @SubscribeEvent
    private fun onPlayerQuit(e: PlayerQuitEvent) {
        cleanupRequest(e.player)
    }

    /* ==================== 公开API - Java友好版本 ==================== */

    /**
     * 获取API实例
     * @return OrryxModAPI实例
     */
    @JvmStatic
    fun getInstance(): OrryxModAPI {
        return this
    }

    /**
     * 检查当前版本是否支持
     * @return 如果是1.12.2版本返回true
     */
    @JvmStatic
    fun isSupportedVersion(): Boolean {
        return checkVersion()
    }

    /**
     * 发起瞄准请求（使用Consumer回调 - Java友好）
     * @param player 目标玩家
     * @param skillId 技能唯一标识
     * @param picture 指示图图片
     * @param size 指示图大小
     * @param radius 瞄准半径（方块）
     * @param onSuccess 成功回调
     * @param onFailure 失败回调
     */
    @JvmStatic
    fun requestAiming(
        player: Player,
        skillId: String,
        picture: String,
        size: Double,
        radius: Double,
        onSuccess: Consumer<AimInfo>,
        onFailure: Consumer<Throwable>
    ) {
        if (!checkVersion()) {
            onFailure.accept(UnsupportedVersionException())
            return
        }

        val future = CompletableFuture<AimInfo>()
        pendingRequests[player.uniqueId] = PendingRequest(radius + size, future)

        future.whenComplete { result, ex ->
            submit {
                if (ex == null && result != null) {
                    onSuccess.accept(result)
                } else {
                    onFailure.accept(ex ?: PlayerCancelledException())
                }
            }
        }

        sendDataPacket(player, PacketType.AIM_REQUEST) { output ->
            output.writeUTF(skillId)
            output.writeUTF(picture)
            output.writeDouble(size)
            output.writeDouble(radius)
        }
    }

    /**
     * 发起瞄准请求（返回CompletableFuture - Java友好）
     * @param player 目标玩家
     * @param skillId 技能唯一标识
     * @param picture 指示图图片
     * @param size 指示图大小
     * @param radius 瞄准半径（方块）
     * @return CompletableFuture<AimInfo>
     */
    @JvmStatic
    fun requestAimingAsync(
        player: Player,
        skillId: String,
        picture: String,
        size: Double,
        radius: Double
    ): CompletableFuture<AimInfo> {
        val resultFuture = CompletableFuture<AimInfo>()

        if (!checkVersion()) {
            resultFuture.completeExceptionally(UnsupportedVersionException())
            return resultFuture
        }

        val future = CompletableFuture<AimInfo>()
        pendingRequests[player.uniqueId] = PendingRequest(radius + size, future)

        future.whenComplete { result, ex ->
            submit {
                if (ex == null && result != null) {
                    resultFuture.complete(result)
                } else {
                    resultFuture.completeExceptionally(ex ?: PlayerCancelledException())
                }
            }
        }

        sendDataPacket(player, PacketType.AIM_REQUEST) { output ->
            output.writeUTF(skillId)
            output.writeUTF(picture)
            output.writeDouble(size)
            output.writeDouble(radius)
        }

        return resultFuture
    }

    /**
     * 发起蓄力瞄准请求（使用Consumer回调 - Java友好）
     * @param player 目标玩家
     * @param skillId 技能唯一标识
     * @param picture 使用的图片组
     * @param minSize 指示图初始大小
     * @param maxSize 指示图最大大小
     * @param radius 瞄准半径（方块）
     * @param maxTick 最大Tick
     * @param onSuccess 成功回调
     * @param onFailure 失败回调
     */
    @JvmStatic
    fun requestPressAiming(
        player: Player,
        skillId: String,
        picture: String,
        minSize: Double,
        maxSize: Double,
        radius: Double,
        maxTick: Long,
        onSuccess: Consumer<AimInfo>,
        onFailure: Consumer<Throwable>
    ) {
        if (!checkVersion()) {
            onFailure.accept(UnsupportedVersionException())
            return
        }

        val future = CompletableFuture<AimInfo>()
        pendingRequests[player.uniqueId] = PendingRequest(radius + maxSize, future)

        future.whenComplete { result, ex ->
            submit {
                if (ex == null && result != null) {
                    onSuccess.accept(result)
                } else {
                    onFailure.accept(ex ?: PlayerCancelledException())
                }
            }
        }

        sendDataPacket(player, PacketType.PRESS_AIM_REQUEST) { output ->
            output.writeUTF(skillId)
            output.writeUTF(picture)
            output.writeDouble(minSize)
            output.writeDouble(maxSize)
            output.writeDouble(radius)
            output.writeLong(maxTick)
        }
    }

    /**
     * 发起蓄力瞄准请求（返回CompletableFuture - Java友好）
     * @param player 目标玩家
     * @param skillId 技能唯一标识
     * @param picture 使用的图片组
     * @param minSize 指示图初始大小
     * @param maxSize 指示图最大大小
     * @param radius 瞄准半径（方块）
     * @param maxTick 最大Tick
     * @return CompletableFuture<AimInfo>
     */
    @JvmStatic
    fun requestPressAimingAsync(
        player: Player,
        skillId: String,
        picture: String,
        minSize: Double,
        maxSize: Double,
        radius: Double,
        maxTick: Long
    ): CompletableFuture<AimInfo> {
        val resultFuture = CompletableFuture<AimInfo>()

        if (!checkVersion()) {
            resultFuture.completeExceptionally(UnsupportedVersionException())
            return resultFuture
        }

        val future = CompletableFuture<AimInfo>()
        pendingRequests[player.uniqueId] = PendingRequest(radius + maxSize, future)

        future.whenComplete { result, ex ->
            submit {
                if (ex == null && result != null) {
                    resultFuture.complete(result)
                } else {
                    resultFuture.completeExceptionally(ex ?: PlayerCancelledException())
                }
            }
        }

        sendDataPacket(player, PacketType.PRESS_AIM_REQUEST) { output ->
            output.writeUTF(skillId)
            output.writeUTF(picture)
            output.writeDouble(minSize)
            output.writeDouble(maxSize)
            output.writeDouble(radius)
            output.writeLong(maxTick)
        }

        return resultFuture
    }

    /**
     * 应用鬼影效果，移动中产生跟随身体的魂影
     * @param viewer 可视玩家
     * @param target 效果目标玩家
     * @param duration 持续时间（毫秒）
     * @param density 密度
     * @param gap 间隔
     */
    @JvmStatic
    fun applyGhostEffect(viewer: Player, target: Player, duration: Long, density: Int, gap: Int) {
        sendDataPacket(viewer, PacketType.GHOST) { output ->
            output.writeUTF(target.uniqueId.toString())
            output.writeLong(duration)
            output.writeInt(density)
            output.writeInt(gap)
        }
    }

    /**
     * 应用闪影效果，原地留下一道虚影
     * @param viewer 可视玩家
     * @param target 效果目标玩家
     * @param timeout 持续时间（毫秒）
     * @param alpha 透明度（0.0-1.0）
     * @param fadeDuration 透明度淡化时间(-1为不淡化)
     * @param scale 缩放
     */
    @JvmStatic
    fun applyFlickerEffect(
        viewer: Player,
        target: Player,
        timeout: Long,
        alpha: Float,
        fadeDuration: Long,
        scale: Float
    ) {
        sendDataPacket(viewer, PacketType.FLICKER) { output ->
            output.writeUTF(target.uniqueId.toString())
            output.writeLong(timeout)
            output.writeFloat(alpha)
            output.writeLong(fadeDuration)
            output.writeFloat(scale)
        }
    }

    /**
     * 应用投影效果，在指定地点投影一个实体虚影
     * @param viewer 可视玩家
     * @param entityId 效果实体UUID
     * @param group 组标识
     * @param location 位置
     * @param timeout 持续时间（毫秒）
     * @param rotateX X轴旋转
     * @param rotateY Y轴旋转
     * @param rotateZ Z轴旋转
     * @param scale 缩放
     */
    @JvmStatic
    fun applyEntityShowEffect(
        viewer: Player,
        entityId: UUID,
        group: String,
        location: Location,
        timeout: Long,
        rotateX: Float,
        rotateY: Float,
        rotateZ: Float,
        scale: Float
    ) {
        if (viewer.world != location.world) {
            return
        }
        sendDataPacket(viewer, PacketType.ENTITY_SHOW) { output ->
            output.writeUTF(entityId.toString())
            output.writeUTF(group)
            output.writeDouble(location.x)
            output.writeDouble(location.y)
            output.writeDouble(location.z)
            output.writeLong(timeout)
            output.writeFloat(rotateX)
            output.writeFloat(rotateY)
            output.writeFloat(rotateZ)
            output.writeFloat(scale)
        }
    }

    /**
     * 删除投影效果
     * @param viewer 可视玩家
     * @param entityId 效果实体UUID
     * @param group 组标识
     */
    @JvmStatic
    fun removeEntityShowEffect(viewer: Player, entityId: UUID, group: String) {
        sendDataPacket(viewer, PacketType.ENTITY_SHOW_REMOVE) { output ->
            output.writeUTF(entityId.toString())
            output.writeUTF(group)
        }
    }

    /**
     * 发起客户端寻路导航
     * @param player 玩家
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param range 目标点范围半径
     */
    @JvmStatic
    fun startPlayerNavigation(player: Player, x: Int, y: Int, z: Int, range: Int) {
        sendDataPacket(player, PacketType.PLAYER_NAVIGATION) { output ->
            output.writeInt(x)
            output.writeInt(y)
            output.writeInt(z)
            output.writeInt(range)
        }
    }

    /**
     * 停止客户端寻路导航
     * @param player 玩家
     */
    @JvmStatic
    fun stopPlayerNavigation(player: Player) {
        sendDataPacket(player, PacketType.PLAYER_NAVIGATION_STOP) { _ -> }
    }

    /**
     * 发送圆形地震波效果
     * @param player 玩家
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param radius 半径
     */
    @JvmStatic
    fun sendCircleShockwave(player: Player, x: Double, y: Double, z: Double, radius: Double) {
        sendDataPacket(player, PacketType.CIRCLE_SHOCKWAVE) { output ->
            output.writeDouble(x)
            output.writeDouble(y)
            output.writeDouble(z)
            output.writeDouble(radius)
        }
    }

    /**
     * 发送圆形地震波效果（使用Location）
     * @param player 玩家
     * @param location 位置
     * @param radius 半径
     */
    @JvmStatic
    fun sendCircleShockwave(player: Player, location: Location, radius: Double) {
        sendCircleShockwave(player, location.x, location.y, location.z, radius)
    }

    /**
     * 发送方形地震波效果
     * @param player 玩家
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param width 宽度
     * @param length 长度
     * @param yaw 方向
     */
    @JvmStatic
    fun sendSquareShockwave(
        player: Player,
        x: Double,
        y: Double,
        z: Double,
        width: Double,
        length: Double,
        yaw: Double
    ) {
        sendDataPacket(player, PacketType.SQUARE_SHOCKWAVE) { output ->
            output.writeDouble(x)
            output.writeDouble(y)
            output.writeDouble(z)
            output.writeDouble(length)
            output.writeDouble(width)
            output.writeDouble(yaw)
        }
    }

    /**
     * 发送方形地震波效果（使用Location）
     * @param player 玩家
     * @param location 位置
     * @param width 宽度
     * @param length 长度
     * @param yaw 方向
     */
    @JvmStatic
    fun sendSquareShockwave(player: Player, location: Location, width: Double, length: Double, yaw: Double) {
        sendSquareShockwave(player, location.x, location.y, location.z, width, length, yaw)
    }

    /**
     * 发送扇形地震波效果
     * @param player 玩家
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param radius 半径
     * @param yaw 方向
     * @param angle 开合角度
     */
    @JvmStatic
    fun sendSectorShockwave(
        player: Player,
        x: Double,
        y: Double,
        z: Double,
        radius: Double,
        yaw: Double,
        angle: Double
    ) {
        sendDataPacket(player, PacketType.SECTOR_SHOCKWAVE) { output ->
            output.writeDouble(x)
            output.writeDouble(y)
            output.writeDouble(z)
            output.writeDouble(radius)
            output.writeDouble(yaw)
            output.writeDouble(angle)
        }
    }

    /**
     * 发送扇形地震波效果（使用Location）
     * @param player 玩家
     * @param location 位置
     * @param radius 半径
     * @param yaw 方向
     * @param angle 开合角度
     */
    @JvmStatic
    fun sendSectorShockwave(player: Player, location: Location, radius: Double, yaw: Double, angle: Double) {
        sendSectorShockwave(player, location.x, location.y, location.z, radius, yaw, angle)
    }

    /* ==================== 内部实现 ==================== */

    private fun handleConfirmation(player: Player, isConfirmed: Boolean): Boolean {
        val request = pendingRequests[player.uniqueId]
        if (request == null) {
            return false
        }
        sendDataPacket(player, PacketType.AIM_CONFIRM) { output ->
            output.writeBoolean(isConfirmed)
        }
        if (!isConfirmed) {
            cleanupRequest(player)
        }
        return true
    }

    private fun cleanupRequest(player: Player) {
        val request = pendingRequests.remove(player.uniqueId)
        if (request != null) {
            request.future.completeExceptionally(PlayerCancelledException())
        }
    }

    private fun sendDataPacket(
        player: Player,
        type: PacketType,
        writer: Consumer<ByteArrayDataOutput>
    ) {
        try {
            val output = ByteStreams.newDataOutput()
            output.writeInt(type.header)
            writer.accept(output)
            player.sendPluginMessage(
                BukkitPlugin.getInstance(),
                CHANNEL_NAME,
                output.toByteArray()
            )
        } catch (ex: Exception) {
            warning("给玩家 ${player.name} 发送数据包失败: ${ex.message}")
        }
    }

    /* ==================== 消息接收处理器 ==================== */

    private class MessageReceiver : PluginMessageListener {
        override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
            if (channel != CHANNEL_NAME) {
                return
            }

            val input = ByteStreams.newDataInput(message)
            try {
                val header = input.readInt()
                if (header == PacketType.AIM_RESPONSE.header) {
                    handleAimResponse(player, input)
                } else {
                    warning("收到未知数据包类型: $header")
                }
            } catch (ex: Exception) {
                warning("处理来自 ${player.name} 的数据包时出错: ${ex.message}")
            }
        }

        private fun handleAimResponse(player: Player, input: ByteArrayDataInput) {
            try {
                val skillId = input.readUTF()
                val location = readLocation(player, input)

                val request = pendingRequests[player.uniqueId]
                val maxDistance = if (request != null) request.maxDistance else 0.0

                if (location.distance(player.location) <= maxDistance) {
                    val removed = pendingRequests.remove(player.uniqueId)
                    if (removed != null) {
                        removed.future.complete(AimInfo(player, location, skillId, System.currentTimeMillis()))
                    }
                } else {
                    pendingRequests.remove(player.uniqueId)
                    warning("玩家${player.name} 向服务器发送了作弊 超远释放${skillId}技能数据包")
                }
            } catch (ex: Exception) {
                warning("解析瞄准数据包失败: ${ex.message}")
            }
        }

        private fun readLocation(player: Player, input: ByteArrayDataInput): Location {
            val x = input.readDouble()
            val y = input.readDouble()
            val z = input.readDouble()
            val yaw = input.readFloat()
            val pitch = input.readFloat()
            return Location(player.world, x, y, z, yaw, pitch)
        }
    }

    /* ==================== 数据类 - Java友好 ==================== */

    /**
     * 瞄准信息类
     */
    class AimInfo(
        private val player: Player,
        private val location: Location,
        private val skillId: String?,
        private val timestamp: Long
    ) {
        /**
         * 获取玩家
         */
        fun getPlayer(): Player = player

        /**
         * 获取瞄准位置
         */
        fun getLocation(): Location = location

        /**
         * 获取技能ID
         */
        fun getSkillId(): String? = skillId

        /**
         * 获取时间戳
         */
        fun getTimestamp(): Long = timestamp

        override fun toString(): String {
            return "AimInfo{player=$player, location=$location, skillId=$skillId, timestamp=$timestamp}"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            other as AimInfo
            return timestamp == other.timestamp &&
                    player == other.player &&
                    location == other.location &&
                    skillId == other.skillId
        }

        override fun hashCode(): Int {
            return Objects.hash(player, location, skillId, timestamp)
        }
    }

    /* ==================== 异常类 ==================== */

    /**
     * 不支持的版本异常
     */
    class UnsupportedVersionException : IllegalStateException("此功能仅支持 1.12.2 版本") {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    /**
     * 玩家取消操作异常
     */
    class PlayerCancelledException : RuntimeException("玩家取消操作") {
        companion object {
            private const val serialVersionUID = 1L
        }
    }
}
