/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.navigation.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.BundleMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.toPackage
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.TestNavigator
import androidx.testutils.test
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

@MediumTest
@RunWith(AndroidJUnit4::class)
class NavControllerTest {

    companion object {
        private const val UNKNOWN_DESTINATION_ID = -1
        private const val TEST_ARG = "test"
        private const val TEST_ARG_VALUE = "value"
        private const val TEST_ARG_VALUE_INT = 123
        private const val TEST_OVERRIDDEN_VALUE_ARG = "test_overriden_value"
        private const val TEST_ACTION_OVERRIDDEN_VALUE_ARG = "test_action_overriden_value"
        private const val TEST_OVERRIDDEN_VALUE_ARG_VALUE = "override"
    }

    @Test
    fun testGetCurrentBackStackEntry() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_start_destination)
        assertEquals(R.id.start_test, navController.currentBackStackEntry?.destination?.id ?: 0)
    }

    @Test
    fun testGetCurrentBackStackEntryEmptyBackStack() {
        val navController = createNavController()
        assertThat(navController.currentBackStackEntry).isNull()
    }

    @Test
    fun testGetPreviousBackStackEntry() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)
        assertEquals(R.id.start_test, navController.previousBackStackEntry?.destination?.id ?: 0)
    }

    @Test
    fun testGetPreviousBackStackEntryEmptyBackStack() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        assertThat(navController.previousBackStackEntry).isNull()
    }

    @Test
    fun testStartDestination() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_start_destination)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
    }

    @Test
    fun testSetGraphTwice() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_start_destination)
        val navigator = navController.navigatorProvider[TestNavigator::class]
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size)
            .isEqualTo(1)

        // Now set a new graph, overriding the first
        navController.setGraph(R.navigation.nav_nested_start_destination)
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.nested_test)
        assertThat(navigator.backStack.size)
            .isEqualTo(1)
    }

    @Test
    fun testStartDestinationWithArgs() {
        val navController = createNavController()
        val args = Bundle().apply {
            putString(TEST_ARG, TEST_ARG_VALUE)
        }
        navController.setGraph(R.navigation.nav_start_destination, args)
        val navigator = navController.navigatorProvider[TestNavigator::class]
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)
        val foundArgs = navigator.current.second
        assertNotNull(foundArgs)
        assertEquals(TEST_ARG_VALUE, foundArgs?.getString(TEST_ARG))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testStartDestinationWithWrongArgs() {
        val navController = createNavController()
        val args = Bundle().apply {
            putInt(TEST_ARG, TEST_ARG_VALUE_INT)
        }
        navController.setGraph(R.navigation.nav_start_destination, args)
    }

    @Test
    fun testStartDestinationWithArgsProgrammatic() {
        val navController = createNavController()
        val args = Bundle().apply {
            putString(TEST_ARG, TEST_ARG_VALUE)
        }

        val navGraph = navController.navigatorProvider.navigation(
                startDestination = R.id.start_test
        ) {
            test(R.id.start_test)
        }
        navController.setGraph(navGraph, args)
        val navigator = navController.navigatorProvider[TestNavigator::class]
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)
        val foundArgs = navigator.current.second
        assertNotNull(foundArgs)
        assertEquals(TEST_ARG_VALUE, foundArgs?.getString(TEST_ARG))
    }

    @Test(expected = IllegalStateException::class)
    fun testMissingStartDestination() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_missing_start_destination)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidStartDestination() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_invalid_start_destination)
    }

    @Test
    fun testNestedStartDestination() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_nested_start_destination)
        assertEquals(R.id.nested_test, navController.currentDestination?.id ?: 0)
    }

    @Test
    fun testSetGraph() {
        val navController = createNavController()

        navController.setGraph(R.navigation.nav_start_destination)
        assertNotNull(navController.graph)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
    }

    @Test
    fun testGetGraphIllegalStateException() {
        val navController = createNavController()
        try {
            navController.graph
            fail("getGraph() should throw an IllegalStateException before setGraph()")
        } catch (expected: IllegalStateException) {
        }
    }

    @Test
    fun testNavigate() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)

        navController.navigate(R.id.second_test)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)
    }

    @Test
    fun testInvalidNavigateViaDeepLink() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val deepLinkRequest = NavDeepLinkRequest.Builder.fromUri(
            Uri.parse("android-app://androidx.navigation.test/invalid")).build()

        try {
            navController.navigate(deepLinkRequest)
            fail("navController.navigate must throw")
        } catch (e: IllegalArgumentException) {
            assertThat(e)
                .hasMessageThat().contains(
                    "Navigation destination that matches request $deepLinkRequest cannot be " +
                            "found in the navigation graph ${navController.graph}"
                )
        }
    }

    @Test
    fun testNavigateViaDeepLink() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = Uri.parse("android-app://androidx.navigation.test/test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @Test
    fun testNavigateViaDeepLinkAction() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = NavDeepLinkRequest(null, "test.action", null)

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @Test
    fun testNavigateViaDeepLinkActionDifferentURI() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = NavDeepLinkRequest(Uri.parse("invalidDeepLink.com"), "test.action", null)

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @Test
    fun testNavigateViaDeepLinkMimeTypeDifferentUri() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = NavDeepLinkRequest(Uri.parse("invalidDeepLink.com"), null, "type/test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @Test
    fun testNavigateViaDeepLinkMimeType() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_deeplink)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = NavDeepLinkRequest(null, null, "type/test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.forth_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @Test
    fun testNavigateViaDeepLinkMimeTypeWildCard() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_deeplink)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = NavDeepLinkRequest(null, null, "any/thing")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.first_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @Test
    fun testNavigateViaDeepLinkMimeTypeWildCardSubtype() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_deeplink)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = NavDeepLinkRequest(null, null, "image/jpg")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @Test
    fun testNavigateViaDeepLinkMimeTypeWildCardType() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_deeplink)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = NavDeepLinkRequest(null, null, "doesNotEvenMatter/test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.third_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @Test
    fun testNavigationViaDeepLinkPopUpTo() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = Uri.parse("android-app://androidx.navigation.test/test")

        navController.navigate(deepLink, navOptions {
            popUpTo(R.id.nav_root) { inclusive = true }
        })
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @Test
    fun testNavigateToDifferentGraphViaDeepLink() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_multiple_navigation)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val deepLink = Uri.parse("android-app://androidx.navigation.test/test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.deep_link_child_second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val popped = navController.popBackStack()
        assertWithMessage("NavController should return true when popping a non-root destination")
            .that(popped)
            .isTrue()
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @Test
    fun testNavigateToDifferentGraphViaDeepLink3x() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_multiple_navigation)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val deepLink = Uri.parse("android-app://androidx.navigation.test/test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.deep_link_child_second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.popBackStack()
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        // repeat nav and pop 2 more times.
        navController.navigate(deepLink)
        navController.popBackStack()
        navController.navigate(deepLink)

        val popped = navController.popBackStack()
        assertWithMessage("NavController should return true when popping a non-root destination")
            .that(popped)
            .isTrue()
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @Test
    fun testNavigateToDifferentGraphViaDeepLinkToGrandchild3x() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_multiple_navigation)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val deepLink = Uri.parse("android-app://androidx.navigation.test/grand_child_test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.deep_link_grandchild_start_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.popBackStack()
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        // repeat nav and pop 2 more times.
        navController.navigate(deepLink)
        navController.popBackStack()
        navController.navigate(deepLink)

        val popped = navController.popBackStack()
        assertWithMessage("NavController should return true when popping a non-root destination")
            .that(popped)
            .isTrue()
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @LargeTest
    @Test
    @SdkSuppress(minSdkVersion = 17)
    fun testNavigateViaImplicitDeepLink() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("android-app://androidx.navigation.test/test/argument1/argument2"),
            ApplicationProvider.getApplicationContext() as Context,
            TestActivity::class.java
        )

        Intents.init()

        with(ActivityScenario.launch<TestActivity>(intent)) {
            moveToState(Lifecycle.State.CREATED)
            onActivity {
                activity ->
                run {
                    val navController = activity.navController
                    navController.setGraph(R.navigation.nav_simple)

                    val navigator =
                        navController.navigatorProvider.getNavigator(TestNavigator::class.java)

                    assertThat(
                        navController.currentDestination!!.id
                    ).isEqualTo(R.id.second_test)

                    // Only the leaf destination should be on the stack.
                    assertThat(navigator.backStack.size).isEqualTo(1)
                    // The parent will be constructed in a new Activity after navigateUp()
                    navController.navigateUp()
                }
            }

            assertThat(this.state).isEqualTo(Lifecycle.State.DESTROYED)
        }

        // this relies on MonitoringInstrumentation.execStartActivity() which was added in API 17
        intended(
            allOf(
                toPackage((ApplicationProvider.getApplicationContext() as Context).packageName),
                not(hasData(anyString())), // The rethrow should not use the URI as primary target.
                hasExtra(NavController.KEY_DEEP_LINK_IDS, intArrayOf(R.id.nav_root)),
                hasExtra(
                    Matchers.`is`(NavController.KEY_DEEP_LINK_EXTRAS),
                    allOf(
                        BundleMatchers.hasEntry("arg1", "argument1"),
                        BundleMatchers.hasEntry("arg2", "argument2"),
                        BundleMatchers.hasEntry(
                            NavController.KEY_DEEP_LINK_INTENT,
                            allOf(
                                hasAction(intent.action),
                                hasData(intent.data),
                                hasComponent(intent.component)
                            )
                        )
                    )
                )
            )
        )

        Intents.release()
    }

    @Test
    fun testSaveRestoreStateXml() {
        val context = ApplicationProvider.getApplicationContext() as Context
        var navController = NavController(context)
        val navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)

        val savedState = navController.saveState()
        navController = NavController(context)
        navController.navigatorProvider.addNavigator(navigator)

        // Restore state doesn't recreate any graph
        navController.restoreState(savedState)
        assertNull(navController.currentDestination)

        // Explicitly setting a graph then restores the state
        navController.setGraph(R.navigation.nav_simple)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)
        // Save state should be called on the navigator exactly once
        assertEquals(1, navigator.saveStateCount)
    }

    @Test
    fun testSaveRestoreStateDestinationChanged() {
        val context = ApplicationProvider.getApplicationContext() as Context
        var navController = NavController(context)
        val navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)

        navController.setGraph(R.navigation.nav_simple)

        val savedState = navController.saveState()
        navController = NavController(context)
        navController.navigatorProvider.addNavigator(navigator)

        // Restore state doesn't recreate any graph
        navController.restoreState(savedState)
        assertNull(navController.currentDestination)

        var destinationChangedCount = 0

        navController.addOnDestinationChangedListener { _, _, _ ->
            destinationChangedCount++
        }

        // Explicitly setting a graph then restores the state
        navController.setGraph(R.navigation.nav_simple)
        // Save state should be called on the navigator exactly once
        assertEquals(1, navigator.saveStateCount)
        // listener should have been fired again when state restored
        assertThat(destinationChangedCount).isEqualTo(1)
    }

    @Test
    fun testSaveRestoreStateProgrammatic() {
        val context = ApplicationProvider.getApplicationContext() as Context
        var navController = NavController(context)
        val navigator = TestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        val graph = NavInflater(context, navController.navigatorProvider)
            .inflate(R.navigation.nav_simple)
        navController.graph = graph
        navController.navigate(R.id.second_test)

        val savedState = navController.saveState()
        navController = NavController(context)
        navController.navigatorProvider.addNavigator(navigator)

        // Restore state doesn't recreate any graph
        navController.restoreState(savedState)
        assertNull(navController.currentDestination)

        // Explicitly setting a graph then restores the state
        navController.graph = graph
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)
    }

    @Test
    fun testSaveRestoreStateBundleParceled() {
        val context = ApplicationProvider.getApplicationContext() as Context
        var navController = NavController(context)
        val navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        navController.setGraph(R.navigation.nav_simple)

        navigator.customParcel = CustomTestParcelable(TEST_ARG_VALUE)

        val savedState = navController.saveState()

        val parcel = Parcel.obtain()
        savedState?.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restoredState = Bundle.CREATOR.createFromParcel(parcel)

        navController = NavController(context)
        navController.navigatorProvider.addNavigator(navigator)

        navController.restoreState(restoredState)
        navController.setGraph(R.navigation.nav_simple)

        // Ensure custom parcelable is present and can be read
        assertThat(navigator.customParcel?.name).isEqualTo(TEST_ARG_VALUE)
    }

    @Test
    fun testSaveRestoreAfterNavigateToDifferentNavGraph() {
        val context = ApplicationProvider.getApplicationContext() as Context
        var navController = NavController(context)
        val navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        navController.setGraph(R.navigation.nav_multiple_navigation)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val deepLink = Uri.parse("android-app://androidx.navigation.test/test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.deep_link_child_second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.navigate(R.id.simple_child_start)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(3)

        val savedState = navController.saveState()
        navController = NavController(context)
        navController.navigatorProvider.addNavigator(navigator)

        // Restore state doesn't recreate any graph
        navController.restoreState(savedState)
        assertThat(navController.currentDestination).isNull()

        // Explicitly setting a graph then restores the state
        navController.setGraph(R.navigation.nav_multiple_navigation)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(3)
        // Save state should be called on the navigator exactly once
        assertThat(navigator.saveStateCount).isEqualTo(1)
    }

    @Test
    fun testBackstackArgsBundleParceled() {
        val context = ApplicationProvider.getApplicationContext() as Context
        var navController = NavController(context)
        val navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)

        val backStackArg1 = Bundle()
        backStackArg1.putParcelable(TEST_ARG, CustomTestParcelable(TEST_ARG_VALUE))
        navController.setGraph(R.navigation.nav_arguments)
        navController.navigate(R.id.second_test, backStackArg1)

        val savedState = navController.saveState()

        val parcel = Parcel.obtain()
        savedState?.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restoredState = Bundle.CREATOR.createFromParcel(parcel)

        navController = NavController(context)
        navController.navigatorProvider.addNavigator(navigator)

        navController.restoreState(restoredState)
        navController.setGraph(R.navigation.nav_arguments)

        navController.addOnDestinationChangedListener { _, _, arguments ->
            assertThat(arguments?.getParcelable<CustomTestParcelable>(TEST_ARG)?.name)
                .isEqualTo(TEST_ARG_VALUE)
        }
    }

    @Test
    fun testNavigateArgs() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_arguments)

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val returnedArgs = navigator.current.second
        assertThat(returnedArgs).isNotNull()
        assertThat(returnedArgs!!["test_start_default"])
            .isEqualTo("default")

        navController.addOnDestinationChangedListener { _, _, arguments ->
            assertThat(arguments).isNotNull()
            assertThat(arguments!!["test_start_default"])
                .isEqualTo("default")
        }
    }

    @Test
    fun testNavigateWithNoDefaultValue() {
        val returnedArgs = navigateWithArgs(null)

        // Test that arguments without a default value aren't passed through at all
        assertFalse(returnedArgs.containsKey("test_no_default_value"))
    }

    @Test
    fun testNavigateWithDefaultArgs() {
        val returnedArgs = navigateWithArgs(null)

        // Test that default values are passed through
        assertEquals("default", returnedArgs.getString("test_default_value"))
    }

    @Test
    fun testNavigateWithArgs() {
        val args = Bundle()
        args.putString(TEST_ARG, TEST_ARG_VALUE)
        val returnedArgs = navigateWithArgs(args)

        // Test that programmatically constructed arguments are passed through
        assertEquals(TEST_ARG_VALUE, returnedArgs.getString(TEST_ARG))
    }

    @Test
    fun testNavigateWithOverriddenDefaultArgs() {
        val args = Bundle()
        args.putString(TEST_OVERRIDDEN_VALUE_ARG, TEST_OVERRIDDEN_VALUE_ARG_VALUE)
        val returnedArgs = navigateWithArgs(args)

        // Test that default values can be overridden by programmatic values
        assertEquals(TEST_OVERRIDDEN_VALUE_ARG_VALUE,
                returnedArgs.getString(TEST_OVERRIDDEN_VALUE_ARG))
    }

    private fun navigateWithArgs(args: Bundle?): Bundle {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_arguments)

        navController.navigate(R.id.second_test, args)

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val returnedArgs = navigator.current.second
        assertNotNull(returnedArgs)

        return returnedArgs!!
    }

    @Test
    fun testPopRoot() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)

        val success = navController.popBackStack()
        assertWithMessage("NavController should return false when popping the root")
                .that(success)
                .isFalse()
        assertNull(navController.currentDestination)
        assertEquals(0, navigator.backStack.size)
    }

    @Test
    fun testPopOnEmptyStack() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)

        val success = navController.popBackStack()
        assertWithMessage("NavController should return false when popping the root")
                .that(success)
                .isFalse()
        assertNull(navController.currentDestination)
        assertEquals(0, navigator.backStack.size)

        val popped = navController.popBackStack()
        assertWithMessage("popBackStack should return false when there's nothing on the " +
                "back stack")
            .that(popped)
            .isFalse()
    }

    @Test
    fun testNavigateThenPop() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)

        navController.navigate(R.id.second_test)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)

        val popped = navController.popBackStack()
        assertWithMessage("NavController should return true when popping a non-root destination")
            .that(popped)
            .isTrue()
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun testNavigateThenPopToUnknownDestination() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)

        navController.navigate(R.id.second_test)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)

        val popped = navController.popBackStack(UNKNOWN_DESTINATION_ID, false)
        assertWithMessage("Popping to an invalid destination should return false")
            .that(popped)
            .isFalse()
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)
    }

    @Test
    fun testNavigateThenNavigateWithPop() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)

        navController.navigate(R.id.second_test, null, navOptions {
            popUpTo(R.id.start_test) { inclusive = true }
        })
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun testNavigateThenNavigateWithPopRoot() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)

        navController.navigate(R.id.second_test, null, navOptions {
            popUpTo(R.id.nav_root) { inclusive = true }
        })
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun testNavigateThenNavigateUp() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)

        navController.navigate(R.id.second_test)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)

        // This should function identically to popBackStack()
        val success = navController.navigateUp()
        assertThat(success)
            .isTrue()
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun testNavigateViaAction() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(1, navigator.backStack.size)

        navController.navigate(R.id.second)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)
    }

    @Test
    fun testNavigateOptionSingleTop() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(2, navigator.backStack.size)

        navController.navigate(R.id.self)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)
    }

    @Test
    fun testNavigateOptionSingleTopNewArgs() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val args = Bundle()
        val testKey = "testKey"
        val testValue = "testValue"
        args.putString(testKey, testValue)

        var destinationListenerExecuted = false

        navController.navigate(R.id.self, args)

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            destinationListenerExecuted = true
            assertThat(destination.id).isEqualTo(R.id.second_test)
            assertThat(arguments?.getString(testKey)).isEqualTo(testValue)
        }

        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val returnedArgs = navigator.current.second
        assertThat(returnedArgs?.getString(testKey)).isEqualTo(testValue)
        assertThat(destinationListenerExecuted).isTrue()
    }

    @Test
    fun testNavigateOptionSingleTopReplaceNullArgs() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(1)
        assertThat(navigator.current.second).isNull()

        val args = Bundle()
        val testKey = "testKey"
        val testValue = "testValue"
        args.putString(testKey, testValue)

        var destinationListenerExecuted = false

        navController.navigate(R.id.start_test, args, navOptions {
            launchSingleTop = true
        })

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            destinationListenerExecuted = true
            assertThat(destination.id).isEqualTo(R.id.start_test)
            assertThat(arguments?.getString(testKey)).isEqualTo(testValue)
        }

        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val returnedArgs = navigator.current.second
        assertThat(returnedArgs?.getString(testKey)).isEqualTo(testValue)
        assertThat(destinationListenerExecuted).isTrue()
    }

    @Test
    fun testNavigateOptionSingleTopNewArgsIgnore() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)

        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val args = Bundle()
        val testKey = "testKey"
        val testValue = "testValue"
        args.putString(testKey, testValue)

        var destinationListenerExecuted = false

        navController.navigate(R.id.second_test, args)

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            destinationListenerExecuted = true
            assertThat(destination.id).isEqualTo(R.id.second_test)
            assertThat(arguments?.getString(testKey)).isEqualTo(testValue)
        }

        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val returnedArgs = navigator.current.second
        assertThat(returnedArgs?.getString(testKey)).isEqualTo(testValue)
        assertThat(destinationListenerExecuted).isTrue()
    }

    @Test
    fun testNavigateOptionPopUpToInAction() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(2, navigator.backStack.size)

        navController.navigate(R.id.finish)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun testNavigateWithPopUpOptionsOnly() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(2, navigator.backStack.size)

        val navOptions = navOptions {
            popUpTo = R.id.start_test
        }
        // the same as to call .navigate(R.id.finish)
        navController.navigate(0, null, navOptions)

        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun testNoDestinationNoPopUpTo() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val options = navOptions {}
        try {
            navController.navigate(0, null, options)
            fail("navController.navigate must throw")
        } catch (e: IllegalArgumentException) {
            // expected exception
        }
    }

    @Test
    fun testNavigateOptionPopSelf() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(2, navigator.backStack.size)

        navController.navigate(R.id.finish_self)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun testNavigateViaActionWithArgs() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_arguments)

        val args = Bundle()
        args.putString(TEST_ARG, TEST_ARG_VALUE)
        args.putString(TEST_OVERRIDDEN_VALUE_ARG, TEST_OVERRIDDEN_VALUE_ARG_VALUE)
        navController.navigate(R.id.second, args)

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val returnedArgs = navigator.current.second
        assertNotNull(returnedArgs)

        // Test that arguments without a default value aren't passed through at all
        assertFalse(returnedArgs!!.containsKey("test_no_default_value"))
        // Test that default values are passed through
        assertEquals("default", returnedArgs.getString("test_default_value"))
        // Test that programmatically constructed arguments are passed through
        assertEquals(TEST_ARG_VALUE, returnedArgs.getString(TEST_ARG))
        // Test that default values can be overridden by programmatic values
        assertEquals(TEST_OVERRIDDEN_VALUE_ARG_VALUE,
                returnedArgs.getString(TEST_OVERRIDDEN_VALUE_ARG))
        // Test that default values can be overridden by action default values
        assertEquals(
            TEST_OVERRIDDEN_VALUE_ARG_VALUE,
            returnedArgs.getString(TEST_ACTION_OVERRIDDEN_VALUE_ARG))
    }

    @Test
    fun testDeepLinkFromNavGraph() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)

        val taskStackBuilder = navController.createDeepLink()
                .setDestination(R.id.second_test)
                .createTaskStackBuilder()
        assertNotNull(taskStackBuilder)
        assertEquals(1, taskStackBuilder.intentCount)
    }

    @Test
    fun testDeepLinkIntent() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)

        val args = Bundle()
        args.putString("test", "test")
        val taskStackBuilder = navController.createDeepLink()
                .setDestination(R.id.second_test)
                .setArguments(args)
                .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertNotNull(intent)
        navController.handleDeepLink(intent)

        // The original Intent should be untouched and safely writable to a Parcel
        val p = Parcel.obtain()
        intent!!.writeToParcel(p, 0)
    }

    @Test
    fun testHandleDeepLinkValid() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val onDestinationChangedListener =
            mock(NavController.OnDestinationChangedListener::class.java)
        navController.addOnDestinationChangedListener(onDestinationChangedListener)
        verify(onDestinationChangedListener).onDestinationChanged(
            eq(navController),
            eq(navController.findDestination(R.id.start_test)),
            any())

        val taskStackBuilder = navController.createDeepLink()
            .setDestination(R.id.second_test)
            .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertNotNull(intent)
        assertWithMessage("NavController should handle deep links to its own graph")
            .that(navController.handleDeepLink(intent))
            .isTrue()

        // Verify that we navigated down to the deep link
        verify(onDestinationChangedListener, times(2)).onDestinationChanged(
            eq(navController),
            eq(navController.findDestination(R.id.start_test)),
            any())
        verify(onDestinationChangedListener).onDestinationChanged(
            eq(navController),
            eq(navController.findDestination(R.id.second_test)),
            any())
        verifyNoMoreInteractions(onDestinationChangedListener)
    }

    @Test
    fun testHandleDeepLinkNestedStartDestination() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_nested_start_destination)
        val onDestinationChangedListener =
            mock(NavController.OnDestinationChangedListener::class.java)
        navController.addOnDestinationChangedListener(onDestinationChangedListener)
        val startDestination = navController.findDestination(R.id.nested_test)
        verify(onDestinationChangedListener).onDestinationChanged(
            eq(navController),
            eq(startDestination),
            any())

        val taskStackBuilder = navController.createDeepLink()
            .setDestination(R.id.second_test)
            .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertNotNull(intent)
        assertWithMessage("NavController should handle deep links to its own graph")
            .that(navController.handleDeepLink(intent))
            .isTrue()

        // Verify that we navigated down to the deep link
        verify(onDestinationChangedListener, times(2)).onDestinationChanged(
            eq(navController),
            eq(startDestination),
            any())
        verify(onDestinationChangedListener).onDestinationChanged(
            eq(navController),
            eq(navController.findDestination(R.id.second_test)),
            any())
        verifyNoMoreInteractions(onDestinationChangedListener)
    }

    @Test
    fun testHandleDeepLinkMultipleDestinations() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_multiple_navigation)
        val onDestinationChangedListener =
            mock(NavController.OnDestinationChangedListener::class.java)
        navController.addOnDestinationChangedListener(onDestinationChangedListener)
        val startDestination = navController.findDestination(R.id.simple_child_start_test)
        verify(onDestinationChangedListener).onDestinationChanged(
            eq(navController),
            eq(startDestination),
            any())
        val childDestination = navController.findDestination(R.id.simple_child_second_test)

        val taskStackBuilder = navController.createDeepLink()
            .setDestination(R.id.simple_child_second_test)
            .addDestination(R.id.deep_link_child_second_test)
            .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertNotNull(intent)
        assertWithMessage("NavController should handle deep links to its own graph")
            .that(navController.handleDeepLink(intent))
            .isTrue()

        // Verify that we navigated down to the deep link
        // First to the destination added via setDestination()
        verify(onDestinationChangedListener, times(2)).onDestinationChanged(
            eq(navController),
            eq(startDestination),
            any())
        verify(onDestinationChangedListener).onDestinationChanged(
            eq(navController),
            eq(childDestination),
            any())
        // Then to the second destination added via addDestination()
        verify(onDestinationChangedListener).onDestinationChanged(
            eq(navController),
            eq(navController.findDestination(R.id.deep_link_child_start_test)),
            any())
        verify(onDestinationChangedListener).onDestinationChanged(
            eq(navController),
            eq(navController.findDestination(R.id.deep_link_child_second_test)),
            any())
        verifyNoMoreInteractions(onDestinationChangedListener)
    }

    @Test
    fun testHandleDeepLinkInvalid() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val onDestinationChangedListener =
            mock(NavController.OnDestinationChangedListener::class.java)
        navController.addOnDestinationChangedListener(onDestinationChangedListener)
        verify(onDestinationChangedListener).onDestinationChanged(
            eq(navController),
            eq(navController.findDestination(R.id.start_test)),
            any())

        val taskStackBuilder = navController.createDeepLink()
            .setGraph(R.navigation.nav_nested_start_destination)
            .setDestination(R.id.nested_second_test)
            .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertNotNull(intent)
        assertWithMessage("handleDeepLink should return false when passed an invalid deep link")
            .that(navController.handleDeepLink(intent))
            .isFalse()

        verifyNoMoreInteractions(onDestinationChangedListener)
    }

    @Test
    fun testHandleDeepLinkToRootInvalid() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val onDestinationChangedListener =
            mock(NavController.OnDestinationChangedListener::class.java)
        navController.addOnDestinationChangedListener(onDestinationChangedListener)
        verify(onDestinationChangedListener).onDestinationChanged(
            eq(navController),
            eq(navController.findDestination(R.id.start_test)),
            any())

        val taskStackBuilder = navController.createDeepLink()
            .setGraph(R.navigation.nav_nested_start_destination)
            .setDestination(R.id.nested_test)
            .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertNotNull(intent)
        assertWithMessage("handleDeepLink should return false when passed an invalid deep link")
            .that(navController.handleDeepLink(intent))
            .isFalse()

        verifyNoMoreInteractions(onDestinationChangedListener)
    }

    private fun createNavController(): NavController {
        val navController = NavController(ApplicationProvider.getApplicationContext())
        val navigator = TestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        return navController
    }
}

