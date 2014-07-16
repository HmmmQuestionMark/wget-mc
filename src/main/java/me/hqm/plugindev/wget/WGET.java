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

import com.iciql.Db;
import org.apache.commons.codec.digest.DigestUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

/**
 * wget for bukkit plugins.
 *
 * @author HmmmQuestionMark
 */
public class WGET extends JavaPlugin implements Listener {
    // -- SETTINGS -- //
    public static Boolean DB_USE;
    public static String DB_URL;
    public static Integer DB_SESSION_MINS;


    // -- BUKKIT METHODS -- //

    /**
     * Standard Bukkit enable method.
     */
    @Override
    public void onEnable() {
        DB_USE = getConfig().getBoolean("db.use", true);
        DB_URL = getConfig().getString("db.url");
        DB_SESSION_MINS = getConfig().getInt("db.session_mins", 5);

        getCommand("wget").setExecutor(new WGCommand(this));

        if (DB_USE) {
            loadSQL();
            getServer().getPluginManager().registerEvents(this, this);
        }

        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    /**
     * Bukkit logout event listener.
     *
     * @param event the quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogout(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        WGUser alias = new WGUser();
        Db db = Db.open(DB_URL);

        // Logout the user from the DB when they logout on the server.
        WGUser user = db.from(alias).where(alias.minecraftId).is(player.getUniqueId().toString()).selectFirst();
        user.sessionExpires = new Timestamp(System.currentTimeMillis());

        db.update(user);
        db.close();
    }

    // -- DB UTIL METHODS -- //

    public void loadSQL() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ignored) {
        }
    }

    public static WGUser getUser(Player player) {
        WGUser alias = new WGUser();
        Db db = Db.open(DB_URL);
        try {
            return db.from(alias).where(alias.minecraftId).is(player.getUniqueId().toString()).selectFirst();
        } finally {
            db.close();
        }
    }

    public static boolean isRegistered(Player player) {
        WGUser alias = new WGUser();
        Db db = Db.open(DB_URL);
        try {
            return db.from(alias).where(alias.minecraftId).is(player.getUniqueId().toString()).select().size() > 0;
        } finally {
            db.close();
        }
    }

    public static boolean isLoggedIn(Player player) {
        return getUser(player).sessionExpires.getTime() > System.currentTimeMillis();
    }

    public static void register(Player player, String password) {
        String passwordHash = DigestUtils.sha512Hex(password);

        WGUser user = new WGUser();
        user.minecraftId = player.getUniqueId().toString();
        user.passwordHash = passwordHash;

        Db db = Db.open(DB_URL);
        db.insert(user);
        db.close();

        login(player);
    }

    public static void login(Player player) {
        WGUser alias = new WGUser();
        Db db = Db.open(DB_URL);

        WGUser user = db.from(alias).where(alias.minecraftId).is(player.getUniqueId().toString()).selectFirst();
        user.sessionExpires = new Timestamp(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(DB_SESSION_MINS));
        user.lastKnownName = player.getName();

        db.update(user);
        db.close();

        player.performCommand("wget " + WGCommand.CACHE.get(player.getName()).toString());
    }
}

