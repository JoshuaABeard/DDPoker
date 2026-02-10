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
package com.donohoedigital.games.poker.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for AIStrategyNode tree structure and hierarchy management.
 */
class AIStrategyNodeTest
{
    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    void should_CreateNode_When_TwoParameterConstructor()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "strategy1");

        assertThat(node).isNotNull();
    }

    @Test
    void should_CreateNode_When_ThreeParameterConstructor()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "strategy1", true);

        assertThat(node).isNotNull();
    }

    @Test
    void should_SetEnabledFalse_When_TwoParameterConstructor()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "strategy1");

        assertThat(node.isEnabled()).isFalse();
    }

    @Test
    void should_SetEnabledTrue_When_ThreeParameterConstructorWithTrue()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "strategy1", true);

        assertThat(node.isEnabled()).isTrue();
    }

    @Test
    void should_SetEnabledFalse_When_ThreeParameterConstructorWithFalse()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "strategy1", false);

        assertThat(node.isEnabled()).isFalse();
    }

    // ========================================
    // Enabled/Expanded State Tests
    // ========================================

    @Test
    void should_ReturnEnabled_When_CreatedWithEnabled()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "strategy1", true);

        assertThat(node.isEnabled()).isTrue();
    }

    @Test
    void should_ReturnNotEnabled_When_CreatedWithoutEnabled()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "strategy1", false);

        assertThat(node.isEnabled()).isFalse();
    }

    @Test
    void should_ReturnNotExpanded_When_NewlyCreated()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "strategy1");

        assertThat(node.isExpanded()).isFalse();
    }

    @Test
    void should_SetExpanded_When_SetExpandedCalled()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "strategy1");

        node.setExpanded(true);

        assertThat(node.isExpanded()).isTrue();
    }

    @Test
    void should_SetNotExpanded_When_SetExpandedCalledWithFalse()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "strategy1");

        node.setExpanded(true);
        node.setExpanded(false);

        assertThat(node.isExpanded()).isFalse();
    }

    @Test
    void should_ToggleExpanded_When_SetExpandedCalledMultipleTimes()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "strategy1");

        node.setExpanded(true);
        assertThat(node.isExpanded()).isTrue();

        node.setExpanded(false);
        assertThat(node.isExpanded()).isFalse();

        node.setExpanded(true);
        assertThat(node.isExpanded()).isTrue();
    }

    // ========================================
    // Indent/Hierarchy Tests
    // ========================================

    @Test
    void should_HaveZeroIndent_When_RootNode()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode root = new AIStrategyNode(playerType, "root");

        assertThat(root.getIndent()).isZero();
    }

    @Test
    void should_IncrementIndent_When_ChildAdded()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode parent = new AIStrategyNode(playerType, "parent");
        AIStrategyNode child = new AIStrategyNode(playerType, "child");

        parent.addChild(child);

        assertThat(child.getIndent()).isEqualTo(1);
    }

    @Test
    void should_IncrementIndentByTwo_When_GrandchildAdded()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode parent = new AIStrategyNode(playerType, "parent");
        AIStrategyNode child = new AIStrategyNode(playerType, "child");
        AIStrategyNode grandchild = new AIStrategyNode(playerType, "grandchild");

        parent.addChild(child);
        child.addChild(grandchild);

        assertThat(grandchild.getIndent()).isEqualTo(2);
    }

    @Test
    void should_IncrementIndentByThree_When_GreatGrandchildAdded()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode root = new AIStrategyNode(playerType, "root");
        AIStrategyNode level1 = new AIStrategyNode(playerType, "level1");
        AIStrategyNode level2 = new AIStrategyNode(playerType, "level2");
        AIStrategyNode level3 = new AIStrategyNode(playerType, "level3");

        root.addChild(level1);
        level1.addChild(level2);
        level2.addChild(level3);

        assertThat(root.getIndent()).isEqualTo(0);
        assertThat(level1.getIndent()).isEqualTo(1);
        assertThat(level2.getIndent()).isEqualTo(2);
        assertThat(level3.getIndent()).isEqualTo(3);
    }

    // ========================================
    // Parent-Child Relationship Tests
    // ========================================

    @Test
    void should_AddChild_When_AddChildCalled()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode parent = new AIStrategyNode(playerType, "parent");
        AIStrategyNode child = new AIStrategyNode(playerType, "child");

        parent.addChild(child);

        // Child should have parent's indent + 1
        assertThat(child.getIndent()).isEqualTo(parent.getIndent() + 1);
    }

    @Test
    void should_AddMultipleChildren_When_AddChildCalledMultipleTimes()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode parent = new AIStrategyNode(playerType, "parent");
        AIStrategyNode child1 = new AIStrategyNode(playerType, "child1");
        AIStrategyNode child2 = new AIStrategyNode(playerType, "child2");
        AIStrategyNode child3 = new AIStrategyNode(playerType, "child3");

        parent.addChild(child1);
        parent.addChild(child2);
        parent.addChild(child3);

        assertThat(child1.getIndent()).isEqualTo(1);
        assertThat(child2.getIndent()).isEqualTo(1);
        assertThat(child3.getIndent()).isEqualTo(1);
    }

    @Test
    void should_BuildTree_When_MultipleNodesAdded()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode root = new AIStrategyNode(playerType, "root");

        AIStrategyNode branch1 = new AIStrategyNode(playerType, "branch1");
        AIStrategyNode branch2 = new AIStrategyNode(playerType, "branch2");

        AIStrategyNode leaf1 = new AIStrategyNode(playerType, "leaf1");
        AIStrategyNode leaf2 = new AIStrategyNode(playerType, "leaf2");

        root.addChild(branch1);
        root.addChild(branch2);
        branch1.addChild(leaf1);
        branch2.addChild(leaf2);

        assertThat(root.getIndent()).isEqualTo(0);
        assertThat(branch1.getIndent()).isEqualTo(1);
        assertThat(branch2.getIndent()).isEqualTo(1);
        assertThat(leaf1.getIndent()).isEqualTo(2);
        assertThat(leaf2.getIndent()).isEqualTo(2);
    }

    // ========================================
    // Tree Depth Tests
    // ========================================

    @Test
    void should_HandleDeepTree_When_ManyLevels()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode current = new AIStrategyNode(playerType, "level0");

        for (int i = 1; i <= 10; i++)
        {
            AIStrategyNode child = new AIStrategyNode(playerType, "level" + i);
            current.addChild(child);
            assertThat(child.getIndent()).isEqualTo(i);
            current = child;
        }
    }

    @Test
    void should_HandleWideTree_When_ManySiblings()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode root = new AIStrategyNode(playerType, "root");

        for (int i = 0; i < 10; i++)
        {
            AIStrategyNode child = new AIStrategyNode(playerType, "child" + i);
            root.addChild(child);
            assertThat(child.getIndent()).isEqualTo(1);
        }
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Test
    void should_PreserveEnabledState_When_AddedAsChild()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode parent = new AIStrategyNode(playerType, "parent", true);
        AIStrategyNode childEnabled = new AIStrategyNode(playerType, "childEnabled", true);
        AIStrategyNode childDisabled = new AIStrategyNode(playerType, "childDisabled", false);

        parent.addChild(childEnabled);
        parent.addChild(childDisabled);

        assertThat(childEnabled.isEnabled()).isTrue();
        assertThat(childDisabled.isEnabled()).isFalse();
    }

    @Test
    void should_MaintainIndentIndependently_When_MultipleTrees()
    {
        PlayerType playerType = new PlayerType("test");

        // First tree
        AIStrategyNode root1 = new AIStrategyNode(playerType, "root1");
        AIStrategyNode child1 = new AIStrategyNode(playerType, "child1");
        root1.addChild(child1);

        // Second tree
        AIStrategyNode root2 = new AIStrategyNode(playerType, "root2");
        AIStrategyNode child2 = new AIStrategyNode(playerType, "child2");
        root2.addChild(child2);

        assertThat(root1.getIndent()).isZero();
        assertThat(child1.getIndent()).isEqualTo(1);
        assertThat(root2.getIndent()).isZero();
        assertThat(child2.getIndent()).isEqualTo(1);
    }

    @Test
    void should_AllowSamePlayerTypeForAllNodes_When_Building()
    {
        PlayerType playerType = new PlayerType("test");

        AIStrategyNode node1 = new AIStrategyNode(playerType, "node1");
        AIStrategyNode node2 = new AIStrategyNode(playerType, "node2");
        AIStrategyNode node3 = new AIStrategyNode(playerType, "node3");

        node1.addChild(node2);
        node2.addChild(node3);

        // All nodes can use same PlayerType instance
        assertThat(node1.getIndent()).isEqualTo(0);
        assertThat(node2.getIndent()).isEqualTo(1);
        assertThat(node3.getIndent()).isEqualTo(2);
    }

    // ========================================
    // Child Access Tests
    // ========================================

    @Test
    void should_ReturnZeroChildCount_When_NoChildren()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "node");

        assertThat(node.getChildCount()).isZero();
    }

    @Test
    void should_ReturnChildCount_When_ChildrenAdded()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode parent = new AIStrategyNode(playerType, "parent");
        parent.addChild(new AIStrategyNode(playerType, "child1"));
        parent.addChild(new AIStrategyNode(playerType, "child2"));
        parent.addChild(new AIStrategyNode(playerType, "child3"));

        assertThat(parent.getChildCount()).isEqualTo(3);
    }

    @Test
    void should_ReturnChild_When_GetChildCalled()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode parent = new AIStrategyNode(playerType, "parent");
        AIStrategyNode child1 = new AIStrategyNode(playerType, "child1");
        AIStrategyNode child2 = new AIStrategyNode(playerType, "child2");

        parent.addChild(child1);
        parent.addChild(child2);

        assertThat(parent.getChild(0)).isSameAs(child1);
        assertThat(parent.getChild(1)).isSameAs(child2);
    }

    @Test
    void should_ReturnChildrenList_When_GetChildrenCalled()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode parent = new AIStrategyNode(playerType, "parent");
        parent.addChild(new AIStrategyNode(playerType, "child1"));
        parent.addChild(new AIStrategyNode(playerType, "child2"));

        assertThat(parent.getChildren()).hasSize(2);
    }

    @Test
    void should_ReturnParent_When_GetParentCalled()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode parent = new AIStrategyNode(playerType, "parent");
        AIStrategyNode child = new AIStrategyNode(playerType, "child");

        parent.addChild(child);

        assertThat(child.getParent()).isSameAs(parent);
    }

    @Test
    void should_ReturnNullParent_When_RootNode()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode root = new AIStrategyNode(playerType, "root");

        assertThat(root.getParent()).isNull();
    }

    // ========================================
    // Label and Help Text Tests
    // ========================================

    @Test
    void should_ReturnStrategyName_When_NoPropertyMessageDefined()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "undefined_strategy");

        // When property not found, implementation returns the strategy name as fallback
        assertThat(node.getLabel()).isEqualTo("undefined_strategy");
    }

    @Test
    void should_ReturnEmptyString_When_NoHelpMessageDefined()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "undefined_strategy");

        String helpText = node.getHelpText();
        // When help message not found, implementation returns empty string
        assertThat(helpText).isEmpty();
    }

    // ========================================
    // Value Get/Set Tests
    // ========================================

    @Test
    void should_GetValue_When_ValueSet()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "testvalue");

        node.setValue(50);

        assertThat(node.getValue()).isEqualTo(50);
    }

    @Test
    void should_UpdateValue_When_SetValueCalled()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "testvalue");

        node.setValue(25);
        assertThat(node.getValue()).isEqualTo(25);

        node.setValue(75);
        assertThat(node.getValue()).isEqualTo(75);
    }

    // ========================================
    // Propagation Tests
    // ========================================

    @Test
    void should_PropagateValueUp_When_ChildValueChanged()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode parent = new AIStrategyNode(playerType, "parent");
        AIStrategyNode child1 = new AIStrategyNode(playerType, "child1");
        AIStrategyNode child2 = new AIStrategyNode(playerType, "child2");

        parent.addChild(child1);
        parent.addChild(child2);

        child1.setValue(40);
        child2.setValue(60);

        child1.propagateUpwards();

        // Parent should have average of children (40 + 60) / 2 = 50
        assertThat(parent.getValue()).isEqualTo(50);
    }

    @Test
    void should_PropagateValueDown_When_ParentValueChanged()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode parent = new AIStrategyNode(playerType, "parent");
        AIStrategyNode child1 = new AIStrategyNode(playerType, "child1");
        AIStrategyNode child2 = new AIStrategyNode(playerType, "child2");

        parent.addChild(child1);
        parent.addChild(child2);

        parent.setValue(75);
        parent.propagateDownwards();

        assertThat(child1.getValue()).isEqualTo(75);
        assertThat(child2.getValue()).isEqualTo(75);
    }

    @Test
    void should_PropagateRecursively_When_MultiLevelTree()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode root = new AIStrategyNode(playerType, "root");
        AIStrategyNode level1 = new AIStrategyNode(playerType, "level1");
        AIStrategyNode level2 = new AIStrategyNode(playerType, "level2");

        root.addChild(level1);
        level1.addChild(level2);

        root.setValue(100);
        root.propagateDownwards();

        assertThat(level1.getValue()).isEqualTo(100);
        assertThat(level2.getValue()).isEqualTo(100);
    }

    @Test
    void should_PropagateValueChange_When_Called()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode parent = new AIStrategyNode(playerType, "parent");
        AIStrategyNode child = new AIStrategyNode(playerType, "child");
        AIStrategyNode grandchild = new AIStrategyNode(playerType, "grandchild");

        parent.addChild(child);
        child.addChild(grandchild);

        child.setValue(80);
        child.propagateValueChange();

        // Should propagate both up and down
        assertThat(grandchild.getValue()).isEqualTo(80);
        assertThat(parent.getValue()).isEqualTo(80);
    }

    // ========================================
    // setMissingValues Tests
    // ========================================

    @Test
    void should_SetDefaultValue_When_ValueMissing()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "newstrat");

        node.setMissingValues(playerType, 50);

        assertThat(node.getValue()).isEqualTo(50);
    }

    @Test
    void should_PreserveExistingValue_When_ValueAlreadySet()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "existingstrat");

        node.setValue(30);
        node.setMissingValues(playerType, 50);

        assertThat(node.getValue()).isEqualTo(30);
    }

    @Test
    void should_PropagateDefaultToChildren_When_SetMissingValues()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode parent = new AIStrategyNode(playerType, "parent");
        AIStrategyNode child = new AIStrategyNode(playerType, "child");

        parent.addChild(child);
        parent.setMissingValues(playerType, 60);

        assertThat(child.getValue()).isEqualTo(60);
    }

    // ========================================
    // smartExpand Tests
    // ========================================

    @Test
    void should_ReturnFalse_When_NoChildren()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode node = new AIStrategyNode(playerType, "leaf");

        assertThat(node.smartExpand()).isFalse();
    }

    @Test
    void should_ReturnFalse_When_AllChildrenHaveSameValue()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode parent = new AIStrategyNode(playerType, "parent");
        AIStrategyNode child1 = new AIStrategyNode(playerType, "child1");
        AIStrategyNode child2 = new AIStrategyNode(playerType, "child2");

        parent.addChild(child1);
        parent.addChild(child2);

        parent.setValue(50);
        child1.setValue(50);
        child2.setValue(50);

        assertThat(parent.smartExpand()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_ChildHasDifferentValue()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode parent = new AIStrategyNode(playerType, "parent");
        AIStrategyNode child1 = new AIStrategyNode(playerType, "child1");
        AIStrategyNode child2 = new AIStrategyNode(playerType, "child2");

        parent.addChild(child1);
        parent.addChild(child2);

        parent.setValue(50);
        child1.setValue(50);
        child2.setValue(60);

        assertThat(parent.smartExpand()).isTrue();
    }

    @Test
    void should_SetExpanded_When_SmartExpandReturnsTrue()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode parent = new AIStrategyNode(playerType, "parent");
        AIStrategyNode child = new AIStrategyNode(playerType, "child");

        parent.addChild(child);

        parent.setValue(50);
        child.setValue(60);

        parent.smartExpand();

        assertThat(parent.isExpanded()).isTrue();
    }

    @Test
    void should_PropagateExpansion_When_NestedDifference()
    {
        PlayerType playerType = new PlayerType("test");
        AIStrategyNode root = new AIStrategyNode(playerType, "root");
        AIStrategyNode level1 = new AIStrategyNode(playerType, "level1");
        AIStrategyNode level2 = new AIStrategyNode(playerType, "level2");

        root.addChild(level1);
        level1.addChild(level2);

        root.setValue(50);
        level1.setValue(50);
        level2.setValue(60);

        root.smartExpand();

        assertThat(root.isExpanded()).isTrue();
        assertThat(level1.isExpanded()).isTrue();
    }
}
