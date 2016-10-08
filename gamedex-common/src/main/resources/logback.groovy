import ch.qos.logback.classic.Level
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.jul.LevelChangePropagator
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy

import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import static ch.qos.logback.classic.Level.DEBUG
import static ch.qos.logback.classic.Level.INFO

log = [
    ROOT: INFO,
    CONSOLE: INFO,
    DATA_PROVIDER: DEBUG,
    PERSISTENCE: DEBUG,
    SQL: DEBUG,
]

scan("30 seconds")

// Bridge to java.util.log ( should update it configuration - fix performance issue)
context = new LevelChangePropagator()
context.resetJUL = true

LOG_DIR = Paths.get("logs").toAbsolutePath().toString() + "/"


/***************************************************************
 *                       Aux methods                           *
 ***************************************************************/
class LoggerConfig {
    String pattern = "%date{dd-MM-yyyy HH:mm:ss.SSS} [%thread] %-5level %logger{0} - %msg%n"
    Level level = null
    String maxFileSize = "5mb"
    boolean transitive = false
}
DEFAULT_CONFIG = new LoggerConfig()

consoleAppender = "console"

nowStr = DateTimeFormatter.ofPattern("yyyy_MM_dd__HH_mm").format(LocalDateTime.now())
mainAppender = "gameDex__" + nowStr

def createLogger(String name, Map loggers, LoggerConfig config = DEFAULT_CONFIG) {
    if (!name || !loggers) {
        throw new RuntimeException("Missing name or loggers!")
    }

    // Configure the logger
    rollingFileAppender(name, config)

    List appenders = [name, mainAppender, consoleAppender]

    loggers.each { logPackage, logLevel ->
        println "LOGBACK:    * $logPackage : $logLevel"
        logPackage == "root" ?
            root(logLevel, appenders) :
            logger(logPackage, logLevel, appenders, config.transitive)
    }
}

def rollingFileAppender(String name, LoggerConfig config) {
    final String logFile = "${LOG_DIR}${name}.log"
    final String logFileZip = "${LOG_DIR}${name}.%i.log.zip"

    appender(name, RollingFileAppender) {
        file = logFile
        encoder(PatternLayoutEncoder) {
            pattern = config.pattern
        }
        rollingPolicy(FixedWindowRollingPolicy) {
            fileNamePattern = logFileZip
            minIndex = 1
            maxIndex = 9
        }
        triggeringPolicy(SizeBasedTriggeringPolicy) {
            maxFileSize = config.maxFileSize
        }
        if (config.level != null) {
            filter(ThresholdFilter) {
                level = config.level
            }
        }
        append = true
    }

    println "LOGBACK: $logFile ${if(config.level != null) "[${config.level}]" else ""}"
}

/***************************************************************
 *                          Loggers                            *
 ***************************************************************/

appender(consoleAppender, ConsoleAppender) {
    filter(ThresholdFilter) {
        level = log.CONSOLE
    }
    encoder(PatternLayoutEncoder) {
        pattern = this.DEFAULT_CONFIG.pattern
    }
}

createLogger(
    mainAppender,
    [
        "root": log.ROOT,
    ],
    new LoggerConfig(level: log.ROOT)
)

createLogger(
    "provider",
    [
        "com.gitlab.ykrasik.gamedex.provider": log.DATA_PROVIDER,
    ]
)

createLogger(
    "persistence",
    [
        "com.gitlab.ykrasik.gamedex.persistence": log.PERSISTENCE,
        "org.flywaydb": log.SQL,
        "Exposed": log.SQL
    ]
)

println "Logging initialized."