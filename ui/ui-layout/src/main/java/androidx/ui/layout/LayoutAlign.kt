/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("Deprecation")

package androidx.ui.layout

import androidx.ui.core.Alignment
import androidx.ui.core.Constraints
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.unit.Density
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.ipx
import androidx.ui.unit.max

/**
 * Provides scope-dependent alignment options for children layouts where the alignment is handled
 * by the parent layout rather than the child itself. Different layout models allow different
 * [LayoutGravity] options. For example, [Row] provides Top and Bottom, while [Column] provides
 * Start and End.
 * Unlike [LayoutAlign], layout children with [LayoutGravity] are aligned only after the size
 * of the parent is known, therefore not affecting the size of the parent in order to achieve
 * their own alignment.
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.SimpleGravityInRow
 *
 * @sample androidx.ui.layout.samples.SimpleGravityInColumn
 */
object LayoutGravity

/**
 * Provides alignment options for a target layout where the alignment is handled by the modifier
 * itself (rather than by the layout's parent). The modifier will occupy as little space as possible
 * to satisfy the minimum incoming constraints in the alignment direction(s), while also being
 * at least as large as the wrapped layout. Consequently, in order for the alignment to work, the
 * incoming min constraints have to be larger than the size of the wrapped layout; in this case,
 * the modifier will align the target layout within itself, and the modifier will occupy the size
 * of the min constraints in the alignment direction(s). Note that, in order to make sure that
 * the alignment happens due to the min incoming constraints, size modifiers such as [LayoutSize],
 * [LayoutWidth] or [LayoutHeight] can be specified before [LayoutAlign] - otherwise, the min
 * icoming constraints can also be enforced by the parent layout model.
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.SimpleAlignedModifier
 * @sample androidx.ui.layout.samples.SimpleVerticallyAlignedModifier
 */
object LayoutAlign {
    /**
     * A layout modifier that positions the target component inside its parent to the top in
     * vertical direction and wraps the component in horizontal direction.
     */
    @Deprecated(
        "Use Modifier.wrapContentHeight(Alignment.TopCenter)",
        replaceWith = ReplaceWith(
            "Modifier.wrapContentHeight(Alignment.TopCenter)",
            "androidx.ui.core.Modifier",
            "androidx.ui.core.Alignment",
            "androidx.ui.layout.wrapContentHeight"
        )
    )
    val Top: LayoutModifier =
        AlignmentModifier(alignment = Alignment.TopStart, direction = Direction.Vertical)

    /**
     * A layout modifier that positions the target component in the center of the parent in
     * vertical direction and wraps the component in horizontal direction.
     */
    @Deprecated(
        "Use Modifier.wrapContentHeight(Alignment.Center)",
        replaceWith = ReplaceWith(
            "Modifier.wrapContentHeight(Alignment.Center)",
            "androidx.ui.core.Modifier",
            "androidx.ui.core.Alignment",
            "androidx.ui.layout.wrapContentHeight"
        )
    )
    val CenterVertically: LayoutModifier =
        AlignmentModifier(alignment = Alignment.CenterStart, direction = Direction.Vertical)

    /**
     * A layout modifier that positions the target component inside its parent to the bottom in
     * vertical direction and wraps the component in horizontal direction.
     */
    @Deprecated(
        "Use Modifier.wrapContentHeight(Alignment.BottomCenter)",
        replaceWith = ReplaceWith(
            "Modifier.wrapContentHeight(Alignment.BottomCenter)",
            "androidx.ui.core.Modifier",
            "androidx.ui.core.Alignment",
            "androidx.ui.layout.wrapContentHeight"
        )
    )
    val Bottom: LayoutModifier =
        AlignmentModifier(alignment = Alignment.BottomStart, direction = Direction.Vertical)