class TestActivity : ComponentActivity() {

    val navController: NavController = createNavController(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(View(this))
    }

    private fun createNavController(activity: Activity): NavController {
        val navController = NavController(activity)
        val navigator = TestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        return navController
    }
}

/**
 * [TestNavigator] that helps with testing saving and restoring state.
 */
@Navigator.Name("test")
class SaveStateTestNavigator : TestNavigator() {

    companion object {
        private const val STATE_SAVED_COUNT = "saved_count"
        private const val TEST_PARCEL = "test_parcel"
    }

    var saveStateCount = 0
    var customParcel: CustomTestParcelable? = null

    override fun onSaveState(): Bundle? {
        saveStateCount += 1
        val state = Bundle()
        state.putInt(STATE_SAVED_COUNT, saveStateCount)
        state.putParcelable(TEST_PARCEL, customParcel)
        return state
    }

    override fun onRestoreState(savedState: Bundle) {
        saveStateCount = savedState.getInt(STATE_SAVED_COUNT)
        customParcel = savedState.getParcelable(TEST_PARCEL)
    }
}

/**
 * [CustomTestParcelable] that helps testing bundled custom parcels
 */
data class CustomTestParcelable(val name: String?) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readString())

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(name)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<CustomTestParcelable> {
        override fun createFromParcel(parcel: Parcel) = CustomTestParcelable(parcel)

        override fun newArray(size: Int): Array<CustomTestParcelable?> = arrayOfNulls(size)
    }
}
