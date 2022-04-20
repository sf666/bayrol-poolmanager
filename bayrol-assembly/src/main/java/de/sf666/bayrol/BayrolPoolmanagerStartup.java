package de.sf666.bayrol;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import de.sf666.bayrol.api.BayrolHttpConnector;
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

    public static void main(String[] args)
    {
        BayrolPoolmanagerStartup.args = args;

        options.addOption("u", "user", true, "this is the login user to bayrol manager web");
        options.addOption("p", "pass", true, "password to bayrol manager web");
        options.addOption("h", "httpPort", true, "http port the rest service listens on, defaults to 32176 [OPTIONAL]");
        options.addOption("s", "scheduler", true, "bayrol website update interval, defaults to 120.000 (2 minutes) [OPTIONAL]");

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

            if (!cmd.hasOption("h"))
            {
                config.httpPort = parseInt(cmd.getOptionValue("h", "32176"));
            }

            if (!cmd.hasOption("s"))
            {
                config.scheduleInterval = parseInt(cmd.getOptionValue("s", "120000"));
            }

            BayrolPoolmanagerStartup.context = SpringApplication.run(BayrolPoolmanagerStartup.class, args);
        }
        catch (Exception e)
        {
            System.err.println("Error parsing command line options : " + e.getLocalizedMessage());
            showHelpAndExit();
        }
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

    @Bean
    public BayrolHttpConnector createBayrolBridge()
    {
        return new BayrolHttpConnector(config.username, config.password);
    }

    private static void showHelpAndExit()
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar de.sf666.bayrol", options);
        System.exit(0);
    }

}
