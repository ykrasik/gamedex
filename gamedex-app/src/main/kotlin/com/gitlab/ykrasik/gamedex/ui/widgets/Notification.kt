/**
 * Copyright (c) 2014, 2016, ControlsFX
 * All rights reserved.

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gitlab.ykrasik.gamedex.ui.widgets

import com.gitlab.ykrasik.gamedex.ui.theme.Theme.Images
import impl.org.controlsfx.skin.NotificationBar
import javafx.animation.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.image.ImageView
import javafx.stage.Popup
import javafx.stage.PopupWindow
import javafx.stage.Screen
import javafx.stage.Window
import javafx.util.Duration
import org.controlsfx.control.action.Action
import org.controlsfx.tools.Utils
import java.lang.ref.WeakReference
import java.util.*

/**
 * An API to show popup notification messages to the user in the corner of their
 * screen, unlike the [NotificationPane] which shows notification messages
 * within your application itself.

 * <h3>Screenshot</h3>
 *
 *
 * The following screenshot shows a sample notification rising from the
 * bottom-right corner of my screen:

 * <br></br>
 * <br></br>
 * <img src="notifications.png" alt="Screenshot of Notifications"></img>

 * <h3>Code Example:</h3>
 *
 *
 * To create the notification shown in the screenshot, simply do the following:

 * <pre>
 * `Notifications.create()
 * .title("Title Text")
 * .text("Hello World 0!")
 * .showWarning();
` *
</pre> *
 */
class Notification {
    private var title: String? = null
    private var text: String? = null
    private var graphic: Node? = null
    private var actions = FXCollections.observableArrayList<Action>()
    private var position = Pos.TOP_RIGHT
    private var automaticallyHideAfterDuration: Duration? = null
    private var hideCloseButton: Boolean = false
    private var onAction: EventHandler<ActionEvent>? = null
    private var owner: Window? = null
    private var screen = Screen.getPrimary()

    private val styleClass = ArrayList<String>()

    /**
     * Specify the text to show in the notification.
     */
    fun text(text: String): Notification = apply { this.text = text }

    /**
     * Specify the title to show in the notification.
     */
    fun title(title: String): Notification = apply { this.title = title }

    /**
     * Specify the graphic to show in the notification.
     */
    fun graphic(graphic: Node): Notification = apply { this.graphic = graphic }

    /**
     * Instructs the notification to be shown, and that it should use the
     * built-in 'confirm' graphic.
     */
    fun confirm(): Notification = graphic(ImageView(Images.confirm))

    /**
     * Instructs the notification to be shown, and that it should use the
     * built-in 'information' graphic.
     */
    fun information(): Notification = graphic(ImageView(Images.information))

    /**
     * Instructs the notification to be shown, and that it should use the
     * built-in 'warning' graphic.
     */
    fun warning(): Notification = graphic(ImageView(Images.warning))

    /**
     * Instructs the notification to be shown, and that it should use the
     * built-in 'error' graphic.
     */
    fun error(): Notification = graphic(ImageView(Images.error))

    /**
     * Specify the position of the notification on screen, by default it is
     * [bottom-right][Pos.BOTTOM_RIGHT].
     */
    fun position(position: Pos): Notification = apply { this.position = position }

    /**
     * The dialog window owner - which can be [Screen], [Window]
     * or [Node]. If specified, the notifications will be inside
     * the owner, otherwise the notifications will be shown within the whole
     * primary (default) screen.
     */
    fun owner(owner: Any): Notification = apply {
        if (owner is Screen) {
            this.screen = owner
        } else {
            this.owner = Utils.getWindow(owner)
        }
    }

    /**
     * Specify the duration that the notification should show, after which it will automatically be hidden.
     * If null, will be shown until manually closed.
     */
    fun automaticallyHideAfter(duration: Duration?): Notification = apply { this.automaticallyHideAfterDuration = duration }

    /**
     * Specify what to do when the user clicks on the notification (in addition
     * to the notification hiding, which happens whenever the notification is
     * clicked on).
     */
    fun onAction(onAction: EventHandler<ActionEvent>): Notification = apply { this.onAction = onAction }

    /**
     * Specify that the notification should use the built-in dark styling,
     * rather than the default 'modena' notification style (which is a
     * light-gray).
     */
    fun darkStyle(): Notification = apply { styleClass.add(STYLE_CLASS_DARK) }

    /**
     * Specify that the close button in the top-right corner of the notification
     * should not be shown.
     */
    fun hideCloseButton(): Notification = apply { this.hideCloseButton = true }

    /**
     * Specify the actions that should be shown in the notification as buttons.
     */
    fun action(vararg actions: Action): Notification = apply {
        this.actions = FXCollections.observableArrayList(*actions)
    }

    /**
     * Instructs the notification to be shown.
     */
    fun show() {
        NotificationPopupHandler.show(this)
    }

