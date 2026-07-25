package com.baritonebot.task;

import com.baritonebot.integration.BaritoneHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.io.File;

/** Build a schematic at an origin using Baritone's builder process. */
public class BuildTask extends DelegatedTask {

    private final String schematicName;
    private final File file;
    private final BlockPos origin;

    public BuildTask(String schematicName, File file, BlockPos origin) {
        super(100);
        this.schematicName = schematicName;
        this.file = file;
        this.origin = origin;
    }

    @Override
    public String name() {
        return "build " + schematicName;
    }

    @Override
    protected void begin(Minecraft mc) {
        boolean started = BaritoneHelper.build(schematicName, file, origin);
        if (!started) {
            throw new IllegalStateException("Baritone rejected schematic \"" + schematicName
                + "\" (missing file, or you may need materials).");
        }
    }

    @Override
    protected boolean active() {
        return BaritoneHelper.isBuilding();
    }

    @Override
    protected String doneMessage() {
        return "Finished building " + schematicName + ".";
    }
}
