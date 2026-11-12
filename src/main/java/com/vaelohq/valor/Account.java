package com.vaelohq.valor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Valor accounts on Supabase Auth with username + password only. Supabase needs an email per account, so each
 * username maps to a placeholder address players never see. Its domain has no mail server (no MX records), so no one
 * can receive mail there, which keeps password-reset links from ever reaching a stranger. Network calls run on one background thread; callbacks run on the game thread.
 */
public final class Account {
    private static final String URL = "https://xwcqvmcrukxkhdhqftcg.supabase.co";
    /** Publishable key: safe to ship in the client; the database is protected by row level security. */
    private static final String KEY = "sb_publishable_XDZRiDSJ2YWnrmtkqfEILg_CVAel3fP";
    private static final ExecutorService NET = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Valor accounts");
        t.setDaemon(true);
        return t;
    });

    /** Placeholder address domain. Supabase rejects reserved/test domains, so this is under the org's github.io name. */
    private static final String DOMAIN = "@players.vaelohq.github.io";
    public static String username;
    private static String access, refresh;

    /** Result of a request: ok, or a message to show the player. */
    public static final class Result {
        public final boolean ok;
        public final String message;

        Result(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }
    }

    public static boolean signedIn() {
        return access != null;
    }

    private static String mail(String name) {
        return name.toLowerCase() + DOMAIN;
    }

    /** Whether a username is free: true, false, or null if the server couldn't be reached. */
    public static void available(String name, Consumer<Boolean> done) {
        NET.execute(() -> {
            Boolean free = null;
            try {
                Response r = post("/rest/v1/rpc/username_available", obj("name", name), null);
                if (r.code == 200) free = "true".equals(r.raw.trim());
            } catch (Exception e) {
                Valor.LOGGER.warn("Username check failed", e);
            }
            Boolean f = free;
            Minecraft.getMinecraft().addScheduledTask(() -> done.accept(f));
        });
    }

    /** Checks the name is free, then creates the account and signs in. */
    public static void signUp(String name, String password, boolean remember, Consumer<Result> done) {
        run(done, () -> {
            // With "Confirm email" on, Supabase would create an unconfirmed account that can never sign in.
            Response settings = request("GET", "/auth/v1/settings", null, null);
            if (settings.json == null || !settings.json.has("mailer_autoconfirm") || !settings.json.get("mailer_autoconfirm").getAsBoolean()) {
                Valor.LOGGER.error("Sign up blocked: turn off 'Confirm email' in Supabase Auth settings");
                return fail("Sign up isn't open yet. Try again later.");
            }
            Response r = post("/rest/v1/rpc/username_available", obj("name", name), null);
            if (r.code == 200 && "false".equals(r.raw.trim())) return fail("That username is taken.");
            JsonObject body = obj("email", mail(name));
            body.addProperty("password", password);
            body.add("data", obj("username", name));
            r = post("/auth/v1/signup", body, null);
            if (r.code != 200) return fail(error(r));
            if (r.json == null || !r.json.has("access_token")) {
                Valor.LOGGER.error("Sign up returned no session: turn off 'Confirm email' in Supabase Auth settings");
                return fail("Sign up isn't available right now. Try again later.");
            }
            session(r.json, remember);
            return new Result(true, "Welcome to Valor, " + username + ".");
        });
    }

    public static void logIn(String name, String password, boolean remember, Consumer<Result> done) {
        run(done, () -> {
            JsonObject body = obj("email", mail(name));
            body.addProperty("password", password);
            Response r = post("/auth/v1/token?grant_type=password", body, null);
            if (r.code != 200) return fail(error(r));
            session(r.json, remember);
            return new Result(true, "Signed in as " + username + ".");
        });
    }

    public static void logOut() {
        String token = access;
        access = refresh = username = null;
        sessionFile().delete();
        if (token != null) NET.execute(() -> post("/auth/v1/logout?scope=local", new JsonObject(), token));
    }

    /** Restores a remembered session at startup, if one was saved. */
    public static void restore() {
        File f = sessionFile();
        if (!f.isFile()) return;
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(f)) {
            p.load(in);
        } catch (Exception e) {
            return;
        }
        String token = p.getProperty("refresh_token");
        if (token == null) return;
        username = p.getProperty("username");
        NET.execute(() -> {
            Response r;
            try {
                r = post("/auth/v1/token?grant_type=refresh_token", obj("refresh_token", token), null);
            } catch (Exception e) {
                Valor.LOGGER.warn("Couldn't restore session (offline?)", e);
                return; // keep the saved session; try again next launch
            }
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (r.code == 200) session(r.json, true);
                else if (r.code >= 400 && r.code < 500) logOut(); // token revoked or expired
            });
        });
    }

    private static void session(JsonObject s, boolean remember) {
        access = s.get("access_token").getAsString();
        refresh = s.get("refresh_token").getAsString();
        JsonObject user = s.getAsJsonObject("user");
        JsonObject meta = user.has("user_metadata") ? user.getAsJsonObject("user_metadata") : null;
        username = meta != null && meta.has("username") ? meta.get("username").getAsString() : "Player";
        if (remember) save();
    }

    /** The refresh token lets the player stay signed in; the file is readable only by the current user. */
    private static void save() {
        File f = sessionFile();
        Properties p = new Properties();
        p.setProperty("refresh_token", refresh);
        p.setProperty("username", username);
        try (OutputStream out = new FileOutputStream(f)) {
            p.store(out, "Valor session. Delete this file to sign out.");
        } catch (Exception e) {
            Valor.LOGGER.warn("Couldn't save session", e);
        }
        f.setReadable(false, false);
        f.setReadable(true, true);
        f.setWritable(false, false);
        f.setWritable(true, true);
    }

    private static File sessionFile() {
        return new File(Loader.instance().getConfigDir(), "valor-session.properties");
    }

    // ---- HTTP ----

    private interface Call {
        Result get() throws Exception;
    }

    private static void run(Consumer<Result> done, Call call) {
        NET.execute(() -> {
            Result r;
            try {
                r = call.get();
            } catch (Exception e) {
                Valor.LOGGER.warn("Account request failed", e);
                r = fail("Couldn't reach Valor servers. Check your connection.");
            }
            Result fr = r;
            Minecraft.getMinecraft().addScheduledTask(() -> done.accept(fr));
        });
    }

    private static final class Response {
        final int code;
        final String raw;
        final JsonObject json;

        Response(int code, String raw) {
            this.code = code;
            this.raw = raw;
            JsonObject j = null;
            try {
                if (raw.trim().startsWith("{")) j = new JsonParser().parse(raw).getAsJsonObject();
            } catch (Exception ignored) {
            }
            this.json = j;
        }
    }

    private static Response post(String path, JsonObject body, String bearer) {
        return request("POST", path, body, bearer);
    }

    private static Response request(String method, String path, JsonObject body, String bearer) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(URL + path).openConnection();
            c.setRequestMethod(method);
            c.setConnectTimeout(10000);
            c.setReadTimeout(15000);
            c.setRequestProperty("apikey", KEY);
            if (bearer != null) c.setRequestProperty("Authorization", "Bearer " + bearer);
            if (body != null) {
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                try (OutputStream out = c.getOutputStream()) {
                    out.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            int code = c.getResponseCode();
            InputStream in = code < 400 ? c.getInputStream() : c.getErrorStream();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            if (in != null) {
                byte[] b = new byte[4096];
                for (int n; (n = in.read(b)) > 0; ) buf.write(b, 0, n);
                in.close();
            }
            return new Response(code, new String(buf.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static JsonObject obj(String k, String v) {
        JsonObject o = new JsonObject();
        o.addProperty(k, v);
        return o;
    }

    private static String field(Response r, String k) {
        return r.json != null && r.json.has(k) && !r.json.get(k).isJsonNull() ? r.json.get(k).getAsString() : "";
    }

    private static Result fail(String message) {
        return new Result(false, message);
    }

    /** Turns a Supabase error into a sentence for the player. */
    private static String error(Response r) {
        switch (field(r, "error_code")) {
            case "invalid_credentials": return "Wrong username or password.";
            case "user_already_exists":
            case "email_exists": return "That username is taken.";
            case "weak_password": return "That password is too weak. Try a longer one.";
            case "over_request_rate_limit": return "Too many attempts. Wait a minute and try again.";
            case "signup_disabled": return "Sign up is closed right now.";
            case "email_not_confirmed":
            case "email_address_invalid":
            case "email_address_not_authorized":
            case "over_email_send_rate_limit":
                Valor.LOGGER.error("Supabase rejected the account ({}): turn off 'Confirm email' in Auth settings", field(r, "error_code"));
                return "Accounts aren't available right now. Try again later.";
        }
        String msg = field(r, "msg");
        if (msg.isEmpty()) msg = field(r, "message");
        if (msg.contains("Database error saving new user")) return "That username is taken.";
        return msg.isEmpty() ? "Something went wrong (" + r.code + "). Try again." : msg;
    }
}