    /**
     * Instructs the notification to be hidden, possibly after a certain delay.
     */
    fun hide(afterDelay: Duration? = null) {
        NotificationPopupHandler.hide(this, afterDelay)
    }

    val isShowing get() = NotificationPopupHandler.isShowing(this)

    private object NotificationPopupHandler {
        private var startX = 0.0
        private var startY = 0.0
        private var screenWidth = 0.0
        private var screenHeight = 0.0

        private val notificationsMap = mutableMapOf<Notification, Popup>()
        private val popupsMap = mutableMapOf<Pos, MutableList<Popup>>()
        private val padding = 15.0

        // for animating in the notifications
        private val parallelTransition = ParallelTransition()

        private var isShowing = false

        fun show(notification: Notification) {
            val window = if (notification.owner == null) {
                // If the owner is not set, we work with the whole screen.
                val screenBounds = notification.screen.visualBounds
                startX = screenBounds.minX
                startY = screenBounds.minY
                screenWidth = screenBounds.width
                screenHeight = screenBounds.height
                Utils.getWindow(null)
            } else {
                // If the owner is set, we will make the notifications popup inside its window.
                startX = notification.owner!!.x
                startY = notification.owner!!.y
                screenWidth = notification.owner!!.width
                screenHeight = notification.owner!!.height
                notification.owner
            }
            show(window, notification)
        }

        private fun show(owner: Window?, notification: Notification) {
            // Stylesheets which are added to the scene of a popup aren't
            // considered for styling. For this reason, we need to find the next
            // window in the hierarchy which isn't a popup.
            var ownerWindow = owner
            while (ownerWindow is PopupWindow) {
                ownerWindow = ownerWindow.ownerWindow
            }
            // need to install our CSS
            val ownerScene = ownerWindow?.scene
            if (ownerScene != null) {
                val stylesheetUrl = Notification::class.java.getResource("/org/controlsfx/control/notificationpopup.css").toExternalForm() //$NON-NLS-1$
                if (!ownerScene.stylesheets.contains(stylesheetUrl)) {
                    // The stylesheet needs to be added at the beginning so that
                    // the styling can be adjusted with custom stylesheets.
                    ownerScene.stylesheets.add(0, stylesheetUrl)
                }
            }

            val popup = Popup()
            popup.isAutoFix = false

            val p = notification.position

            val notificationBar = object : NotificationBar() {
                override fun getTitle(): String? = notification.title
                override fun getText(): String? = notification.text
                override fun getGraphic(): Node? = notification.graphic
                override fun getActions(): ObservableList<Action> = notification.actions
                override fun isShowing(): Boolean = this@NotificationPopupHandler.isShowing

                override fun computeMinWidth(height: Double): Double {
                    val text = text
                    val graphic = graphic
                    if ((text == null || text.isEmpty()) && graphic != null) {
                        return graphic.minWidth(height)
                    }
                    return 400.0
                }

                override fun computeMinHeight(width: Double): Double {
                    val text = text
                    val graphic = graphic
                    if ((text == null || text.isEmpty()) && graphic != null) {
                        return graphic.minHeight(width)
                    }
                    return 100.0
                }

                override fun isShowFromTop(): Boolean = this@NotificationPopupHandler.isShowFromTop(notification.position)

                override fun hide() {
                    this@NotificationPopupHandler.isShowing = false

                    // this would slide the notification bar out of view,
                    // but I prefer the fade out below
                    // doHide();

                    // animate out the popup by fading it
                    createHideTimeline(notification, this, Duration.ZERO).play()
                }

                override fun isCloseButtonVisible(): Boolean = !notification.hideCloseButton
                override fun getContainerHeight(): Double = startY + screenHeight

                override fun relocateInParent(x: Double, y: Double) {
                    // this allows for us to slide the notification upwards
                    when (p) {
                        Pos.BOTTOM_LEFT, Pos.BOTTOM_CENTER, Pos.BOTTOM_RIGHT ->
                            popup.anchorY = y - this@NotificationPopupHandler.padding
                        else -> {
                        }// no-op
                    }
                }
            }

            notificationBar.styleClass.addAll(notification.styleClass)

            notificationBar.setOnMouseClicked {
                if (notification.onAction != null) {
                    val actionEvent = ActionEvent(notificationBar, notificationBar)
                    notification.onAction!!.handle(actionEvent)

                    // animate out the popup
                    createHideTimeline(notification, notificationBar, Duration.ZERO).play()
                }
            }

            popup.content.add(notificationBar)
            popup.show(owner, 0.0, 0.0)

            // determine location for the popup
            val barWidth = notificationBar.width
            val barHeight = notificationBar.height

            // get anchorX
            popup.anchorX = when (p) {
                Pos.TOP_LEFT, Pos.CENTER_LEFT, Pos.BOTTOM_LEFT -> padding + startX
                Pos.TOP_CENTER, Pos.CENTER, Pos.BOTTOM_CENTER -> startX + screenWidth / 2.0 - barWidth / 2.0 - padding / 2.0
                else -> startX + screenWidth - barWidth - padding
            }

            // get anchorY
            popup.anchorY = when (p) {
                Pos.TOP_LEFT, Pos.TOP_CENTER, Pos.TOP_RIGHT -> padding + startY
                Pos.CENTER_LEFT, Pos.CENTER, Pos.CENTER_RIGHT -> startY + screenHeight / 2.0 - barHeight / 2.0 - padding / 2.0
                else -> startY + screenHeight - barHeight - padding
            }

            isShowing = true
            notificationBar.doShow()

            notificationsMap += notification to popup
            addPopupToMap(p, popup)

            // begin a timeline to get rid of the popup
            if (notification.automaticallyHideAfterDuration != null) {
                createHideTimeline(notification, notificationBar, notification.automaticallyHideAfterDuration!!).play()
            }
        }

