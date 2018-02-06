package httpserver;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import static java.nio.file.LinkOption.*;

import static java.nio.file.StandardWatchEventKinds.*;

public class FileWatcher {
    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private Map<Path, byte[]> cache;
    private Map<Path, Boolean> fileIsModify;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }
    
    FileWatcher(Path dir, Map<Path, byte[]> cache, Map<Path, Boolean> fileIsModify) throws IOException {
        this.cache = cache;
        this.fileIsModify = fileIsModify;
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<>();
        register(dir);
    }

    private void register(final Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws IOException {
                WatchKey key = path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                keys.put(key, path);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    void check() {
        WatchKey key = watcher.poll();
        if (key == null) {
            return;
        }

        Path dir = keys.get(key);
        if (dir == null) {
            return;
        }

        for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();

            if (kind == OVERFLOW) {
                continue;
            }

            WatchEvent<Path> ev = cast(event);
            Path name = ev.context();
            Path filename = dir.resolve(name);

            if (kind == ENTRY_DELETE) {
                cache.remove(filename);
                fileIsModify.remove(filename);
            }
            if (kind == ENTRY_MODIFY) {
                fileIsModify.put(filename, true);
            }

            if (kind == ENTRY_CREATE) {
                try {
                    if (Files.isDirectory(filename, NOFOLLOW_LINKS)) {
                        register(filename);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        key.reset();
    }
}
