package org.aksw.limes.core.controller;

import static org.fusesource.jansi.Ansi.ansi;
import static org.fusesource.jansi.Ansi.Color.RED;

import org.aksw.limes.core.exceptions.UnsupportedMLImplementationException;
import org.aksw.limes.core.execution.engine.ExecutionEngineFactory;
import org.aksw.limes.core.execution.planning.planner.ExecutionPlannerFactory;
import org.aksw.limes.core.execution.rewriter.RewriterFactory;
//import org.aksw.limes.core.gui.LimesGUI;
import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.cache.HybridCache;
import org.aksw.limes.core.io.cache.MemoryCache;
import org.aksw.limes.core.io.config.Configuration;
import org.aksw.limes.core.io.config.reader.AConfigurationReader;
import org.aksw.limes.core.io.config.reader.rdf.RDFConfigurationReader;
import org.aksw.limes.core.io.config.reader.xml.XMLConfigurationReader;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.serializer.ISerializer;
import org.aksw.limes.core.io.serializer.SerializerFactory;
import org.aksw.limes.core.measures.mapper.MappingOperations;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.andrewoma.dexx.collection.HashMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.function.Function;

/**
 * This is the default LIMES Controller used to run the software as CLI.
 *
 * @author Kevin Dreßler
 */
public class Controller {

	private static final String DEFAULT_LOGGING_PATH = "limes.log";
	private static final int MAX_ITERATIONS_NUMBER = 1;
	private static Logger logger = null;
	private static Options options = getOptions();

	/**
	 * Take configuration file as argument and run the specified linking task.
	 *
	 * @param args
	 *            Command line arguments
	 */
	public static void main(String[] args) {

		Controller ctr = new Controller();
		ctr.run(args);

	}

