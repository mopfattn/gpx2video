import com.charleskorn.kaml.Yaml
import de.pfattner.gpx2video.ImageGenerator
import de.pfattner.gpx2video.TrackLoader
import de.pfattner.gpx2video.config.ColorParser
import de.pfattner.gpx2video.config.Config
import de.pfattner.gpx2video.config.ConfigUtil
import de.pfattner.gpx2video.config.ConfigUtil.checkConfig
import de.pfattner.gpx2video.map.MapViewLatLng
import org.apache.commons.cli.*
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.util.logging.ConsoleHandler
import java.util.logging.LogManager
import java.util.logging.Logger
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

private val LOGGER = Logger.getLogger("gpx2video")

private fun initLogging(verbose: Boolean) {
    val sb = StringBuilder()

    if (verbose) {
        sb.append(".level = ALL\n")
    } else {
        sb.append(".level = INFO\n")
    }
    sb.append("java.util.logging.SimpleFormatter.format = %1\$tF %1\$tT %3\$s [%4\$s] %5\$s %6\$s%n\n")
    sb.append("handlers = ${ConsoleHandler::class.java.name}\n")

    runCatching {
        ByteArrayInputStream(sb.toString().toByteArray(charset("UTF-8"))).use { input ->
            LogManager.getLogManager().readConfiguration(input)
        }
    }
}

private fun printHelp(options: Options) {
    HelpFormatter().printHelp("java -jar gpx2video.jar [options] <config.yml>", options)
}

private fun loadConfig(options: Options, commandLine: CommandLine): Config? {
    // Check whether the config file exists
    val configFilename = commandLine.argList.lastOrNull()
    if (configFilename == null) {
        LOGGER.severe("no config file specified")
        printHelp(options)
        return null
    }

    val configFile = File(configFilename)
    if (!configFile.exists()) {
        LOGGER.severe("config file $configFile not found")
        return null
    }

    return runCatching {
        Yaml.default.decodeFromStream(Config.serializer(), FileInputStream(configFile))
    }.onFailure {
        LOGGER.severe("Error reading config: ${it.message}")
    }.getOrNull()
}

private fun imageGeneratorOptions(config: Config): ImageGenerator.Options {
    val defaultTmpDir = System.getProperty("java.io.tmpdir") + File.separator + "gpx2video"

    val theme = ImageGenerator.Theme(
        trackColor = ColorParser.parseColor(config.video.highlightedColor) ?: ImageGenerator.Theme.DEFAULT.trackColor,
        trackFadedColor = ColorParser.parseColor(config.video.fadedColor) ?: ImageGenerator.Theme.DEFAULT.trackFadedColor,
        darkMap = config.map.invertColors,
    )
    return ImageGenerator.Options(
        size = ImageGenerator.Size(
            width = config.video.width,
            height = config.video.height,
            trackFadedWidth = config.video.fadedWidth.toFloat(),
            trackHighlightWidth = config.video.highlightedWidth.toFloat(),
        ),
        center = MapViewLatLng(config.map.latitude, config.map.longitude),
        zoomLevel = config.map.zoom,
        outputDirectory = File(config.tmpDir ?: defaultTmpDir),
        theme = theme,
        maxTrackDuration = config.video.maxDurationInSeconds.seconds,
        highlightDuration = config.video.highlightDurationInSeconds.seconds,
        stepForwardBy = config.video.stepInSeconds.seconds,
        runCommand = config.runCommand,
    )
}

private fun trackLoaderOptions(config: Config): TrackLoader.Options {
    val regex = if (config.inputFilterRegex != null) Regex(config.inputFilterRegex) else null
    return TrackLoader.Options(sourceFile = File(ConfigUtil.resolveVariables(config.input)), regex)
}

fun main(args: Array<String>) {
    val helpOption = Option("h", "help", false, "print this message")
    val debugOption = Option("v", "verbose", false, "verbose logging")

    val options = Options()
    options.addOption(helpOption)
    options.addOption(debugOption)

    val commandLine = runCatching {
        DefaultParser().parse(options, args)
    }.onFailure {
        printHelp(options)
    }.getOrElse {
        exitProcess(1)
    }

    initLogging(commandLine.hasOption(debugOption))

    val config = loadConfig(options, commandLine) ?: exitProcess(1)

    // Do some sanity checks on the config
    if (!checkConfig(config)) {
        exitProcess(1)
    }

    val tracks = TrackLoader(trackLoaderOptions(config)).loadTracks()
    if (tracks.isEmpty()) {
        LOGGER.severe("No tracks loaded")
        exitProcess(1)
    }

    ImageGenerator(imageGeneratorOptions(config)).generateImagesAndRunCommand(tracks)
}