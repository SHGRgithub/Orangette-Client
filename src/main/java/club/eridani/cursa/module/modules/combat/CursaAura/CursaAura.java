package club.eridani.cursa.module.modules.combat.CursaAura;

import club.eridani.cursa.client.FontManager;
import club.eridani.cursa.client.FriendManager;
import club.eridani.cursa.client.GUIManager;
import club.eridani.cursa.common.annotations.Module;
import club.eridani.cursa.common.annotations.Parallel;
import club.eridani.cursa.concurrent.event.Listener;
import club.eridani.cursa.concurrent.event.Priority;
import club.eridani.cursa.concurrent.repeat.RepeatUnit;
import club.eridani.cursa.event.events.network.PacketEvent;
import club.eridani.cursa.event.events.render.RenderEvent;
import club.eridani.cursa.event.events.render.RenderModelEvent;
import club.eridani.cursa.mixin.mixins.accessor.AccessorCPacketPlayer;
import club.eridani.cursa.mixin.mixins.accessor.AccessorCPacketUseEntity;
import club.eridani.cursa.mixin.mixins.accessor.AccessorMinecraft;
import club.eridani.cursa.module.Category;
import club.eridani.cursa.module.ModuleBase;
import club.eridani.cursa.notification.NotificationManager;
import club.eridani.cursa.setting.Setting;
import club.eridani.cursa.utils.*;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.network.play.server.SPacketSpawnObject;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static club.eridani.cursa.concurrent.TaskManager.repeat;
import static club.eridani.cursa.concurrent.TaskManager.runRepeat;
import static club.eridani.cursa.utils.CrystalUtil.*;
import static org.lwjgl.opengl.GL11.GL_QUADS;

/**
 * Created by B_312 on 01/15/21
 * Updated by B_312 on 07/22/21
 * This Crystal is Under GNU Public License v3 instead of MIT
 * Commercial use is forbidden
 * Only personal use is allowed
 * If you want to use some parts of this AutoCrystal for your personal use,It's OK
 * But if you want to distribute your client or modified code
 * You have to public it and credit EridaniTeam
 */
