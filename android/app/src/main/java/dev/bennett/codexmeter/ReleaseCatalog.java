package dev.bennett.codexmeter;

import android.content.Context;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Offline editorial archive, distinct from remotely verified installable releases. */
final class ReleaseCatalog {
    static final class Entry {
        final String version, title, notes, origin, original;
        Entry(JSONObject row, boolean korean) throws Exception {
            version = row.getString("version"); origin = row.getString("origin");original=row.optString("source","");
            JSONObject localized = row.getJSONObject(korean ? "ko" : "en");
            title = localized.getString("title");
            JSONArray lines = localized.getJSONArray("changes");
            StringBuilder body = new StringBuilder();
            for (int i = 0; i < lines.length(); i++) body.append("- ").append(lines.getString(i)).append('\n');
            notes = body.toString();
        }
    }
    static List<Entry> all(Context c) {
        List<Entry> entries = new ArrayList<>();
        boolean korean = c.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ko");
        try (InputStream in = c.getAssets().open("release-history.json")) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096]; int count;
            while ((count = in.read(buffer)) != -1) bytes.write(buffer, 0, count);
            JSONArray rows = new JSONArray(bytes.toString("UTF-8"));
            for (int i = 0; i < rows.length(); i++) entries.add(new Entry(rows.getJSONObject(i), korean));
        } catch (Exception ignored) { /* Remote release information still remains available. */ }
        return entries;
    }
    static String notes(Context c, String version, String fallback) {
        String normalized = version.replaceFirst("^v", "").replaceFirst("[-+].*$", "");
        for (Entry entry : all(c)) if (entry.version.equals(normalized)) return entry.notes;
        return fallback;
    }
}
