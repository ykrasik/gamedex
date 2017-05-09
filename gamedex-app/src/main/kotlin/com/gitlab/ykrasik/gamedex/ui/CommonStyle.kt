package com.gitlab.ykrasik.gamedex.ui

import javafx.geometry.Pos
import javafx.scene.control.ContentDisplay
import javafx.scene.effect.BlurType
import javafx.scene.effect.DropShadow
import javafx.scene.paint.Color
import tornadofx.*

/**
 * User: ykrasik
 * Date: 06/05/2017
 * Time: 13:43
 */
class CommonStyle : Stylesheet() {
    companion object {
        val hoverable by cssclass()

        // TODO: Move this to the GameWallStyle?
        val card by cssclass()

        val jfxButton by cssclass()

        val extraButton by cssclass()
        val acceptButton by cssclass()
        val cancelButton by cssclass()
        val deleteButton by cssclass()

        val popoverMenu by cssclass()

        init {
            importStylesheet(CommonStyle::class)
        }
    }

    init {
//        root {
//            unsafe("-fx-accent", linkColor)
//            unsafe("-fx-faint-focus-color", raw("transparent"))
//            unsafe("-fx-text-background-color", raw("ladder( -fx-background, -fx-dark-text-color 46%, -fx-dark-text-color 59%, -fx-mid-text-color 60% )"))
//        }
//
//        rowWrapper {
//            alignment = CENTER
//        }
//
//        rowWrapper child star {
//            alignment = CENTER
//        }
//
//        linkLook {
//            fill = linkColor
//            textFill = linkColor
//        }
//
//        contentWrapper {
//            minWidth = pageWidth
//            maxWidth = minWidth
//            alignment = CENTER
//        }
//
//        content {
//            minWidth = pageWidth
//            maxWidth = minWidth
//            alignment = CENTER_LEFT
//        }
//
//        defaultContentPadding {
//            padding = box(15.px)
//        }
//
//        defaultSpacing {
//            spacing = 10.px
//        }
//
//        lightBackground {
//            backgroundColor += lightBackgroundColor
//        }
//
//        whiteBackground {
//            backgroundColor += WHITE
//        }
//
//        bold {
//            fontWeight = BOLD
//        }
//
//        black {
//            textFill = BLACK
//        }
//
//        codeview child contentWrapper {
//            spacing = 10.px
//        }
//
//        loginScreen {
//            val common = mixin {
//                borderRadius += box(4.px)
//                maxWidth = 316.px
//                alignment = CENTER
//            }
//            fontSize = 14.px
//            alignment = CENTER
//            padding = box(40.px)
//            spacing = 20.px
//            backgroundColor += lightBackgroundColor
//            form {
//                +common
//                field {
//                    padding = box(8.px, 0.px)
//                }
//                labelContainer {
//                    padding = box(5.px, 0.px)
//                }
//                textField {
//                    minHeight = 33.px
//                }
//                padding = box(15.px)
//                backgroundColor += WHITE
//                borderColor += box(borderLineColor)
//                successButton {
//                    prefWidth = infinity
//                }
//            }
//            newToGitHub {
//                +common
//                borderColor += box(borderLineColor)
//                padding = box(12.px)
//            }
//            errorMessage {
//                +common
//                label {
//                    textFill = c(153, 17, 17)
//                }
//                icon {
//                    backgroundColor += c(153, 17, 17)
//                }
//                backgroundColor += c(252, 222, 222)
//                borderColor += box(c(210, 178, 178))
//                padding = box(12.px)
//            }
//            footer {
//                padding = box(50.px, 0.px)
//                alignment = CENTER
//                label {
//                    textFill = darkTextColor
//                }
//            }
//        }
//
//        userscreen {
//            backgroundColor += WHITE
//            userinfo {
//                backgroundColor += WHITE
//                padding = box(20.px, 10.px)
//                minWidth = pageWidth / 4
//                maxWidth = minWidth
//                stat {
//                    alignment = CENTER
//                    borderColor += box(borderLineColor, TRANSPARENT)
//                }
//                stat child star {
//                    spacing = 2.px
//                    padding = box(10.px)
//                    alignment = CENTER
//                    s(label) {
//                        fontSize = 22.px
//                        fontWeight = BOLD
//                        textFill = linkColor
//                    }
//                    fontSize = 12.px
//                }
//            }
//            detail {
//                backgroundColor += WHITE
//                minWidth = pageWidth * 0.75
//                maxWidth = minWidth
//            }
//        }
//
//        statsbar {
//            borderColor += box(TRANSPARENT, TRANSPARENT, contrastColor, TRANSPARENT)
//            borderWidth += box(0.px, 0.px, 8.px, 0.px)
//
//            borderColor += box(borderLineColor)
//            borderWidth += box(1.px)
//
//            label {
//                textFill = darkTextColor
//            }
//
//            stat {
//                spacing = 3.px
//                padding = box(14.px, 0.px)
//                alignment = CENTER
//            }
//        }
//
//        topbar {
//            backgroundColor += darkBackgroundColor
//            padding = box(10.px, 0.px)
//            content {
//                spacing = 20.px
//            }
//            borderColor += box(TRANSPARENT, TRANSPARENT, borderLineColor, TRANSPARENT)
//            label {
//                fontWeight = BOLD
//                fontSize = 16.px
//            }
//        }
//
//        hContainer {
//            spacing = 6.px
//            padding = box(0.px, 0.px)
//            alignment = CENTER_LEFT
//        }
//
//        h1 {
//            fontSize = 22.px
//        }
//
//        h2 {
//            fontSize = 18.px
//            textFill = darkTextColor
//        }
//
//        head {
//            padding = box(20.px, 0.px)
//            spacing = 20.px
//            backgroundColor += lightBackgroundColor
//            label {
//                fontWeight = BOLD
//                fontSize = 18.px
//            }
//        }
//
//        s(listView, tableView) {
//            s(hover, selected) {
//                backgroundColor += darkBackgroundColor
//            }
//            focusColor = TRANSPARENT
//            faintFocusColor = TRANSPARENT
//            add(focused) {
//                unsafe("-fx-background-color", raw("-fx-box-border, -fx-control-inner-background"))
//                backgroundInsets = multi(box(0.px), box(1.px))
//                padding = box(1.px)
//            }
//        }
//
//        listCell and odd {
//            unsafe("-fx-background", raw("-fx-control-inner-background"))
//        }
//
//        tabPane {
//            prefWidth = pageWidth
//            tabHeaderBackground {
//                backgroundColor += lightBackgroundColor
//                borderColor += box(TRANSPARENT, TRANSPARENT, borderLineColor, TRANSPARENT)
//            }
//            tab {
//                backgroundColor += TRANSPARENT
//                textFill = darkTextColor
//                padding = box(7.px, 11.px)
//            }
//            tab and selected {
//                backgroundColor += WHITE
//                borderColor += box(contrastColor, TRANSPARENT, TRANSPARENT, TRANSPARENT)
//                borderColor += box(TRANSPARENT, borderLineColor)
//                borderColor += box(TRANSPARENT)
//                borderWidth += box(3.px)
//                borderWidth += box(1.px)
//                borderWidth += box(1.px)
//                borderRadius += box(3.px, 3.px, 0.px, 0.px)
//                focusColor = TRANSPARENT
//                faintFocusColor = TRANSPARENT
//            }
//        }
//
//        issuelist contains icon {
//            translateY = 3.px
//        }
//
//        issuelist contains listCell {
//            padding = box(10.px)
//        }
//
//        scrollBar {
//            padding = box(0.px)
//            prefWidth = 12.px
//            prefHeight = 12.px
//            track {
//                backgroundColor += c("#f3f3f3")
//            }
//            thumb {
//                borderColor += box(c("#bbb"))
//                backgroundColor += c("#f3f3f3")
//            }
//            add(hover, pressed) {
//                thumb {
//                    backgroundColor += c("#757575")
//                    borderColor += box(c("#757575"))
//                    backgroundInsets += box(0.px)
//                }
//            }
//        }
//
//        // Buttons
//        successButton {
//            borderRadius += box(4.px)
//            padding = box(8.px, 15.px)
//            backgroundInsets += box(0.px)
//            borderColor += box(c("#5ca941"))
//            textFill = WHITE
//            fontWeight = BOLD
//            backgroundColor += LinearGradient(0.0, 0.0, 0.0, 1.0, true, CycleMethod.NO_CYCLE, Stop(0.0, c("#8add6d")), Stop(1.0, c("#60b044")))
//            and(hover) {
//                backgroundColor += LinearGradient(0.0, 0.0, 0.0, 1.0, true, CycleMethod.NO_CYCLE, Stop(0.0, c("#79d858")), Stop(1.0, c("#569e3d")))
//            }
//            and(pressed) {
//                backgroundColor += c("#569e3d")
//            }
//            icon {
//                backgroundColor += WHITE
//            }
//        }
//
//        // Icons
//        icon {
//            minWidth = 16.px
//            maxWidth = 16.px
//            minHeight = 16.px
//            maxHeight = 16.px
//            backgroundColor += GRAY
//            add(small) {
//                minWidth = 12.px
//                maxWidth = 12.px
//                minHeight = 12.px
//                maxHeight = 12.px
//            }
//            add(medium) {
//                minWidth = 28.px
//                maxWidth = 28.px
//                minHeight = 28.px
//                maxHeight = 28.px
//            }
//            add(large) {
//                minWidth = 48.px
//                maxWidth = 48.px
//                minHeight = 48.px
//                maxHeight = 48.px
//            }
//        }
//        repoIcon { shape = "M4 9H3V8h1v1zm0-3H3v1h1V6zm0-2H3v1h1V4zm0-2H3v1h1V2zm8-1v12c0 .55-.45 1-1 1H6v2l-1.5-1.5L3 16v-2H1c-.55 0-1-.45-1-1V1c0-.55.45-1 1-1h10c.55 0 1 .45 1 1zm-1 10H1v2h2v-1h3v1h5v-2zm0-10H2v9h9V1z" }
//        codeIcon { shape = "M9.5 3L8 4.5 11.5 8 8 11.5 9.5 13 14 8 9.5 3zm-5 0L0 8l4.5 5L6 11.5 2.5 8 6 4.5 4.5 3z" }
//        issuesIcon { shape = "M7 2.3c3.14 0 5.7 2.56 5.7 5.7s-2.56 5.7-5.7 5.7A5.71 5.71 0 0 1 1.3 8c0-3.14 2.56-5.7 5.7-5.7zM7 1C3.14 1 0 4.14 0 8s3.14 7 7 7 7-3.14 7-7-3.14-7-7-7zm1 3H6v5h2V4zm0 6H6v2h2v-2z" }
//        pullRequestsIcon { shape = "M11 11.28V5c-.03-.78-.34-1.47-.94-2.06C9.46 2.35 8.78 2.03 8 2H7V0L4 3l3 3V4h1c.27.02.48.11.69.31.21.2.3.42.31.69v6.28A1.993 1.993 0 0 0 10 15a1.993 1.993 0 0 0 1-3.72zm-1 2.92c-.66 0-1.2-.55-1.2-1.2 0-.65.55-1.2 1.2-1.2.65 0 1.2.55 1.2 1.2 0 .65-.55 1.2-1.2 1.2zM4 3c0-1.11-.89-2-2-2a1.993 1.993 0 0 0-1 3.72v6.56A1.993 1.993 0 0 0 2 15a1.993 1.993 0 0 0 1-3.72V4.72c.59-.34 1-.98 1-1.72zm-.8 10c0 .66-.55 1.2-1.2 1.2-.65 0-1.2-.55-1.2-1.2 0-.65.55-1.2 1.2-1.2.65 0 1.2.55 1.2 1.2zM2 4.2C1.34 4.2.8 3.65.8 3c0-.65.55-1.2 1.2-1.2.65 0 1.2.55 1.2 1.2 0 .65-.55 1.2-1.2 1.2z" }
//        settingsIcon { shape = "M14 8.77v-1.6l-1.94-.64-.45-1.09.88-1.84-1.13-1.13-1.81.91-1.09-.45-.69-1.92h-1.6l-.63 1.94-1.11.45-1.84-.88-1.13 1.13.91 1.81-.45 1.09L0 7.23v1.59l1.94.64.45 1.09-.88 1.84 1.13 1.13 1.81-.91 1.09.45.69 1.92h1.59l.63-1.94 1.11-.45 1.84.88 1.13-1.13-.92-1.81.47-1.09L14 8.75v.02zM7 11c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3z" }
//        historyIcon { shape = "M8 13H6V6h5v2H8v5zM7 1C4.81 1 2.87 2.02 1.59 3.59L0 2v4h4L2.5 4.5C3.55 3.17 5.17 2.3 7 2.3c3.14 0 5.7 2.56 5.7 5.7s-2.56 5.7-5.7 5.7A5.71 5.71 0 0 1 1.3 8c0-.34.03-.67.09-1H.08C.03 7.33 0 7.66 0 8c0 3.86 3.14 7 7 7s7-3.14 7-7-3.14-7-7-7z" }
//        branchIcon { shape = "M10 5c0-1.11-.89-2-2-2a1.993 1.993 0 0 0-1 3.72v.3c-.02.52-.23.98-.63 1.38-.4.4-.86.61-1.38.63-.83.02-1.48.16-2 .45V4.72a1.993 1.993 0 0 0-1-3.72C.88 1 0 1.89 0 3a2 2 0 0 0 1 1.72v6.56c-.59.35-1 .99-1 1.72 0 1.11.89 2 2 2 1.11 0 2-.89 2-2 0-.53-.2-1-.53-1.36.09-.06.48-.41.59-.47.25-.11.56-.17.94-.17 1.05-.05 1.95-.45 2.75-1.25S8.95 7.77 9 6.73h-.02C9.59 6.37 10 5.73 10 5zM2 1.8c.66 0 1.2.55 1.2 1.2 0 .65-.55 1.2-1.2 1.2C1.35 4.2.8 3.65.8 3c0-.65.55-1.2 1.2-1.2zm0 12.41c-.66 0-1.2-.55-1.2-1.2 0-.65.55-1.2 1.2-1.2.65 0 1.2.55 1.2 1.2 0 .65-.55 1.2-1.2 1.2zm6-8c-.66 0-1.2-.55-1.2-1.2 0-.65.55-1.2 1.2-1.2.65 0 1.2.55 1.2 1.2 0 .65-.55 1.2-1.2 1.2z" }
//        releasesIcon { shape = "M7.73 1.73C7.26 1.26 6.62 1 5.96 1H3.5C2.13 1 1 2.13 1 3.5v2.47c0 .66.27 1.3.73 1.77l6.06 6.06c.39.39 1.02.39 1.41 0l4.59-4.59a.996.996 0 0 0 0-1.41L7.73 1.73zM2.38 7.09c-.31-.3-.47-.7-.47-1.13V3.5c0-.88.72-1.59 1.59-1.59h2.47c.42 0 .83.16 1.13.47l6.14 6.13-4.73 4.73-6.13-6.15zM3.01 3h2v2H3V3h.01z" }
//        contributorsIcon { shape = "M16 12.999c0 .439-.45 1-1 1H7.995c-.539 0-.994-.447-.995-.999H1c-.54 0-1-.561-1-1 0-2.634 3-4 3-4s.229-.409 0-1c-.841-.621-1.058-.59-1-3 .058-2.419 1.367-3 2.5-3s2.442.58 2.5 3c.058 2.41-.159 2.379-1 3-.229.59 0 1 0 1s1.549.711 2.42 2.088C9.196 9.369 10 8.999 10 8.999s.229-.409 0-1c-.841-.62-1.058-.59-1-3 .058-2.419 1.367-3 2.5-3s2.437.581 2.495 3c.059 2.41-.158 2.38-1 3-.229.59 0 1 0 1s3.005 1.366 3.005 4" }
//        commentIcon { shape = "M14 1H2c-.55 0-1 .45-1 1v8c0 .55.45 1 1 1h2v3.5L7.5 11H14c.55 0 1-.45 1-1V2c0-.55-.45-1-1-1zm0 9H7l-2 2v-2H2V2h12v8z" }
//        starIcon { shape = "M14 6l-4.9-.64L7 1 4.9 5.36 0 6l3.6 3.26L2.67 14 7 11.67 11.33 14l-.93-4.74z" }
//        locationIcon { shape = "M6 0C2.69 0 0 2.5 0 5.5 0 10.02 6 16 6 16s6-5.98 6-10.5C12 2.5 9.31 0 6 0zm0 14.55C4.14 12.52 1 8.44 1 5.5 1 3.02 3.25 1 6 1c1.34 0 2.61.48 3.56 1.36.92.86 1.44 1.97 1.44 3.14 0 2.94-3.14 7.02-5 9.05zM8 5.5c0 1.11-.89 2-2 2-1.11 0-2-.89-2-2 0-1.11.89-2 2-2 1.11 0 2 .89 2 2z" }
//        linkIcon { shape = "M4 9h1v1H4c-1.5 0-3-1.69-3-3.5S2.55 3 4 3h4c1.45 0 3 1.69 3 3.5 0 1.41-.91 2.72-2 3.25V8.59c.58-.45 1-1.27 1-2.09C10 5.22 8.98 4 8 4H4c-.98 0-2 1.22-2 2.5S3 9 4 9zm9-3h-1v1h1c1 0 2 1.22 2 2.5S13.98 12 13 12H9c-.98 0-2-1.22-2-2.5 0-.83.42-1.64 1-2.09V6.25c-1.09.53-2 1.84-2 3.25C6 11.31 7.55 13 9 13h4c1.45 0 3-1.69 3-3.5S14.5 6 13 6z" }
//        clockIcon { shape = "M8 8h3v2H7c-.55 0-1-.45-1-1V4h2v4zM7 2.3c3.14 0 5.7 2.56 5.7 5.7s-2.56 5.7-5.7 5.7A5.71 5.71 0 0 1 1.3 8c0-3.14 2.56-5.7 5.7-5.7zM7 1C3.14 1 0 4.14 0 8s3.14 7 7 7 7-3.14 7-7-3.14-7-7-7z" }
//        crossIcon {
//            shape = "M7.48 8l3.75 3.75-1.48 1.48L6 9.48l-3.75 3.75-1.48-1.48L4.52 8 .77 4.25l1.48-1.48L6 6.52l3.75-3.75 1.48 1.48z"
//            cursor = HAND
//        }
//        openIssueIcon {
//            shape = "M7 2.3c3.14 0 5.7 2.56 5.7 5.7s-2.56 5.7-5.7 5.7A5.71 5.71 0 0 1 1.3 8c0-3.14 2.56-5.7 5.7-5.7zM7 1C3.14 1 0 4.14 0 8s3.14 7 7 7 7-3.14 7-7-3.14-7-7-7zm1 3H6v5h2V4zm0 6H6v2h2v-2z"
//            backgroundColor += c("#6cc644")
//        }
//        logoIcon {
//            backgroundColor += c("#333")
//            shape = "M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0 0 16 8c0-4.42-3.58-8-8-8z"
//        }
//
//        hyperlink {
//            textFill = linkColor
//            padding = box(0.px)
//            s(armed, visited, hover and armed) {
//                unsafe("-fx-text-fill", raw("-fx-accent"))
//            }
//        }

        hoverable {
            and(hover) {
                translateX = 1.px
                translateY = 1.px
                // TODO: Find something less hideous
                effect = DropShadow(BlurType.GAUSSIAN, Color.web("#0093ff"), 12.0, 0.2, 0.0, 1.0)
            }
        }

        card {
            borderColor = multi(box(Color.BLACK))
            borderRadius = multi(box(10.px))
            backgroundColor = multi(Color.LIGHTGRAY)
            backgroundRadius = multi(box(10.px))
        }

        jfxButton {
            and(hover) {
                backgroundColor = multi(Color.LIGHTBLUE)
            }
        }

        extraButton {
            prefWidth = 160.px
            contentDisplay = ContentDisplay.RIGHT
            alignment = Pos.CENTER_RIGHT
            graphicTextGap = 6.px
        }

        acceptButton {
            prefWidth = 100.px
            prefHeight = 40.px
            and(hover) {
                backgroundColor = multi(Color.LIMEGREEN)
            }
        }

        cancelButton {
            prefWidth = 100.px
            prefHeight = 40.px
            and(hover) {
                backgroundColor = multi(Color.INDIANRED)
            }
        }

        deleteButton {
            and(hover) {
                backgroundColor = multi(Color.RED)
            }
        }

        popoverMenu {
            spacing = 5.px
            padding = box(5.px)
        }
    }
}