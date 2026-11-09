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
 * Valor accounts on Supabase Auth: sign up (with email confirmation), log in, resend confirmation, log out, and an
 * optional remembered session. Network calls run on one background thread; callbacks run on the game thread.
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

    public static String username, email;
    private static String access, refresh;

    /** Result of a request: ok, or a message to show the player. needsConfirm marks "email not confirmed". */
    public static final class Result {
        public final boolean ok, needsConfirm;
        public final String message;

        Result(boolean ok, boolean needsConfirm, String message) {
            this.ok = ok;
            this.needsConfirm = needsConfirm;
            this.message = message;
        }
    }

    public static boolean signedIn() {
        return access != null;
    }

    /** Checks the name is free, then creates the account. Supabase emails a confirmation link. */
    public static void signUp(String name, String mail, String password, Consumer<Result> done) {
        run(done, () -> {
            Response r = post("/rest/v1/rpc/username_available", obj("name", name), null);
            if (r.code == 200 && "false".equals(r.raw.trim())) return fail("That username is taken.");
            JsonObject body = obj("email", mail);
            body.addProperty("password", password);
            body.add("data", obj("username", name));
            r = post("/auth/v1/signup", body, null);
            if (r.code != 200) return fail(error(r));
            if (r.json != null && r.json.has("access_token")) {
                session(r.json, false);
                return new Result(true, false, "Account created. You're signed in.");
            }
            return new Result(true, false, "Check " + mail + " for a confirmation link, then log in.");
        });
    }

    public static void logIn(String mail, String password, boolean remember, Consumer<Result> done) {
        run(done, () -> {
            JsonObject body = obj("email", mail);
            body.addProperty("password", password);
            Response r = post("/auth/v1/token?grant_type=password", body, null);
            if (r.code != 200) {
                boolean confirm = "email_not_confirmed".equals(field(r, "error_code"));
                return new Result(false, confirm, error(r));
            }
            session(r.json, remember);
            return new Result(true, false, "Signed in as " + username + ".");
        });
    }

    public static void resend(String mail, Consumer<Result> done) {
        run(done, () -> {
            JsonObject body = obj("type", "signup");
            body.addProperty("email", mail);
            Response r = post("/auth/v1/resend", body, null);
            return r.code == 200 ? new Result(true, false, "Confirmation email sent again.") : fail(error(r));
        });
    }

    public static void logOut() {
        String token = access;
        access = refresh = username = email = null;
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
        email = p.getProperty("email");
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
        email = user.get("email").getAsString();
        JsonObject meta = user.has("user_metadata") ? user.getAsJsonObject("user_metadata") : null;
        username = meta != null && meta.has("username") ? meta.get("username").getAsString() : email;
        if (remember) save();
    }

    /** The refresh token lets the player stay signed in; the file is readable only by the current user. */
    private static void save() {
        File f = sessionFile();
        Properties p = new Properties();
        p.setProperty("refresh_token", refresh);
        p.setProperty("username", username);
        p.setProperty("email", email);
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
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(URL + path).openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(10000);
            c.setReadTimeout(15000);
            c.setDoOutput(true);
            c.setRequestProperty("apikey", KEY);
            c.setRequestProperty("Content-Type", "application/json");
            if (bearer != null) c.setRequestProperty("Authorization", "Bearer " + bearer);
            try (OutputStream out = c.getOutputStream()) {
                out.write(body.toString().getBytes(StandardCharsets.UTF_8));
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
        return new Result(false, false, message);
    }

    /** Turns a Supabase error into a sentence for the player. */
    private static String error(Response r) {
        switch (field(r, "error_code")) {
            case "invalid_credentials": return "Wrong email or password.";
            case "email_not_confirmed": return "Confirm your email first. Check your inbox.";
            case "user_already_exists":
            case "email_exists": return "An account with this email already exists.";
            case "weak_password": return "That password is too weak. Try a longer one.";
            case "email_address_invalid": return "That email address isn't valid.";
            case "email_address_not_authorized": return "Valor can't send email yet. Try again later.";
            case "over_email_send_rate_limit": return "Too many emails sent. Try again in an hour.";
            case "over_request_rate_limit": return "Too many attempts. Wait a minute and try again.";
            case "signup_disabled": return "Sign up is closed right now.";
        }
        String msg = field(r, "msg");
        if (msg.isEmpty()) msg = field(r, "message");
        if (msg.contains("Database error saving new user")) return "That username is taken.";
        return msg.isEmpty() ? "Something went wrong (" + r.code + "). Try again." : msg;
    }
}
