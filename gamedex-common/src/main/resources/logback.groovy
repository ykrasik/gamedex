import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.jul.LevelChangePropagator
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy

import java.nio.file.Paths

import static ch.qos.logback.classic.Level.DEBUG
import static ch.qos.logback.classic.Level.INFO

LOG_LEVELS = [
    ROOT: INFO,
    CONSOLE: INFO,
    GAME_INFO_SERVICE: INFO,
    PERSISTENCE: DEBUG,
    SQL: INFO,
]

scan("30 seconds")

// Bridge to java.util.log ( should update it configuration - fix performance issue)
context = new LevelChangePropagator()
context.resetJUL = true

LOG_DIR = Paths.get("logs").toAbsolutePath().toString() + "/"
DEFAULT_PATTERN = "%date{dd-MM-yyyy HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

systemAppenders = []

/***************************************************************
 *                       Aux methods                           *
 ***************************************************************/
def rollingFileAppender(String name, def filterLevel = null, def _pattern = DEFAULT_PATTERN) {
    String appenderFile = "${LOG_DIR}${name}.log"
    String appenderFileZip = "${LOG_DIR}${name}.%i.log.zip"

    println "LOGBACK: ${appenderFile}"

    appender(name, RollingFileAppender) {
        file = appenderFile
        encoder(PatternLayoutEncoder) {
            pattern = _pattern
        }
        rollingPolicy(FixedWindowRollingPolicy) {
            fileNamePattern = appenderFileZip
            minIndex = 1
            maxIndex = 9
        }
        triggeringPolicy(SizeBasedTriggeringPolicy) {
            maxFileSize = "5MB"
        }
        if (filterLevel != null) {
            filter(ThresholdFilter) {
                level = filterLevel
            }
        }
        append = true
    }
    return appenderFile
}

def createLogger(String name, Map loggers, Map args = [:]) {
    if (!name || !loggers) {
        throw new RuntimeException("Missing name or loggers!")
    }
    boolean includeSystemAppenders = args.includeSystemAppenders != null ? args.includeSystemAppenders : true
    def filterLevel = args.filterLevel ?: null
    boolean transitive = args.transitive != null ? args.transitive : false

    // Configure the logger
    rollingFileAppender(name, filterLevel)

    List appenders = [name]
    if (includeSystemAppenders) {
        appenders += systemAppenders
    }

    loggers.each { logPackage, logLevel ->
        println "LOGBACK:    * $logPackage : $logLevel"
        logPackage == "root" ?
            root(logLevel, appenders) :
            logger(logPackage, logLevel, appenders, transitive)
    }
}

/***************************************************************
 *                          Loggers                            *
 ***************************************************************/

// Console
def APPENDER_CONSOLE = "console"
appender(APPENDER_CONSOLE, ConsoleAppender) {
    filter(ThresholdFilter) {
        level = INFO
    }
    encoder(PatternLayoutEncoder) {
        pattern = this.DEFAULT_PATTERN
    }
}
systemAppenders += APPENDER_CONSOLE

// Root
createLogger(
    "gameDex",
    [
        "root": LOG_LEVELS.ROOT,
    ]
)

// Game info service
createLogger(
    "provider",
    [
        "com.github.ykrasik.gamedex.provider": LOG_LEVELS.GAME_INFO_SERVICE,
    ]
)

// SQL
createLogger(
    "persistence",
    [
        "com.github.ykrasik.gamedex.persistence": LOG_LEVELS.PERSISTENCE,
        "com.j256.ormlite": LOG_LEVELS.SQL,
    ],
    [
        includeSystemAppenders: false
    ]
)

println "Logging initialized."