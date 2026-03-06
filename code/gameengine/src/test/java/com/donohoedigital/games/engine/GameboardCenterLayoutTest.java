/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 * Copyright (c) 2026 the DD Poker community
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
package com.donohoedigital.games.engine;

import com.donohoedigital.gui.ScaleConstraintsFixed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for GameboardCenterLayout - A LayoutManager2 that centers components.
 */
@ExtendWith(MockitoExtension.class)
class GameboardCenterLayoutTest {

    private GameboardCenterLayout layout;

    @BeforeEach
    void setUp() {
        layout = new GameboardCenterLayout();
    }

    // ========== addLayoutComponent(String, Component) Tests ==========

    @Test
    void should_ThrowUnsupportedOperationException_When_AddLayoutComponentWithStringCalled() {
        Component component = new Canvas();

        assertThatThrownBy(() -> layout.addLayoutComponent("name", component))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ========== addLayoutComponent(Component, Object) Tests ==========

    @Test
    void should_StoreConstraints_When_ScaleConstraintsFixedProvided() {
        Component component = new Canvas();
        ScaleConstraintsFixed constraints = mock(ScaleConstraintsFixed.class);

        assertThatCode(() -> layout.addLayoutComponent(component, constraints)).doesNotThrowAnyException();
    }

    @Test
    void should_IgnoreConstraints_When_NonScaleConstraintsProvided() {
        Component component = new Canvas();

        assertThatCode(() -> layout.addLayoutComponent(component, "not-a-constraint")).doesNotThrowAnyException();
    }

    @Test
    void should_IgnoreConstraints_When_NullConstraintsProvided() {
        Component component = new Canvas();

        assertThatCode(() -> layout.addLayoutComponent(component, null)).doesNotThrowAnyException();
    }

    // ========== removeLayoutComponent Tests ==========

    @Test
    void should_NotThrow_When_RemoveLayoutComponentCalled() {
        Component component = new Canvas();

        assertThatCode(() -> layout.removeLayoutComponent(component)).doesNotThrowAnyException();
    }

    @Test
    void should_NotThrow_When_RemovePreviouslyAddedComponent() {
        Component component = new Canvas();
        ScaleConstraintsFixed constraints = mock(ScaleConstraintsFixed.class);

        layout.addLayoutComponent(component, constraints);
        assertThatCode(() -> layout.removeLayoutComponent(component)).doesNotThrowAnyException();
    }

    // ========== maximumLayoutSize Tests ==========

    @Test
    void should_ReturnMaxDimension_When_MaximumLayoutSizeCalled() {
        Container target = new Panel();

        Dimension max = layout.maximumLayoutSize(target);

        assertThat(max.width).isEqualTo(Integer.MAX_VALUE);
        assertThat(max.height).isEqualTo(Integer.MAX_VALUE);
    }

    // ========== getLayoutAlignmentX Tests ==========

    @Test
    void should_ReturnCenterAlignment_When_GetLayoutAlignmentXCalled() {
        Container target = new Panel();

        assertThat(layout.getLayoutAlignmentX(target)).isEqualTo(0.5F);
    }

    // ========== getLayoutAlignmentY Tests ==========

    @Test
    void should_ReturnCenterAlignment_When_GetLayoutAlignmentYCalled() {
        Container target = new Panel();

        assertThat(layout.getLayoutAlignmentY(target)).isEqualTo(0.5F);
    }

    // ========== invalidateLayout Tests ==========

    @Test
    void should_NotThrow_When_InvalidateLayoutCalled() {
        Container container = new Panel();

        assertThatCode(() -> layout.invalidateLayout(container)).doesNotThrowAnyException();
    }

    // ========== preferredLayoutSize Tests ==========

    @Test
    void should_ReturnChildPreferredSizePlusInsets_When_PreferredLayoutSizeCalled() {
        Panel container = new Panel(layout);
        Canvas child = new Canvas() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(200, 100);
            }
        };
        container.add(child);

        Dimension result = layout.preferredLayoutSize(container);

