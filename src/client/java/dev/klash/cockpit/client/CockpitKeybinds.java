package dev.klash.cockpit.client;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;

public class CockpitKeybinds {

//    public static KeyBinding BARREL_ROLL;

    public static KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("cockpit", "general"));

    public static void init() {
//        BARREL_ROLL = KeyBindingHelper.registerKeyBinding(new KeyBinding(
//                "key.infinilytra.barrel_roll",
//                InputUtil.Type.KEYSYM,
//                GLFW.GLFW_KEY_B,
//                CATEGORY
//        ));
    }
}