    /**
     * A layout modifier that positions the target component inside its parent to the start edge
     * in horizontal direction and wraps the component in vertical direction.
     */
    @Deprecated(
        "Use Modifier.wrapContentWidth(Alignment.CenterStart)",
        replaceWith = ReplaceWith(
            "Modifier.wrapContentWidth(Alignment.CenterStart)",
            "androidx.ui.core.Modifier",
            "androidx.ui.core.Alignment",
            "androidx.ui.layout.wrapContentWidth"
        )
    )
    val Start: LayoutModifier =
        AlignmentModifier(alignment = Alignment.TopStart, direction = Direction.Horizontal)

    /**
     * A layout modifier that positions the target component in the center of the parent in
     * horizontal direction and wraps the component in vertical direction.
     */
    @Deprecated(
        "Use Modifier.wrapContentWidth(Alignment.Center)",
        replaceWith = ReplaceWith(
            "Modifier.wrapContentWidth(Alignment.Center)",
            "androidx.ui.core.Modifier",
            "androidx.ui.core.Alignment",
            "androidx.ui.layout.wrapContentWidth"
        )
    )
    val CenterHorizontally: LayoutModifier =
        AlignmentModifier(alignment = Alignment.TopCenter, direction = Direction.Horizontal)

    /**
     * A layout modifier that positions the target component inside its parent to the end edge
     * in horizontal direction and wraps the component in vertical direction.
     */
    @Deprecated(
        "Use Modifier.wrapContentWidth(Alignment.CenterEnd)",
        replaceWith = ReplaceWith(
            "Modifier.wrapContentWidth(Alignment.CenterEnd)",
            "androidx.ui.core.Modifier",
            "androidx.ui.core.Alignment",
            "androidx.ui.layout.wrapContentWidth"
        )
    )
    val End: LayoutModifier =
        AlignmentModifier(alignment = Alignment.TopEnd, direction = Direction.Horizontal)

    /**
     * A layout modifier that positions the target component top-left inside its parent.
     */
    @Deprecated(
        "Use Modifier.wrapContentSize(Alignment.TopStart)",
        replaceWith = ReplaceWith(
            "Modifier.wrapContentSize(Alignment.TopStart)",
            "androidx.ui.core.Modifier",
            "androidx.ui.core.Alignment",
            "androidx.ui.layout.wrapContentSize"
        )
    )
    val TopStart: LayoutModifier =
        AlignmentModifier(alignment = Alignment.TopStart, direction = Direction.Both)

    /**
     * A layout modifier that positions the target component top-center inside its parent.
     */
    @Deprecated(
        "Use Modifier.wrapContentSize(Alignment.TopCenter)",
        replaceWith = ReplaceWith(
            "Modifier.wrapContentSize(Alignment.TopCenter)",
            "androidx.ui.core.Modifier",
            "androidx.ui.core.Alignment",
            "androidx.ui.layout.wrapContentSize"
        )
    )
    val TopCenter: LayoutModifier =
        AlignmentModifier(alignment = Alignment.TopCenter, direction = Direction.Both)

    /**
     * A layout modifier that positions the target component top-right inside its parent.
     */
    @Deprecated(
        "Use Modifier.wrapContentSize(Alignment.TopEnd)",
        replaceWith = ReplaceWith(
            "Modifier.wrapContentSize(Alignment.TopEnd)",
            "androidx.ui.core.Modifier",
            "androidx.ui.core.Alignment",
            "androidx.ui.layout.wrapContentSize"
        )
    )
    val TopEnd: LayoutModifier =
        AlignmentModifier(alignment = Alignment.TopEnd, direction = Direction.Both)

    /**
     * A layout modifier that positions the target component center-left inside its parent.
     */
    @Deprecated(
        "Use Modifier.wrapContentSize(Alignment.CenterStart)",
        replaceWith = ReplaceWith(
            "Modifier.wrapContentSize(Alignment.CenterStart)",
            "androidx.ui.core.Modifier",
            "androidx.ui.core.Alignment",
            "androidx.ui.layout.wrapContentSize"
        )
    )
    val CenterStart: LayoutModifier =
        AlignmentModifier(alignment = Alignment.CenterStart, direction = Direction.Both)

