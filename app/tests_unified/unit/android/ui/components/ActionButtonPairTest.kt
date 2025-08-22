package com.multisensor.recording.ui.components

import android.content.Context
import android.widget.Button
import androidx.test.core.app.ApplicationProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ActionButtonPairTest : DescribeSpec({

    val context: Context = ApplicationProvider.getApplicationContext()
    lateinit var actionButtonPair: ActionButtonPair

    beforeEach {
        actionButtonPair = ActionButtonPair(context)
    }

    describe("ActionButtonPair initialization") {

        it("should initialize correctly") {
            actionButtonPair shouldNotBe null
            actionButtonPair.childCount shouldBe 2

            actionButtonPair.getChildAt(0).shouldBeInstanceOf<Button>()
            actionButtonPair.getChildAt(1).shouldBeInstanceOf<Button>()
        }
    }

    describe("ActionButtonPair button configuration") {

        it("should set button text and styles correctly") {
            actionButtonPair.setButtons(
                "Start Recording",
                "Stop Recording",
                ActionButtonPair.ButtonStyle.PRIMARY,
                ActionButtonPair.ButtonStyle.SECONDARY
            )

            val leftButton = actionButtonPair.getLeftButton()
            val rightButton = actionButtonPair.getRightButton()

            leftButton.text shouldBe "Start Recording"
            rightButton.text shouldBe "Stop Recording"
        }

        it("should set buttons with default styles") {
            actionButtonPair.setButtons("Connect", "Disconnect")

            val leftButton = actionButtonPair.getLeftButton()
            val rightButton = actionButtonPair.getRightButton()

            leftButton.text shouldBe "Connect"
            rightButton.text shouldBe "Disconnect"
        }

        it("should handle all button style combinations") {
            val styles = arrayOf(
                ActionButtonPair.ButtonStyle.PRIMARY,
                ActionButtonPair.ButtonStyle.SECONDARY,
                ActionButtonPair.ButtonStyle.NEUTRAL,
                ActionButtonPair.ButtonStyle.WARNING
            )

            for (leftStyle in styles) {
                for (rightStyle in styles) {
                    actionButtonPair.setButtons("Left", "Right", leftStyle, rightStyle)
                    actionButtonPair.getLeftButton().text shouldBe "Left"
                    actionButtonPair.getRightButton().text shouldBe "Right"
                }
            }
        }
    }

    describe("ActionButtonPair click listener functionality") {

        it("should handle click listeners correctly") {
            var leftClicked = false
            var rightClicked = false

            val leftListener = android.view.View.OnClickListener { leftClicked = true }
            val rightListener = android.view.View.OnClickListener { rightClicked = true }

            actionButtonPair.setOnClickListeners(leftListener, rightListener)

            actionButtonPair.getLeftButton().performClick()
            actionButtonPair.getRightButton().performClick()

            leftClicked shouldBe true
            rightClicked shouldBe true
        }

        it("should handle null click listeners gracefully") {
            actionButtonPair.setOnClickListeners(null, null)

            actionButtonPair.getLeftButton().performClick()
            actionButtonPair.getRightButton().performClick()
        }
    }

    describe("ActionButtonPair button state management") {

        it("should enable and disable buttons correctly") {
            actionButtonPair.setButtonsEnabled(true, false)

            actionButtonPair.getLeftButton().isEnabled shouldBe true
            actionButtonPair.getRightButton().isEnabled shouldBe false

            actionButtonPair.setButtonsEnabled(false, true)

            actionButtonPair.getLeftButton().isEnabled shouldBe false
            actionButtonPair.getRightButton().isEnabled shouldBe true
        }
    }

    describe("ActionButtonPair component structure") {

        it("should provide correct button references") {
            val leftButton = actionButtonPair.getLeftButton()
            val rightButton = actionButtonPair.getRightButton()

            leftButton shouldNotBe null
            rightButton shouldNotBe null
            leftButton.shouldBeInstanceOf<Button>()
            rightButton.shouldBeInstanceOf<Button>()
            leftButton shouldBe actionButtonPair.getChildAt(0)
            rightButton shouldBe actionButtonPair.getChildAt(1)
        }

        it("should have horizontal layout orientation") {
            actionButtonPair.orientation shouldBe android.widget.LinearLayout.HORIZONTAL
        }

        it("should have correct button layout parameters") {
            val leftButton = actionButtonPair.getLeftButton()
            val rightButton = actionButtonPair.getRightButton()

            val leftParams = leftButton.layoutParams as android.widget.LinearLayout.LayoutParams
            val rightParams = rightButton.layoutParams as android.widget.LinearLayout.LayoutParams

            leftParams.weight shouldBe 1f
            rightParams.weight shouldBe 1f

            leftParams.width shouldBe 0
            rightParams.width shouldBe 0
        }
    }

    describe("ActionButtonPair usage scenarios") {

        it("should handle recording button scenario correctly") {
            actionButtonPair.setButtons("Start Recording", "Stop Recording")
            actionButtonPair.setButtonsEnabled(true, false)

            var recordingStarted = false
            var recordingStopped = false

            actionButtonPair.setOnClickListeners(
                {
                    recordingStarted = true
                    actionButtonPair.setButtonsEnabled(false, true)
                },
                {
                    recordingStopped = true
                    actionButtonPair.setButtonsEnabled(true, false)
                }
            )

            actionButtonPair.getLeftButton().performClick()
            recordingStarted shouldBe true
            actionButtonPair.getLeftButton().isEnabled shouldBe false
            actionButtonPair.getRightButton().isEnabled shouldBe true

            actionButtonPair.getRightButton().performClick()
            recordingStopped shouldBe true
            actionButtonPair.getLeftButton().isEnabled shouldBe true
            actionButtonPair.getRightButton().isEnabled shouldBe false
        }

        it("should handle connect/disconnect scenario correctly") {
            actionButtonPair.setButtons(
                "Connect",
                "Disconnect",
                ActionButtonPair.ButtonStyle.PRIMARY,
                ActionButtonPair.ButtonStyle.SECONDARY
            )

            var connected = false

            actionButtonPair.setOnClickListeners(
                {
                    connected = true
                    actionButtonPair.setButtonsEnabled(false, true)
                },
                {
                    connected = false
                    actionButtonPair.setButtonsEnabled(true, false)
                }
            )

            actionButtonPair.getLeftButton().performClick()
            connected shouldBe true

            actionButtonPair.getRightButton().performClick()
            connected shouldBe false
        }

        it("should handle multiple style changes correctly") {
            actionButtonPair.setButtons(
                "Test1",
                "Test2",
                ActionButtonPair.ButtonStyle.PRIMARY,
                ActionButtonPair.ButtonStyle.SECONDARY
            )
            actionButtonPair.setButtons(
                "Test3",
                "Test4",
                ActionButtonPair.ButtonStyle.NEUTRAL,
                ActionButtonPair.ButtonStyle.WARNING
            )
            actionButtonPair.setButtons(
                "Test5",
                "Test6",
                ActionButtonPair.ButtonStyle.WARNING,
                ActionButtonPair.ButtonStyle.PRIMARY
            )

            actionButtonPair.getLeftButton().text shouldBe "Test5"
            actionButtonPair.getRightButton().text shouldBe "Test6"
        }
    }
})
