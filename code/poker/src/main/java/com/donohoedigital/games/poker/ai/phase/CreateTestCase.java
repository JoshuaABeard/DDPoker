/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
/*
 * GameOver.java
 *
 * Created on April 17, 2003, 9:20 PM
 */

package com.donohoedigital.games.poker.ai.phase;

import com.donohoedigital.games.engine.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;

/**
 * Previously allowed creating AI test cases using the local
 * V2Player/RuleEngine. Now that AI decisions are handled by the embedded server
 * engine, the local rule engine is no longer available. This dialog is retained
 * as a placeholder since it is referenced in gamedef.xml.
 */
public class CreateTestCase extends DialogPhase {

    public JComponent createDialogContents() {
        DDPanel base = new DDPanel(GuiManager.DEFAULT, STYLE);
        base.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        base.setLayout(new BorderLayout(16, 16));
        base.setPreferredSize(new Dimension(200, 200));
        DDLabel label = new DDLabel(GuiManager.DEFAULT, STYLE);
        label.setText("AI test case creation is not available (server-driven AI).");
        base.add(label, BorderLayout.CENTER);
        return base;
    }
}
