package club.eridani.cursa.module.modules.movement;

import club.eridani.cursa.common.annotations.Module;
import club.eridani.cursa.common.annotations.Parallel;
import club.eridani.cursa.module.Category;
import club.eridani.cursa.module.ModuleBase;

@Parallel
@Module(name="EzFly", category = Category.MOVEMENT)

public class EzFly extends ModuleBase {
    //fly on coed
    @Override
    public void onTick() {

        mc.player.capabilities.isFlying = true ;
    }


    //fly off
    @Override
    public void onDisable() {

        mc.player.capabilities.isFlying = false;
    }
}