	public void run(String[] args) {

		// I. Configure Logger
		CommandLine cmd = parseCommandLine(args);
		System.setProperty("logFilename", cmd.hasOption('o') ? cmd.getOptionValue("o") : DEFAULT_LOGGING_PATH);
		((org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false)).reconfigure();
		logger = LoggerFactory.getLogger(Controller.class);
		// II. Digest Options

		// III. Has Arguments?
		if (cmd.getArgs().length < 1) {
			logger.error("Error:\n\t Please specify a configuration file to use!");
			printHelp();
			System.exit(1);
		}

		Configuration config = getConfig(cmd);
		config.setAcceptanceThreshold(0.1);

		//Try to create the logging file
		try {
			File f = new File(config.log_file);
			f.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (int i = 0; i < config.repetitions; i++) {


			try {
				File file = new File(config.log_file);
				file.createNewFile();
				
				String outString = "\n\n\n --- ( " + args[0].split("/")[args[0].split("/").length - 1]
						+ " | " + (i+1) + " ) ---\n";
				Files.write(Paths.get(config.log_file), outString.getBytes(),
						StandardOpenOption.APPEND);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				File cachefolder = new File("cache/");
				FileUtils.deleteDirectory(cachefolder);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			GoldStandardBatchReader gsbr = new GoldStandardBatchReader(config.gold_file, config.log_file);
			ResultMappings mappings = getMapping(config, gsbr);
			writeResults(mappings, config);

		}

	}

	private static CommandLine parseCommandLine(String[] args) {
		CommandLineParser parser = new BasicParser();
		CommandLine cl = null;
		try {
			cl = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println(ansi().fg(RED).a("Parsing error:\n\t" + e.getMessage()).reset());
			printHelp();
			System.exit(-1);
		}
		return cl;
	}

	public static Configuration getConfig(CommandLine cmd) {
		if (logger == null)
			logger = LoggerFactory.getLogger(Controller.class);
		// 1. Determine appropriate ConfigurationReader
		String format = "xml";
		String fileNameOrUri = cmd.getArgs()[0];
		if (cmd.hasOption('f')) {
			format = cmd.getOptionValue("f").toLowerCase();
		} else if (fileNameOrUri.endsWith(".nt") || fileNameOrUri.endsWith(".ttl") || fileNameOrUri.endsWith(".n3")
				|| fileNameOrUri.endsWith(".rdf")) {
			format = "rdf";
		}

		AConfigurationReader reader = null;
		switch (format) {
		case "xml":
			reader = new XMLConfigurationReader(fileNameOrUri);
			break;
		case "rdf":
			reader = new RDFConfigurationReader(fileNameOrUri);
			break;
		default:
			logger.error("Error:\n\t Not a valid format: \"" + format + "\"!");
			printHelp();
			System.exit(1);
		}

		// 2. Read configuration

		return reader.read();
	}

	/**
	 * Execute LIMES
	 *
	 * @param config
	 *            LIMES configuration object
	 *
	 * @return Instance of ResultMapping
	 *
	 */
	public static ResultMappings getMapping(Configuration config, GoldStandardBatchReader gsbr) {
		return getMapping(config, -1, gsbr);
	}

	static ResultMappings getMapping(Configuration config, int limit, GoldStandardBatchReader gsbr) {
		if (logger == null)
			logger = LoggerFactory.getLogger(Controller.class);
		AMapping results = null;

		// 3. Fill Caches
		ACache sourceCache = HybridCache.getData(config.getSourceInfo());
		ACache targetCache = HybridCache.getData(config.getTargetInfo());
		if (limit > 0) {
			Function<ACache, ACache> getSubCache = c -> {
				ACache reducedCache = new MemoryCache();
				c.getAllInstances().subList(0, limit).forEach(reducedCache::addInstance);
				return reducedCache;
			};
			sourceCache = getSubCache.apply(sourceCache);
			targetCache = getSubCache.apply(targetCache);
		}

		// 4. Machine Learning or Planning
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		boolean isAlgorithm = !config.getMlAlgorithmName().equals("");
		if (isAlgorithm) {
			try {
				results = MLPipeline.execute(sourceCache, targetCache, config, config.getMlAlgorithmName(),

						config.getMlImplementationType(), config.getMlAlgorithmParameters(),
						config.getTrainingDataFile(), config.getMlPseudoFMeasure(), MAX_ITERATIONS_NUMBER,
						config.maxIterations, gsbr);
			} catch (UnsupportedMLImplementationException e) {
				e.printStackTrace();
			}
		} else {
			results = LSPipeline.execute(sourceCache, targetCache, config.getMetricExpression(),
					config.getVerificationThreshold(), config.getSourceInfo().getVar(), config.getTargetInfo().getVar(),
					RewriterFactory.getRewriterType(config.getExecutionRewriter()),
					ExecutionPlannerFactory.getExecutionPlannerType(config.getExecutionPlanner()),
					ExecutionEngineFactory.getExecutionEngineType(config.getExecutionEngine()));
		}
		logger.info("Mapping task finished in " + stopWatch.getTime() + " ms");
		assert results != null;
		AMapping acceptanceMapping = results.getSubMap(config.getAcceptanceThreshold());
		AMapping verificationMapping = MappingOperations.difference(results, acceptanceMapping);
		logger.info("Mapping size: " + acceptanceMapping.size() + " (accepted) + " + verificationMapping.size()
				+ " (need verification) = " + results.size() + " (total)");
		return new ResultMappings(verificationMapping, acceptanceMapping);
	}

	private static void writeResults(ResultMappings mappings, Configuration config) {
		String outputFormat = config.getOutputFormat();
		ISerializer output = SerializerFactory.createSerializer(outputFormat);
		output.setPrefixes(config.getPrefixes());
		output.writeToFile(mappings.getVerificationMapping(), config.getVerificationRelation(),
				config.getVerificationFile());
		output.writeToFile(mappings.getAcceptanceMapping(), config.getAcceptanceRelation(), config.getAcceptanceFile());
	}

	/**
	 * Print the usage text
	 */
	private static void printHelp() {
		new HelpFormatter().printHelp("limes [OPTION]... <config_file_or_uri>", options);
	}

	/**
	 * Get available options for CLI
	 *
	 * @return Options object containing all available command line options
	 */
	private static Options getOptions() {
		Options options = new Options();
		// options.addOption("g", false, "Run LIMES GUI");
		options.addOption("s", false, "Run LIMES Server");
		options.addOption("h", false, "Show this help");
		options.addOption("o", true, "Set path of log file. Default is 'limes.log'");
		options.addOption("f", true, "Optionally configure format of <config_file_or_uri>, either \"xml\" (default) or "
				+ "\"rdf\". If not specified, LIMES tries to infer the format from file ending.");
		options.addOption("p", true,
				"Optionally configure HTTP server port. Only effective if -s is specified. Default port is 8080.");
		options.addOption("l", true,
				"Optionally configure a limit for source and target resources processed by LIMES Server. Only effective if -s is specified. Default value is -1 (no limit).");
		// options.addOption("v", false, "Verbose run");
		return options;
	}

}
