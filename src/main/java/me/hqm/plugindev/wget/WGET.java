// The MIT License (MIT)
//
// Copyright © 2014 Alexander Chauncey (aka HmmmQuestionMark)
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of
// this software and associated documentation files (the "Software"), to deal in
// the Software without restriction, including without limitation the rights to
// use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// the Software, and to permit persons to whom the Software is furnished to do so,
// subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package me.hqm.plugindev.wget;

import mkremins.fanciful.FancyMessage;
import net.minecraft.util.org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * wget for bukkit plugins.
 *
 * @author HmmmQuestionMark
 */
public class WGET extends JavaPlugin implements CommandExecutor {
    // -- CONSTANTS -- //

    private static final String PREFIX = ChatColor.YELLOW + "[wget] ";
    private static final ConcurrentMap<String, URL> CACHE = new ConcurrentHashMap<>();

    // -- BUKKIT METHODS -- //

    /**
     * Standard Bukkit enable method.
     */
    @Override
    public void onEnable() {
        getCommand("wget").setExecutor(this);
    }

    /**
     * Standard Bukkit command executor.
     * @param sender The command sender
     * @param command The command being sent
     * @param label The label/alias being used
     * @param args The arguments following the command
     * @return The command ran successfully
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check for correct arg length
        if (args.length == 1) {
            // make a valid url when possible
            String input = args[0];
            if (!input.startsWith("http://") && !input.startsWith("https://")) {
                input = ("http://" + input);
            }

            // Attempt to get a valid url
            URL url = getUrl(input);

            // Check for a valid url
            if (url != null) {
                // If sender is the console, skip verification
                if (sender instanceof ConsoleCommandSender) {
                    downloadTask(sender, url);
                } else {
                    // Add url to cache before asking for verification
                    Player player = (Player) sender;
                    CACHE.putIfAbsent(player.getName(), url);

                    // Send cool click text verification
                    FancyMessage message = new FancyMessage("Are you sure? ");
                    message.color(ChatColor.YELLOW).
                            then("Yes").
                            color(ChatColor.DARK_GREEN).
                            command("/wget " + player.getName() + " yes").
                            tooltip("Download " + fileName(url) + "?").
                            then(" or ").
                            color(ChatColor.YELLOW).
                            then("No").
                            color(ChatColor.RED).
                            command("/wget " + sender.getName() + " no").
                            tooltip("Cancel?").
                            send(player);
                }
            }
        }

        // Verification command
        if (args.length == 2) {
            String name = args[0];
            // If verified correctly start the download task
            if ("yes".equalsIgnoreCase(args[1])) {
                downloadTask(sender, CACHE.get(name));
            } else {
                // Cancel download and alert the sender.
                CACHE.remove(name);
                sender.sendMessage(PREFIX + "Download cancelled.");
            }
        }
        return true;
    }

    // -- PRIVATE HELPER/UTIL METHODS -- //

    /**
     * Get a URL from an input string.
     * @param input URL String
     * @return Valid URL
     */
    private URL getUrl(String input) {
        // We only accept jar or zip files
        if (input.endsWith(".jar") || input.endsWith(".zip")) {
            // Try to create the object, and test the connection
            try {
                URI uri = new URI(input);
                URL url = uri.toURL();
                URLConnection conn = url.openConnection();
                conn.connect();
                // Return the url
                return url;
            } catch (IOException | URISyntaxException ignored) {
            }
        }
        // Failed, return null
        return null;
    }

    /**
     * Get the file name from the URL.
     * @param url Valid URL
     * @return Filename
     */
    private String fileName(URL url) {
        // Split up the url by each '/'
        String[] fileNameParts = url.toString().split("/");
        // Return the last section, removing duplicate periods
        return fileNameParts[fileNameParts.length - 1].replace("..", ".");
    }

    /**
     * Create and start a download (async) task.
     * @param sender The sender who started the task
     * @param url Valid URL
     */
    private void downloadTask(final CommandSender sender, final URL url) {
        // Make sure the URL still exists
        if (url == null) {
            sender.sendMessage(PREFIX + "The download is cancelled, please start over.");
        } else {
            // Alert the sender the download has begun
            sender.sendMessage(PREFIX + "Downloading...");

            // Create and schedule the new async task
            Bukkit.getScheduler().scheduleAsyncDelayedTask(this, new Runnable() {
                @Override
                public void run() {
                    // Download the file
                    if (download(url)) {
                        // If the sender is the console just let it know the download is complete
                        if (sender instanceof ConsoleCommandSender) {
                            sender.sendMessage(PREFIX + "Download complete, reload for update.");
                        } else {
                            // Send the player a fancy message allowing them to click to reload
                            Player player = (Player) sender;
                            FancyMessage message = new FancyMessage("[wget] Download complete, ");
                            message.color(ChatColor.YELLOW).
                                    then("reload for update").
                                    style(ChatColor.UNDERLINE).
                                    color(ChatColor.YELLOW).
                                    command("/reload").
                                    tooltip("Reload the server?").
                                    then(".").
                                    color(ChatColor.YELLOW).
                                    send(player);
                        }
                    } else {
                        // Oops, something went wrong during download
                        sender.sendMessage(PREFIX + ChatColor.RED + "ERR: Could not finish downloading.");
                    }
                }
            });
        }
    }

    /**
     * Download a URL (jar or zip) to a file
     * @param url Valid URL
     * @return Success or failure
     */
    private boolean download(URL url) {
        // The plugin folder path
        String path = getFile().getParentFile().getPath();

        // Wrap everything in a try/catch
        try {
            // Get the filename from the url
            String fileName = fileName(url);

            // Create a new input stream and output file
            InputStream in = url.openStream();
            File outFile = new File(path + "/" + fileName);

            // If the file already exists, delete it
            if (outFile.exists()) {
                outFile.delete();
            }

            // Create the output stream and download the file
            FileOutputStream out = new FileOutputStream(outFile);
            IOUtils.copy(in, out);

            // Close the streams
            in.close();
            out.close();

            // If downloaded file is a zip file...
            if (fileName.endsWith(".zip")) {
                // Declare a new input stream outside of a try/catch
                ZipInputStream zis = null;
                try {
                    // Define the input stream
                    zis = new ZipInputStream(new FileInputStream(outFile));

                    // Decalre a zip entry for the while loop
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        // Make a new file object for the entiry
                        File entryFile = new File(path, entry.getName());

                        // If it is a directory and doesn't already exist, create the new directory
                        if (entry.isDirectory() && !entryFile.exists()) {
                            entryFile.mkdirs();
                        } else {
                            // Make sure all folders exist
                            if (entryFile.getParentFile() != null && !entryFile.getParentFile().exists()) {
                                entryFile.getParentFile().mkdirs();
                            }

                            // Create file on disk
                            if (!entryFile.exists()) {
                                entryFile.createNewFile();
                            }

                            // Write data to file from zip.
                            OutputStream os = null;
                            try {
                                os = new FileOutputStream(entryFile);
                                IOUtils.copy(zis, os);
                            } finally {
                                // Silently close the output stream
                                IOUtils.closeQuietly(os);
                            }
                        }
                    }
                } finally {
                    // Always close streams
                    IOUtils.closeQuietly(zis);
                }

                // Delete the zip file
                outFile.delete();
            }

            // Return success
            return true;
        } catch (NullPointerException | IOException ignored) {
        }

        // The download failed, report failure
        return false;
    }
}

