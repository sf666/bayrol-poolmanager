package de.sf666.bayrol;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import de.sf666.bayrol.api.BayrolHttpConnector;
import de.sf666.bayrol.bridge.BayrolBridge;
import de.sf666.bayrol.bridge.BayrolConfig;

@SpringBootApplication(scanBasePackages = "de.sf666.bayrol")
@EnableScheduling
public class BayrolPoolmanagerStartup
{
    private static ConfigurableApplicationContext context;
    private static String[] args;

    private static Options options = new Options();
    private static CommandLine cmd = null;
    private static BayrolConfig config = new BayrolConfig();

    private static LoggerContext loggingCtx = null;
    private static BuiltConfiguration loggingConfiguration = null;

    public static void main(String[] args)
    {
        BayrolPoolmanagerStartup.args = args;

        options.addOption("u", "user", true, "this is the login user to bayrol manager web");
        options.addOption("p", "pass", true, "password to bayrol manager web");
        options.addOption("h", "httpPort", true, "http port the rest service listens on, defaults to 32176 [OPTIONAL]");
        options.addOption("s", "scheduler", true, "bayrol website update interval, defaults to 120.000 (2 minutes) [OPTIONAL]");
        options.addOption("l", "logfile", true, "Logfile location, defaults to 'bayrol.log'");
        options.addOption("ll", "logLevel", true, "Log-Level, defaults to 'INFO'. Available level are : FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL");

        CommandLineParser parser = new DefaultParser();

        try
        {
            cmd = parser.parse(options, args);
            if (!cmd.hasOption("u"))
            {
                showHelpAndExit();
            }
            if (!cmd.hasOption("p"))
            {
                showHelpAndExit();
            }

            config.username = cmd.getOptionValue("u");
            config.password = cmd.getOptionValue("p");

            config.httpPort = parseInt(cmd.getOptionValue("h", "32176"));
            config.scheduleInterval = parseInt(cmd.getOptionValue("s", "120000"));
            config.logfile = cmd.getOptionValue("l", "bayrol.log");
            config.logLevel = cmd.getOptionValue("ll", "INFO");

            BayrolPoolmanagerStartup.context = SpringApplication.run(BayrolPoolmanagerStartup.class, args);
        }
        catch (Exception e)
        {
            System.err.println("Error parsing command line options : " + e.getLocalizedMessage());
            showHelpAndExit();
        }
    }

    private static void configureLogger()
    {
        String rollingAppenderName = "rolling";
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        // Rolling file configuration
        AppenderComponentBuilder rollingFile = builder.newAppender(rollingAppenderName, "RollingFile");
        rollingFile.addAttribute("fileName", config.logfile);
        rollingFile.addAttribute("filePattern", "rolling-%d{MM-dd-yy}.log.gz");

        ComponentBuilder triggeringPolicies = builder.newComponent("Policies").addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", "100M"));
        rollingFile.addComponent(triggeringPolicies);

        LayoutComponentBuilder standard = builder.newLayout("PatternLayout");
        standard.addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable");
        rollingFile.add(standard);
        builder.add(rollingFile);

        // root logger configuration
        RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.toLevel(config.logLevel, Level.INFO));
        rootLogger.add(builder.newAppenderRef(rollingAppenderName));
        builder.add(rootLogger);

        // Init Logger
        loggingConfiguration = builder.build();
        loggingCtx = Configurator.initialize(loggingConfiguration);
        loggingCtx.reconfigure(loggingConfiguration);
        loggingCtx.start();
    }

    private static int parseInt(String optionValue)
    {
        return Integer.parseInt(optionValue);
    }

    @Bean
    public BayrolConfig createBayrolConfig()
    {
        return config;
    }

    @Autowired
    @Bean
    BayrolBridge createBridge(BayrolHttpConnector bayrol)
    {
        return new BayrolBridge(bayrol);
    }

    @Bean
    public BayrolHttpConnector createBayrolBridge()
    {
        return new BayrolHttpConnector(config.username, config.password);
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event)
    {
        // Hack, because Spring overwrites log4j config
        configureLogger();
        Logger log = LoggerFactory.getLogger(BayrolPoolmanagerStartup.class.getName());
        log.debug("debug");
        log.info("info");
        log.error("error");

    }

    public static void shutdownApplication(int code)
    {
        int exitCode = SpringApplication.exit(context, new ExitCodeGenerator()
        {
            @Override
            public int getExitCode()
            {
                return code;
            }
        });

        System.exit(exitCode);
    }

    private static void showHelpAndExit()
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar de.sf666.bayrol", options);
        System.exit(0);
    }

}