    /**
     * A layout modifier that positions the target component in the center of its parent.
     */
    @Deprecated(
        "Use Modifier.wrapContentSize(Alignment.Center)",
        replaceWith = ReplaceWith(
            "Modifier.wrapContentSize(Alignment.Center)",
            "androidx.ui.core.Modifier",
            "androidx.ui.core.Alignment",
            "androidx.ui.layout.wrapContentSize"
        )
    )
    val Center: LayoutModifier =
        AlignmentModifier(alignment = Alignment.Center, direction = Direction.Both)

    /**
     * A layout modifier that positions the target component center-right inside its parent.
     */
    @Deprecated(
        "Use Modifier.wrapContentSize(Alignment.CenterEnd)",
        replaceWith = ReplaceWith(
            "Modifier.wrapContentSize(Alignment.CenterEnd)",
            "androidx.ui.core.Modifier",
            "androidx.ui.core.Alignment",
            "androidx.ui.layout.wrapContentSize"
        )
    )
    val CenterEnd: LayoutModifier =
        AlignmentModifier(alignment = Alignment.CenterEnd, direction = Direction.Both)

    /**
     * A layout modifier that positions the target component bottom-left inside its parent.
     */
    @Deprecated(
        "Use Modifier.wrapContentSize(Alignment.BottomStart)",
        replaceWith = ReplaceWith(
            "Modifier.wrapContentSize(Alignment.BottomStart)",
            "androidx.ui.core.Modifier",
            "androidx.ui.core.Alignment",
            "androidx.ui.layout.wrapContentSize"
        )
    )
    val BottomStart: LayoutModifier =
        AlignmentModifier(alignment = Alignment.BottomStart, direction = Direction.Both)

    /**
     * A layout modifier that positions the target component bottom-center inside its parent.
     */
    @Deprecated(
        "Use Modifier.wrapContentSize(Alignment.BottomCenter)",
        replaceWith = ReplaceWith(
            "Modifier.wrapContentSize(Alignment.BottomCenter)",
            "androidx.ui.core.Modifier",
            "androidx.ui.core.Alignment",
            "androidx.ui.layout.wrapContentSize"
        )
    )
    val BottomCenter: LayoutModifier =
        AlignmentModifier(alignment = Alignment.BottomCenter, direction = Direction.Both)

    /**
     * A layout modifier that positions the target component bottom-right inside its parent.
     */
    @Deprecated(
        "Use Modifier.wrapContentSize(Alignment.BottomEnd)",
        replaceWith = ReplaceWith(
            "Modifier.wrapContentSize(Alignment.BottomEnd)",
            "androidx.ui.core.Modifier",
            "androidx.ui.core.Alignment",
            "androidx.ui.layout.wrapContentSize"
        )
    )
    val BottomEnd: LayoutModifier =
        AlignmentModifier(alignment = Alignment.BottomEnd, direction = Direction.Both)
}

private enum class Direction {
    Vertical, Horizontal, Both
}

private data class AlignmentModifier(
    private val alignment: Alignment,
    private val direction: Direction
) : LayoutModifier {
    override fun Density.modifyConstraints(
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ) = when (direction) {
        Direction.Both -> constraints.copy(minWidth = 0.ipx, minHeight = 0.ipx)
        Direction.Horizontal -> constraints.copy(minWidth = 0.ipx)
        Direction.Vertical -> constraints.copy(minHeight = 0.ipx)
    }

    override fun Density.modifySize(
        constraints: Constraints,
        layoutDirection: LayoutDirection,
        childSize: IntPxSize
    ): IntPxSize = IntPxSize(
        max(constraints.minWidth, childSize.width),
        max(constraints.minHeight, childSize.height)
    )

    override fun Density.modifyPosition(
        childSize: IntPxSize,
        containerSize: IntPxSize,
        layoutDirection: LayoutDirection
    ): IntPxPosition = alignment.align(
        IntPxSize(
            containerSize.width - childSize.width,
            containerSize.height - childSize.height
        ),
        layoutDirection
    )
}