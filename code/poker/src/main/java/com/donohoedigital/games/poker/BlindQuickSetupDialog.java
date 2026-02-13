/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Dialog for quick setup of blind levels using predefined templates.
 */
public class BlindQuickSetupDialog extends DialogPhase implements ActionListener {

    public static final String PARAM_PROFILE = "profile";

    private TournamentProfile profile_;
    private DDComboBox templateCombo_;
    private DDNumberSpinner numLevelsSpinner_;
    private DDCheckBox includeBreaksCheckbox_;
    private DDNumberSpinner breakFrequencySpinner_;
    private DDLabel previewLabel_;

    /**
     * Create dialog contents
     */
    public JComponent createDialogContents() {
        profile_ = (TournamentProfile) gamephase_.getObject(PARAM_PROFILE);
        ApplicationError.assertNotNull(profile_, "No 'profile' in params");

        DDPanel base = new DDPanel();
        base.setBorderLayoutGap(10, 10);
        base.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        DDLabel title = new DDLabel("msg.blind.quicksetup.title", STYLE);
        base.add(title, BorderLayout.NORTH);

        // Form panel
        DDPanel form = new DDPanel();
        form.setLayout(new GridLayout(0, 2, 10, 5));
        base.add(form, BorderLayout.CENTER);

        // Template selection
        form.add(new DDLabel("msg.blind.template", STYLE));
        templateCombo_ = new DDComboBox("template", STYLE);
        for (BlindTemplate template : BlindTemplate.values()) {
            templateCombo_.addItem(template);
        }
        templateCombo_.addActionListener(this);
        form.add(templateCombo_);

        // Number of levels
        form.add(new DDLabel("msg.blind.numlevels", STYLE));
        numLevelsSpinner_ = new DDNumberSpinner(15, 1, TournamentProfile.MAX_LEVELS, STYLE);
        numLevelsSpinner_.addChangeListener(e -> updatePreview());
        form.add(numLevelsSpinner_);

        // Include breaks
        form.add(new DDLabel("msg.blind.includebreaks", STYLE));
        includeBreaksCheckbox_ = new DDCheckBox("includebreaks", STYLE);
        includeBreaksCheckbox_.addActionListener(this);
        form.add(includeBreaksCheckbox_);

        // Break frequency
        form.add(new DDLabel("msg.blind.breakfreq", STYLE));
        breakFrequencySpinner_ = new DDNumberSpinner(3, 1, 10, STYLE);
        breakFrequencySpinner_.setEnabled(false);
        breakFrequencySpinner_.addChangeListener(e -> updatePreview());
        form.add(breakFrequencySpinner_);

        // Preview panel
        DDPanel previewPanel = new DDPanel();
        previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));
        previewPanel.setPreferredSize(new Dimension(400, 150));

        previewLabel_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        previewLabel_.setVerticalAlignment(SwingConstants.TOP);
        previewPanel.add(previewLabel_, BorderLayout.CENTER);

        base.add(previewPanel, BorderLayout.SOUTH);

        // Initial preview
        updatePreview();

        return base;
    }

    /**
     * Handle combo box and checkbox changes
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == includeBreaksCheckbox_) {
            breakFrequencySpinner_.setEnabled(includeBreaksCheckbox_.isSelected());
            updatePreview();
        } else if (e.getSource() == templateCombo_) {
            updatePreview();
        }
    }

    /**
     * Update the preview display
     */
    private void updatePreview() {
        BlindTemplate template = (BlindTemplate) templateCombo_.getSelectedItem();
        if (template == null)
            return;

        int numLevels = Math.min(5, numLevelsSpinner_.getValue()); // Show first 5 levels
        boolean includeBreaks = includeBreaksCheckbox_.isSelected();
        int breakFreq = breakFrequencySpinner_.getValue();

        // Create temporary profile for preview
        TournamentProfile temp = new TournamentProfile("Preview");
        template.generateLevels(temp, numLevels, includeBreaks, breakFreq);

        // Build preview text
        StringBuilder preview = new StringBuilder("<html>");
        preview.append("<b>First ").append(numLevels).append(" levels:</b><br>");

        int displayLevel = 1;
        for (int i = 1; i <= numLevels + (includeBreaks ? numLevels / breakFreq : 0)
                && displayLevel <= numLevels * 2; i++) {
            if (temp.isBreak(i)) {
                preview.append("Level ").append(i).append(": <i>Break (15 min)</i><br>");
            } else if (temp.getBigBlind(i) > 0) {
                int ante = temp.getAnte(i);
                int small = temp.getSmallBlind(i);
                int big = temp.getBigBlind(i);
                int mins = temp.getMinutes(i);

                preview.append("Level ").append(i).append(": ");
                if (ante > 0) {
                    preview.append(ante).append("/");
                }
                preview.append(small).append("/").append(big);
                preview.append(" (").append(mins).append(" min)<br>");

                if (++displayLevel > 5)
                    break;
            }
        }

        if (numLevels > 5) {
            preview.append("<i>... and ").append(numLevels - 5).append(" more levels</i>");
        }

        preview.append("</html>");
        previewLabel_.setText(preview.toString());
    }

    /**
     * Process OK button - apply the template
     */
    public boolean processButton(GameButton button) {
        if (button.getName().equals(okayButton_.getName())) {
            BlindTemplate template = (BlindTemplate) templateCombo_.getSelectedItem();
            int numLevels = numLevelsSpinner_.getValue();
            boolean includeBreaks = includeBreaksCheckbox_.isSelected();
            int breakFreq = breakFrequencySpinner_.getValue();

            // Apply template to actual profile
            template.generateLevels(profile_, numLevels, includeBreaks, breakFreq);

            // Signal success
            gamephase_.setBoolean("applied", true);
        }

        return super.processButton(button);
    }

    /**
     * Focus on template combo box
     */
    protected Component getFocusComponent() {
        return templateCombo_;
    }
}
