package com.panayotis.appenh;

import com.panayotis.appenh.AFileChooser.FileSelectionMode;
import com.panayotis.appenh.FileChooserFactory.Result;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * The dbus-java "frontman": every reference to dbus-java lives here, so the class only ever
 * loads once {@link LinuxEnhancer} has decided the portal is present. Drives the xdg-desktop-portal
 * FileChooser (OpenFile / SaveFile) over the session bus, which yields the native GTK/KDE dialog
 * on a normal desktop AND transparently works inside a flatpak sandbox. The async Response carries
 * {@code current_filter}, so the chosen Save-As format is reported back exactly like Swing does.
 */
final class PortalFileChooser implements FileChooserFactory {

    private static final String PORTAL = "org.freedesktop.portal.Desktop";
    private static final String PORTAL_PATH = "/org/freedesktop/portal/desktop";

    /** Cheap, cached: is the FileChooser portal reachable on the session bus? Any failure -> false. */
    static boolean isAvailable() {
        try (DBusConnection c = DBusConnectionBuilder.forSessionBus().build()) {
            DBus bus = c.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);
            if (bus.NameHasOwner(PORTAL))
                return true;
            for (String n : bus.ListActivatableNames())
                if (PORTAL.equals(n))
                    return true;
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    /* ===================== FileChooserFactory ===================== */

    @Override
    public Result showOpenDialog(Component parent, String title, String buttonTitle, File directory, boolean openMulti,
                                 FileSelectionMode mode, List<FileNameExtensionFilter> filters) {
        boolean dirsOnly = mode == FileSelectionMode.DirectoriesOnly;
        String token = parentToken(parent);
        return pump(() -> call(token, false, title, directory, null, openMulti, dirsOnly, filters));
    }

    @Override
    public Result showSaveDialog(Component parent, String title, String buttonTitle, File directory, String file,
                                 List<FileNameExtensionFilter> filters) {
        String token = parentToken(parent);
        return pump(() -> call(token, true, title, directory, file, false, false, filters));
    }

    /* ===================== the actual portal round-trip ===================== */

    private Result call(String parent, boolean save, String title, File directory, String file,
                        boolean multiple, boolean dirsOnly, List<FileNameExtensionFilter> filters) throws Exception {
        Map<String, Variant<?>> opts = new HashMap<>();
        opts.put("handle_token", new Variant<>("appenh" + (System.nanoTime() & 0xffffff)));
        if (filters != null && !filters.isEmpty()) {
            List<Filter> pf = new ArrayList<>();
            for (FileNameExtensionFilter f : filters)
                pf.add(toPortalFilter(f));
            opts.put("filters", new Variant<>(pf, "a(sa(us))"));
            opts.put("current_filter", new Variant<>(pf.get(0), "(sa(us))"));
        }
        if (save) {
            if (file != null)
                opts.put("current_name", new Variant<>(file));
            if (directory != null)
                opts.put("current_folder", new Variant<>(pathBytes(directory), "ay"));
        } else {
            if (multiple)
                opts.put("multiple", new Variant<>(Boolean.TRUE));
            if (dirsOnly)
                opts.put("directory", new Variant<>(Boolean.TRUE));
        }

        BlockingQueue<Request.Response> inbox = new ArrayBlockingQueue<>(1);
        try (DBusConnection conn = DBusConnectionBuilder.forSessionBus().build()) {
            conn.addSigHandler(Request.Response.class, sig -> inbox.offer(sig));
            FileChooser fc = conn.getRemoteObject(PORTAL, PORTAL_PATH, FileChooser.class);
            String heading = title != null ? title : (save ? "Save" : "Open");
            String owner = parent != null ? parent : "";
            if (save)
                fc.SaveFile(owner, heading, opts);
            else
                fc.OpenFile(owner, heading, opts);

            Request.Response r = inbox.poll(5, TimeUnit.MINUTES);
            if (r == null || r.response.intValue() != 0)
                return new Result(Collections.<File>emptyList(), null);

            List<File> files = urisToFiles(r.results.get("uris"));
            FileNameExtensionFilter chosen = matchFilter(r.results.get("current_filter"), filters);
            return new Result(files, chosen);
        }
    }

    /* ===================== EDT-friendly blocking ===================== */

    /**
     * The portal reply is asynchronous; the callers ({@code AFileChooser.save()} etc.) are synchronous
     * and usually on the EDT. Run the dbus work on a worker thread and pump a Swing {@link SecondaryLoop}
     * so the application window keeps repainting while the native dialog is up — mirroring how a modal
     * {@code JFileChooser} behaves. Off the EDT we simply block.
     */
    private static Result pump(Callable<Result> task) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                return task.call();
            } catch (Throwable t) {
                return new Result(Collections.<File>emptyList(), null);
            }
        }
        final Result[] out = {new Result(Collections.<File>emptyList(), null)};
        SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
        Thread worker = new Thread(() -> {
            try {
                out[0] = task.call();
            } catch (Throwable ignored) {
            } finally {
                // Post on the EDT so exit() can never race ahead of enter().
                EventQueue.invokeLater(loop::exit);
            }
        }, "appenh-portal");
        worker.setDaemon(true);
        worker.start();
        loop.enter();
        return out[0];
    }

    /* ===================== helpers ===================== */

    /**
     * Build the portal {@code parent_window} token for a caller component: {@code "x11:<hex-XID>"}.
     * Java runs as an X11 client (natively or via XWayland), so its top-level has an X11 window id; we
     * pull it out of the Swing peer by reflection. This needs the bundled launcher to grant
     * {@code --add-opens java.desktop/java.awt=ALL-UNNAMED --add-opens java.desktop/sun.awt=ALL-UNNAMED
     * --add-opens java.desktop/sun.awt.X11=ALL-UNNAMED}; without them (or off X11) we return "" and the
     * dialog simply isn't parented. Returns "" on any failure.
     */
    private static String parentToken(Component comp) {
        try {
            Window w = comp instanceof Window ? (Window) comp : SwingUtilities.getWindowAncestor(comp);
            if (w == null)
                return "";
            Class<?> awtAccessor = Class.forName("sun.awt.AWTAccessor");
            Object compAccessor = awtAccessor.getMethod("getComponentAccessor").invoke(null);
            Method getPeer = compAccessor.getClass().getMethod("getPeer", Component.class);
            getPeer.setAccessible(true);
            Object peer = getPeer.invoke(compAccessor, w);
            if (peer == null)
                return "";
            Method getWindow = peer.getClass().getMethod("getWindow");   // sun.awt.X11.XBaseWindow#getWindow
            getWindow.setAccessible(true);
            long xid = ((Number) getWindow.invoke(peer)).longValue();
            return xid > 0 ? "x11:" + Long.toHexString(xid) : "";
        } catch (Throwable t) {   // non-X11 peer, module not opened, headless, etc.
            return "";
        }
    }

    private static Filter toPortalFilter(FileNameExtensionFilter f) {
        List<Pattern> pats = new ArrayList<>();
        for (String ext : f.getExtensions())
            pats.add(new Pattern(new UInt32(0), "*." + caseInsensitive(ext)));
        return new Filter(f.getDescription(), pats);
    }

    /** Turn "srt" into "[sS][rR][tT]" so the glob matches regardless of case, like the Swing side. */
    private static String caseInsensitive(String ext) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ext.length(); i++) {
            char c = ext.charAt(i);
            char lo = Character.toLowerCase(c), up = Character.toUpperCase(c);
            if (lo != up)
                sb.append('[').append(lo).append(up).append(']');
            else
                sb.append(c);
        }
        return sb.toString();
    }

    private static byte[] pathBytes(File dir) {
        byte[] p = dir.getAbsolutePath().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] withNul = new byte[p.length + 1];   // portal wants a NUL-terminated bytestring
        System.arraycopy(p, 0, withNul, 0, p.length);
        return withNul;
    }

    @SuppressWarnings("unchecked")
    private static List<File> urisToFiles(Variant<?> urisV) {
        List<File> files = new ArrayList<>();
        if (urisV == null)
            return files;
        Object val = urisV.getValue();
        if (val instanceof List)
            for (Object u : (List<Object>) val) {
                try {
                    files.add(new File(new URI(String.valueOf(u))));
                } catch (Exception ignored) {
                }
            }
        return files;
    }

    /** Map the returned {@code current_filter} struct back to the caller's own filter, by name. */
    private static FileNameExtensionFilter matchFilter(Variant<?> cfV, List<FileNameExtensionFilter> filters) {
        if (filters == null || filters.isEmpty())
            return null;
        String name = filterName(cfV == null ? null : cfV.getValue());
        if (name != null)
            for (FileNameExtensionFilter f : filters)
                if (name.equals(f.getDescription()))
                    return f;
        return filters.get(0);   // portal omitted it -> the one we preselected is still current
    }

    @SuppressWarnings("unchecked")
    private static String filterName(Object cf) {
        try {
            if (cf instanceof Filter)
                return ((Filter) cf).name;
            if (cf instanceof Object[]) {
                Object[] a = (Object[]) cf;
                return a.length > 0 ? String.valueOf(a[0]) : null;
            }
            if (cf instanceof List) {
                List<Object> l = (List<Object>) cf;
                return l.isEmpty() ? null : String.valueOf(l.get(0));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /* ===================== D-Bus type shapes ===================== */

    public static final class Pattern extends Struct {
        @Position(0) public final UInt32 type;   // 0 = glob, 1 = mimetype
        @Position(1) public final String value;

        public Pattern(UInt32 type, String value) {
            this.type = type;
            this.value = value;
        }
    }

    public static final class Filter extends Struct {
        @Position(0) public final String name;
        @Position(1) public final List<Pattern> patterns;

        public Filter(String name, List<Pattern> patterns) {
            this.name = name;
            this.patterns = patterns;
        }
    }

    @DBusInterfaceName("org.freedesktop.portal.FileChooser")
    public interface FileChooser extends DBusInterface {
        DBusPath OpenFile(String parentWindow, String title, Map<String, Variant<?>> options);

        DBusPath SaveFile(String parentWindow, String title, Map<String, Variant<?>> options);
    }

    @DBusInterfaceName("org.freedesktop.portal.Request")
    public interface Request extends DBusInterface {
        class Response extends DBusSignal {
            public final UInt32 response;                 // 0 = ok, 1 = cancelled, 2 = error
            public final Map<String, Variant<?>> results;

            public Response(String path, UInt32 response, Map<String, Variant<?>> results) throws Exception {
                super(path, response, results);
                this.response = response;
                this.results = results;
            }
        }
    }
}