        // Panel has zero insets by default
        assertThat(result.width).isEqualTo(200);
        assertThat(result.height).isEqualTo(100);
    }

    // ========== minimumLayoutSize Tests ==========

    @Test
    void should_ReturnSameAsPreferred_When_MinimumLayoutSizeCalled() {
        Panel container = new Panel(layout);
        Canvas child = new Canvas() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(150, 75);
            }
        };
        container.add(child);

        Dimension preferred = layout.preferredLayoutSize(container);
        Dimension minimum = layout.minimumLayoutSize(container);

        assertThat(minimum).isEqualTo(preferred);
    }

    // ========== layoutContainer Tests ==========

    @Test
    void should_CenterVisibleComponent_When_LayoutContainerCalled() {
        Panel container = new Panel(layout);
        container.setSize(400, 300);
        Canvas child = new Canvas() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, 50);
            }
        };
        container.add(child);

        layout.layoutContainer(container);

        // Component 100x50 centered in 400x300 => (150, 125)
        assertThat(child.getX()).isEqualTo(150);
        assertThat(child.getY()).isEqualTo(125);
        assertThat(child.getWidth()).isEqualTo(100);
        assertThat(child.getHeight()).isEqualTo(50);
    }

    @Test
    void should_SkipInvisibleComponent_When_LayoutContainerCalled() {
        Panel container = new Panel(layout);
        container.setSize(400, 300);
        Canvas child = new Canvas() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, 50);
            }
        };
        child.setVisible(false);
        container.add(child);

        layout.layoutContainer(container);

        // Invisible component should not have been positioned
        assertThat(child.getX()).isEqualTo(0);
        assertThat(child.getY()).isEqualTo(0);
    }

    @Test
    void should_HandleEmptyContainer_When_LayoutContainerCalled() {
        Panel container = new Panel(layout);
        container.setSize(400, 300);

        assertThatCode(() -> layout.layoutContainer(container)).doesNotThrowAnyException();
    }

    @Test
    void should_NotMoveBounds_When_BoundsAlreadyCorrect() {
        Panel container = new Panel(layout);
        container.setSize(400, 300);
        Canvas child = new Canvas() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, 50);
            }
        };
        container.add(child);

        // Layout once to get correct position
        layout.layoutContainer(container);
        assertThat(child.getX()).isEqualTo(150);
        assertThat(child.getY()).isEqualTo(125);

        // Layout again - should remain the same
        layout.layoutContainer(container);
        assertThat(child.getX()).isEqualTo(150);
        assertThat(child.getY()).isEqualTo(125);
    }

    @Test
    void should_LayoutMultipleVisibleComponents_When_ContainerHasMultiple() {
        Panel container = new Panel(layout);
        container.setSize(400, 300);

        Canvas child1 = new Canvas() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, 50);
            }
        };
        Canvas child2 = new Canvas() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(200, 100);
            }
        };
        container.add(child1);
        container.add(child2);

        layout.layoutContainer(container);

        // child1: 100x50 centered in 400x300
        assertThat(child1.getX()).isEqualTo(150);
        assertThat(child1.getY()).isEqualTo(125);
        assertThat(child1.getWidth()).isEqualTo(100);
        assertThat(child1.getHeight()).isEqualTo(50);

        // child2: 200x100 centered in 400x300
        assertThat(child2.getX()).isEqualTo(100);
        assertThat(child2.getY()).isEqualTo(100);
        assertThat(child2.getWidth()).isEqualTo(200);
        assertThat(child2.getHeight()).isEqualTo(100);
    }

    @Test
    void should_AdjustCentering_When_ContainerSizeChanges() {
        Panel container = new Panel(layout);
        Canvas child = new Canvas() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, 100);
            }
        };
        container.add(child);

        // Layout with one size
        container.setSize(400, 400);
        layout.layoutContainer(container);
        assertThat(child.getX()).isEqualTo(150);
        assertThat(child.getY()).isEqualTo(150);

        // Layout with a different size
        container.setSize(200, 200);
        layout.layoutContainer(container);
        assertThat(child.getX()).isEqualTo(50);
        assertThat(child.getY()).isEqualTo(50);
    }
}