        fun hide(notification: Notification, afterDelay: Duration? = null) {
            if (afterDelay == null) {
                doHide(notification)
            } else {
                val notificationBar = notificationsMap[notification]!!.content.first() as NotificationBar
                createHideTimeline(notification, notificationBar, afterDelay).play()
            }
        }

        private fun doHide(notification: Notification) {
            val popup = notificationsMap.remove(notification)
            if (popup != null) {
                // Popup may have already been hidden (by clicking the close button)
                popup.hide()
                removePopupFromMap(notification.position, popup)
            }
        }

        fun isShowing(notification: Notification) = notificationsMap.containsKey(notification)

        private fun createHideTimeline(notification: Notification, bar: NotificationBar, startDelay: Duration): Timeline {
            val fadeOutBegin = KeyValue(bar.opacityProperty(), 1.0)
            val fadeOutEnd = KeyValue(bar.opacityProperty(), 0.0)

            val kfBegin = KeyFrame(Duration.ZERO, fadeOutBegin)
            val kfEnd = KeyFrame(Duration.millis(500.0), fadeOutEnd)

            val timeline = Timeline(kfBegin, kfEnd)
            timeline.delay = startDelay
            timeline.onFinished = EventHandler<ActionEvent> { doHide(notification) }

            return timeline
        }

        private fun addPopupToMap(p: Pos, popup: Popup) {
            val popups = popupsMap.getOrPut(p) { mutableListOf() }

            doAnimation(p, popup)

            // add the popup to the list so it is kept in memory and can be accessed later on
            popups.add(popup)
        }

        private fun removePopupFromMap(p: Pos, popup: Popup) = popupsMap[p]?.remove(popup)

        private fun doAnimation(p: Pos, changedPopup: Popup) {
            val popups = popupsMap[p] ?: return

            val newPopupHeight = changedPopup.content[0].boundsInParent.height

            parallelTransition.stop()
            parallelTransition.children.clear()

            val isShowFromTop = isShowFromTop(p)

            // animate all other popups in the list upwards so that the new one is in the 'new' area.
            // firstly, we need to determine the target positions for all popups
            var sum = 0.0
            val targetAnchors = DoubleArray(popups.size)
            popups.reversed().forEachIndexed { i, popup ->
                val popupHeight = popup.content[0].boundsInParent.height

                if (isShowFromTop) {
                    if (i == popups.size - 1) {
                        sum = startY + newPopupHeight + padding
                    } else {
                        sum += popupHeight
                    }
                    targetAnchors[i] = sum
                } else {
                    if (i == popups.size - 1) {
                        sum = changedPopup.anchorY - popupHeight
                    } else {
                        sum -= popupHeight
                    }

                    targetAnchors[i] = sum
                }
            }

            // then we set up animations for each popup to animate towards the target
            popups.reversed().forEachIndexed { i, popup ->
                val anchorYTarget = targetAnchors[i]
                if (anchorYTarget < 0) {
                    popup.hide()
                }
                val oldAnchorY = popup.anchorY
                val distance = anchorYTarget - oldAnchorY

                val t = CustomTransition(popup, oldAnchorY, distance)
                parallelTransition.children.add(t)
            }
            parallelTransition.play()
        }

        private fun isShowFromTop(p: Pos): Boolean = when (p) {
            Pos.TOP_LEFT, Pos.TOP_CENTER, Pos.TOP_RIGHT -> true
            else -> false
        }

        private class CustomTransition(popup: Popup, private val oldAnchorY: Double, private val distance: Double) : Transition() {
            private val popupWeakReference: WeakReference<Popup> = WeakReference(popup)

            init {
                cycleCount = 1
                cycleDuration = Duration.millis(350.0)
            }

            override fun interpolate(frac: Double) {
                val popup = popupWeakReference.get()
                if (popup != null) {
                    val newAnchorY = oldAnchorY + distance * frac
                    popup.anchorY = newAnchorY
                }
            }

        }
    }

    companion object {
        private val STYLE_CLASS_DARK = "dark" //$NON-NLS-1$
    }
}

