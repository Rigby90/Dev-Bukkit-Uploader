package com.onarandombox.dev;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class Main {
    public static void main(String[] args) {
        OptionParser parser = new OptionParser() {
            {
                acceptsAll(asList("?", "help"), "Show the help");

                acceptsAll(asList("a", "apikeyfile"), "File which contains the API Key to use")
                        .withRequiredArg()
                        .ofType(File.class)
                        .describedAs("FILE");

                acceptsAll(asList("k", "apikey"), "API Key to use")
                        .withRequiredArg()
                        .ofType(String.class)
                        .defaultsTo("")
                        .describedAs("API Key");

                acceptsAll(asList("g", "game"), "Game to upload a file for.")
                        .withRequiredArg()
                        .ofType(String.class)
                        .defaultsTo("minecraft")
                        .describedAs("Minecraft");

                acceptsAll(asList("p", "project"), "Project slug to upload to, more often than not this is the project name.")
                        .withRequiredArg()
                        .ofType(String.class)
                        .defaultsTo("")
                        .describedAs("Project Slug");

                acceptsAll(asList("v", "version"), "Version for the Upload")
                        .withRequiredArg()
                        .ofType(String.class)
                        .defaultsTo("")
                        .describedAs("Upload Version");

                acceptsAll(asList("t", "type"), "Upload type - 'r'= Release, 'a' = Alpha & 'b' = Beta")
                        .withRequiredArg()
                        .ofType(String.class)
                        .defaultsTo("r")
                        .describedAs("Upload Type");

                acceptsAll(asList("l", "changelog"), "Changelog for this Upload")
                        .withRequiredArg()
                        .ofType(String.class)
                        .defaultsTo("")
                        .describedAs("Upload Changelog");

                acceptsAll(asList("m", "markup"), "Changelog Markup Type")
                        .withRequiredArg()
                        .ofType(String.class)
                        .defaultsTo("plain")
                        .describedAs("Upload Changelog Markup");

                acceptsAll(asList("c", "caveats"), "Caveats for this Upload")
                        .withRequiredArg()
                        .ofType(String.class)
                        .defaultsTo("")
                        .describedAs("Upload Caveats");

                acceptsAll(asList("caveatsmarkup"), "Caveats Markup Type")
                        .withRequiredArg()
                        .ofType(String.class)
                        .defaultsTo("plain")
                        .describedAs("Upload Caveats Markup");

                acceptsAll(asList("f", "file"), "File to Upload")
                        .withRequiredArg()
                        .ofType(File.class)
                        .describedAs("File to Upload");
            }
        };

        OptionSet options = null;

        try {
            options = parser.parse(args);
        } catch (joptsimple.OptionException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage());
        }

        if ((options == null) || (options.has("?"))) {
            try {
                parser.printHelpOn(System.out);
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                DevBukkitUploader.main(options);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static List<String> asList(String... params) {
        return Arrays.asList(params);
    }
}