@Parallel(runnable = true)
@Module(name = "CursaAura", category = Category.COMBAT)
public strictfp class CursaAura extends ModuleBase {

    public static CursaAura INSTANCE;

    Setting<String> page = setting("Page", "General", listOf("General", "Calculation", "Render"));

    //General
    Setting<Boolean> autoSwitch = setting("AutoSwitch", false).whenAtMode(page, "General");
    Setting<Integer> threadDelay = setting("ThreadDelay", 10, 1, 50).whenAtMode(page, "General");
    Setting<Boolean> targetPlayer = setting("Players", true).whenAtMode(page, "General");
    Setting<Boolean> targetMob = setting("Mobs", false).whenAtMode(page, "General");
    Setting<Boolean> targetAnimal = setting("Animals", false).whenAtMode(page, "General");
    Setting<Boolean> autoPlace = setting("Place", true).whenAtMode(page, "General");
    Setting<Boolean> autoExplode = setting("Explode", true).whenAtMode(page, "General");
    Setting<Boolean> packetExplode = setting("PacketExplode", true).whenAtMode(page, "General");
    Setting<Boolean> predictExplode = setting("PredictExplode", false).whenAtMode(page, "General");
    Setting<Integer> predictExplodeCount = setting("PredictHitCount", 2, 1, 20).whenAtMode(page, "General").whenTrue(predictExplode);
    Setting<Boolean> clearClickDelay = setting("ClearClickDelay", true).whenAtMode(page, "General");
    Setting<Boolean> multiPlace = setting("MultiPlace", false).whenAtMode(page, "General");
    Setting<Double> attackSpeed = setting("AttackSpeed", 35d, 0, 50).whenAtMode(page, "General");
    Setting<Double> placeSpeed = setting("PlaceSpeed", 35d, 0d, 50).whenAtMode(page, "General");
    Setting<Double> distance = setting("Distance", 7.0D, 0D, 8D).whenAtMode(page, "General");
    Setting<Double> placeRange = setting("PlaceRange", 4.5D, 0D, 8D).whenAtMode(page, "General");
    Setting<Double> hitRange = setting("HitRange", 5.5D, 0D, 8D).whenAtMode(page, "General");
    Setting<Boolean> rotate = setting("Rotate", true).whenAtMode(page, "General");
    Setting<Boolean> rayTrace = setting("RayTrace", false).whenAtMode(page, "General");
    //Calculation
    Setting<Boolean> oneThirtyPlace = setting("1.13Place", false).whenAtMode(page, "Calculation");
    Setting<Boolean> wall = setting("Wall", true).whenAtMode(page, "Calculation");
    Setting<Double> wallRange = setting("WallRange", 3.0, 0d, 5d).whenAtMode(page, "Calculation");
    Setting<Boolean> noSuicide = setting("NoSuicide", true).whenAtMode(page, "Calculation");
    Setting<Boolean> facePlace = setting("FacePlace", true).whenAtMode(page, "Calculation");
    Setting<Double> blastHealth = setting("MinHealthFace", 10d, 0d, 20d).whenAtMode(page, "Calculation").whenTrue(facePlace);
    Setting<Double> placeMinDmg = setting("PlaceMinDamage", 4.5d, 0d, 20d).whenAtMode(page, "Calculation");
    Setting<Double> placeMaxSelf = setting("PlaceMaxSelf", 10d, 0d, 36d).whenAtMode(page, "Calculation");
    Setting<String> attackMode = setting("HitMode", "Smart", listOf("Smart", "Always")).whenAtMode(page, "Calculation");
    Setting<Double> breakMinDmg = setting("BreakMinDmg", 4.5, 0.0, 36.0).whenAtMode(page, "Calculation").whenAtMode(attackMode, "Smart");
    Setting<Double> breakMaxSelf = setting("BreakMaxSelf", 12.0, 0.0, 36.0).whenAtMode(page, "Calculation").whenAtMode(attackMode, "Smart");
    Setting<Boolean> lethalTryIt = setting("LethalTryIt", true).whenAtMode(page, "Calculation").whenAtMode(attackMode, "Smart");
    Setting<Boolean> ghostHand = setting("GhostHand", false).whenAtMode(page, "Calculation");
    Setting<Boolean> pauseWhileEating = setting("PauseWhileEating", false).whenAtMode(page, "Calculation");
    Setting<Boolean> clientSide = setting("ClientSideConfirm", false).whenAtMode(page, "Calculation");
    //Render
    Setting<Boolean> renderDmg = setting("RenderDamage", false).whenAtMode(page, "Render");
    Setting<String> renderMode = setting("RenderBlock", "Full", listOf("Solid", "Up", "UpLine", "Full", "Outline", "OFF")).whenAtMode(page, "Render");
    Setting<Boolean> syncGUI = setting("SyncGui", false).whenAtMode(page, "Render");
    Setting<Integer> red = setting("Red", 255, 0, 255).whenAtMode(page, "Render").whenFalse(syncGUI);
    Setting<Integer> green = setting("Green", 0, 0, 255).whenAtMode(page, "Render").whenFalse(syncGUI);
    Setting<Integer> blue = setting("Blue", 0, 0, 255).whenAtMode(page, "Render").whenFalse(syncGUI);
    Setting<Integer> transparency = setting("Alpha", 26, 0, 255).whenAtMode(page, "Render");
    Setting<Boolean> rainbow = setting("Rainbow", false).whenAtMode(page, "Render").whenFalse(syncGUI);
    Setting<Float> rainbowSpeed = setting("RGB Speed", 1.0f, 0.0f, 10.0f).whenAtMode(page, "Render").whenFalse(syncGUI);
    Setting<Float> saturation = setting("Saturation", 0.65f, 0.0f, 1.0f).whenAtMode(page, "Render").whenFalse(syncGUI);
    Setting<Float> brightness = setting("Brightness", 1.0f, 0.0f, 1.0f).whenAtMode(page, "Render").whenFalse(syncGUI);

    transient public static float yaw;
    transient public static float pitch;
    transient public static float renderPitch;
    transient public static boolean shouldSpoofPacket;
    transient public static Entity target;
    transient private int placements = 0;
    transient private int oldSlot = -1;
    transient private int prevSlot;
    transient private boolean switchCoolDown;
    transient private boolean togglePitch = false;
    transient private BlockPos renderBlock;
    transient private static boolean rotating = false;
    transient Timer placeTimer = new Timer();
    transient Timer breakTimer = new Timer();
    transient Timer packetBreakTimer = new Timer();
    transient AtomicBoolean shouldIgnoreEntity = new AtomicBoolean(false);

    transient AtomicInteger lastEntityId = new AtomicInteger(-1);

    public final LocalTarget localTarget = new LocalTarget();
    private final HashMap<BlockPos, Double> renderBlockDmg = new HashMap<>();

    RepeatUnit placeCalculation = new RepeatUnit(() -> (int) ((1000 / placeSpeed.getValue()) - 5) / (multiPlace.getValue() ? 1 : 2), () -> {
        if (mc.player == null || mc.world == null) return;
        if (multiPlace.getValue()) localTarget.putPlaceTarget(calculator(shouldIgnoreEntity.get()));
    }).timeOut(it -> NotificationManager.error("Can't keep up!The calculation can't catch up with your place speed!Decrease PlaceSpeed plz!"));

    RepeatUnit breakCalculation = new RepeatUnit(() -> (int) ((1000 / attackSpeed.getValue()) - 5), () -> {
        if (mc.player == null || mc.world == null) return;
        localTarget.putAttackTarget(new ArrayList<>(mc.world.loadedEntityList)
                .stream()
                .filter(e -> e instanceof EntityEnderCrystal && canHitCrystal(e.getPositionVector()))
                .map(e -> (EntityEnderCrystal) e)
                .min(Comparator.comparing(e -> mc.player.getDistance(e))).orElse(null));
    }).timeOut(it -> NotificationManager.error("Can't keep up!The calculation can't catch up with your attack speed!Decrease BreakSpeed plz!"));

    List<RepeatUnit> repeatUnits = new ArrayList<>();

    public CursaAura() {
        INSTANCE = this;
        repeatUnits.add(placeCalculation);
        repeatUnits.add(breakCalculation);
        repeatUnits.add(updateAutoCrystal);
        repeatUnits.forEach(it -> {
            it.suspend();
            runRepeat(it);
        });
    }

    @Override
    public void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) return;
        if (clearClickDelay.getValue() && mc.player.getHeldItemMainhand().getItem() == Items.END_CRYSTAL || mc.player.getHeldItemOffhand().getItem() == Items.END_CRYSTAL) {
            ((AccessorMinecraft) mc).setRightClickDelayTimer(0);
        }
        if (event.packet instanceof SPacketSoundEffect) {
            SPacketSoundEffect packet = (SPacketSoundEffect) event.packet;
            if (packet.getCategory() == SoundCategory.BLOCKS && packet.getSound() == SoundEvents.ENTITY_GENERIC_EXPLODE) {
                new ArrayList<>(mc.world.loadedEntityList).stream()
                        .filter(it -> it instanceof EntityEnderCrystal)
                        .filter(it -> it.getDistance(packet.getX(), packet.getY(), packet.getZ()) <= 6.0f)
                        .forEach(Entity::setDead);
            }
        }
        if (event.packet instanceof SPacketSpawnObject) {
            if (((SPacketSpawnObject) (event.packet)).getType() == 51) {
                lastEntityId.getAndUpdate(it -> Math.max(it, ((SPacketSpawnObject) (event.packet)).getEntityID()));
                if (packetExplode.getValue()) packetBreak(mc.player, (SPacketSpawnObject) event.packet);
            } else {
                lastEntityId.set(-1);
            }
        }
    }

    private void packetBreak(EntityPlayerSP player, SPacketSpawnObject packet) {
        if ((1000 / attackSpeed.getValue()) > 0 && !packetBreakTimer.passed((1000 / attackSpeed.getValue()))) return;
        double distance = player.getDistance(packet.getX(), packet.getY(), packet.getZ());
        if (distance > hitRange.getValue()) return;
        Vec3d pos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
        if (wall.getValue() && distance > wallRange.getValue() && !canSeeBlock(new BlockPos(pos)))
            return;
        if (!canHitCrystal(pos)) return;
        CPacketUseEntity attackPacket = new CPacketUseEntity();
        setEntityId(attackPacket, packet.getEntityID());
        setAction(attackPacket, CPacketUseEntity.Action.ATTACK);
        mc.player.connection.sendPacket(attackPacket);
        if (rotate.getValue()) lookAt(pos);
        packetBreakTimer.reset();
    }

    public static void setAction(CPacketUseEntity packet, CPacketUseEntity.Action action) {
        ((AccessorCPacketUseEntity) packet).setAction(action);
    }

    public static void setEntityId(CPacketUseEntity packet, int entityId) {
        ((AccessorCPacketUseEntity) packet).setId(entityId);
    }

    @Listener(priority = Priority.PARALLEL)
    public void renderModelRotation(RenderModelEvent event) {
        if (!rotate.getValue()) return;
        if (rotating) {
            event.rotating = true;
            event.pitch = renderPitch;
        }
    }

    @Override
    public void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null) return;
        if (!rotate.getValue()) {
            return;
        }
        if (event.packet instanceof CPacketPlayer) {
            CPacketPlayer packet = (CPacketPlayer) event.packet;
            if (shouldSpoofPacket) {
                ((AccessorCPacketPlayer) packet).setYaw(yaw);
                ((AccessorCPacketPlayer) packet).setPitch(pitch);
                shouldSpoofPacket = false;
            }
        }
    }


    /**
     * Calculation of explosion
     * If return true,then we can explode it.
     */
    private boolean canHitCrystal(Vec3d crystal) {
        if (mc.player.getDistance(crystal.x, crystal.y, crystal.z) > hitRange.getValue()) return false;
        if (attackMode.getValue().equals("Smart")) {
            float selfDamage = calculateDamage(crystal.x, crystal.y, crystal.z, mc.player, mc.player.getPositionVector());
            if (selfDamage >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) return false;
            List<EntityPlayer> entities = new ArrayList<>(mc.world.playerEntities);
            entities = entities.stream()
                    .filter(e -> mc.player.getDistance(e) <= distance.getValue())
                    .filter(e -> mc.player != e)
                    .filter(e -> !FriendManager.isFriend(e))
                    .sorted(Comparator.comparing(e -> mc.player.getDistance(e)))
                    .collect(Collectors.toList());
            for (EntityPlayer player : entities) {
                if (player.isDead || (player.getHealth() + player.getAbsorptionAmount()) <= 0.0f) continue;
                double minDamage = breakMinDmg.getValue();
                double maxSelf = breakMaxSelf.getValue();
                if (canFacePlace(player, blastHealth.getValue())) {
                    minDamage = 1;
                }
                double target = calculateDamage(crystal.x, crystal.y, crystal.z, player, player.getPositionVector());
                if (target > player.getHealth() + player.getAbsorptionAmount()
                        && selfDamage < mc.player.getHealth() + mc.player.getAbsorptionAmount()
                        && lethalTryIt.getValue()
                ) return true;
                if (selfDamage > maxSelf) continue;
                if (target < minDamage) continue;
                if (selfDamage > target) continue;
                return true;
            }
        } else return true;
        return false;
    }

    private void resetRotation() {
        if (shouldSpoofPacket) {
            yaw = mc.player.rotationYaw;
            pitch = mc.player.rotationPitch;
            shouldSpoofPacket = false;
            rotating = false;
        }
    }

    private void explodeCrystal(EntityEnderCrystal crystal) {
        if (rotate.getValue()) lookAtCrystal(crystal);
        if (breakDelayRun(attackSpeed.getValue())) {
            if (crystal != null) {
                mc.playerController.attackEntity(mc.player, crystal);
                mc.player.swingArm(mc.player.getHeldItemOffhand().getItem() == Items.END_CRYSTAL ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);
                if (clientSide.getValue()) {
                    if (mc.player.canEntityBeSeen(crystal)) crystal.setDead();
                }
                mc.player.resetCooldown();
            }
            breakTimer.reset();
        }
    }

    private void placeCrystal(BlockPos targetBlock, EnumHand enumHand) {
        EnumFacing facing = rayTrace.getValue() ? enumFacing(targetBlock) : EnumFacing.UP;
        if (rotate.getValue()) lookAtPos(targetBlock, facing);
        if (placeDelayRun(placeSpeed.getValue())) {
            repeat(3, () -> {
                mc.player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(targetBlock, facing, enumHand, 0, 0, 0));
                placements++;
            });
            if (predictExplode.getValue()) {
                int syncedId = lastEntityId.get();
                AtomicInteger count = new AtomicInteger(0);
                repeat(predictExplodeCount.getValue(), () -> {
                    if (syncedId != -1) {
                        CPacketUseEntity attackPacket = new CPacketUseEntity();
                        setEntityId(attackPacket, syncedId + count.getAndIncrement() + 1);
                        setAction(attackPacket, CPacketUseEntity.Action.ATTACK);
                        mc.player.connection.sendPacket(attackPacket);
                    }
                });
            }
            placeTimer.reset();
        }
    }

    private boolean placeDelayRun(double speed) {
        return placeTimer.passed(1000 / speed);
    }

    private boolean breakDelayRun(double speed) {
        return breakTimer.passed(1000 / speed);
    }

    private List<BlockPos> findCrystalBlocks(double range) {
        NonNullList<BlockPos> newList = NonNullList.create();
        newList.addAll(BlockInteractionHelper.getSphere(getPlayerPos(), (float) range, (int) range, false, true, 0)
                .stream()
                .filter(it -> canPlaceCrystal(it, oneThirtyPlace.getValue())).collect(Collectors.toList()));
        return newList;
    }

    private void drawBlock(BlockPos blockPos, int color) {
        CursaTessellator.prepare(GL_QUADS);
        if (!renderMode.getValue().equals("OFF")) {
            if (renderMode.getValue().equals("Solid") || renderMode.getValue().equals("Up")) {
                if (renderMode.getValue().equals("Up")) {
                    CursaTessellator.drawBox(blockPos, color, GeometryMasks.Quad.UP);
                } else {
                    CursaTessellator.drawBox(blockPos, color, GeometryMasks.Quad.ALL);
                }
            } else {
                if (renderMode.getValue().equals("Full")) {
                    CursaTessellator.drawFullBox(blockPos, 1f, color);
                } else if (renderMode.getValue().equals("Outline")) {
                    CursaTessellator.drawBoundingBox(blockPos, 2f, color);
                } else {
                    CursaTessellator.drawBoundingBox(blockPos.add(0, 1, 0), 2f, color);
                }
            }
        }
        CursaTessellator.release();
        if (renderDmg.getValue()) {
            if (renderBlockDmg.containsKey(blockPos)) {
                GlStateManager.pushMatrix();
                glBillboardDistanceScaled((float) blockPos.getX() + 0.5f, (float) blockPos.getY() + 0.5f, (float) blockPos.getZ() + 0.5f, mc.player);
                final double damage = renderBlockDmg.get(blockPos);
                final String damageText = "DMG: " + (Math.floor(damage) == damage ? (int) damage : String.format("%.1f", damage));
                GlStateManager.disableDepth();
                GlStateManager.translate(-(FontManager.fontRenderer.getStringWidth(damageText) / 2.0d), 0, 0);
                FontManager.fontRenderer.drawStringWithShadow(damageText, 0, 0, -1);
                GlStateManager.popMatrix();
            }
        }
    }

    public boolean canFacePlace(EntityLivingBase target, double blast) {
        float healthTarget = target.getHealth() + target.getAbsorptionAmount();
        return healthTarget <= blast;
    }

    public boolean canPlaceCrystal(BlockPos blockPos, boolean newPlace) {
        BlockPos boost = blockPos.add(0, 1, 0);
        BlockPos boost2 = blockPos.add(0, 2, 0);

        Block base = mc.world.getBlockState(blockPos).getBlock();
        Block b1 = mc.world.getBlockState(boost).getBlock();
        Block b2 = mc.world.getBlockState(boost2).getBlock();

        if (base != Blocks.BEDROCK && base != Blocks.OBSIDIAN) return false;

        if (b1 != Blocks.AIR && !isReplaceable(b1)) return false;
        if (!newPlace && b2 != Blocks.AIR) return false;

        AxisAlignedBB box = new AxisAlignedBB(
                blockPos.getX(), blockPos.getY() + 1.0, blockPos.getZ(),
                blockPos.getX() + 1.0, blockPos.getY() + 3.0, blockPos.getZ() + 1.0
        );

        List<Entity> entities = new ArrayList<>(mc.world.loadedEntityList);

        for (Entity entity : entities) {
            if (shouldIgnoreEntity.get() && !multiPlace.getValue() && entity instanceof EntityEnderCrystal) continue;
            if (entity.getEntityBoundingBox().intersects(box)) return false;
        }
        return true;
    }

    public void lookAtPos(BlockPos block, EnumFacing face) {
        float[] v = RotationUtil.getRotationsBlock(block, face, false);
        float[] v2 = RotationUtil.getRotationsBlock(block.add(0, +0.5, 0), face, false);
        setYawAndPitch(v[0], v[1], v2[1]);
    }

    public void lookAt(Vec3d vec3d) {
        float[] v = RotationUtil.getRotations(mc.player.getPositionEyes(mc.getRenderPartialTicks()), vec3d);
        float[] v2 = RotationUtil.getRotations(mc.player.getPositionEyes(mc.getRenderPartialTicks()), vec3d.add(0, -0.5, 0));
        setYawAndPitch(v[0], v[1], v2[1]);
    }

    public void lookAtCrystal(EntityEnderCrystal ent) {
        float[] v = RotationUtil.getRotations(mc.player.getPositionEyes(mc.getRenderPartialTicks()), ent.getPositionVector());
        float[] v2 = RotationUtil.getRotations(mc.player.getPositionEyes(mc.getRenderPartialTicks()), ent.getPositionVector().add(0, -0.5, 0));
        setYawAndPitch(v[0], v[1], v2[1]);
    }

    public void setYawAndPitch(float yaw1, float pitch1, float renderPitch1) {
        yaw = yaw1;
        pitch = pitch1;
        renderPitch = renderPitch1;
        mc.player.rotationYawHead = yaw;
        mc.player.renderYawOffset = yaw;
        shouldSpoofPacket = true;
        rotating = true;
    }

    RepeatUnit updateAutoCrystal = new RepeatUnit(() -> threadDelay.getValue(), () -> {
        if (mc.player == null || mc.world == null) return;

        if (placeTimer.passed(1050) || breakTimer.passed(1050)) rotating = false;
        if (pauseWhileEating.getValue() && isEating()) return;

        localTarget.checkAll((int) (1000 / placeSpeed.getValue()), (int) (1000 / attackSpeed.getValue()));

        EntityEnderCrystal attackCrystalTarget = localTarget.getAttackTarget();

        if (autoExplode.getValue() && attackCrystalTarget != null) {
            if (!mc.player.canEntityBeSeen(attackCrystalTarget) && mc.player.getDistance(attackCrystalTarget) > wallRange.getValue() && wall.getValue()) {
                shouldIgnoreEntity.set(false);
            } else {
                explodeCrystal(attackCrystalTarget);
                if (!multiPlace.getValue()) shouldIgnoreEntity.set(true);
                if (placements >= 3) {
                    placements = 0;
                    shouldIgnoreEntity.set(true);
                }
            }
        } else {
            resetRotation();
            if (oldSlot != -1) {
                mc.player.inventory.currentItem = oldSlot;
                oldSlot = -1;
            }
        }

        int crystalSlot = mc.player.getHeldItemMainhand().getItem() == Items.END_CRYSTAL ? mc.player.inventory.currentItem : -1;
        if (crystalSlot == -1) {
            for (int l = 0; l < 9; ++l) {
                if (mc.player.inventory.getStackInSlot(l).getItem() == Items.END_CRYSTAL) {
                    crystalSlot = l;
                    break;
                }
            }
        }

        boolean offhand = false;
        if (mc.player.getHeldItemOffhand().getItem() == Items.END_CRYSTAL) offhand = true;
        else if (crystalSlot == -1) return;
        BlockPos targetBlock = null;
        CrystalTarget placeTarget = multiPlace.getValue() ? localTarget.getPlaceTarget(shouldIgnoreEntity.get()) : calculator(shouldIgnoreEntity.get());
        if (placeTarget != null) {
            target = placeTarget.target;
            targetBlock = placeTarget.blockPos;
        }
        if (target == null || targetBlock == null) {
            renderBlock = null;
            resetRotation();
            return;
        }
        renderBlock = targetBlock;
        if (autoPlace.getValue()) {
            EnumHand enumHand = offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
            if (!offhand && mc.player.inventory.currentItem != crystalSlot) {
                if (autoSwitch.getValue()) {
                    mc.player.inventory.currentItem = crystalSlot;
                    resetRotation();
                    switchCoolDown = true;
                }
                return;
            }
            if (switchCoolDown) {
                switchCoolDown = false;
                return;
            }
            if (ghostHand.getValue()) ghostHand();
            placeCrystal(targetBlock, enumHand);
        }
        if (rotate.getValue() && shouldSpoofPacket) {
            if (togglePitch) {
                mc.player.rotationPitch += 0.0004;
                togglePitch = false;
            } else {
                mc.player.rotationPitch -= 0.0004;
                togglePitch = true;
            }
        }
        if (prevSlot != -1 && ghostHand.getValue()) {
            mc.player.inventory.currentItem = prevSlot;
            mc.playerController.updateController();
        }
    }).timeOut(it -> NotificationManager.error("Can't keep up!The calculation can't catch up with your update speed!Increase Thread Delay plz!"));

    /**
     * Calculation of target block to place crystal
     * return a target contains coordinate and target entity.
     */
    public CrystalTarget calculator(boolean ignoredEntity) {
        double damage = 0.5;
        BlockPos tempBlock = null;
        Entity target = null;
        List<BlockPos> crystalBlocks = findCrystalBlocks(distance.getValue());
        List<Entity> entities = getEntities();
        prevSlot = -1;
        for (Entity entity2 : entities) {
            if (entity2 != mc.player) {
                if (((EntityLivingBase) entity2).getHealth() <= 0.0f) continue;
                Vec3d targetVec = new Vec3d(entity2.posX, entity2.posY, entity2.posZ);
                for (BlockPos blockPos : crystalBlocks) {
                    if (entity2.getDistanceSq(blockPos) >= distance.getValue() * distance.getValue()) continue;
                    if (mc.player.getDistance(blockPos.getX(), blockPos.getY(), blockPos.getZ()) > placeRange.getValue())
                        continue;
                    double targetDamage = calculateDamage(blockPos.getX() + 0.5, blockPos.getY() + 1, blockPos.getZ() + 0.5, entity2, targetVec);
                    if (targetDamage < damage) continue;
                    if (targetDamage < (facePlace.getValue() ? (canFacePlace((EntityLivingBase) entity2, blastHealth.getValue()) ? 1 : placeMinDmg.getValue()) : placeMinDmg.getValue()))
                        continue;
                    float healthTarget = ((EntityLivingBase) entity2).getHealth() + ((EntityLivingBase) entity2).getAbsorptionAmount();
                    float healthSelf = mc.player.getHealth() + mc.player.getAbsorptionAmount();
                    double selfDamage = calculateDamage(blockPos.getX() + 0.5, blockPos.getY() + 1, blockPos.getZ() + 0.5, mc.player);
                    if (selfDamage > targetDamage && targetDamage < healthTarget) continue;
                    if (selfDamage - 0.5 > healthSelf && noSuicide.getValue()) continue;
                    if (selfDamage > placeMaxSelf.getValue()) continue;
                    if (!wall.getValue()
                            && wallRange.getValue() > 0
                            && !canSeeBlock(blockPos)
                            && getVecDistance(blockPos, mc.player.posX, mc.player.posY, mc.player.posZ) >= wallRange.getValue()
                    ) continue;
                    damage = targetDamage;
                    tempBlock = blockPos;
                    target = entity2;
                    if (renderDmg.getValue()) renderBlockDmg.put(tempBlock, targetDamage);
                }
                if (target != null) break;
            }
        }
        return new CrystalTarget(tempBlock, target, ignoredEntity);
    }

    private void ghostHand() {
        if (mc.player.getHeldItemMainhand().getItem() != Items.END_CRYSTAL && mc.player.getHeldItemOffhand().getItem() != Items.END_CRYSTAL) {
            for (int i = 0; i < 9; ++i) {
                ItemStack stack = mc.player.inventory.getStackInSlot(i);
                if (stack == ItemStack.EMPTY) continue;
                if (stack.getItem() == Items.END_CRYSTAL) {
                    prevSlot = mc.player.inventory.currentItem;
                    mc.player.inventory.currentItem = i;
                    mc.playerController.updateController();
                }
            }
        }
    }

    private List<Entity> getEntities() {
        List<Entity> entities = new ArrayList<>(mc.world.loadedEntityList);
        entities = entities.stream()
                .filter(entity -> EntityUtil.isLiving(entity) && (EntityUtil.isPassive(entity) ? targetAnimal.getValue() : targetMob.getValue()))
                .filter(entity -> mc.player.getDistance(entity) < distance.getValue())
                .collect(Collectors.toList());
        if (targetPlayer.getValue()) entities.addAll(mc.world.playerEntities);
        entities.removeIf(entity -> entity == mc.player || FriendManager.isFriend(entity));
        entities.sort(Comparator.comparingDouble(entity -> entity.getDistance(mc.player)));
        return entities;
    }

    public int getColor() {
        float[] tick_color = {(System.currentTimeMillis() % 11520L) / 11520.0f * rainbowSpeed.getValue()};
        int color_rgb = Color.HSBtoRGB(tick_color[0], saturation.getValue(), brightness.getValue());
        return rainbow.getValue() ?
                new Color(color_rgb >> 16 & 0xFF, color_rgb >> 8 & 0xFF, color_rgb & 0xFF, transparency.getValue()).getRGB() :
                new Color(red.getValue(), green.getValue(), blue.getValue(), transparency.getValue()).getRGB();
    }


    @Override
    public void onRenderWorld(RenderEvent event) {
        int color = syncGUI.getValue() ?
                new Color(GUIManager.getRed(), GUIManager.getGreen(), GUIManager.getBlue(), transparency.getValue()).getRGB() :
                getColor();
        if (renderBlock != null) drawBlock(renderBlock, color);
    }

    @Override
    public void onEnable() {
        lastEntityId.set(-1);
        shouldIgnoreEntity.set(false);
        repeatUnits.forEach(RepeatUnit::resume);
        renderBlockDmg.clear();
    }

    @Override
    public void onDisable() {
        repeatUnits.forEach(RepeatUnit::suspend);
        rotating = false;
        resetRotation();
        placeTimer.restart();
        breakTimer.restart();
        packetBreakTimer.restart();
        renderBlock = null;
        target = null;
        yaw = mc.player.rotationYaw;
        pitch = mc.player.rotationPitch;
    }

}
