package com.github.ykrasik.gamedex.core.ui.statusbar;

import javafx.scene.control.Skin;
import org.controlsfx.control.StatusBar;

/**
 * @author Yevgeny Krasik
 */
public class WorkingStatusBar extends StatusBar {
    @Override
    protected Skin<?> createDefaultSkin() {
        return new WorkingStatusBarSkin(this);
    }
}
